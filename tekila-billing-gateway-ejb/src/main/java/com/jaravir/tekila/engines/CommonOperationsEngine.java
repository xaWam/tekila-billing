package com.jaravir.tekila.engines;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.base.persistence.manager.BillingSettingsManager;
import com.jaravir.tekila.base.persistence.manager.Register;
import com.jaravir.tekila.module.accounting.InvoiceState;
import com.jaravir.tekila.module.accounting.entity.*;
import com.jaravir.tekila.module.accounting.manager.AccountingTransactionPersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.InvoicePersistenceFacade;
import com.jaravir.tekila.module.event.BillingEvent;
import com.jaravir.tekila.module.queue.PersistentQueueManager;
import com.jaravir.tekila.module.service.ResourceBucketType;
import com.jaravir.tekila.module.service.ServiceSettingType;
import com.jaravir.tekila.module.service.ServiceType;
import com.jaravir.tekila.module.service.ValueAddedServiceType;
import com.jaravir.tekila.module.service.entity.ServiceSetting;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.service.model.BillingPrinciple;
import com.jaravir.tekila.module.service.persistence.manager.ServiceSettingPersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.VASPersistenceFacade;
import com.jaravir.tekila.module.store.RangePersistenceFacade;
import com.jaravir.tekila.module.store.ip.IpAddressResult;
import com.jaravir.tekila.module.store.ip.persistence.IpAddress;
import com.jaravir.tekila.module.store.ip.persistence.IpAddressRange;
import com.jaravir.tekila.module.store.nas.Nas;
import com.jaravir.tekila.module.subscription.persistence.entity.*;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionSettingPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionVASPersistenceFacade;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.broadband.devices.manager.Providers;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.util.Iterator;
import java.util.List;

