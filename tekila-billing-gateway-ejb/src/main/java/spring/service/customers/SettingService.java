package spring.service.customers;

import com.jaravir.tekila.common.device.DeviceStatus;
import com.jaravir.tekila.engines.*;
import com.jaravir.tekila.module.service.ServiceSettingType;
import com.jaravir.tekila.module.service.ServiceType;
import com.jaravir.tekila.module.service.entity.*;
import com.jaravir.tekila.module.service.persistence.manager.*;
import com.jaravir.tekila.module.store.ats.AtsPersistenceFacade;
import com.jaravir.tekila.module.store.ip.persistence.IpAddress;
import com.jaravir.tekila.module.subscription.persistence.entity.*;
import com.jaravir.tekila.module.subscription.persistence.management.DealerPersistanceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.ResellerPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionSettingPersistenceFacade;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import com.jaravir.tekila.provision.broadband.devices.manager.MiniPopPersistenceFacade;
import com.jaravir.tekila.provision.broadband.devices.manager.Providers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import spring.dto.*;
import spring.exceptions.CustomerOperationException;
import spring.mapper.AtsMapper;
import spring.mapper.subscription.MinipopMapperForDropdown;
import spring.mapper.subscription.ResellerMapperDropdown;
import spring.mapper.subscription.ServiceMapperToDropdown;
import spring.mapper.subscription.SubscriptionSettingMapper;

import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static spring.util.Constants.INJECTION_POINT;

/**
 * @author ElmarMa on 3/29/2018
 */
@Service
public class SettingService {
    private final Logger logger = LoggerFactory.getLogger(SettingService.class);

    @EJB(mappedName = INJECTION_POINT + "SubscriptionPersistenceFacade")
    private SubscriptionPersistenceFacade subscriptionpf;

    @EJB(mappedName = INJECTION_POINT + "ServicePropertyPersistenceFacade")
    private ServicePropertyPersistenceFacade servicePropertypf;

    @EJB(mappedName = INJECTION_POINT + "MiniPopPersistenceFacade")
    private MiniPopPersistenceFacade miniPoppf;

    @EJB(mappedName = INJECTION_POINT + "SubscriptionServiceTypePersistenceFacade")
    private SubscriptionServiceTypePersistenceFacade serviceTypepf;

    @EJB(mappedName = INJECTION_POINT + "EngineFactory")
    private EngineFactory engineFactory;

    @EJB(mappedName = INJECTION_POINT + "ServicePersistenceFacade")
    private ServicePersistenceFacade servicepf;

    @EJB(mappedName = INJECTION_POINT + "CommonOperationsEngine")
    private CommonOperationsEngine commonOperationsEngine;

    @EJB(mappedName = INJECTION_POINT + "SubscriptionSettingPersistenceFacade")
    private SubscriptionSettingPersistenceFacade subscriptionSettingpf;

