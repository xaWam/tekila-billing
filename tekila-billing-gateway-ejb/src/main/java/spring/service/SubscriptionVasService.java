package spring.service;


import com.jaravir.tekila.module.periiodic.JobPersistenceFacade;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.service.persistence.manager.VASPersistenceFacade;
import com.jaravir.tekila.module.store.RangePersistenceFacade;
import com.jaravir.tekila.module.store.ip.StaticIPType;
import com.jaravir.tekila.module.store.ip.persistence.IpAddress;
import com.jaravir.tekila.module.store.ip.persistence.IpAddressRange;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionVAS;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionVASPersistenceFacade;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import com.jaravir.tekila.provision.broadband.devices.manager.Providers;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import spring.controller.vm.SubscriptionVASCreationVM;
import spring.dto.CsmAddVasRequest;
import spring.dto.CsmVasRequest;
import spring.dto.IpAddressDTO;
import spring.dto.SubscriptionVASDTO;
import spring.exceptions.CustomerOperationException;
import spring.exceptions.SubscriptionNotFoundException;
import spring.exceptions.VasException;
import spring.mapper.IpAddressMapper;
import spring.mapper.subscription.SubscriptionVasMapper;

import javax.ejb.EJB;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static spring.util.Constants.INJECTION_POINT;

@Service
public class SubscriptionVasService {

    private final Logger log = LoggerFactory.getLogger(SubscriptionVasService.class);