/**
 * Created by kmaharov on 18.05.2017.
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class CommonOperationsEngine {
    @EJB
    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;
    @EJB
    private UserPersistenceFacade userFacade;
    @Resource
    private SessionContext ctx;
    @EJB
    private Register register;
    @EJB
    private SystemLogger systemLogger;
    @EJB
    private BillingSettingsManager billSettings;
    @EJB
    private InvoicePersistenceFacade invoiceFacade;
    @EJB
    private PersistentQueueManager queueManager;
    @EJB
    private RangePersistenceFacade rangeFacade;
    @EJB
    private EngineFactory engineFactory;
    @EJB
    private VASPersistenceFacade vasPersistenceFacade;
    @EJB
    private BillingSettingsManager billingSettings;
    @EJB
    private AccountingTransactionPersistenceFacade accTransFacade;
    @EJB
    private ServiceSettingPersistenceFacade serviceSettingFacade;
    @EJB
    private SubscriptionSettingPersistenceFacade settingFacade;
    @EJB
    private SubscriptionVASPersistenceFacade subscriptionVASPersistenceFacade;

    private final static Logger log = Logger.getLogger(CommonOperationsEngine.class);

    private double getVasCount(double vasCount) {
        if (vasCount == 0) {
            return 1;
        }
        return vasCount;
    }

    private Subscription addVASWithSetting(
            Subscription subscription,
            ValueAddedService vas,
            String sipNumber,
            User user,
            DateTime startDate,
            DateTime expireDate,
            double vasCount) {
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
            subscriptionSetting.setValue(sipNumber);//ip address
            ServiceSetting serviceSetting = createServiceSetting("SIP", ServiceSettingType.SIP, Providers.CITYNET, ServiceType.BROADBAND, "");
            subscriptionSetting.setProperties(serviceSetting);
            settingFacade.save(subscriptionSetting);
            subscriptionSetting = settingFacade.update(subscriptionSetting);
            subscription.getSettings().add(subscriptionSetting);


            String message = String.format("SIP number=%s reserved for subscription id=%d, vas id=%d", sipNumber, subscription.getId(), vas.getId());
            log.debug("addVAS: " + message);
            systemLogger.success(SystemEvent.SIP_NUMBER_ADDED, subscription, message);

            subscription.addVAS(sbnVAS);

            /*if (vas.isProvisioned()) {
                ProvisioningEngine provisioner = engineFactory.getProvisioningEngine(subscription);

                message = String.format("Calling provisioner for static ip VAS subscription id=%d, vas id=%d, status=ACTIVE",
                        subscription.getId(), vas.getId());

                if (provisioner.reprovision(subscription)) {
                    systemLogger.success(SystemEvent.VAS_STATUS_ACTIVE, subscription, message);
                } else {
                    systemLogger.error(SystemEvent.VAS_STATUS_ACTIVE, subscription, message);
                    //ctx.setRollbackOnly();
                }
            }*/

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

    public Subscription addVAS(VASCreationParams params) {
        Subscription subscription = params.subscription;
        subscription = subscriptionPersistenceFacade.update(subscription);
        DateTime startDate = params.startDate;
        ValueAddedService vas = params.vas;
        DateTime expiresDate = params.expiresDate;
        IpAddress ipAddress = params.ipAddress;
        double vasCount = params.count;
        String sipNumber = params.sipNumber;

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

        if (ipAddress != null) {
            try {
                if (vas.getResource() != null && !vas.getResource().getBucketList().isEmpty()) {
                    subscription = addVASWithResources(subscription, vas, ipAddress, user, startDate, expiresDate, getVasCount(vasCount));
                }
            } catch (Exception ex) {
                log.error(String.format("Cannot add vas id=%d to subscription id=%d", vas.getId(), subscription.getId()), ex);
                ctx.setRollbackOnly();
                systemLogger.error(SystemEvent.CHARGE, subscription, "Charge for vas=" + vas.getName() + " failed");
            }
        } else if (sipNumber != null) {
            try {
                subscription = addVASWithSetting(subscription, vas, sipNumber, user, startDate, expiresDate, getVasCount(vasCount));
            } catch (Exception ex) {
                log.error(String.format("Cannot add vas id=%d to subscription id=%d", vas.getId(), subscription.getId()), ex);
                ctx.setRollbackOnly();
                systemLogger.error(SystemEvent.CHARGE, subscription, "Charge for vas=" + vas.getName() + " failed");
            }
        } else {
            log.debug("addVASMain");

            try {
                if (vas.getResource() != null && !vas.getResource().getBucketList().isEmpty()) {
                    log.debug("addVASWithResources");
                    subscription = addVASWithResources(subscription, vas, null, user, startDate, expiresDate, getVasCount(vasCount));
                } else if (vas.getCode().getType() == ValueAddedServiceType.PERIODIC_STATIC && vas.getResource() == null) {
                    log.debug("addVASWithoutResources");
                    subscription = addVASWithoutResources(subscription, vas, user, startDate, expiresDate, getVasCount(vasCount));
                } else {
                    if (!vas.isDoPartialCharge() || !doPartialChargeOfVAS(subscription, vas, user, getVasCount(vasCount))) {

                        long rate = (long)(vas.getPrice() * getVasCount(vasCount));
                        log.debug(String.format("addVAS: vas id=%d, price=%d", vas.getId(),
                                vas.getPrice()));
                        //Charge charge = chargeFacade.chargeForVAS(subscription, rate, "Charge for " + vas.getName(), user.getId(), vas);
                        Invoice invoice = invoiceFacade.findOpenBySubscriberForCharging(subscription.getSubscriber().getId());

                        invoiceFacade.addVasChargeToInvoice(invoice, subscription, rate, vas, user.getId(), String.format("Charge for vas=%s count=%s", vas.getName(), getVasCount(vasCount)));
                    }

                    queueManager.sendVASNotification(BillingEvent.VAS_ADDED, subscription, vas);
                }
            } catch (Exception ex) {
                log.error(String.format("Cannot add vas id=%d to subscription id=%d", vas.getId(), subscription.getId()), ex);
                ctx.setRollbackOnly();
                systemLogger.error(SystemEvent.CHARGE, subscription, "Charge for vas=" + vas.getName() + " failed");
            }
        }
        subscription = restateIptvOwnership(subscription);
        return subscription;
    }

    public ServiceSetting createServiceSetting(String title, ServiceSettingType settingType, Providers provider, ServiceType serviceType, String desc) {
        return serviceSettingFacade.find(provider.getId(), serviceType, settingType);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void initService(Subscription subscription) {
        try {
            ProvisioningEngine provisioner = engineFactory.getProvisioningEngine(subscription);
            log.debug("startinh init service for: " + subscription.toString());
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

    public Subscription restateIptvOwnership(Subscription subscription) {
        boolean iptvOwner = (subscription.getService() != null && subscription.getService().isIptvBundled());
        if (subscription.getVasList() != null && !subscription.getVasList().isEmpty()) {
            for (final SubscriptionVAS sbnVas : subscription.getVasList()) {
                if (sbnVas.getVas() != null && sbnVas.getVas().isIptv()) {
                    iptvOwner = true;
                }
            }
        }
        subscription.setIptvOwner(iptvOwner);
        try {

            if (subscription.getService().getProvider().getId() == Providers.CITYNET.getId() ||
                    subscription.getService().getProvider().getId() == Providers.QUTU.getId()) {
                if (engineFactory.getProvisioningEngine(subscription).provisionIptv(subscription)) {
                    log.debug(String.format("Provisioned iptv for agreement = %s", subscription.getAgreement()));
                } else {
                    log.debug(String.format("Iptv not provisioned for agreement = %s", subscription.getAgreement()));
                }
            }
        } catch (ProvisionerNotFoundException ex) {
            log.error(ex);
        }
        return subscription;
    }

    private Subscription addVASWithoutResources(
            Subscription subscription,
            ValueAddedService vas,
            User user,
            DateTime startDate,
            DateTime expireDate,
            double vasCount) throws ProvisionerNotFoundException {
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

            sbnVAS.setVasStatus(1);
            sbnVAS.setStatus(SubscriptionStatus.ACTIVE);
            sbnVAS.setVas(vas);
            sbnVAS.setRemainCount(vas.getCount() - 1);
            sbnVAS.setCount(getVasCount(vasCount));
            subscription.addVAS(sbnVAS);

            //systemLogger.success(SystemEvent.CHARGE, subscription, String.format("Charge id=%d for vas=%s", charge.getId(), vas.getName()));
            log.debug(vas);

            if (!vas.isDoPartialCharge() || !doPartialChargeOfVAS(subscription, vas, user, getVasCount(vasCount))) {
                long rate = (long) (vas.getPrice() * sbnVAS.getCount());
                log.debug(String.format("addVAS without resources: vas id=%d, price=%d, count=%s", vas.getId(),
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

    private Subscription addVASWithResources(
            Subscription subscription,
            ValueAddedService vas,
            IpAddress ipAddress,
            User user,
            DateTime startDate,
            DateTime expireDate,
            double vasCount) throws ProvisionerNotFoundException {
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

        log.debug(String.format("addVAS partial: vas id=%d, price=%d, days=%d, count=%s",
                vas.getId(), vas.getPrice(), days, getVasCount(vasCount)));
        Invoice invoice = invoiceFacade.findOpenBySubscriberForCharging(subscription.getSubscriber().getId());
        invoiceFacade.addVasChargeToInvoice(invoice, subscription, rate, vas, user.getId(), String.format("Partial Charge for vas=%s count=%s days=%s", vas.getName(), getVasCount(vasCount), days));

        return true;
    }


    public Subscription removeVAS(Subscription subscription, SubscriptionVAS vas) {
        subscription = subscriptionPersistenceFacade.update(subscription);
        vas = subscriptionPersistenceFacade.merge(vas);

        log.debug(String.format("remveVAS: sbnvas id=%d, subscription id=%d", vas.getId(), subscription.getId()));
        SubscriptionResourceBucket bucket = null;
        if (vas.getResource() != null) {
            bucket = vas.getResource().getBucketByType(ResourceBucketType.INTERNET_IP_ADDRESS);
        }

        if (vas.getVas().getCode().getType() == ValueAddedServiceType.PERIODIC_STATIC) {
            try {
                if (bucket != null) {
                    String ipAddress = bucket.getCapacity();
                    log.debug("removeVAS: ip address = " + ipAddress+" for subscription "+subscription.getId());

                    ProvisioningEngine provisioner = engineFactory.getProvisioningEngine(subscription);

                    String message = null;

                    message = String.format("CLOSE VAS returned subscription id=%d, vas id=%d, status=%s",
                            subscription.getId(), vas.getVas().getId(), vas.getStatus());
                    log.info("removeVAS: " + message);

                    if (provisioner.closeVAS(subscription, vas.getVas(), vas)) {
                        systemLogger.success(SystemEvent.VAS_STATUS_BLOCK, subscription, message);
                    } else {
                        systemLogger.error(SystemEvent.VAS_STATUS_BLOCK, subscription, message);
                    }

                    List<IpAddress> ip = subscriptionPersistenceFacade.getIpAddressList(ipAddress);
                    log.debug("removeVAS: ip address list=" + ip);
                    if (ip != null && !ip.isEmpty()) {
                        List<IpAddressRange> range = subscriptionPersistenceFacade.getIpAddressList(ip.get(0).getId());

                        if (range != null && !range.isEmpty()) {
                            range.get(0).freeAddress(ip.get(0));
                        }

                        for (IpAddress addr : ip) {
                            if (range != null && !range.isEmpty() && !range.get(0).getStart().equals(addr) && !range.get(0).getEnd().equals(addr)) {
                                subscriptionPersistenceFacade.remove(addr);
                            }
                            message = String.format("IP address=%s removed from subscription id=%d, vas id=%d", ipAddress, subscription.getId(), vas.getVas().getId());
                            log.debug("edtiVAS: " + message);
                            systemLogger.success(SystemEvent.IP_ADDRESS_UNRESERVED, subscription, message);
                        }
                    }
                    // END IF Ip Address
                } else if (vas.getVas().isSip()) {
                    List<SubscriptionSetting> settingList = subscription.getSettings();
                    for (Iterator<SubscriptionSetting> settingIterator = settingList.iterator();
                         settingIterator.hasNext(); ) {
                        SubscriptionSetting setting = settingIterator.next();
                        if (setting.getProperties().getType() == ServiceSettingType.SIP) {
                            String msg = String.format("removing sip setting from subscription id = %d, setting id = %d", subscription.getId(), setting.getId());
                            log.info(msg);
                            settingFacade.delete(setting);
                            settingIterator.remove();
                            systemLogger.success(SystemEvent.SIP_NUMBER_REMOVED, subscription, msg);
                            break;
                        }
                    }
                    subscription.setSettings(settingList);
                }

                Iterator<SubscriptionVAS> it = subscription.getVasList().iterator();
                SubscriptionVAS sbnVAS = null;

                while (it.hasNext()) {
                    sbnVAS = it.next();

                    if (sbnVAS.getId() == vas.getId()) {
                        log.debug("removeVAS: removing sbn vas id=" + sbnVAS.getId());
                        it.remove();
                        break;
                    }

                    subscriptionPersistenceFacade.remove(vas);
                }
            } catch (Exception ex) {
                log.error(String.format("Cannot remove VAS subscription: subscription=%s, service=%s, vas id=%d", subscription, subscription.getService(), vas.getId()), ex);
                ctx.setRollbackOnly();
            }
        }

        subscription = restateIptvOwnership(subscription);
        return subscription;
    }


    public Subscription removeVAS(Subscription subscription, ValueAddedService vasID) {
        SubscriptionVAS vas = subscription.getVASByServiceId(vasID.getId());

        subscription = subscriptionPersistenceFacade.update(subscription);
        vas = subscriptionPersistenceFacade.merge(vas);

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

                    List<IpAddress> ip = subscriptionPersistenceFacade.getIpAddressList(ipAddress);
                    log.debug("removeVAS: ip address list=" + ip);
                    if (ip != null && !ip.isEmpty()) {
                        List<IpAddressRange> range = subscriptionPersistenceFacade.getIpAddressList(ip.get(0).getId());

                        if (range != null && !range.isEmpty()) {
                            range.get(0).freeAddress(ip.get(0));
                        }
                        log.debug("IPAddress Info -> "+ip.get(0) + " for subscription " + subscription.getId());

                        for (IpAddress addr : ip) {
                            subscriptionPersistenceFacade.remove(addr);
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

                    subscriptionPersistenceFacade.remove(vas);
                }
            } catch (Exception ex) {
                log.error(String.format("Cannot remove VAS subscription: subscription=%s, service=%s, vas id=%d", subscription, subscription.getService(), vas.getId()), ex);
                ctx.setRollbackOnly();
            }
        }

        subscription = restateIptvOwnership(subscription);
        return subscription;
    }

    public Subscription editVAS(VasEditParams params) {
        Subscription subscription = params.subscription;
        long sbnVASId = params.sbnVasId;
        try {
            IpAddress ipAddressEntity = params.ipAddress;
            DateTime vasExpDate = params.expiresDate;
            String sipNumber = params.sipNumber;

            subscription = subscriptionPersistenceFacade.update(subscription);
            Subscription oldSbn = subscription;
            SubscriptionVAS oldVAS = subscription.getVASById(sbnVASId);
            SubscriptionVAS vas = subscription.getVASById(sbnVASId);

            if (vas == null) {
                throw new Exception("Subscription VAS not found, getVASById returned " + vas);
            }

            if (vasExpDate != null) {
                vas.setExpirationDate(vasExpDate);
            }

            if (vas.getVas().getCode().getType() == ValueAddedServiceType.PERIODIC_STATIC) {
                if (vas.getVas().isSip()) {
                    vas.setCount(params.count);
                    subscriptionVASPersistenceFacade.update(vas);

                    String message = String.format("Sip number=%s reserved for subscription id=%d, vas id=%d", sipNumber, subscription.getId(), vas.getVas().getId());
                    log.debug("edtiVAS: " + message);
                    systemLogger.success(SystemEvent.SIP_NUMBER_MODIFIED, subscription, message);
                    subscription.getSettingByType(ServiceSettingType.SIP).setValue(sipNumber);
                } else {
                    String ipAddressAsString = vas.getResource().getBucketByType(ResourceBucketType.INTERNET_IP_ADDRESS).getCapacity();

                    log.debug("BUCKET IP:" + ipAddressAsString);

                    List<IpAddress> ipAddressList = subscriptionPersistenceFacade.getIpAddressList();
                    log.debug("IP LIST SIZE: " + ipAddressList.size());
                    IpAddress ipAddress = null;
                    for (IpAddress address : ipAddressList) {
                        if (address.getAddressAsString().equals(ipAddressAsString)) {
                            log.debug("Found IpAddress: " + address.getAddressAsString());
                            ipAddress = address;
                            break;
                        }
                    }
                    IpAddressRange ipAddressRange = rangeFacade.findReservedIpRange(
                            subscriptionPersistenceFacade.findMinipop(subscription).getNas(), ipAddress);

                    if (ipAddress == null) {
                        throw new Exception("Ip Address  not found");
                    }

                    log.debug("Range: " + ipAddressRange);
                    String message;

                    if (ipAddressEntity == null) {

                        if (ipAddressList != null && !ipAddressList.isEmpty()) {

                            //List<IpAddressRange> range = em.createQuery("select r from IpAddressRange r join r.reservedAddressList a where a.id = :addr", IpAddressRange.class)
                            //      .setParameter("addr", ip.get(0).getId()).getResultList();
                            IpAddress newIp = rangeFacade.findAndReserve(
                                    subscriptionPersistenceFacade.findMinipop(subscription).getNas());
                            if (newIp == null) {
                                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "No IP range or full, create one", "No IP range or full, create one"));
                                return oldSbn;
                            }
                            subscriptionPersistenceFacade.persist(newIp);
                            log.debug("Automatic IP selection: " + newIp.getAddressAsString());

                            message = String.format("IP address=%s reserved from subscription id=%d, vas id=%d", newIp.getAddressAsString(), subscription.getId(), vas.getVas().getId());
                            log.debug("edtiVAS: " + message);
                            systemLogger.success(SystemEvent.IP_ADDRESS_RESERVED, subscription, message);

                            vas.getResource().getBucketByType(ResourceBucketType.INTERNET_IP_ADDRESS).setCapacity(newIp.getAddressAsString());
                            if (ipAddressRange != null && !newIp.getAddressAsString().equals(ipAddress.getAddressAsString())) {
                                ipAddressRange.freeAddress(ipAddress);
                                rangeFacade.update(ipAddressRange);
                            }
                        }
                    } else {
                        log.debug("Manual IP selection: " + ipAddressEntity.getAddressAsString());
                        subscriptionPersistenceFacade.persist(ipAddressEntity);
                        message = String.format("IP address=%s reserved from subscription id=%d, vas id=%d", ipAddressEntity.getAddressAsString(), subscription.getId(), vas.getVas().getId());
                        log.debug("edtiVAS: " + message);
                        systemLogger.success(SystemEvent.IP_ADDRESS_RESERVED, subscription, message);

                        IpAddressRange newRange = rangeFacade.findIpRange(
                                subscriptionPersistenceFacade.findMinipop(subscription).getNas(), ipAddressEntity);
                        log.debug("NEW RANGE: " + newRange);
                        log.debug("ENTITY: " + ipAddressEntity.getAddressAsString());
                        newRange.reserveAddress(ipAddressEntity);
                        rangeFacade.update(newRange);

                        vas.getResource().getBucketByType(ResourceBucketType.INTERNET_IP_ADDRESS).setCapacity(ipAddressEntity.getAddressAsString());

                        if (ipAddressRange != null && !ipAddressEntity.getAddressAsString().equals(ipAddress.getAddressAsString())) {
                            ipAddressRange.freeAddress(ipAddress);
                            rangeFacade.update(ipAddressRange);
                        }

                    }

                    subscriptionPersistenceFacade.remove(ipAddress);
                    message = String.format("IP address=%s removed from subscription id=%d, vas id=%d", ipAddress, subscription.getId(), vas.getVas().getId());
                    log.debug("edtiVAS: " + message);
                    systemLogger.success(SystemEvent.IP_ADDRESS_UNRESERVED, subscription, message);

                    log.debug("Provisioning processing...");

                    ProvisioningEngine provisioner = engineFactory.getProvisioningEngine(oldSbn);
                    message = String.format("CLOSE VAS returned subscription id=%d, vas id=%d, status=%s",
                            oldSbn.getId(), oldVAS.getVas().getId(), oldVAS.getStatus());

                    if (provisioner.closeVAS(oldSbn, oldVAS.getVas())) {
                        systemLogger.success(SystemEvent.VAS_STATUS_BLOCK, oldSbn, message);
                    } else {
                        systemLogger.error(SystemEvent.VAS_STATUS_BLOCK, oldSbn, message);
                    }

                    message = String.format("OPEN VAS returned subscription id=%d, vas id=%d, status=%s",
                            subscription.getId(), vas.getVas().getId(), vas.getStatus());
                    if (provisioner.openVAS(subscription, vas.getVas())) {
                        systemLogger.success(SystemEvent.VAS_STATUS_ACTIVE, subscription, message);
                    } else {
                        systemLogger.error(SystemEvent.VAS_STATUS_ACTIVE, subscription, message);
                    }

                    log.debug("Provisioning finished.");

                }
            }
        } catch (Exception ex) {
            ctx.setRollbackOnly();
            log.error(String.format("editVAS: Cannot modify sbnVAS id=%d, subscription id=%d", sbnVASId, subscription.getId()), ex);
        }

        return subscription;
    }

    public Subscription changeStatus(Subscription subscription, SubscriptionStatus newStatus) {
        log.debug("Changing Status for " + subscription.getAgreement() + " to " + newStatus);
        SubscriptionStatus currentStatus = subscription.getStatus();

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

            log.debug(String.format("Subscription [%s] status become: %s (old status: %s)", subscription.getId(), subscription.getStatus(), currentStatus));

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
                            if (charge.getSubscription().getId() != subscription.getId()) {
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

    private void checkForTransitionFromInitialToActive(Subscription subscription) {
        if (subscription.getStatus() == SubscriptionStatus.INITIAL
                && subscription.getSubscriber().getLifeCycle() == SubscriberLifeCycleType.PREPAID
                && subscription.getService().getServicePrice() == 0
                && subscription.getBalance().getRealBalance() >= 0
                && subscription.getActivationDate() == null && subscription.getExpirationDate() == null) {
            engineFactory.getOperationsEngine(subscription).activatePrepaid(subscription);
        }
    }

    public Subscription prolongPrepaid(Subscription subscription) {
        subscription = subscriptionPersistenceFacade.update(subscription);
        log.debug("ActivatePrepaid, agreement=" + subscription.getAgreement());

        if (subscription.getStatus() == SubscriptionStatus.INITIAL) {
            subscription.setActivationDate(DateTime.now());
        }

        DateTime expDate = (subscription.getStatus() == SubscriptionStatus.ACTIVE) ? subscription.getExpirationDate()
                : DateTime.now();

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
        if (subscription.getBillingModel().getPrinciple() == BillingPrinciple.GRACE_MONTH)
            subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDate().plusMonths(1));

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
    }

    boolean activatePostpaid(Subscription subscription) throws ProvisionerNotFoundException {
        ProvisioningEngine provisioner = engineFactory.getProvisioningEngine(subscription);

        if (provisioner.openService(subscription)) {
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
        } else {
            return false;
        }
    }
}
