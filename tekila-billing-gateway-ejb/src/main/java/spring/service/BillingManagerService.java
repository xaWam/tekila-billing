package spring.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @author MusaAl
 * @date 6/8/2018 : 5:07 PM
 */
@Service
public class BillingManagerService {

    private final static Logger log = LoggerFactory.getLogger(BillingManagerService.class);

    private final TaskExecutor taskExecutor;

    public BillingManagerService(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

//    @EJB(mappedName = INJECTION_POINT+"BillingSettingsManager")
//    private BillingSettingsManager billSettings;
//
//    @EJB(mappedName = INJECTION_POINT+"SubscriberPersistenceFacade")
//    private SubscriberPersistenceFacade subscriberFacade;
//
//    @EJB(mappedName = INJECTION_POINT+"SubscriptionPersistenceFacade")
//    private SubscriptionPersistenceFacade subscriptionFacade;
//
//    @EJB(mappedName = INJECTION_POINT+"InvoicePersistenceFacade")
//    private InvoicePersistenceFacade invoiceFacade;
//
//    @EJB(mappedName = INJECTION_POINT+"TransactionPersistenceFacade")
//    private TransactionPersistenceFacade transFacade;
//
//    @EJB(mappedName = INJECTION_POINT+"ChargePersistenceFacade")
//    private ChargePersistenceFacade chargeFacade;
//
//    @EJB(mappedName = INJECTION_POINT+"SystemLogger")
//    private SystemLogger systemLogger;
//
//    @EJB(mappedName = INJECTION_POINT+"ErrorLogger")
//    private ErrorLogger errorLogger;
//
//    @EJB(mappedName = INJECTION_POINT+"PersistentQueueManager")
//    private PersistentQueueManager queueManager;
//
//    @EJB(mappedName = INJECTION_POINT+"UserPersistenceFacade")
//    private UserPersistenceFacade userFacade;
//
//    @EJB(mappedName = INJECTION_POINT+"SubscriptionReactivationPersistenceFacade")
//    private SubscriptionReactivationPersistenceFacade reactivationFacade;
//
//    @EJB(mappedName = INJECTION_POINT+"CampaignRegisterPersistenceFacade")
//    private CampaignRegisterPersistenceFacade campaignRegisterFacade;
//
//    @EJB(mappedName = INJECTION_POINT+"SubscriptionVASPersistenceFacade")
//    private SubscriptionVASPersistenceFacade vasFacade;
//
//    @EJB(mappedName = INJECTION_POINT+"JobPersistenceFacade")
//    private JobPersistenceFacade jobPersistenceFacade;
//
//    @EJB(mappedName = INJECTION_POINT+"AccountingTransactionPersistenceFacade")
//    private AccountingTransactionPersistenceFacade accTransFacade;
//
//    @EJB(mappedName = INJECTION_POINT+"ServicePersistenceFacade")
//    private ServicePersistenceFacade servicePersistenceFacade;
//
//    @EJB(mappedName = INJECTION_POINT+"ConvergentQueueFacade")
//    private ConvergentQueueFacade convergentQueueFacade;
//
//    @EJB(mappedName = INJECTION_POINT+"SipChargesFetcher")
//    private SipChargesFetcher sipChargesFetcher;
//
//    @EJB(mappedName = INJECTION_POINT+"EngineFactory")
//    private EngineFactory engineFactory;


    @Async
    public void testJobs(){
        log.info("Jobs started --------------------------------------------->   ");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error("Exception on Spring tekila async task --------------------------------------------->   ");
        }
        log.info("Jobs finished --------------------------------------------->   ");
    }


    public void executeAsynchronously() {
        taskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                //TODO add long running task
            }
        });
    }


}
