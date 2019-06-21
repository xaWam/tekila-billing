package spring.service;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.base.entity.Language;
import com.jaravir.tekila.base.model.BillingModelPersistenceFacade;
import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.engines.OperationsEngine;
import com.jaravir.tekila.equip.EquipmentPersistenceFacade;
import com.jaravir.tekila.module.campaign.Campaign;
import com.jaravir.tekila.module.campaign.CampaignPersistenceFacade;
import com.jaravir.tekila.module.event.BillingEvent;
import com.jaravir.tekila.module.queue.PersistentQueueManager;
import com.jaravir.tekila.module.service.NotificationSettingRow;
import com.jaravir.tekila.module.service.ServiceSettingType;
import com.jaravir.tekila.module.service.entity.*;
import com.jaravir.tekila.module.service.model.BillingModel;
import com.jaravir.tekila.module.service.model.BillingPrinciple;
import com.jaravir.tekila.module.service.persistence.manager.ServicePersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.ServicePropertyPersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.ServiceProviderPersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.SubscriptionServiceTypePersistenceFacade;
import com.jaravir.tekila.module.store.equip.Equipment;
import com.jaravir.tekila.module.subscription.exception.DuplicateAgreementException;
import com.jaravir.tekila.module.subscription.persistence.entity.*;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriberDetailsPeristenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriberPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionResourcePersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.sequence.AgreementGenerator;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import com.jaravir.tekila.provision.broadband.devices.exception.NoFreePortLeftException;
import com.jaravir.tekila.provision.broadband.devices.exception.PortAlreadyReservedException;
import com.jaravir.tekila.provision.broadband.devices.manager.MiniPopPersistenceFacade;
import com.jaravir.tekila.provision.broadband.devices.manager.Providers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import spring.controller.vm.SubscriptionCreationVM;
import spring.dto.MiniPopDTO;
import spring.dto.SubscriberDetailsSmallDTO;
import spring.dto.SubscriptionDTO;
import spring.dto.SubscriptionResourceDTO;
import spring.exceptions.BadRequestAlertException;
import spring.exceptions.CustomerOperationException;
import spring.exceptions.SubscriptionNotFoundException;
import spring.mapper.MiniPopMapper;
import spring.mapper.SubscriberMapper;
import spring.mapper.SubscriberSmallDetailsMapper;
import spring.mapper.subscription.ServicePropertyMapper;
import spring.mapper.subscription.SubscriptionMapper;
import spring.mapper.subscription.SubscriptionResourceMapper;
import spring.security.SecurityModuleUtils;
import spring.service.customers.ExternalSystemService;
import spring.service.customers.SettingService;

import javax.ejb.EJB;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static spring.util.Constants.INJECTION_POINT;

/**
 * @author MusaAl
 * @date 3/12/2018 : 3:39 PM
 */
@org.springframework.stereotype.Service
//@Transactional
public class   SubscriptionService {


