package com.jaravir.tekila.engines;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.base.entity.Language;
import com.jaravir.tekila.base.model.BillingModelPersistenceFacade;
import com.jaravir.tekila.base.persistence.manager.BillingSettingsManager;
import com.jaravir.tekila.base.persistence.manager.Register;
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
import com.jaravir.tekila.module.service.entity.*;
import com.jaravir.tekila.module.service.model.BillingModel;
import com.jaravir.tekila.module.service.model.BillingPrinciple;
import com.jaravir.tekila.module.service.persistence.manager.*;
import com.jaravir.tekila.module.store.IpAddressPersistenceFacade;
import com.jaravir.tekila.module.store.equip.Equipment;
import com.jaravir.tekila.module.store.equip.EquipmentStatus;
import com.jaravir.tekila.module.store.ip.persistence.IpAddress;
import com.jaravir.tekila.module.subscription.exception.DuplicateAgreementException;
import com.jaravir.tekila.module.subscription.persistence.entity.*;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriberPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionSettingPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionVASPersistenceFacade;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import com.jaravir.tekila.provision.broadband.devices.Port;
import com.jaravir.tekila.provision.broadband.devices.exception.NoFreePortLeftException;
import com.jaravir.tekila.provision.broadband.devices.exception.PortAlreadyReservedException;
import com.jaravir.tekila.provision.broadband.devices.manager.MiniPopPersistenceFacade;
import com.jaravir.tekila.provision.broadband.devices.manager.Providers;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spring.controller.vm.SubscriptionCreationVM;
import spring.exceptions.BadRequestAlertException;
import spring.security.SecurityModuleUtils;

import javax.ejb.*;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by ShakirG on 21.03.2018.
 */