    @EJB(mappedName = INJECTION_POINT + "VASPersistenceFacade")
    private VASPersistenceFacade vasPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "ZonePersistenceFacade")
    private ZonePersistenceFacade zonePersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "AtsPersistenceFacade")
    private AtsPersistenceFacade atsPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "ServiceProviderPersistenceFacade")
    private ServiceProviderPersistenceFacade serviceProviderPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "ResellerPersistenceFacade")
    private ResellerPersistenceFacade resellerPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "DealerPersistanceFacade")
    private DealerPersistanceFacade dealerPersistanceFacade;

    @Autowired
    private SubscriptionSettingMapper subscriptionSettingMapper;

    @Autowired
    private ServiceMapperToDropdown serviceMapperToDropdown;

    @Autowired
    private AtsMapper atsMapper;

    @Autowired
    private MinipopMapperForDropdown minipopMapperForDropdown;

    @Autowired
    private ResellerMapperDropdown resellerMapperDropdown;


    public List<SubscriptionSettingDTO> getSubscriptionSettings(Long subscriptionId) {

        Subscription subscription = subscriptionpf.find(subscriptionId);
        if (subscription == null)
            throw new CustomerOperationException("can not load subscription settings ,can not find subscription with id = " + subscriptionId);


        List<SubscriptionSettingDTO> settings = subscription.getSettings().
                stream().
                filter(subscriptionSetting -> generatePredicate(subscription.getService().getProvider()).test(subscriptionSetting)).
                map(subscriptionSetting -> {
                    SubscriptionSettingDTO dto = subscriptionSettingMapper.toDto(subscriptionSetting);
                    dto.setName(getNameFromId(subscriptionSetting, subscription));
                    return dto;
                }).collect(Collectors.toList());

        return settings;
    }

    private Predicate<SubscriptionSetting> generatePredicate(ServiceProvider serviceProvider) {
        Predicate<SubscriptionSetting> predicate = null;
        if (serviceProvider.getId() == Providers.DATAPLUS.getId()) {
            predicate = subscriptionSetting ->
                    subscriptionSetting.getProperties().getType() == ServiceSettingType.DEALER ||
                            subscriptionSetting.getProperties().getType() == ServiceSettingType.BROADBAND_SWITCH ||
                            subscriptionSetting.getProperties().getType() == ServiceSettingType.ZONE ||
                            subscriptionSetting.getProperties().getType() == ServiceSettingType.SERVICE_TYPE ||
                            subscriptionSetting.getProperties().getType() == ServiceSettingType.PASSWORD ||
                            subscriptionSetting.getProperties().getType() == ServiceSettingType.USERNAME ||
                            subscriptionSetting.getProperties().getType() == ServiceSettingType.IP_ADDRESS;
        } else {
            predicate = subscriptionSetting -> true;
        }
        return predicate;
    }


    public void updateServiceAndSettings(Long subscriptionId, EditSettingsRequestDTO editSettingsRequest) {
        logger.info("updateServiceAndSettings method starts: \n subscriptionId: {} \n editSettingsRequest: {}", subscriptionId, editSettingsRequest);

        Subscription subscription = subscriptionpf.find(subscriptionId);
        if (subscription == null)
            throw new CustomerOperationException("can not update subscription settings ,can not find subscription with id = " + subscriptionId);

        if (editSettingsRequest.getServiceTypeId() != null &&
                editSettingsRequest.getZoneId() != null &&
                servicePropertypf.find(
                        String.valueOf(subscription.getService().getId()),
                        String.valueOf(editSettingsRequest.getZoneId())) == null)
            throw new CustomerOperationException("Cannot update settings. The combination of service, zone, and type is not valid");


        try {
            changeMinipop(editSettingsRequest.getServiceTypeId(),
                    editSettingsRequest.getMinipopId(),
                    editSettingsRequest.getPortId(),
                    subscription);

            changeService(editSettingsRequest.getServiceId(), subscription);

            addSettingsIfAbsent(subscription, editSettingsRequest);

            updateSettingsIfExist(subscription, editSettingsRequest);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new CustomerOperationException("can not add or update setting or service ," + e.getMessage(), e);
        }


        try {
            subscription.getDetails().setAts(editSettingsRequest.getAts());
            subscription = subscriptionpf.update(subscription);
            ProvisioningEngine provisioner = engineFactory.getProvisioningEngine(subscription);
            provisioner.reprovision(subscription);
        } catch (Exception ex) {
            throw new CustomerOperationException("Cannot update subscription settings ," + ex.getMessage(), ex);
        }

    }

    public void changeMinipop(Long serviceTypeId, Long minipopId, Long portId, Subscription subscription) {
        SubscriptionServiceType serviceType = serviceTypepf.find(serviceTypeId);

        MiniPop minipop = null;
        if (serviceType.getProfile().getProfileType().equals(ProfileType.IPoE)) {

            if (minipopId == null) {
                throw new CustomerOperationException("Cannot update settings. Minipop must be selected");
            }

            minipop = miniPoppf.find(minipopId);

            if (subscription.getService() != null && subscription.getService().getSettingByType(ServiceSettingType.BROADBAND_SWITCH) != null
                    && minipop != null && minipop.getPreferredPort() != null
                    && !minipop.checkPort(minipop.getPreferredPort())) {
                throw new CustomerOperationException("Cannot update settings. Minipop's preferred port is invalid");
            }

            if (minipopId != null && portId != null && minipop != null) {
                minipop.setNextAvailablePortHintAsNumber(portId.intValue());
                subscriptionpf.changeMinipop(subscription, minipop);
            }

        }

    }

    private void changeService(Long serviceId, Subscription subscription) {
        if (serviceId != null && subscription.getService().getId() != serviceId) {
            engineFactory.
                    getOperationsEngine(subscription)
                    .changeService(subscription,
                            servicepf.find(serviceId),
                            false,
                            false);
        }
    }


    private void addSettingsIfAbsent(Subscription subscription, EditSettingsRequestDTO editSettingsRequest) {
        Map<ServiceSettingType, String> newSettings = new HashMap<>();

        if (!checkSetting(ServiceSettingType.ZONE, subscription) && editSettingsRequest.getZoneId() != null) {
            newSettings.put(ServiceSettingType.ZONE, String.valueOf(editSettingsRequest.getZoneId()));
        }
        if (!checkSetting(ServiceSettingType.SERVICE_TYPE, subscription) && editSettingsRequest.getServiceTypeId() != null) {
            newSettings.put(ServiceSettingType.SERVICE_TYPE, String.valueOf(editSettingsRequest.getServiceTypeId()));
        }
        if (!checkSetting(ServiceSettingType.DEALER, subscription) &&
                editSettingsRequest.getResellerId() != null) {
            newSettings.put(ServiceSettingType.DEALER, String.valueOf(editSettingsRequest.getResellerId()));
        }
        if (!checkSetting(ServiceSettingType.BROADBAND_SWITCH, subscription) && editSettingsRequest.getMinipopId() != null) {
            newSettings.put(ServiceSettingType.BROADBAND_SWITCH, String.valueOf(editSettingsRequest.getMinipopId()));
        }
        if (!checkSetting(ServiceSettingType.BROADBAND_SWITCH_PORT, subscription) && editSettingsRequest.getPortId() != null) {
            newSettings.put(ServiceSettingType.BROADBAND_SWITCH_PORT, String.valueOf(editSettingsRequest.getPortId()));
        }
        if (!checkSetting(ServiceSettingType.USERNAME, subscription) && !editSettingsRequest.getUsername().isEmpty()) {
            newSettings.put(ServiceSettingType.USERNAME, editSettingsRequest.getUsername());
        }
        if (!checkSetting(ServiceSettingType.PASSWORD, subscription) && !editSettingsRequest.getPassword().isEmpty()) {
            newSettings.put(ServiceSettingType.PASSWORD, editSettingsRequest.getPassword());
        }

        if (subscription.getService().getServiceType() == ServiceType.BROADBAND) {
            List<SubscriptionSetting> settingList = subscription.getSettings();
            SubscriptionSetting subscriptionSetting = new SubscriptionSetting();

            for (ServiceSettingType st : newSettings.keySet()) {
                subscriptionSetting.setValue(newSettings.get(st));
                ServiceSetting serviceSetting = commonOperationsEngine.createServiceSetting(
                        st.toString(),
                        st,
                        Providers.findById(subscription.getService().getProvider().getId()),
                        ServiceType.BROADBAND,
                        "");
                subscriptionSetting.setProperties(serviceSetting);
                subscriptionSettingpf.save(subscriptionSetting);
                subscriptionSetting = subscriptionSettingpf.update(subscriptionSetting);
                subscription.setSettings(settingList);
                settingList.add(subscriptionSetting);
                subscriptionSetting = new SubscriptionSetting();
            }
        }
    }

    private void updateSettingsIfExist(Subscription subscription, EditSettingsRequestDTO editSettingsRequest) {
        for (SubscriptionSetting ss : subscription.getSettings()) {
            switch (ss.getProperties().getType()) {
                case ZONE:
                    if (editSettingsRequest.getZoneId() != null) {
                        ss.setValue(String.valueOf(editSettingsRequest.getZoneId()));
                    }
                    break;
                case SERVICE_TYPE:
                    if (editSettingsRequest.getServiceId() != null) {
                        ss.setValue(String.valueOf(editSettingsRequest.getServiceTypeId()));
                    }
                    break;
                case DEALER:
                    if (editSettingsRequest.getResellerId() != null) {
                        ss.setValue(String.valueOf(editSettingsRequest.getResellerId()));
                    }
                    break;
                case BROADBAND_SWITCH:
                    if (editSettingsRequest.getMinipopId() != null) {
                        ss.setValue(String.valueOf(editSettingsRequest.getMinipopId()));
                    }
                    break;
                case BROADBAND_SWITCH_PORT:
                    if (editSettingsRequest.getPortId() != null) {
                        ss.setValue(String.valueOf(editSettingsRequest.getPortId()));
                    }
                    break;
                case PASSWORD:
                    if (editSettingsRequest.getPassword() != null) {
                        ss.setValue(String.valueOf(editSettingsRequest.getPassword()));
                    }
                    break;
                case USERNAME:
                    if (editSettingsRequest.getUsername() != null) {
                        ss.setValue(String.valueOf(editSettingsRequest.getUsername()));
                    }
                    break;
            }
        }
    }


    public String getNameFromId(SubscriptionSetting set, Subscription subscription) {
        List<Long> dealerList = Arrays.asList(Providers.DATAPLUS.getId(), Providers.GLOBAL.getId(), Providers.CNC.getId());
        if (set.getProperties().getType().equals(ServiceSettingType.DEALER)) {
//            if (subscription.getService().getProvider().getId() == Providers.DATAPLUS.getId()) {
            if (dealerList.contains(subscription.getService().getProvider().getId())) {
                String resellerID = set.getValue();
                if (resellerID == null || resellerID.isEmpty()) {
                    return null;
                }
                Reseller reseller = resellerPersistenceFacade.find(Long.valueOf(resellerID));
                if (reseller == null) {
                    return null;
                }
                return reseller.getName();
            } else {
                String dealerID = set.getValue();
                if (dealerID == null || dealerID.isEmpty()) {
                    return null;
                }
                Dealer dealer = dealerPersistanceFacade.find(Long.valueOf(dealerID));
                if (dealer == null) {
                    return null;
                }
                return dealer.getName();
            }
        } else if (set.getProperties().getType().equals(ServiceSettingType.BROADBAND_SWITCH)) {
            String switchID = set.getValue();
            if (switchID == null || switchID.isEmpty()) {
                return null;
            }
            MiniPop miniPop = miniPoppf.find(Long.valueOf(switchID));
            if (miniPop == null) {
                return null;
            }
            return miniPop.getSwitch_id();
        } else if (set.getProperties().getType().equals(ServiceSettingType.SERVICE_TYPE)) {
            String serviceTypeId = set.getValue();
            if (serviceTypeId == null || serviceTypeId.isEmpty()) {
                return null;
            }
            SubscriptionServiceType serviceType =
                    serviceTypepf.find(Long.valueOf(serviceTypeId));
            if (serviceType == null) {
                return null;
            }
            return serviceType.getName();
        } else if (set.getProperties().getType().equals(ServiceSettingType.ZONE)) {
            String zoneId = set.getValue();
            if (zoneId == null || zoneId.isEmpty()) {
                return null;
            }
            Zone zone = zonePersistenceFacade.find(Long.valueOf(zoneId));
            if (zone == null) {
                return null;
            }
            return zone.getName();
        }
        return null;
    }


    public void mergeStaticIP(Long subscriptionId, String staticIp, int count) {

        Subscription subscription = subscriptionpf.find(subscriptionId);
        if (subscription == null)
            throw new CustomerOperationException("can not manipulate static ip ,can not find subscription with id = " + subscriptionId);

        SubscriptionSetting subscriptionSetting = subscription.getSettingByType(ServiceSettingType.IP_ADDRESS);

        if (subscriptionSetting == null) {
            addStaticIP(subscription, staticIp, count);
        } else if (staticIp == null || staticIp.isEmpty() ) {
            removeStaticIP(subscription);
        } else {
            modifyStaticIP(subscription, staticIp, count);
        }
    }

    private void modifyStaticIP(Subscription subscription, String staticIp, int count) {
        try {
            String[] ipSplitted = staticIp.split("\\.");
            IpAddress address = new IpAddress(
                    Integer.valueOf(ipSplitted[0]),
                    Integer.valueOf(ipSplitted[1]),
                    Integer.valueOf(ipSplitted[2]),
                    Integer.valueOf(ipSplitted[3])
            );

            long sbnVasId = 0L;
            if (subscription.getVasList() != null && !subscription.getVasList().isEmpty()) {
                sbnVasId = subscription.getVasList().get(0).getId();
            }
            engineFactory.getOperationsEngine(subscription).editVAS(
                    new VasEditParams.Builder().
                            setSubscription(subscription).
                            setSubscriptionVasId(sbnVasId).
                            setIpAddress(address).
                            setCount(count).build());

        } catch (Exception e) {
            throw new CustomerOperationException("can not modify static ip ," + e.getMessage(), e);
        }
    }

    private void addStaticIP(Subscription subscription, String ip, int count) {
        try {
            ValueAddedService vas = vasPersistenceFacade.findEligibleVas(subscription);

            String[] ipSplitted = ip.split("\\.");
            IpAddress address = new IpAddress(
                    Integer.valueOf(ipSplitted[0]),
                    Integer.valueOf(ipSplitted[1]),
                    Integer.valueOf(ipSplitted[2]),
                    Integer.valueOf(ipSplitted[3])
            );

            engineFactory.getOperationsEngine(subscription).addVAS(
                    new VASCreationParams.Builder().
                            setSubscription(subscription).
                            setValueAddedService(vas).
                            setIpAddress(address).
                            setExpiresDate(null).
                            setCount(count >= 0 ? count : 0).
                            build());

        } catch (Exception e) {
            throw new CustomerOperationException("can not add static ip ," + e.getMessage(), e);
        }
    }

    private void removeStaticIP(Subscription subscription) {
        try {
            SubscriptionVAS subscriptionVAS = null;

            if (subscription.getVasList() != null && !subscription.getVasList().isEmpty()) {
                subscriptionVAS = subscription.getVasList().get(0);
            }

            engineFactory.getOperationsEngine(subscription).
                    removeVAS(subscription, subscriptionVAS);

        } catch (Exception e) {
            throw new CustomerOperationException("can not remove static ip ," + e.getMessage(), e);
        }
    }


    private boolean checkSetting(ServiceSettingType st, Subscription subscription) {
        for (SubscriptionSetting ss : subscription.getSettings()) {
            if (ss.getProperties().getType() == st) {
                return true;
            }
        }
        return false;
    }


    public List<ServiceDropdownDTO> getServicesForSelectBox(Long subscriptionId) {

        Subscription subscription = subscriptionpf.find(subscriptionId);
        if (subscription == null)
            throw new CustomerOperationException("can not load services ,can not find subscription with id = " + subscriptionId);

        List<com.jaravir.tekila.module.service.entity.Service> services = null;

        try {
            services = servicepf.findAll(subscription.getService().getProvider().getId());
            return services.stream().map(service -> serviceMapperToDropdown.toDto(service)).collect(Collectors.toList());
        } catch (Exception e) {
            throw new CustomerOperationException("can not load services ," + e.getMessage(), e);
        }
    }


    public List<SubscriptionServiceType> getServiceTypesForSelectBox(Long providerId) {
        return serviceTypepf.findAll()
                .stream()
                .filter(subscriptionServiceType -> subscriptionServiceType.getProvider() == null || subscriptionServiceType.getProvider().getId() == providerId.longValue())
                .collect(Collectors.toList());
    }


    public List<Zone> getZones() {
        return zonePersistenceFacade.findAll();
    }

    public List<AtsDTO> getAtsForDropDown(Long subscriptionId) {
        Subscription subscription = subscriptionpf.find(subscriptionId);
        if (subscription == null)
            throw new CustomerOperationException("can not load services ,can not find subscription with id = " + subscriptionId);

        List<Ats> atsList = new ArrayList<>();

        List<Long> atsListByProvider = Arrays.asList(Providers.DATAPLUS.getId(), Providers.GLOBAL.getId(), Providers.CNC.getId());
        Long currentProvider = subscription.getService().getProvider().getId();

        logger.info("Ats list for subscription: {}, provider: {}", subscriptionId, currentProvider);

        if (atsListByProvider.contains(currentProvider))
            return atsPersistenceFacade.
                    findByProvider(currentProvider).
                    stream().
                    map(ats -> atsMapper.toDto(ats)).
                    collect(Collectors.toList());

        return atsList.
                stream().
                map(ats -> atsMapper.toDto(ats)).
                collect(Collectors.toList());
    }

    public List<MinipopDropdownDTO> getMinipopsForSelectBox(String atsName, Long serviceTypeId) {

        if (serviceTypeId == 2 || serviceTypeId == 3 || serviceTypeId == 4)
            return new ArrayList<>();

        Ats ats = atsPersistenceFacade.findByIndex(atsName);

        ServiceProvider provider = serviceProviderPersistenceFacade.find(Providers.DATAPLUS.getId());

        if (ats != null && provider != null) {
            List<MiniPop> miniPopList = miniPoppf.findByFilters(provider, ats, DeviceStatus.ACTIVE);
            return miniPopList.stream().map(miniPop -> minipopMapperForDropdown.toDto(miniPop)).collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    public List<PortDTO> getPorts(Long minipopId, Long serviceTypeId) {
        List<PortDTO> ports = new ArrayList<>();

        if (serviceTypeId == 2 || serviceTypeId == 3 || serviceTypeId == 4)
            return ports;

        MiniPop miniPop = miniPoppf.find(minipopId);
        if (miniPop != null) {
            for (int portId = 1; portId <= miniPop.getNumberOfPorts(); ++portId) {
                ports.add(new PortDTO(portId, String.valueOf(portId), miniPop.getReserved(portId) != null));
            }
        }

        return ports;
    }

    public List<ResellerDropdownDTO> getResellersForSelectBox() {
        return resellerPersistenceFacade.
                findAll().
                stream().
                map(reseller -> resellerMapperDropdown.toDto(reseller)).
                collect(Collectors.toList());
    }

}