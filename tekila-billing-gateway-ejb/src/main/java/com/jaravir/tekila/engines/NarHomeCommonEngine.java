package com.jaravir.tekila.engines;


import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.model.BillingModelPersistenceFacade;
import com.jaravir.tekila.base.persistence.manager.BillingSettingsManager;
import com.jaravir.tekila.equip.EquipmentPersistenceFacade;
import com.jaravir.tekila.module.accounting.InvoiceState;
import com.jaravir.tekila.module.accounting.entity.Charge;
import com.jaravir.tekila.module.accounting.entity.Invoice;
import com.jaravir.tekila.module.accounting.entity.Transaction;
import com.jaravir.tekila.module.accounting.entity.TransactionType;
import com.jaravir.tekila.module.accounting.manager.ChargePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.InvoicePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.TransactionPersistenceFacade;
import com.jaravir.tekila.module.campaign.*;
import com.jaravir.tekila.module.event.BillingEvent;
import com.jaravir.tekila.module.queue.PersistentQueueManager;
import com.jaravir.tekila.module.service.*;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.service.entity.ServiceSetting;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.service.model.BillingPrinciple;
import com.jaravir.tekila.module.service.persistence.manager.NotificationSettingPersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.ServicePersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.ServiceSettingPersistenceFacade;
import com.jaravir.tekila.module.store.equip.Equipment;
import com.jaravir.tekila.module.store.equip.EquipmentStatus;
import com.jaravir.tekila.module.subscription.persistence.entity.*;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import com.jaravir.tekila.provision.broadband.devices.Port;
import com.jaravir.tekila.provision.broadband.devices.exception.NoFreePortLeftException;
import com.jaravir.tekila.provision.broadband.devices.exception.PortAlreadyReservedException;
import com.jaravir.tekila.provision.broadband.devices.manager.MiniPopPersistenceFacade;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import com.jaravir.tekila.provision.exception.ProvisioningException;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.ejb.*;
import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * Created by ShakirG on 13/09/2018.
 */


