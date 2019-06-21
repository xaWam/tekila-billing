/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jaravir.tekila.module.accounting.periodic;

import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.manager.BillingSettingsManager;
import com.jaravir.tekila.engines.*;
import com.jaravir.tekila.module.accounting.entity.Charge;
import com.jaravir.tekila.module.accounting.entity.Transaction;
import com.jaravir.tekila.module.accounting.listener.BeforeBillingListener;
import com.jaravir.tekila.module.accounting.manager.AccountingTransactionPersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.ChargePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.InvoicePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.TransactionPersistenceFacade;
import com.jaravir.tekila.module.campaign.CampaignRegisterPersistenceFacade;
import com.jaravir.tekila.module.convergent.ConvergentQueue;
import com.jaravir.tekila.module.periiodic.JobPersistenceFacade;
import com.jaravir.tekila.module.queue.PersistentQueueManager;
import com.jaravir.tekila.module.service.ServiceSettingType;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.service.model.BillingPrinciple;
import com.jaravir.tekila.module.service.persistence.manager.ServicePersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionVAS;
import com.jaravir.tekila.module.subscription.persistence.management.*;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.ErrorLogger;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.broadband.devices.manager.Providers;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import spring.Filters;
import spring.dto.SubscriptionSmallDTO;
import spring.mapper.manual.SubscriptionMapper;

import javax.annotation.PostConstruct;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.annotation.security.RunAs;
import javax.ejb.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Manages lifecycles of subscriptions
 *
 * @author khsadigov, kmaharov
 */
@DeclareRoles({"system"})
@RunAs("system")
/**  Tekila JOBS runs on tekila_jobs branch  */ // @Startup
@Singleton
public class BillingManager {

    private final static Logger log = LoggerFactory.getLogger(BillingManager.class);

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
    @EJB
    private SipChargesFetcher sipChargesFetcher;

    private List<BeforeBillingListener> beforeBillingListeners;

//    private SubscriptionMapper subscriptionMapper = new SubscriptionMapper();

    private final SubscriptionMapper subscriptionMapper;

//    private final AsynchMiddleware asynchMiddleware;

    private transient final DateTimeFormatter frm = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

//    public BillingManager(SubscriptionMapper subscriptionMapper) {
//        this.subscriptionMapper = subscriptionMapper;
//    }


    @EJB
    private EngineFactory engineFactory;

    public BillingManager() {
        subscriptionMapper = new SubscriptionMapper();
//        asynchMiddleware = new AsynchMiddleware();
    }


    @PostConstruct
    public void onPostConstrcut() {
        beforeBillingListeners = Collections.<BeforeBillingListener>synchronizedList(new ArrayList<BeforeBillingListener>());
    }