    private final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    @EJB(mappedName = INJECTION_POINT+"SubscriberPersistenceFacade")
    private SubscriberPersistenceFacade subscriberPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT+"SubscriptionPersistenceFacade")
    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT+"UserPersistenceFacade")
    private UserPersistenceFacade userPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT+"BillingModelPersistenceFacade")
    private BillingModelPersistenceFacade modelFacade;

    @EJB(mappedName = INJECTION_POINT+"CampaignPersistenceFacade")
    private CampaignPersistenceFacade campaignFacade;

    @EJB(mappedName = INJECTION_POINT+"EngineFactory")
    private EngineFactory engineFactory;

    @EJB(mappedName = INJECTION_POINT+"SystemLogger")
    private SystemLogger systemLogger;

    @EJB(mappedName = INJECTION_POINT+"PersistentQueueManager")
    private PersistentQueueManager queueManager;

    @EJB(mappedName = INJECTION_POINT+"MiniPopPersistenceFacade")
    private MiniPopPersistenceFacade miniPopPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT+"ServicePropertyPersistenceFacade")
    private ServicePropertyPersistenceFacade servicePropertyPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT+"SubscriptionServiceTypePersistenceFacade")
    private SubscriptionServiceTypePersistenceFacade serviceTypePersistenceFacade;

    @EJB(mappedName = INJECTION_POINT+"ServicePersistenceFacade")
    private ServicePersistenceFacade servicePersistenceFacade;

    @EJB(mappedName = INJECTION_POINT+"SubscriptionResourcePersistenceFacade")
    private SubscriptionResourcePersistenceFacade subscriptionResourcePersistenceFacade;

    @EJB(mappedName = INJECTION_POINT+"SubscriberDetailsPeristenceFacade")
    private SubscriberDetailsPeristenceFacade subscriberDetailsPeristenceFacade;

    @EJB(mappedName = INJECTION_POINT+"EquipmentPersistenceFacade")
    private EquipmentPersistenceFacade equipmentPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT+"ServiceProviderPersistenceFacade")
    private ServiceProviderPersistenceFacade serviceProviderPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT+"AgreementGenerator")
    private AgreementGenerator agreementGenerator;

    @Autowired
    private SettingService settingService;

    private final SubscriptionMapper subscriptionMapper;

    private final SubscriberMapper subscriberMapper;

    private final MiniPopMapper miniPopMapper;

    private final SubscriberSmallDetailsMapper subscriberSmallDetailsMapper;

    private final ServicePropertyMapper servicePropertyMapper;


    @Autowired
    private ExternalSystemService externalSystemService;

    public SubscriptionService(SubscriptionMapper subscriptionMapper,
                               SubscriberMapper subscriberMapper,
                               MiniPopMapper miniPopMapper,
                               SubscriberSmallDetailsMapper subscriberSmallDetailsMapper,
                               ServicePropertyMapper servicePropertyMapper) {
        this.subscriptionMapper = subscriptionMapper;
        this.subscriberMapper = subscriberMapper;
        this.miniPopMapper = miniPopMapper;
        this.subscriberSmallDetailsMapper = subscriberSmallDetailsMapper;
        this.servicePropertyMapper = servicePropertyMapper;
    }

    /**
     *  DataPlus creation. For now just return Subscription id... in future SubscriptionDTO
     * */
    public Long save(SubscriptionCreationVM subscriptionCreationVM) throws BadRequestAlertException {

        log.debug("Request for saving Subscription : {}", subscriptionCreationVM);

        log.debug("subscriptionCreationVM.getSelectedSubscriber() "+subscriptionCreationVM.getSelectedSubscriber());
        // for now I only transfer subscriber id... that's why need fetching in backend again
        Subscriber selectedSubscriber = subscriberMapper.toEntity(subscriptionCreationVM.getSelectedSubscriber());

        log.debug("selectedSubscriber "+selectedSubscriber);

        log.debug("subscriptionCreationVM.getSubscriptionDto() "+subscriptionCreationVM.getSubscriptionDto());
        Subscription subscription = subscriptionMapper.creationDTOtoEntity(subscriptionCreationVM.getSubscriptionDto());
        log.debug("subscription.getSubscriber() " +subscription.getSubscriber());

        MiniPop selectedMiniPop = null;

        Campaign selectedCampaign = null;

        boolean isUseStockEquipment = false;

        Equipment selectedEquipment = null;

        List<NotificationSettingRow> notificationSettings = null;

        subscription.setBillingModel(modelFacade.find(BillingPrinciple.CONTINUOUS));

        if(subscription.getAgreement() == null){
            log.debug("Cannot create subscription. Agreement must be provided.");
            throw new BadRequestAlertException(" Agreement must be provided. There is no agreement provided");
        }
        if (subscriptionCreationVM.getZoneId() == null) {
            log.debug("Cannot create subscription. Zone must be selected");
            throw new BadRequestAlertException("Zone must be selected. There is no zone selected");
        }
        if (subscriptionCreationVM.getServiceTypeId() == null) {
            log.debug("Cannot create subscription. Service Type(PPPoE/Wireless/IPoE) must be selected");
            throw new BadRequestAlertException("Service type must be selected. There is no service type selected");
        }
        if (selectedSubscriber == null) {
            log.debug("Cannot create subscription. Subscriber not selected");
            throw new BadRequestAlertException("Subscriber must be selected. There is no subscriber selected");
        }
//        if (isUseStockEquipment && selectedEquipment == null) {
//            log.debug("Cannot create subscription. Equipment not selected");
//            throw new BadRequestAlertException("Equipment must be selected. There is no equipment selected");
//        }

        if (subscriptionCreationVM.getServiceId() == null) {
            log.debug("Cannot create subscription. Service not selected");
            throw new BadRequestAlertException("Service must be selected. There is no service selected");
        }

        if (servicePropertyPersistenceFacade.find(subscriptionCreationVM.getServiceId().toString(), subscriptionCreationVM.getZoneId()) == null) {
            log.debug("Cannot create subscription. The combination of service, zone, and type is not valid");
            throw new BadRequestAlertException("The combination of service, zone, and type is not valid. The combination of service, zone, and type is not valid");
        }

        SubscriptionServiceType serviceType =
                serviceTypePersistenceFacade.find(Long.parseLong(subscriptionCreationVM.getServiceTypeId()));

        Service service = servicePersistenceFacade.find(subscriptionCreationVM.getServiceId());

        if (serviceType.getProfile().getProfileType().equals(ProfileType.IPoE)) { //IPoE
            if (subscriptionCreationVM.getMinipopId() == null || subscriptionCreationVM.getPortId() == null) {
                log.debug("Cannot create subscription. Minipop and port must be selected");
                throw new BadRequestAlertException("Cannot create subscription. Minipop and port must be selected. Cannot create subscription. Minipop and port must be selected");
            }

            selectedMiniPop = miniPopPersistenceFacade.find(subscriptionCreationVM.getMinipopId());
            selectedMiniPop.setNextAvailablePortHintAsNumber(subscriptionCreationVM.getPortId());

            if (service != null && service.getSettingByType(ServiceSettingType.BROADBAND_SWITCH) != null
                    && selectedMiniPop != null && selectedMiniPop.getPreferredPort() != null
                    && !selectedMiniPop.checkPort(selectedMiniPop.getPreferredPort())) {
                log.debug("Cannot create subscription. Minipop's preferred port is invalid. " + selectedMiniPop);
                throw new BadRequestAlertException("Minipop's port is invalid. Minipop's port is invalid");
            }
        } else { //PPPoE
            if (subscriptionCreationVM.getMinipopId() != null || subscriptionCreationVM.getPortId() != null || selectedMiniPop != null) {
                log.debug("Cannot create subscription. Minipop/port not needed for PPPoE");
                throw new BadRequestAlertException("Cannot create subscription. Minipop/port not needed for PPPoE. Cannot create subscription. Minipop/port not needed for PPPoE");
            }
        }

        //add default VASes
//        if (getSubscriptionVAS() != null && getSubscriptionVAS().getSettings().size() > 0) {
//            subscription.addVAS(subscriptionVAS);
//        }


//        if (service != null && service.getDefaultVasList() != null
//                && (subscription.getVasList().size() < service.getDefaultVasList().size())) {
//            log.debug("Not all charges for default VAS added: subscriptionVas: " + subscriptionVAS.getSettings() + ", defaultVasList: " + service.getDefaultVasList());
//            throw new BadRequestAlertException("Not all charges added. Not all charges added");
//            return null;
//        }


        try {
            // in case of finding any data with this aggrement it will throw exception
            subscriptionPersistenceFacade.findByAgreement(subscription.getAgreement());

            selectedSubscriber = subscriberPersistenceFacade.find(selectedSubscriber.getId());

            subscription.setService(service);

            subscription.setPaymentType(PaymentTypes.CASH);
            BillingModel billingModel = modelFacade.find(BillingPrinciple.CONTINUOUS);

            log.debug("Billing model: " + billingModel);
            subscription.setBillingModel(billingModel);

            subscription.getDetails().setLanguage(Language.AZERI);

            log.debug("Selected minipop: " + selectedMiniPop);
            // Port port = minipopFacade.getAvailablePort(selectedMiniPop);
            // log.debug("Resulting port: " + port);
            String currentWorkingUserOnThreadLocal = SecurityModuleUtils.getCurrentUserLogin();

            log.info("Subscriber creation process -> Currently working user is : {}", currentWorkingUserOnThreadLocal);

            User user = userPersistenceFacade.findByUserName(currentWorkingUserOnThreadLocal);

            boolean modemFree = false;
            if (modemFree) {
                for (final Campaign campaign : campaignFacade.findAllActive(subscription.getService(), false)) {
                    if (campaign.isAvailableOnCreation()) {
                        selectedCampaign = campaign;
                        break;
                    }
                }
            } else {
                for (final Campaign campaign : campaignFacade.findAllActive(subscription.getService(), true)) {
                    if (!campaign.isActivateOnPayment()) {
                        selectedCampaign = campaign;
                        break;
                    }
                }
            }

            OperationsEngine operationsEngine = engineFactory.getOperationsEngine(subscription.getService().getProvider());
            operationsEngine.createSubscriptionSpring(
                    selectedSubscriber,
                    subscription,
                    subscriptionCreationVM.getServiceId(),
                    selectedMiniPop,
                    isUseStockEquipment,
                    selectedEquipment,
                    notificationSettings,
                    0D,
                    selectedCampaign,
                    user,
                    subscriptionCreationVM.getZoneId(),
                    subscriptionCreationVM.getServiceTypeId(),
                    subscriptionCreationVM.getSelectedReseller()
            );


            log.debug("finished");
            log.debug(subscription.getAgreement()+"   subscription");
            log.debug(subscription.getSubscriber()+"   subscriber");
            log.debug(subscription.getSubscriber().getId()+"   iiiii");
            systemLogger.success(SystemEvent.SUBSCRIPTION_CREATED, subscription,
                    "Subscription created"
            );
            try {
                queueManager.addToNotificationsQueue(BillingEvent.SUBSCRIPTION_ADDED, subscription.getId(), null);
                systemLogger.success(SystemEvent.NOTIFICATION_SCHEDULED, subscription,
                        String.format("Spring Event=" + BillingEvent.SUBSCRIPTION_ADDED)
                );
            } catch (Exception ex) {
                log.error("Spring Cannot add to notification queue: ", ex);
                systemLogger.error(SystemEvent.NOTIFICATION_SCHEDULED, subscription, ex.getCause().getMessage());
            }

        } catch (DuplicateAgreementException ex) {
            log.error("Cannot create subscription: ", ex);
            throw new BadRequestAlertException("Agreement already exists!");
        } catch (NoFreePortLeftException ex) {
            log.error("Cannot create subscription: ", ex);
            throw new BadRequestAlertException("Minipop has no free ports left!");
        } catch (PortAlreadyReservedException ex) {
            String msg = String.format("Port %d already reserved or illegal", selectedMiniPop.getPreferredPort());
            log.error("Cannot create subscription: ", ex);
            throw new BadRequestAlertException(msg);
        } catch (Exception ex) {
            log.error("Cannot create subscription: ", ex);
            systemLogger.error(SystemEvent.SUBSCRIPTION_CREATED, subscription, ex.getCause().getMessage());
            throw new BadRequestAlertException("Cannot create subscription");
        }

        return subscription.getId();
//        return subscriptionMapper.toDto(subscriptionPersistenceFacade.findForceRefresh(subscription.getId()));
    }




    public Long saveBBTVNkhchivan(SubscriptionCreationVM subscriptionCreationVM) throws BadRequestAlertException {
        log.debug("Request for saving Subscription : {}", subscriptionCreationVM.getSelectedSubscriber().getId());

        // for now I only transfer subscriber id... that's why need fetching in backend again
        Subscriber selectedSubscriber = subscriberMapper.toEntity(subscriptionCreationVM.getSelectedSubscriber());

        Subscription subscription = subscriptionMapper.creationDTOtoEntity(subscriptionCreationVM.getSubscriptionDto());
        log.debug("equipment "+subscriptionCreationVM.getEquipmentID());
        MiniPop selectedMiniPop = null;

        Campaign selectedCampaign = null;

        boolean isUseStockEquipment = true;

        Equipment selectedEquipment = null;
        if (subscriptionCreationVM.getEquipmentID() != 0){
            selectedEquipment = equipmentPersistenceFacade.find(subscriptionCreationVM.getEquipmentID());
        }

        List<NotificationSettingRow> notificationSettings = null;

        subscription.setBillingModel(modelFacade.find(BillingPrinciple.CONTINUOUS));

        if(subscription.getAgreement() == null){
            log.debug("Cannot create subscription. Agreement must be provided.");
            throw new BadRequestAlertException(" Agreement must be provided. There is no agreement provided");
        }


        if (selectedSubscriber == null) {
            log.debug("Cannot create subscription. Subscriber not selected");
            throw new BadRequestAlertException("Subscriber must be selected. There is no subscriber selected");
        }
        if (isUseStockEquipment && selectedEquipment == null) {
            log.debug("Cannot create subscription. Equipment not selected");
            throw new BadRequestAlertException("Equipment must be selected. There is no equipment selected");
        }

        if (subscriptionCreationVM.getServiceId() == null) {
            log.debug("Cannot create subscription. Service not selected");
            throw new BadRequestAlertException("Service must be selected. There is no service selected");
        }


        Service service = servicePersistenceFacade.find(subscriptionCreationVM.getServiceId());


        try {
            // in case of finding any data with this aggrement it will throw exception
            subscriptionPersistenceFacade.findByAgreement(subscription.getAgreement());

            selectedSubscriber = subscriberPersistenceFacade.find(selectedSubscriber.getId());
            log.debug("selectedSubscriber "+selectedSubscriber);
            subscription.setService(service);

            subscription.setPaymentType(PaymentTypes.CASH);
            BillingModel billingModel = modelFacade.find(BillingPrinciple.CONTINUOUS);

            log.debug("Billing model: " + billingModel);
            subscription.setBillingModel(billingModel);

            subscription.getDetails().setLanguage(Language.AZERI);

            log.debug("Selected minipop: " + selectedMiniPop);
            // Port port = minipopFacade.getAvailablePort(selectedMiniPop);
            // log.debug("Resulting port: " + port);
            String currentWorkingUserOnThreadLocal = SecurityModuleUtils.getCurrentUserLogin();

            log.info("Subscriber creation process -> Currently working user is : {}", currentWorkingUserOnThreadLocal);

            User user = userPersistenceFacade.findByUserName(currentWorkingUserOnThreadLocal);

            boolean modemFree = false;
            if (modemFree) {
                for (final Campaign campaign : campaignFacade.findAllActive(subscription.getService(), false)) {
                    if (campaign.isAvailableOnCreation()) {
                        selectedCampaign = campaign;
                        break;
                    }
                }
            } else {
                for (final Campaign campaign : campaignFacade.findAllActive(subscription.getService(), true)) {
                    if (!campaign.isActivateOnPayment()) {
                        selectedCampaign = campaign;
                        break;
                    }
                }
            }

            OperationsEngine operationsEngine = engineFactory.getOperationsEngine(subscription.getService().getProvider());
            operationsEngine.createSubscriptionSpring(
                    selectedSubscriber,
                    subscription,
                    subscriptionCreationVM.getServiceId(),
                    selectedMiniPop,
                    isUseStockEquipment,
                    selectedEquipment,
                    notificationSettings,
                    0D,
                    selectedCampaign,
                    user,
                    subscriptionCreationVM.getZoneId(),
                    subscriptionCreationVM.getServiceTypeId(),
                    subscriptionCreationVM.getSelectedReseller()
            );
            log.debug("finished");


            systemLogger.success(SystemEvent.SUBSCRIPTION_CREATED, subscription,
                    "Subscription created"
            );
            try {
                queueManager.addToNotificationsQueue(BillingEvent.SUBSCRIPTION_ADDED, subscription.getId(), null);
                systemLogger.success(SystemEvent.NOTIFICATION_SCHEDULED, subscription,
                        String.format("Spring Event=" + BillingEvent.SUBSCRIPTION_ADDED)
                );
            } catch (Exception ex) {
                log.error("Spring Cannot add to notification queue: ", ex);
                systemLogger.error(SystemEvent.NOTIFICATION_SCHEDULED, subscription, ex.getCause().getMessage());
            }

        } catch (DuplicateAgreementException ex) {
            log.error("Cannot create subscription: ", ex);
            throw new BadRequestAlertException("Agreement already exists!");
        } catch (NoFreePortLeftException ex) {
            log.error("Cannot create subscription: ", ex);
            throw new BadRequestAlertException("Minipop has no free ports left!");
        } catch (PortAlreadyReservedException ex) {
            String msg = String.format("Port %d already reserved or illegal", selectedMiniPop.getPreferredPort());
            log.error("Cannot create subscription: ", ex);
            throw new BadRequestAlertException(msg);
        } catch (Exception ex) {
            log.error("Cannot create subscription: ", ex);
            systemLogger.error(SystemEvent.SUBSCRIPTION_CREATED, subscription, ex.getCause().getMessage());
            throw new BadRequestAlertException("Cannot create subscription");
        }

        return subscription.getId();

    }