    @EJB(mappedName = INJECTION_POINT + "SubscriptionVASPersistenceFacade")
    private SubscriptionVASPersistenceFacade subscriptionVASPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "SubscriptionPersistenceFacade")
    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "VASPersistenceFacade")
    private VASPersistenceFacade vasPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "RangePersistenceFacade")
    private RangePersistenceFacade rangeFacade;

    @EJB(mappedName = INJECTION_POINT + "JobPersistenceFacade")
    private JobPersistenceFacade jobPersistenceFacade;

    private final SubscriptionVasMapper subscriptionVasMapper;

    private final IpAddressMapper ipAddressMapper;

    public SubscriptionVasService(SubscriptionVasMapper subscriptionVasMapper, IpAddressMapper ipAddressMapper) {
        this.subscriptionVasMapper = subscriptionVasMapper;
        this.ipAddressMapper = ipAddressMapper;
    }

    public SubscriptionVASDTO save(SubscriptionVASDTO subscriptionVASDTO) {
        SubscriptionVAS subscriptionVAS = subscriptionVasMapper.toEntity(subscriptionVASDTO);
        subscriptionVASPersistenceFacade.save(subscriptionVAS);
        return subscriptionVasMapper.toDto(subscriptionVASPersistenceFacade.findForceRefresh(subscriptionVAS.getId()));
    }

    public SubscriptionVASDTO add(SubscriptionVASCreationVM subscriptionVASCreationVM) {
        try {
            Subscription subscription = subscriptionPersistenceFacade.find(subscriptionVASCreationVM.getSubscriptionId());

            ValueAddedService vas = vasPersistenceFacade.find(subscriptionVASCreationVM.getVasId());

            if (checkVasExist(subscription, vas)) {
                log.info("The VAS already exist");
                throw new VasException("The VAS already exist");
            }
//        log.debug("vas fee and count " + getVasFee() + " ** " + getVasAddCount());

            if (subscription.getService().getProvider().getId() == Providers.AZERTELECOMPOST.getId()) {
                vas.setPriceInDouble(subscriptionVASCreationVM.getPrice());
                vas.setCount(subscriptionVASCreationVM.getCount());
            }

            DateTimeFormatter formatter = DateTimeFormat.forPattern("MM/dd/yyyy");  // MM/dd/yyyy
            DateTimeFormatter futureDateFormatter = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm:ss");
            DateTime expirationDate = formatter.parseDateTime(subscriptionVASCreationVM.getExpirationDate());
            DateTime futureDate = subscriptionVASCreationVM.getDateForTheFuture() != null ? futureDateFormatter.parseDateTime(subscriptionVASCreationVM.getDateForTheFuture()) : null;
            String chargedIpAddressString = "192.168.1.1";  // must be resolved
            String freeIpAddressString = chargedIpAddressString;
            List<IpAddress> freeIpList = getFreeIpList(subscription);

            if (subscriptionVASCreationVM != null && futureDate != null && (futureDate.isAfter(DateTime.now().plusMinutes(61)))) {

                jobPersistenceFacade.createVasAddJob(
                        subscription,
                        subscriptionVASCreationVM.getVasId(),
                        expirationDate.toDate(),
                        vas.getStaticIPType() == StaticIPType.NORMAL_CHARGED ? chargedIpAddressString : freeIpAddressString,
                        subscriptionVASCreationVM.getCount(),
                        futureDate);
            } else {
                subscription = subscriptionPersistenceFacade.addVAS(subscription, vas, expirationDate.toDate(),
                        vas.getStaticIPType() == StaticIPType.NORMAL_CHARGED ? chargedIpAddressString : freeIpAddressString,
                        subscriptionVASCreationVM.getCount() != 0.0d ? subscriptionVASCreationVM.getCount() : 1.0, freeIpList);
            }
        } catch (Exception ex) {
            log.error("Error occurs when add subscription vas, subscription id: "+subscriptionVASCreationVM.getSubscriptionId(), ex);
            throw new VasException(ex.getMessage(), ex);
        }

        return null;
    }

    public List<IpAddress> getFreeIpList(Subscription subscription) {
        List<IpAddress> freeIpList = new ArrayList<>();
        MiniPop miniPop = subscriptionPersistenceFacade.findMinipop(subscription);
        List<IpAddressRange> ipRanges = rangeFacade.findRangesByNas(miniPop.getNas());
        for (IpAddressRange range : ipRanges) {
            freeIpList.addAll(range.findFreeAddresses());
        }
        return freeIpList;
    }

    public List<IpAddressDTO> getFreeIpList(Long subscriptionId) {
        List<IpAddress> freeIpList = new ArrayList<>();
        Subscription subscription = subscriptionPersistenceFacade.find(subscriptionId);
        MiniPop miniPop = subscriptionPersistenceFacade.findMinipop(subscription);
        List<IpAddressRange> ipRanges = rangeFacade.findRangesByNas(miniPop.getNas());
        for (IpAddressRange range : ipRanges) {
            freeIpList.addAll(range.findFreeAddresses());
        }
        return freeIpList.stream().map(ipAddressMapper::toDto).collect(Collectors.toList());
    }

    public boolean checkVasExist(Subscription subscription, ValueAddedService vas) {
        if (vas.getMaxNumber() != 1 || subscription.getActiveResource() == null) {
            return false;
        }
        for (SubscriptionVAS sbnVAS : subscription.getVasList()) {
            if (sbnVAS.getVas().getId() == vas.getId()) {
                return true;
            }
        }
        return false;
    }

    public SubscriptionVASDTO update(SubscriptionVASDTO subscriptionVASDTO) {

        SubscriptionVAS subscriptionVAS = subscriptionVASPersistenceFacade.find(subscriptionVASDTO.getId());
        subscriptionVASDTO.attachToEntity(subscriptionVAS);
        subscriptionVAS = subscriptionVASPersistenceFacade.update(subscriptionVAS);

        return subscriptionVasMapper.toDto(subscriptionVAS);
    }

    public List<SubscriptionVASDTO> findAllBySubscription(Long subscriptionId) {
        List<SubscriptionVAS> subsVas = subscriptionVASPersistenceFacade.findBySubscription(subscriptionId);
        log.info("subsVas -> " + subsVas);
        return subsVas.stream().map(subscriptionVasMapper::toDto).collect(Collectors.toList());
    }

    public SubscriptionVASDTO findOne(Long id) {
        SubscriptionVAS subscriptionVAS = subscriptionVASPersistenceFacade.find(id);
        return subscriptionVasMapper.toDto(subscriptionVAS);
    }

    public void delete(Long id) {
        try {
            subscriptionVASPersistenceFacade.removeIt(id);
        }catch (Exception ex){
            log.error("Error occurs when delete subscription vas", ex);
            throw new CustomerOperationException(ex.getMessage(), ex);
        }
    }


    public SubscriptionVASDTO addCsmVas(CsmAddVasRequest vasRequest) {
        Long subscriptionId = Optional.ofNullable(subscriptionPersistenceFacade.findSubscriptionIdByAgreement(vasRequest.getAgreement()))
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found!"));
        SubscriptionVASCreationVM vm = new SubscriptionVASCreationVM();
        vm.setSubscriptionId(subscriptionId);
        vm.setVasId(vasRequest.getVasId());
        vm.setExpirationDate("12/31/2099");
        return add(vm);
    }

    public void delete(String agreement, Long vasId) {
        Subscription subscription = Optional.ofNullable(subscriptionPersistenceFacade.findByAgreementOrdinary(agreement))
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found!"));

        log.info("vas list: {}", subscription.getVasList());
        boolean flag = false;
        for (SubscriptionVAS sbnVAS : subscription.getVasList()) {
            log.debug("sbnVAS.getVas().getId() [{}] == vasId [{}]", sbnVAS.getVas().getId(), vasId);
            if (sbnVAS.getVas().getId() == vasId) {
                flag = true;
                delete(sbnVAS.getId());
            }
        }
        if (!flag)
            throw new VasException("VAS not found or not periodic static");
    }


    public boolean modifyCsmVas(CsmVasRequest vasRequest) {
        switch (vasRequest.getActionType()) {
            case 1: //SUBSCRIBE
            case 4: //RESUME:
                CsmAddVasRequest addVas = new CsmAddVasRequest(vasRequest.getAgreement(), vasRequest.getVasId());
                addCsmVas(addVas);
                break;
            case 2: //UNSUBSCRIBE
            case 3: //SUSPEND
                delete(vasRequest.getAgreement(), vasRequest.getVasId());
                break;
            default:
                throw new CustomerOperationException("Action type is not correct! Should be 1 (SUBSCRIBE), 2 (UNSUBSCRIBE), 3 (SUSPEND) or 4 (RESUME)");
        }

        return true;
    }

}
