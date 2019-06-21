package spring.service;

import com.jaravir.tekila.common.device.DeviceStatus;
import com.jaravir.tekila.module.accounting.manager.AccountingCategoryPersistenceFacade;
import com.jaravir.tekila.module.service.ValueAddedServiceType;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.service.model.BillingPrinciple;
import com.jaravir.tekila.module.service.persistence.manager.ServicePersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.ServiceProviderPersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.SubscriptionServiceTypePersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.ZonePersistenceFacade;
import com.jaravir.tekila.module.store.ats.AtsPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.*;
import com.jaravir.tekila.module.subscription.persistence.management.DealerPersistanceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.ResellerPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import com.jaravir.tekila.provision.broadband.devices.manager.MiniPopPersistenceFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import spring.dto.*;
import spring.exceptions.BadRequestAlertException;
import spring.mapper.AccountingCategoryMapper;
import spring.mapper.AtsMapper;
import spring.mapper.StreetMapper;
import spring.mapper.select.ServiceSelectMapper;
import spring.mapper.select.ServiceTypeSelectMapper;
import spring.mapper.select.ZoneSelectMapper;
import spring.mapper.subscription.DealerMapper;
import spring.mapper.subscription.MinipopMapperForDropdown;
import spring.mapper.subscription.ResellerMapperDropdown;
import spring.security.SecurityModuleUtils;
import spring.security.wrapper.Subject;

import javax.ejb.EJB;
import javax.faces.model.SelectItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static spring.util.Constants.INJECTION_POINT;

/**
 * @author MusaAl
 * @date 4/12/2018 : 3:50 PM
 */
@Service
public class ListService {

    private final Logger log = LoggerFactory.getLogger(ListService.class);

