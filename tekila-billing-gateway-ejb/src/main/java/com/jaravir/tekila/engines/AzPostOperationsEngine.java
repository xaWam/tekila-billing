package com.jaravir.tekila.engines;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.base.model.BillingModelPersistenceFacade;
import com.jaravir.tekila.base.persistence.manager.BillingSettingsManager;
import com.jaravir.tekila.base.persistence.manager.Register;
import com.jaravir.tekila.equip.EquipmentPersistenceFacade;
import com.jaravir.tekila.module.accounting.InvoiceState;
import com.jaravir.tekila.module.accounting.entity.*;
import com.jaravir.tekila.module.accounting.manager.ChargePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.InvoicePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.TransactionPersistenceFacade;
import com.jaravir.tekila.module.campaign.*;
import com.jaravir.tekila.module.event.BillingEvent;
import com.jaravir.tekila.module.service.*;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.service.entity.ServiceSetting;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.service.model.BillingPrinciple;
import com.jaravir.tekila.module.service.persistence.manager.NotificationSettingPersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.ServicePersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.ServiceSettingPersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.VASPersistenceFacade;
import com.jaravir.tekila.module.store.RangePersistenceFacade;
import com.jaravir.tekila.module.store.equip.Equipment;
import com.jaravir.tekila.module.store.equip.EquipmentStatus;
import com.jaravir.tekila.module.store.ip.IpAddressResult;
import com.jaravir.tekila.module.store.ip.persistence.IpAddress;
import com.jaravir.tekila.module.store.ip.persistence.IpAddressRange;
import com.jaravir.tekila.module.store.nas.Nas;
import com.jaravir.tekila.module.subscription.persistence.entity.*;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import com.jaravir.tekila.provision.broadband.devices.Port;
import com.jaravir.tekila.provision.broadband.devices.exception.NoFreePortLeftException;
import com.jaravir.tekila.provision.broadband.devices.exception.PortAlreadyReservedException;
import com.jaravir.tekila.provision.broadband.devices.manager.MiniPopPersistenceFacade;
import com.jaravir.tekila.provision.broadband.devices.manager.Providers;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import com.jaravir.tekila.provision.exception.ProvisioningException;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spring.controller.vm.SubscriptionCreationVM;

import javax.ejb.*;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * Created by kmaharov on 18.05.2017.
 */