    public void addBeforeBillingListener(BeforeBillingListener listener) {
        beforeBillingListeners.add(listener);
        log.debug("before billing listeners count: " + beforeBillingListeners.size());
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void processBeforeBillingListeners(Subscription subscription) {
        synchronized (beforeBillingListeners) {
// may these is meaningful in unstable branch but on tekila-jobs branch this is useless and big lack for performance
            log.info("before billing: found : {} listeners", beforeBillingListeners.size());

            for (BeforeBillingListener listener : beforeBillingListeners) {
                try {
                    listener.beforeBilling(subscription);
                } catch (Exception ex) {
                    log.error(String.format("Cannot process BEFORE BILLING listener for subscription id=%d, agreement=%s",
                            subscription.getId(), subscription.getAgreement()), ex);
                    errorLogger.create(ex);
                }
            }
        }
    }

    public void init() {
        System.out.println(DateTime.now().getDayOfMonth() + " --- " + DateTime.now().get(DateTimeFieldType.dayOfMonth())
                + " --- " + billSettings.getSettings().getPrepaidlifeCycleLength()
        );
    }

    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */
    // @Schedule(dayOfMonth = "25", hour = "3", minute = "0")  //ignoreline
    @Asynchronous
    public void doPeriodicBillingPostPaid() {
        log.info("Starting billing process for postpaid subscriptions");
        List<Subscription> subscriptions = subscriptionFacade.findAllPostpaidForBilling();
        log.info("Found : {} postpaid subscriptions", subscriptions.size());
//        for (Subscription sub : subscriptions) {
//            try {
//                engineFactory.getBillingEngine(sub).billPostpaid(sub);
//            } catch (Exception ex) {
//                log.error(String.format("Cannot bill postpaid subscription id =%d, agreement=%s", sub.getId(), sub.getAgreement()), ex);
//                errorLogger.create(ex);
//            }
//
//        }

        long start = System.currentTimeMillis();

        subscriptions.stream().forEach(sub -> {
            try {
                engineFactory.getBillingEngine(sub).billPostpaid(sub);
            } catch (Exception ex) {
                log.error(String.format("Cannot bill postpaid subscription id =%d, agreement=%s", sub.getId(), sub.getAgreement()), ex);
                errorLogger.create(ex);
            }

        });

        log.info("doPeriodicBillingPostPaid  elapsed time : {} sec", (System.currentTimeMillis() - start) / 1000);
        log.info("Billing process for postpaid finished successfully");
    }


    //------------------> Azertelecom Postpaid <--------------------------\\

//    @RolesAllowed("system")
//    @Schedule(dayOfMonth = "5", hour = "1", minute = "00")
//    public void doPeriodicBillingAzPostPaid() {
//        log.info("Starting billing process for postpaid subscriptions");
//        List<Subscription> subscriptions = subscriptionFacade.findAzPostpaidForBilling();
//        log.info(String.format("Found %d postpaid subscriptions: %s", subscriptions.size(), subscriptions.toString()));
//        for (Subscription sub : subscriptions) {
//            try {
//                engineFactory.getBillingEngine(sub).billPostpaid(sub);
//            } catch (Exception ex) {
//                log.error(String.format("Cannot bill postpaid subscription id =%d, agreement=%s", sub.getId(), sub.getAgreement()), ex);
//                errorLogger.create(ex);
//            }
//
//        }
//        log.info("Billing process for postpaid finished successfully");
//    }
//
//    @RolesAllowed("system")
//    @Schedule(dayOfMonth = "20", hour = "1", minute = "00")
//    public void manageLifeCycleAzPostpaid() {
//        log.info("Starting lifecycle management for AzpostPaid");
//        List<Subscription> subscriptions = subscriptionFacade.findAllExpiredAzPostpaid();
//        log.info(String.format("Found %d expired postpaid subscriptions: %s", subscriptions.size(), subscriptions.toString()));
//        for (Subscription sub : subscriptions) {
//            engineFactory.getBillingEngine(sub).manageLifeCyclePostapid(sub);
//        }
//    }
//
//
//    @RolesAllowed("system")
//    @Schedule(dayOfMonth = "21", hour = "2", minute = "10")
//    public void applyLateFeeOrFinalizeAzPOST() {
//        log.info("Starting apply Late Fee");
//
//        List<Subscription> subscriptions = subscriptionFacade.findAllLateAzPOSTGrace();
//        log.info(String.format("Found %d Azpost subscriptions for applying late fee: %s", subscriptions.size(), subscriptions.toString()));
//        for (Subscription sub : subscriptions) {
//            engineFactory.getBillingEngine(sub).applyLateFeeOrFinalize(sub);
//        }
//        log.info("Apply Late Fee for prepaid successfully finished");
//    }
//
//

    //^\\
    //------------------> Azertelecom Postpaid <--------------------------\\

    @RolesAllowed("system")
//    @Schedule(hour = "19", minute = "15")
    @Asynchronous
    public void narhomeProductPortfolioChange() {
        String[] sourceServices = {"Layt", "Ultra", "Premium", "Prestij", "Layt + Bonus",
                "Layt with Summer Campaign", "Premium + Bonus",
                "Prestij + Bonus", "Ultra + Bonus", "Ultra with Summer Campaign"};
        String[] targetServices = {"Fiber-12", "Fiber-17", "Fiber-29", "Fiber-39", "Fiber-12+Bonus",
                "Fiber-12 with Summer campaign", "Fiber-29+Bonus",
                "Fiber-39+Bonus", "Fiber-17+Bonus", "Fiber-17 with Summer campaign"};

        log.info("Start narhomeProductPortfolioChange job");
        List<Service> narhomeServices =
                servicePersistenceFacade.findAll(Providers.AZERTELECOM.getId());

        for (int i = 0; i < sourceServices.length; ++i) {
            String srcService = sourceServices[i];
            Service targetService = null;
            for (Service narhomeService : narhomeServices) {
                if (narhomeService.getName().equals(targetServices[i])) {
                    targetService = narhomeService;
                    break;
                }
            }
            log.info("targetService " + i + ": " + targetService);

            Filters filters = new Filters();
            SubscriptionPersistenceFacade.Filter serviceFilter =
                    SubscriptionPersistenceFacade.Filter.SERVICE;
            serviceFilter.setOperation(MatchingOperation.EQUALS);
            filters.addFilter(serviceFilter, srcService);

            List<Subscription> subscriptions =
                    subscriptionFacade.findAllPaginated(0, 10000, filters);

            for (final Subscription subscription : subscriptions) {
                log.info("narhomeProductPortfolioChange. Change service for subscription id = " + subscription.getId());
                changeSubscriptionService(subscription, targetService);
            }
            serviceFilter.setOperation(MatchingOperation.LIKE);
        }
        log.info("Finish narhomeProductPortfolioChange job");
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void changeSubscriptionService(Subscription subscription, Service targetService) {
        engineFactory.getOperationsEngine(subscription).
                changeService(subscription,
                        targetService,
                        false,
                        false);
    }


    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */ // @Schedule(hour = "11", minute = "36") //ignoreline
    @Asynchronous
    public void fixDataplusConcurrencyIssue() {
        log.info("fixDataplusConcurrencyIssue() start");

        List<Subscription> subscriptions = subscriptionFacade.findAllDataplusToFix();
        log.info("fixDataplusConcurrencyIssue(). Found : {} prepaid subscriptions to fix", subscriptions.size());


        long start = System.currentTimeMillis();
        subscriptions.stream().forEach(sub -> {
            log.info("fixDataplusConcurrencyIssue(). Start Fix " + sub.getAgreement());
            List<Transaction> transactions = transFacade.getTransactionsBySubscriptionId(sub.getId());

            Optional<Transaction> lastOptional = transactions.stream().max((t1, t2) -> {
                if (t1.getLastUpdateDate().isBefore(t2.getLastUpdateDate())) {
                    return -1;
                } else if (t1.getLastUpdateDate().isEqual(t2.getLastUpdateDate())) {
                    return 0;
                } else {
                    return 1;
                }
            });
            if (lastOptional.isPresent()) {
                Transaction last = lastOptional.get();
                sub.getBalance().setRealBalance(last.getEndBalance());
                sub.setBilledUpToDate(sub.getBilledUpToDate().plusDays(billSettings.getSettings().getMaximumPrepaidlifeCycleLength()));
                if (last.getEndBalance() >= 0) {
                    sub.setStatus(SubscriptionStatus.ACTIVE);
                    sub.setExpirationDate(sub.getBilledUpToDate());
                    sub.setExpirationDateWithGracePeriod(sub.getBilledUpToDate());
                    sub.synchronizeExpiratioDates();
                    try {
                        engineFactory.getProvisioningEngine(sub).openService(sub, sub.getExpirationDateWithGracePeriod());
                    } catch (Exception e) {
                        log.error("fixDataplusConcurrencyIssue()", e);
                    }
                }
            }
            log.info("fixDataplusConcurrencyIssue(). End Fix " + sub.getAgreement());
        });

        log.info("fixDataplusConcurrencyIssue  elapsed time : {} sec", (System.currentTimeMillis() - start) / 1000);
        log.info("fixDataplusConcurrencyIssue() end");
    }


    ///////////////////////////////////for web service invocation/////////////////////////////////////
    @RolesAllowed("elmarmammadov")
    public void activateNonBilledForWebService() {
        log.info("Start billing resurrection process");

        List<Subscription> subscriptions = subscriptionFacade.findAllToResurrect();
        log.info("Found : {} prepaid subscriptions to resurrect", subscriptions.size());
        log.info("shuffling the list");
        Collections.shuffle(subscriptions);
        log.info("end shuffling the list");
        subscriptions = subscriptions.subList(0, subscriptions.size() > 2001 ? 2000 : subscriptions.size());
        for (Subscription sub : subscriptions) {
            if (sub.getStatus().equals(SubscriptionStatus.BLOCKED) | sub.getStatus().equals(SubscriptionStatus.PARTIALLY_BLOCKED) |
                    sub.getStatus().equals(SubscriptionStatus.ACTIVE)) {
                log.info(String.format("resurrect agreement = %s", sub.getAgreement()));
                sub.setStatus(SubscriptionStatus.ACTIVE);
                f(sub);

            }

        }
        log.info("End billing resurrection process");
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////

    //    @Asynchronous
//    @RolesAllowed("system")
//    @Schedule(hour = "11", minute = "31")
    public void activateNonBilled() {
        log.info("Start billing resurrection process");
        List<Subscription> subscriptions = null;
        try {
            subscriptions = subscriptionFacade.findAllToResurrect();
        } catch (Exception ex) {
            log.error("selecting error");
        }

        log.info("Found : {} prepaid subscriptions to resurrect", subscriptions.size());
//        for (Subscription sub : subscriptions) {
//            if (sub.getStatus().equals(SubscriptionStatus.BLOCKED) | sub.getStatus().equals(SubscriptionStatus.PARTIALLY_BLOCKED) |
//                    sub.getStatus().equals(SubscriptionStatus.ACTIVE)) {
//                log.info(String.format("resurrect agreement = %s", sub.getAgreement()));
//                sub.setStatus(SubscriptionStatus.ACTIVE);
//                f(sub);
//
//            }
//
//        }

        long start = System.currentTimeMillis();
        subscriptions.stream().forEach(sub -> {
            if (sub.getStatus().equals(SubscriptionStatus.BLOCKED) | sub.getStatus().equals(SubscriptionStatus.PARTIALLY_BLOCKED) |
                    sub.getStatus().equals(SubscriptionStatus.ACTIVE)) {
                log.info(String.format("resurrect agreement = %s", sub.getAgreement()));
                sub.setStatus(SubscriptionStatus.ACTIVE);
                f(sub);
            }

        });
        log.info("activateNonBilled  elapsed time : {} sec", (System.currentTimeMillis() - start) / 1000);
        log.info("End billing resurrection process");
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void f(Subscription sub) {

        try {
            engineFactory.getBillingEngine(sub).billPrepaid(sub);
        } catch (Exception ex) {
            log.debug("exception 328 " + ex);
        }

    }

    //    @RolesAllowed("system")
//    @Schedule(hour = "11", minute = "20")
    @Asynchronous
    public void dechargeNonBilled() {
        log.info("Start billing decharge process");

        List<Subscription> subscriptions = subscriptionFacade.findAllToDecharge();
        log.info("Found : {} prepaid subscriptions to decharge", subscriptions.size());

//        for (Subscription sub : subscriptions) {
//            if (sub.getStatus().equals(SubscriptionStatus.BLOCKED)) {
//                log.info(String.format("decharge agreement = %s", sub.getAgreement()));
//                sub.setStatus(SubscriptionStatus.ACTIVE);
//                engineFactory.getBillingEngine(sub).billPrepaid(sub);
//            }
//        }

        long start = System.currentTimeMillis();
        subscriptions.stream().forEach(sub -> {
            if (sub.getStatus().equals(SubscriptionStatus.BLOCKED)) {
                log.info("decharge agreement : {}", sub.getAgreement());
                sub.setStatus(SubscriptionStatus.ACTIVE);
                engineFactory.getBillingEngine(sub).billPrepaid(sub);
            }
        });
        log.info("dechargeNonBilled  elapsed time : {} sec", (System.currentTimeMillis() - start) / 1000);
        log.info("End billing decharge process");
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private void billPrepaid(Subscription sub) {
//        sub = subscriptionFacade.findForceRefresh(sub.getId());
        try {
            processBeforeBillingListeners(sub);
        } catch (Exception ex) {
            log.error(String.format("Cannot process BEFORE BILLING listeners for subscription id=%d, agreement=%s",
                    sub.getId(), sub.getAgreement()), ex);
            errorLogger.create(ex);
        }
        try {
            engineFactory.getBillingEngine(sub).billPrepaid(sub);
        } catch (Exception ex) {
            log.error(String.format("Cannot bill prepaid subscription id =%d, agreement=%s", sub.getId(), sub.getAgreement()), ex);
            try {
                errorLogger.create(ex);
            } catch (Exception ex2) {
                log.error("Cannot call errorLogger.create", ex2);
            }
        }
        try {
            queueManager.sendToAfterBillingTopic(sub);
        } catch (Exception ex) {
            log.error(String.format("Cannot send to AFTER BILLING topic, subscription id=%d, agreement=%s",
                    sub.getId(), sub.getAgreement()), ex);
            errorLogger.create(ex);
        }

    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private void billPrepaidDTO(SubscriptionSmallDTO subscriptionSmallDTO) {

        long start = System.currentTimeMillis();

        Subscription sub = subscriptionFacade.findForceRefresh(subscriptionSmallDTO.getId()); // May be Subscription accepted new payment during billing
        try {
            processBeforeBillingListeners(sub);
        } catch (Exception ex) {
            log.error(String.format("Cannot process BEFORE BILLING listeners for subscription id=%d, agreement=%s",
                    sub.getId(), sub.getAgreement()), ex);
            errorLogger.create(ex);
        }
        try {
            engineFactory.getBillingEngine(sub).billPrepaid(sub);
        } catch (Exception ex) {
            log.error(String.format("Cannot bill prepaid subscription id =%d, agreement=%s", sub.getId(), sub.getAgreement()), ex);
            try {
                errorLogger.create(ex);
            } catch (Exception ex2) {
                log.error("Cannot call errorLogger.create", ex2);
            }
        }
        try {
            log.debug("queueManager.sendToAfterBillingTopic(sub) starting for "+sub.getAgreement());
            queueManager.sendToAfterBillingTopic(sub);
            log.debug("queueManager.sendToAfterBillingTopic(sub) finished for "+sub.getAgreement());
        } catch (Exception ex) {
            log.error(String.format("Cannot send to AFTER BILLING topic, subscription id=%d, agreement=%s",
                    sub.getId(), sub.getAgreement()), ex);
            errorLogger.create(ex);
        }

        log.info("billPrepaidDTO for Subscription : {} and elapsed time : {} sec", sub.getAgreement(), (System.currentTimeMillis() - start) / 1000);

    }


    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */ // @Schedule(hour = "03", minute = "20")
    @Asynchronous
    public void doPeriodicBillingDPCNCGB() {
        log.info("Starting billing process for DataPlusCNCGlobal");
        List<SubscriptionSmallDTO> subscriptionSmallDTOList = subscriptionFacade
                .findAllPrepaidForBillingDGC()
                .stream()
                .map(subscriptionMapper::toSmallDTO)
                .collect(Collectors.toList());
        log.info("Found :{} DataPlusCNCGlobal subscriptions:", subscriptionSmallDTOList.size());

        long start = System.currentTimeMillis();
//        subscriptions.parallelStream().forEach(sub-> {
//            log.info("calling to billing for Agreement : {}" + sub.getAgreement());
//            billPrepaid(sub);
//
//        });

        subscriptionSmallDTOList.stream().forEach(sub -> {
            log.info("calling to DataPlusCNCGlobal billing for Agreement : {}" + sub.getAgreement());
            long innerstart = System.currentTimeMillis();
            billPrepaidDTO(sub);
            log.info("innerstart  DataPlusCNCGlobal elapsed time : {} sec  =>" + sub.getAgreement(), (System.currentTimeMillis() - innerstart) / 1000.);
        });

        log.info("doPeriodicBillingDPCNCGB  elapsed time : {} sec", (System.currentTimeMillis() - start) / 1000);
        log.info("Billing process for DataPlusCNCGlobal successfully finished");
    }


    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */ // @Schedule(hour = "04", minute = "20")  //ignoreline
    @Asynchronous
    public void doPeriodicBillingPrepaid() {
        log.info("doPeriodicBillingPrepaid Starting billing process for prepaid");
        List<SubscriptionSmallDTO> subscriptionSmallDTOList = subscriptionFacade
                .findAllPrepaidForBillingNonAzertelekom()
                .stream()
                .map(subscriptionMapper::toSmallDTO)
                .collect(Collectors.toList());
        log.info("doPeriodicBillingPrepaid Found :{} prepaid subscriptions:", subscriptionSmallDTOList.size());

        long start = System.currentTimeMillis();
//        subscriptions.parallelStream().forEach(sub-> {
//            log.info("calling to billing for Agreement : {}" + sub.getAgreement());
//            billPrepaid(sub);
//
//        });
        subscriptionSmallDTOList.stream().forEach(sub -> {
            log.info("calling doPeriodicBillingPrepaid to billing for Agreement : {}" + sub.getAgreement());
            long innerstart = System.currentTimeMillis();
            billPrepaidDTO(sub);
            log.info("innerstart doPeriodicBillingPrepaid elapsed time : {} sec  =>" + sub.getAgreement(), (System.currentTimeMillis() - innerstart) / 1000);
        });

        log.info("doPeriodicBillingPrepaid  elapsed time : {} sec", (System.currentTimeMillis() - start) / 1000);
        log.info("doPeriodicBillingPrepaid Billing process for prepaid successfully finished");
    }

    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */ // @Schedule(hour = "04", minute = "00")  //ignoreline
    @Asynchronous
    public void doPeriodicBillingPrepaidAzertelekom() {
        log.info("Starting billing process for prepaid(Azertelekom)");
//        List<Subscription> subscriptions = subscriptionFacade.findAllPrepaidForBillingAzertelekom();
        List<SubscriptionSmallDTO> subscriptionSmallDTOList = subscriptionFacade
                .findAllPrepaidForBillingAzertelekom()
                .stream()
                .map(subscriptionMapper::toSmallDTO)
                .collect(Collectors.toList());
        log.info("Found : {} Azertelekom prepaid subscriptions", subscriptionSmallDTOList.size());

//        for (Subscription sub : subscriptions) {
//            billPrepaid(sub);
//        }
        long start = System.currentTimeMillis();
        subscriptionSmallDTOList.stream().forEach(sub -> billPrepaidDTO(sub));
        log.info("doPeriodicBillingPrepaidAzertelekom  elapsed time : {} sec", (System.currentTimeMillis() - start) / 1000);
        log.info("Billing process for prepaid successfully finished(Azertelekom)");
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void billPrepaid(long id) {
        Subscription sub = subscriptionFacade.find(id);
        engineFactory.getBillingEngine(sub).billPrepaid(sub);
    }


    //Manage LifeCycle
    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */
    // @Schedule(hour = "0, 1, 2, 5", minute = "20", second = "30")    //ignoreline
    @Asynchronous
    public void manageLifeCyclePostpaid() {
        log.info("Starting lifecycle management for postpaid");
        List<Subscription> subscriptions = subscriptionFacade.findAllExpiredPostpaid();
        log.info("Found : {} expired postpaid subscriptions", subscriptions.size());
//        for (Subscription sub : subscriptions) {
//            engineFactory.getBillingEngine(sub).manageLifeCyclePostapid(sub);
//        }
        long start = System.currentTimeMillis();
        subscriptions.stream().forEach(sub -> engineFactory.getBillingEngine(sub).manageLifeCyclePostapid(sub));
        log.info("manageLifeCyclePostpaid  elapsed time : {} sec", (System.currentTimeMillis() - start) / 1000);
    }

    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */
    // @Schedule(hour = "0, 1, 2, 5, 6, 18", minute = "10", second = "10")
    @Asynchronous
    public void manageLifeCyclePrepaidNonAzertelekom() {
        log.info("Starting lifecycle management process for prepaid, manageLifeCyclePrepaidNonAzertelekom()");
        long start = System.currentTimeMillis();
        List<Subscription> subscriptions = subscriptionFacade.findAllExpiredPrepaidNonAzertelekom();
        log.info("Elapsed time for manageLifeCyclePrepaidNonAzertelekom query : {}", ((System.currentTimeMillis() - start) / 1000));
        log.info("Found : {} expired manageLifeCyclePrepaidNonAzertelekom prepaid subscriptions", subscriptions.size());

//        for (Subscription sub : subscriptions) {
//            log.info("managing lifecycle  manageLifeCyclePrepaidNonAzertelekom for : {}", sub.getAgreement());
//            engineFactory.getBillingEngine(sub).manageLifeCyclePrepaid(sub);
//        }
        long startProcess = System.currentTimeMillis();
        subscriptions.stream().forEach(sub -> {
            log.info("managing lifecycle  manageLifeCyclePrepaidNonAzertelekom for : {}", sub.getAgreement());
            engineFactory.getBillingEngine(sub).manageLifeCyclePrepaid(sub);
        });
        log.info("manageLifeCyclePrepaidNonAzertelekom  elapsed time : {} sec", (System.currentTimeMillis() - startProcess) / 1000);
        log.info("Ended manageLifeCyclePrepaid() job succesfully.");
    }


    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */ //@Schedule(hour = "0, 1, 2, 5, 6", minute = "40", second = "00")
    @Asynchronous
    public void manageLifeCyclePrepaidDPGB() {
        log.info("Starting lifecycle management process for prepaid, manageLifeCyclePrepaidDPGBCNC");
        long start = System.currentTimeMillis();
        List<Subscription> subscriptions = subscriptionFacade.findAllExpiredPrepaidDPGB();
        log.info("Elapsed time for manageLifeCyclePrepaidDPGBCNC query : {}", ((System.currentTimeMillis() - start) / 1000));
        log.info("Found : {} expired manageLifeCyclePrepaidDPGBCNC prepaid subscriptions", subscriptions.size());

//        for (Subscription sub : subscriptions) {
//            log.info("managing lifecycle  manageLifeCyclePrepaidNonAzertelekom for : {}", sub.getAgreement());
//            engineFactory.getBillingEngine(sub).manageLife    CyclePrepaid(sub);
//        }
        long startProcess = System.currentTimeMillis();
        subscriptions.stream().forEach(sub -> {
            log.info("managing lifecycle  manageLifeCyclePrepaidDPGBCNC for : {}", sub.getAgreement());
            engineFactory.getBillingEngine(sub).manageLifeCyclePrepaid(sub);
        });
        log.info("manageLifeCyclePrepaidDPGBCNC  elapsed time : {} sec", (System.currentTimeMillis() - startProcess) / 1000);
        log.info("Ended manageLifeCyclePrepaidDPGBCNC job succesfully.");
    }


    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */ //@Schedule(hour = "0, 1, 2, 5, 6", minute = "40", second = "00")
    @Asynchronous
    public void manageLifeCyclePrepaidCNC() {
        log.info("Starting lifecycle management process for prepaid, manageLifeCyclePrepaidDPGBCNC");
        long start = System.currentTimeMillis();
        List<Subscription> subscriptions = subscriptionFacade.findAllExpiredPrepaidCNC();
        log.info("Elapsed time for manageLifeCyclePrepaidDPGBCNC query : {}", ((System.currentTimeMillis() - start) / 1000));
        log.info("Found : {} expired manageLifeCyclePrepaidDPGBCNC prepaid subscriptions", subscriptions.size());

//        for (Subscription sub : subscriptions) {
//            log.info("managing lifecycle  manageLifeCyclePrepaidNonAzertelekom for : {}", sub.getAgreement());
//            engineFactory.getBillingEngine(sub).manageLife    CyclePrepaid(sub);
//        }
        long startProcess = System.currentTimeMillis();
        subscriptions.stream().forEach(sub -> {
            log.info("managing lifecycle  manageLifeCyclePrepaidDPGBCNC for : {}", sub.getAgreement());
            engineFactory.getBillingEngine(sub).manageLifeCyclePrepaid(sub);
        });
        log.info("manageLifeCyclePrepaidDPGBCNC  elapsed time : {} sec", (System.currentTimeMillis() - startProcess) / 1000);
        log.info("Ended manageLifeCyclePrepaidDPGBCNC job succesfully.");
    }

    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */
    // @Schedule(hour = "0, 1, 2, 5, 6", minute = "15", second = "15")     //ignoreline
    @Asynchronous
    public void manageLifeCyclePrepaidAzertelekom() {
        log.info("Starting lifecycle management process for prepaid, manageLifeCyclePrepaidAzertelekom");
        List<Subscription> subscriptions = subscriptionFacade.findAllExpiredPrepaidAzertelekom();
        log.info("Found : {} expired prepaid subscriptions(Azertelekom):", subscriptions.size());
//        for (Subscription sub : subscriptions) {
//            engineFactory.getBillingEngine(sub).manageLifeCyclePrepaid(sub);
//        }

        long start = System.currentTimeMillis();
        subscriptions.stream().forEach(sub -> engineFactory.getBillingEngine(sub).manageLifeCyclePrepaid(sub));
        log.info("manageLifeCyclePrepaidAzertelekom  elapsed time : {} sec", (System.currentTimeMillis() - start) / 1000);
        log.info("Ended manageLifeCyclePrepaid() job succesfully(Azertelekom).");
    }

    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */
    // @Schedule(hour = "0, 1, 2, 5, 6, 11", minute = "15", second = "15")     //ignoreline
    @Asynchronous
    public void manageLifeCyclePrepaidGraceNonAzertelekom() {
        log.info("Starting lifecycle management process for prepaid with grace, manageLifeCyclePrepaidGraceNonAzertelekom");

        List<Subscription> subscriptions = subscriptionFacade.findAllExpiredWithGracePrepaidNonAzertelekom();
        log.info("Found : {} expired prepaid subscriptions:", subscriptions.size());
//        for (Subscription sub : subscriptions) {
//            try {
//                engineFactory.getBillingEngine(sub).manageLifeCyclePrepaidGrace(sub);
//            } catch (Exception ex) {
//                errorLogger.create(ex);
//            }
//        }
        long start = System.currentTimeMillis();
        subscriptions.stream().forEach(sub -> {
            try {
                engineFactory.getBillingEngine(sub).manageLifeCyclePrepaidGrace(sub);
            } catch (Exception ex) {
                errorLogger.create(ex);
            }
        });
        log.info("manageLifeCyclePrepaidGraceNonAzertelekom  elapsed time : {} sec", (System.currentTimeMillis() - start) / 1000);
        log.info("Ended manageLifeCyclePrepaidGrace() job succesfully.");
    }

    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */
    // @Schedule(hour = "0, 1, 2, 5, 6", minute = "15", second = "15")     //ignoreline
    @Asynchronous
    public void manageLifeCyclePrepaidGraceAzertelekom() {
        log.info("Starting lifecycle management process for prepaid with grace, manageLifeCyclePrepaidGraceAzertelekom");

        List<Subscription> subscriptions = subscriptionFacade.findAllExpiredWithGracePrepaidAzertelekom();
        log.info("Found : {} expired prepaid subscriptions(Azertelekom)", subscriptions.size());
//        for (Subscription sub : subscriptions) {
//            try {
//                engineFactory.getBillingEngine(sub).manageLifeCyclePrepaidGrace(sub);
//            } catch (Exception ex) {
//                errorLogger.create(ex);
//            }
//        }
        long start = System.currentTimeMillis();
        subscriptions.stream().forEach(sub -> {
            try {
                engineFactory.getBillingEngine(sub).manageLifeCyclePrepaidGrace(sub);
            } catch (Exception ex) {
                errorLogger.create(ex);
            }
        });
        log.info("manageLifeCyclePrepaidGraceAzertelekom  elapsed time : {} sec", (System.currentTimeMillis() - start) / 1000);
        log.info("Ended manageLifeCyclePrepaidGrace() job succesfully for Azertelekom.");
    }


    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */ // @Schedule(hour = "4", minute = "00")
    @Asynchronous
    public void manageActivationCityNet() {
        log.info("Starting CityNet Activation process for prepaid");
        List<Subscription> subscriptions = subscriptionFacade.findAllNotActivatedPrepaid(Providers.CITYNET.getId());
        log.info("Found : {} prepaid CityNet subscriptions needed to activate", subscriptions.size());
//        for (Subscription sub : subscriptions) {
//            try {
//                engineFactory.getOperationsEngine(sub).activatePrepaid(sub);
//                log.debug("Activation finished for: " + sub.getAgreement());
//            } catch (Exception ex) {
//                log.info("Activation process for prepaid finished with error: ", ex);
//            }
//        }
        long start = System.currentTimeMillis();
        subscriptions.stream().forEach(sub -> {
            try {
                log.debug("Activation started for: " + sub.getAgreement());
                engineFactory.getOperationsEngine(sub).activatePrepaid(sub);
                log.debug("Activation finished for: " + sub.getAgreement());
            } catch (Exception ex) {
                log.info("Activation process for prepaid finished with error: ", ex);
            }
        });
        log.info("manageActivationCityNet  elapsed time : {} sec", (System.currentTimeMillis() - start) / 1000);
        log.info("CityNet Activation process for prepaid successfully finished");
    }

    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */ // @Schedule(hour = "4", minute = "30")        //ignoreline
    @Asynchronous
    public void manageActivationNarFix() {
        log.info("Starting NarFix Activation process for prepaid");
        List<Subscription> subscriptions = subscriptionFacade.findAllNotActivatedPrepaid(Providers.NARFIX.getId());
        log.info("Found : {} prepaid NarFix subscriptions needed to activate", subscriptions.size());
//        for (Subscription sub : subscriptions) {
//            try {
//                engineFactory.getOperationsEngine(sub).activatePrepaid(sub);
//                log.debug("Activation finished for: " + sub.getAgreement());
//            } catch (Exception ex) {
//                log.info("Activation process for prepaid finished with error: ", ex);
//            }
//        }
        long start = System.currentTimeMillis();
        subscriptions.stream().forEach(sub -> {
            try {
                engineFactory.getOperationsEngine(sub).activatePrepaid(sub);
                log.debug("Activation finished for: " + sub.getAgreement());
            } catch (Exception ex) {
                log.info("Activation process for prepaid finished with error: ", ex);
            }
        });
        log.info("manageActivationNarFix  elapsed time : {} sec", (System.currentTimeMillis() - start) / 1000);
        log.info("NarFix Activation process for prepaid successfully finished");
    }


//    @RolesAllowed("system")
//    @Schedule(hour = "4", minute = "30")
//    public void manageActivationUniNet() {
//        log.info("Starting UniNet Activation process for prepaid");
//        List<Subscription> subscriptions = subscriptionFacade.findAllNotActivatedPrepaid(Providers.UNINET.getId());
//        log.info(String.format("Found %d prepaid UniNet subscriptions needed to activate: %s", subscriptions.size(), subscriptions.toString()));
//        for (Subscription sub : subscriptions) {
//            try {
//                    engineFactory.getOperationsEngine(sub).activatePrepaid(sub);
//                    log.debug("Activation finished for: " + sub.getAgreement());
//            } catch (Exception ex) {
//                log.info("Activation process for prepaid finished with error: ", ex);
//            }
//        }
//        log.info("UniNet Activation process for prepaid successfully finished");
//    }

    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */ // @Schedule(hour = "6", minute = "00")        //ignoreline
    @Asynchronous
    public void finalizePrepaid() {

        List<Subscription> subscriptions = subscriptionFacade.findAllFinalizedPrepaid();
        log.info("Found : {} prepaid subscriptions for finalizing", subscriptions.size());
//        for (Subscription sub : subscriptions) {
//            engineFactory.getBillingEngine(sub).finalizePrepaid(sub);
//        }
        long start = System.currentTimeMillis();
        subscriptions.stream().forEach(sub -> engineFactory.getBillingEngine(sub).finalizePrepaid(sub));
        log.info("finalizePrepaid  elapsed time : {} sec", (System.currentTimeMillis() - start) / 1000);

    }

    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */ // @Schedule(hour = "2", minute = "00")        //ignoreline
    @Asynchronous
    public void applyLateFeeOrFinalize() {
        log.info("Starting apply Late Fee");

        List<Subscription> subscriptions = subscriptionFacade.findAllLatePrepaid();
        log.info("Found :{} prepaid subscriptions for applying late fee", subscriptions.size());
//        for (Subscription sub : subscriptions) {
//            engineFactory.getBillingEngine(sub).applyLateFeeOrFinalize(sub);
//        }
        long start = System.currentTimeMillis();
        subscriptions.stream().forEach(sub -> {
                    log.info("late fee applied for: {}", sub.getAgreement());
                    engineFactory.getBillingEngine(sub).applyLateFeeOrFinalize(sub);

                }
        );
        log.info("applyLateFeeOrFinalize  elapsed time : {} sec", (System.currentTimeMillis() - start) / 1000);
        log.info("Apply Late Fee for prepaid successfully finished");
    }


    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */ // @Schedule(hour = "2", minute = "50")        //ignoreline
    @Asynchronous
    public void manageLifeCycleVAS() {
        log.debug("Starting manageLifeCycle for VAS");

        List<SubscriptionVAS> subscriptionVASList = subscriptionFacade.findAllVASForManageLifeCycle();
        if (subscriptionVASList == null) {
            log.debug("Found 0 expired VAS");
            return;
        }
        log.debug(String.format("Found %d expired VAS: %s", subscriptionVASList.size(), subscriptionVASList.toString()));

        long start = System.currentTimeMillis();
        for (SubscriptionVAS vas : subscriptionVASList) {

            try {
                if (vas.getExpirationDate() != null && vas.getExpirationDate().isBefore(DateTime.now()) && vas.getVasStatus() != -1 && vas.getVas().getProvider().getId() == Providers.CITYNET.getId()) {
                    vas = vasFacade.update(vas);
                    engineFactory.getOperationsEngine(vas.getSubscription()).
                            removeVAS(vas.getSubscription(), vas);
                    vas.setVasStatus(-1);
                    log.debug(String.format("VAS removed during VAS life cycle, VAS: %s subscription: %s", vas, vas.getSubscription()));
                }
            } catch (Exception ex) {
                log.error(String.format("VAS cannot removed during VAS life cycle, VAS: %s subscription: %s", vas, vas.getSubscription()));
            }
        }
        log.info("manageLifeCycleVAS  elapsed time : {} sec", (System.currentTimeMillis() - start) / 1000);
        log.debug("All expired VAS removed successfully");

    }


    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */ // @Schedule(hour = "*", minute = "*/1")       //ignoreline
    @Asynchronous
    public void convergentQueueProcessing() {
        try {
            List<ConvergentQueue> queue = convergentQueueFacade.findAll();
            Subscription subscription = null;
            ProvisioningEngine provisioner = null;
            Service service;

            long start = System.currentTimeMillis();

            for (ConvergentQueue conv : queue) {
                subscription = subscriptionFacade.findConvergent(conv.getMsisdn());
                if (subscription == null) {
                    log.debug("Convergent Subscription for " + conv.getMsisdn() + " not found!");
                    continue;
                }

                if (subscription.getService().getProvider().getId() == 454114)
                    service = servicePersistenceFacade.find(conv.getService2());
                else
                    service = servicePersistenceFacade.find(conv.getService());


                provisioner = engineFactory.getProvisioningEngine(subscription);


                switch (conv.getEvent()) {
                    case "ADD":
                        if (subscription.getService().getId() != conv.getService()) {
                            OperationsEngine operationsEngine = engineFactory.getOperationsEngine(subscription);
                            operationsEngine.changeService(subscription, service, true, true);
                            subscription = subscriptionFacade.update(subscription);
                        }
                        systemLogger.success(SystemEvent.CONVERGENT, subscription, "Service " + service.getId() + " added");
                        break;
                    case "SUCCESS":
                        subscription.setBilledUpToDate(conv.getTd());
                        subscription.setExpirationDate(conv.getTd());
                        subscription.setExpirationDateWithGracePeriod(conv.getTd().plusDays(1));
                        subscription.setStatus(SubscriptionStatus.ACTIVE);
                        provisioner.openService(subscription);

                        Charge convCharge = new Charge();
                        convCharge.setAmount(new Double(conv.getPrice() * 100000).longValue());
                        convCharge.setSubscriber(subscription.getSubscriber());
                        convCharge.setUser_id(20000L);
                        convCharge.setDsc(conv.getOffer());
                        convCharge.setDatetime(DateTime.now());
                        convCharge.setSubscription(subscription);

                        chargeFacade.save(convCharge);

                        systemLogger.success(SystemEvent.CONVERGENT, subscription, "Service prolonged till " + conv.getTd());
                        break;
                    case "FAIL":
                        provisioner.closeService(subscription);
                        subscription.setStatus(SubscriptionStatus.BLOCKED);
                        systemLogger.success(SystemEvent.CONVERGENT, subscription, "Service closed");
                        break;
                }

                conv.setStatus(1);
                convergentQueueFacade.update(conv);
            }

            log.info("convergentQueueProcessing  elapsed time : {} sec", (System.currentTimeMillis() - start) / 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void billingRadiusSync() {
        log.debug("Starting reprovision");
        List<Subscription> subscriptionList = subscriptionFacade.findAllActivePrepaid();
        if (subscriptionList == null) {
            log.debug("Found 0 to reprovision");
            return;
        }
        log.debug("Found : {} subscription to reprovision", subscriptionList.size());
        long start = System.currentTimeMillis();
        subscriptionList.stream().forEach(subscription -> {
            if (subscription.getStatus() == SubscriptionStatus.ACTIVE && subscription.getService().getProvider().getId() != Providers.CITYNET.getId()) {
                try {
                    ProvisioningEngine provisioner = engineFactory.getProvisioningEngine(subscription);
                    boolean res = provisioner.reprovision(subscription);
                    log.debug("reprovision result, agreement " + res + ", " + subscription.getAgreement());
                } catch (Exception ex) {
                    log.debug("Ex billingRadiusSync" + ex);
                }
            }
        });

        log.info("billingRadiusSync  elapsed time : {} sec", (System.currentTimeMillis() - start) / 1000);

    }

    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */ // @Schedule(hour = "08", minute = "00")       //ignoreline
    @Asynchronous
    public void doPeriodicSipCharges() {
        log.info("Starting Sip charging process for CityNet");
        List<SipCharge> sipCharges = sipChargesFetcher.getSipChargesForYesterday();
//        log.info(String.format("Found %d sip charges: %s", sipCharges.size(), sipCharges.toString()));
        log.info("Found :{} sip charges", sipCharges.size());

//        for (final SipCharge sipCharge : sipCharges) {
//            Subscription subscription =
//                    subscriptionFacade.getBySetting(ServiceSettingType.SIP, sipCharge.sipNumber);
//            if (subscription != null) {
//                engineFactory.getBillingEngine(subscription).sipCharge(subscription, sipCharge.chargeAmount);
//            }
//        }
        long start = System.currentTimeMillis();
        sipCharges.stream().forEach(sipCharge -> {
            Subscription subscription =
                    subscriptionFacade.getBySetting(ServiceSettingType.SIP, sipCharge.sipNumber);
            if (subscription != null) {
                engineFactory.getBillingEngine(subscription).
                        sipCharge(subscription, sipCharge.chargeAmount);
            }
        });
        log.info("doPeriodicSipCharges  elapsed time : {} sec", (System.currentTimeMillis() - start) / 1000);
        log.info("Finishing Sip charging process for CityNet");
    }

    /**
     * Tekila JOBS runs on tekila_jobs branch
     */ // @Schedule(hour = "16", minute = "00")       //ignoreline
    @Asynchronous
    public void testJobs() {
        log.info("Jobs started EJB --------------------------------------------->   ");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error("Exception on Spring tekila async task --------------------------------------------->   ");
        }
        log.info("Jobs finished EJB --------------------------------------------->   ");
    }

    @RolesAllowed("system")
//    @Schedule(hour = "19", minute = "25")
    @Asynchronous
    public void provisionBBTVBaku() {
        log.info("provisionBBTVBaku start --------------------------------------------->   ");

        List<Subscription> subscriptions = subscriptionFacade.findAllBBTV();

        log.info("provisionBBTVBaku Found : {} ", subscriptions.size());

        subscriptions.parallelStream().forEach(sub -> {
            try {
                log.info("provisionBBTVBaku provisioning started for : {} ", sub.getAgreement());
                engineFactory.getProvisioningEngine(sub).reprovision(sub);
            } catch (ProvisionerNotFoundException e) {
                e.printStackTrace();
                log.error("provisionBBTVBaku Exception on Subscription : {}", sub.getAgreement());
            }
        });

        log.info("provisionBBTVBaku end --------------------------------------------->   ");
    }
}
