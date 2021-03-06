package com.jaravir.tekila.engines;

import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.base.persistence.manager.BillingSettingsManager;
import com.jaravir.tekila.extern.sip.domain.Extension;
import com.jaravir.tekila.extern.sip.repository.ExtensionRepository;
import com.jaravir.tekila.module.accounting.InvoiceState;
import com.jaravir.tekila.module.accounting.entity.*;
import com.jaravir.tekila.module.accounting.manager.AccountingTransactionPersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.ChargePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.InvoicePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.TransactionPersistenceFacade;
import com.jaravir.tekila.module.accounting.periodic.AsynchMiddleware;
import com.jaravir.tekila.module.campaign.CampaignRegisterPersistenceFacade;
import com.jaravir.tekila.module.campaign.CampaignTarget;
import com.jaravir.tekila.module.event.BillingEvent;
import com.jaravir.tekila.module.periiodic.JobPersistenceFacade;
import com.jaravir.tekila.module.periodic.Job;
import com.jaravir.tekila.module.periodic.JobProperty;
import com.jaravir.tekila.module.periodic.JobPropertyType;
import com.jaravir.tekila.module.queue.PersistentQueueManager;
import com.jaravir.tekila.module.service.ServiceSettingType;
import com.jaravir.tekila.module.service.ValueAddedServiceType;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.service.model.BillingPrinciple;
import com.jaravir.tekila.module.service.persistence.manager.ServicePersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscriber;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionVAS;
import com.jaravir.tekila.module.subscription.persistence.management.*;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.ErrorLogger;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spring.controller.vm.SIPStatusesVM;

import javax.ejb.*;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by khsadigov on 5/16/2017.
 */