    @EJB(mappedName = INJECTION_POINT + "ServiceProviderPersistenceFacade")
    private ServiceProviderPersistenceFacade providerPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "AccountingCategoryPersistenceFacade")
    private AccountingCategoryPersistenceFacade categoryPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "ServicePersistenceFacade")
    private ServicePersistenceFacade servicePersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "SubscriptionServiceTypePersistenceFacade")
    private SubscriptionServiceTypePersistenceFacade subscriptionServiceTypePersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "ZonePersistenceFacade")
    private ZonePersistenceFacade zonePersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "AtsPersistenceFacade")
    private AtsPersistenceFacade atsPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "MiniPopPersistenceFacade")
    private MiniPopPersistenceFacade miniPopPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "ResellerPersistenceFacade")
    private ResellerPersistenceFacade resellerPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "SubscriptionPersistenceFacade")
    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "DealerPersistanceFacade")
    private DealerPersistanceFacade dealerFacade;

    private final AccountingCategoryMapper categoryMapper;

    private final ServiceSelectMapper serviceSelectMapper;

    private final ServiceTypeSelectMapper serviceTypeSelectMapper;

    private final ZoneSelectMapper zoneSelectMapper;

    private final AtsMapper atsMapper;

    private List<SelectItem> sbnVASTypeList;

    private List<SelectItem> sbnVasStatusList;

    private List<ValueAddedService> allowedVasList;

    // it means minipopMapper
    private final MinipopMapperForDropdown minipopMapper;

    private final ResellerMapperDropdown resellerMapper;

    private final DealerMapper dealerMapper;

    private final StreetMapper streetMapper;


    public ListService(AccountingCategoryMapper categoryMapper, ServiceSelectMapper serviceSelectMapper, ServiceTypeSelectMapper serviceTypeSelectMapper, ZoneSelectMapper zoneSelectMapper, AtsMapper atsMapper, MinipopMapperForDropdown minipopMapper, ResellerMapperDropdown resellerMapper, DealerMapper dealerMapper, StreetMapper streetMapper) {
        this.categoryMapper = categoryMapper;
        this.serviceSelectMapper = serviceSelectMapper;
        this.serviceTypeSelectMapper = serviceTypeSelectMapper;
        this.zoneSelectMapper = zoneSelectMapper;
        this.atsMapper = atsMapper;
        this.minipopMapper = minipopMapper;
        this.resellerMapper = resellerMapper;
        this.dealerMapper = dealerMapper;
        this.streetMapper = streetMapper;
    }


    public List<SubscriptionStatusResponse> getStatusList() {
        return Arrays.stream(SubscriptionStatus.values()).
                map(SubscriptionStatusResponse::from).collect(Collectors.toList());
    }

    public List<ServiceProviderResponse> getProviderList() {
        return providerPersistenceFacade.findAll().stream().
                map(ServiceProviderResponse::from).collect(Collectors.toList());
    }

    public List<BillingPrinciplesResponse> getBillingPrinclipesList() {
        return Arrays.stream(BillingPrinciple.values()).
                map(BillingPrinciplesResponse::from).collect(Collectors.toList());
    }

    public List<AccountingCategoryDTO> getAccountingCategories() {
        return categoryPersistenceFacade.findAll().stream().map(categoryMapper::toDto).collect(Collectors.toList());
    }

    public List<ServiceSelectDTO> getServices() {
        return servicePersistenceFacade.findAll().stream().map(serviceSelectMapper::toDto).collect(Collectors.toList());
    }

    public List<ServiceSelectDTO> getServices(Long provider_id) {
        return servicePersistenceFacade.findAll(provider_id).stream().map(serviceSelectMapper::toDto).collect(Collectors.toList());
    }

    public List<ServiceTypeSelectDTO> getServiceTypes() {
        return subscriptionServiceTypePersistenceFacade.findAll().stream().map(serviceTypeSelectMapper::toDto).collect(Collectors.toList());
    }

    public List<ZoneSelectDTO> getZones() {
        return zonePersistenceFacade.findAll().stream().map(zoneSelectMapper::toDto).collect(Collectors.toList());
    }

    public List<ZoneSelectDTO> getServiceZones(Long serviceId) {
        if (serviceId == null) throw new BadRequestAlertException("Service must be provided!");
        return zonePersistenceFacade.findAllByService(serviceId).stream().map(zoneSelectMapper::toDto).collect(Collectors.toList());
    }

    public List<ZoneSelectDTO> getServiceZones2(Long serviceId) {
        if (serviceId == null) throw new BadRequestAlertException("Service must be provided!");
        return zonePersistenceFacade.findAllByService2(serviceId).stream().map(zoneSelectMapper::toDto).collect(Collectors.toList());
    }


    public List<ZoneSelectDTO> getServiceZones3(Long serviceId) {
        if (serviceId == null) throw new BadRequestAlertException("Service must be provided!");
        return zonePersistenceFacade.findAllByService3(serviceId).stream().map(zoneSelectMapper::toDto).collect(Collectors.toList());
    }

    public List<ZoneSelectDTO> getServiceZones4(Long serviceId) {
        if (serviceId == null) throw new BadRequestAlertException("Service must be provided!");
        return zonePersistenceFacade.findAllByService4(serviceId).stream().map(zoneSelectMapper::toDto).collect(Collectors.toList());
    }

    public List<AtsDTO> getAllAts() {

        String userName = SecurityModuleUtils.getCurrentUserLogin();
        Subject user = SecurityModuleUtils.getCurrentSubject();

        log.info("Username : {}, Current User: {}", userName, user != null ? user.toString() : " cart ");

        return atsPersistenceFacade.findAll().stream().map(atsMapper::toDto).collect(Collectors.toList());
    }

    public List<AtsDTO> getAllAtsByProvider(Long providerId){
        List<Ats> ats = atsPersistenceFacade.findByProvider(providerId);
        return ats.stream().map(atsMapper::toDto).collect(Collectors.toList());
    }

    public List<AtsDTO> getAllAtsByDandigCase(){
        return atsPersistenceFacade.findByProviderByDandigCase().
                stream().
                map(atsMapper::toDto).
                collect(Collectors.toList());
    }

    public List<MinipopDropdownDTO> getAtsMinipops(Long atsId) {
        Ats ats = atsPersistenceFacade.find(atsId);
        return miniPopPersistenceFacade.findByAts(ats, DeviceStatus.ACTIVE)
                .stream().map(minipopMapper::toDto).collect(Collectors.toList());
    }

    public List<PortDTO> getMinipopPorts(Long minipopId) {
//        , Long serviceTypeId
        List<PortDTO> ports = new ArrayList<>();

//        if (serviceTypeId == 1 || serviceTypeId == 2 || serviceTypeId == 3)
//            return ports;

        MiniPop miniPop = miniPopPersistenceFacade.find(minipopId);
        if (miniPop != null) {
            for (int portId = 1; portId <= miniPop.getNumberOfPorts(); ++portId) {
                ports.add(new PortDTO(portId, String.valueOf(portId), miniPop.getReserved(portId) != null));
            }
        }

        return ports;
    }

    public List<ResellerDropdownDTO> findAllReseller() {
        return resellerPersistenceFacade.findAll().stream().map(resellerMapper::toDto).collect(Collectors.toList());
    }

    public List<ResellerDropdownDTO> findAllResellerByProvider(Long providerId) {
        return resellerPersistenceFacade.findByProviderId(providerId).stream().map(resellerMapper::toDto).collect(Collectors.toList());
    }

    public List<SelectItem> getSbnVASTypeList() {
        if (sbnVASTypeList == null) {
            sbnVASTypeList = new ArrayList<>();

            for (ValueAddedServiceType type : ValueAddedServiceType.values()) {
                sbnVASTypeList.add(new SelectItem(type));
            }
        }
        return sbnVASTypeList;
    }

    public List<SelectItem> getSbnVasStatusList() {
        if (sbnVasStatusList == null) {
            sbnVasStatusList = new ArrayList<>();
            List<SubscriptionStatus> sbnVasStatuses = new ArrayList<>(Arrays.asList(SubscriptionStatus.values()));

            for (SubscriptionStatus st : sbnVasStatuses) {
                sbnVasStatusList.add(new SelectItem(st));
            }
        }
        return sbnVasStatusList;
    }

/*
    public List<PaymentTypesResponse> getPaymentTypes() {
        return Arrays.stream(PaymentTypes.values())
                .map(PaymentTypesResponse::from)
                .collect(Collectors.toList());
    }
*/

    public List<PaymentTypes> getPaymentTypes() {
        return Arrays.asList(PaymentTypes.values());
    }

    public List<BillingPrinciple> getBillingPrinclipesList2() {
//        return Arrays.asList(BillingPrinciple.values());
        return Arrays.stream(BillingPrinciple.values())
                .filter(p -> p != BillingPrinciple.REQUIRE_REACTIVATION)
                .collect(Collectors.toList());
    }

    public List<DealerDropdownDTO> findAllDealer() {
        return dealerFacade.findAll().stream().map(dealerMapper::toDto).collect(Collectors.toList());
    }

    public List<StreetDTO> getStreetsByAts(Long atsIndex){
        List<Streets> streets = subscriptionPersistenceFacade.getStreetsOfAts(atsIndex);
        return streets.stream().map(streetMapper::toDto).collect(Collectors.toList());
    }

}