@Stateless(name = "AzPostOperationsEngine", mappedName = "AzPostOperationsEngine")
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class AzPostOperationsEngine implements OperationsEngine {
    private final static Logger log = LoggerFactory.getLogger(AzPostOperationsEngine.class);

    @javax.annotation.Resource
    private SessionContext ctx;
    @EJB
    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;
    @EJB
    private ServicePersistenceFacade serviceFacade;
    @EJB
    private ServiceSettingPersistenceFacade serviceSettingFacade;
    @EJB
    private SystemLogger systemLogger;
    @EJB
    private MiniPopPersistenceFacade miniPopFacade;
    @EJB
    private CampaignJoinerBean campaignJoinerBean;
    @EJB
    private CampaignRegisterPersistenceFacade campaignRegisterFacade;
    @EJB
    private InvoicePersistenceFacade invoiceFacade;
    @EJB
    private ChargePersistenceFacade chargeFacade;
    @EJB
    private TransactionPersistenceFacade transFacade;
    @EJB
    private BillingSettingsManager billSettings;
    @EJB
    private BillingModelPersistenceFacade modelFacade;
    @EJB
    private EquipmentPersistenceFacade equipmentFacade;
    @EJB
    private NotificationSettingPersistenceFacade notifSettingFacade;
    @EJB
    private EngineFactory provisioningFactory;
    @EJB
    private CommonOperationsEngine commonOperationsEngine;
    @EJB
    private VASPersistenceFacade vasPersistenceFacade;
    @EJB
    private EngineFactory engineFactory;
    @EJB
    private BillingSettingsManager billingSettings;
    @EJB
    private UserPersistenceFacade userFacade;
    @EJB
    private Register register;
    @EJB
    private RangePersistenceFacade rangeFacade;



    @Override
    public Subscription createSubscriptionSpring(SubscriptionCreationVM subscriptionCreationVM, Subscriber selectedSubscriber, Subscription subscription, Service service, User user) {
        log.info("AzPost createSubscriptionSpring method starts..");

        //...

        //parametrler texmini qeyd edilib, providerlara uygunlasdirilmalidir..
        createSubscription(selectedSubscriber,
                subscription,
                String.valueOf(subscriptionCreationVM.getServiceId()),
                null,
                false,
                null,
                null,
                0D,
                null,
                null,
                user);
        //....

        return subscription;
    }


    @Override
    public void createSubscription(Subscriber selectedSubscriber,
                                   Subscription subscription,
                                   String serviceId,
                                   MiniPop miniPop,
                                   boolean isUseStockEquipment,
                                   Equipment equipment,
                                   List<NotificationSettingRow> notificationSettings,
                                   double installationFeeRate,
                                   Campaign selectedCampaign,
                                   HttpSession session,
                                   User user,String ... additionalProperties) {

        Long userId = session != null ? (Long) session.getAttribute("userID") : user.getId();

        //ctx.setRollbackOnly();
        subscriptionPersistenceFacade.save(subscription);
        Balance balance = new Balance();
        balance.setRealBalance(0L);
        balance.setPromoBalance(0L);

        if (selectedSubscriber.getLifeCycle() == SubscriberLifeCycleType.POSTPAID) {
            balance.setVirtualBalance(0);
        }

            subscription.setInstallationFeeInDouble(installationFeeRate);


        selectedSubscriber.getSubscriptions().add(subscription);
        subscription.setUser(user);
        subscription.setSubscriber(selectedSubscriber);
        subscription.setService(serviceFacade.find(Long.valueOf(serviceId)));
        subscription.setStatus(SubscriptionStatus.INITIAL);

        log.debug("the created subscription: " + subscription);

        subscription.setBalance(balance);
        subscription.setIdentifier(subscription.getAgreement());

        List<com.jaravir.tekila.module.service.entity.Resource> resList = subscription.getService().getResourceList();

        List<ServiceSetting> settings = subscription.getService().getSettings();
        subscription.copySettingsFromService(settings);


        log.debug("isUseStockEquipment: " + isUseStockEquipment);
        log.debug("equipment: " + equipment);


        if (isUseStockEquipment && equipment != null) {
            if (!subscription.setSettingByType(ServiceSettingType.TV_EQUIPMENT, equipment.getPartNumber())) {
                SubscriptionSetting ss = new SubscriptionSetting();
                ss.setProperties(serviceSettingFacade.find(subscription.getService().getProvider().getId(), ServiceType.TV, ServiceSettingType.TV_EQUIPMENT));
                ss.setValue(equipment.getPartNumber());
                subscription.addSetting(ss);
            }
            EquipmentStatus oldEquipmentStatus = equipment.getStatus();
            equipment.reserve();

            systemLogger.success(
                    SystemEvent.EQUIPMENT_RESERVED,
                    subscription, String.format("equipment.id=%d, status changed from %s to %s",
                            equipment.getId(), oldEquipmentStatus, equipment.getStatus())
            );
        }



//        if (subscription.getService().getServiceType() == ServiceType.BROADBAND) {
//            //MiniPop miniPop = miniPopFacade.find(miniPopId);
//            Port port = miniPopFacade.getAvailablePort(miniPop);
//
//            //if (port != null) return;
//            subscription.getSettingByType(ServiceSettingType.USERNAME).setValue(
//                    //"Ethernet0/0/5:4846-fbe9-b9f0" + DateTime.now().toString(DateTimeFormat.forPattern("yyyyMMddHHmmss")) +"@narhome");
//                    "Ethernet0/0/" + String.valueOf(port.getNumber()) + ":" + miniPop.getMac()
//            );
//            subscription.getSettingByType(ServiceSettingType.PASSWORD).setValue("-");
//            subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH).setValue(String.valueOf(miniPop.getId()));
//            subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH_PORT).setValue(String.valueOf(port.getNumber()));
//
//            log.debug("USERNAME: " + subscription.getSettingByType(ServiceSettingType.USERNAME));
//
//        }
        //log.debug("service: " + subscription.getService() + ", resource list: " + resList);
        if (resList.size() > 0) {
            for (com.jaravir.tekila.module.service.entity.Resource res : resList) {
                SubscriptionResource subResource = new SubscriptionResource(res);
                /*if (subscription.getService().getServiceType().equals(ServiceType.BROADBAND)) {
                 subResource.getBucketByType(ResourceBucketType.INTERNET_SWITCH).setCapacity("test switch");
                 subResource.getBucketByType(ResourceBucketType.INTERNET_SWITCH_PORT).setCapacity("test port");
                 subResource.getBucketByType(ResourceBucketType.INTERNET_USERNAME).setCapacity("test switch:testport:" + DateTime.now().toString(DateTimeFormat.forPattern("yyyyMMddHHmmss")) +"@narhome");
                 }*/
                subscription.addResource(subResource);
            }
        }
        log.debug("azazaz");
        List<CampaignRegister> registerList = null;
//        log.info(String.format("create: agreement = %s, selectedCampaign id=%d", subscription.getAgreement(),
//                selectedCampaign != null ? selectedCampaign.getId() : 0));

//        if (selectedCampaign != null) {
//            registerList = campaignJoinerBean.tryAddToCampaigns(subscription, selectedCampaign, true, false, null);
//            log.debug(String.format("create: agreement = %s, registerList=%s", subscription.getAgreement(),
//                    registerList));
//        } else {
//            campaignJoinerBean.tryAddToCampaigns(subscription);
//        }

        if (subscription.getService().getServiceType() == ServiceType.BROADBAND) {
            log.debug("<<<<<<<<<<<<<<< 2");
            //String bandwidth = serviceFacade.getBucketCapacity(subscription);
//            Long bandwidth = campaignRegisterFacade.getBonusAmount(subscription, CampaignTarget.RESOURCE_INTERNET_BANDWIDTH);

//            log.info(String.format("create: subscription agreement=%s, found resource campaign, new bandwidth=%s", subscription.getAgreement(), bandwidth));

            log.debug("buckrt capasity" +ResourceBucketType.INTERNET_DOWN +" - "+ subscription.getSpeed());

//            if (bandwidth != null) {
//                log.debug("inside condition ");
            subscription.setBucketCapacityByType(ResourceBucketType.INTERNET_DOWN, String.valueOf(subscription.getSpeed()));
            subscription.setBucketCapacityByType(ResourceBucketType.INTERNET_UP, String.valueOf(subscription.getSpeed()));
//            }
        }
        log.debug("<<<<<<<<<<<<<<  3");
        long internetSpeed = Long.valueOf(subscription.sgetBucketCapacityByType(ResourceBucketType.INTERNET_DOWN));
        log.debug("create: created sub: " + subscription);
        log.debug("Internet download speed "+subscription.sgetBucketCapacityByType(ResourceBucketType.INTERNET_DOWN));

        int totalCharge = 0;

        log.debug(" selectedSubscriber.getLifeCycle() == SubscriberLifeCycleType.PREPAID " + selectedSubscriber.getLifeCycle() +"  -  "+ SubscriberLifeCycleType.PREPAID);



            Long rate = null;

            if (rate == null) {
                rate = subscription.getServiceFeeRate()*internetSpeed;
            }

            boolean isPartial = false;
            long partial_rate;
            long creation_date = DateTime.now().getDayOfMonth();
            long max_day = DateTime.now().plusMonths(1).withDayOfMonth(1).minusDays(1).getDayOfMonth();
            long diff_date = max_day - creation_date;
            if (diff_date == 0){
                diff_date = 1;
            }

            log.debug("day of month "+creation_date);
            log.debug(
                    "max date "+max_day
            );
            if (creation_date > 1) {
                isPartial = true;
                partial_rate = rate / max_day;

                partial_rate = partial_rate * diff_date;
            }else{
                isPartial = false;
                partial_rate = rate;
            }


        log.debug("TAX Category " +selectedSubscriber.getTaxCategory());
        if (selectedSubscriber.getTaxCategory().getVATRate() == 0){
            log.debug("Tax category selected 0    => "+partial_rate*1.18);
            partial_rate = (long) (partial_rate*1.18);
        }else{
            log.debug("Tax category selected 18000    => "+partial_rate);

        }


            //charge for service fee if > 0
            if (partial_rate >= 0) {
                log.debug("<<<<<<<<<<<<< partial rate "+partial_rate);
                partial_rate = subscription.rerate(partial_rate);
                Charge servFeeCharge = new Charge();
                servFeeCharge.setService(subscription.getService());

                servFeeCharge.setAmount(partial_rate);
                servFeeCharge.setSubscriber(subscription.getSubscriber());
                servFeeCharge.setUser_id(userId);
                servFeeCharge.setSubscription(subscription);
                if (isPartial){
                    servFeeCharge.setDsc("Charge for service fee for "+diff_date);
                }else {
                    servFeeCharge.setDsc("Charge for service fee");
                }
                servFeeCharge.setDatetime(DateTime.now());
                log.debug("<<<<<<<<<<<<<< charge "+servFeeCharge.toString());
                chargeFacade.save(servFeeCharge);
                //balance.debitReal(rate * 100000);
                Transaction transDebitServiceFee = new Transaction(
                        TransactionType.DEBIT,
                        subscription,
                        partial_rate,
                        "Charged for service fee"
                );
                transDebitServiceFee.execute();
                transFacade.save(transDebitServiceFee);

                servFeeCharge.setTransaction(transDebitServiceFee);


                systemLogger.success(
                        SystemEvent.CHARGE,
                        subscription,
                        transDebitServiceFee,
                        String.format("Charge id=%d for service, amount=%s",
                                servFeeCharge.getId(), servFeeCharge.getAmountForView()
                        )
                );
                /*if (selectedSubscriber.getLifeCycle() == SubscriberLifeCycleType.POSTPAID)
                 totalCharge += servFeeCharge.getAmount();    */
            }

                log.debug("<<<<<<<<<<<<<<<<<<< 3");
//            if (billSettings.getSettings().getPrepaidlifeCycleLength() == 30) {

                subscription.setBilledUpToDate(DateTime.now().plusMonths(1).withDayOfMonth(4));

//            } else {
//                subscription.setBilledUpToDate(DateTime.now().plusDays(
//                        billSettings.getSettings().getPrepaidlifeCycleLength()
//                ));
//            }









        if (isUseStockEquipment && equipment != null) {
            equipmentFacade.update(equipment);
        }

        if (notificationSettings != null && !notificationSettings.isEmpty()) {
            NotificationSetting setting = null;

            for (NotificationSettingRow row : notificationSettings) {
                if (row.getSelectedChannelList() != null && !row.getSelectedChannelList().isEmpty()) {
                    setting = notifSettingFacade.find(row.getEvent(), row.getSelectedChannelListAsChannels());

                    subscription.addNotification(setting);

                    systemLogger.success(SystemEvent.NOTIFICATION_ADDED, subscription,
                            String.format("Notification setting id=%d", setting.getId())
                    );
                    setting = null;
                }
            }
        }

//        initService(subscription);
    }

    @Override
    public Subscription createSubscriptionSpring(Subscriber selectedSubscriber, Subscription subscription, Long serviceId, MiniPop miniPop, boolean isUseStockEquipment, Equipment equipment, List<NotificationSettingRow> notificationSettings, double installationFee, Campaign selectedCampaign, User user, String... additionalProperties) throws NoFreePortLeftException, PortAlreadyReservedException {
        return subscription;
    }

    // upCharge and downCharge are not working
    @Override
    public Subscription changeService(Subscription subscription, Service targetService, boolean upCharge, boolean downCharge) {
        String msg = String.format("subscription id=%d, targetService id=%d, currentService id=%d", subscription.getId(),
                targetService.getId(), subscription.getService().getId());
        log.info(msg);

        SystemEvent event = SystemEvent.SUBSCRIPTION_CHANGE_SERVICE;
        subscription = subscriptionPersistenceFacade.update(subscription);
        subscription.setService(null);
        subscription.setService(targetService);
        subscription.getResources().clear();

        if (targetService.getResourceList() != null && targetService.getResourceList().size() > 0) {
            if (targetService.getServiceType() == ServiceType.BROADBAND) {
                for (com.jaravir.tekila.module.service.entity.Resource res : targetService.getResourceList()) {
                    SubscriptionResource subResource = new SubscriptionResource(res);
                    /*if (subscription.getService().getServiceType().equals(ServiceType.BROADBAND)) {
                     subResource.getBucketByType(ResourceBucketType.INTERNET_SWITCH).setCapacity("test switch");
                     subResource.getBucketByType(ResourceBucketType.INTERNET_SWITCH_PORT).setCapacity("test port");
                     subResource.getBucketByType(ResourceBucketType.INTERNET_USERNAME).setCapacity("test switch:testport:" + DateTime.now().toString(DateTimeFormat.forPattern("yyyyMMddHHmmss")) +"@narhome");
                     }*/
                    subscription.addResource(subResource);
                }
            }
        }
        log.debug(
                String.format("changeService: after change service subscription id=%d, agreement=%s changed to service id=%d, name=%s",
                        subscription.getId(), subscription.getAgreement(), subscription.getService().getId(), subscription.getService().getName()));

        try {
            ProvisioningEngine provisioner = provisioningFactory.getProvisioningEngine(subscription);

            provisioner.changeService(subscription, targetService);
            log.info("changeService: successfully changed service, " + msg);
            systemLogger.success(event, subscription, msg);
        } catch (ProvisionerNotFoundException | ProvisioningException ex) {
            ctx.setRollbackOnly();
            log.error("changeService: cannot change service, " + msg, ex);
            systemLogger.error(event, subscription, msg);
        }

        log.debug(
                String.format("changeService: subscription id=%d, agreement=%s changed to service id=%d, name=%s",
                        subscription.getId(), subscription.getAgreement(), subscription.getService().getId(), subscription.getService().getName()));
        return subscription;
    }



    @Override
    public Subscription addVAS(VASCreationParams params) {

        Subscription subscription = params.subscription;
        subscription = subscriptionPersistenceFacade.update(subscription);
        DateTime startDate = params.startDate;
        ValueAddedService vas = params.vas;
        DateTime expiresDate = params.expiresDate;
        IpAddress ipAddress = params.ipAddress;
        long vasFee = params.vasFee;
        double vasCount = params.count;

//        if (ipAddress != null) {
//            User user = userFacade.findByUserName(ctx.getCallerPrincipal().getName());
//            if (startDate == null) {
//                startDate = DateTime.now();
//            }
//
//            if (vas.getCount() > 0) {
//                expiresDate = DateTime.now().plusMonths((int) vas.getCount());
//            }
//            if (expiresDate == null) {
//                expiresDate = register.getDefaultExpirationDate();
//            }
//
//            try {
//                    subscription = addVASWithResources(subscription, vas, ipAddress, user, startDate, expiresDate, getVasCount(vasCount));
//
//            } catch (Exception ex) {
//                log.error(String.format("Cannot add vas id=%d to subscription id=%d", vas.getId(), subscription.getId()), ex);
//                ctx.setRollbackOnly();
//                systemLogger.error(SystemEvent.CHARGE, subscription, "Charge for vas=" + vas.getName() + " failed");
//            }
//            return subscription;
//        } else {
        log.debug("addVASMain");

        User user = userFacade.findByUserName(ctx.getCallerPrincipal().getName());
        if (startDate == null) {
            startDate = DateTime.now();
        }

        try {
                if (vasFee == 0){
                    vasFee = vas.getPrice();
                }
            if (vas.getCode().getType() == ValueAddedServiceType.PERIODIC_DYNAMIC ){

                log.debug("addVASWithoutResources");
                subscription = addVASWithoutResources(subscription, vas, user, startDate, expiresDate, getVasCount(vasCount), vasFee);

            } else {
                log.debug("vasFee" + vasFee);
                log.debug("getVasCount(vasCount) "+getVasCount(vasCount));
                long rate = (long)(vasFee * getVasCount(vasCount));
                invoiceFacade.addVasCharge(subscription, rate, vas, user.getId(), String.format("Charge for vas=%s count=%s", vas.getName(), getVasCount(vasCount)));

            }

//            queueManager.sendVASNotification(BillingEvent.VAS_ADDED, subscription, vas);

        } catch (Exception ex) {
            log.error(String.format("Cannot add vas id=%d to subscription id=%d", vas.getId(), subscription.getId()), ex);
            ctx.setRollbackOnly();
            systemLogger.error(SystemEvent.CHARGE, subscription, "Charge for vas=" + vas.getName() + " failed");
        }
        return subscription;
//    }


    }

    private double getVasCount(double vasCount) {
        if (vasCount == 0) {
            return 1;
        }
        return vasCount;
    }




    private Subscription addVASWithoutResources(
            Subscription subscription,
            ValueAddedService vas,
            User user,
            DateTime startDate,
            DateTime expireDate,
            double vasCount,
            long vasFee) throws ProvisionerNotFoundException {
        try {

            if (vas.getMaxNumber() >= 0 && subscription.countVASById(vas.getId()) >= vas.getMaxNumber()) {
                return subscription;
            }

            SubscriptionVAS sbnVAS = new SubscriptionVAS();
            subscriptionPersistenceFacade.persist(sbnVAS);

            sbnVAS.setSubscription(subscription);

            if (startDate != null) {
                sbnVAS.setActiveFromDate(startDate);
            }

            if (expireDate != null) {
                sbnVAS.setExpirationDate(expireDate);
            }
            log.debug("inserting vas fee "+vasFee);
            sbnVAS.setVasStatus(1);
            sbnVAS.setStatus(SubscriptionStatus.ACTIVE);
            sbnVAS.setVas(vas);
            sbnVAS.setRemainCount(vas.getCount() - 1);
            sbnVAS.setCount(getVasCount(vasCount));
            sbnVAS.setServiceFeeRate(vasFee);
            subscription.addVAS(sbnVAS);

            //systemLogger.success(SystemEvent.CHARGE, subscription, String.format("Charge id=%d for vas=%s", charge.getId(), vas.getName()));
            log.debug("vas: "+vas);

            log.debug("VAS Price "+sbnVAS.getServiceFeeRate());
            long rate = (long)(vasFee * sbnVAS.getCount());
            invoiceFacade.addVasCharge(subscription, rate, vas, user.getId(), String.format("Charge for vas=%s count=%s", vas.getName(), getVasCount(vasCount)));

//            queueManager.sendVASNotification(BillingEvent.VAS_ADDED, subscription, vas);

        } catch (Exception ex) {
            log.error(String.format("Cannot add vas id=%d to subscription id=%d", vas.getId(), subscription.getId()), ex);
            ctx.setRollbackOnly();
            systemLogger.error(SystemEvent.CHARGE, subscription, "Charge for vas=" + vas.getName() + " failed");
        }
        return subscription;
    }





    private Subscription addVASWithResources(
            Subscription subscription,
            ValueAddedService vas,
            IpAddress ipAddress,
            User user,
            DateTime startDate,
            DateTime expireDate,
            int vasCount) throws ProvisionerNotFoundException {
        //obey "maximum allowable number" restriction in VAS
        try {

            Subscription oldSbn = subscription;

            if (vas.getMaxNumber() >= 0 && subscription.countVASById(vas.getId()) >= vas.getMaxNumber()) {
                return subscription;
            }

            for (SubscriptionVAS sbnVAS : subscription.getVasList()) {
                if (sbnVAS.getVas().getId() == vas.getId() && vas.getMaxNumber() == 1) {
                    return subscription;
                }
            }

            SubscriptionVAS sbnVAS = new SubscriptionVAS();
            subscriptionPersistenceFacade.persist(sbnVAS);

            sbnVAS.setSubscription(subscription);

            if (startDate != null) {
                sbnVAS.setActiveFromDate(startDate);
            }

            if (expireDate != null) {
                sbnVAS.setExpirationDate(expireDate);
            }

            sbnVAS.setVasStatus(1);
            sbnVAS.setVas(vas);
            sbnVAS.setRemainCount(vas.getCount() - 1);
            sbnVAS.setCount(getVasCount(vasCount));

            SubscriptionResource resource = new SubscriptionResource();

            if (vas.getCode().getType() == ValueAddedServiceType.PERIODIC_STATIC) { //static ip
                subscriptionPersistenceFacade.persist(resource);

                ResourceBucketType bucketType = vas.getResource().getBucketList().get(0).getType();
                SubscriptionResourceBucket bucket = new SubscriptionResourceBucket();
                bucket.setType(bucketType);
                bucket.setLastUpdateDate();

                String message = null;
                IpAddressRange ipRange = null;

                if (bucketType == ResourceBucketType.INTERNET_IP_ADDRESS) { //static ip

                    if (ipAddress == null) {
                        Nas nas = subscriptionPersistenceFacade.findMinipop(subscription).getNas();
                        if (nas == null) {
                            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Nas is null, assign switch to Nas", "Nas is null, assign switch to Nas"));
                            return oldSbn;
                        }
                        IpAddressResult result = rangeFacade.findResultAndReserve(nas);
                        if (result == null) {
                            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "No IP range or full, create one", "No IP range or full, create one"));
                            return oldSbn;
                        }
                        ipAddress = result.getIpAddress();
                        ipRange = result.getIpAddressRange();
                        log.debug("Range: " + ipRange);
                        log.debug("IpAddress: " + ipAddress);

                    } else {
                        ipRange = rangeFacade.findIpRange(subscriptionPersistenceFacade.findMinipop(subscription).getNas(), ipAddress);

                        if (ipRange == null) {
                            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Cannot find IP range", "Cannot find IP range"));
                            return oldSbn;
                        }
                        log.debug("Range: " + ipRange);
                        log.debug("IpAddress: " + ipAddress);
                        ipRange = rangeFacade.update(ipRange);
                        ipRange.reserveAddress(ipAddress);
                    }

                    if (ipRange != null && !ipRange.getStart().equals(ipAddress) && !ipRange.getEnd().equals(ipAddress)) {
                        subscriptionPersistenceFacade.persist(ipAddress);
                        log.debug("Ipaddress persisted:" + ipAddress);
                    }

                    message = String.format("IP address=%s reserved from subscription id=%d, vas id=%d", ipAddress.getAddressAsString(), subscription.getId(), vas.getId());
                    log.debug("addVAS: " + message);
                    systemLogger.success(SystemEvent.IP_ADDRESS_RESERVED, subscription, message);

                    bucket.setCapacity(ipAddress.getAddressAsString());
                }

                resource.addBucket(bucket);
                sbnVAS.setResource(resource);

                //systemLogger.success(SystemEvent.CHARGE, subscription, String.format("Charge id=%d for vas=%s", charge.getId(), vas.getName()));
                subscription.addVAS(sbnVAS);

                if (vas.isProvisioned()) {
                    ProvisioningEngine provisioner = engineFactory.getProvisioningEngine(subscription);

                    message = String.format("OPEN VAS returned subscription id=%d, vas id=%d, status=ACTIVE",
                            subscription.getId(), vas.getId());

                    if (provisioner.openVAS(subscription, vas)) {
                        systemLogger.success(SystemEvent.VAS_STATUS_ACTIVE, subscription, message);
                    } else {
                        systemLogger.error(SystemEvent.VAS_STATUS_ACTIVE, subscription, message);
                        //ctx.setRollbackOnly();
                    }
                } else {
                    sbnVAS.setStatus(SubscriptionStatus.ACTIVE);
                }
            }

            if (!vas.isDoPartialCharge() || !doPartialChargeOfVAS(subscription, vas, user, getVasCount(vasCount))) {

                log.debug(" VAS details"+vas.toString());
                long rate = 0;
                if (vas.getCode().getType().toString().equals("PERIODIC_DYNAMIC")){
                    log.debug("Dymanic starts");
                    rate = (long)(sbnVAS.getServiceFeeRate() * sbnVAS.getCount());
                    log.debug("PERIODIC_DYMANIC RATE: "+sbnVAS.getServiceFeeRate());
                }


//                Charge charge = chargeFacade.chargeForVAS(subscription, rate, "Charge for " + vas.getName(), user.getId(), vas);
//                Invoice invoice = invoiceFacade.findOpenBySubscriberForCharging(subscription.getSubscriber().getId());

                invoiceFacade.addVasCharge(subscription, rate, vas, user.getId(), String.format("Charge for vas=%s count=%s", vas.getName(), getVasCount(vasCount)));
            }

//            queueManager.sendVASNotification(BillingEvent.VAS_ADDED, subscription, vas);

        } catch (Exception ex) {
            log.error(String.format("Cannot add vas id=%d to subscription id=%d", vas.getId(), subscription.getId()), ex);
            ctx.setRollbackOnly();
            systemLogger.error(SystemEvent.CHARGE, subscription, "Charge for vas=" + vas.getName() + " failed");
        }
        return subscription;
    }


    private boolean doPartialChargeOfVAS(Subscription subscription, ValueAddedService vas, User user, double vasCount) {
        log.debug("Charging partially");
        long days;
        if (subscription.getBilledUpToDate() == null) days = 0;
        else
            days = Days.daysBetween(DateTime.now(), subscription.getBilledUpToDate()).getDays();

        log.debug("DateTime.now():" + DateTime.now());
        log.debug("subscription.getBilledUpToDate():" + subscription.getBilledUpToDate());
        log.debug("Days: " + days);
        if (days <= 0 || days >= 30) {
            log.debug("Cannot charge partially. Days is " + days);
            return false;
        }

        long rate = (long)(vas.getPrice() / 30 * days * getVasCount(vasCount));
        rate = rate == rate / 1000 * 1000 ? rate : rate / 1000 * 1000 + 1000;

        log.debug("Rate: " + rate);

        log.debug(String.format("addVAS partial: vas id=%d, profile=%s, rate=%d, profile=%s, days=%s, count=%s", vas.getId(), days, getVasCount(vasCount)));
        Invoice invoice = invoiceFacade.findOpenBySubscriberForCharging(subscription.getSubscriber().getId());
        invoiceFacade.addVasChargeToInvoice(invoice, subscription, rate, vas, user.getId(), String.format("Partial Charge for vas=%s count=%s days=%s", vas.getName(), getVasCount(vasCount), days));

        return true;
    }







    @Override
    public Subscription removeVAS(Subscription subscription, SubscriptionVAS vas) {
        return commonOperationsEngine.removeVAS(subscription, vas);
    }

    @Override
    public Subscription removeVAS(Subscription subscription, ValueAddedService vasID) {
        return commonOperationsEngine.removeVAS(subscription, vasID);
    }

    @Override
    public Subscription editVAS(VasEditParams params) {
        return commonOperationsEngine.editVAS(params);
    }

    @Override
    public void activatePrepaid(Subscription subscription) {
        subscription = subscriptionPersistenceFacade.update(subscription);
        log.debug("ActivatePrepaid, agreement=" + subscription.getAgreement());

        if (subscription.getStatus() == SubscriptionStatus.INITIAL) {
            subscription.setActivationDate(DateTime.now());
        }

        DateTime expDate = (subscription.getStatus() == SubscriptionStatus.ACTIVE) ? subscription.getExpirationDate()
                : DateTime.now();

        if (billSettings.getSettings().getPrepaidlifeCycleLength() == 30) {
            subscription.setExpirationDate(expDate.plusMonths(1));
        } else {
            subscription.setExpirationDate(expDate.plusDays(billSettings.getSettings().getPrepaidlifeCycleLength()));
        }

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setLastStatusChangeDate(DateTime.now());
        }

        subscription.synchronizeExpiratioDates();
        subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDate().plusDays(
                subscription.getBillingModel().getGracePeriodInDays()
        ));

        DateTimeFormatter frm = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

        subscription.setBilledUpToDate(subscription.getExpirationDate());
        log.info(
                String.format("Subscription id=%d, agreement=%s, status=%s, biiledUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s successfully activated",
                        subscription.getId(),
                        subscription.getAgreement(),
                        subscription.getStatus(),
                        subscription.getBilledUpToDate() != null ? subscription.getBilledUpToDate().toString(frm) : null,
                        subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                        subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                ));
        systemLogger.success(
                SystemEvent.SUBSCRIPTION_STATUS_ACTIVE,
                subscription,
                String.format("status=%s, biiledUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s successfully activated",
                        subscription.getStatus(),
                        subscription.getBilledUpToDate() != null ? subscription.getBilledUpToDate().toString(frm) : null,
                        subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                        subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                ));
    }

    @Override
    public Subscription prolongPrepaid(Subscription subscription) {
        return null;
    }


    public   boolean activatePostpaid(Subscription subscription){
//        ProvisioningEngine provisioner = engineFactory.getProvisioningEngine(subscription);

//        if (provisioner.openService(subscription)) {
        if (billingSettings.getSettings().getPospaidLifeCycleLength() == 30) {
            subscription.setExpirationDate(DateTime.now().plusMonths(1));
        } else {
            subscription.setExpirationDate(DateTime.now().plusDays(
                    billingSettings.getSettings().getPospaidLifeCycleLength()
                    )
            );
        }

        subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDate().plusDays(
                billingSettings.getSettings().getPostpaidDefaultGracePeriod()
        ));
        subscription.synchronizeExpiratioDates();

        if (subscription.getStatus() == SubscriptionStatus.INITIAL) {
            subscription.setActivationDate(DateTime.now());
            subscriptionPersistenceFacade.chargePostpaidVirtuallyOnActivate(subscription);
        }

        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setLastStatusChangeDate(DateTime.now());
        subscriptionPersistenceFacade.update(subscription);
        return true;
