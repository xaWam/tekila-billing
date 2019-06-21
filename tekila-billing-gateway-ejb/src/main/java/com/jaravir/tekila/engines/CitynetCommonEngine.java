package com.jaravir.tekila.engines;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.base.model.BillingModelPersistenceFacade;
import com.jaravir.tekila.base.persistence.manager.BillingSettingsManager;
import com.jaravir.tekila.equip.EquipmentPersistenceFacade;
import com.jaravir.tekila.module.accounting.InvoiceState;
import com.jaravir.tekila.module.accounting.entity.*;
import com.jaravir.tekila.module.accounting.manager.AccountingTransactionPersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.ChargePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.InvoicePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.TransactionPersistenceFacade;
import com.jaravir.tekila.module.campaign.*;
import com.jaravir.tekila.module.event.BillingEvent;
import com.jaravir.tekila.module.queue.PersistentQueueManager;
import com.jaravir.tekila.module.service.*;
import com.jaravir.tekila.module.service.entity.*;
import com.jaravir.tekila.module.service.model.BillingPrinciple;
import com.jaravir.tekila.module.service.persistence.manager.NotificationSettingPersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.ServicePersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.ServiceSettingPersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.VASPersistenceFacade;
import com.jaravir.tekila.module.store.equip.Equipment;
import com.jaravir.tekila.module.store.equip.EquipmentStatus;
import com.jaravir.tekila.module.store.ip.persistence.IpAddress;
import com.jaravir.tekila.module.subscription.persistence.entity.*;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionSettingPersistenceFacade;
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
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.ejb.*;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by kmaharov on 18.05.2017.
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class CitynetCommonEngine {
    private final static Logger log = Logger.getLogger(CitynetCommonEngine.class);

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
    private PersistentQueueManager queueManager;
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
    private SubscriptionSettingPersistenceFacade settingFacade;
    @EJB
    private VASPersistenceFacade vasFacade;
    @EJB
    private CommonOperationsEngine commonEngine;
    @EJB
    private EngineFactory engineFactory;
    @EJB
    private VASPersistenceFacade vasPersistenceFacade;
    @EJB
    private UserPersistenceFacade userFacade;
    @EJB
    private AccountingTransactionPersistenceFacade accTransFacade;


    public void createSubscription(
            Subscriber selectedSubscriber,
            Subscription subscription,
            String serviceId,
            MiniPop miniPop,
            boolean isUseStockEquipment,
            Equipment equipment,
            List<NotificationSettingRow> notificationSettings,
            double installationFeeRate,
            Campaign selectedCampaign,
            HttpSession session,
            User user,
            String... additionalProperties) throws NoFreePortLeftException, PortAlreadyReservedException {
        log.debug("INTSALLATION FEE: " + installationFeeRate);

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
        subscription.setIptvOwner(subscription.getService() != null && subscription.getService().isIptvBundled());
        subscription.setStatus(SubscriptionStatus.INITIAL);

        log.debug("the created subscription: " + subscription);


        subscription.setAgreement(subscription.getAgreement().trim());


        subscription.setBalance(balance);
        subscription.setIdentifier(subscription.getAgreement());

        List<com.jaravir.tekila.module.service.entity.Resource> resList = subscription.getService().getResourceList();

        List<ServiceSetting> settings = subscription.getService().getSettings();
        subscription.copySettingsFromService(settings);

        if (isUseStockEquipment && equipment != null) {
            subscription.setSettingByType(ServiceSettingType.TV_EQUIPMENT, equipment.getPartNumber());
            EquipmentStatus oldEquipmentStatus = equipment.getStatus();
            equipment.reserve();

            systemLogger.success(
                    SystemEvent.EQUIPMENT_RESERVED,
                    subscription, String.format("equipment.id=%d, status changed from %s to %s",
                            equipment.getId(), oldEquipmentStatus, equipment.getStatus())
            );
        }

        if (subscription.getService().getServiceType() == ServiceType.BROADBAND) {
            //MiniPop miniPop = miniPopFacade.find(miniPopId);
            Port port = miniPopFacade.getAvailablePort(miniPop);

                log.debug("PORT: " + port);

            //if (port != null) return;
            List<SubscriptionSetting> settingList = new ArrayList<>();
            SubscriptionSetting subscriptionSetting = new SubscriptionSetting();

            subscriptionSetting.setValue(miniPop.getMasterVlan().toString() + "-" + (miniPop.getSubVlan() + port.getNumber()));
            ServiceSetting serviceSetting = commonEngine.createServiceSetting("Username", ServiceSettingType.USERNAME, Providers.CITYNET, ServiceType.BROADBAND, "");
            subscriptionSetting.setProperties(serviceSetting);
            settingFacade.save(subscriptionSetting);
            subscriptionSetting = settingFacade.update(subscriptionSetting);
            settingList.add(subscriptionSetting);

            subscriptionSetting = new SubscriptionSetting();
            subscriptionSetting.setValue(String.valueOf(miniPop.getId()));
            serviceSetting = commonEngine.createServiceSetting("Switch", ServiceSettingType.BROADBAND_SWITCH, Providers.CITYNET, ServiceType.BROADBAND, "");
            subscriptionSetting.setProperties(serviceSetting);
            settingFacade.save(subscriptionSetting);
            subscriptionSetting = settingFacade.update(subscriptionSetting);
            settingList.add(subscriptionSetting);

            subscriptionSetting = new SubscriptionSetting();
            subscriptionSetting.setValue(String.valueOf(port.getNumber()));
            serviceSetting = commonEngine.createServiceSetting("Switch port", ServiceSettingType.BROADBAND_SWITCH_PORT, Providers.CITYNET, ServiceType.BROADBAND, "");
            subscriptionSetting.setProperties(serviceSetting);
            settingFacade.save(subscriptionSetting);
            subscriptionSetting = settingFacade.update(subscriptionSetting);
            settingList.add(subscriptionSetting);

            subscriptionSetting = new SubscriptionSetting();
            subscriptionSetting.setValue("-");
            serviceSetting = commonEngine.createServiceSetting("Password", ServiceSettingType.PASSWORD, Providers.CITYNET, ServiceType.BROADBAND, "");
            subscriptionSetting.setProperties(serviceSetting);
            settingFacade.save(subscriptionSetting);
            subscriptionSetting = settingFacade.update(subscriptionSetting);
            settingList.add(subscriptionSetting);

            subscriptionSetting = new SubscriptionSetting();
            subscriptionSetting.setValue(miniPop.getIp());
            serviceSetting = commonEngine.createServiceSetting("Switch IP", ServiceSettingType.BROADBAND_SWITCH_IP, Providers.CITYNET, ServiceType.BROADBAND, "");
            subscriptionSetting.setProperties(serviceSetting);
            settingFacade.save(subscriptionSetting);
            subscriptionSetting = settingFacade.update(subscriptionSetting);
            settingList.add(subscriptionSetting);

            subscription.setSettings(settingList);

            //subscription.setSettingByType(ServiceSettingType.USERNAME).setValue("Test");
            //subscription.getSettingByType(ServiceSettingType.PASSWORD).setValue("-");
            //subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH).setValue(String.valueOf(miniPop.getId()));
            //subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH_PORT).setValue(String.valueOf(port.getNumber()));
            log.debug("USERNAME: " + subscription.getSettingByType(ServiceSettingType.USERNAME));

        }
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

        List<CampaignRegister> registerList = null;
        log.info(String.format("create: agreement = %s, selectedCampaign id=%d", subscription.getAgreement(),
                selectedCampaign != null ? selectedCampaign.getId() : 0));

        if (selectedCampaign != null) {
            registerList = campaignJoinerBean.tryAddToCampaigns(subscription, selectedCampaign, true, false, null);
            log.debug(String.format("create: agreement = %s, registerList=%s", subscription.getAgreement(),
                    registerList));
        } else {
            campaignJoinerBean.tryAddToCampaigns(subscription);
        }

        if (subscription.getService().getServiceType() == ServiceType.BROADBAND) {
            //String bandwidth = serviceFacade.getBucketCapacity(subscription);
            Long bandwidth = campaignRegisterFacade.getBonusAmount(subscription, CampaignTarget.RESOURCE_INTERNET_BANDWIDTH);

            log.info(String.format("create: subscription agreement=%s, found resource campaign, new bandwidth=%s", subscription.getAgreement(), bandwidth));

            if (bandwidth != null) {
                subscription.setBucketCapacityByType(ResourceBucketType.INTERNET_DOWN, String.valueOf(bandwidth));
                subscription.setBucketCapacityByType(ResourceBucketType.INTERNET_UP, String.valueOf(bandwidth));
            }
        }
        log.debug("create: created sub: " + subscription);

        int totalCharge = 0;

        if (selectedSubscriber.getLifeCycle() == SubscriberLifeCycleType.PREPAID) {
            Invoice invoice = null;
            List<Invoice> openInvoiceList = invoiceFacade.findOpenBySubscriber(subscription.getSubscriber().getId());

            log.debug("create: Invoice search result: " + openInvoiceList);

            boolean isNewInvoice = false;

            if (openInvoiceList == null || openInvoiceList.isEmpty()) {
                invoice = new Invoice();
                isNewInvoice = true;
            } else {
                invoice = openInvoiceList.get(0);
            }

            invoice.setSubscriber(selectedSubscriber);
            invoice.setState(InvoiceState.OPEN);
            //charge for installation fee if > 0
            long installationFee = installationFeeRate != 0 ? subscription.getInstallationFee() : subscription.getService().getInstallationFee();

            if (installationFee >= 0) {
                //balance.debitReal(subscription.getService().getInstallationFee() * 100000);
                Charge instFeeCharge = new Charge();
                instFeeCharge.setService(subscription.getService());
                installationFee = subscription.rerate(installationFee);
                instFeeCharge.setAmount(installationFee);
                instFeeCharge.setSubscriber(subscription.getSubscriber());
                instFeeCharge.setUser_id(userId);
                instFeeCharge.setSubscription(subscription);
                instFeeCharge.setDsc("Charge for installation fee");
                instFeeCharge.setDatetime(DateTime.now());
                chargeFacade.save(instFeeCharge);

                Transaction transDebitInstFee = new Transaction(
                        TransactionType.DEBIT,
                        subscription,
                        installationFee,
                        "Charged for installation fee");
                transDebitInstFee.execute();
                transFacade.save(transDebitInstFee);

                instFeeCharge.setTransaction(transDebitInstFee);

                invoice.addChargeToList(instFeeCharge);

                log.info(
                        String.format("Charge for installation fee: subscription id=%d, agreement=%s, amount=%d",
                                subscription.getId(),
                                subscription.getAgreement(), instFeeCharge.getAmount())
                );

                systemLogger.success(SystemEvent.CHARGE, subscription, transDebitInstFee,
                        String.format(
                                "Charged id=%d for installation fee=%s",
                                instFeeCharge.getId(), instFeeCharge.getAmountForView())
                );
                /*if (selectedSubscriber.getLifeCycle() == SubscriberLifeCycleType.POSTPAID)
                 totalCharge += instFeeCharge.getAmount();    */
            }

            if (isUseStockEquipment && equipment != null) {
                double discount = 0;

                if (selectedCampaign != null && registerList != null && !registerList.isEmpty()) {
                    for (CampaignRegister register : registerList) {
                        if (register.getCampaign().getTarget() == CampaignTarget.SERVICE_RATE) {
                            discount = register.getCampaign().getEquipmentDiscount();
                            log.info(String.format("create: agreement=%s equipment charge register id=%d, discount=%f", subscription.getAgreement(), register.getId(), discount));
                            break;
                        }
                    }
                }

                invoiceFacade.addEquipmentChargeToInvoice(invoice, equipment, subscription, discount, userId);
            }

            Long rate = null;
            log.info(String.format("create: agreement=%s  charging for service registerList %s", subscription.getAgreement(), registerList));

            if (selectedCampaign != null && registerList != null && !registerList.isEmpty()) {
                for (CampaignRegister register : registerList) {
                    if (register.getCampaign().getTarget() == CampaignTarget.SERVICE_RATE) {
                        rate = register.getBonusAmount();
                        register.decrementLifecycleCount();
                        log.info(String.format("create: agreement=%s  register id=%d", subscription.getAgreement(), register.getId()));
                        break;
                    }
                }
            }

            if (selectedCampaign == null || rate == null) {
                rate = campaignRegisterFacade.getBonusAmount(subscription, CampaignTarget.SERVICE_RATE);
            }

            if (rate == null) {
                rate = subscription.getService().getServicePrice();
            }

            //charge for service fee if > 0
            if (rate >= 0) {
                Charge servFeeCharge = new Charge();
                servFeeCharge.setService(subscription.getService());
                rate = subscription.rerate(rate);
                servFeeCharge.setAmount(rate);
                servFeeCharge.setSubscriber(subscription.getSubscriber());
                servFeeCharge.setUser_id(userId);
                servFeeCharge.setSubscription(subscription);
                servFeeCharge.setDsc("Charge for service fee");
                servFeeCharge.setDatetime(DateTime.now());
                chargeFacade.save(servFeeCharge);
                //balance.debitReal(rate * 100000);
                Transaction transDebitServiceFee = new Transaction(
                        TransactionType.DEBIT,
                        subscription,
                        rate,
                        "Charged for service fee"
                );
                transDebitServiceFee.execute();
                transFacade.save(transDebitServiceFee);

                servFeeCharge.setTransaction(transDebitServiceFee);

                invoice.addChargeToList(servFeeCharge);

                log.info(
                        String.format("Charge for servce fee: subscription id=%d, agreement=%s, amount=%d",
                                subscription.getId(),
                                subscription.getAgreement(),
                                servFeeCharge.getAmount())
                );

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

            log.debug(String.format("service default vas list %d, subscription vasList: %d", subscription.getService().getDefaultVasList().size(), subscription.getVasList().size()));
            if (subscription.getService().getDefaultVasList() != null && !subscription.getService().getDefaultVasList().isEmpty() && !subscription.getVasList().isEmpty()) {
                for (SubscriptionVAS vas : subscription.getVasList()) {
                    StringBuilder message = null;

                    long vasTotal = 0;

                    for (SubscriptionVASSetting set : vas.getSettings()) {
                        vasTotal += set.getTotal();

                        message = new StringBuilder();

                        message.append(set.getName()).
                                append(": price ").append(set.getValue())
                                .append(", length/count ").append(set.getLength());

                        if (set.getDsc() != null && !set.getDsc().isEmpty()) {
                            message.append(" (").append(set.getDsc()).append("), ");
                        }

                        log.debug("USER: " + user + " vas.getVas(): " + vas.getVas());

                        invoiceFacade.addVasChargeToInvoice(
                                invoice,
                                subscription,
                                vasTotal,
                                vas.getVas(),
                                user.getId(),
                                "Charge for " + vas.getVas().getName() + " - " + (message.lastIndexOf(" (") != -1 ? message.substring(0, message.lastIndexOf(" (")) : message));
                    }

                    log.debug("BEFORE PERSIRT");

                    subscriptionPersistenceFacade.persist(vas);

                    log.debug("HERE");

                    systemLogger.success(SystemEvent.VAS_ADDED, subscription,
                            String.format("Added VAS id=%d, subscriptionVAS id=%d", vas.getVas().getId(), vas.getId())
                    );
                }
            }
            //this.update(subscription);

            if (isNewInvoice) {
                invoiceFacade.save(invoice);
                systemLogger.success(SystemEvent.INVOICE_CREATED, subscription, "invoice id=" + invoice.getId());

                try {
                    queueManager.sendInvoiceNotification(BillingEvent.INVOICE_CREATED, subscription, invoice);
                    String msg = String.format("event=%s, subscription id=%d", BillingEvent.INVOICE_CREATED, subscription.getId());
                    systemLogger.success(SystemEvent.NOTIFICATION_SCHEDULED, subscription, msg);
                    log.info("Notification added: " + msg);
                } catch (Exception ex) {
                    String msg = String.format("event=%s, subscription id=%d, invoice id=%d", BillingEvent.INVOICE_CREATED, subscription.getId(), invoice.getId());
                    systemLogger.error(SystemEvent.NOTIFICATION_SCHEDULED, subscription, msg);
                    log.error("Cannot add notification: " + msg, ex);
                }
            }

            if (billSettings.getSettings().getPrepaidlifeCycleLength() == 30) {
                subscription.setBilledUpToDate(DateTime.now().plusMonths(1));
            } else {
                subscription.setBilledUpToDate(DateTime.now().plusDays(
                        billSettings.getSettings().getPrepaidlifeCycleLength()
                ));
            }

            //subscription.synchronizeExpiratioDates();
            /* else {
             invoiceFacade.update(invoice);
             }*/
        } // END IF PREPAID

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

        commonEngine.initService(subscription);

        if (subscription.getService().getProvider().getId() == Providers.CITYNET.getId() ||
                subscription.getService().getProvider().getId() == Providers.QUTU.getId()) {
            provisionIptv(subscription);
        }
    }


    public Subscription changeService(Subscription subscription, Service targetService, boolean upCharge, boolean downCharge) {
        subscription = handleServiceChange(subscription, targetService, upCharge, downCharge);
        log.info(String.format("deleting all campaigns on service change, subscription id = %d", subscription.getId()));
        subscription = deleteAllCampaigns(subscription);
        log.info(String.format("deleted all campaigns on service change, subscription id = %d", subscription.getId()));

        List<SubscriptionVAS> sbnVasList = subscription.getVasList();
        if (sbnVasList != null) {
            for (final SubscriptionVAS sbnVAS : sbnVasList) {
                boolean allowed = false;

                final List<ValueAddedService> allowedVasList = targetService.getAllowedVASList();
                if (allowedVasList != null) {
                    for (final ValueAddedService vas : allowedVasList) {
                        if (vas.getId() == sbnVAS.getVas().getId()) {
                            allowed = true;
                        }
                    }
                }
                if (!allowed) {
                    log.info(
                            String.format(
                                    "deleting value added service on service change, vas id = %d, subscription id = %d",
                                    sbnVAS.getVas().getId(),
                                    subscription.getId())
                    );
                    subscription = commonEngine.removeVAS(subscription, sbnVAS);
                }
            }
        }
        subscription = commonEngine.restateIptvOwnership(subscription);
        return subscription;
    }

    private Subscription deleteAllCampaigns(Subscription subscription) {
        List<CampaignRegister> campaigns = subscription.getCampaignRegisters();
        subscription.getBalance().setPromoBalance(0);
        systemLogger.success(SystemEvent.RESET_PROMO_BALANCE, subscription, "Promo balance resetted");
        for (final CampaignRegister campaignRegister : campaigns) {
            /*if (campaignRegister.getCampaign().getTarget().equals(CampaignTarget.PAYMENT)) {
                doit
            }*/
            campaignRegisterFacade.removeDetached(campaignRegister);
            String dsc = String.format("subscription id=%s, campaign id=%s", subscription.getId(), campaignRegister.getCampaign().getId());
            log.info("Deleted campaign on service change, " + dsc);
            systemLogger.success(SystemEvent.SUBSCRIPTION_CAMPAIGN_DELETED, subscription, dsc);
        }
        subscription.setCampaignRegisters(null);
        subscription = subscriptionPersistenceFacade.update(subscription);
        return subscription;
    }

    private boolean feeForDowngrade(Subscription subscription, Service targetService) {
        log.debug("target: " + targetService.getServicePrice() + "old: " + subscription.getService().getServicePrice());
        ValueAddedService vas = findVasForCharge(
                ValueAddedServiceType.SERVICE_CHANGE_DOWN_CHARGE,
                subscription.getService().getProvider().getId());
        if (vas == null) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Not found VAS for service change", "Not found VAS for service change"));
            return false;
        }
        feeForChangeService(subscription, vas.getPrice());
        return true;
    }

    private boolean feeForUpgrade(Subscription subscription, Service targetService) {
        log.debug("target: " + targetService.getServicePrice() + "old: " + subscription.getService().getServicePrice());
        ValueAddedService vas = findVasForCharge(
                ValueAddedServiceType.SERVICE_CHANGE_UP_CHARGE,
                subscription.getService().getProvider().getId());
        if (vas == null) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Not found VAS for service change", "Not found VAS for service change"));
            return false;
        }
        feeForChangeService(subscription, (targetService.getServicePrice() - subscription.getService().getServicePrice()));
        return true;
    }

    private Subscription handleServiceChange(
            Subscription subscription,
            Service targetService,
            boolean upCharge,
            boolean downCharge) {
        String msg = String.format("subscription id=%d, targetService id=%d, currentService id=%d", subscription.getId(),
                targetService.getId(), subscription.getService().getId());
        log.info(msg);


        if (subscription.getService().getServicePrice() > targetService.getServicePrice()) {
            if (downCharge) {
                if (!feeForDowngrade(subscription, targetService)) {
                    log.info(String.format(
                            "Could not charge for service change, vas not found. Subscription id=%d, Target Service id=%d",
                            subscription.getId(),
                            targetService.getId()));
                    return null;
                }
            }
        } else if (upCharge) {
            if (!feeForUpgrade(subscription, targetService)) {
                log.info(String.format(
                        "Could not charge for service change, vas not found. Subscription id=%d, Target Service id=%d",
                        subscription.getId(),
                        targetService.getId()));
                return null;
            }
        }

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

    private boolean feeForChangeService(Subscription sub, long rate) {
        try {
            Invoice newInvoice = null;
            Subscriber subscriber = sub.getSubscriber();
            for (Invoice inv : subscriber.getInvoices()) {
                if (inv.getState() == InvoiceState.OPEN
                        && new DateTime(inv.getCreationDate()).isAfter((DateTime.now().withTimeAtStartOfDay()))) {
                    newInvoice = inv;
                }
            }

            if (newInvoice == null) {
                newInvoice = new Invoice();
                newInvoice.setState(InvoiceState.OPEN);
                newInvoice.setSubscriber(subscriber);
                newInvoice.setSubscription(sub);
                invoiceFacade.save(newInvoice);

                log.debug(String.format("New invoice created for subscriber id %d, subscription id %d", sub.getSubscriber().getId(), sub.getId()));
                systemLogger.success(SystemEvent.INVOICE_CREATED, sub,
                        String.format("Created during billing process: invoice id=%d", newInvoice.getId())
                );
            }

            Charge lateFeeCharge = new Charge();
            lateFeeCharge.setService(sub.getService());
            rate = sub.rerate(rate);
            lateFeeCharge.setAmount(rate);
            lateFeeCharge.setSubscriber(subscriber);
            lateFeeCharge.setUser_id(20000L);
            lateFeeCharge.setDsc("ChangeServiceFee");
            lateFeeCharge.setDatetime(DateTime.now());
            lateFeeCharge.setSubscription(sub);
            chargeFacade.save(lateFeeCharge);

            Transaction transDebitServiceFee = new Transaction(
                    TransactionType.DEBIT,
                    sub,
                    rate,
                    "Change Service Fee"
            );
            transDebitServiceFee.execute();
            transFacade.save(transDebitServiceFee);
            lateFeeCharge.setTransaction(transDebitServiceFee);

            newInvoice.addChargeToList(lateFeeCharge);

            log.info(
                    String.format("Charge for change service fee: subscription id=%d, agreement=%s, amount=%d",
                            sub.getId(), sub.getAgreement(), lateFeeCharge.getAmount())
            );

            systemLogger.success(
                    SystemEvent.CHARGE,
                    sub,
                    transDebitServiceFee,
                    String.format("Charge id=%d for change service fee, amount=%s",
                            lateFeeCharge.getId(), lateFeeCharge.getAmountForView()
                    )
            );

            return true;
        } catch (Exception ex) {

            log.info("Cannot charge for change service fee: " + ex);
            return false;
        }
    }


    public Subscription addVAS(VASCreationParams params) {
        return commonEngine.addVAS(params);
    }


    public Subscription removeVAS(Subscription subscription, SubscriptionVAS vas) {
        return commonEngine.removeVAS(subscription, vas);
    }


    public Subscription removeVAS(Subscription subscription, ValueAddedService vasID) {
        return commonEngine.removeVAS(subscription, vasID);
    }

    public Subscription editVAS(VasEditParams params) {
        return commonEngine.editVAS(params);
    }

    public void activatePrepaid(Subscription subscription) {
        subscription = subscriptionPersistenceFacade.update(subscription);
        long subsId = subscription.getId();

        DateTime activationDate = null;
        ProvisioningEngine provisioner = null;
        try {
            provisioner = engineFactory.getProvisioningEngine(subscription);
        } catch (ProvisionerNotFoundException e) {
            log.error("Privisioner not found on activatePrepaid for subscription "+subsId, e);
        }
        DateTimeFormatter frm = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
        if (provisioner != null) {
            activationDate = provisioner.getActivationDate(subscription);
        }
        if (activationDate != null) {
            log.debug("activationDate is not null for subscription "+subsId);
            subscription.setActivationDate(activationDate);
        }

        try {
            int graceDays = 0;
            if (subscription.getBillingModel() != null) {
                graceDays = subscription.getBillingModel().getGracePeriodInDays();
            }

            DateTime expirationWithGrace = activationDate.plusMonths(1).plusDays(graceDays);

            if (subscription.getBillingModel().getPrinciple() == BillingPrinciple.GRACE_MONTH) {
                expirationWithGrace = activationDate.plusMonths(2);
            }

            if (subscription.getStatus() == SubscriptionStatus.INITIAL
                    && subscription.getBalance().getRealBalance() >= 0
                    && activationDate != null
                    && provisioner.openService(subscription, expirationWithGrace)) {
                log.debug("Balance is >= 0 for subscription "+subsId);
                subscription.setBilledUpToDate(activationDate.plusMonths(1).withTime(23, 59, 59, 999));
                subscription.setExpirationDate(subscription.getBilledUpToDate());
                subscription.setExpirationDateWithGracePeriod(expirationWithGrace.withTime(23, 59, 59, 999));
                subscription = commonEngine.changeStatus(subscription, SubscriptionStatus.ACTIVE);
                subscription = subscriptionPersistenceFacade.update(subscription);
            } else {
                log.debug("Balance is < 0. Skipping... for subscription "+subsId);
                return;
            }

            log.info(
                    String.format("Subscription successfully activated: id=%d, agreement=%s, status=%s, realBalance=%s, billedUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s",
                            subscription.getId(), subscription.getAgreement(), subscription.getStatus(),
                            subscription.getBalance().getRealBalanceForView(),
                            subscription.getBilledUpToDate().toString(frm),
                            subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                            subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                    )
            );

            systemLogger.success(SystemEvent.SUBSCRIPTION_ACTIVATION_REPROCESS, subscription,
                    String.format("status=%s, realBalance=%s, billedUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s",
                            subscription.getStatus(),
                            subscription.getBalance().getRealBalanceForView(),
                            subscription.getBilledUpToDate().toString(frm),
                            subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                            subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                    )
            );

        } catch (Exception ex){
            log.error("Error occurs on activatePrepaid for "+subsId, ex);
        }
    }

    public Subscription prolongPrepaid(Subscription subscription) {
        return commonEngine.prolongPrepaid(subscription);
    }

    public boolean activatePostpaid(Subscription subscription) throws ProvisionerNotFoundException {
        return commonEngine.activatePostpaid(subscription);
    }


    public Subscription changeStatus(Subscription subscription, SubscriptionStatus newStatus) {
        log.debug("Changing Status for " + subscription.getAgreement() + " to " + newStatus);

        if (subscription == null || newStatus == null) {
            throw new IllegalArgumentException(String.format("changeStatus arguments cannot be null, provided newStatus=%s, subscription=%s", newStatus, subscription));
        }

        try {
            subscription = subscriptionPersistenceFacade.update(subscription);

            SystemEvent event = null;
            ProvisioningEngine provisioner = engineFactory.getProvisioningEngine(subscription);

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
                    event = SystemEvent.SUBSCRIPTION_STATUS_ACTIVE;
                    provisioner.openService(subscription);
                    break;
                case SUSPENDED:
                    event = SystemEvent.SUBSCRIPTION_STATUS_SUSPEND;
                    provisioner.closeService(subscription);
                    break;
                case PRE_FINAL:
                    event = SystemEvent.SUBSCRIPTION_STATUS_PRE_FINAL;
                    provisioner.closeService(subscription);
                    break;
                case FINAL:
                    subscription = removeAllVasList(subscription);
                    subscription = closeOpenInvoices(subscription);
                    subscription = resetBalanceToZero(subscription);
                    event = SystemEvent.SUBSCRIPTION_STATUS_FINAL;
                    provisioner.closeService(subscription);
                    break;
                case CANCEL:
                    subscription = removeAllVasList(subscription);
                    subscription = closeOpenInvoices(subscription);
                    subscription = resetBalanceToZero(subscription);
                    nullifyAndCloseInvoice(subscription);

                    event = SystemEvent.SUBSCRIPTION_STATUS_CANCEL;
                    provisioner.closeService(subscription);
                    break;
                default:
                    event = SystemEvent.SUBSCRIPTION_STATUS_BLOCK;
                    break;
            }

            if (subscription.getStatus() == SubscriptionStatus.INITIAL
                    && newStatus == SubscriptionStatus.ACTIVE
                    && subscription.getService().getProvider().getId() != Providers.CITYNET.getId()
                    && subscription.getService().getProvider().getId() != Providers.QUTU.getId()
                    && subscription.getService().getProvider().getId() != Providers.UNINET.getId()) {
                checkForTransitionFromInitialToActive(subscription);
            } else {
                systemLogger.success(event, subscription, String.format("subscription id=%d changed status from %s",
                        subscription.getId(), subscription.getStatus()));
                subscription.setStatus(newStatus);
                subscription.setLastStatusChangeDate(DateTime.now());
            }
            String msg = String.format("event=%s, subscription id=%d", BillingEvent.STATUS_CHANGED, subscription.getId());

            log.debug("Status become: " + subscription.getStatus());

            try {
                queueManager.sendStatusNotification(BillingEvent.STATUS_CHANGED, subscription);
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

public void nullifyAndCloseInvoice(Subscription subscription){
    Balance balance = new Balance();
    balance.setPromoBalance(0);
    balance.setRealBalance(0);
    subscription.setBalance(balance);
    for (Invoice invoice:subscription.getSubscriber().getInvoices()
            ) {

        if(invoice.getState() == InvoiceState.CLOSED){
            invoice.close();
        }
    }
}



    private void checkForTransitionFromInitialToActive(Subscription subscription) {
        if (subscription.getStatus() == SubscriptionStatus.INITIAL
                && subscription.getSubscriber().getLifeCycle() == SubscriberLifeCycleType.PREPAID
                && subscription.getService().getServicePrice() == 0
                && subscription.getBalance().getRealBalance() >= 0
                && subscription.getActivationDate() == null && subscription.getExpirationDate() == null) {
            engineFactory.getOperationsEngine(subscription).activatePrepaid(subscription);
        }
    }


    private Subscription resetBalanceToZero(Subscription sub) throws Exception {
        if (sub.getBalance().getRealBalance() < 0 &&
                !subscriptionPersistenceFacade.hasCreditVas(sub)) {
            Operation operation = new Operation();
            operation.setSubscription(sub);
            operation.setUser(userFacade.find(20000L));
            operation.setAmount(sub.getBalance().getRealBalance() * -1);
            operation.setDsc("Balance set to 0 because of went to FINAL");
            AccountingTransaction accountingTransaction = accTransFacade.createFromForm(operation, TransactionType.CREDIT, null);
            systemLogger.success(SystemEvent.SUBSCRIPTION_STATUS_FINAL, sub, "Balance was set to 0. accountingTransaction:" + accountingTransaction.getId());
            return operation.getSubscription();
        }
        return sub;
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
                            if (charge.getSubscription().getId()!=subscription.getId()){
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
//        subscription = subscriptionPersistenceFacade.update(subscription);
        log.info(String.format("closed invoices with non-credit vas charges. subscription id = %d", subscription.getId()));
        return subscription;
    }

    public String generateAgreement(AgreementGenerationParams params) {
        return null;
    }

    private ValueAddedService findVasForCharge(ValueAddedServiceType type, long providerId) {
        List<ValueAddedService> vasList = vasFacade.findAll();
        for (ValueAddedService vas : vasList) {
            if (vas.getCode().getType() == type && vas.getProvider().getId() == providerId) {
                return vas;
            }
        }
        for (ValueAddedService vas : vasList) {
            if (vas.getCode().getType() == type) {
                return vas;
            }
        }
        return null;
    }

    protected ServiceProvider createProvider(Providers provider) {
        ServiceProvider serviceProvider = new ServiceProvider();
        if (provider == Providers.CITYNET) {
            serviceProvider.setId(Providers.CITYNET.getId());
            serviceProvider.setName("Citynet");
        } else if (provider == Providers.AZERTELECOM) {
            serviceProvider.setId(Providers.AZERTELECOM.getId());
            serviceProvider.setName("Azertelecom");
        } else if (provider == Providers.BBTV) {
            serviceProvider.setId(Providers.BBTV.getId());
            serviceProvider.setName("BBTV");
        } else if (provider == Providers.UNINET) {
            serviceProvider.setId(Providers.UNINET.getId());
            serviceProvider.setName("UniNet");
        }

        return serviceProvider;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void provisionIptv(Subscription subscription) {
        try {
            ProvisioningEngine provisioner = provisioningFactory.getProvisioningEngine(subscription);
            if (provisioner.provisionIptv(subscription)) {
                systemLogger.success(SystemEvent.IPTV_PROVISIONED, subscription,
                        String.format("IpTv Provisioned")
                );
            }
        } catch (ProvisionerNotFoundException ex) {
            log.error("Error provisioning iptv", ex);
            systemLogger.error(SystemEvent.IPTV_PROVISIONED, subscription, ex.getCause().getMessage());
        }
    }
}