@Stateless(name = "CitynetBillingManager", mappedName = "CitynetBillingManager")
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class CitynetBillingManager implements BillingEngine {

    private final static Logger log = LoggerFactory.getLogger(CitynetBillingManager.class);

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

    private final AsynchMiddleware asynchMiddleware = new AsynchMiddleware();

    private transient final DateTimeFormatter frm = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void billPrepaid(Subscription subscription) {
        subscription = subscriptionFacade.update(subscription);
        log.debug("billPrepaid Subscription: " + subscription.toString()+ " status is "+subscription.getStatus());

        if (subscription.getStatus() == SubscriptionStatus.BLOCKED ||
                subscription.getStatus() == SubscriptionStatus.INITIAL) {
            log.info(String.format("subscription id=%d, agreement=%s status is %s. Skipping...", subscription.getId(), subscription.getAgreement(), subscription.getStatus()));
            return;
        }

        boolean switchToSuspend = false;
        boolean switchFromSuspend = false;
        try {
            List<Job> jobs = jobPersistenceFacade.findActualJobs(subscription);
                log.debug("found jobs for "+subscription.getAgreement());
            if (jobs != null && !jobs.isEmpty()) {
                for (final Job job : jobs) {
                    List<JobProperty> propertyList = job.getPropertyList();
                    for (final JobProperty property : propertyList) {
                        if (property.getType().equals(JobPropertyType.FINAL_STATUS) &&
                                (SubscriptionStatus.valueOf(property.getValue()).equals(SubscriptionStatus.PRE_FINAL) ||
                                        SubscriptionStatus.valueOf(property.getValue()).equals(SubscriptionStatus.FINAL))) { //....->final
                            String msg = String.format("status change job = %d, final status = prefinal/final", job.getId());
                            log.info(String.format("status change job(to prefinal/final) has been registered for subscription %d", subscription.getId()));
                            systemLogger.success(SystemEvent.SKIP_BILLING_FINAL, subscription, msg);
                            return;
                        } else if (property.getType().equals(JobPropertyType.SUBSCRIPTION_SERVICE_ID)) {
                            Long serviceId = Long.valueOf(property.getValue());
                            Service service = servicePersistenceFacade.find(serviceId);
                            OperationsEngine operationsEngine = engineFactory.getOperationsEngine(subscription);
                            subscription = subscriptionFacade.update(operationsEngine.changeService(subscription, service, false, true));
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
            }
            log.debug("finsihing job execution "+subscription.getAgreement());
        } catch (Exception ex) {
            log.info(String.format("Billing Job ERROR for subscription %d", subscription.getId()));
        }

        Invoice newInvoice = null;

        Subscriber subscriber = subscription.getSubscriber();
        log.info("invoices fetching started for "+subscription.getAgreement());
        List<Invoice> openInvoices = invoiceFacade.findOpenBySubscriber(subscriber.getId());
        log.info("invoices fetching ended for "+subscription.getAgreement());
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
                if (nextEstimatedBillDate.getDayOfMonth() < subscription.getBilledUpToDate().getDayOfMonth()) {
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

            log.debug("vas list size: {}", subscription.getVasList() == null ? 0 : subscription.getVasList().size());
            if (subscription.getVasList() != null && !subscription.getVasList().isEmpty()) {
                log.info(String.format("Subscription id=%d, agreement=%s vas count = %s",
                        subscription.getId(), subscription.getAgreement(), subscription.getVasList().size()));

                long vasPrice = 0;

                long userID = userFacade.findByUserName("system").getId();

                Double bonusDiscount = campaignRegisterFacade.getBonusDiscount(subscription);
                List<ValueAddedService> applicableVasList = campaignRegisterFacade.getApplicableVasList(subscription);

                for (SubscriptionVAS vas : subscription.getVasList()) {
                    log.info(String.format("Subscription id=%d, agreement=%s vas id=%d, name=%s, status=%s, type=%s, price=%d",
                            subscription.getId(), subscription.getAgreement(), vas.getId(), vas.getVas().getName(), vas.getStatus(),
                            vas.getVas().getCode().getType(), vas.getVas().getPrice()));

                    if (vas.getVas().getCode().getType() == ValueAddedServiceType.PERIODIC_STATIC) {
                        vasPrice = vas.getVas().getPrice();
log.info(" Log level1 286" + subscription.getAgreement());
                        if ((vas.getRemainCount() != null && vas.getRemainCount() == -99L) || vas.getStatus() == SubscriptionStatus.FINAL) {
                            continue;
                        }

                        if (vas.getExpirationDate() != null && vas.getExpirationDate().isBefore(DateTime.now().plusDays(1))) {
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
                        log.info(" Log level2 315" + subscription.getAgreement());
                        if ((switchToSuspend || subscription.getStatus().equals(SubscriptionStatus.SUSPENDED))) {
                            if (switchFromSuspend) {
                                if (!vas.getVas().isSuspension()) {
                                    invoiceFacade.addVasChargeToInvoice(newInvoice, subscription, vasRate, vas.getVas(), userID, String.format("Charge for vas=%s count=%s", vas.getVas().getName(), vas.getCount()));
                                }
                            } else if (vas.getVas().isSuspension()) {
                                invoiceFacade.addVasChargeToInvoice(newInvoice, subscription, vasRate, vas.getVas(), userID, String.format("Charge for vas=%s count=%s", vas.getVas().getName(), vas.getCount()));
                            }
                        } else {
                            invoiceFacade.addVasChargeToInvoice(newInvoice, subscription, vasRate, vas.getVas(), userID, String.format("Charge for vas=%s count=%s", vas.getVas().getName(), vas.getCount()));
                        }
                    }
                }
            }

            closingBalance = subscription.getBalance().getRealBalance();
            log.info(" Log level3 332" + subscription.getAgreement() + "    closingBalance "+closingBalance);
            if (startingBalance > 0 && closingBalance < 0 && subscription.getSubscriber().getBilledByLifeCycle()) {
                newInvoice.reduceDebt(startingBalance);
            } else if ((closingBalance > 0
                    || closingBalance == 0) || !subscription.getSubscriber().getBilledByLifeCycle()) {
                if (subscription.getSubscriber().getBilledByLifeCycle()) {
                    newInvoice.reduceDebt(totalCharge);
                    newInvoice.setState(InvoiceState.CLOSED);
                    newInvoice.setCloseDate(DateTime.now());
                }
                log.info(" Log level4 342" + subscription.getAgreement() );
                if (billSettings.getSettings().getPrepaidlifeCycleLength() == 30
                        && subscription.getExpirationDate() != null) {
                    log.info(" Log level5 345" + subscription.getAgreement() + "  -  "+subscription.getExpirationDate());
                    DateTime nextEstimatedExpiryDate = subscription.getExpirationDate().plusMonths(1);
                    if (nextEstimatedExpiryDate.getDayOfMonth() < subscription.getExpirationDate().getDayOfMonth()) {
                        nextEstimatedExpiryDate = nextEstimatedExpiryDate.plusDays(1);
                    }
                    subscription.setExpirationDate(nextEstimatedExpiryDate);
                    subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDate().plusDays(
                            subscription.getBillingModel().getGracePeriodInDays()));
                    if (subscription.getBillingModel().getPrinciple() == BillingPrinciple.GRACE_MONTH) {
                        subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDate().plusMonths(1));
                    }
                } else if (subscription.getExpirationDate() != null) {
                    //can only be violated by Subscription in INITIAL status
                    subscription.setExpirationDate(subscription.getExpirationDate().plusDays(
                            billSettings.getSettings().getPrepaidlifeCycleLength()
                            )
                    );
                    subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDate().plusDays(
                            subscription.getBillingModel().getGracePeriodInDays()));
                    if (subscription.getBillingModel().getPrinciple() == BillingPrinciple.GRACE_MONTH) {
                        subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDate().plusMonths(1));
                    }
                }
                subscription.synchronizeExpiratioDates();

                Boolean provRes = Boolean.FALSE;

                if (subscription.getStatus().equals(SubscriptionStatus.SUSPENDED)) {
                    log.info(String.format("Subscription id = %d is suspended. Cannot be prolonged in radius", subscription.getId()));
                } else {
                    try {
                        log.info(" Log level6 376" + subscription.getAgreement() );
                        provRes = engineFactory.getProvisioningEngine(subscription).openService(subscription, subscription.getExpirationDateWithGracePeriod());
                    } catch (Exception ex) {
                        log.error(String.format("subscription id=%d, agreement=%s cannot provision", subscription.getId(), subscription.getAgreement()), ex);
                    }
                }
                log.info(" Log level7 382" + subscription.getAgreement() + "  provRes "+provRes);
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
        log.info(" Log level8 426" + subscription.getAgreement() );
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

    }

    @Override
    public void manageLifeCyclePrepaid(Subscription subscription) {
        ProvisioningEngine provisioner;
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
                && (subscription.getBillingModel().getPrinciple() == BillingPrinciple.GRACE
                || subscription.getBillingModel().getPrinciple() == BillingPrinciple.GRACE_MONTH)) {
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

    public void manageLifeCyclePrepaidGrace(Subscription subscription) {

        log.info("Starting lifecycle management process for prepaid with grace");
        ProvisioningEngine provisioner = null;
        subscription = subscriptionFacade.findForceRefresh(subscription.getId());
        if (subscription.getExpirationDateWithGracePeriod().isAfterNow()) {
            log.info(
                    String.format("Subscription accepted new payment during managelifecycle grace : id=%d, agreement=%s, status=%s, realBalance=%s, billedUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s",
                            subscription.getId(), subscription.getAgreement(), subscription.getStatus(),
                            subscription.getBalance().getRealBalanceForView(),
                            subscription.getBilledUpToDate().toString(frm),
                            subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                            subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                    )
            );
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
            log.info(String.format("Manage lifecycle finished for subscription id = %d", subscription.getId()));
        } catch (ProvisionerNotFoundException ex) {
            log.error(String.format("Cannot close service for subscription id = %d: ", subscription.getId()), ex);
        } catch (Exception ex) {
            log.info(String.format("Lifecycle management process for prepaid with grace finished with error, subscription id = %d:", subscription.getId()), ex);
        }
        log.info("Lifecycle management process for prepaid  with grace successfully finished.");
    }

    @Override
    public void manageLifeCyclePostapid(Subscription subscription) {

    }

    @Override
    public void cancelPrepaid(Subscription subscription) {
        try {
            engineFactory.getOperationsEngine(subscription).changeStatus(subscription, SubscriptionStatus.CANCEL);
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
        try {

            subscription = engineFactory.getOperationsEngine(subscription).changeStatus(subscription, SubscriptionStatus.FINAL);
            subscription.setLastStatusChangeDate(DateTime.now());

            log.info(
                    String.format("Subscription successfully finalized: id=%d, agreement=%s, status=%s, realBalance=%s, billedUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s",
                            subscription.getId(), subscription.getAgreement(), subscription.getStatus(),
                            subscription.getBalance().getRealBalanceForView(),
                            subscription.getBilledUpToDate().toString(frm),
                            subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                            subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                    )
            );

            systemLogger.success(SystemEvent.SUBSCRIPTION_STATUS_FINAL, subscription,
                    String.format("status=%s, realBalance=%s, billedUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s",
                            subscription.getStatus(),
                            subscription.getBalance().getRealBalanceForView(),
                            subscription.getBilledUpToDate().toString(frm),
                            subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                            subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                    )
            );

            log.info("Finalization process for prepaid successfully finished");
        } catch (Exception ex) {
            log.info("Finalization process for prepaid finished with error: ", ex);
        }
    }

    private void finalizeSubscription(Subscription sub) throws Exception {
        engineFactory.getOperationsEngine(sub).changeStatus(sub, SubscriptionStatus.FINAL);
        log.debug("Subscription's status is changed to FINAL");
    }

    @Override
    public void applyLateFeeOrFinalize(Subscription subscription) {

        try {
            Calendar nowDate = Calendar.getInstance();
            nowDate.setTime(new DateTime().toDate());
            Calendar expDate = Calendar.getInstance();
            expDate.setTime(subscription.getExpiresAsDate());
            Calendar billedDate = Calendar.getInstance();
            billedDate.setTime(subscription.getBilledUpToDateAsDate());
            long months_passed = nowDate.get(Calendar.MONTH) - expDate.get(Calendar.MONTH);
            if (months_passed < 0) {
                months_passed += 12;
            }
            Calendar monthMaxDate = Calendar.getInstance();
            monthMaxDate.setTime(new DateTime().toDate());
            monthMaxDate.set(Calendar.DAY_OF_MONTH, monthMaxDate.getActualMaximum(Calendar.DAY_OF_MONTH));

            if (subscription.getBillingModel().getPrinciple() == BillingPrinciple.GRACE
                    || subscription.getBillingModel().getPrinciple() == BillingPrinciple.GRACE_MONTH) {
                log.debug("months_passed is " + months_passed + " for " + subscription.getAgreement());

                long daydiff = TimeUnit.DAYS.convert(nowDate.getTimeInMillis() - expDate.getTimeInMillis(), TimeUnit.MILLISECONDS);
                if (months_passed >= 3 && daydiff >= (28 * 3)) {
                    finalizeSubscription(subscription);
                } else {

                    long rate = 100000L;
                    if (subscriptionFacade.applyLateFee(subscription, rate)) {
                        subscription.setBilledUpToDate(subscription.getBilledUpToDate().plusMonths(1));
                        subscription = subscriptionFacade.update(subscription);

                        log.info("Subscription charged for Late Fee: " + subscription);

                        log.info(
                                String.format("Late Fee successfully applied for subscription: id=%d, agreement=%s, status=%s, realBalance=%s, billedUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s",
                                        subscription.getId(), subscription.getAgreement(), subscription.getStatus(),
                                        subscription.getBalance().getRealBalanceForView(),
                                        subscription.getBilledUpToDate().toString(frm),
                                        subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                                        subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                                )
                        );

                        systemLogger.success(SystemEvent.LATE_FEE, subscription,
                                String.format("status=%s, realBalance=%s, billedUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s",
                                        subscription.getStatus(),
                                        subscription.getBalance().getRealBalanceForView(),
                                        subscription.getBilledUpToDate().toString(frm),
                                        subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                                        subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                                )
                        );
                    } else {
                        log.error(
                                String.format("Failed to apply late fee to subscription: id=%d, agreement=%s, status=%s, realBalance=%s, billedUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s",
                                        subscription.getId(), subscription.getAgreement(), subscription.getStatus(),
                                        subscription.getBalance().getRealBalanceForView(),
                                        subscription.getBilledUpToDate().toString(frm),
                                        subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                                        subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                                )
                        );

                        systemLogger.error(SystemEvent.LATE_FEE, subscription,
                                String.format("status=%s, realBalance=%s, billedUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s",
                                        subscription.getStatus(),
                                        subscription.getBalance().getRealBalanceForView(),
                                        subscription.getBilledUpToDate().toString(frm),
                                        subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                                        subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                                )
                        );
                    }

                }
            } else if (subscription.getBillingModel().getPrinciple() == BillingPrinciple.CONTINUOUS) {
                if (months_passed >= 3 &&
                        (monthMaxDate.get(Calendar.DAY_OF_MONTH) == nowDate.get(Calendar.DAY_OF_MONTH)
                                || nowDate.get(Calendar.DAY_OF_MONTH) > billedDate.get(Calendar.DAY_OF_MONTH))) {
                    finalizeSubscription(subscription);
                }
            }
        } catch (Exception ex) {
            log.info("Apply Late Fee for prepaid finished with error: ", ex);
        }

    }

    @Override
    public void sipCharge(Subscription subscription, double amount) {
        Invoice newInvoice = null;

        Subscriber subscriber = subscription.getSubscriber();
        for (Invoice inv : subscriber.getInvoices()) {
            if (inv.getState() == InvoiceState.OPEN) {
                newInvoice = inv;
            }
        }

        if (newInvoice == null) {
            newInvoice = new Invoice();
            newInvoice.setState(InvoiceState.OPEN);
            newInvoice.setSubscriber(subscriber);
            invoiceFacade.save(newInvoice);

            log.info(String.format("New invoice created for subscriber id %d", subscription.getSubscriber().getId()));
            systemLogger.success(
                    SystemEvent.INVOICE_CREATED, subscription,
                    String.format("Created during sip charging process: invoice id=%d", newInvoice.getId())
            );
            try {
                queueManager.sendInvoiceNotification(BillingEvent.INVOICE_CREATED, subscription.getId(), newInvoice);
            } catch (Exception ex) {
                log.error(String.format("Cannot send notification: event=%s, subscription id=%d, invoice %s", BillingEvent.INVOICE_CREATED, subscription.getId(), newInvoice), ex);
            }
        }

        long rate = (long) (amount * 100000.0);
        Charge sipCharge = new Charge();
        sipCharge.setService(subscription.getService());
        sipCharge.setAmount(rate);
        sipCharge.setSubscriber(subscriber);
        sipCharge.setUser_id(20000L);
        sipCharge.setDsc("Sip Charged for overdraft call");
        sipCharge.setDatetime(DateTime.now());
        sipCharge.setSubscription(subscription);
        chargeFacade.save(sipCharge);

        Transaction transDebitServiceFee = new Transaction(
                TransactionType.DEBIT,
                subscription,
                rate,
                "Sip Charged for overdraft call"
        );
        transDebitServiceFee.execute();
        transFacade.save(transDebitServiceFee);


        /**    Open Close Outgoing SIP service for CityNet    **/
        int sipNumber = subscription.getSettingByType(ServiceSettingType.SIP) !=null ? Integer.parseInt(subscription.getSettingByType(ServiceSettingType.SIP).getValue()) : 0;
        if(sipNumber!=0){
            final ExtensionRepository extensionRepository = new ExtensionRepository();

            SIPStatusesVM vm = new SIPStatusesVM();
            vm.setExtensionNo(sipNumber);
            log.debug("Subscription : {}, sipNuber : {}, End balance : {}",subscription.getAgreement(),sipNumber, transDebitServiceFee.getEndBalance());
            if(transDebitServiceFee.getEndBalance()<=0){


                Extension extension = extensionRepository.findOneBySipNumber(vm);
                if(extension!=null){
                    final int sipOldOutgStatus=extension.getOutgStatus();
                    subscriptionFacade.updateSubscriptionSIPOutgoingStatus(subscription, sipOldOutgStatus);
                }
                // subscriptionSetting set SIP_OLD_OUTG_STATUS = sipOldOutgStatus
                vm.setOutgStatus(1);
                asynchMiddleware.sipUpdateSubscriptionOutgoingStatus(vm);   // on SIP DB
            }else{

                int sipOldOutgStatus=3;

                if(subscription.getSettingByType(ServiceSettingType.SIP_OLD_OUTG_STATUS) !=null){
                    sipOldOutgStatus = Integer.parseInt(subscription.getSettingByType(ServiceSettingType.SIP_OLD_OUTG_STATUS).getValue());
                }else{
                    Extension extension = extensionRepository.findOneBySipNumber(vm);
                    if(extension!=null){
                        sipOldOutgStatus=extension.getOutgStatus();
                    }
                }
                vm.setOutgStatus(sipOldOutgStatus);
                asynchMiddleware.sipUpdateSubscriptionOutgoingStatus(vm);  // on SIP DB
            }
        }
        /**    End Open Close Outgoing SIP service for CityNet    **/

        sipCharge.setTransaction(transDebitServiceFee);

        newInvoice.addChargeToList(sipCharge);

        log.info(
                String.format("Charge for sip call: subscription id=%d, agreement=%s, amount=%d",
                        subscription.getId(), subscription.getAgreement(), sipCharge.getAmount())
        );

        systemLogger.success(
                SystemEvent.CHARGE,
                subscription,
                transDebitServiceFee,
                String.format("Charge id=%d for sip, amount=%s",
                        sipCharge.getId(), sipCharge.getAmountForView()
                )
        );
    }

}