/**
 * Created by kmaharov on 18.05.2017.
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class NarHomeCommonEngine {
    private final static Logger log = Logger.getLogger(NarHomeCommonEngine.class);

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
    private CommonOperationsEngine commonOperationsEngine;


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
                                   User user, String ... additionalProperties) throws NoFreePortLeftException, PortAlreadyReservedException {


        Long userId = session != null ? (Long) session.getAttribute("userID") : user.getId();

        //ctx.setRollbackOnly();

        subscriptionPersistenceFacade.save(subscription);
        log.debug("test 0 " +subscription.getSettingByType(ServiceSettingType.USERNAME));
        Balance balance = new Balance();
        balance.setRealBalance(0L);
        balance.setPromoBalance(0L);

        if (selectedSubscriber.getLifeCycle() == SubscriberLifeCycleType.POSTPAID) {
            balance.setVirtualBalance(0);
        }
        subscription.setInstallationFeeInDouble(installationFeeRate);
log.debug("test 1"+subscription.getSettingByType(ServiceSettingType.USERNAME));
        selectedSubscriber.getSubscriptions().add(subscription);
        subscription.setUser(user);
        subscription.setSubscriber(selectedSubscriber);
        subscription.setService(serviceFacade.find(Long.valueOf(serviceId)));
        subscription.setStatus(SubscriptionStatus.INITIAL);

        log.debug("the created subscription: " + subscription);
        log.debug("the created username: " + subscription.getSettingByType(ServiceSettingType.USERNAME));

        subscription.setBalance(balance);
        if (subscription.getService().getProvider().getId() != 454114) {
            subscription.setIdentifier(subscription.getAgreement());
        }

        List<com.jaravir.tekila.module.service.entity.Resource> resList = subscription.getService().getResourceList();
        log.debug(subscription.getAgreement()+" resList for "+ resList);
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

        if (subscription.getService().getServiceType() == ServiceType.BROADBAND) {
            //MiniPop miniPop = miniPopFacade.find(miniPopId);
            Port port = miniPopFacade.getAvailablePort(miniPop);

            //if (port != null) return;
            log.debug("settings proof "+subscription.getSettingByType(ServiceSettingType.USERNAME));
           subscription.getSettingByType(ServiceSettingType.USERNAME)
                    .setValue(
                    //"Ethernet0/0/5:4846-fbe9-b9f0" + DateTime.now().toString(DateTimeFormat.forPattern("yyyyMMddHHmmss")) +"@narhome");
                    "Ethernet0/0/" + String.valueOf(port.getNumber()) + ":" + miniPop.getMac()
            );
            subscription.getSettingByType(ServiceSettingType.PASSWORD).setValue("-");
            subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH).setValue(String.valueOf(miniPop.getId()));
            subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH_PORT).setValue(String.valueOf(port.getNumber()));

            log.debug("USERNAME: " + subscription.getSettingByType(ServiceSettingType.USERNAME));

        }
        log.debug(subscription.getAgreement()+" service: " + subscription.getService() + ", resource list: " + resList);
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
                installationFee = subscription.rerate(installationFee);
                Charge instFeeCharge = new Charge();
                instFeeCharge.setService(subscription.getService());
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
                        register.setStatus(CampaignStatus.ACTIVE);
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
                rate = subscription.rerate(rate);
                Charge servFeeCharge = new Charge();
                servFeeCharge.setService(subscription.getService());
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

                        log.debug("subscriptionVAS.getVAS: " + vas.getVas());

                        invoiceFacade.addVasChargeToInvoice(
                                invoice,
                                subscription,
                                vasTotal,
                                vas.getVas(),
                                user.getId(),
                                "Charge for " + vas.getVas().getName() + " - " + (message.lastIndexOf(" (") != -1 ? message.substring(0, message.lastIndexOf(" (")) : message));
                    }

                    subscriptionPersistenceFacade.persist(vas);

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

            subscription.setBillingModel(modelFacade.find(BillingPrinciple.CONTINUOUS));

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

        if (subscription.getBalance().getRealBalance() >= 0) {
            activatePrepaid(subscription);
        }
        initService(subscription);
        log.debug("end of GeneralOPerationsEngine");
    }


    public Subscription createSubscriptionSpring(Subscriber selectedSubscriber, Subscription subscription, Long serviceId, MiniPop miniPop, boolean isUseStockEquipment, Equipment equipment, List<NotificationSettingRow> notificationSettings,
                                                 double installationFeeRate, Campaign selectedCampaign, User user, String... additionalProperties) throws NoFreePortLeftException, PortAlreadyReservedException {

        subscriptionPersistenceFacade.save(subscription);
        log.debug("test 0 " +subscription.getSettingByType(ServiceSettingType.USERNAME));
        Balance balance = new Balance();
        balance.setRealBalance(0L);
        balance.setPromoBalance(0L);

        if (selectedSubscriber.getLifeCycle() == SubscriberLifeCycleType.POSTPAID) {
            balance.setVirtualBalance(0);
        }
        subscription.setInstallationFeeInDouble(installationFeeRate);
        log.debug("test 1"+subscription.getSettingByType(ServiceSettingType.USERNAME));
        selectedSubscriber.getSubscriptions().add(subscription);
        subscription.setUser(user);
        subscription.setSubscriber(selectedSubscriber);
        subscription.setService(serviceFacade.find(Long.valueOf(serviceId)));
        subscription.setStatus(SubscriptionStatus.INITIAL);

        log.debug("the created subscription: " + subscription);
        log.debug("the created username: " + subscription.getSettingByType(ServiceSettingType.USERNAME));

        subscription.setBalance(balance);
        if (subscription.getService().getProvider().getId() != 454114) {
            subscription.setIdentifier(subscription.getAgreement());
        }

        List<com.jaravir.tekila.module.service.entity.Resource> resList = subscription.getService().getResourceList();
        log.debug(subscription.getAgreement()+" resList for "+ resList);
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

        if (subscription.getService().getServiceType() == ServiceType.BROADBAND) {
            //MiniPop miniPop = miniPopFacade.find(miniPopId);
            Port port = miniPopFacade.getAvailablePort(miniPop);

            //if (port != null) return;
            log.debug("settings proof "+subscription.getSettingByType(ServiceSettingType.USERNAME));
            subscription.getSettingByType(ServiceSettingType.USERNAME)
                    .setValue(
                            //"Ethernet0/0/5:4846-fbe9-b9f0" + DateTime.now().toString(DateTimeFormat.forPattern("yyyyMMddHHmmss")) +"@narhome");
                            "Ethernet0/0/" + String.valueOf(port.getNumber()) + ":" + miniPop.getMac()
                    );
            subscription.getSettingByType(ServiceSettingType.PASSWORD).setValue("-");
            subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH).setValue(String.valueOf(miniPop.getId()));
            subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH_PORT).setValue(String.valueOf(port.getNumber()));

            log.debug("USERNAME: " + subscription.getSettingByType(ServiceSettingType.USERNAME));

        }
        log.debug(subscription.getAgreement()+" service: " + subscription.getService() + ", resource list: " + resList);
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
                installationFee = subscription.rerate(installationFee);
                Charge instFeeCharge = new Charge();
                instFeeCharge.setService(subscription.getService());
                instFeeCharge.setAmount(installationFee);
                instFeeCharge.setSubscriber(subscription.getSubscriber());
                instFeeCharge.setUser_id(user.getId());
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

                invoiceFacade.addEquipmentChargeToInvoice(invoice, equipment, subscription, discount, user.getId());
            }

            Long rate = null;
            log.info(String.format("create: agreement=%s  charging for service registerList %s", subscription.getAgreement(), registerList));

            if (selectedCampaign != null && registerList != null && !registerList.isEmpty()) {
                for (CampaignRegister register : registerList) {
                    if (register.getCampaign().getTarget() == CampaignTarget.SERVICE_RATE) {
                        rate = register.getBonusAmount();
                        register.decrementLifecycleCount();
                        register.setStatus(CampaignStatus.ACTIVE);
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
                rate = subscription.rerate(rate);
                Charge servFeeCharge = new Charge();
                servFeeCharge.setService(subscription.getService());
                servFeeCharge.setAmount(rate);
                servFeeCharge.setSubscriber(subscription.getSubscriber());
                servFeeCharge.setUser_id(user.getId());
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

                        log.debug("subscriptionVAS.getVAS: " + vas.getVas());

                        invoiceFacade.addVasChargeToInvoice(
                                invoice,
                                subscription,
                                vasTotal,
                                vas.getVas(),
                                user.getId(),
                                "Charge for " + vas.getVas().getName() + " - " + (message.lastIndexOf(" (") != -1 ? message.substring(0, message.lastIndexOf(" (")) : message));
                    }

                    subscriptionPersistenceFacade.persist(vas);

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

            subscription.setBillingModel(modelFacade.find(BillingPrinciple.CONTINUOUS));

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

        if (subscription.getBalance().getRealBalance() >= 0) {
            activatePrepaid(subscription);
        }
        initService(subscription);
        log.debug("end of GeneralOPerationsEngine");

        return subscription;
    }


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


    public Subscription addVAS(VASCreationParams params) {
        return commonOperationsEngine.addVAS(params);
    }


    public Subscription removeVAS(Subscription subscription, SubscriptionVAS vas) {
        return commonOperationsEngine.removeVAS(subscription, vas);
    }


    public Subscription removeVAS(Subscription subscription, ValueAddedService vasID) {
        return commonOperationsEngine.removeVAS(subscription, vasID);
    }

    public Subscription editVAS(VasEditParams params) {
        return commonOperationsEngine.editVAS(params);
    }


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


    public Subscription prolongPrepaid(Subscription subscription) {
        return commonOperationsEngine.prolongPrepaid(subscription);
    }

    public boolean activatePostpaid(Subscription subscription) throws ProvisionerNotFoundException {
        return commonOperationsEngine.activatePostpaid(subscription);
    }


    public Subscription changeStatus(Subscription subscription, SubscriptionStatus newStatus) {
        return commonOperationsEngine.changeStatus(subscription, newStatus);
    }


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
