package com.jaravir.tekila.engines;

import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.base.persistence.manager.BillingSettingsManager;
import com.jaravir.tekila.module.accounting.InvoiceState;
import com.jaravir.tekila.module.accounting.entity.Charge;
import com.jaravir.tekila.module.accounting.entity.Invoice;
import com.jaravir.tekila.module.accounting.entity.Transaction;
import com.jaravir.tekila.module.accounting.entity.TransactionType;
import com.jaravir.tekila.module.accounting.manager.AccountingTransactionPersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.ChargePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.InvoicePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.TransactionPersistenceFacade;
import com.jaravir.tekila.module.accounting.periodic.BillingManager;
import com.jaravir.tekila.module.campaign.CampaignRegisterPersistenceFacade;
import com.jaravir.tekila.module.campaign.CampaignTarget;
import com.jaravir.tekila.module.event.BillingEvent;
import com.jaravir.tekila.module.periiodic.JobPersistenceFacade;
import com.jaravir.tekila.module.periodic.Job;
import com.jaravir.tekila.module.periodic.JobProperty;
import com.jaravir.tekila.module.periodic.JobPropertyType;
import com.jaravir.tekila.module.queue.PersistentQueueManager;
import com.jaravir.tekila.module.service.ResourceBucketType;
import com.jaravir.tekila.module.service.ValueAddedServiceType;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.service.model.BillingPrinciple;
import com.jaravir.tekila.module.service.persistence.manager.ServicePersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.*;
import com.jaravir.tekila.module.subscription.persistence.entity.reactivation.SubscriptionReactivation;
import com.jaravir.tekila.module.subscription.persistence.management.*;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.ErrorLogger;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.ejb.*;
import java.util.List;

/**
 * Created by khsadigov on 5/16/2017.
 */