//    public SubscriptionDTO save(SubscriptionDTO subscriptionDTO) throws BadRequestAlertException {
//        log.debug("Request for saving Subscription : {}", subscriptionDTO);
//        Subscription subscription = subscriptionMapper.toEntity(subscriptionDTO);
//
//        SubscriptionDetails subscriptionDetails = subscription.getDetails();
//
//
//        try {
//            subscriptionPersistenceFacade.save(subscription);
//        } catch (Exception e) {
//            log.debug("Exception on saving subscription and details " + e);
//        }
//
//        return subscriptionMapper.toDto(subscriptionPersistenceFacade.findForceRefresh(subscription.getId()));
//    }
    public SubscriptionDTO update(SubscriptionDTO subscriptionDTO) {
        log.debug("Request for updating Subscription : {}", subscriptionDTO);
        Subscription subscription = subscriptionPersistenceFacade.find(subscriptionDTO.getId());

//        subscription = subscriptionPersistenceFacade.updateIt(subscription);

        return subscriptionMapper.toDto(subscription);
    }

    public void delete(Long id) {
        log.debug("Request to delete Subscription: {} ", id);
        Subscription subscription = subscriptionPersistenceFacade.find(id);
//        subscriptionPersistenceFacade.removeIt(subscription);
    }

//    public List<SubscriptionDTO> findAllByCustomCriteria(String name, String subscriptionIndex){
//        log.debug("Request to get all Subscription by custom criteria name: {} and SubscriptionIndex: {} ",name, subscriptionIndex);
//        return subscriptionPersistenceFacade.findAllByCustomCriteria(name, subscriptionIndex)
//                .stream()
//                .map(SubscriptionMapper::toDto).collect(Collectors.toList());
//
//    }
    public void prolongPrepaidSubscriptionOneTime(Long subscriptionId) {
        log.debug("prolongPrepaidSubscriptionOneTime method starts for subscription id {}", subscriptionId);
        Subscription subscription = subscriptionPersistenceFacade.find(subscriptionId);
        if (subscription == null)
            throw new CustomerOperationException("can not prolong, not find subscription id = " + subscriptionId);

        try {
            if (subscription.getStatus() == SubscriptionStatus.INITIAL || subscription.getStatus() == SubscriptionStatus.ACTIVE) {
                if (!systemLogger.checkOneTimeProlongationIsApplied(subscription.getAgreement())) {
                    if (subscription.getStatus() == SubscriptionStatus.INITIAL) {
                        engineFactory.getOperationsEngine(subscription).activatePrepaid(subscription);
                    } else if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
                        engineFactory.getOperationsEngine(subscription).prolongPrepaid(subscription);
                    }
                    log.debug("Prolongation process finished, start to provisioning for subscription {}", subscriptionId);
                    externalSystemService.reprovision(subscription, "Provisioning after prolong for one-time");
                    log.info("Prolong for one-time process completed successfully for subscription {}", subscriptionId);
                    systemLogger.success(SystemEvent.SUBSCRIPTION_PROLONGED_FOR_ONE_TIME, subscription, "subscription prolonged for one time");
                } else {
                    log.error("One time prolongation was used for this subscription "+subscriptionId);
                    systemLogger.error(SystemEvent.SUBSCRIPTION_PROLONGED_FOR_ONE_TIME, subscription, "One time prolongation was used for this subscription");
                    throw new CustomerOperationException("One time prolongation was used for this subscription "+subscriptionId);
              }
            } else {
                log.error("You can only apply prolongation to INITIAL and ACTIVE subscriptions, current subscription id: "+subscriptionId);
                systemLogger.error(SystemEvent.SUBSCRIPTION_PROLONGED_FOR_ONE_TIME, subscription, "You can only apply prolongation to INITIAL and ACTIVE subscriptions");
                throw new CustomerOperationException("You can only apply prolongation to INITIAL and ACTIVE subscriptions, current subscription id: "+subscriptionId);
            }
        } catch (Exception e) {
            log.error("Error occurs while prolong subscription for one time", e);
            throw new CustomerOperationException(e.getMessage(), e);
        }
    }




    /**
     *  Creation new Subscription
     * */
    public Long saveNewSubscription(SubscriptionCreationVM subscriptionCreationVM) throws BadRequestAlertException {
        log.debug("Request for saving new Subscription : {}", subscriptionCreationVM);


        String currentWorkingUserOnThreadLocal = SecurityModuleUtils.getCurrentUserLogin();
        log.info("Subscription creation process -> Currently working user is : {}", currentWorkingUserOnThreadLocal);
        if(currentWorkingUserOnThreadLocal==null) throw new BadRequestAlertException("Security User not found on Thread Local. May be token is not valid or empty");
        User user = userPersistenceFacade.findByUserName(currentWorkingUserOnThreadLocal);

        Subscriber selectedSubscriber = subscriberMapper.toEntity(subscriptionCreationVM.getSelectedSubscriber());
        Subscription subscription = subscriptionMapper.creationDTOtoEntity(subscriptionCreationVM.getSubscriptionDto());

        //daha yaxshi olar ki, bu checkler UI terefde edilsin, lazimsiz api requestlerin qarshisini alaq
        if (selectedSubscriber == null) {
            log.debug("Cannot create subscription. Subscriber not selected");
            throw new BadRequestAlertException("Subscriber must be selected. There is no subscriber selected");
        }

        if(subscription.getAgreement() == null){
            log.debug("Cannot create subscription. Agreement must be provided.");
            throw new BadRequestAlertException("Agreement must be provided. There is no agreement provided");
        }

        if (subscriptionCreationVM.getServiceId() == null) {
            log.debug("Cannot create subscription. Service not selected");
            throw new BadRequestAlertException("Service must be selected. There is no service selected");
        }

        Service service = servicePersistenceFacade.find(subscriptionCreationVM.getServiceId());

        OperationsEngine operationsEngine = engineFactory.getOperationsEngine(service.getProvider());
        subscription = operationsEngine.createSubscriptionSpring(subscriptionCreationVM,
                selectedSubscriber, subscription, service, user);

        return subscription.getId();
//        return subscriptionMapper.toDto(subscriptionPersistenceFacade.findForceRefresh(subscription.getId()));
    }


    public SubscriptionDTO getSubscription(Long id){
        try {
            Subscription subscription = subscriptionPersistenceFacade.find(id);
            SubscriberDetails details = subscriberDetailsPeristenceFacade.find(subscription.getSubscriber().getDetails().getId());

            SubscriptionDTO subscriptionDTO = subscriptionMapper.toDto(subscription);
            SubscriberDetailsSmallDTO subscriberDetailsSmallDTO = subscriberSmallDetailsMapper.toDto(details);
            subscriptionDTO.setSubscriberDetails(subscriberDetailsSmallDTO);

            ServiceProperty property = null;
            if (subscription.getService() != null &&
                    subscription.getSettingByType(ServiceSettingType.ZONE) != null &&
                    subscription.getSettingByType(ServiceSettingType.SERVICE_TYPE) != null) {
                property = servicePropertyPersistenceFacade.find(
                        subscription.getService(),
                        subscription.getSettingByType(ServiceSettingType.ZONE));
                subscriptionDTO.setServiceType(
                        settingService.getNameFromId(subscription.getSettingByType(ServiceSettingType.SERVICE_TYPE), subscription));
            }
            subscriptionDTO.setServiceProperty(servicePropertyMapper.toDto(property));
            return subscriptionDTO;
        } catch(Exception ex){
            log.error(String.format("Error occurs when getting subscription id: %s, msg: %s", id, ex.getMessage()),ex);
            throw new CustomerOperationException(ex.getMessage(), ex);
        }
    }

    public SubscriptionDTO getSubscription(String agreement){
//        Long id = subscriptionPersistenceFacade.findSubscriptionIdByAgreement(agreement);
//        return getSubscription(id);

        Optional<Long> id = Optional.ofNullable(subscriptionPersistenceFacade.findSubscriptionIdByAgreement(agreement));
        return getSubscription(id.orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found!")));
    }


    public void updateSubscriptionEcare(Long subscriptionId, Boolean isAvailableOnEcare){
        log.info("~~~Updating eCare for subscription with id: {}", subscriptionId);
        log.info("~~~"+isAvailableOnEcare+"~~~");
        Subscription subscription = subscriptionPersistenceFacade.find(subscriptionId);
        log.info("OLD CONDITION: " + subscription.getDetails().isAvailableEcare());
        subscription.getDetails().setAvailableEcare(isAvailableOnEcare);
        log.info("NEW CONDITION: " + subscription.getDetails().isAvailableEcare());
        subscriptionPersistenceFacade.update(subscription);

    }


    public String generateSubscriptionAgreementByServiceId(Long serviceId){

        Service service = null;
        String agreement = null;
        String identifier = null;

        if(serviceId == null){
            throw new BadRequestAlertException("Service cannot be null");
        }

        service = servicePersistenceFacade.find(serviceId);


        if(service == null){
            throw new BadRequestAlertException("Cannot find service with id: " + serviceId);
        }


        log.debug("~~~Service for provider: {}", service.getProvider());

        if(service.getProvider().getId() == Providers.QUTUNARHOME.getId()){
            ServiceProvider serviceProvider = serviceProviderPersistenceFacade.find(454100);

            identifier = String.valueOf(agreementGenerator.generate(serviceProvider).toString());
            log.debug("~~~Generated identifier: {}", identifier);
            return identifier;
        }else if(service.getProvider().getId() != Providers.CITYNET.getId()
                && service.getProvider().getId() != Providers.QUTU.getId()
                && service.getProvider().getId() != Providers.BBTV_BAKU.getId()
                && service.getProvider().getId() != Providers.AZERTELECOMPOST.getId()){

            agreement = String.valueOf(agreementGenerator.generate(service.getProvider()));

            log.debug("~~~Generated agreement: " + agreement);

            if(agreement == null){
                throw new BadRequestAlertException("Agreement cannot be generated...");
            }


        }

        return agreement;

    }



}