@Stateless(name = "CNCOperationsEngine", mappedName = "CNCOperationsEngine")
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class CNCOperationsEngine implements OperationsEngine {

    private final static Logger log = LoggerFactory.getLogger(CNCOperationsEngine.class);

    ///////////////////////////////////EJB IMPORTS//////////////////////////////////////////////
    @EJB
    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;
    @EJB
    private SubscriptionVASPersistenceFacade subscriptionVASPersistenceFacade;
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
    private CampaignPersistenceFacade campaignFacade;
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
    private EngineFactory engineFactory;
    @EJB
    private UserPersistenceFacade userFacade;
    @javax.annotation.Resource
    private SessionContext ctx;
    @EJB
    private Register register;
    @EJB
    private CitynetCommonEngine citynetCommonEngine;
    @EJB
    private CommonOperationsEngine commonOperationsEngine;
    @EJB
    private IpAddressPersistenceFacade ipAddressPersistenceFacade;
    @EJB
    private ServicePropertyPersistenceFacade servicePropertyPersistenceFacade;
    @EJB
    private SubscriptionServiceTypePersistenceFacade serviceTypePersistenceFacade;
    @EJB
    private SubscriberPersistenceFacade subscriberPersistenceFacade;
    ////////////////////////////////////////////////////////////////////////////////////////////


    @Override
    public Subscription createSubscriptionSpring(SubscriptionCreationVM subscriptionCreationVM, Subscriber selectedSubscriber, Subscription subscription, Service service, User user) {

        log.info("CNC createSubscriptionSpring method starts..");

        Campaign selectedCampaign = null;

        if (subscriptionCreationVM.getServiceTypeId() == null) {
            log.debug("Cannot create subscription. Service Type(PPPoE/Wireless/IPoE) must be selected");
            throw new BadRequestAlertException("Service type must be selected. There is no service type selected");
        }

        SubscriptionServiceType serviceType = serviceTypePersistenceFacade.find(Long.parseLong(subscriptionCreationVM.getServiceTypeId()));


//        if (serviceType.getProfile().getProfileType().equals(ProfileType.IPoE)) { //IPoE
//            if (subscriptionCreationVM.getMinipopId() == null || subscriptionCreationVM.getPortId() == null) {
//                log.debug("Cannot create subscription. Minipop and port must be selected");
//                throw new BadRequestAlertException("Cannot create subscription. Minipop and port must be selected. Cannot create subscription. Minipop and port must be selected");
//            }
//
//            selectedMiniPop = miniPopPersistenceFacade.find(subscriptionCreationVM.getMinipopId());
//            selectedMiniPop.setNextAvailablePortHintAsNumber(subscriptionCreationVM.getPortId());
//
//            if (service != null && service.getSettingByType(ServiceSettingType.BROADBAND_SWITCH) != null
//                    && selectedMiniPop != null && selectedMiniPop.getPreferredPort() != null
//                    && !selectedMiniPop.checkPort(selectedMiniPop.getPreferredPort())) {
//                log.debug("Cannot create subscription. Minipop's preferred port is invalid. " + selectedMiniPop);
//                throw new BadRequestAlertException("Minipop's port is invalid. Minipop's port is invalid");
//            }
//        } else { //PPPoE
//            if (subscriptionCreationVM.getMinipopId() != null || subscriptionCreationVM.getPortId() != null || selectedMiniPop != null) {
//                log.debug("Cannot create subscription. Minipop/port not needed for PPPoE");
//                throw new BadRequestAlertException("Cannot create subscription. Minipop/port not needed for PPPoE. Cannot create subscription. Minipop/port not needed for PPPoE");
//            }
//        }


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

//            log.debug("Selected minipop: " + selectedMiniPop);
            // Port port = minipopFacade.getAvailablePort(selectedMiniPop);
            // log.debug("Resulting port: " + port);

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

            createSubscription(
                    selectedSubscriber,
                    subscription,
                    String.valueOf(subscriptionCreationVM.getServiceId()),
                    null,
                    false,
                    null,
                    null,
                    0D,
                    selectedCampaign,
                    null,
                    user,
                    subscriptionCreationVM.getServiceTypeId(),
                    subscriptionCreationVM.getSelectedReseller()
            );

            systemLogger.success(SystemEvent.SUBSCRIPTION_CREATED, subscription,"Subscription created");

            try {
                queueManager.addToNotificationsQueue(BillingEvent.SUBSCRIPTION_ADDED, subscription.getId(), null);
                systemLogger.success(SystemEvent.NOTIFICATION_SCHEDULED, subscription, String.format("Spring Event=" + BillingEvent.SUBSCRIPTION_ADDED));
            } catch (Exception ex) {
                log.error("Spring Cannot add to notification queue: ", ex);
                systemLogger.error(SystemEvent.NOTIFICATION_SCHEDULED, subscription, ex.getCause().getMessage());
            }

            return subscription; //????createSubscription metodundan subscription donmelidir

        } catch (DuplicateAgreementException ex) {
            log.error("Cannot create subscription: ", ex);
            throw new BadRequestAlertException("Agreement already exists!");
        }
        catch (Exception ex) {
            log.error("Cannot create subscription: ", ex);
            systemLogger.error(SystemEvent.SUBSCRIPTION_CREATED, subscription, ex.getCause().getMessage());
            throw new BadRequestAlertException("Cannot create subscription");
        }
    }

    @Override
    public Subscription createSubscriptionSpring(Subscriber selectedSubscriber, Subscription subscription, Long serviceId, MiniPop miniPop, boolean isUseStockEquipment, Equipment equipment, List<NotificationSettingRow> notificationSettings, double installationFee, Campaign selectedCampaign, User user, String... additionalProperties) throws NoFreePortLeftException, PortAlreadyReservedException {
        return null;
    }

    @Override
    public void createSubscription(Subscriber selectedSubscriber, Subscription subscription, String serviceId, MiniPop miniPop, boolean isUseStockEquipment, Equipment equipment, List<NotificationSettingRow> notificationSettings, double installationFeeRate, Campaign selectedCampaign, HttpSession session, User user, String... additionalProperties) throws NoFreePortLeftException, PortAlreadyReservedException {
        log.debug("INTSALLATION FEE: " + installationFeeRate);

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
        subscription.setServiceFeeRateWoTax(subscription.getService().getServicePrice()/100000);
        log.debug("the created CNC subscription: " + subscription);

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

        log.debug("subscription.getService().getServiceType() " + subscription.getService().getServiceType());
        if (subscription.getService().getServiceType() == ServiceType.BROADBAND) {
            //MiniPop miniPop = miniPopFacade.find(miniPopId);

            log.debug("PORT: ");

            List<SubscriptionSetting> settingList = new ArrayList<>();

            SubscriptionSetting subscriptionSetting;
            ServiceSetting serviceSetting;

            if (additionalProperties != null &&
                    additionalProperties.length > 0 &&
                    additionalProperties[0] != null &&
                    !additionalProperties[0].isEmpty()) { //serviceType
                subscriptionSetting = new SubscriptionSetting();
                subscriptionSetting.setValue(additionalProperties[0]);
                serviceSetting = commonOperationsEngine.createServiceSetting("SERVICE_TYPE", ServiceSettingType.SERVICE_TYPE, Providers.CNC, ServiceType.BROADBAND, "");
                subscriptionSetting.setProperties(serviceSetting);
                settingFacade.save(subscriptionSetting);
                subscriptionSetting = settingFacade.update(subscriptionSetting);
                settingList.add(subscriptionSetting);
            }

            if (additionalProperties != null &&
                    additionalProperties.length > 1 &&
                    additionalProperties[1] != null &&
                    !additionalProperties[1].isEmpty()) { //dealer
                subscriptionSetting = new SubscriptionSetting();
                subscriptionSetting.setValue(additionalProperties[1]);
                serviceSetting = commonOperationsEngine.createServiceSetting("DEALER", ServiceSettingType.DEALER, Providers.CNC, ServiceType.BROADBAND, "");
                subscriptionSetting.setProperties(serviceSetting);
                settingFacade.save(subscriptionSetting);
                subscriptionSetting = settingFacade.update(subscriptionSetting);
                settingList.add(subscriptionSetting);
            }

            subscriptionSetting = new SubscriptionSetting();
            subscriptionSetting.setValue(subscription.getAgreement());
            serviceSetting = commonOperationsEngine.createServiceSetting("USERNAME", ServiceSettingType.USERNAME, Providers.CNC, ServiceType.BROADBAND, "");
            subscriptionSetting.setProperties(serviceSetting);
            settingFacade.save(subscriptionSetting);
            subscriptionSetting = settingFacade.update(subscriptionSetting);
            settingList.add(subscriptionSetting);

            String password = null;
            if (subscription.getAgreement().contains("-")) {
                password = subscription.getAgreement().split("-")[1];
            }else
            {
                password = subscription.getAgreement();
            }

            subscriptionSetting = new SubscriptionSetting();
            subscriptionSetting.setValue(password);
            serviceSetting = commonOperationsEngine.createServiceSetting("PASSWORD", ServiceSettingType.PASSWORD, Providers.CNC, ServiceType.BROADBAND, "");
            subscriptionSetting.setProperties(serviceSetting);
            settingFacade.save(subscriptionSetting);
            subscriptionSetting = settingFacade.update(subscriptionSetting);
            settingList.add(subscriptionSetting);

            subscription.setSettings(settingList);
            log.debug("USERNAME: " + subscription.getSettingByType(ServiceSettingType.USERNAME));
            log.debug("SERVICE_TYPE: " + subscription.getSettingByType(ServiceSettingType.SERVICE_TYPE));
            log.debug("DEALER: " + subscription.getSettingByType(ServiceSettingType.DEALER));
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
                instFeeCharge.setUser_id((Long) session.getAttribute("userID"));
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

                invoiceFacade.addEquipmentChargeToInvoice(invoice, equipment, subscription, discount, (Long) (session.getAttribute("userID")));
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
                servFeeCharge.setUser_id((Long) session.getAttribute("userID"));
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

            subscription.setBilledUpToDate(DateTime.now().plusDays(
                    billSettings.getSettings().getMaximumPrepaidlifeCycleLength()
            ));

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

        subscription = subscriptionPersistenceFacade.update(subscription);
        commonOperationsEngine.initService(subscription);
    }


    @Override
    public Subscription changeService(Subscription subscription, Service service, boolean upCharge, boolean downCharge) {
        subscription.setService(service);
        subscription.setServiceFeeRateonChange(service.getServicePrice());
        return subscriptionPersistenceFacade.update(subscription);
        //return citynetCommonEngine.changeService(subscription, service, upCharge, false);
    }

    @Override
    public Subscription addVAS(VASCreationParams params) {
        Subscription subscription = params.subscription;
        subscription = subscriptionPersistenceFacade.update(subscription);
        DateTime startDate = params.startDate;
        ValueAddedService vas = params.vas;
        DateTime expiresDate = params.expiresDate;
        IpAddress ipAddress = params.ipAddress;
        double vasCount = params.count;

        if (ipAddress != null) {
            User user = userFacade.findByUserName(ctx.getCallerPrincipal().getName());
            if (startDate == null) {
                startDate = DateTime.now();
            }

            if (vas.getCount() > 0) {
                expiresDate = DateTime.now().plusMonths((int) vas.getCount());
            }
            if (expiresDate == null) {
                expiresDate = register.getDefaultExpirationDate();
            }

            try {
                subscription = addVASWithSetting(subscription, vas, ipAddress, user, startDate, expiresDate, getVasCount(vasCount));
            } catch (Exception ex) {
                log.error(String.format("Cannot add vas id=%d to subscription id=%d", vas.getId(), subscription.getId()), ex);
                ctx.setRollbackOnly();
                systemLogger.error(SystemEvent.CHARGE, subscription, "Charge for vas=" + vas.getName() + " failed");
            }
        }
        return subscription;
    }

    private Subscription addVASWithSetting(
            Subscription subscription,
            ValueAddedService vas,
            IpAddress ipAddress,
            User user,
            DateTime startDate,
            DateTime expireDate,
            double vasCount) throws ProvisionerNotFoundException {
        try {
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


            SubscriptionSetting subscriptionSetting = new SubscriptionSetting();
            subscriptionSetting.setValue(ipAddress.getAddressAsString());//ip address
            ServiceSetting serviceSetting = commonOperationsEngine.createServiceSetting("IP_ADDRESS", ServiceSettingType.IP_ADDRESS, Providers.UNINET, ServiceType.BROADBAND, "");
            subscriptionSetting.setProperties(serviceSetting);
            settingFacade.save(subscriptionSetting);
            subscriptionSetting = settingFacade.update(subscriptionSetting);
            subscription.getSettings().add(subscriptionSetting);

            subscriptionPersistenceFacade.persist(ipAddress);
            log.debug("Ipaddress persisted:" + ipAddress);

            String message = String.format("IP address=%s reserved for subscription id=%d, vas id=%d", ipAddress.getAddressAsString(), subscription.getId(), vas.getId());
            log.debug("addVAS: " + message);
            systemLogger.success(SystemEvent.IP_ADDRESS_RESERVED, subscription, message);

            subscription.addVAS(sbnVAS);

            if (vas.isProvisioned()) {
                ProvisioningEngine provisioner = engineFactory.getProvisioningEngine(subscription);

                message = String.format("Calling provisioner for static ip VAS subscription id=%d, vas id=%d, status=ACTIVE",
                        subscription.getId(), vas.getId());

                if (provisioner.reprovision(subscription)) {
                    systemLogger.success(SystemEvent.VAS_STATUS_ACTIVE, subscription, message);
                } else {
                    systemLogger.error(SystemEvent.VAS_STATUS_ACTIVE, subscription, message);
                    //ctx.setRollbackOnly();
                }
            }

            sbnVAS.setStatus(SubscriptionStatus.ACTIVE);

            if (!vas.isDoPartialCharge()) {
                long rate = (long)(vas.getPrice() * sbnVAS.getCount());
                log.debug(String.format("addVAS: vas id=%d, price=%d, count=%s", vas.getId(),
                        vas.getPrice(), getVasCount(vasCount)));
                //Charge charge = chargeFacade.chargeForVAS(subscription, rate, "Charge for " + vas.getName(), user.getId(), vas);
                Invoice invoice = invoiceFacade.findOpenBySubscriberForCharging(subscription.getSubscriber().getId());

                invoiceFacade.addVasChargeToInvoice(invoice, subscription, rate, vas, user.getId(), String.format("Charge for vas=%s count=%s", vas.getName(), getVasCount(vasCount)));
            }

            queueManager.sendVASNotification(BillingEvent.VAS_ADDED, subscription, vas);
        } catch (Exception ex) {
            log.error(String.format("Cannot add vas id=%d to subscription id=%d", vas.getId(), subscription.getId()), ex);
            ctx.setRollbackOnly();
            systemLogger.error(SystemEvent.CHARGE, subscription, "Charge for vas=" + vas.getName() + " failed");
        }
        return subscription;
    }

    private double getVasCount(double vasCount) {
        if (vasCount == 0) {
            return 1;
        }
        return vasCount;
    }

    @Override
    public Subscription removeVAS(Subscription subscription, SubscriptionVAS vas) {
        try {
            subscription = subscriptionPersistenceFacade.update(subscription);
            vas = subscriptionPersistenceFacade.merge(vas);

            log.debug(String.format("removeVAS: sbnvas id=%d, subscription id=%d", vas.getId(), subscription.getId()));

            String message = String.format("Closing vas. Subscription id=%d, vas id=%d, status=%s",
                    subscription.getId(), vas.getVas().getId(), vas.getStatus());
            log.info("removeVAS: " + message);

            if (subscription.getSettingByType(ServiceSettingType.IP_ADDRESS) != null) {
                String ipAddressAsString = subscription.getSettingByType(ServiceSettingType.IP_ADDRESS).getValue();
                IpAddress ipAddress = ipAddressPersistenceFacade.find(ipAddressAsString);
                subscriptionPersistenceFacade.remove(ipAddress);
                message = String.format("IP address=%s removed from subscription id=%d, vas id=%d", ipAddressAsString, subscription.getId(), vas.getVas().getId());
                log.debug("removeVAS: " + message);
                systemLogger.success(SystemEvent.IP_ADDRESS_UNRESERVED, subscription, message);
            }

            List<SubscriptionSetting> settingList = subscription.getSettings();
            for (Iterator<SubscriptionSetting> settingIterator = settingList.iterator();
                 settingIterator.hasNext(); ) {
                SubscriptionSetting setting = settingIterator.next();
                if (setting.getProperties().getType() == ServiceSettingType.IP_ADDRESS) {
                    log.info(String.format("removing ip setting from subscription id = %d, setting id = %d", subscription.getId(), setting.getId()));
                    settingFacade.delete(setting);
                    settingIterator.remove();
                    break;
                }
            }
            subscription.setSettings(settingList);

            ProvisioningEngine provisioner = engineFactory.getProvisioningEngine(subscription);
            if (provisioner.closeVAS(subscription, vas.getVas())) {
                systemLogger.success(SystemEvent.VAS_STATUS_BLOCK, subscription, message);
            } else {
                systemLogger.error(SystemEvent.VAS_STATUS_BLOCK, subscription, message);
            }

            subscriptionPersistenceFacade.remove(vas);
            log.debug("removeVAS: removing sbn vas id=" + vas.getId());
        } catch (Exception ex) {
            log.error(String.format("Cannot remove VAS subscription: subscription id=%d, service id=%d, vas id=%d", subscription.getId(), subscription.getService().getId(), vas.getId()), ex);
            ctx.setRollbackOnly();
        }
        return subscription;
    }

    @Override
    public Subscription removeVAS(Subscription subscription, ValueAddedService vasID) {
        return citynetCommonEngine.removeVAS(subscription, vasID);
    }

    @Override
    public Subscription editVAS(VasEditParams params) {
        Subscription subscription = params.subscription;
        long sbnVASId = params.sbnVasId;
        IpAddress ipAddressEntity = params.ipAddress;

        try {
            subscription = subscriptionPersistenceFacade.update(subscription);
            SubscriptionVAS vas = subscription.getVASById(sbnVASId);
            String message;

            vas.setCount(params.count);
            subscriptionVASPersistenceFacade.update(vas);

            if (subscription.getSettingByType(ServiceSettingType.IP_ADDRESS) != null) {
                String ipAddressAsString = subscription.getSettingByType(ServiceSettingType.IP_ADDRESS).getValue();
                IpAddress ipAddress = ipAddressPersistenceFacade.find(ipAddressAsString);
                subscriptionPersistenceFacade.remove(ipAddress);
                message = String.format("IP address=%s removed from subscription id=%d, vas id=%d", ipAddressAsString, subscription.getId(), vas.getVas().getId());
                log.debug("edtiVAS: " + message);
                systemLogger.success(SystemEvent.IP_ADDRESS_UNRESERVED, subscription, message);
            }

            log.debug("Manual IP selection: " + ipAddressEntity.getAddressAsString());
            subscriptionPersistenceFacade.persist(ipAddressEntity);
            message = String.format("IP address=%s reserved for subscription id=%d, vas id=%d", ipAddressEntity.getAddressAsString(), subscription.getId(), vas.getVas().getId());
            log.debug("edtiVAS: " + message);
            systemLogger.success(SystemEvent.IP_ADDRESS_RESERVED, subscription, message);
            subscription.getSettingByType(ServiceSettingType.IP_ADDRESS).setValue(ipAddressEntity.getAddressAsString());

            engineFactory.getProvisioningEngine(subscription).reprovision(subscription);
        } catch (Exception ex) {
            ctx.setRollbackOnly();
            log.error(String.format("editVAS: Cannot modify sbnVAS id=%d, subscription id=%d", sbnVASId, subscription.getId()), ex);
        }
        return subscription;
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

        subscription.setExpirationDate(expDate.plusDays(billSettings.getSettings().getMaximumPrepaidlifeCycleLength()));

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
        subscription = subscriptionPersistenceFacade.update(subscription);
        log.debug("ActivatePrepaid, agreement=" + subscription.getAgreement()+ ", "+subscription.toString());

        if (subscription.getStatus() == SubscriptionStatus.INITIAL) {
            subscription.setActivationDate(DateTime.now());
        }

//        DateTime expDate = (subscription.getStatus() == SubscriptionStatus.ACTIVE) ? subscription.getExpirationDate()
//                : DateTime.now();

        DateTime expDate =  DateTime.now();

        if (billSettings.getSettings().getPrepaidlifeCycleLength() == 30) {
            DateTime estimatedNextExpirationDate = expDate.plusMonths(1);
            if (estimatedNextExpirationDate.getDayOfMonth() < expDate.getDayOfMonth()) {
                estimatedNextExpirationDate = estimatedNextExpirationDate.plusDays(1);
            }
            subscription.setExpirationDate(estimatedNextExpirationDate);
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
        //fix to new billing model
//        if (subscription.getBillingModel().getPrinciple() == BillingPrinciple.GRACE_MONTH)
//            subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDate().plusMonths(1));

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
        return subscription;
//        return citynetCommonEngine.prolongPrepaid(subscription);
    }

    @Override
    public boolean activatePostpaid(Subscription subscription) throws ProvisionerNotFoundException {
        return citynetCommonEngine.activatePostpaid(subscription);
    }

    @Override
    public Subscription changeStatus(Subscription subscription, SubscriptionStatus newStatus) {
        return commonOperationsEngine.changeStatus(subscription, newStatus);
    }

    @Override
    public String generateAgreement(AgreementGenerationParams params) {
        return params.ats + "00" + (params.str != null ? params.str.getStreetIndex() : "") + "00" + params.building + "00" + params.apartment;
    }
}
