package com.jaravir.tekila.module.subscription.persistence.management;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.base.persistence.manager.BillingSettingsManager;
import com.jaravir.tekila.base.persistence.manager.Register;
import com.jaravir.tekila.engines.*;
import com.jaravir.tekila.equip.EquipmentPersistenceFacade;
import com.jaravir.tekila.module.accounting.InvoiceState;
import com.jaravir.tekila.module.accounting.entity.*;
import com.jaravir.tekila.module.accounting.manager.*;
import com.jaravir.tekila.module.campaign.*;
import com.jaravir.tekila.module.event.BillingEvent;
import com.jaravir.tekila.module.periiodic.JobPersistenceFacade;
import com.jaravir.tekila.module.queue.PersistentQueueManager;
import com.jaravir.tekila.module.service.*;
import com.jaravir.tekila.module.service.entity.*;
import com.jaravir.tekila.module.service.model.BillingPrinciple;
import com.jaravir.tekila.module.service.persistence.manager.*;
import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.model.BillingModelPersistenceFacade;
import com.jaravir.tekila.module.store.IpAddressPersistenceFacade;
import com.jaravir.tekila.module.store.RangePersistenceFacade;
import com.jaravir.tekila.module.store.equip.Equipment;
import com.jaravir.tekila.module.store.equip.EquipmentStatus;
import com.jaravir.tekila.module.store.ip.StaticIPType;
import com.jaravir.tekila.module.store.ip.persistence.IpAddress;
import com.jaravir.tekila.module.store.ip.persistence.IpAddressRange;
import com.jaravir.tekila.module.subscription.exception.DuplicateAgreementException;
import com.jaravir.tekila.module.subscription.persistence.entity.*;
import com.jaravir.tekila.module.subscription.persistence.entity.reactivation.SubscriptionReactivation;
import com.jaravir.tekila.module.subscription.persistence.entity.transition.StatusChangeRule;
import com.jaravir.tekila.module.subscription.persistence.management.util.CompensationDetails;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.module.system.synchronisers.UninetSynchroniser;
import com.jaravir.tekila.module.web.service.exception.NoSuchSubscriptionException;
import com.jaravir.tekila.provision.broadband.devices.manager.Providers;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import com.jaravir.tekila.provision.broadband.devices.Port;
import com.jaravir.tekila.provision.broadband.devices.exception.NoFreePortLeftException;
import com.jaravir.tekila.provision.broadband.devices.exception.PortAlreadyReservedException;
import com.jaravir.tekila.provision.broadband.devices.manager.MiniPopPersistenceFacade;
import com.jaravir.tekila.provision.broadband.entity.BackProvisionDetails;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import com.jaravir.tekila.provision.exception.ProvisioningException;
import org.apache.log4j.Logger;
import org.apache.poi.ss.formula.functions.T;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import spring.Filters;
import reseller.OnlineSessionObject;
import spring.dto.KapitalSubscriptionDTO;
import spring.exceptions.SubscriptionNotFoundException;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.persistence.*;
import javax.persistence.criteria.*;
import javax.annotation.Resource;
import javax.ejb.*;
import javax.servlet.http.HttpSession;
import javax.xml.registry.infomodel.Slot;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class SubscriptionPersistenceFacade extends AbstractPersistenceFacade<Subscription> {

    //@PersistenceContext(unitName="tekila")
    @PersistenceContext
    private EntityManager em;
    @Resource
    private SessionContext ctx;
    private Subscriber subscriber;

    @EJB
    private Register register;
    @EJB
    private BillingSettingsManager billingSettings;
    @EJB
    private ServiceSettingPersistenceFacade serviceSettingFacade;
    @EJB
    private SubscriptionSettingPersistenceFacade subscriptionSettingFacade;
    @EJB
    private InvoicePersistenceFacade invoiceFacade;
    @EJB
    private ChargePersistenceFacade chargeFacade;
    @EJB
    private TransactionPersistenceFacade transFacade;
    @EJB
    private ServicePersistenceFacade serviceFacade;
    @EJB
    private BillingSettingsManager billSettings;
    @EJB
    private PaymentPersistenceFacade paymentFacade;
    @EJB
    private EquipmentPersistenceFacade equipmentFacade;
    @EJB
    private UserPersistenceFacade userFacade;
    @EJB
    private NotificationSettingPersistenceFacade notifSettingFacade;
    @EJB
    private AccountingTransactionPersistenceFacade accTransFacade;
    @EJB
    private MiniPopPersistenceFacade miniPopFacade;
    @EJB
    private EngineFactory engineFactory;
    @EJB
    private PersistentQueueManager queueManager;
    @EJB
    private SystemLogger systemLogger;
    @EJB
    private RangePersistenceFacade rangeFacade;
    @EJB
    private JobPersistenceFacade jobFacade;
    @EJB
    private SubscriptionReactivationPersistenceFacade reactivationFacade;
    @EJB
    private CampaignPersistenceFacade campaignFacade;
    @EJB
    private CampaignJoinerBean campaignJoinerBean;
    @EJB
    private CampaignRegisterPersistenceFacade campaignRegisterFacade;
    @EJB
    private SubscriptionSettingPersistenceFacade settingFacade;

    @EJB
    private BillingModelPersistenceFacade billingModelFacade;
    @EJB
    private VASPersistenceFacade vasFacade;
    @EJB
    private IpAddressPersistenceFacade ipAddressFacade;
    @EJB
    private CitynetCommonEngine citynetCommonEngine;
    @EJB
    private CommonOperationsEngine commonOperationsEngine;
    @EJB
    private SubscriptionServiceTypePersistenceFacade subscriptionServiceTypePersistenceFacade;

    private final static Logger log = Logger.getLogger(SubscriptionPersistenceFacade.class);
    private final static long CABLE_VAS_CODE = 126;

    public enum Filter implements Filterable {

        AGREEMENT("agreement"),
        IDENTIFIER("identifier"),
        ID("id"),
        FIRSTNAME("subscriber.details.firstName"),
        LASTNAME("subscriber.details.surname"),
        MIDDLENAME("subscriber.details.middleName"),
        CITY_OF_BIRTH("subscriber.details.cityOfBirth"),
        CITIZENSHIP("subscriber.details.citizenship"),
        COUNTRY("subscriber.details.country"),
        PASSPORT_SERIES("subscriber.details.passportSeries"),
        PASSPORT_NUMBER("subscriber.details.passportNumber"),
        PASSPORT_AUTHORITY("subscriber.details.passportAuthority"),
        PASSPORT_VALID("subscriber.details.passportValidTill"),
        EMAIL("subscriber.details.email"),
        PHONE_MOBILE("subscriber.details.phoneMobile"),
        PHONE_ALT("subscriber.details.phoneMobileAlt"),
        PHONE_LANDLINE("subscriber.details.phoneLandline"),
        ADDRESS_CITY("subscriber.details.city"),
        ADDRESS_ATS("subscriber.details.ats"),
        ADDRESS_STREET("subscriber.details.street"),
        ADDRESS_BUILDING("subscriber.details.building"),
        ADDRESS_APARTMENT("subscriber.details.apartment"),
        CORPORATE_COMPANY("subscriber.details.companyName"),
        CORPORATE_COMPANY_TYPE("subscriber.details.bankAccount"),
        DATE_OF_BIRTH("subscriber.details.dateOfBirth"),
        CREATED_ON("creationDate"),
        ENTRY_DATE("entryDate"),
        EQUIPMENT_PARTNUMBER("value"),
        MINIPOP_MAC_ADDRESS("value"),
        MINIPOP_PORT("value"),
        SETTING_TYPE("properties.type"),
        SUBSCRIBER_NAME("subscriber.details.firstName"),
        SUBSCRIBER_MIDDLENAME("subscriber.details.middleName"),
        SUBSCRIBER_SURNAME("subscriber.details.surname"),
        SUBSCRIBER_TYPE("subscriber.details.type"),
        SERVICE("service.name"),
        USERNAME("settings.value"),
        IP("capacity"),
        BUCKET_TYPE("type"),
        STATUS("status"),
        PROVIDER("service.provider");

        private final String field;
        private MatchingOperation operation;

        Filter(String field) {
            this.field = field;
            this.operation = MatchingOperation.LIKE;
        }

        public String getField() {
            return field;
        }

        @Override
        public MatchingOperation getOperation() {
            return operation;
        }

        public void setOperation(MatchingOperation operation) {
            this.operation = operation;
        }
    }

    public SubscriptionPersistenceFacade() {
        super(Subscription.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return this.em;
    }

    @Override
    public void save(Subscription sub) {
        sub.setCreationDate(DateTime.now().toDate());
        sub.synchronizeExpiratioDates();
        super.save(sub);
    }

    @Override
    public Subscription update(Subscription sub) {
        try {
            sub.getNotificationSettings().stream().forEach(k -> log.debug(k));
        } catch (Exception e) {

        }
        sub.synchronizeExpiratioDates();
        return super.update(sub);
        /*
        boolean res = false;
        Subscription subscription = find(sub.getId());
        try {
            ProvisioningEngine provisioner = engineFactory.getProvisioningEngine(sub);
            res = provisioner.updateAccount(sub);
        } catch (ProvisionerNotFoundException e) {
            e.printStackTrace();
        }
        if(res) {
            sub.synchronizeExpiratioDates();
            subscription = super.update(sub);
        }
        return subscription;
         */
    }

    public Subscription update(Subscription subscription, List<NotificationSettingRow> notificationSettings, MiniPop miniPop, Equipment equipment)
            throws NoFreePortLeftException, PortAlreadyReservedException, ProvisioningException {
        NotificationSetting setting = null;
        //srv.setNotificationSettings(null);
        if (notificationSettings != null && !notificationSettings.isEmpty()) {
            for (NotificationSettingRow row : notificationSettings) {
                setting = subscription.getNotificationSettingByEvent(row.getEvent());

                if (setting != null) { //update setting
                    if (row.getSelectedChannelList() != null && !row.getSelectedChannelList().isEmpty()) {
                        //setting.setChannelList(row.getSelectedChannelListAsChannels());
                        setting = notifSettingFacade.find(row.getEvent(), row.getSelectedChannelListAsChannels());
                        subscription.updateNotificationSetting(setting);
                    } else { //none selected - remove the setting
                        subscription.getNotificationSettings().remove(setting);
                        //notifSettingFacade.updateAndDelete(setting);
                    }
                } else if (row.getSelectedChannelList() != null && !row.getSelectedChannelList().isEmpty()) {
                    setting = notifSettingFacade.find(row.getEvent(), row.getSelectedChannelListAsChannels());
                    subscription.addNotification(setting);
                }
                setting = null;
            }
        } //end notification settings update
        //change miniPop
        if (subscription.getService().getServiceType() == ServiceType.BROADBAND && miniPop != null) {
            changeMinipop(subscription, miniPop);
        } //END MINIPOP CHANGE

        if (subscription.getSettingByType(ServiceSettingType.TV_EQUIPMENT) != null && equipment != null) {
            changeEquipmentForTV(subscription, equipment);
        }

        if (subscription.getBillingModel() != null && subscription.getExpirationDate() != null) {
            subscription.setExpirationDateWithGracePeriod(
                    subscription.getExpirationDate().plusDays(subscription.getBillingModel().getGracePeriodInDays()));
        }
        return update(subscription);
    }

    public Subscription update(Subscription subscription, List<NotificationSetting> notificationSettings) {
        NotificationSetting setting = null;
        if (notificationSettings != null) {
            for (NotificationSetting s : notificationSettings) {
                setting = subscription.getNotificationSettingByEvent(s.getEvent());
                if (setting != null) {
                    setting.getChannelList().stream().forEach(notificationChannell -> log.debug(notificationChannell));//update setting
                    if (s.getChannelList() != null && !s.getChannelList().isEmpty()) {
                        setting = notifSettingFacade.find(s.getEvent(), s.getChannelList());
                        subscription.updateNotificationSetting(setting);
                    } else {
                        subscription.removeNotificationSetting(setting.getEvent());
                    }
                } else if (s.getChannelList() != null && !s.getChannelList().isEmpty()) {
                    setting = notifSettingFacade.find(s.getEvent(), s.getChannelList());
                    subscription.addNotification(setting);
                }
            }
        }
        return update(subscription);
    }

    public void changeEquipmentForTV(Subscription subscription, Equipment equipment) {
        String oldEquipmentPartNumber = subscription.getSettingByType(ServiceSettingType.TV_EQUIPMENT).getValue();
        String logHeader = String.format("changeEquipmentTV: subscription id=%d, agreement=%s, equipment id=%d, part number=%s",
                subscription.getId(), subscription.getAgreement(), equipment.getId(), equipment.getPartNumber());
        log.info(String.format("%s", logHeader));
        Equipment oldEquipment = null;
        try {
            oldEquipment = equipmentFacade.findByPartNumber(oldEquipmentPartNumber);
            EquipmentStatus oldEquipmentOldStatus = oldEquipment.getStatus();
            oldEquipment.free();

            log.info(
                    String.format("update: changed equipment.id=%d, status changed from %s to %s, was on subscription.id=%d, setting=%s",
                            oldEquipment.getId(), oldEquipmentOldStatus, oldEquipment.getStatus(), subscription.getId(), subscription.getSettingByType(ServiceSettingType.TV_EQUIPMENT))
            );
            systemLogger.success(
                    SystemEvent.EQUIPMENT_UNRESERVED,
                    subscription, String.format("equipment.id=%d, status changed from %s to %s",
                            oldEquipment.getId(), oldEquipmentOldStatus, oldEquipment.getStatus())
            );
        } catch (Exception ex) {
            log.error(String.format("Cannot find equipment partNumber=%s subscription id=%d", oldEquipmentPartNumber, subscription.getId()), ex);
        }

        subscription.setSettingByType(ServiceSettingType.TV_EQUIPMENT, equipment.getPartNumber());
        EquipmentStatus oldEquipmentStatus = equipment.getStatus();
        equipment.reserve();
        equipmentFacade.update(equipment);

        log.debug("update: equipment after update: " + equipment);

        log.info(
                String.format("update: subscription.id=%d, changed status of subscription setting=%s, old status was %s",
                        subscription.getId(), subscription.getSettingByType(ServiceSettingType.TV_EQUIPMENT), oldEquipmentStatus)
        );

        systemLogger.success(
                SystemEvent.SUBSCRIPTION_SETTING_MODIFIED,
                subscription, String.format("setting.id=%d changed from %s to %s",
                        subscription.getSettingByType(ServiceSettingType.TV_EQUIPMENT).getId(),
                        oldEquipmentPartNumber, subscription.getSettingByType(ServiceSettingType.TV_EQUIPMENT).getValue())
        );

        log.info(
                String.format("update: changed equipment.id=%d, status changed from %s to %s, assigned to subscription.id=%d, setting=%s",
                        equipment.getId(), oldEquipmentStatus, equipment.getStatus(), subscription.getId(), subscription.getSettingByType(ServiceSettingType.TV_EQUIPMENT))
        );

        systemLogger.success(
                SystemEvent.EQUIPMENT_RESERVED,
                subscription, String.format("equipment.id=%d, status changed from %s to %s",
                        equipment.getId(), oldEquipmentStatus, equipment.getStatus())
        );

        boolean res = false;

        try {
            ProvisioningEngine tvProvisioner = engineFactory.getProvisioningEngine(subscription);
            res = tvProvisioner.changeEquipment(subscription, equipment.getPartNumber());

            log.info(String.format("update: agreement=%s, INIT SERVICE returned %b", subscription.getAgreement(), res));

            res = false;

            if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
                res = tvProvisioner.openService(subscription);
            }

            log.debug(String.format("oldEquipment brand=%s, equipment brand=%s", oldEquipment.getBrand().getName(), equipment.getBrand().getName()));
            if (!equipment.getBrand().getName().equals(oldEquipment.getBrand().getName()) && equipment.getBrand().getName().toLowerCase().contains("sandmartin")
                    && !oldEquipment.getBrand().getName().toLowerCase().contains("sandmartin")) {
                Invoice invoice = invoiceFacade.createInvoiceForSubscriber(subscription.getSubscriber());
                long rate = 9 * 100000;
                String userName = ctx.getCallerPrincipal().getName();
                long userID = userFacade.findByUserName(userName).getId();
                Charge servFeeCharge = new Charge();
                servFeeCharge.setService(subscription.getService());
                servFeeCharge.setAmount(rate);
                servFeeCharge.setSubscriber(subscription.getSubscriber());
                servFeeCharge.setUser_id(userID);
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

            } else {
                res = tvProvisioner.initService(subscription);
            }

            if (res) {
                log.info(String.format("update: agreement=%s, provisioning successfull", subscription.getAgreement()));
                systemLogger.success(SystemEvent.PROVISIONING, subscription,
                        String.format("deviceID=%s", equipment.getPartNumber()));
            }
        } catch (Exception ex) {
            log.error(String.format("update: agreement=%s, cannot OPEN SERVICE", subscription.getAgreement()), ex);
        }

        if (res && subscription.getStatus() == SubscriptionStatus.INITIAL && subscription.getBalance().getRealBalance() >= 0) {
            engineFactory.getOperationsEngine(subscription).activatePrepaid(subscription);
        }

        if (!res) {
            log.error(String.format("update: agreement=%s, provisioning unsuccessfull", subscription.getAgreement()));
            systemLogger.success(SystemEvent.PROVISIONING, subscription,
                    String.format("deviceID=%s", equipment.getPartNumber()));
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Subscription changeMinipop(Subscription subscription, MiniPop miniPop) {
        String header = String.format("changeMinipop: subscription id=%d, agreement=%s, miniPop id=%d", subscription.getId(), subscription.getAgreement(), miniPop.getId());

        try {
            //MiniPop miniPop = miniPopFacade.find(miniPopId);
            //miniPop = miniPopFacade.update(miniPop);
            Port port = miniPopFacade.getAvailablePort(miniPop);

            log.debug(String.format("%s: received minipop=%s", header, miniPop));

            SubscriptionSetting minipopSetting = subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH);
            SubscriptionSetting minipopPort = subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH_PORT);

            MiniPop oldMiniPop = null;

            if (minipopSetting != null && !minipopSetting.equals("-")) {
                miniPopFacade.clearFilters();
                try {
                    oldMiniPop = miniPopFacade.find(Long.valueOf(minipopSetting.getValue()));
                    if (oldMiniPop != null && minipopPort != null) {
                        oldMiniPop.free(Integer.valueOf(minipopPort.getValue()));
                    }
                } catch (Exception ex) {
                    log.error(String.format("%s: cannot parse minipopSetting %s", header, minipopSetting.getValue()));
                }
            }

            //if (port != null) return;
            String oldUsername;
            if (subscription.getService().getProvider().getId() != Providers.UNINET.getId())
                oldUsername = subscription.getSettingByType(ServiceSettingType.USERNAME).getValue();
            else {
                oldUsername = subscription.getAgreement();
            }
            String oldMinipopID = subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH) != null ?
                    subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH).getValue() : "";
            String oldPort = subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH_PORT) != null ?
                    subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH_PORT).getValue() : "";

            String newUsername;
            if (subscription.getService().getProvider().getId() == Providers.AZERTELECOM.getId() ||
                    subscription.getService().getProvider().getId() == Providers.DATAPLUS.getId()) {
                newUsername = "Ethernet0/0/" + String.valueOf(port.getNumber()) + ":" + miniPop.getMac();
            } else if (subscription.getService().getProvider().getId() == Providers.UNINET.getId()) {
                newUsername = subscription.getAgreement();
            } else {
                newUsername = miniPop.getMasterVlan().toString() + "-" + (miniPop.getSubVlan() + port.getNumber());
            }
            if (subscription.getService().getProvider().getId() != Providers.UNINET.getId())
                subscription.getSettingByType(ServiceSettingType.USERNAME).setValue(
                        //"Ethernet0/0/5:4846-fbe9-b9f0" + DateTime.now().toString(DateTimeFormat.forPattern("yyyyMMddHHmmss")) +"@narhome");
                        newUsername
                );

            if (subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH) == null) {
                addServiceSetting(subscription, ServiceSettingType.BROADBAND_SWITCH, String.valueOf(miniPop.getId()));
            } else {
                subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH).setValue(String.valueOf(miniPop.getId()));
            }

            if (subscription.getService().getProvider().getId() != Providers.UNINET.getId() &&
                    subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH_PORT) != null) {
                subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH_PORT).setValue(String.valueOf(port.getNumber()));
            }

            if (subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH_IP) == null) {
                addServiceSetting(subscription, ServiceSettingType.BROADBAND_SWITCH_IP, String.valueOf(miniPop.getIp()));
            } else {
                subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH_IP).setValue(String.valueOf(miniPop.getIp()));
            }

            subscription = update(subscription);
            ProvisioningEngine provisioner = engineFactory.getProvisioningEngine(subscription);
            provisioner.changeEquipment(subscription, newUsername);

            String msg = String.format("old minipop id=%s, oldPort=%s, new minipop id=%d, new port=%d, old username=%s, new username=%s",
                    oldMinipopID, oldPort, miniPop.getId(), port.getNumber(), oldUsername, newUsername);
            log.info(String.format("%s: %s, %s", header, "minipop / port changed successfully", msg));
            systemLogger.success(SystemEvent.MINIPOP_RESERVED, subscription, msg);
            try {
                if (miniPop.getId() != Long.valueOf(oldMinipopID)) {
                    systemLogger.success(SystemEvent.MINIPOP_UNRESERVED, subscription, msg);
                }
            } catch (Exception ex) {
                log.error(String.format("%s: cannot parse oldMinipopSetting %s", header, oldMinipopID));
            }
            return subscription;
        } catch (ProvisionerNotFoundException ex) {
            ctx.setRollbackOnly();
            log.error(String.format("%s: cannot find provisioner", header), ex);
            systemLogger.error(SystemEvent.PROVISIONING, subscription, String.format("minipop id=%d, %s", miniPop.getId(), ex.getMessage()));
        } catch (NoFreePortLeftException | PortAlreadyReservedException ex) {
            ctx.setRollbackOnly();
            log.error(String.format("%s: cannot reserve port", header), ex);
            systemLogger.error(SystemEvent.MINIPOP_RESERVED, subscription, String.format("minipop id=%d, %s", miniPop.getId(), ex.getMessage()));
        } catch (ProvisioningException ex) {
            ctx.setRollbackOnly();
            log.error(String.format("%s: cannot provision", header), ex);
            systemLogger.error(SystemEvent.PROVISIONING, subscription, String.format("minipop id=%d, %s", miniPop.getId(), ex.getMessage()));
        } catch (Exception ex) {
            ctx.setRollbackOnly();
            log.error(String.format("%s: cannot change minipop", header), ex);
            systemLogger.error(SystemEvent.MINIPOP_RESERVED, subscription, String.format("minipop id=%d, %s", miniPop.getId(), ex.getMessage()));
        }
        return null;
    }

    private void addServiceSetting(Subscription subscription, ServiceSettingType settingType, String settingValue) {
        SubscriptionSetting subscriptionSetting = new SubscriptionSetting();

        subscriptionSetting.setValue(settingValue);//INFO
        ServiceSetting serviceSetting = commonOperationsEngine.createServiceSetting("", settingType, Providers.UNINET, ServiceType.BROADBAND, "");
        subscriptionSetting.setProperties(serviceSetting);
        settingFacade.save(subscriptionSetting);
        subscriptionSetting = settingFacade.update(subscriptionSetting);
        subscription.getSettings().add(subscriptionSetting);
    }

    public Subscription find(Long pk, LockModeType lockModeType) {
        return em.find(Subscription.class, pk, lockModeType);
    }

    public Subscription findByCustomerIdentifier(String identifier) {

        return this.getEntityManager()
                .createQuery("select s from Subscription s where s.identifier = :ident", Subscription.class)
                .setParameter("ident", identifier).getSingleResult();

    }

    public void updateWithResources(Subscription sub) {
        for (SubscriptionResource res : sub.getResources()) {
            for (SubscriptionResourceBucket buck : res.getBucketList()) {
                em.persist(buck);
            }
            em.persist(res);
        }
        this.update(sub);
    }

    public List<Subscription> findAllExpired() {
        return em.createQuery("select s from Subscription s where s.expirationDate < CURRENT_TIMESTAMP").getResultList();
    }

    public List<Subscription> findAllPostpaidForBilling() {
        return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
                        + "where owner.lifeCycle =:lifeCycle and sub.status != :status "
                        + "and sub.activationDate IS NOT NULL "
                        + "and (sub.billedUpToDate <= CURRENT_TIMESTAMP or sub.billedUpToDate IS NULL)",
                Subscription.class)
                .setParameter("lifeCycle", SubscriberLifeCycleType.POSTPAID)
                .setParameter("status", SubscriptionStatus.FINAL)
                .getResultList();
    }

    public List<Subscription> findAllDataPlusFirstMonthCampaignProblem() {
        return em.createQuery("select sub from Subscription sub where " +
                "sub.status = :sts " +
                "and sub.service.provider.id = :pid " +
                "and sub.billedUpToDate > CURRENT_TIMESTAMP ", Subscription.class)
                .setParameter("sts", SubscriptionStatus.BLOCKED)
                .setParameter("pid", Providers.DATAPLUS.getId())
                .getResultList();
    }

    public List<Subscription> findAllToResurrect() {
        log.info("recsurrect selecting ");
        return em.createQuery("select sub from Subscription sub join sub.subscriber owner where owner.lifeCycle =:lifeCycle " +
                "and sub.billedUpToDate between :startLimit and :expireLimit " +
                "and sub.status <> :final and sub.status <> :cancel " +
                "and sub.status <> :prefinal ", Subscription.class)
                .setParameter("lifeCycle", SubscriberLifeCycleType.PREPAID)
                .setParameter("expireLimit", DateTime.now())
                .setParameter("startLimit", DateTime.now().minusDays(2))
                .setParameter("final", SubscriptionStatus.FINAL)
                .setParameter("cancel", SubscriptionStatus.CANCEL)
                .setParameter("prefinal", SubscriptionStatus.PRE_FINAL)
                .getResultList();
    }

    public List<Subscription> findAllDataplusToFix() {
        return em.createQuery("select sub from Subscription sub join sub.subscriber owner where owner.lifeCycle =:lifeCycle and sub.billedUpToDate between :startLimit and :expireLimit and sub.status <> :final and sub.status <> :cancel and sub.status <> :prefinal" +
                " and sub.service.provider.id = :dataplusId", Subscription.class)
                .setParameter("lifeCycle", SubscriberLifeCycleType.PREPAID)
                .setParameter("expireLimit", DateTime.now().plusDays(1))
                .setParameter("startLimit", DateTime.now().minusDays(1))
                .setParameter("final", SubscriptionStatus.FINAL)
                .setParameter("cancel", SubscriptionStatus.CANCEL)
                .setParameter("prefinal", SubscriptionStatus.PRE_FINAL)
                .setParameter("dataplusId", Providers.DATAPLUS.getId())
                .getResultList();
    }

    public List<Subscription> findAllToDecharge() {
        return em.createQuery("select sub from Subscription sub join sub.subscriber owner where owner.lifeCycle =:lifeCycle and sub.billedUpToDate between :startLimit and :expireLimit and sub.expirationDate between :startLimit and :expireLimit and sub.status <> :final and sub.status <> :cancel and sub.status <> :prefinal", Subscription.class)
                .setParameter("lifeCycle", SubscriberLifeCycleType.PREPAID)
                .setParameter("expireLimit", DateTime.now().minusDays(1))
                .setParameter("startLimit", DateTime.now().minusDays(2))
                .setParameter("final", SubscriptionStatus.FINAL)
                .setParameter("cancel", SubscriptionStatus.CANCEL)
                .setParameter("prefinal", SubscriptionStatus.PRE_FINAL)
                .getResultList();
    }

    public List<Subscription> findAllPrepaidForBillingAzertelekom() {
        return em.createQuery("select sub from Subscription sub join sub.subscriber owner " +
                "where sub.service.provider.id = :provider and owner.lifeCycle =:lifeCycle and sub.billedUpToDate between :startLimit and :expireLimit and sub.status <> :final and sub.status <> :cancel and sub.status <> :prefinal", Subscription.class)
                .setParameter("provider", Providers.AZERTELECOM.getId())
                .setParameter("lifeCycle", SubscriberLifeCycleType.PREPAID)
                .setParameter("expireLimit", DateTime.now().plusDays(billingSettings.getSettings().getPrepaidNumDaysToBillBeforeExpiration()))
                .setParameter("startLimit", DateTime.now().minusDays(billingSettings.getSettings().getPrepaidNumDaysToBillAfterExpiration()))
                .setParameter("final", SubscriptionStatus.FINAL)
                .setParameter("cancel", SubscriptionStatus.CANCEL)
                .setParameter("prefinal", SubscriptionStatus.PRE_FINAL)
                .getResultList();
    }



    public List<Subscription> findAllPrepaidForBillingDGC() {
        List<Long> providers = Arrays.asList(Providers.DATAPLUS.getId(), Providers.CNC.getId(), Providers.GLOBAL.getId());
        return em.createQuery("select sub from Subscription sub join sub.subscriber owner " +
                "where sub.service.provider.id IN :provider " +
                "and owner.lifeCycle =:lifeCycle " +
                "and sub.billedUpToDate between :startLimit and :expireLimit and sub.status <> :final and sub.status <> :cancel and sub.status <> :prefinal", Subscription.class)
                .setParameter("provider", providers)
                .setParameter("lifeCycle", SubscriberLifeCycleType.PREPAID)
                .setParameter("expireLimit", DateTime.now().plusDays(billingSettings.getSettings().getPrepaidNumDaysToBillBeforeExpiration()))
                .setParameter("startLimit", DateTime.now().minusDays(billingSettings.getSettings().getPrepaidNumDaysToBillAfterExpiration()))
                .setParameter("final", SubscriptionStatus.FINAL)
                .setParameter("cancel", SubscriptionStatus.CANCEL)
                .setParameter("prefinal", SubscriptionStatus.PRE_FINAL)
                .getResultList();
    }


    public List<Subscription> findAllPrepaidForBillingNonAzertelekom() {
        List<Long> providers = Arrays.asList(Providers.AZERTELECOM.getId(), Providers.DATAPLUS.getId(), Providers.CNC.getId(),
                Providers.GLOBAL.getId());

        return em.createQuery("select sub from Subscription sub join sub.subscriber owner " +
                "where sub.service.provider.id NOT IN :providerAzT " +
                "and owner.lifeCycle =:lifeCycle " +
                "and sub.billedUpToDate between :startLimit and :expireLimit " +
                "and sub.status <> :final " +
                "and sub.status <> :cancel " +
                "and sub.status <> :prefinal", Subscription.class)
                .setParameter("providerAzT", providers)
                .setParameter("lifeCycle", SubscriberLifeCycleType.PREPAID)
                .setParameter("expireLimit", DateTime.now().plusDays(billingSettings.getSettings().getPrepaidNumDaysToBillBeforeExpiration()))
                .setParameter("startLimit", DateTime.now().minusDays(billingSettings.getSettings().getPrepaidNumDaysToBillAfterExpiration()))
                .setParameter("final", SubscriptionStatus.FINAL)
                .setParameter("cancel", SubscriptionStatus.CANCEL)
                .setParameter("prefinal", SubscriptionStatus.PRE_FINAL)
                .getResultList();
    }

    public List<Subscription> findAllExpiredPostpaid() {
        return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
                + "where owner.lifeCycle =:lifeCycle and sub.status = :status and "
                + "sub.expirationDateWithGracePeriod <= CURRENT_TIMESTAMP", Subscription.class)
                .setParameter("lifeCycle", SubscriberLifeCycleType.POSTPAID)
                .setParameter("status", SubscriptionStatus.ACTIVE)
                .getResultList();
    }

    public List<Subscription> findAllExpiredPrepaidNonAzertelekom() {


        List<Long> providers = Arrays.asList(Providers.DATAPLUS.getId(), Providers.CNC.getId(),
                                             Providers.GLOBAL.getId(), Providers.AZERTELECOM.getId());


        return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
                + "where sub.service.provider.id NOT IN :provider and owner.lifeCycle =:lifeCycle and sub.status = :status and "
                + "sub.expirationDate <= CURRENT_TIMESTAMP"
                + " and owner.isBilledByLifeCycle = :bill_by"
//                + " and sub.agreement <> :agr"
                , Subscription.class)
                .setParameter("provider", providers)
                .setParameter("lifeCycle", SubscriberLifeCycleType.PREPAID)
                .setParameter( "status", SubscriptionStatus.ACTIVE)
                .setParameter("bill_by", Boolean.TRUE)
                .getResultList();
    }

    public List<Subscription> findAllExpiredPrepaidDPGB() {

        List<Long> providers = Arrays.asList(Providers.DATAPLUS.getId(), Providers.GLOBAL.getId());

        return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
                        + "where sub.service.provider.id IN :provider and owner.lifeCycle =:lifeCycle and sub.status = :status and "
                        + "sub.expirationDate <= CURRENT_TIMESTAMP"
                        + " and owner.isBilledByLifeCycle = :bill_by"
//                + " and sub.agreement <> :agr"
                , Subscription.class)
                .setParameter("provider", providers)
                .setParameter("lifeCycle", SubscriberLifeCycleType.PREPAID)
                .setParameter( "status", SubscriptionStatus.ACTIVE)
                .setParameter("bill_by", Boolean.TRUE)
                .getResultList();
    }


    public List<Subscription> findAllExpiredPrepaidCNC() {

        List<Long> providers = Arrays.asList(Providers.CNC.getId());

        return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
                        + "where sub.service.provider.id IN :provider and owner.lifeCycle =:lifeCycle and sub.status = :status and "
                        + "sub.expirationDate <= CURRENT_TIMESTAMP"
                        + " and owner.isBilledByLifeCycle = :bill_by"
//                + " and sub.agreement <> :agr"
                , Subscription.class)
                .setParameter("provider", providers)
                .setParameter("lifeCycle", SubscriberLifeCycleType.PREPAID)
                .setParameter( "status", SubscriptionStatus.ACTIVE)
                .setParameter("bill_by", Boolean.TRUE)
                .getResultList();
    }

    public List<Subscription> findAllExpiredPrepaidAzertelekom() {
        return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
                + "where sub.service.provider.id = :provider and owner.lifeCycle =:lifeCycle and sub.status = :status and "
                + "sub.expirationDate <= CURRENT_TIMESTAMP"
                + " and owner.isBilledByLifeCycle = :bill_by", Subscription.class)
                .setParameter("provider", Providers.AZERTELECOM.getId())
                .setParameter("lifeCycle", SubscriberLifeCycleType.PREPAID)
                .setParameter("status", SubscriptionStatus.ACTIVE)
                .setParameter("bill_by", Boolean.TRUE)
                .getResultList();
    }

    public List<Subscription> findAllExpiredPrepaidForNotification() {
        Calendar date = Calendar.getInstance();
        date.add(Calendar.DAY_OF_MONTH, 1);

        return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
                + "where owner.lifeCycle =:lifeCycle and sub.status = :status and "
                + "sub.expirationDate <= :changeDate"
                + " and owner.isBilledByLifeCycle = :bill_by", Subscription.class)
                .setParameter("lifeCycle", SubscriberLifeCycleType.PREPAID)
                .setParameter("status", SubscriptionStatus.ACTIVE)
                .setParameter("bill_by", Boolean.TRUE)
                .setParameter("changeDate", new DateTime(date.getTime()))
                .getResultList();
    }

    public List<Subscription> findAllExpiredWithGracePrepaidNonAzertelekom() {

        List<Long> providers = Arrays.asList(Providers.DATAPLUS.getId(), Providers.CNC.getId(),
                                            Providers.GLOBAL.getId(),Providers.AZERTELECOM.getId());


        return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
                + "where sub.service.provider.id NOT IN :provider and owner.lifeCycle =:lifeCycle and sub.status = :status and "
                + "sub.expirationDateWithGracePeriod <= CURRENT_TIMESTAMP "
                + "and owner.isBilledByLifeCycle = :bill_by", Subscription.class)
                .setParameter("provider", providers)
                .setParameter("lifeCycle", SubscriberLifeCycleType.PREPAID)
                .setParameter("status", SubscriptionStatus.PARTIALLY_BLOCKED)
                .setParameter("bill_by", Boolean.TRUE)
                .getResultList();
    }

    public List<Subscription> findAllExpiredWithGracePrepaidAzertelekom() {
        return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
                + "where sub.service.provider.id = :provider and owner.lifeCycle =:lifeCycle and sub.status = :status and "
                + "sub.expirationDateWithGracePeriod <= CURRENT_TIMESTAMP"
                + " and owner.isBilledByLifeCycle = :bill_by", Subscription.class)
                .setParameter("provider", Providers.AZERTELECOM.getId())
                .setParameter("lifeCycle", SubscriberLifeCycleType.PREPAID)
                .setParameter("status", SubscriptionStatus.PARTIALLY_BLOCKED)
                .setParameter("bill_by", Boolean.TRUE)
                .getResultList();
    }

    public List<Subscription> findAllExpiredWithGracePrepaidForNotification() {
        Calendar date = Calendar.getInstance();
        date.add(Calendar.DAY_OF_MONTH, 1);
        Date leftDate = date.getTime();

        date.add(Calendar.DAY_OF_MONTH, 1);
        Date rightDate = date.getTime();

        return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
                + "where owner.lifeCycle =:lifeCycle and sub.status = :status and "
                + "sub.expirationDateWithGracePeriod >= :leftDate and "
                + "sub.expirationDateWithGracePeriod <= :rightDate and "
                + "owner.isBilledByLifeCycle = :bill_by", Subscription.class)
                .setParameter("lifeCycle", SubscriberLifeCycleType.PREPAID)
                .setParameter("status", SubscriptionStatus.PARTIALLY_BLOCKED)
                .setParameter("bill_by", Boolean.TRUE)
                .setParameter("leftDate", new DateTime(leftDate))
                .setParameter("rightDate", new DateTime(rightDate))
                .getResultList();
    }

    public List<Subscription> findAllExpiredWithContinuousPrepaidForNotification() {
        Calendar date = Calendar.getInstance();
        date.add(Calendar.DAY_OF_MONTH, 1);
        Date leftDate = date.getTime();

        date.add(Calendar.DAY_OF_MONTH, 1);
        Date rightDate = date.getTime();
//List<Long> providers = Arrays.asList(Providers.DATAPLUS.getId(), Providers.CNC.getId(), Providers.GLOBAL.getId());


//        return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
//                + "where sub.agreement = :agree", Subscription.class)
//                .setParameter("agree", "testtest0124")
//                .getResultList();


        return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
                + "where owner.lifeCycle =:lifeCycle and sub.status = :status and "
                + "sub.expirationDateWithGracePeriod >= :leftDate and "
                + "sub.expirationDateWithGracePeriod <= :rightDate and " +
                " sub.service.provider.id = :provider and "
                + " owner.isBilledByLifeCycle = :bill_by", Subscription.class)
                .setParameter("lifeCycle", SubscriberLifeCycleType.PREPAID)
                .setParameter("status", SubscriptionStatus.ACTIVE)
                .setParameter("bill_by", Boolean.TRUE)
                .setParameter("provider", Providers.DATAPLUS.getId())
                .setParameter("leftDate", new DateTime(leftDate))
                .setParameter("rightDate", new DateTime(rightDate))
                .getResultList();
    }




    public List<Subscription> findAllExpiredGlobalAndCNCForNotification() {
        Calendar date = Calendar.getInstance();
        date.add(Calendar.DAY_OF_MONTH, 2);
        Date leftDate = date.getTime();

        date.add(Calendar.DAY_OF_MONTH, 1);
        Date rightDate = date.getTime();
List<Long> providers = Arrays.asList(Providers.CNC.getId(), Providers.GLOBAL.getId());

//List<String> testusers = Arrays.asList("testCNC");



//        return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
//                + "where sub.agreement in :agree", Subscription.class)
//                .setParameter("agree", testusers)
//                .getResultList();


        return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
                + "where owner.lifeCycle =:lifeCycle and sub.status = :status and "
                + "sub.expirationDateWithGracePeriod >= :leftDate and "
                + "sub.expirationDateWithGracePeriod <= :rightDate and " +
                " sub.service.provider.id in :provider and "
                + " owner.isBilledByLifeCycle = :bill_by", Subscription.class)
                .setParameter("lifeCycle", SubscriberLifeCycleType.PREPAID)
                .setParameter("status", SubscriptionStatus.ACTIVE)
                .setParameter("bill_by", Boolean.TRUE)
                .setParameter("provider", providers)
                .setParameter("leftDate", new DateTime(leftDate))
                .setParameter("rightDate", new DateTime(rightDate))
                .getResultList();
    }


    public List<Subscription> findAllExpiredWithBLOCKCOUNTandPARTBLOCKforUNICITY() {
        Calendar date = Calendar.getInstance();
        date.add(Calendar.DAY_OF_MONTH, 4);
        Date leftDate = date.getTime();

        date.add(Calendar.DAY_OF_MONTH, 1);
        Date rightDate = date.getTime();

        return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
                + "where owner.lifeCycle =:lifeCycle and sub.status = :status and "
                + "sub.expirationDateWithGracePeriod >= :leftDate and "
                + "sub.expirationDateWithGracePeriod <= :rightDate and "
                + "owner.isBilledByLifeCycle = :bill_by", Subscription.class)
                .setParameter("lifeCycle", SubscriberLifeCycleType.PREPAID)
                .setParameter("status", SubscriptionStatus.ACTIVE)
                .setParameter("bill_by", Boolean.TRUE)
                .setParameter("leftDate", new DateTime(leftDate))
                .setParameter("rightDate", new DateTime(rightDate))
                .getResultList();
    }

    public List<Subscription> findAllExpiredWithSoonPartBlock() {
        Calendar date = Calendar.getInstance();
        date.add(Calendar.DAY_OF_MONTH, 4);
        Date leftDate = date.getTime();

        date.add(Calendar.DAY_OF_MONTH, 1);
        Date rightDate = date.getTime();

//        return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
//                + "where  "
//                + "sub.agreement = :agree ", Subscription.class)
//                .setParameter("agree", "74005009500148")
//                .getResultList();

        return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
                + "where owner.lifeCycle =:lifeCycle and sub.status = :status and "
                + "sub.expirationDate >= :leftDate and "
                + "sub.expirationDate <= :rightDate and "
                + "owner.isBilledByLifeCycle = :bill_by", Subscription.class)
                .setParameter("lifeCycle", SubscriberLifeCycleType.PREPAID)
                .setParameter("status", SubscriptionStatus.ACTIVE)
                .setParameter("bill_by", Boolean.TRUE)
                .setParameter("leftDate", new DateTime(leftDate))
                .setParameter("rightDate", new DateTime(rightDate))
                .getResultList();
    }



    public List<Subscription> findAllCanceledUniAndCity() {
        return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
                + "where owner.lifeCycle =:lifeCycle and sub.status = :status and "
                + "sub.activationDate is null and sub.lastPaymentDate is null"
                + " and sub.creationDate <= :cancelday and sub.restoredFromFinal = FALSE", Subscription.class)
                .setParameter("lifeCycle", SubscriberLifeCycleType.PREPAID)
                .setParameter("status", SubscriptionStatus.INITIAL)
                .setParameter("cancelday", DateTime.now().minusDays(billingSettings.getSettings().getBeforeCancelPeriodInDays()).toDate())
                .getResultList();
    }


    public List<Subscription> findAllCanceledPrepaid() {
        return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
                + "where owner.lifeCycle =:lifeCycle and sub.status = :status and "
                + "sub.activationDate is null and sub.lastPaymentDate is null"
                + " and sub.creationDate <= :cancelday and sub.restoredFromFinal = FALSE", Subscription.class)
                .setParameter("lifeCycle", SubscriberLifeCycleType.PREPAID)
                .setParameter("status", SubscriptionStatus.INITIAL)
                .setParameter("cancelday", DateTime.now().minusDays(billingSettings.getSettings().getBeforeCancelPeriodInDays()).toDate())
                .getResultList();
    }

    public List<Subscription> findAllFinalizedPrepaid() {
        return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
                + "where owner.lifeCycle = :lifeCycle and sub.status = :status and "
                + "sub.activationDate is null and sub.restoredFromFinal = TRUE and "
                + "sub.restorationDate <= :cancelday", Subscription.class)
                .setParameter("lifeCycle", SubscriberLifeCycleType.PREPAID)
                .setParameter("status", SubscriptionStatus.INITIAL)
                .setParameter("cancelday", DateTime.now().minusDays(billingSettings.getSettings().getBeforeCancelPeriodInDays()))
                .getResultList();
    }

    public List<Subscription> findAllLatePrepaidGrace() {
        return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
                + "where owner.lifeCycle =:lifeCycle and sub.status = :status and "
                + "sub.billedUpToDate <= CURRENT_TIMESTAMP"
                + " and sub.service.provider.id =:provider"
                + " and sub.billingModel.principle =:principle"
                + " and owner.isBilledByLifeCycle = :bill_by", Subscription.class)
                .setParameter("lifeCycle", SubscriberLifeCycleType.PREPAID)
                .setParameter("status", SubscriptionStatus.BLOCKED)
                .setParameter("bill_by", Boolean.TRUE)
                .setParameter("provider", Providers.CITYNET.getId())
                .setParameter("principle", BillingPrinciple.GRACE)
                .getResultList();
    }

    public List<Subscription> findAllLatePrepaid() {
        return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
                + "where owner.lifeCycle =:lifeCycle and sub.status = :status and "
                + "sub.billedUpToDate <= CURRENT_TIMESTAMP"
                + " and sub.service.provider.id in :providers"
                + " and owner.isBilledByLifeCycle = :bill_by", Subscription.class)
                .setParameter("lifeCycle", SubscriberLifeCycleType.PREPAID)
                .setParameter("status", SubscriptionStatus.BLOCKED)
                .setParameter("bill_by", Boolean.TRUE)
                .setParameter("providers", Arrays.asList(Providers.CITYNET.getId(), Providers.UNINET.getId()))
                .getResultList();
    }

    public List<Subscription> findAllNotActivatedPrepaid(Long providerId) {
        return em.createQuery("select sub from Subscription sub where (sub.activationDate is null or sub.status = :status) and sub.service.provider.id =:provider").
                setParameter("provider", providerId).
                setParameter("status", SubscriptionStatus.INITIAL).getResultList();
    }

    public List<SubscriptionVAS> findAllVASForManageLifeCycle() {
        return em.createQuery("select got from SubscriptionVAS got where got.expirationDate is not null and got.expirationDate <= CURRENT_TIMESTAMP " +
                "and got.vas.provider.id = :provider and got.vasStatus != :status", SubscriptionVAS.class)
                .setParameter("provider", Providers.CITYNET.getId())
                .setParameter("status", -1)
                .getResultList();
    }

    public List<Subscription> findAllActivePrepaid() {
        return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
                + "where owner.lifeCycle =:lifeCycle and sub.status = :status and "
                + "sub.expirationDate > CURRENT_TIMESTAMP and "
                + "sub.service.provider.id != :provider", Subscription.class)
                .setParameter("lifeCycle", SubscriberLifeCycleType.PREPAID)
                .setParameter("status", SubscriptionStatus.ACTIVE)
                .setParameter("provider", Providers.CITYNET.getId())
                .getResultList();
    }


    public Subscription findDPSubscriptionCC(String agreement) {
        return em.createQuery("select sub from Subscription sub "
                + " where sub.agreement = :agreement and sub.service.provider.id = :provider", Subscription.class)
                .setParameter("agreement", agreement)
                .setParameter("provider", Providers.DATAPLUS.getId())
                .getSingleResult();
    }

    public Subscription findSubscriptionByAgreementAndProviders(String agreement, List<Long> providerIds, boolean isExact) {
        if (isExact)
            return em.createQuery("select sub from Subscription sub "
                    + " where sub.agreement = :agreement and sub.service.provider.id IN :providers", Subscription.class)
                    .setParameter("agreement", agreement)
                    .setParameter("providers", providerIds)
                    .getSingleResult();
        else return em.createQuery("select sub from Subscription sub "
                + " where sub.agreement like :agreement and sub.service.provider.id IN :providers", Subscription.class)
                .setParameter("agreement", "%"+agreement)
                .setParameter("providers", providerIds)
                .getSingleResult();
    }

    public Subscription findGlobalSubscriptionCC(String agreement) {
        return em.createQuery("select sub from Subscription sub "
                + " where sub.agreement = :agreement and sub.service.provider.id = :provider", Subscription.class)
                .setParameter("agreement", agreement)
                .setParameter("provider", Providers.GLOBAL.getId())
                .getSingleResult();
    }


    public Subscription findCNCSubscriptionCC(String agreement) {
        return em.createQuery("select sub from Subscription sub "
                + " where sub.agreement = :agreement and sub.service.provider.id = :provider", Subscription.class)
                .setParameter("agreement", agreement)
                .setParameter("provider", Providers.CNC.getId())
                .getSingleResult();
    }

    public Subscription findSubscription(String agreement, String dealerId) throws NoResultException {
        Subscription sub = null;
        try {
            sub = (Subscription) em.createNativeQuery("select sup.* from  " +
                    "subscriptions sup join " +
                    "sub_sub_settings_join sub_sub  " +
                    "on sup.id = sub_sub.subscription_id join " +
                    "subscription_settings sett " +
                    "on sub_sub.subscription_setting_id = sett.id " +
                    "join Service_Settings ser_s " +
                    "on sett.service_setting_id = ser_s.id " +
                    "where sett.setting_value  = '" + dealerId + "' " +
                    "and ser_s.setting_type = '9' and sup.agreement ='" + agreement + "' ", Subscription.class)
                    .getSingleResult();
        } catch (NoResultException nre) {
            log.debug("NoResultException 846" + nre);
            throw nre;
        }
        return sub;
    }

    public BigDecimal findAllDataPlusSubscriptionsC() {
        return (BigDecimal) em.createNativeQuery("select count(*) c from subscriptions sup  " +
                "join subscriptions_services ser " +
                "on sup.id = ser.subscription_id " +
                "join services s " +
                "on ser.service_id = s.id  " +
                "where s.provider_id = '" + Providers.DATAPLUS.getId() + "' " +
                "and sup.agreement <> '0'")
                .getSingleResult();

    }


    public BigDecimal findAllGlobalSubscriptionsC() {
        return (BigDecimal) em.createNativeQuery("select count(*) c from subscriptions sup  " +
                "join subscriptions_services ser " +
                "on sup.id = ser.subscription_id " +
                "join services s " +
                "on ser.service_id = s.id  " +
                "where s.provider_id = '" + Providers.GLOBAL.getId() + "' " +
                "and sup.agreement <> '0'")
                .getSingleResult();

    }


    public BigDecimal findAllCNCSubscriptionsC() {
        return (BigDecimal) em.createNativeQuery("select count(*) c from subscriptions sup  " +
                "join subscriptions_services ser " +
                "on sup.id = ser.subscription_id " +
                "join services s " +
                "on ser.service_id = s.id  " +
                "where s.provider_id = '" + Providers.CNC.getId() + "' " +
                "and sup.agreement <> '0'")
                .getSingleResult();

    }

    public List<Subscription> findAllDataPlusSubscriptions() {
        return em.createNativeQuery("select sup.* from subscriptions sup  " +
                "join subscriptions_services ser " +
                "on sup.id = ser.subscription_id " +
                "join services s " +
                "on ser.service_id = s.id  " +
                "where s.provider_id = '" + Providers.DATAPLUS.getId() + "' " +
                "and sup.agreement <> '0'  ", Subscription.class).setMaxResults(200).setFirstResult(2)
                .getResultList();

    }

    public List<Subscription> findAllCNCSubscriptions() {
        return em.createNativeQuery("select sup.* from subscriptions sup  " +
                "join subscriptions_services ser " +
                "on sup.id = ser.subscription_id " +
                "join services s " +
                "on ser.service_id = s.id  " +
                "where s.provider_id = '" + Providers.CNC.getId() + "' " +
                "and sup.agreement <> '0'  ", Subscription.class).setMaxResults(200).setFirstResult(2)
                .getResultList();

    }


    public List<Subscription> findAllGlobalSubscriptions() {
        return em.createNativeQuery("select sup.* from subscriptions sup  " +
                "join subscriptions_services ser " +
                "on sup.id = ser.subscription_id " +
                "join services s " +
                "on ser.service_id = s.id  " +
                "where s.provider_id = '" + Providers.GLOBAL.getId() + "' " +
                "and sup.agreement <> '0'  ", Subscription.class).setMaxResults(200).setFirstResult(2)
                .getResultList();

    }

    public List<Subscription> findAllDataPlusSubscriptions2(int start, int end) {
        return em.createNativeQuery("select * from ( " +
                "select rownum r, d.* from ( " +
                "select sup.* from subscriptions sup  " +
                "join subscriptions_services ser " +
                "on sup.id = ser.subscription_id " +
                "join services s " +
                "on ser.service_id = s.id  " +
                "where s.provider_id = '" + Providers.DATAPLUS.getId() + "' " +
                "and sup.agreement <> '0' " +
                " order by agreement) d) " +
                " where r between " + start + " and " + end, Subscription.class)
                .getResultList();

    }


    public List<Subscription> findAllGlobalSubscriptions2(int start, int end) {
        return em.createNativeQuery("select * from ( " +
                "select rownum r, d.* from ( " +
                "select sup.* from subscriptions sup  " +
                "join subscriptions_services ser " +
                "on sup.id = ser.subscription_id " +
                "join services s " +
                "on ser.service_id = s.id  " +
                "where s.provider_id = '" + Providers.GLOBAL.getId() + "' " +
                "and sup.agreement <> '0' " +
                " order by agreement) d) " +
                " where r between " + start + " and " + end, Subscription.class)
                .getResultList();

    }


    public List<Subscription> findAllCNCSubscriptions2(int start, int end) {
        return em.createNativeQuery("select * from ( " +
                "select rownum r, d.* from ( " +
                "select sup.* from subscriptions sup  " +
                "join subscriptions_services ser " +
                "on sup.id = ser.subscription_id " +
                "join services s " +
                "on ser.service_id = s.id  " +
                "where s.provider_id = '" + Providers.CNC.getId() + "' " +
                "and sup.agreement <> '0' " +
                " order by agreement) d) " +
                " where r between " + start + " and " + end, Subscription.class)
                .getResultList();

    }

    public List<T> findAllDataPlusPaginated(int first, int pageSize) {
        Query query = !getFilters().isEmpty() ? getPaginatedQueryWithFilters() : getPaginatedQuery();

        log.debug("findAllPaginated, JPA query: " + query);

        if (query == null) {
            return null;
        }

        List<T> data = query
                .setFirstResult(first)
                .setMaxResults(pageSize)
                .getResultList();

        //log.debug(String.format("findAllPaginated: paginated data - size=%d, data=%s", data.size(), data));
        return data;
    }

    public List<Subscription> findSubscriptionsByDealer2(String dealerId, int start, int end) {
        return em.createNativeQuery("select * from (" +
                "                select rownum r, d.* from (" +
                "                select sup.* from  " +
                "                subscriptions sup join " +
                "                sub_sub_settings_join sub_sub  " +
                "                on sup.id = sub_sub.subscription_id join " +
                "                subscription_settings sett " +
                "                on sub_sub.subscription_setting_id = sett.id " +
                "                join Service_Settings ser_s " +
                "                on sett.service_setting_id = ser_s.id " +
                "                where sett.setting_value  = '" + dealerId + "' " +
                "                and ser_s.setting_type = '9'  " +
                "                and sup.agreement <> '0' " +
                "                order by agreement) d) " +
                "                 where  " +
                "                r between " + start + " and " + end, Subscription.class).getResultList();
    }

    public List<Subscription> findSubscriptionsByDealer3(String dealerId) {
        return em.createNativeQuery("select sup.* from  " +
                "                subscriptions sup join " +
                "                sub_sub_settings_join sub_sub  " +
                "                on sup.id = sub_sub.subscription_id join " +
                "                subscription_settings sett " +
                "                on sub_sub.subscription_setting_id = sett.id " +
                "                join Service_Settings ser_s " +
                "                on sett.service_setting_id = ser_s.id " +
                "                where sett.setting_value  = '" + dealerId + "' " +
                "                and ser_s.setting_type = '9'  " +
                "                and sup.agreement <> '0' ", Subscription.class).getResultList();
    }


    public Subscription findSubscriptionsByUsername(String username) {
        return (Subscription) em.createNativeQuery("select sup.* from  " +
                "                subscriptions sup join " +
                "                sub_sub_settings_join sub_sub " +
                "                on sup.id = sub_sub.subscription_id join " +
                "                subscription_settings sett " +
                "                on sub_sub.subscription_setting_id = sett.id " +
                "                join Service_Settings ser_s " +
                "                on sett.service_setting_id = ser_s.id " +
                "                where sett.setting_value  = '" + username + "' and  " +
                "                ser_s.setting_type = '0' and sup.agreement <> '0'", Subscription.class).getSingleResult();
    }

    public Subscription findSubscriptionByDealer(String dealerId, String agreement) {
        return (Subscription) em.createNativeQuery("select sup.* from  " +
                "subscriptions sup join " +
                "sub_sub_settings_join sub_sub  " +
                "on sup.id = sub_sub.subscription_id join " +
                "subscription_settings sett " +
                "on sub_sub.subscription_setting_id = sett.id " +
                "join Service_Settings ser_s " +
                "on sett.service_setting_id = ser_s.id " +
                "where sett.setting_value  = '" + dealerId + "' " +
                "and ser_s.setting_type = '9' and sup.agreement = '" + agreement + "'", Subscription.class).getSingleResult();
    }


    public List<Subscription> findSubscriptionsByDealer(String dealerId) {
        return em.createNativeQuery("select sup.* from  " +
                "subscriptions sup join " +
                "sub_sub_settings_join sub_sub  " +
                "on sup.id = sub_sub.subscription_id join " +
                "subscription_settings sett " +
                "on sub_sub.subscription_setting_id = sett.id " +
                "join Service_Settings ser_s " +
                "on sett.service_setting_id = ser_s.id " +
                "where sett.setting_value  = '" + dealerId + "' " +
                "and ser_s.setting_type = '9' ", Subscription.class).getResultList();
    }


    public BigDecimal findSubscriptionsByDealerC(String dealerId) {
        return (BigDecimal) em.createNativeQuery("select count(*) c from  " +
                "                subscriptions sup join " +
                "                sub_sub_settings_join sub_sub  " +
                "                on sup.id = sub_sub.subscription_id join " +
                "                subscription_settings sett " +
                "                on sub_sub.subscription_setting_id = sett.id " +
                "                join Service_Settings ser_s " +
                "                on sett.service_setting_id = ser_s.id " +
                "                where sett.setting_value  = '" + dealerId + "' " +
                "                and ser_s.setting_type = '9'  " +
                "                and sup.agreement <> '0'").getSingleResult();
    }


    public List<Subscription> findSubscriptionsByDealerLast2Days(String dealerId) {
        return em.createNativeQuery("select sup.* from  " +
                "subscriptions sup join " +
                "sub_sub_settings_join sub_sub  " +
                "on sup.id = sub_sub.subscription_id join " +
                "subscription_settings sett " +
                "on sub_sub.subscription_setting_id = sett.id " +
                "join Service_Settings ser_s " +
                "on sett.service_setting_id = ser_s.id " +
                "where sett.setting_value  = '" + dealerId + "' " +
                "and ser_s.setting_type = '9' and sup.agreement != '0' " +
                "and sup.exp_date_with_grace_period between  sysdate+2 and trunc(sysdate+3, 'dd') ", Subscription.class).getResultList();
    }


    public BigDecimal findPaymentsListC(String agreement) {
        return (BigDecimal) em.createNativeQuery("select count(*) co " +
                "                  from payments " +
                "                 where contract = '" + agreement + "'").getSingleResult();
    }

    public List<Payment> findPaymentsList(String agreement) {
        return em.createQuery("select pay from Payment pay " +
                "where pay.contract = :agree", Payment.class)
                .setParameter("agree", agreement).getResultList();
    }

    public List<Payment> findPaymentsListR(String agreement, int start, int end) {
        return em.createNativeQuery("select * " +
                "  from (select rownum r, d.* " +
                "          from (select * " +
                "                  from payments " +
                "                 where contract = '" + agreement + "'" +
                "                 order by id desc) d) " +
                " where r between " + start + " and " + end, Payment.class).getResultList();
    }


    //----------------->Azpost persistence facade-----------------\\


    public List<Subscription> findAzPostpaidForBilling() {
        return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
                        + "where sub.service.provider.id = :provider and "
                        + "owner.lifeCycle =:lifeCycle and sub.status != :status "
                        + "and sub.activationDate IS NOT NULL "
                        + "and (sub.billedUpToDate <= CURRENT_TIMESTAMP or sub.billedUpToDate IS NULL)",
                Subscription.class)
                .setParameter("provider", Providers.AZERTELECOMPOST.getId())
                .setParameter("lifeCycle", SubscriberLifeCycleType.POSTPAID)
                .setParameter("status", SubscriptionStatus.FINAL)
                .getResultList();
    }


    public List<Subscription> findAllExpiredAzPostpaid() {
        return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
                + "where sub.service.provider.id = :provider and "
                + "owner.lifeCycle =:lifeCycle and "
                + "sub.billedUpToDate <= CURRENT_TIMESTAMP"
                + " and sub.service.provider.id =:provider"
                + " and sub.billingModel.principle =:principle", Subscription.class)
                .setParameter("provider", Providers.AZERTELECOMPOST.getId())
                .setParameter("lifeCycle", SubscriberLifeCycleType.POSTPAID)
                .setParameter("principle", BillingPrinciple.GRACE)
                .getResultList();
    }

    public List<Subscription> findAllLateAzPOSTGrace() {
        return em.createQuery("select sub from Subscription sub join sub.subscriber owner "
                + "where sub.service.provider.id = :provider and "
                + "owner.lifeCycle =:lifeCycle and sub.status = :status and "
                + "sub.billedUpToDate <= CURRENT_TIMESTAMP"
                + " and sub.service.provider.id =:provider"
                + " and sub.billingModel.principle =:principle"
                + " and owner.isBilledByLifeCycle = :bill_by", Subscription.class)
                .setParameter("provider", Providers.AZERTELECOMPOST.getId())
                .setParameter("lifeCycle", SubscriberLifeCycleType.POSTPAID)
                .setParameter("status", SubscriptionStatus.BLOCKED)
                .setParameter("bill_by", Boolean.TRUE)
                .setParameter("provider", Providers.AZERTELECOMPOST.getId())
                .setParameter("principle", BillingPrinciple.GRACE)
                .getResultList();
    }


    public boolean applyLateFee(Subscription sub) {

        long rate = 0;
        DateTime invoiceDate = null;
        try {
            Invoice newInvoice = null;
            Subscriber subscriber = sub.getSubscriber();
            for (Invoice inv : subscriber.getInvoices()) {

                log.debug(" <><><><> Invoices: " + inv);

                if (inv.getState() == InvoiceState.OPEN
                        && new DateTime(inv.getCreationDate()).isAfter((DateTime.now().withTimeAtStartOfDay()))) {
                    rate += inv.getBalance();
                    invoiceDate = new DateTime(inv.getCreationDate());
                    newInvoice = inv;
                }
            }
            rate = (long) Math.abs((rate * 0.001));
            log.debug("Rate is : " + rate);

            //new subscription - never billed
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
            lateFeeCharge.setAmount(rate);
            lateFeeCharge.setSubscriber(subscriber);
            lateFeeCharge.setUser_id(20000L);
            lateFeeCharge.setDsc("LateFee");
            lateFeeCharge.setDatetime(invoiceDate);
            lateFeeCharge.setSubscription(sub);
            chargeFacade.save(lateFeeCharge);

            Transaction transDebitServiceFee = new Transaction(
                    TransactionType.DEBIT,
                    sub,
                    rate,
                    "LateFee"
            );
            transDebitServiceFee.execute();
            transFacade.save(transDebitServiceFee);
            lateFeeCharge.setTransaction(transDebitServiceFee);

            newInvoice.addChargeToList(lateFeeCharge);

            log.info(
                    String.format("Charge for late fee: subscription id=%d, agreement=%s, amount=%d",
                            sub.getId(), sub.getAgreement(), lateFeeCharge.getAmount())
            );

            systemLogger.success(
                    SystemEvent.CHARGE,
                    sub,
                    transDebitServiceFee,
                    String.format("Charge id=%d for LateFee, amount=%s",
                            lateFeeCharge.getId(), lateFeeCharge.getAmountForView()
                    )
            );

            return true;
        } catch (Exception ex) {

            log.info("Cannot charge for LateFee: " + ex);
            ex.printStackTrace();
            return false;
        }
    }


    //^\\
    //=================> Azpost persistence facade =================\\

    public ServiceSetting createServiceSetting(Long id, String title, ServiceSettingType settingType, Providers provider, ServiceType serviceType, String desc) {
        ServiceSetting serviceSetting = new ServiceSetting();
        serviceSetting.setTitle(title);
        serviceSetting.setProvider(createProvider(provider));
        serviceSetting.setServiceType(serviceType);
        serviceSetting.setType(settingType);
        serviceSetting.setDsc(desc);
        serviceSetting.setId(id);

        return serviceSetting;
    }

    public ServiceProvider createProvider(Providers provider) {
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
        }

        return serviceProvider;
    }

    public boolean restoreSubscription(Subscription subscription) {
        ProvisioningEngine subsProvisioning = null;
        try {
            subsProvisioning = engineFactory.getProvisioningEngine(subscription);
        } catch (ProvisionerNotFoundException e) {
            e.printStackTrace();
        }
        if (!(subscription.getStatus().equals(SubscriptionStatus.FINAL) ||
                subscription.getStatus().equals(SubscriptionStatus.CANCEL))) {
            FacesContext.getCurrentInstance().addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_ERROR,
                            "Subscription Status should be final or cancel",
                            "Subscription Status should be final or cancel for restoring to defaults"
                    ));
            return false;
        }
        List<Invoice> openInvoiceList = invoiceFacade.findOpenBySubscriber(subscription.getSubscriber().getId());
        if (openInvoiceList != null) {
            for (final Invoice invoice : openInvoiceList) {
                if (invoice.getState().equals(InvoiceState.OPEN)) {
                    FacesContext.getCurrentInstance().addMessage(
                            null,
                            new FacesMessage(
                                    FacesMessage.SEVERITY_ERROR,
                                    "Subscriber has open invoices",
                                    String.format("Subscriber has open invoice with id = %d", invoice.getId())
                            ));
                    return false;
                }
            }
        }

        if (subscription.getBalance().getRealBalance() < 0) {
            FacesContext.getCurrentInstance().addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_ERROR,
                            "Subscription balance is negative",
                            "Cannot switch from final(or cancel) to initial when balance is negative"
                    ));
            return false;
        }

        Long rate = subscription.getService().getServicePrice();
        HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
        String userName = FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal().getName();
        User user = userFacade.findByUserName(userName);

        Invoice invoice = new Invoice();
        invoice.setState(InvoiceState.OPEN);
        invoice.setSubscriber(subscription.getSubscriber());
        //charge for service fee if > 0
        if (rate >= 0) {
            Charge servFeeCharge = new Charge();
            servFeeCharge.setService(subscription.getService());
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
        }

        if (subscription.getVasList() != null) {
            for (SubscriptionVAS vas : subscription.getVasList()) {
                long vasPrice = vas.getVas().getPrice();

                if ((vas.getRemainCount() != null && vas.getRemainCount() == -99L) || vas.getStatus() == SubscriptionStatus.FINAL) {
                    continue;
                }

                if (vas.getRemainCount() != null && vas.getRemainCount() >= 1) {
                    log.debug("vas: " + vas + " vas count: " + vas.getRemainCount());
                    manageStaticVas(subscription, vas);
                }

//                long vasRate = vasPrice * vas.getCount();
                long vasRate = (long)(vasPrice * vas.getCount());

                invoiceFacade.addVasChargeToInvoice(invoice, subscription, vasRate, vas.getVas(), user.getId(), String.format("Charge for vas=%s count=%s", vas.getVas().getName(), vas.getCount()));
            }
        }

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

        if (billSettings.getSettings().getPrepaidlifeCycleLength() == 30) {
            subscription.setBilledUpToDate(DateTime.now().plusMonths(1));
        } else {
            subscription.setBilledUpToDate(DateTime.now().plusDays(
                    billSettings.getSettings().getPrepaidlifeCycleLength()
            ));
        }
        subsProvisioning.removeFD(subscription);
        final StatusChangeRule rule =
                subscription.getService().findRule(subscription.getStatus(), SubscriptionStatus.INITIAL);
        if (rule != null) {
            subscription = engineFactory.getOperationsEngine(subscription).addVAS(
                    new VASCreationParams.Builder().
                            setSubscription(subscription).
                            setValueAddedService(rule.getVas()).
                            build()
            );
        }

        subscription.setStatus(SubscriptionStatus.INITIAL);
        String msg = String.format("event=%s, subscription id=%d", BillingEvent.STATUS_CHANGED, subscription.getId());

        try {
            queueManager.sendStatusNotification(BillingEvent.STATUS_CHANGED, subscription);
            systemLogger.success(SystemEvent.NOTIFICATION_SCHEDULED, subscription, msg);
        } catch (Exception ex) {
            log.error("changeStatus: " + " Cannot send notification, " + msg, ex);
            systemLogger.error(SystemEvent.NOTIFICATION_SCHEDULED, subscription, msg);
        }

        subscription.setActivationDate(null);
        DateTime endDate = DateTime.now().plusDays(4).withTime(23, 59, 59, 999);
        subscription.setRestoredFromFinal(true);
        subscription.setRestorationDate(DateTime.now());
        subscription.initializeExpirationDates();
        update(subscription);
        reopenService(subscription, endDate);
        systemLogger.success(SystemEvent.SUBSCRIPTION_REINITIALIZED, subscription,
                String.format("subscription id=%d,service id=%d", subscription.getId(), subscription.getService().getId()));
        return true;
    }

    public void reopenService(Subscription subscription, DateTime endDate) {
        try {
            ProvisioningEngine citynetProvisioner = engineFactory.getProvisioningEngine(subscription);
            if (citynetProvisioner.reprovisionWithEndDate(subscription, endDate)) {
                systemLogger.success(
                        SystemEvent.REPROVISION, subscription,
                        String.format("subscription id=%d, service id=%d", subscription.getId(), subscription.getService().getId())
                );
            } else {
                systemLogger.error(
                        SystemEvent.REPROVISION, subscription,
                        String.format("subscription id=%d, service id=%d", subscription.getId(), subscription.getService().getId())
                );
            }
        } catch (ProvisionerNotFoundException e) {
            log.error(e);
            systemLogger.error(
                    SystemEvent.REPROVISION, subscription,
                    String.format("subscription id=%d, service id=%d", subscription.getId(), subscription.getService().getId())
            );
        }
    }

    public void manageStaticVas(Subscription subscription, SubscriptionVAS vas) {
        vas = em.merge(vas);
        vas.setRemainCount(vas.getRemainCount() - 1);

        if (vas.getRemainCount() == 0) {
            vas.setRemainCount(-99D);
            engineFactory.getOperationsEngine(subscription).
                    removeVAS(subscription, vas);
        }
    }

    public Subscription removeVAS(Subscription subscription, ValueAddedService vasID) {
        SubscriptionVAS vas = subscription.getVASByServiceId(vasID.getId());

        subscription = update(subscription);
        vas = em.merge(vas);

        log.debug(String.format("remveVAS: vas id=%d, subscription id=%d", vas.getId(), subscription.getId()));

        SubscriptionResourceBucket bucket = null;
        if (vas.getResource() != null) {
            bucket = vas.getResource().getBucketByType(ResourceBucketType.INTERNET_IP_ADDRESS);
        }

        if (vas.getVas().getCode().getType() == ValueAddedServiceType.PERIODIC_STATIC) {
            try {
                if (bucket != null) {
                    String ipAddress = bucket.getCapacity();
                    log.debug("removeVAS: ip address = " + ipAddress);

                    ProvisioningEngine provisioner = engineFactory.getProvisioningEngine(subscription);

                    String message = null;

                    message = String.format("CLOSE VAS returned subscription id=%d, vas id=%d, status=%s",
                            subscription.getId(), vas.getVas().getId(), vas.getStatus());
                    log.info("removeVAS: " + message);

                    if (provisioner.closeVAS(subscription, vas.getVas())) {
                        systemLogger.success(SystemEvent.VAS_STATUS_BLOCK, subscription, message);
                    } else {
                        systemLogger.error(SystemEvent.VAS_STATUS_BLOCK, subscription, message);
                    }

                    List<IpAddress> ip = em.createQuery("select a from IpAddress a where a.addressAsString = :addr", IpAddress.class).setParameter("addr", ipAddress).getResultList();
                    log.debug("removeVAS: ip address list=" + ip);
                    if (ip != null && !ip.isEmpty()) {
                        List<IpAddressRange> range = em.createQuery("select r from IpAddressRange r join r.reservedAddressList a where a.id = :addr", IpAddressRange.class)
                                .setParameter("addr", ip.get(0).getId()).getResultList();

                        if (range != null && !range.isEmpty()) {
                            range.get(0).freeAddress(ip.get(0));
                        }

                        for (IpAddress addr : ip) {
                            em.remove(addr);
                            message = String.format("IP address=%s removed from subscription id=%d, vas id=%d", ipAddress, subscription.getId(), vas.getVas().getId());
                            log.debug("edtiVAS: " + message);
                            systemLogger.success(SystemEvent.IP_ADDRESS_UNRESERVED, subscription, message);
                        }
                    }
                }// END IF Ip Address

                Iterator<SubscriptionVAS> it = subscription.getVasList().iterator();
                SubscriptionVAS sbnVAS = null;

                while (it.hasNext()) {
                    sbnVAS = it.next();

                    if (sbnVAS.getId() == vas.getId()) {
                        log.debug("removeVAS: removing sbn vas id=" + sbnVAS.getId());
                        it.remove();
                        break;
                    }

                    em.remove(vas);
                }
            } catch (Exception ex) {
                log.error(String.format("Cannot remove VAS subscription: subscription=%s, service=%s, vas id=%d", subscription, subscription.getService(), vas.getId()), ex);
                ctx.setRollbackOnly();
            }
        }

        return subscription;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    private void initService(Subscription subscription) {
        try {
            ProvisioningEngine provisioner = engineFactory.getProvisioningEngine(subscription);
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

    public void createSubscriber() {

        Balance b = new Balance();
        b.setRealBalance(100);

        /*Rate rt = new Rate();
         rt.setName("Default Rate");
         rt.setPrice(10000);
         rt.setUsePromoResources(true);

         RateProfile rp = new RateProfile();
         rp.setName("RP for ADSL");
         rp.addRate(rt);

         Service s = new Service();
         s.setName("service-test");
         s.setServiceType(ServiceType.ADSL);
         s.setIsBillByLifeCycle(true);
         s.setRateProfile(rp);

         String ident = "1-ident";

         Subscription sub = new Subscription();
         sub.setAgreement("agrem");
         sub.setIdentifier(ident);
         sub.setBalance(b);
         sub.setService(s);
         this.save(sub);
         */
        //this.getEntityManager().persist(b);
    }

    public void chargePostpaidVirtuallyOnActivate(Subscription sub) {
        //virtual charge for installation fee
        sub.getBalance().debitVirtual(sub.getService().getInstallationFee());
        long rate = sub.getService().getServicePrice();
        if (rate > 0) { //virtual charge for service fee
            rate = getPartialRateForPostpaid(sub, rate);
            sub.getBalance().debitVirtual(rate);
        }
    }

    public long getPartialRateForPostpaid(Subscription sub, long rate) {

        int numOfDaysUsed = 0;
        int dayInMonth = sub.getActivationDate().get(DateTimeFieldType.dayOfMonth());

        if (dayInMonth >= sub.getActivationDate().dayOfMonth().getMaximumValue()) {
            numOfDaysUsed = 1;
        } else {
            numOfDaysUsed = billSettings.getSettings().getPospaidLifeCycleLength() - (dayInMonth - 1);
        }

        if (numOfDaysUsed < billSettings.getSettings().getPospaidLifeCycleLength()) {
            rate = (rate / billSettings.getSettings().getPospaidLifeCycleLength()) * numOfDaysUsed;
        }

        return rate;
    }

    public void chargePostPaidOnActivate(Subscription subscription) {
        Invoice newInvoice = null;

        Subscriber subscriber = subscription.getSubscriber();
        for (Invoice inv : subscriber.getInvoices()) {
            if (inv.getState() == InvoiceState.OPEN
                    && new DateTime(inv.getCreationDate()).isAfter(subscription.getBilledUpToDate())) {
                newInvoice = inv;
            }
        }
        //new subscription - never billed
        if (newInvoice == null) {
            newInvoice = invoiceFacade.createInvoiceForSubscriber(subscriber);
        }

        long rate = subscription.getService().getServicePrice();
        ;
        long startBalance = subscription.getBalance().getRealBalance();
        long totalCharged = 0;

        //subscription is new - never billed
        if (subscription.getBilledUpToDate() == null) {
            //calculate the rate for partial billing
            int numOfDaysUsed = 0;

            int dayInMonth = DateTime.now().get(DateTimeFieldType.dayOfMonth());
            //calculate
            if (dayInMonth >= subscription.getActivationDate().dayOfMonth().getMaximumValue()) {
                numOfDaysUsed = 1;
            } else {
                numOfDaysUsed = billSettings.getSettings().getPospaidLifeCycleLength() - (dayInMonth - 1);
            }

            if (numOfDaysUsed < billSettings.getSettings().getPospaidLifeCycleLength()) {
                rate = (rate / billSettings.getSettings().getPospaidLifeCycleLength()) * numOfDaysUsed;
            }

            log.info(String.format("Partial rate for %d days is %d", numOfDaysUsed, rate));

            //charge for installation fee
            if (subscription.getService().getInstallationFee() > 0) {
                //balance.debitReal(subscription.getService().getInstallationFee() * 100000);
                Charge instFeeCharge = new Charge();
                instFeeCharge.setService(subscription.getService());
                instFeeCharge.setAmount(subscription.getService().getInstallationFee());
                instFeeCharge.setSubscriber(subscription.getSubscriber());
                instFeeCharge.setUser_id(20000L);
                instFeeCharge.setDsc("Autocharged  for installation fee upon manual activation");
                instFeeCharge.setDatetime(DateTime.now());
                instFeeCharge.setSubscription(subscription);
                chargeFacade.save(instFeeCharge);

                Transaction transDebitInstFee = new Transaction(
                        TransactionType.DEBIT,
                        subscription,
                        subscription.getService().getInstallationFee(),
                        "Autocharged  for installation fee upon manual activation");
                transDebitInstFee.execute();
                transFacade.save(transDebitInstFee);

                newInvoice.addChargeToList(instFeeCharge);

                totalCharged += instFeeCharge.getAmount();
            }
        }
        //charge for service fee
        if (rate > 0) {
            Charge servFeeCharge = new Charge();
            servFeeCharge.setService(subscription.getService());
            servFeeCharge.setAmount(rate);
            servFeeCharge.setSubscriber(subscriber);
            servFeeCharge.setUser_id(20000L);
            servFeeCharge.setDsc("Autocharged  for service fee upon manual activation");
            servFeeCharge.setDatetime(DateTime.now());
            servFeeCharge.setSubscription(subscription);
            chargeFacade.save(servFeeCharge);

            Transaction transDebitServiceFee = new Transaction(
                    TransactionType.DEBIT,
                    subscription,
                    rate,
                    "Autocharged for service fee upon manual activation"
            );
            transDebitServiceFee.execute();
            transFacade.save(transDebitServiceFee);
            newInvoice.addChargeToList(servFeeCharge);
            totalCharged += servFeeCharge.getAmount();
        } // end if for rate

        //adjust invoice debt to subscription's balance
        long endBalance = subscription.getBalance().getRealBalance();
        //newInvoice.addDebt(totalCharged);

        /* if (startBalance <= 0) {
         newInvoice.addDebt(totalCharged);
         } */
        if (startBalance >= 0
                && endBalance < 0) {
            //newInvoice.addDebt(Math.abs(endBalance));
            //newInvoice.addDebt(totalCharged);
            newInvoice.reduceDebt(startBalance);
        } else if (endBalance >= 0) {
            //newInvoice.addDebt(totalCharged);
            newInvoice.reduceDebt(totalCharged);
            if (newInvoice.getBalance() >= 0) {
                newInvoice.setState(InvoiceState.CLOSED);
                newInvoice.setCloseDate(DateTime.now());
            }
            //subscriber paid out the debt - extend subscription date
            if (billSettings.getSettings().getPospaidLifeCycleLength() == 30) {
                /*sub.setBilledUpToDate(DateTime.now().plusMonths(1).plusDays(
                 billSettings.getSettings().getPostpaidDefaultGracePeriod()));
                 */
                subscription.setExpirationDate(DateTime.now().plusMonths(1));
                subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDate().plusDays(
                        billSettings.getSettings().getPostpaidDefaultGracePeriod()));
            } else {
                /*sub.setBilledUpToDate(DateTime.now().plusDays(
                 billSettings.getSettings().getPospaidLifeCycleLength()
                 +  billSettings.getSettings().getPostpaidDefaultGracePeriod()));
                 */
                subscription.setExpirationDate(DateTime.now().plusDays(
                        billSettings.getSettings().getPospaidLifeCycleLength()
                        )
                );
                subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDate().plusDays(
                        billSettings.getSettings().getPostpaidDefaultGracePeriod()));
            }
            subscription.synchronizeExpiratioDates();
        } // end if for charging

        //adjust last billing date
        DateTime baseDate = DateTime.now().plusMonths(1).withDayOfMonth(1).withTimeAtStartOfDay().minusSeconds(1);
        if (billSettings.getSettings().getPospaidLifeCycleLength() == 30) {
            subscription.setBilledUpToDate(baseDate.plusMonths(1));
        } else {
            subscription.setBilledUpToDate(baseDate.plusDays(
                    billSettings.getSettings().getPospaidLifeCycleLength())
            );
        }
    }

     @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Payment settlePayment(long subscriptionID, Long amount, double fAmount, long paymentID) throws NoSuchSubscriptionException, Exception {
        log.info(String.format("Settling payment. Parameters: subscriptionID=%d, amount=%d, fAmount=%f, paymentID=%d", subscriptionID, amount, fAmount, paymentID));

        Payment payment = null;
        try {
            em.setProperty("javax.persistence.lock.timeout", 10000);
            Subscription subscription = this.find(subscriptionID, LockModeType.PESSIMISTIC_WRITE);
            payment = paymentFacade.find(paymentID);

            accTransFacade.makePayment(payment, subscription, amount, userFacade.findByUserName(ctx.getCallerPrincipal().getName()));
            log.info(String.format("Subscription %s real balance is %s after makePayment process", subscriptionID, subscription.getBalance().getRealBalance()));

            if (payment.getSubscriber_id() == null || payment.getSubscriber_id() == 0) {
                payment.setSubscriber_id(subscription.getSubscriber().getId());
            }

            //paymentFacade.update(payment);
            List<Invoice> openInvoiceList = invoiceFacade.findOpenBySubscriberForPayment(subscription.getSubscriber().getId());
            /* if (openInvoiceList == null || openInvoiceList.isEmpty()) {
             throw new NoInvoiceFoundException(String.format("No invoice exists for subscriber %d", subscriber.getId()));
             }
             */
            long residualValue = 0;

            log.info("Found open invoices: " + openInvoiceList);

            if (openInvoiceList != null && !openInvoiceList.isEmpty()) {
                for (Invoice invoice : openInvoiceList) {
                    residualValue = invoice.addPaymentToList(payment, residualValue);

                    if (invoice.getState() == InvoiceState.CLOSED) {
                        systemLogger.success(SystemEvent.INVOICE_CLOSED, subscription,
                                String.format("invoice id=%d, payment id=%d", invoice.getId(), payment.getId())
                        );
                    }

                    if (residualValue <= 0) {
                        break;
                    }

                    log.debug("Invoice after payment" + invoice + " for subscription "+subscriptionID);
                }
                //invoiceFacade.update(invoice);
                ///invoiceFacade.save(invoice);
            }
            return payment;

            //this.em.merge(subscription.getBalance());
            //subscription.setLastPaymentDate(payment.getLastUpdateDate());
            //this.update(subscription);
            //Subscription sbn = this.findWithoutRefresh(subscriptionID);
            //log.debug(String.format("Subscription upon retrieval: %s", sbn));
        } catch (NoResultException ex) {
            log.error(String.format("No subscription retrieved for ID %d", subscriptionID), ex);
            ctx.setRollbackOnly();
            throw new NoSuchSubscriptionException(String.format("subscriptionID %d not found"));
        }
    }




    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Payment settlePayment(long subscriptionID, Long amount, double fAmount, long paymentID, String userN) throws NoSuchSubscriptionException, Exception {
        log.info(String.format("Settling payment. Parameters: subscriptionID=%d, amount=%d, fAmount=%f, paymentID=%d", subscriptionID, amount, fAmount, paymentID));

        Payment payment = null;
        try {
            em.setProperty("javax.persistence.lock.timeout", 10000);
            Subscription subscription = this.find(subscriptionID, LockModeType.PESSIMISTIC_WRITE);
            payment = paymentFacade.find(paymentID);

            accTransFacade.makePaymentExP(payment, subscription, amount, userFacade.findByUserName(userN));

            if (payment.getSubscriber_id() == null || payment.getSubscriber_id() == 0) {
                payment.setSubscriber_id(subscription.getSubscriber().getId());
            }

            //paymentFacade.update(payment);
            List<Invoice> openInvoiceList = invoiceFacade.findOpenBySubscriberForPayment(subscription.getSubscriber().getId());
            /* if (openInvoiceList == null || openInvoiceList.isEmpty()) {
             throw new NoInvoiceFoundException(String.format("No invoice exists for subscriber %d", subscriber.getId()));
             }
             */
            long residualValue = 0;

            log.info("Found open invoices: " + openInvoiceList);

            if (openInvoiceList != null && !openInvoiceList.isEmpty()) {
                for (Invoice invoice : openInvoiceList) {
                    residualValue = invoice.addPaymentToList(payment, residualValue);

                    if (invoice.getState() == InvoiceState.CLOSED) {
                        systemLogger.successExP(SystemEvent.INVOICE_CLOSED, subscription,
                                String.format("invoice id=%d, payment id=%d", invoice.getId(), payment.getId()), userN
                        );
                    }

                    if (residualValue <= 0) {
                        break;
                    }

                    log.debug("Invoice after payment" + invoice);
                }
                //invoiceFacade.update(invoice);
                ///invoiceFacade.save(invoice);
            }
            return payment;

            //this.em.merge(subscription.getBalance());
            //subscription.setLastPaymentDate(payment.getLastUpdateDate());
            //this.update(subscription);
            //Subscription sbn = this.findWithoutRefresh(subscriptionID);
            //log.debug(String.format("Subscription upon retrieval: %s", sbn));
        } catch (NoResultException ex) {
            log.error(String.format("No subscription retrieved for ID %d", subscriptionID), ex);
            ctx.setRollbackOnly();
            throw new NoSuchSubscriptionException(String.format("subscriptionID %d not found"));
        }
    }






    @Deprecated
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Payment settlePaymentOld(long subscriptionID, Long amount, double fAmount, long paymentID) throws NoSuchSubscriptionException {
        log.info(String.format("Settling payment. Parameters: subscriptionID=%d, amount=%d, fAmount=%f, paymentID=%d", subscriptionID, amount, fAmount, paymentID));

        Payment payment = null;
        try {
            em.setProperty("javax.persistence.lock.timeout", 10000);
            Subscription subscription = this.find(subscriptionID, LockModeType.PESSIMISTIC_WRITE);

            transFacade.createTransation(
                    TransactionType.PAYMENT, subscription,
                    amount,
                    String.format("Payment of %f AZN for Subscription %s of Subscriber %s",
                            fAmount, subscription.getAgreement(), subscription.getSubscriber().getMasterAccount()));

            log.debug(String.format("Subscription %d after payment: %s", subscription.getId(), subscription));
            //subscription.getBalance().creditVirtual(amount);
            subscription.setLastPaymentDate(DateTime.now());
            subscription.getSubscriber().setLastPaymentDate(DateTime.now());

            payment = paymentFacade.find(paymentID);
            payment.setProcessed(1);

            if (payment.getSubscriber_id() == null || payment.getSubscriber_id() == 0) {
                payment.setSubscriber_id(subscription.getSubscriber().getId());
            }

            //paymentFacade.update(payment);
            List<Invoice> openInvoiceList = invoiceFacade.findOpenBySubscriberForPayment(subscription.getSubscriber().getId());
            /* if (openInvoiceList == null || openInvoiceList.isEmpty()) {
             throw new NoInvoiceFoundException(String.format("No invoice exists for subscriber %d", subscriber.getId()));
             }
             */
            long residualValue = 0;

            log.info("Found open invoices: " + openInvoiceList);

            if (openInvoiceList != null && !openInvoiceList.isEmpty()) {
                for (Invoice invoice : openInvoiceList) {
                    residualValue = invoice.addPaymentToList(payment, residualValue);
                    if (residualValue <= 0) {
                        break;
                    }

                    log.debug("Invoice after payment" + invoice);
                }
                //invoiceFacade.update(invoice);
                ///invoiceFacade.save(invoice);
            }

            //this.em.merge(subscription.getBalance());
            //subscription.setLastPaymentDate(payment.getLastUpdateDate());
            //this.update(subscription);
            //Subscription sbn = this.findWithoutRefresh(subscriptionID);
            //log.debug(String.format("Subscription upon retrieval: %s", sbn));
        } catch (NoResultException ex) {
            log.error(String.format("No subscription retrieved for ID %d", subscriptionID), ex);
            ctx.setRollbackOnly();
            throw new NoSuchSubscriptionException(String.format("subscriptionID %d not found"));
        }
        return payment;
    }

    public Subscription findWithoutRefresh(long pk) {
        return em.find(Subscription.class, pk);
    }

    public void transferBalance(Long paymentID, Long targetID) throws Exception {
        Payment payment = null;
        Subscription targetSubscription = null;

        try {
            payment = paymentFacade.find(paymentID);
            targetSubscription = find(targetID);
            transFacade.transferPaymentToSubscription(payment, targetSubscription);
            payment.setDsc(String.format("%s Payment transfered from subscription %s", (payment.getDsc() != null ? payment.getDsc() : ""), payment.getAccount().getAgreement()));
            payment.setAccount(targetSubscription);
            payment.setSubscriber_id(targetID);
            payment.setServiceId(targetSubscription.getService());
        } catch (Exception ex) {
            StringBuilder sb = new StringBuilder("Cannot transfer payment=");
            sb.append(payment);
            sb.append("to subscription=");
            sb.append(targetSubscription);
            String message = sb.toString();
            log.error(message);
            throw new Exception(message, ex);
        }
    }

    public void transferPaymentForFinance(Long paymentID, Long targetID) throws Exception {
        Payment payment = null;
        Subscription targetSubscription = null;

        try {

            HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
            User user = userFacade.find((Long) session.getAttribute("userID"));
            payment = paymentFacade.find(paymentID);
            targetSubscription = find(targetID);
            //transFacade.transferPaymentToSubscription(payment, targetSubscription);
            AccountingTransaction trans = accTransFacade.transferPayment(targetSubscription, payment.getAccount(), payment, user);
            payment.setDsc(String.format("%s Payment amount transfered to subscription %s, accountingTransaction=%d", (payment.getDsc() != null ? payment.getDsc() : ""), targetSubscription.getAgreement(), trans.getId()));
            //payment.setAccount(targetSubscription);
            // payment.setSubscriber_id(targetID);
            //payment.setServiceId(targetSubscription.getService());
        } catch (Exception ex) {
            StringBuilder sb = new StringBuilder("Cannot transfer payment=");
            sb.append(payment);
            sb.append("to subscription=");
            sb.append(targetSubscription);
            String message = sb.toString();
            log.error(message);
            throw new Exception(message, ex);
        }
    }

    public void transferPaymentForFinance(Long paymentID, Subscription targetSubscription) throws Exception {
        Payment payment = null;
        try {

            HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
            User user = userFacade.find((Long) session.getAttribute("userID"));
            payment = paymentFacade.find(paymentID);
            AccountingTransaction trans = accTransFacade.transferPayment(targetSubscription, payment.getAccount(), payment, user);

            payment.setAccount(targetSubscription);
            payment.setContract(targetSubscription.getAgreement());
            payment.setSubscriber_id(targetSubscription.getSubscriber().getId());
            payment.setServiceId(targetSubscription.getService());
            paymentFacade.update(payment);

            payment.setDsc(String.format("%s Payment amount transfered to subscription %s, accountingTransaction=%d", (payment.getDsc() != null ? payment.getDsc() : ""), targetSubscription.getAgreement(), trans.getId()));
        } catch (Exception ex) {
            StringBuilder sb = new StringBuilder("Cannot transfer payment=");
            sb.append(payment);
            sb.append("to subscription=");
            sb.append(targetSubscription);
            String message = sb.toString();
            log.error(message);
            throw new Exception(message, ex);
        }
    }

    public void transferBalanceForFinance(Long subscriptionID, Long targetID) throws Exception {
        Subscription subscription = null;
        Subscription targetSubscription = null;

        try {
            HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
            User user = userFacade.find((Long) session.getAttribute("userID"));
            subscription = find(subscriptionID);
            targetSubscription = find(targetID);
            //transFacade.transferPaymentToSubscription(payment, targetSubscription);
            accTransFacade.transferBalance(targetSubscription, subscription, user);
        } catch (Exception ex) {
            StringBuilder sb = new StringBuilder("Cannot transfer balance from subscription=");
            sb.append(subscription);
            sb.append("to subscription=");
            sb.append(targetSubscription);
            String message = sb.toString();
            log.error(message);
            throw new Exception(message, ex);
        }
    }

    public void transferBalanceForFinance(Long subscriptionID, Long targetID, Long amount) throws Exception {
        Subscription subscription = null;
        Subscription targetSubscription = null;

        try {
            HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
            User user = userFacade.find((Long) session.getAttribute("userID"));
            subscription = find(subscriptionID);
            targetSubscription = find(targetID);
            //transFacade.transferPaymentToSubscription(payment, targetSubscription);
            accTransFacade.transferBalance(targetSubscription, subscription, amount, user);
        } catch (Exception ex) {
            StringBuilder sb = new StringBuilder("Cannot transfer balance from subscription=");
            sb.append(subscription);
            sb.append("to subscription=");
            sb.append(targetSubscription);
            String message = sb.toString();
            log.error(message);
            throw new Exception(message, ex);
        }
    }

    public void transferBalanceForFinanceForFinance(Long paymentID, Long targetID) throws Exception {
        Payment payment = null;
        Subscription targetSubscription = null;

        try {
            payment = paymentFacade.find(paymentID);
            targetSubscription = find(targetID);
            transFacade.transferPaymentToSubscription(payment, targetSubscription);
            payment.setDsc(String.format("%s Payment transfered from subscription %s", (payment.getDsc() != null ? payment.getDsc() : ""), payment.getAccount().getAgreement()));
            payment.setAccount(targetSubscription);
            payment.setSubscriber_id(targetID);
            payment.setServiceId(targetSubscription.getService());
        } catch (Exception ex) {
            StringBuilder sb = new StringBuilder("Cannot transfer payment=");
            sb.append(payment);
            sb.append("to subscription=");
            sb.append(targetSubscription);
            String message = sb.toString();
            log.error(message);
            throw new Exception(message, ex);
        }
    }

    public void findByAgreement(String agreement) throws DuplicateAgreementException {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root root = query.from(Subscription.class);
        query.select(cb.count(root));
        query.where(cb.equal(root.get("agreement"), agreement));
        Long numberOfSbns = getEntityManager().createQuery(query).getSingleResult();

        if (numberOfSbns > 0) {
            throw new DuplicateAgreementException();
        }

    }

    public Subscription findByAgreementOrdinary(String agreement) {
        try {
            CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
            CriteriaQuery<Subscription> query = cb.createQuery(Subscription.class);
            Root root = query.from(Subscription.class);
            query.select(root);
            query.where(cb.equal(root.get("agreement"), agreement));
            return getEntityManager().createQuery(query).getSingleResult();
        } catch (NoResultException ex) {
            log.error("Subscription didnt find "+ex);
            return null;
        }
    }



    public Subscription findSubscription(String agreement, long provider_id) {
        try {

            Subscription subscription = em.createQuery("select sup from Subscription sup " +
                    "left join fetch sup.service service " +
                    "left join fetch sup.details " +
                    "left join fetch sup.settings setting " +
                    "left join fetch setting.properties property " +
                    "where service.provider.id = :providerId " +
                    "and sup.agreement = :agreement ", Subscription.class)
                    .setParameter("providerId", provider_id)
                    .setParameter("agreement", agreement).getSingleResult();

            return subscription;
        } catch (NoResultException ex) {
            log.error("Subscription didnt find "+ex);
            return null;
        }
    }



    public Subscription findSubscsforKapital(String agreement) {


        Subscription subscription = em.createQuery("select sup from Subscription sup " +
                "left join fetch sup.service service " +
                "left join fetch sup.details " +
                "left join fetch sup.settings setting " +
                "left join fetch setting.properties property " +
                "where service.provider.id = :providerId " +
                "and sup.agreement = :aggrement " +
                "and property.type = :type ", Subscription.class)
                .setParameter("providerId", Providers.DATAPLUS.getId())
                .setParameter("aggrement", agreement)
                .setParameter("type", ServiceSettingType.BROADBAND_SWITCH).getSingleResult();


        return subscription;

    }

    public Subscription findByRuntimeShaHash(String shaHash) {
        try {
            return getEntityManager().createQuery("select s from Subscription s where s.runtimeDetails.shaHash = :hashValue", Subscription.class)
                    .setParameter("hashValue", shaHash).getSingleResult();
        } catch (Exception ex) {
            return null;
        }
    }

    public Subscription findSubscriptionByAgreement(String agreement) {
        try {
            return em.createQuery("select s from Subscription s where " +
                    " s.agreement = :agree", Subscription.class).
                    setParameter("agree", agreement).setMaxResults(1).getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }



    public Subscription getBySetting(ServiceSettingType settingType, String settingValue) {
        try {
            return em.createQuery("select s from Subscription s join s.settings sett on sett.value = :settingValue and" +
                    " sett.properties.type = :settingType", Subscription.class).
                    setParameter("settingValue", settingValue).
                    setParameter("settingType", settingType).setMaxResults(1).getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }


    public Subscription findByEmailHash(String emailHash) {
        try {
            return getEntityManager().createQuery("select s from Subscription s where s.runtimeDetails.emailChecksum is not null and " +
                    "s.runtimeDetails.emailChecksum = :hashValue", Subscription.class)
                    .setParameter("hashValue", emailHash).getSingleResult();
        } catch (Exception ex) {
            return null;
        }
    }

    public List<Invoice> findInvoicesForSubscriber(String agreement, String startDate, String endDate) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd.MM.yyyy");
        Date start_date = formatter.parseDateTime(startDate).toDate();
        Date end_date = formatter.parseDateTime(endDate).toDate();

        try {
            return em.createQuery("SELECT inv FROM Subscription sbn JOIN sbn.subscriber sub JOIN sub.invoices inv "
                    + "WHERE sbn.agreement = :agreement and inv.creationDate >= :startDate and "
                    + " inv.creationDate <= :endDate", Invoice.class)
                    .setParameter("agreement", agreement)
                    .setParameter("startDate", start_date)
                    .setParameter("endDate", end_date)
                    .getResultList();
        } catch (Exception ex) {
            return null;
        }
    }

    public String findPasswordByAgreement(String agreement) {
        try {
            return em.createQuery("select sbn.details.password from Subscription sbn where sbn.agreement = :agreement", String.class)
                    .setParameter("agreement", agreement)
                    .getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

    public boolean isAvailableOnEcare(String agreement) {
        boolean available = true;
        try {
            SubscriptionDetails details = (SubscriptionDetails) em.createQuery("select sbn.details from Subscription sbn where sbn.agreement = :agreement")
                    .setParameter("agreement", agreement)
                    .getSingleResult();

            available = details.isAvailableEcare();
        } catch (NoResultException ex) {
            available = false;
        }
        return available;
    }

    public List<Subscription> findAllEquipmentPaginated() {
        return em.createQuery("select distinct sbn from Subscription sbn join SubscriptionSetting st where st.value like '%1%'", Subscription.class)
                .getResultList();
    }

    public List<Subscription> findBySubscriberId(long subscriber_id) {
        return em.createQuery("select sbn from Subscription sbn where sbn.subscriber.id = :sub_id", Subscription.class)
                .setParameter("sub_id", subscriber_id)
                .getResultList();
    }

    public List<Long> findSubscriptionIDsBySubscriberId(long subscriber_id) {
        return em.createQuery("select sbn.id from Subscription sbn where sbn.subscriber.id = :sub_id", Long.class)
                .setParameter("sub_id", subscriber_id)
                .getResultList();
    }

    @Override
    protected <Z, X> From<Z, X> getWhereRoot(CriteriaQuery query, From root) {
        if (getFilters().containsKey(Filter.EQUIPMENT_PARTNUMBER)
                || getFilters().containsKey(Filter.MINIPOP_MAC_ADDRESS)
                || getFilters().containsKey(Filter.MINIPOP_PORT)
                || getFilters().containsKey(Filter.SETTING_TYPE)) {
            return root.join("settings");
        } else {
            return root;
        }
    }

    @Override
    protected <Z, X> From<Z, X> getWhereRootWithFilters(CriteriaQuery query, From root, Filters filterSet) {
        if (filterSet.getFilters().containsKey(Filter.EQUIPMENT_PARTNUMBER)
                || filterSet.getFilters().containsKey(Filter.MINIPOP_MAC_ADDRESS)
                || filterSet.getFilters().containsKey(Filter.MINIPOP_PORT)
                || filterSet.getFilters().containsKey(Filter.SETTING_TYPE)) {
            return root.join("settings");
        } else {
            return root;
        }
    }

    private void changeExpirationDate(Subscription subscription, boolean isUp) {
        if (billSettings.getSettings().getPrepaidlifeCycleLength() == 30) {
            if (isUp) {
                subscription.setExpirationDate(DateTime.now().plusMonths(1));
            } else {
                subscription.setExpirationDate(DateTime.now());
            }
        } else if (isUp) {
            subscription.setExpirationDate(DateTime.now().plusDays(billSettings.getSettings().getPrepaidlifeCycleLength()));
        } else {
            subscription.setExpirationDate(DateTime.now());
        }

        subscription.synchronizeExpiratioDates();
        subscription.setExpirationDateWithGracePeriod(DateTime.now()
        );
    }

    public void shiftUpExpirationDate(Subscription subscription) {
        changeExpirationDate(subscription, true);
    }

    public void shiftBackExpirationDate(Subscription subscription) {
        changeExpirationDate(subscription, false);
    }

    public void addContractFile(Contract contract, Subscription subscription) {
        contract.setUser(userFacade.findByUserName(ctx.getCallerPrincipal().getName()));
        subscription.setContract(contract);
        em.persist(contract);
        update(subscription);
    }

    public void addAddendum(Addendum addendum, Subscription subscription) {
        if (subscription == null) {
            throw new IllegalArgumentException("Subscription is required, none provided");
        }

        if (subscription.getContract() == null) {
            throw new IllegalStateException("Subscription does not have the contract");
        }

        addendum.setUser(userFacade.findByUserName(ctx.getCallerPrincipal().getName()));

        em.persist(addendum);
        subscription.getContract().addAddendum(addendum);
        em.merge(subscription.getContract());
    }

    public Addendum findAddendum(long addendumID) {
        return em.createQuery("select adm from Addendum adm where adm.id = :admID", Addendum.class)
                .setParameter("admID", addendumID).getSingleResult();

    }

    public Addendum updateAddendum(Addendum addendum) {
        addendum.setUser(userFacade.findByUserName(ctx.getCallerPrincipal().getName()));
        return em.merge(addendum);
    }

    public Contract updateContract(Contract contract) {
        contract.setUser(userFacade.findByUserName(ctx.getCallerPrincipal().getName()));
        return em.merge(contract);
    }

    public Subscription addChangeStatusJob(Subscription subscription, SubscriptionStatus newStatus, DateTime startDate, DateTime expiresDate) {
        if (subscription == null || newStatus == null || startDate == null || (expiresDate != null && expiresDate.isBefore(startDate)) || startDate.isBeforeNow()) {
            throw new IllegalArgumentException(String.format("changeStatus arguments cannot be null, provided newStatus=%s, subscription=%s", newStatus, subscription));
        }

        SubscriptionStatus fromStatus = subscription.getStatus();

        StatusChangeRule rule = subscription.getService().findRule(subscription.getStatus(), newStatus);
        Long vasId = ((rule != null) ? rule.getVas().getId() : null);

        if (startDate.isAfter(DateTime.now()) && startDate.isBefore(DateTime.now().plusMinutes(61))) {
            if (newStatus == SubscriptionStatus.SUSPENDED) {
                List<ValueAddedService> suspensionVasList = vasFacade.findSuspensionVasListBySubscription(subscription);
                if (suspensionVasList != null) {
                    for (final ValueAddedService vas : suspensionVasList) {
                        subscription = engineFactory.getOperationsEngine(subscription).addVAS(
                                new VASCreationParams.Builder().
                                        setSubscription(subscription).
                                        setValueAddedService(vas).
                                        setStartDate(startDate).
                                        setExpiresDate(expiresDate).
                                        build()
                        );
                    }
                }
            } else if (rule != null && rule.getInitialStatus() == fromStatus && rule.getFinalStatus() == newStatus) {
                subscription = engineFactory.getOperationsEngine(subscription).addVAS(
                        new VASCreationParams.Builder().
                                setSubscription(subscription).
                                setValueAddedService(rule.getVas()).
                                setStartDate(startDate).
                                setExpiresDate(expiresDate).
                                build()
                );
            }
            subscription = engineFactory.getOperationsEngine(subscription).changeStatus(subscription, newStatus);
        } else {
            jobFacade.createStatusChangeJob(subscription, startDate, fromStatus, newStatus, vasId, expiresDate, false);
        }

        if (expiresDate != null && fromStatus != SubscriptionStatus.INITIAL) {
            jobFacade.createStatusChangeJob(subscription, expiresDate, newStatus, fromStatus, vasId, null, true);
        }
        return subscription;
    }

    public Subscription closeOpenInvoices(Subscription subscription) {
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
        subscription = update(subscription);
        log.info(String.format("closed invoices with non-credit vas charges. subscription id = %d", subscription.getId()));
        return subscription;
    }

    public Subscription removeAllVasList(Subscription subscription) {
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

    public boolean applyLateFee(Subscription sub, long rate) {
        try {
            Invoice newInvoice = null;
            Subscriber subscriber = sub.getSubscriber();
            for (Invoice inv : subscriber.getInvoices()) {
                if (inv.getState() == InvoiceState.OPEN
                        && new DateTime(inv.getCreationDate()).isAfter((DateTime.now().withTimeAtStartOfDay()))) {
                    newInvoice = inv;
                }
            }

            //new subscription - never billed
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
            lateFeeCharge.setAmount(rate);
            lateFeeCharge.setSubscriber(subscriber);
            lateFeeCharge.setUser_id(20000L);
            lateFeeCharge.setDsc("LateFee");
            lateFeeCharge.setDatetime(DateTime.now());
            lateFeeCharge.setSubscription(sub);
            chargeFacade.save(lateFeeCharge);

            Transaction transDebitServiceFee = new Transaction(
                    TransactionType.DEBIT,
                    sub,
                    rate,
                    "LateFee"
            );
            transDebitServiceFee.execute();
            transFacade.save(transDebitServiceFee);
            lateFeeCharge.setTransaction(transDebitServiceFee);

            newInvoice.addChargeToList(lateFeeCharge);

            log.info(
                    String.format("Charge for late fee: subscription id=%d, agreement=%s, amount=%d",
                            sub.getId(), sub.getAgreement(), lateFeeCharge.getAmount())
            );

            systemLogger.success(
                    SystemEvent.CHARGE,
                    sub,
                    transDebitServiceFee,
                    String.format("Charge id=%d for LateFee, amount=%s",
                            lateFeeCharge.getId(), lateFeeCharge.getAmountForView()
                    )
            );

            return true;
        } catch (Exception ex) {

            log.info("Cannot charge for LateFee: " + ex);
            ex.printStackTrace();
            return false;
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

    public void finalizeSubscription(Subscription subscription) {
        /**
         * TODO: close and return to poll all static IPs TODO: release all stock
         * items (minipops)
         */
        for (SubscriptionVAS sbnVAS : subscription.getVasList()) {
            engineFactory.getOperationsEngine(subscription).
                    removeVAS(subscription, sbnVAS);
        }

        for (SubscriptionResource sbnResource : subscription.getResources()) {
            for (SubscriptionResourceBucket sbnBucket : sbnResource.getBucketList()) {

            }
        }
    }

    public void disconnectSession(Subscription subscription) throws Exception {
        String msg = "subscription id=" + subscription.getId();
        try {
            ProvisioningEngine provisioner = engineFactory.getProvisioningEngine(subscription);
            provisioner.disconnect(subscription);
            log.info("disconnectSession: sucessfully disconnected session, " + msg);
            systemLogger.success(SystemEvent.SUBSCRIPTION_DISCONNECT_SESSION, subscription, msg);
        } catch (ProvisionerNotFoundException | ProvisioningException ex) {
            log.error("disconnectSession: cannot disconnect session, " + msg, ex);
            systemLogger.success(SystemEvent.SUBSCRIPTION_DISCONNECT_SESSION, subscription, msg);
            throw new Exception(msg, ex);
        }
    }

    public void registerServiceChangeJob(Subscription selected, Service targetService, Date startDate) {
        jobFacade.createServiceChangeJob(selected, targetService, startDate);
    }

    public Subscription attachSubscription(Subscription subscription, Subscriber subscriber) {
        subscription.setSubscriber(subscriber);
        return update(subscription);
    }

    public void increasePromoBalance(Subscription subscription, long amount) {
        long promoBalance = subscription.getBalance().getPromoBalance();
        log.debug(String.format("increasePromoBalance: subscription id=%d, amount=%d, current promo balance=%d",
                subscription.getId(), amount, promoBalance));
        subscription.getBalance().setPromoBalance(promoBalance + amount);
        log.debug(String.format("increasePromoBalance: after increment - subscription id=%d, amount=%d, current promo balance=%d",
                subscription.getId(), amount, promoBalance));
        update(subscription);
    }

    public void usePromoBalance(Subscription subscription, long amount) {
        Transaction transDebitPromoBalance = new Transaction(
                TransactionType.DEBIT_PROMO,
                subscription,
                amount,
                "Promo balance transfer"
        );
        transDebitPromoBalance.executePromo();
        transFacade.save(transDebitPromoBalance);

        Transaction transCreditRealBalance = new Transaction(
                TransactionType.CREDIT,
                subscription,
                amount,
                "Promo balance transfer"
        );
        transCreditRealBalance.execute();
        transFacade.save(transCreditRealBalance);
    }

    public boolean activatePrepaidOnAdjustment(long subscriptionID, long accountingTransactionID, String msg) {
        Subscription subscription = find(subscriptionID);
        String header = String.format("subscription id=%d, agreement=%s", subscription.getId(), subscription.getAgreement());
        boolean res = false;
        try {
            AccountingTransaction accountingTransaction = accTransFacade.find(accountingTransactionID);
            Operation operation = accountingTransaction.getOperations().get(0);
            List<Invoice> openInvoiceList = invoiceFacade.findOpenBySubscriberForPayment(subscription.getSubscriber().getId());

            log.info(String.format("%s, Found open invoices: %s", header, openInvoiceList));

            if (openInvoiceList != null && !openInvoiceList.isEmpty()) {
                Invoice invoice = openInvoiceList.get(openInvoiceList.size() - 1);

                if (Math.abs(invoice.getBalance()) <= operation.getAmount()) {
                    invoice.setState(InvoiceState.CLOSED);
                    invoice.setCloseDate(DateTime.now());
                    log.info(String.format("activatePrepaidOnAdjustment: %s, %s, activation successfull", header, msg));

                    systemLogger.success(SystemEvent.INVOICE_CLOSED, subscription,
                            String.format("%s, %s, invoice id=%d, invoice balance=%d, accounting transaction id=%d, operation id=%d, operation amount=%d",
                                    header, msg, invoice.getId(), invoice.getBalance(), accountingTransactionID, operation.getId(), operation.getAmount()));

                    engineFactory.getOperationsEngine(subscription).activatePrepaid(subscription);
                    ProvisioningEngine provisioner = engineFactory.getProvisioningEngine(subscription);

                    res = provisioner.openService(subscription, subscription.getExpirationDateWithGracePeriod());

                    if (res) {
                        log.info(String.format("activatePrepaidOnAdjustment: %s, %s, activation successfull", header, msg));
                        systemLogger.success(SystemEvent.SUBSCRIPTION_PROLONGED, subscription, msg);
                        return res;
                    } else {
                        throw new ProvisioningException(String.format("%s, %s, provisioning unsuccessfull", header, msg));
                    }
                } // end if invoice balance
            } // end if open invoice found
            return res;
        } catch (Exception ex) {
            ctx.setRollbackOnly();
            log.error(String.format("activatePrepaidOnAdjustment: %s, %s, activation failed", header, msg), ex);
            systemLogger.error(SystemEvent.SUBSCRIPTION_PROLONGED, subscription, msg + ", " + ex.getCause().getMessage());
            return false;
        }
    }

    public boolean tryReactivate(Subscription subscription) {
        SubscriptionReactivation reactivation = reactivationFacade.findPendingReactivation(subscription);

        if (reactivation == null) {
            return false;
        }

        long totalCharge = subscription.calculateTotalCharge();

        String dsc = String.format("realBalance=%d, totalCharge=%d", subscription.getBalance().getRealBalance(), totalCharge);

        if (totalCharge <= subscription.getBalance().getRealBalance()) {
            User user = userFacade.findByUserName("system");
            long serviceFee = subscription.getService().getServicePrice();

            invoiceFacade.addServiceChargeToInvoice(null, subscription, serviceFee, user.getId(), "Autocharged for service fee during reactivation");
            //chargeFacade.chargeForService(subscription, subscription., "Autocharged for service fee", user.getId());

            if (subscription.getVasList() != null && !subscription.getVasList().isEmpty()) {
                long vasPrice = 0;
                Long vasRate = null;
                //long userID = userFacade.findByUserName("system").getId();

                for (SubscriptionVAS vas : subscription.getVasList()) {
                    if (vas.getVas().getCode().getType() == ValueAddedServiceType.PERIODIC_STATIC) {
                        vasPrice = vas.getPrice();

                        if (vas.getStatus() == SubscriptionStatus.FINAL) {
                            continue;
                        }

                        vasRate = vasPrice;

                        invoiceFacade.addVasChargeToInvoice(null, subscription, vasRate, vas.getVas(), user.getId(), String.format("Autocharged for vas=%s during reactivation", vas.getVas().getName()));
                        //chargeFacade.chargeForVAS(subscription, vasRate, "Autocharged for vas" + vas.getVas().getName(), user.getId(), vas.getVas());
                    }
                }
            }
            reactivationFacade.reactivate(reactivation);
            systemLogger.success(SystemEvent.SUBSCRIPTION_REACTIVATION, subscription, dsc);
            return true;
        } else {
            systemLogger.error(SystemEvent.SUBSCRIPTION_REACTIVATION, subscription, dsc);
            return false;
        }
    }

    public List<Subscription> findLatelyPaidAndNotProlonged() {
        return em.createQuery("select s from Subscription s where s.lastPaymentDate > :startDate and s.balance.realBalance >= 0 and s.expirationDateWithGracePeriod < s.billedUpToDate", Subscription.class)
                .setParameter("startDate", DateTime.now().minusHours(48))
                .getResultList();
    }

    public void removeDebt(Subscription subscription, String dsc) {
        invoiceFacade.cancelInvoice(subscription, dsc);

        if (subscription.getStatus() == SubscriptionStatus.BLOCKED) {
            engineFactory.getOperationsEngine(subscription).activatePrepaid(subscription);
        }
    }

    public Subscription overwriteInvoice(Subscription subscription, long charge, String dsc) {
        Invoice openInvoice = invoiceFacade.cancelInvoice(subscription, dsc);

        if (openInvoice == null) {
            log.info(String.format("overwriteInvoice: subscription agreement=%s no open invoice found.Skippping invoice creation", subscription.getAgreement()));
            return subscription;
        }

        Invoice invoice = invoiceFacade.createInvoiceForSubscriber(subscription.getSubscriber());
        invoice.setSubscription(subscription);

        Long userID = userFacade.getIdByUserName(ctx.getCallerPrincipal().getName());
        invoiceFacade.addServiceChargeToInvoice(invoice, subscription, charge, userID, String.format("Charge for service id=%d", subscription.getService().getId()));

        systemLogger.success(SystemEvent.INVOICE_CREATED, subscription, "invoice id=" + invoice.getId());

        try {
            queueManager.sendInvoiceNotification(BillingEvent.INVOICE_CREATED, subscription, invoice);
            String msg = String.format("event=%s, subscription id=%d", BillingEvent.INVOICE_CREATED, subscription.getId());
            systemLogger.success(SystemEvent.NOTIFICATION_SCHEDULED, subscription, msg);
            log.info("Notification added: " + msg);
        } catch (Exception ex) {
            String msg = String.format("event=%s, subscription id=%d, invoice id=%d", BillingEvent.INVOICE_CREATED, subscription.getId(), invoice.getId());
            systemLogger.error(SystemEvent.NOTIFICATION_SCHEDULED, subscription, msg);
        }

        return update(subscription);
    }

    public String findSubscriptionByIp(Map<Filterable, Object> filters) {
        String sqlQuery = "select distinct vas.subscription from SubscriptionVAS vas join vas.resource res join res.bucketList bck";
        String where = " where bck.capacity like '%"
                + filters.get(SubscriptionPersistenceFacade.Filter.IP) + "%'"
                + " and bck.type = :type";
        filters.remove(SubscriptionPersistenceFacade.Filter.IP);
        return sqlQuery + where;
    }

    public String searchByKeyword(String keyword) {
        String sqlQuery = "select distinct s from Subscription s join s.settings stg join s.subscriber sub join sub.details det ";
        String where = "where s.agreement like '%" + keyword + "%' or s.identifier like '%" + keyword + "%' "
                + "or lower(det.city) like '%" + keyword.toLowerCase() + "%' or lower(det.street) like '%" + keyword.toLowerCase() + "%' or det.phoneMobile like '%" + keyword + "%'  "
                + "or lower(det.passportNumber) like '%" + keyword.toLowerCase() + "%' or lower(stg.value) like '%" + keyword.toLowerCase() + "%'";
        return sqlQuery + where;
    }
//      old version which throws exception
//    public String searchByKeyword(String keyword) {
//        String sqlQuery = "select distinct s from Subscription s join s.settings stg join s.subscriber sub join sub.details det ";
//        String where = "where s.agreement like '%" + keyword + "%' or s.identifier like '%" + keyword + "%' or concat( lower(det.firstName) , ' ' , lower(det.surname) ) like '%" + keyword.toLowerCase() + "%'  "
//                + "or lower(det.city) like '%" + keyword.toLowerCase() + "%' or lower(det.street) like '%" + keyword.toLowerCase() + "%' or det.phoneMobile like '%" + keyword + "%'  "
//                + "or lower(det.passportNumber) like '%" + keyword.toLowerCase() + "%' or lower(stg.value) like '%" + keyword.toLowerCase() + "%'";
//        return sqlQuery + where;
//    }

    public MiniPop findMinipop(Subscription sub) {
        try {
            for (SubscriptionSetting setting : sub.getSettings()) {
                if (setting.getProperties().getType() == ServiceSettingType.BROADBAND_SWITCH) {
                    MiniPop miniPop = miniPopFacade.find(Long.valueOf(setting.getValue()));
//                    log.debug("minipop: " + miniPop); // logu doldurur
                    return miniPop;
                }
            }
        } catch (Exception ex) {
            log.debug(ex);
            return null;
        }
        return null;
    }

    public List<Ats> getAtsList() {
        return em.createQuery("select s from Ats s", Ats.class).getResultList();
    }

    public List<Streets> getStreetsOfAts(String atsIndex) {

        Ats ats = em.createQuery("select s from Ats s where s.atsIndex = '" + atsIndex + "'", Ats.class)
                .getSingleResult();

        return em.createQuery("select s from Streets s where s.atsIndex = '" + ats.getId() + "' order by s.name ASC", Streets.class)
                .getResultList();
    }

    public List<Streets> getStreetsOfAts(long atsID) {
        return em.createQuery("select s from Streets s where s.atsIndex=:id", Streets.class)
                .setParameter("id", String.valueOf(atsID)).getResultList();
    }

    public boolean updateAccount(Subscription subscription) {
        try {
            ProvisioningEngine provisioner = engineFactory.getProvisioningEngine(subscription);
            boolean res = provisioner.updateAccount(subscription);
            log.debug("Update account provisioner: " + res);
            return res;
        } catch (Exception ex) {
            log.debug("Error occur during update account: " + subscription.getAgreement());
            return false;
        }
    }

    private static class TimeField {

        public String desc;
        public DateTime before;
        public DateTime after;

        public TimeField(String desc) {
            this.desc = desc;
        }

    }

    public JSONArray addCompensation(Subscription subscription, CompensationDetails details) {
        List<Compensation> compensationList = subscription.getCompensationList();
        if (compensationList == null) {
            compensationList = new ArrayList<>();
            subscription.setCompensationList(compensationList);
        }

        TimeField exprF = new TimeField("expirationDate");
        //update expiration date
        DateTime exprDate = subscription.getExpirationDate();
        if (!subscription.getStatus().equals(SubscriptionStatus.ACTIVE)) {
            exprDate = DateTime.now();
        }
        exprF.before = exprDate;
        exprDate = exprDate.plusDays(details.getDayCount());
        subscription.setExpirationDate(exprDate);
        exprF.after = subscription.getExpirationDate();

        //update billed date
        TimeField billedF = new TimeField("BilledUpToDate");
        DateTime billedDate = subscription.getBilledUpToDate();
        billedF.before = billedDate;
        if (!subscription.getStatus().equals(SubscriptionStatus.ACTIVE)) {
            billedDate = exprDate.plusMonths(1);
        } else {
            billedDate = billedDate.plusDays(details.getDayCount());
        }
        subscription.setBilledUpToDate(billedDate);
        billedF.after = subscription.getBilledUpToDate();

        //update expiration date with grace period
        TimeField exprGF = new TimeField("expiration with grace period");
        exprGF.before = subscription.getExpirationDateWithGracePeriod();
        subscription.setExpirationDateWithGracePeriod(
                subscription.getExpirationDate()
        );
        exprGF.after = subscription.getExpirationDateWithGracePeriod();

        if (!subscription.getStatus().equals(SubscriptionStatus.ACTIVE)) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setLastStatusChangeDate(DateTime.now());
        }
        subscription.setLastUpdateDate();

        JSONArray fieldList = new JSONArray();
        JSONObject objectF = new JSONObject();
        objectF.put("field", exprF.desc);
        objectF.put("before", exprF.before);
        objectF.put("after", exprF.after);
        fieldList.add(objectF);

        JSONObject objectB = new JSONObject();
        objectB.put("field", billedF.desc);
        objectB.put("before", billedF.before);
        objectB.put("after", billedF.after);
        fieldList.add(objectB);

        JSONObject objectG = new JSONObject();
        objectG.put("field", exprGF.desc);
        objectG.put("before", exprGF.before);
        objectG.put("after", exprGF.after);
        fieldList.add(objectG);

        Compensation comp = new Compensation();
        comp.setFromDate(new java.sql.Date(details.getFromDate().getTime()));
        comp.setDayCount(details.getDayCount());
        comp.setComments(details.getComments());
        comp.setTicketId(details.getTicketId());
        comp.setSubscription(subscription);
        compensationList.add(comp);
        return fieldList;
    }

    public void removeDetached(Subscription subscription) {
        Subscription entity = getEntityManager().getReference(Subscription.class, subscription.getId());
        getEntityManager().remove(entity);
    }

    public boolean hasCreditVas(Subscription subscription) {
        List<SubscriptionVAS> subVas = subscription.getVasList();
        boolean hass = false;

        for (SubscriptionVAS vas : subVas) {

            if (vas.getVas().isCredit()) {
                hass = true;
            }
        }

        return hass;
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void removePromo(CampaignRegister selectedRegister) {
        Subscription subscription = selectedRegister.getSubscription();
        subscription = findForceRefresh(subscription.getId());
        if (selectedRegister.getStatus().equals(CampaignStatus.ACTIVE)
                && selectedRegister.getCampaign().getTarget().equals(CampaignTarget.PAYMENT)
                && subscription.getBalance() != null
                && subscription.getBalance().getPromoBalance() >= selectedRegister.getBonusAmount()) {
            subscription = update(subscription);
            long promobalance = subscription.getBalance().getPromoBalance();
            promobalance -= selectedRegister.getBonusAmount();
            subscription.getBalance().setPromoBalance(promobalance);
            update(subscription);
            systemLogger.success(
                    SystemEvent.SUBSCRIPTION_PROMO_BALANCE_DEBIT,
                    subscription,
                    String.format("cancelled campaign id=%d, promo debit=%d", selectedRegister.getCampaign().getId(), selectedRegister.getBonusAmount()));

        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean rollbackBonusDate(CampaignRegister selectedRegister) {
        Subscription subscription = selectedRegister.getSubscription();
        if (selectedRegister.getStatus().equals(CampaignStatus.ACTIVE)
                && selectedRegister.getCampaign().getTarget().equals(CampaignTarget.EXPIRATION_DATE)) {
            subscription = update(subscription);
            DateTime nobonusDate = selectedRegister.getNobonusDate();
            if (nobonusDate == null && subscription.getActivationDate() != null) {
                nobonusDate = subscription.getActivationDate().plusMonths(1).withTime(23, 59, 59, 999);
            }
            if (nobonusDate == null) {
                return false;
            }

            subscription.setExpirationDate(nobonusDate);
            subscription.setBilledUpToDate(subscription.getExpirationDate());
            subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDate().plusDays(
                    subscription.getBillingModel().getGracePeriodInDays()));
            subscription = update(subscription);
        }
        try {
            engineFactory.getProvisioningEngine(subscription).reprovision(subscription);
        } catch (Exception ex) {
            log.error("Cannot reprovision: subscription id=" + subscription.getId(), ex);
        }
        return true;
    }

    public Subscription findConvergent(String msisdn) {
        try {
            return em.createQuery("select s from Subscription s where (s.service.provider.id=454106 or s.service.provider.id=454114)" +
                    "and substring(s.agreement,0,9)=:msisdn order by s.id desc", Subscription.class)
                    .setParameter("msisdn", msisdn)
                    .getResultList().get(0);
        } catch (Exception ex) {
            return null;
        }
    }

    public void persist(Object entity) {
        em.persist(entity);
    }

    public SubscriptionVAS merge(SubscriptionVAS entity) {
        return em.merge(entity);
    }

    public void remove(Object entity) {
        em.remove(entity);
    }

    public List<IpAddress> getIpAddressList() {
        return em.createQuery("select a from IpAddress a", IpAddress.class).getResultList();
    }

    public List<IpAddress> getIpAddressList(String ipAddress) {
        return em.createQuery("select a from IpAddress a where a.addressAsString = :addr", IpAddress.class).setParameter("addr", ipAddress).getResultList();
    }

    public List<IpAddressRange> getIpAddressList(long id) {
        return em.createQuery("select r from IpAddressRange r join r.reservedAddressList a where a.id = :addr", IpAddressRange.class)
                .setParameter("addr", id).getResultList();
    }

    public Subscription addVAS(
            Subscription selected,
            ValueAddedService selectedVAS,
            Date vasExpirationDate,
            String ipAddressString,
            double vasAddCount,
            List<IpAddress> freeIpList
    ) {
        DateTime vasExpireDateTime = vasExpirationDate == null ? null : new DateTime(vasExpirationDate.getTime() + 23 * 3600 * 1000 + 59 * 60 * 1000 + 59 * 1000);

        log.debug("SELECTED VAS: " + selectedVAS.getName() + " " + selectedVAS.getStaticIPType());

        IpAddress ipAddress = null;
        campaignRegisterFacade.tryActivateCampaignOnVasAdd(selected, selectedVAS);
        if (selectedVAS.getCode().getType() == ValueAddedServiceType.PERIODIC_STATIC && selectedVAS.getStaticIPType() == StaticIPType.NORMAL_CHARGED
                && ipAddressString != null && !ipAddressString.isEmpty()) {
            for (IpAddress address : freeIpList) {
                if (address.getAddressAsString().equals(ipAddressString)) {
                    ipAddress = address;
                    break;
                }
            }
            log.debug("selected charged ip:" + ipAddress);

            selected = engineFactory.getOperationsEngine(selected).addVAS(
                    new VASCreationParams.Builder().
                            setSubscription(selected).
                            setValueAddedService(selectedVAS).
                            setIpAddress(ipAddress).
                            setExpiresDate(vasExpireDateTime).
                            build()
            );
        } else if (selectedVAS.getCode().getType() == ValueAddedServiceType.PERIODIC_STATIC && selectedVAS.getStaticIPType() == StaticIPType.COMMANDANT_FREE
                && ipAddressString != null && !ipAddressString.isEmpty()) {
            for (IpAddress address : freeIpList) {
                if (address.getAddressAsString().equals(ipAddressString)) {
                    ipAddress = address;
                    break;
                }
            }
            log.debug("selected free ip:" + ipAddress);

            selected = engineFactory.getOperationsEngine(selected).addVAS(
                    new VASCreationParams.Builder().
                            setSubscription(selected).
                            setValueAddedService(selectedVAS).
                            setIpAddress(ipAddress).
                            setExpiresDate(vasExpireDateTime).
                            build()
            );
        } else {
            log.debug("Auto give ip");
            selected = engineFactory.getOperationsEngine(selected).addVAS(
                    new VASCreationParams.Builder().
                            setSubscription(selected).
                            setValueAddedService(selectedVAS).
                            setCount(vasAddCount).
                            setExpiresDate(vasExpireDateTime).
                            build()
            );
        }
        return selected;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void synchronizeSubscription(UninetSynchroniser.SyncResult rs, Subscription subscription) {
        Subscription currentSubscription = update(subscription);

        log.info(currentSubscription);
        try {

            String Slot = null;
            String Port = null;
            String DSLAM = null;
            String Mac = null;
            String password = null;
            for (SubscriptionSetting ss : currentSubscription.getSettings()) {
                if (ss.getProperties().getType() == ServiceSettingType.BROADBAND_SWITCH_SLOT) {
                    Slot = ss.getValue();
                    if (Slot != null && !Slot.equalsIgnoreCase(rs.slot)) {
                        ss.setValue(rs.slot);
                    }
                    break;
                }
            }
            for (SubscriptionSetting ss : currentSubscription.getSettings()) {
                if (ss.getProperties().getType() == ServiceSettingType.BROADBAND_SWITCH_PORT) {
                    Port = ss.getValue();
                    if (Port != null && !Port.equalsIgnoreCase(rs.port)) {
                        ss.setValue(rs.port);
                    }
                    break;
                }
            }
            for (SubscriptionSetting ss : currentSubscription.getSettings()) {
                if (ss.getProperties().getType() == ServiceSettingType.BROADBAND_SWITCH_IP) {
                    DSLAM = ss.getValue();
                    if (DSLAM != null && !DSLAM.equalsIgnoreCase(rs.dslam)) {
                        ss.setValue(rs.dslam);
                    }
                    break;
                }
            }
            for (SubscriptionSetting ss : currentSubscription.getSettings()) {
                if (ss.getProperties().getType() == ServiceSettingType.MAC_ADDRESS) {
                    Mac = ss.getValue();
                    if (Mac != null && !Mac.equalsIgnoreCase(rs.mac)) {
                        ss.setValue(rs.mac);
                    }
                    break;
                }
            }

            for (SubscriptionSetting ss : currentSubscription.getSettings()) {
                if (ss.getProperties().getType() == ServiceSettingType.PASSWORD) {
                    password = ss.getValue();
                    if (password != null && !password.equalsIgnoreCase(rs.password)) {
                        ss.setValue(rs.password);
                    }
                    break;
                }
            }

            ServiceSetting serviceSetting = null;
            if (Mac == null) {
                SubscriptionSetting subscriptionSetting = new SubscriptionSetting();
                subscriptionSetting.setValue(rs.mac);//Port
                serviceSetting = commonOperationsEngine.createServiceSetting("MAC_ADDRESS", ServiceSettingType.MAC_ADDRESS, Providers.UNINET, ServiceType.BROADBAND, "");
                subscriptionSetting.setProperties(serviceSetting);
                settingFacade.save(subscriptionSetting);
                subscriptionSetting = settingFacade.update(subscriptionSetting);
                currentSubscription.getSettings().add(subscriptionSetting);
            }
            if (Slot == null) {
                SubscriptionSetting subscriptionSetting = new SubscriptionSetting();
                subscriptionSetting.setValue(rs.slot);//SLOT
                serviceSetting = commonOperationsEngine.createServiceSetting("BROADBAND_SWITCH_SLOT", ServiceSettingType.BROADBAND_SWITCH_SLOT, Providers.UNINET, ServiceType.BROADBAND, "");
                subscriptionSetting.setProperties(serviceSetting);
                settingFacade.save(subscriptionSetting);
                subscriptionSetting = settingFacade.update(subscriptionSetting);
                currentSubscription.getSettings().add(subscriptionSetting);
            }
            if (Port == null) {
                SubscriptionSetting subscriptionSetting = new SubscriptionSetting();
                subscriptionSetting.setValue(rs.port);//Port
                serviceSetting = commonOperationsEngine.createServiceSetting("BROADBAND_SWITCH_PORT", ServiceSettingType.BROADBAND_SWITCH_PORT, Providers.UNINET, ServiceType.BROADBAND, "");
                subscriptionSetting.setProperties(serviceSetting);
                settingFacade.save(subscriptionSetting);
                subscriptionSetting = settingFacade.update(subscriptionSetting);
                currentSubscription.getSettings().add(subscriptionSetting);
            }
            if (DSLAM == null) {
                SubscriptionSetting subscriptionSetting = new SubscriptionSetting();
                subscriptionSetting.setValue(rs.dslam);//DSLAM
                serviceSetting = commonOperationsEngine.createServiceSetting("BROADBAND_SWITCH_IP", ServiceSettingType.BROADBAND_SWITCH_IP, Providers.UNINET, ServiceType.BROADBAND, "");
                subscriptionSetting.setProperties(serviceSetting);
                settingFacade.save(subscriptionSetting);
                subscriptionSetting = settingFacade.update(subscriptionSetting);
                currentSubscription.getSettings().add(subscriptionSetting);
            }
            if (password == null) {
                SubscriptionSetting subscriptionSetting = new SubscriptionSetting();
                subscriptionSetting.setValue(rs.password);//password
                serviceSetting = commonOperationsEngine.createServiceSetting("PASSWORD", ServiceSettingType.PASSWORD, Providers.UNINET, ServiceType.BROADBAND, "");
                subscriptionSetting.setProperties(serviceSetting);
                settingFacade.save(subscriptionSetting);
                subscriptionSetting = settingFacade.update(subscriptionSetting);
                currentSubscription.getSettings().add(subscriptionSetting);
            }

            update(currentSubscription);
            log.info("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&SUCCESSFULL&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
        } catch (Exception e) {
            log.error(e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Subscription updateSubscriptionSIPOutgoingStatus(Subscription subscription, int sipOldOutgStatus){
        if(sipOldOutgStatus != 0){
            SubscriptionSetting subscriptionSetting = new SubscriptionSetting();
            subscriptionSetting.setValue(String.valueOf(sipOldOutgStatus));//SIP_OLD_OUTG_STATUS
            ServiceSetting serviceSetting = commonOperationsEngine.createServiceSetting("SIP_OLD_OUTG_STATUS", ServiceSettingType.SIP_OLD_OUTG_STATUS, Providers.CITYNET, ServiceType.SIP, "Sip number outgoing old status.");
            subscriptionSetting.setProperties(serviceSetting);
            settingFacade.save(subscriptionSetting);
            subscriptionSetting = settingFacade.update(subscriptionSetting);
            subscription.getSettings().add(subscriptionSetting);
            update(subscription);
        }
        return subscription;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<Object[]> findUnsynchronized() {
        List<Object[]> result = getEntityManager().createNativeQuery("SELECT  * FROM  radius_sync_detail").getResultList();
        return result;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void synchronizeSubscription(BackProvisionDetails details, Subscription subscription) {
        subscription = findForceRefresh(subscription.getId());
        Subscription currentSubscription = update(subscription);

        log.info("Before synchronize -> " + currentSubscription);
        try {
            MiniPop connectedMinipop = miniPopFacade.findBySwitchId(details.switchName);
            String connectedMinipopId = String.valueOf(connectedMinipop.getId());

            String Slot = null;
            String Port = null;
            String minipop = null;
            String mac = null;
            String password = null;

        /*
            for (SubscriptionSetting ss : currentSubscription.getSettings()) {
                if (ss.getProperties().getType() == ServiceSettingType.BROADBAND_SWITCH_SLOT) {
                    Slot = ss.getValue();
                    if (Slot != null && !Slot.equalsIgnoreCase(details.slot)) {
                        ss.setValue(details.slot);
                    }
                    break;
                }
            }
            for (SubscriptionSetting ss : currentSubscription.getSettings()) {
                if (ss.getProperties().getType() == ServiceSettingType.BROADBAND_SWITCH) {
                    minipop = ss.getValue();
                    if (minipop != null && !minipop.equalsIgnoreCase(connectedMinipopId)) {
                        ss.setValue(connectedMinipopId);
                    }
                    break;
                }
            }
            for (SubscriptionSetting ss : currentSubscription.getSettings()) {
                if (ss.getProperties().getType() == ServiceSettingType.BROADBAND_SWITCH_PORT) {
                    Port = ss.getValue();
                    if (Port != null && !Port.equalsIgnoreCase(details.port)) {
                        ss.setValue(details.port);
                    }
                    break;
                }
            }
            for (SubscriptionSetting ss : currentSubscription.getSettings()) {
                if (ss.getProperties().getType() == ServiceSettingType.MAC_ADDRESS) {
                    mac = ss.getValue();
                    if (mac != null && !mac.equalsIgnoreCase(details.mac)) {
                        ss.setValue(details.mac);
                    }
                    break;
                }
            }
        */


            for (SubscriptionSetting ss : currentSubscription.getSettings()) {
                switch (ss.getProperties().getType()){
                    case BROADBAND_SWITCH_SLOT:
                        Slot = ss.getValue();
                        if (Slot != null && !Slot.equals(details.slot))
                            ss.setValue(details.slot);
                        break;
                    case BROADBAND_SWITCH:
                        minipop = ss.getValue();
                        if (minipop != null && !minipop.equalsIgnoreCase(connectedMinipopId))
                            ss.setValue(connectedMinipopId);
                        break;
                    case BROADBAND_SWITCH_PORT:
                        Port = ss.getValue();
                        if (Port != null && !Port.equals(details.port))
                            ss.setValue(details.port);
                        break;
                    case MAC_ADDRESS:
                        mac = ss.getValue();
                        if (mac != null && !mac.equalsIgnoreCase(details.mac))
                            ss.setValue(details.mac);
                        break;
                    case PASSWORD:
                        password = ss.getValue();
                        if (password != null && !password.equals(details.password))
                            ss.setValue(details.password);
                        break;
                    default:
                        break;

                }
            }

            log.info(String.format("[Synchronize subscription=%s] After FOR loop: Slot=%s, Minipop=%s, Port=%s, Mac=%s, Password=%s", subscription.getId(), Slot, minipop, Port, mac, password));

/*
            ServiceSetting serviceSetting = null;
            if (minipop == null) {
                SubscriptionSetting subscriptionSetting = new SubscriptionSetting();
                subscriptionSetting.setValue(connectedMinipopId);//MINIPOP
                serviceSetting = commonOperationsEngine.createServiceSetting("BROADBAND_SWITCH", ServiceSettingType.BROADBAND_SWITCH, Providers.DATAPLUS, ServiceType.BROADBAND, "");
                subscriptionSetting.setProperties(serviceSetting);
                settingFacade.save(subscriptionSetting);
                subscriptionSetting = settingFacade.update(subscriptionSetting);
                currentSubscription.getSettings().add(subscriptionSetting);
            }
            if (Slot == null) {
                SubscriptionSetting subscriptionSetting = new SubscriptionSetting();
                subscriptionSetting.setValue(details.slot);//SLOT
                serviceSetting = commonOperationsEngine.createServiceSetting("BROADBAND_SWITCH_SLOT", ServiceSettingType.BROADBAND_SWITCH_SLOT, Providers.DATAPLUS, ServiceType.BROADBAND, "");
                subscriptionSetting.setProperties(serviceSebtting);
                settingFacade.save(subscriptionSetting);
                subscriptionSetting = settingFacade.update(subscriptionSetting);
                currentSubscription.getSettings().add(subscriptionSetting);
            }
            if (Port == null) {
                SubscriptionSetting subscriptionSetting = new SubscriptionSetting();
                subscriptionSetting.setValue(details.port);//Port
                serviceSetting = commonOperationsEngine.createServiceSetting("BROADBAND_SWITCH_PORT", ServiceSettingType.BROADBAND_SWITCH_PORT, Providers.DATAPLUS, ServiceType.BROADBAND, "");
                subscriptionSetting.setProperties(serviceSetting);
                settingFacade.save(subscriptionSetting);
                subscriptionSetting = settingFacade.update(subscriptionSetting);
                currentSubscription.getSettings().add(subscriptionSetting);
            }
            if (mac == null) {
                SubscriptionSetting subscriptionSetting = new SubscriptionSetting();
                subscriptionSetting.setValue(details.mac);//mac
                serviceSetting = commonOperationsEngine.createServiceSetting("MAC_ADDRESS", ServiceSettingType.MAC_ADDRESS, Providers.DATAPLUS, ServiceType.BROADBAND, "");
                subscriptionSetting.setProperties(serviceSetting);
                settingFacade.save(subscriptionSetting);
                subscriptionSetting = settingFacade.update(subscriptionSetting);
                currentSubscription.getSettings().add(subscriptionSetting);
            }
            if (password == null) {
                SubscriptionSetting subscriptionSetting = new SubscriptionSetting();
                subscriptionSetting.setValue(details.password); //password
                serviceSetting = commonOperationsEngine.createServiceSetting("PASSWORD", ServiceSettingType.PASSWORD, Providers.DATAPLUS, ServiceType.BROADBAND, "");
                subscriptionSetting.setProperties(serviceSetting);
                settingFacade.save(subscriptionSetting);
                subscriptionSetting = settingFacade.update(subscriptionSetting);
                currentSubscription.getSettings().add(subscriptionSetting);
            }
            */

            if (minipop == null) {
                SubscriptionSetting subscriptionSetting = createSubscriptionSetting(ServiceSettingType.BROADBAND_SWITCH, connectedMinipopId);
                currentSubscription.getSettings().add(subscriptionSetting);
            }
            if (Slot == null) {
                SubscriptionSetting subscriptionSetting = createSubscriptionSetting(ServiceSettingType.BROADBAND_SWITCH_SLOT, details.slot);
                currentSubscription.getSettings().add(subscriptionSetting);
            }
            if (Port == null) {
                SubscriptionSetting subscriptionSetting = createSubscriptionSetting(ServiceSettingType.BROADBAND_SWITCH_PORT, details.port);
                currentSubscription.getSettings().add(subscriptionSetting);
            }
            if (mac == null) {
                SubscriptionSetting subscriptionSetting = createSubscriptionSetting(ServiceSettingType.MAC_ADDRESS, details.mac);
                currentSubscription.getSettings().add(subscriptionSetting);
            }
            if (password == null) {
                SubscriptionSetting subscriptionSetting = createSubscriptionSetting(ServiceSettingType.PASSWORD, details.password);
                currentSubscription.getSettings().add(subscriptionSetting);
            }

            log.debug(String.format("[Synchronize subscription=%s] If conditions passed, starting to update..", subscription.getId()));

            update(currentSubscription);
            log.info("Subscription "+ currentSubscription.getId() + " synchronize successfully");
        } catch (Exception e) {
            log.error(String.format("Error occurs, subscription %s didn't synchronize: %s", subscription.getId(), e.getMessage()), e);
        }
    }

    private SubscriptionSetting createSubscriptionSetting(ServiceSettingType serviceSettingType, String value){ //now synchronization only works for Dataplus
        SubscriptionSetting subscriptionSetting = new SubscriptionSetting();
        subscriptionSetting.setValue(value);
        ServiceSetting serviceSetting = commonOperationsEngine.createServiceSetting(serviceSettingType.name(), serviceSettingType, Providers.DATAPLUS, ServiceType.BROADBAND, "");
        subscriptionSetting.setProperties(serviceSetting);
        settingFacade.save(subscriptionSetting);
        subscriptionSetting = settingFacade.update(subscriptionSetting);
        return subscriptionSetting;
    }

    public List<Subscription> findUnsynchronizedDataplus() {
        List<String> serviceTypesIds = new ArrayList<>();
        List<SubscriptionServiceType> serviceTypes = subscriptionServiceTypePersistenceFacade.findAll();

        for (final SubscriptionServiceType serviceType : serviceTypes) {
            if (serviceType.getProfile().getProfileType().equals(ProfileType.PPPoE) &&
                    (serviceType.getProvider() == null || serviceType.getProvider().getId() == Providers.DATAPLUS.getId())) {
                serviceTypesIds.add(String.valueOf(serviceType.getId()));
            }
        }

        return getEntityManager().createQuery("select s from Subscription s where (select count(sett) from s.settings sett where sett.properties.type in :settingTypes) < 4" +
                " and (select count(sett) from s.settings sett where sett.properties.type = :serviceTypeSetting and sett.value in :serviceTypeIds) = 1 and s.service.provider.id = :providerId", Subscription.class)
                .setParameter("settingTypes", Arrays.asList(ServiceSettingType.BROADBAND_SWITCH, ServiceSettingType.BROADBAND_SWITCH_PORT, ServiceSettingType.BROADBAND_SWITCH_SLOT, ServiceSettingType.MAC_ADDRESS))
                .setParameter("providerId", Providers.DATAPLUS.getId())
                .setParameter("serviceTypeSetting", ServiceSettingType.SERVICE_TYPE)
                .setParameter("serviceTypeIds", serviceTypesIds)
                .getResultList();
    }


    public List<Subscription> findAllBBTV() {

        log.info("findAllBBTV sql starts");

        try {
            return em.createQuery("select sub from Subscription sub " +
                    " where sub.service.provider.id = :bbtvNaxId or sub.service.provider.id = :bbtvBakuId ", Subscription.class)
                    .setParameter("bbtvNaxId", Providers.BBTV.getId())
                    .setParameter("bbtvBakuId", Providers.BBTV_BAKU.getId())
                    .getResultList();
        }catch (Exception ex){
            ex.printStackTrace();
            log.error("Exception on findAllBBTV ", ex);
        }

        return new ArrayList<>();

    }


    public Long findSubscriptionIdByAgreement(String agreement) {
        try {
            return em.createQuery("select s.id from Subscription s where s.agreement = :agreement ", Long.class)
                    .setParameter("agreement", agreement)
                    .getSingleResult();
        } catch (Exception ex) {
            log.error("Error occurs when finding subscription id by agreement "+agreement, ex);
            return null;
        }
    }

}