//        } else {
//            return false;
//        }
    }











//    @Override
//    public boolean activatePostpaid(Subscription subscription) throws ProvisionerNotFoundException {
//        return commonOperationsEngine.activatePostpaid(subscription);
//    }

    @Override
    public Subscription changeStatus(Subscription subscription, SubscriptionStatus newStatus) {
//        return commonOperationsEngine.changeStatus(subscription, newStatus);




        log.debug("Changing Status for " + subscription.getAgreement() + " to " + newStatus);

        if (subscription == null || newStatus == null) {
            throw new IllegalArgumentException(String.format("changeStatus arguments cannot be null, provided newStatus=%s, subscription=%s", newStatus, subscription));
        }

        try {
            subscription = subscriptionPersistenceFacade.update(subscription);

            SystemEvent event = null;
//            ProvisioningEngine provisioner = engineFactory.getProvisioningEngine(subscription);

            if (subscription.getStatus().equals(SubscriptionStatus.SUSPENDED) &&
                    newStatus.equals(SubscriptionStatus.ACTIVE)) {
                List<ValueAddedService> suspensionVasList = vasPersistenceFacade.findSuspensionVasListBySubscription(subscription);
                if (suspensionVasList != null) {
                    for (final ValueAddedService vas : suspensionVasList) {
                        SubscriptionVAS sbnVas = subscription.getVASByServiceId(vas.getId());
                        if (sbnVas != null) {
                            subscription = engineFactory.getOperationsEngine(subscription).
                                    removeVAS(subscription, sbnVas);
                        }
                    }
                }
            }

            switch (newStatus) {
                case ACTIVE:
                    log.debug("In active case");
                    event = SystemEvent.SUBSCRIPTION_STATUS_ACTIVE;
//                    provisioner.openService(subscription);
                    break;
                case SUSPENDED:
                    event = SystemEvent.SUBSCRIPTION_STATUS_SUSPEND;
//                    provisioner.closeService(subscription);
                    break;
                case FINAL:
                    subscription = removeAllVasList(subscription);
                    subscription = closeOpenInvoices(subscription);
                    event = SystemEvent.SUBSCRIPTION_STATUS_FINAL;
//                    provisioner.closeService(subscription);
                    break;
                default:
                    event = SystemEvent.SUBSCRIPTION_STATUS_BLOCK;
                    break;
            }

            if (subscription.getStatus() == SubscriptionStatus.INITIAL
                    && newStatus == SubscriptionStatus.ACTIVE
                    && subscription.getService().getProvider().getId() != Providers.CITYNET.getId()) {
                log.debug("true condition in if");
                checkForTransitionFromInitialToActive(subscription);
            } else {
                log.debug("false condition in if");
                systemLogger.success(event, subscription, String.format("subscription id=%d changed status from %s",
                        subscription.getId(), subscription.getStatus()));
                subscription.setStatus(newStatus);
                subscription.setLastStatusChangeDate(DateTime.now());
            }
            String msg = String.format("event=%s, subscription id=%d", BillingEvent.STATUS_CHANGED, subscription.getId());

            log.debug("Status become: " + subscription.getStatus());

            try {
//                queueManager.sendStatusNotification(BillingEvent.STATUS_CHANGED, subscription);
                systemLogger.success(SystemEvent.NOTIFICATION_SCHEDULED, subscription, msg);
            } catch (Exception ex) {
                log.error("changeStatus: " + " Cannot send notification, " + msg, ex);
                systemLogger.error(SystemEvent.NOTIFICATION_SCHEDULED, subscription, msg);
            }
        } catch (Exception ex) {
            log.error(String.format("changeStatus: failed on subscription id=%d, newStatus=%s", subscription.getId(), newStatus), ex);
        }
        return subscription;




    }




    private Subscription removeAllVasList(Subscription subscription) {
        List<SubscriptionVAS> vasList = subscription.getVasList();
        if (vasList != null && !vasList.isEmpty()) {
            for (SubscriptionVAS vas : vasList) {
                if (vas.getVas() != null && !vas.getVas().isCredit()) {
                    subscription = engineFactory.getOperationsEngine(subscription).
                            removeVAS(subscription, vas);
                }
            }
        }
        return subscription;
    }






    private Subscription closeOpenInvoices(Subscription subscription) {
        log.info(String.format("closing invoices with non-credit vas charges. subscription id = %d", subscription.getId()));
        if (subscription.getSubscriber() != null &&
                subscription.getSubscriber().getInvoices() != null) {
            List<Invoice> invoiceList = subscription.getSubscriber().getInvoices();
            for (final Invoice invoice : invoiceList) {
                if (invoice.getState().equals(InvoiceState.OPEN)) {
                    boolean closeInvoice = true;
                    List<Charge> charges = invoice.getCharges();
                    if (charges != null) {
                        for (final Charge charge : charges) {
                            if (charge.getVas() != null && charge.getVas().isCredit()) {
                                closeInvoice = false;
                            }
                        }
                    }
                    if (closeInvoice) {
                        log.info(String.format("closing invoice. invoice id = %d", invoice.getId()));
                        invoice.setState(InvoiceState.CLOSED);
                        invoiceFacade.update(invoice);
                    }
                }
            }
        }
        subscription = subscriptionPersistenceFacade.update(subscription);
        log.info(String.format("closed invoices with non-credit vas charges. subscription id = %d", subscription.getId()));
        return subscription;
    }







    private void checkForTransitionFromInitialToActive(Subscription subscription) {



        if (subscription.getStatus() == SubscriptionStatus.INITIAL
                && subscription.getSubscriber().getLifeCycle() == SubscriberLifeCycleType.POSTPAID
                && subscription.getBalance().getRealBalance() >= 0
                && subscription.getActivationDate() == null && subscription.getExpirationDate() == null) {
//            engineFactory.getOperationsEngine(subscription).activatePrepaid(subscription);
            log.debug("before activatePostpaid(subscription);");
            activatePostpaid(subscription);

        }
        log.debug("after activatePostpaid(subscription);");
    }








    @Override
    public String generateAgreement(AgreementGenerationParams params) {
        if (params.agreement != null) {
            return params.agreement;
        }
        return null;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    private void initService(Subscription subscription) {
        try {
            ProvisioningEngine provisioner = provisioningFactory.getProvisioningEngine(subscription);
            if (provisioner.initService(subscription)) {
                systemLogger.success(SystemEvent.SERVICE_INITIALIZED, subscription,
                        String.format("Service initialized")
                );
            }
        } catch (ProvisionerNotFoundException ex) {
            log.error("Error initializing service", ex);
            systemLogger.error(SystemEvent.SERVICE_INITIALIZED, subscription, ex.getCause().getMessage());
        }
    }
}
