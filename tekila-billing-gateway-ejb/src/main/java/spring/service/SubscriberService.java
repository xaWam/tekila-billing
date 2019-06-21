package spring.service;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.TaxCategoryPeristenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscriber;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberDetails;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriberPersistenceFacade;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import spring.Filters;
import spring.controller.vm.SubscriberExistVM;
import spring.dto.SubscriberDTO;
import spring.exceptions.BadRequestAlertException;
import spring.exceptions.CustomerOperationException;
import spring.mapper.SubscriberMapper;
import spring.security.SecurityModuleUtils;

import javax.ejb.EJB;
import javax.ws.rs.BadRequestException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author MusaAl
 * @date 3/12/2018 : 3:39 PM
 */
@Service
//@Transactional
public class SubscriberService {


    private final Logger log = LoggerFactory.getLogger(SubscriberService.class);

    @EJB(mappedName = "java:global/tekila-billing-gateway-ear-0.0.1/tekila-billing-gateway-ejb-0.0.1/SubscriberPersistenceFacade")
    private SubscriberPersistenceFacade subscriberPersistenceFacade;

    @EJB(mappedName = "java:global/tekila-billing-gateway-ear-0.0.1/tekila-billing-gateway-ejb-0.0.1/UserPersistenceFacade")
    private UserPersistenceFacade userPersistenceFacade;

    @EJB(mappedName = "java:global/tekila-billing-gateway-ear-0.0.1/tekila-billing-gateway-ejb-0.0.1/TaxCategoryPeristenceFacade")
    private TaxCategoryPeristenceFacade taxCategoryPeristenceFacade;

    private final SubscriberMapper subscriberMapper;

    public SubscriberService(SubscriberMapper subscriberMapper) {
        this.subscriberMapper = subscriberMapper;
    }

    public SubscriberDTO save(SubscriberDTO subscriberDTO) throws BadRequestAlertException {
        log.debug("Request for saving Subscriber : {}", subscriberDTO);
        Subscriber subscriber = subscriberMapper.toEntity(subscriberDTO);

        SubscriberDetails subscriberDetails = subscriber.getDetails();

        /**
         *  Checking subscriber existing. If exist throw exception
         * */
        SubscriberExistVM vm = new SubscriberExistVM();
        vm.setPassportNumber(subscriberDetails.getPassportNumber());
        vm.setPinCode(subscriberDetails.getPinCode());

        if(subscriberIsExist(vm)) throw new BadRequestAlertException("Customer already exist!");

        String currentWorkingUserOnThreadLocal = SecurityModuleUtils.getCurrentUserLogin();

        log.info("Subscriber creation process -> Currently working user is : {}", currentWorkingUserOnThreadLocal);

        User user = userPersistenceFacade.findByUserName(currentWorkingUserOnThreadLocal);

        subscriberDetails.setEntryDate(DateTime.now().toDate());
        subscriberDetails.setUser(user);


        subscriber.setMasterAccount(Long.valueOf(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())));
        subscriber.setTaxCategory(taxCategoryPeristenceFacade.findDefault());
        subscriber.setDetails(subscriberDetails);
        subscriberDetails.setSubscriber(subscriber);

        try {
            subscriberPersistenceFacade.save(subscriber);
        } catch (Exception e) {
            log.debug("Exception on saving subscriber and details " + e);
        }

        return subscriberMapper.toDto(subscriberPersistenceFacade.findForceRefresh(subscriber.getId()));
    }


    public SubscriberDTO update(SubscriberDTO subscriberDTO) {
        log.debug("Request for updateing Subscriber : {}", subscriberDTO);
        Subscriber subscriber = subscriberPersistenceFacade.find(subscriberDTO.getId());

        subscriber = subscriberPersistenceFacade.updateIt(subscriber);

        return subscriberMapper.toDto(subscriber);
    }

    public List<SubscriberDTO> findAll() {
        log.debug("Request to get all Subscriber");
        return subscriberPersistenceFacade.findAllAscending().stream().
                map(subscriberMapper::toDto).collect(Collectors.toList());
    }

    public SubscriberDTO find(Long id) {
        log.debug("Request to get one Subscriber: {} ", id);
        return subscriberMapper.toDto(subscriberPersistenceFacade.find(id));
    }

    public void delete(Long id) {
        log.debug("Request to delete Subscriber: {} ", id);
        Subscriber subscriber = subscriberPersistenceFacade.find(id);
        subscriberPersistenceFacade.removeIt(subscriber);
    }

    public boolean subscriberIsExist(SubscriberExistVM subscriberExistVM) {

        if (subscriberExistVM == null) return false;

        Filters filters = new Filters();
        filters.clearFilters();
        log.debug(String.format("count() method. serial = %s, passportNumber = %s, pinCode = %s",
                subscriberExistVM.getPassportSeries(), subscriberExistVM.getPassportNumber(), subscriberExistVM.getPinCode()));

        if (subscriberExistVM.getPassportNumber() != null) {
            filters.addFilter(SubscriberPersistenceFacade.Filter.PASSPORTNUMBER, subscriberExistVM.getPassportNumber());
        }

        if (subscriberExistVM.getPinCode() != null) {
            filters.addFilter(SubscriberPersistenceFacade.Filter.PIN, subscriberExistVM.getPinCode());
        }

        long cnt = subscriberPersistenceFacade.count(filters);


        return cnt > 0;
    }



    public List<SubscriberDTO> findAllMatching(SubscriberExistVM subscriberExistVM) {

        if (subscriberExistVM == null) return null;

        Filters filters = new Filters();
        filters.clearFilters();
        log.debug(String.format("find all matched method. serial = %s, passportNumber = %s, pinCode = %s",
                subscriberExistVM.getPassportSeries(), subscriberExistVM.getPassportNumber(), subscriberExistVM.getPinCode()));

        if (subscriberExistVM.getPassportNumber() != null) {
            filters.addFilter(SubscriberPersistenceFacade.Filter.PASSPORTNUMBER, subscriberExistVM.getPassportNumber());
        }

        if (subscriberExistVM.getPinCode() != null) {
            filters.addFilter(SubscriberPersistenceFacade.Filter.PIN, subscriberExistVM.getPinCode());
        }

         return subscriberPersistenceFacade.findAllPaginated(0, 10, filters)
                 .stream()
                 .map(subscriberMapper::toDto)
                 .collect(Collectors.toList());
    }

//    public List<SubscriberDTO> findAllByCustomCriteria(String name, String subscriberIndex){
//        log.debug("Request to get all Subscriber by custom criteria name: {} and SubscriberIndex: {} ",name, subscriberIndex);
//        return subscriberPersistenceFacade.findAllByCustomCriteria(name, subscriberIndex)
//                .stream()
//                .map(SubscriberMapper::toDto).collect(Collectors.toList());
//
//    }
}