@Stateless(name = "GeneralBillingManager", mappedName = "GeneralBillingManager")
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class GeneralBillingManager implements BillingEngine {

    private final static Logger log = Logger.getLogger(BillingManager.class);

    @EJB
    private BillingSettingsManager billSettings;
    @EJB
    private SubscriberPersistenceFacade subscriberFacade;
    @EJB
    private SubscriptionPersistenceFacade subscriptionFacade;
    @EJB
    private InvoicePersistenceFacade invoiceFacade;
    @EJB
    private TransactionPersistenceFacade transFacade;
    @EJB
    private ChargePersistenceFacade chargeFacade;
    @EJB
    private EngineFactory engineFactory;
    @EJB
    private SystemLogger systemLogger;
    @EJB
    private ErrorLogger errorLogger;
    @EJB
    private PersistentQueueManager queueManager;
    @EJB
    private UserPersistenceFacade userFacade;
    @EJB
    private SubscriptionReactivationPersistenceFacade reactivationFacade;
    @EJB
    private CampaignRegisterPersistenceFacade campaignRegisterFacade;
    @EJB
    private SubscriptionVASPersistenceFacade vasFacade;
    @EJB
    private JobPersistenceFacade jobPersistenceFacade;
    @EJB
    private AccountingTransactionPersistenceFacade accTransFacade;
    @EJB
    private ServicePersistenceFacade servicePersistenceFacade;
    @EJB
    private ConvergentQueueFacade convergentQueueFacade;

    private transient final DateTimeFormatter frm = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    private boolean isRequiresReactivation(Subscription sub) {
        return sub.getService().getBillingModel() != null
                && sub.getService().getBillingModel().getPrinciple() == BillingPrinciple.REQUIRE_REACTIVATION;
    }

    private boolean tryReactivate(Subscription sub) {
        log.info(String.format("subscription id=%d, agreement=%s requires reactivation", sub.getId(), sub.getAgreement()));

        if (sub.getStatus() == SubscriptionStatus.BLOCKED) {
            log.info(String.format("subscription id=%d, agreement=%s status = %s, skipping...", sub.getId(), sub.getAgreement(), sub.getStatus()));
            return false;
        }

        SubscriptionReactivation reactivation = null;

        if ((reactivation = reactivationFacade.findPendingReactivation(sub)) != null) {
            log.debug(String.format("Found pending reactivation id = %d, status = %s, created date=%s",
                    reactivation.getId(), reactivation.getStatus(), reactivation.getCreatedDate().toString()));
            return false;
        }

        long total = sub.calculateTotalCharge();

        if (total > sub.getBalance().getRealBalance()) {
            log.error(
                    String.format("subscription id=%d, agreement=%s total charge=%d, realBalance=%d, reactivation failed", sub.getId(), sub.getAgreement(), total, sub.getBalance().getRealBalance()));
            systemLogger.error(SystemEvent.SUBSCRIPTION_REACTIVATION, sub,
                    String.format("total charge=%d, realBalance=%d", total, sub.getBalance().getRealBalance())
            );

            reactivationFacade.add(sub);

            systemLogger.success(SystemEvent.SUBSCRIPTION_REACTIVATION_ADDED, sub,
                    String.format("pending reactivation added", total, sub.getBalance().getRealBalance())
            );
            return false;
        } else {
            return true;
        }
    }


    @Override
    public void billPrepaid(Subscription subscription) {
        subscription = subscriptionFacade.update(subscription);
        log.debug("Subscription: " + subscription.toString());

        try {
            if (isRequiresReactivation(subscription) && !tryReactivate(subscription)) {
                return;
            }
        } catch (Exception ex) {
            log.error(String.format("subscription id=%d, agreement=%s cannot reactivate", subscription.getId(), subscription.getAgreement()), ex);
        }


        if (subscription.getStatus() == SubscriptionStatus.BLOCKED ||
                subscription.getStatus() == SubscriptionStatus.INITIAL) {
            log.info(String.format("subscription id=%d, agreement=%s status is %s. Skipping...", subscription.getId(), subscription.getAgreement(), subscription.getStatus()));
            return;
        }

        boolean switchToSuspend = false;
        boolean switchFromSuspend = false;
        try {
            Job job = jobPersistenceFacade.findActualJob(subscription);

            if (job != null) {
                List<JobProperty> propertyList = job.getPropertyList();
                for (final JobProperty property : propertyList) {
                    if (property.getType().equals(JobPropertyType.FINAL_STATUS) &&
                            SubscriptionStatus.valueOf(property.getValue()).equals(SubscriptionStatus.FINAL)) { //....->final
                        String msg = String.format("status change job = %d, final status = %s", job.getId(), SubscriptionStatus.FINAL.toString());
                        log.info(String.format("status change job(to final) has been registered for subscription %d", subscription.getId()));
                        systemLogger.success(SystemEvent.SKIP_BILLING_FINAL, subscription, msg);
                        return;
                    } else if (property.getType().equals(JobPropertyType.SUBSCRIPTION_SERVICE_ID)) {
                        Long serviceId = Long.valueOf(property.getValue());
                        Service service = servicePersistenceFacade.find(serviceId);
                        subscription = subscriptionFacade.update(
                                engineFactory.getOperationsEngine(subscription).changeService(subscription, service, false, true)
                        );
                        break;
                    } else if (property.getType().equals(JobPropertyType.FINAL_STATUS) &&
                            SubscriptionStatus.valueOf(property.getValue()).equals(SubscriptionStatus.SUSPENDED)) { //active->suspended
                        String msg = String.format("status change job = %d, final status = %s", job.getId(), SubscriptionStatus.SUSPENDED.toString());
                        log.info(String.format("Suspension job has been detected for subscription id = %d, executing.....", subscription.getId()));
                        systemLogger.success(SystemEvent.SUSPEND_ON_BILLING, subscription, msg);
                        switchToSuspend = true;
                        break;
                    } else if (property.getType().equals(JobPropertyType.INITIAL_STATUS) &&
                            SubscriptionStatus.valueOf(property.getValue()).equals(SubscriptionStatus.SUSPENDED)) { //suspended -> active
                        String msg = String.format("status change job = %d, initial status = %s", job.getId(), SubscriptionStatus.SUSPENDED.toString());
                        log.info(String.format("Counter-suspension job has been detected for subscription id = %d, executing.....", subscription.getId()));
                        systemLogger.success(SystemEvent.COUNTER_SUSPEND_ON_BILLING, subscription, msg);
                        switchFromSuspend = true;
                    }
                }
            }
        } catch (Exception ex) {
            log.info(String.format("Billing Job ERROR for subscription %d", subscription.getId()));
        }

        Invoice newInvoice = null;

        Subscriber subscriber = subscription.getSubscriber();
        List<Invoice> openInvoices = invoiceFacade.findOpenBySubscriber(subscriber.getId());
        for (Invoice inv : openInvoices) {
            if (new DateTime(inv.getCreationDate()).isAfter((DateTime.now().withTimeAtStartOfDay()))) {
                newInvoice = inv;
            }
        }
        //new subscription - never billed

        if (newInvoice == null) {
            newInvoice = new Invoice();
            newInvoice.setState(InvoiceState.OPEN);
            newInvoice.setSubscriber(subscriber);
            invoiceFacade.save(newInvoice);

            log.info(String.format("New invoice created for subscriber id %d", subscription.getSubscriber().getId()));
            systemLogger.success(
                    SystemEvent.INVOICE_CREATED, subscription,
                    String.format("Created during billing process: invoice id=%d", newInvoice.getId())
            );
            try {
                queueManager.sendInvoiceNotification(BillingEvent.INVOICE_CREATED, subscription.getId(), newInvoice);
            } catch (Exception ex) {
                log.error(String.format("Cannot send notification: event=%s, subscription id=%d, invoice %s", BillingEvent.INVOICE_CREATED, subscription.getId(), newInvoice), ex);
            }
        }

        Long rate = campaignRegisterFacade.getBonusAmount(subscription, CampaignTarget.SERVICE_RATE, false);
        if (rate == null) {
            Double bonusDiscount = campaignRegisterFacade.getBonusDiscount(subscription);
            if (bonusDiscount != null) {
                rate = Double.valueOf(Math.ceil(subscription.getService().getServicePrice() * bonusDiscount)).longValue();
            }
        }

        if (rate == null) {
            rate = subscription.getService().getServicePrice();
        }

        Long startingBalance = subscription.getBalance().getRealBalance();
        Long closingBalance = null;
        Long totalCharge = subscription.calculateTotalCharge();

        if (rate >= 0) {
            if (switchFromSuspend || !(switchToSuspend || subscription.getStatus().equals(SubscriptionStatus.SUSPENDED))) {
                //charge for service
                Charge servFeeCharge = new Charge();
                servFeeCharge.setService(subscription.getService());
                rate = subscription.rerate(rate);
                servFeeCharge.setAmount(rate);
                servFeeCharge.setSubscriber(subscriber);
                servFeeCharge.setUser_id(20000L);
                servFeeCharge.setDsc("Autocharged  for service fee during lifecycle billing");
                servFeeCharge.setDatetime(DateTime.now());
                servFeeCharge.setSubscription(subscription);
                chargeFacade.save(servFeeCharge);

                Transaction transDebitServiceFee = new Transaction(
                        TransactionType.DEBIT,
                        subscription,
                        rate,
                        "Autocharged for service fee during lifecycle billing"
                );
                transDebitServiceFee.execute();
                transFacade.save(transDebitServiceFee);
                servFeeCharge.setTransaction(transDebitServiceFee);

                newInvoice.addChargeToList(servFeeCharge);

                log.info(
                        String.format("Charge for servce fee: subscription id=%d, agreement=%s, amount=%d",
                                subscription.getId(), subscription.getAgreement(), servFeeCharge.getAmount())
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

            subscription.setLastBilledDate(DateTime.now());

            //adjust last billing date to avoid double charging
            if (billSettings.getSettings().getPrepaidlifeCycleLength() == 30) {
                DateTime nextEstimatedBillDate = subscription.getBilledUpToDate().plusMonths(1);
                if(nextEstimatedBillDate.getDayOfMonth() < subscription.getBilledUpToDate().getDayOfMonth()){
                    nextEstimatedBillDate = nextEstimatedBillDate.plusDays(1);
                }
                subscription.setBilledUpToDate(nextEstimatedBillDate);
            } else {
                subscription.setBilledUpToDate(subscription.getBilledUpToDate().plusDays(
                        billSettings.getSettings().getPrepaidlifeCycleLength())
                );
            }

            log.info(
                    String.format("Subscription successfully billed: id=%d, agreement=%s, status=%s, realBalance=%s, billedUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s",
                            subscription.getId(), subscription.getAgreement(), subscription.getStatus(),
                            subscription.getBalance().getRealBalanceForView(),
                            subscription.getBilledUpToDate().toString(frm),
                            subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                            subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                    )
            );

            systemLogger.success(SystemEvent.SUBSCRIPTION_BILLED, subscription,
                    String.format("status=%s, realBalance=%s, billedUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s",
                            subscription.getStatus(),
                            subscription.getBalance().getRealBalanceForView(),
                            subscription.getBilledUpToDate().toString(frm),
                            subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                            subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                    )
            );

            log.debug("vas list size: " + subscription.getVasList() == null ? 0 : subscription.getVasList().size());
            if (subscription.getVasList() != null && !subscription.getVasList().isEmpty()) {
                log.info(String.format("Subscription id=%d, agreement=%s vas count = %s",
                        subscription.getId(), subscription.getAgreement(), subscription.getVasList().size()));

                long vasPrice = 0;

                long userID = userFacade.findByUserName("system").getId();

                Double bonusDiscount = campaignRegisterFacade.getBonusDiscount(subscription);
                List<ValueAddedService> applicableVasList = campaignRegisterFacade.getApplicableVasList(subscription);

                for (SubscriptionVAS vas : subscription.getVasList()) {
                    log.info(String.format("Subscription id=%d, agreement=%s vas id=%d, name=%s, status=%s, type=%s, price=%d",
                            subscription.getId(), subscription.getAgreement(), vas.getId(), vas.getVas().getName(), vas.getStatus(), vas.getVas().getCode().getType(),
                            vas.getPrice()));

                    if (vas.getVas().getCode().getType() == ValueAddedServiceType.PERIODIC_STATIC) {
                        vasPrice = vas.getVas().getPrice();

                        if ((vas.getRemainCount() != null && vas.getRemainCount() == -99L) || vas.getStatus() == SubscriptionStatus.FINAL) {
                            continue;
                        }

                        if (vas.getRemainCount() != null && vas.getRemainCount() >= 1) {
                            log.debug("vas: " + vas + " vas count: " + vas.getRemainCount());
                            subscriptionFacade.manageStaticVas(subscription, vas);
                        }

                        long vasRate = (long)(vasPrice * vas.getCount());
                        if (bonusDiscount != null) {
                            boolean applicable = false;
                            if (applicableVasList != null) {
                                for (ValueAddedService campaignVas : applicableVasList) {
                                    if (campaignVas.getId() == vas.getVas().getId()) {
                                        applicable = true;
                                        break;
                                    }
                                }
                            }
                            if (applicable) {
                                vasRate = (long) (1.0 * vasRate * bonusDiscount);
                            }
                        }
                        if ((switchToSuspend || subscription.getStatus().equals(SubscriptionStatus.SUSPENDED)) && !switchFromSuspend) {
                            if (vas.getVas().isSuspension()) {
                                invoiceFacade.addVasChargeToInvoice(newInvoice, subscription, vasRate, vas.getVas(), userID, String.format("Charge for vas=%s count=%s", vas.getVas().getName(), vas.getCount()));
                            }
                        } else {
                            invoiceFacade.addVasChargeToInvoice(newInvoice, subscription, vasRate, vas.getVas(), userID, String.format("Charge for vas=%s count=%s", vas.getVas().getName(), vas.getCount()));
                        }
                    }
                }
            }

            closingBalance = subscription.getBalance().getRealBalance();

            if (startingBalance > 0 && closingBalance < 0 && subscription.getSubscriber().getBilledByLifeCycle()) {
                newInvoice.reduceDebt(startingBalance);
            } else if ((closingBalance > 0
                    || closingBalance == 0) || !subscription.getSubscriber().getBilledByLifeCycle()) {
                if (subscription.getSubscriber().getBilledByLifeCycle()) {
                    newInvoice.reduceDebt(totalCharge);
                    newInvoice.setState(InvoiceState.CLOSED);
                    newInvoice.setCloseDate(DateTime.now());
                }

                if (billSettings.getSettings().getPrepaidlifeCycleLength() == 30
                        && subscription.getExpirationDate() != null) {
                    DateTime nextEstimatedExpiryDate = subscription.getExpirationDate().plusMonths(1);
                    if(nextEstimatedExpiryDate.getDayOfMonth() < subscription.getExpirationDate().getDayOfMonth()){
                        nextEstimatedExpiryDate = nextEstimatedExpiryDate.plusDays(1);
                    }
                    subscription.setExpirationDate(nextEstimatedExpiryDate);
                    subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDate().plusDays(
                            subscription.getBillingModel().getGracePeriodInDays()));
                } else if (subscription.getExpirationDate() != null) {
                    //can only be violated by Subscription in INITIAL status
                    subscription.setExpirationDate(subscription.getExpirationDate().plusDays(
                            billSettings.getSettings().getPrepaidlifeCycleLength()
                            )
                    );
                    subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDate().plusDays(
                            subscription.getBillingModel().getGracePeriodInDays()));
                }
                subscription.synchronizeExpiratioDates();

                Boolean provRes = Boolean.FALSE;

                try {
                    provRes = engineFactory.getProvisioningEngine(subscription).openService(subscription, subscription.getExpirationDate());
                } catch (Exception ex) {
                    log.error(String.format("subscription id=%d, agreement=%s cannot provision", subscription.getId(), subscription.getAgreement()), ex);
                }

                if (subscription.getExpirationDate() != null && provRes) { //if expDate not null => it was prolonged hence log it
                    log.info(
                            String.format("Subscription successfully prolonged: id=%d, agreement=%s, status=%s, realBalance=%s, billedUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s",
                                    subscription.getId(), subscription.getAgreement(), subscription.getStatus(),
                                    subscription.getBalance().getRealBalanceForView(),
                                    subscription.getBilledUpToDate().toString(frm),
                                    subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                                    subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                            )
                    );

                    systemLogger.success(SystemEvent.SUBSCRIPTION_PROLONGED, subscription,
                            String.format("status=%s, realBalance=%s, billedUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s",
                                    subscription.getStatus(),
                                    subscription.getBalance().getRealBalanceForView(),
                                    subscription.getBilledUpToDate().toString(frm),
                                    subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                                    subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                            )
                    );
                } else if (!provRes) {
                    log.error(
                            String.format("Subscription cannot be prolonged: id=%d, agreement=%s, status=%s, realBalance=%s, billedUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s",
                                    subscription.getId(), subscription.getAgreement(), subscription.getStatus(),
                                    subscription.getBalance().getRealBalanceForView(),
                                    subscription.getBilledUpToDate().toString(frm),
                                    subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                                    subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                            )
                    );

                    systemLogger.error(SystemEvent.SUBSCRIPTION_PROLONGED, subscription,
                            String.format("status=%s, realBalance=%s, billedUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s",
                                    subscription.getStatus(),
                                    subscription.getBalance().getRealBalanceForView(),
                                    subscription.getBilledUpToDate().toString(frm),
                                    subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                                    subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                            )
                    );
                }
            }//end if for charging*/
        } // end if for rate

        if (subscription.getBalance().getRealBalance() < 0 && subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            try {
                queueManager.sendInvoiceNotification(BillingEvent.SUBSCRIPTION_SOON_EXPIRES, subscription.getId(), newInvoice);
            } catch (Exception ex) {
                log.error(String.format("Cannot send notification: event=%s, subscription id=%d, invoice %s", BillingEvent.SUBSCRIPTION_SOON_EXPIRES, subscription.getId(), newInvoice), ex);
            }
        }


    }

    @Override
    public void billPostpaid(Subscription subscription) {

        log.debug("Starting Postpaid billing for Subscription: " + subscription.toString());
        Invoice newInvoice = null;

        Subscriber subscriber = subscription.getSubscriber();
        for (Invoice inv : subscriber.getInvoices()) {
            if (inv.getState() == InvoiceState.OPEN && inv.getSubscription() != null
                    && subscription.getId() == inv.getSubscription().getId()
                    && new DateTime(inv.getCreationDate()).isAfter((DateTime.now().withTimeAtStartOfDay()))) {
                newInvoice = inv;
            }
        }
        //new subscription - never billed
        if (newInvoice == null) {
            newInvoice = new Invoice();
            newInvoice.setState(InvoiceState.OPEN);
            newInvoice.setSubscriber(subscriber);
            newInvoice.setSubscription(subscription);
            invoiceFacade.save(newInvoice);

            log.debug(String.format("New invoice created for subscriber id %d, subscription id %d", subscription.getSubscriber().getId(), subscription.getId()));
            systemLogger.success(SystemEvent.INVOICE_CREATED, subscription,
                    String.format("Created during billing process: invoice id=%d", newInvoice.getId())
            );
        }

        long rate = subscription.getServiceFeeRate();
        long startBalance = subscription.getBalance().getRealBalance();
        long totalCharged = 0;

        //subscription is new - never billed
        if (subscription.getBilledUpToDate() == null) {
            //calculate the rate for partial billing
            int numOfDaysUsed = 0;

            int dayInMonth = subscription.getActivationDate().get(DateTimeFieldType.dayOfMonth());

            if (dayInMonth >= subscription.getActivationDate().dayOfMonth().getMaximumValue()) {
                numOfDaysUsed = 1;
            } else {
                numOfDaysUsed = billSettings.getSettings().getPospaidLifeCycleLength() - (dayInMonth - 1);
            }

            if (numOfDaysUsed < billSettings.getSettings().getPospaidLifeCycleLength()) {
                rate = (rate / billSettings.getSettings().getPospaidLifeCycleLength()) * numOfDaysUsed;
            }

            log.debug(String.format("Partial rate for %d days is %d", numOfDaysUsed, rate));

            //charge for installation fee
            if (subscription.getService().getInstallationFee() >= 0) {
                //balance.debitReal(subscription.getService().getInstallationFee() * 100000);
                Charge instFeeCharge = new Charge();
                instFeeCharge.setService(subscription.getService());
                instFeeCharge.setAmount(subscription.getService().getInstallationFee());
                instFeeCharge.setSubscriber(subscription.getSubscriber());
                instFeeCharge.setUser_id(20000L);
                instFeeCharge.setDsc("Autocharged  for installation fee during lifecycle billing");
                instFeeCharge.setDatetime(DateTime.now());
                instFeeCharge.setSubscription(subscription);
                chargeFacade.save(instFeeCharge);

                Transaction transDebitInstFee = new Transaction(
                        TransactionType.DEBIT,
                        subscription,
                        subscription.getService().getInstallationFee(),
                        "Autocharged  for installation fee during lifecycle billing");
                transDebitInstFee.execute();
                transFacade.save(transDebitInstFee);

                newInvoice.addChargeToList(instFeeCharge);

                totalCharged += instFeeCharge.getAmount();

                log.debug(
                        String.format("Charge for installation fee: subscription id=%d, agreement=%s, amount=%d",
                                subscription.getId(), subscription.getAgreement(), instFeeCharge.getAmount())
                );

                systemLogger.success(SystemEvent.CHARGE, subscription, transDebitInstFee,
                        String.format(
                                "Charged id=%d for installation fee=%s",
                                instFeeCharge.getId(), instFeeCharge.getAmountForView())
                );
            }
        }

        if (rate >= 0) {

            int speed = 0;

            for (SubscriptionResource res : subscription.getResources()) {
                log.debug("INTERNET_DOWN:" + res.getBucketByType(ResourceBucketType.INTERNET_DOWN));
                if (res.getBucketByType(ResourceBucketType.INTERNET_DOWN) != null) {
                    speed = Integer.parseInt(res.getBucketByType(ResourceBucketType.INTERNET_DOWN).getCapacity());
                    break;
                }
            }

            //charge for service
            Charge servFeeCharge = new Charge();
            servFeeCharge.setService(subscription.getService());
            servFeeCharge.setAmount(rate * speed);
            servFeeCharge.setSubscriber(subscriber);
            servFeeCharge.setUser_id(20000L);
            servFeeCharge.setDsc("Autocharged  for service fee during lifecycle billing");
            servFeeCharge.setDatetime(DateTime.now());
            servFeeCharge.setSubscription(subscription);
            chargeFacade.save(servFeeCharge);

            Transaction transDebitServiceFee = new Transaction(
                    TransactionType.DEBIT,
                    subscription,
                    rate,
                    "Autocharged for service fee during lifecycle billing"
            );
            transDebitServiceFee.execute();
            transFacade.save(transDebitServiceFee);
            newInvoice.addChargeToList(servFeeCharge);
            totalCharged += servFeeCharge.getAmount();

            log.debug(
                    String.format("Charge for servce fee: subscription id=%d, agreement=%s, amount=%d",
                            subscription.getId(), subscription.getAgreement(), servFeeCharge.getAmount())
            );

            systemLogger.success(
                    SystemEvent.CHARGE,
                    subscription,
                    transDebitServiceFee,
                    String.format("Charge id=%d for service, amount=%s",
                            servFeeCharge.getId(), servFeeCharge.getAmountForView()
                    )
            );

            subscription.setLastBilledDate(DateTime.now());
            //adjust last billing date
            if (billSettings.getSettings().getPospaidLifeCycleLength() == 30) {
                subscription.setBilledUpToDate(DateTime.now().plusMonths(1));
            } else {
                subscription.setBilledUpToDate(DateTime.now().plusDays(
                        billSettings.getSettings().getPospaidLifeCycleLength())
                );
            }

            log.debug(
                    String.format("Subscription successfully billed: id=%d, agreement=%s, status=%s, realBalance=%s, billedUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s",
                            subscription.getId(), subscription.getAgreement(), subscription.getStatus(),
                            subscription.getBalance().getRealBalanceForView(),
                            subscription.getBilledUpToDate().toString(frm),
                            subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                            subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                    )
            );

            systemLogger.success(SystemEvent.SUBSCRIPTION_BILLED, subscription,
                    String.format("status=%s, realBalance=%s, billedUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s",
                            subscription.getStatus(),
                            subscription.getBalance().getRealBalanceForView(),
                            subscription.getBilledUpToDate().toString(frm),
                            subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                            subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                    )
            );
        } // end if for rate

        //charge for VAS
        if (subscription.getVasList() != null && !subscription.getVasList().isEmpty()) {
            log.debug(String.format("Subscription id=%d, agreement=%s vas count = %s",
                    subscription.getId(), subscription.getAgreement(), subscription.getVasList().size()));

            long vasPrice = 0;

            long userID = userFacade.findByUserName("system").getId();
            long vasRate;

            for (SubscriptionVAS vas : subscription.getVasList()) {
                log.debug(String.format("Subscription id=%d, agreement=%s vas id=%d, name=%s, status=%s, type=%s, price=%d, count=%s",
                        subscription.getId(), subscription.getAgreement(), vas.getId(), vas.getVas().getName(), vas.getStatus(), vas.getVas().getCode().getType(),
                        vas.getPrice(), vas.getCount()));

                if (vas.getVas().getCode().getType() == ValueAddedServiceType.PERIODIC_STATIC) {
                    vasPrice = vas.getVas().getPrice();

                    if ((vas.getRemainCount() != null && vas.getRemainCount() == -99L)  || vas.getStatus() == SubscriptionStatus.FINAL) {
                        continue;

                    }

                    if (vas.getRemainCount() != null && vas.getRemainCount() >= 1) {
                        log.debug("vas: " + vas + " vas count: " + vas.getRemainCount());
                        subscriptionFacade.manageStaticVas(subscription, vas);
                    }

                    vasRate = vasPrice;
                    for (int i = 0; i < vas.getCount(); i++) {
                        invoiceFacade.addVasChargeToInvoice(newInvoice, subscription, vasRate, vas.getVas(), userID, String.format("Charge for vas=%s", vas.getVas().getName()));
                    }
                } else if (vas.getVas().getCode().getType() == ValueAddedServiceType.PERIODIC_STATIC_VARIABLE_CHARGE) {
                    vasPrice = vas.getVas().getPrice();

                    if (vas.getStatus() == SubscriptionStatus.FINAL) {
                        continue;
                    }

                    vasRate = vas.getServiceFeeRate();
                    for (int i = 0; i < vas.getCount(); i++) {
                        invoiceFacade.addVasChargeToInvoice(newInvoice, subscription, vasRate, vas.getVas(), userID, String.format("Charge for vas=%s", vas.getVas().getName()));
                    }
                }
            }
        }
        //adjust invoice debt to subscription's balance
        long endBalance = subscription.getBalance().getRealBalance();
        //newInvoice.addDebt(totalCharged);

        if (startBalance >= 0
                && endBalance < 0) {
            newInvoice.reduceDebt(startBalance);
        } else if (endBalance >= 0) {
            //newInvoice.addDebt(totalCharged);
            newInvoice.reduceDebt(totalCharged);
            newInvoice.setState(InvoiceState.CLOSED);
            newInvoice.setCloseDate(DateTime.now());

            //subscriber paid out the debt - extend subscription date
            if (billSettings.getSettings().getPospaidLifeCycleLength() == 30) {
                subscription.setExpirationDate(DateTime.now().plusMonths(1));
            } else {
                subscription.setExpirationDate(DateTime.now().plusDays(
                        billSettings.getSettings().getPospaidLifeCycleLength()
                        )
                );
            }
            subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDate());
            subscription.synchronizeExpiratioDates();

            log.debug(
                    String.format("Subscription successfully prolonged: id=%d, agreement=%s, status=%s, realBalance=%s, billedUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s",
                            subscription.getId(), subscription.getAgreement(), subscription.getStatus(),
                            subscription.getBalance().getRealBalanceForView(),
                            subscription.getBilledUpToDate().toString(frm),
                            subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                            subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                    )
            );

            systemLogger.success(SystemEvent.SUBSCRIPTION_PROLONGED, subscription,
                    String.format("status=%s, realBalance=%s, billedUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s",
                            subscription.getStatus(),
                            subscription.getBalance().getRealBalanceForView(),
                            subscription.getBilledUpToDate().toString(frm),
                            subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                            subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                    )
            );
        } // end if for charging

    }

    @Override
    public void manageLifeCyclePrepaid(Subscription subscription) {


        ProvisioningEngine provisioner = null;
        log.info("Starting lifecycle management process for CityNet prepaid");
        subscription = subscriptionFacade.findForceRefresh(subscription.getId());
        if (subscription.getExpirationDate().isAfterNow()) {
            log.info(
                    String.format("Subscription accepted new payment during managelifecycle : id=%d, agreement=%s, status=%s, realBalance=%s, billedUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s",
                            subscription.getId(), subscription.getAgreement(), subscription.getStatus(),
                            subscription.getBalance().getRealBalanceForView(),
                            subscription.getBilledUpToDate().toString(frm),
                            subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                            subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                    )
            );
            return;
        }
        if (subscription.getExpirationDate().isBefore(subscription.getExpirationDateWithGracePeriod())
                && subscription.getBillingModel() != null
                && subscription.getBillingModel().getPrinciple() == BillingPrinciple.GRACE) {
            subscription.setStatus(SubscriptionStatus.PARTIALLY_BLOCKED);
            subscription.setLastStatusChangeDate(DateTime.now());
            return;
        }

        try {
            provisioner = engineFactory.getProvisioningEngine(subscription);

            if (provisioner.closeService(subscription)) {
                subscription.setStatus(SubscriptionStatus.BLOCKED);
                subscription.setLastStatusChangeDate(DateTime.now());

                log.info(
                        String.format("Subscription successfully blocked: id=%d, agreement=%s, status=%s, realBalance=%s, billedUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s",
                                subscription.getId(), subscription.getAgreement(), subscription.getStatus(),
                                subscription.getBalance().getRealBalanceForView(),
                                subscription.getBilledUpToDate().toString(frm),
                                subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                                subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                        )
                );

                systemLogger.success(SystemEvent.SUBSCRIPTION_STATUS_BLOCK, subscription,
                        String.format("status=%s, realBalance=%s, billedUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s",
                                subscription.getStatus(),
                                subscription.getBalance().getRealBalanceForView(),
                                subscription.getBilledUpToDate().toString(frm),
                                subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                                subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                        )
                );
                try {
                    queueManager.sendStatusNotification(BillingEvent.STATUS_CHANGED, subscription.getId());
                } catch (Exception ex) {
                    log.error(String.format("Cannot send notification: event=%s, subscription id=%d", BillingEvent.STATUS_CHANGED, subscription.getId()), ex);
                }
            }
            log.info("Lifecycle management process for prepaid successfully finished");
        } catch (ProvisionerNotFoundException ex) {
            log.error(String.format("Cannot close service for subscription %s: ", subscription), ex);
        } catch (Exception ex) {
            log.info("Lifecycle management process for prepaid finished with error: ", ex);
        }
    }

    @Override
    public void manageLifeCyclePostapid(Subscription subscription) {

        log.info("Starting lifecycle management for " + subscription);
        ProvisioningEngine provisioner = null;
        try {
            provisioner = engineFactory.getProvisioningEngine(subscription);

            if (provisioner.closeService(subscription)) {
                subscription.setStatus(SubscriptionStatus.BLOCKED);
                subscription.setLastStatusChangeDate(DateTime.now());

                log.info(
                        String.format("Subscription successfully blocked: id=%d, agreement=%s, status=%s, realBalance=%s, billedUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s",
                                subscription.getId(), subscription.getAgreement(), subscription.getStatus(),
                                subscription.getBalance().getRealBalanceForView(),
                                subscription.getBilledUpToDate().toString(frm),
                                subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                                subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                        )
                );

                systemLogger.success(SystemEvent.SUBSCRIPTION_STATUS_BLOCK, subscription,
                        String.format("status=%s, realBalance=%s, billedUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s",
                                subscription.getStatus(),
                                subscription.getBalance().getRealBalanceForView(),
                                subscription.getBilledUpToDate().toString(frm),
                                subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                                subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                        )
                );
            }

            log.info("Lifecycle management process for postpaid successfully finished");
        } catch (ProvisionerNotFoundException ex) {
            log.error(String.format("Cannot close service for subscription %s: "), ex);
        } catch (Exception ex) {
            log.info("Lifecycle management process for postpaid finished with error: ", ex);
        }


    }

    @Override
    public void manageLifeCyclePrepaidGrace(Subscription subscription) {

    }


    @Override
    public void cancelPrepaid(Subscription subscription) {
        try {

            subscription.setStatus(SubscriptionStatus.CANCEL);
            subscription.setLastStatusChangeDate(DateTime.now());

            log.info(
                    String.format("Subscription successfully cancelled: id=%d, agreement=%s, status=%s, realBalance=%s, billedUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s",
                            subscription.getId(), subscription.getAgreement(), subscription.getStatus(),
                            subscription.getBalance().getRealBalanceForView(),
                            subscription.getBilledUpToDate().toString(frm),
                            subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                            subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                    )
            );

            systemLogger.success(SystemEvent.SUBSCRIPTION_STATUS_CANCEL, subscription,
                    String.format("status=%s, realBalance=%s, billedUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s",
                            subscription.getStatus(),
                            subscription.getBalance().getRealBalanceForView(),
                            subscription.getBilledUpToDate().toString(frm),
                            subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                            subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                    )
            );

            log.info("Cancel process for prepaid successfully finished");
        } catch (Exception ex) {
            log.info("Cancel process for prepaid finished with error: ", ex);
        }
    }

    @Override
    public void finalizePrepaid(Subscription subscription) {

    }

    @Override
    public void applyLateFeeOrFinalize(Subscription subscription) {

    }

    @Override
    public void sipCharge(Subscription subscription, double amount) {

    }
}
