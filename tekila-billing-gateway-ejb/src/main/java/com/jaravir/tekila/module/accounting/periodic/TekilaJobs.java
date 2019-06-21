package com.jaravir.tekila.module.accounting.periodic;

import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;

import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Service;
import javax.ejb.*;
import java.util.List;


/**
 * @author MusaAl
 * @date 11/22/2018 : 5:27 PM
 */
@Service
@EnableScheduling
public class TekilaJobs {

    private static final Logger log = LoggerFactory.getLogger(TekilaJobs.class);

//    @EJB(mappedName = INJECTION_POINT+"SubscriptionPersistenceFacade")
//    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;
//
//    @EJB(mappedName = INJECTION_POINT+"EngineFactory")
//    private EngineFactory engineFactory;

    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;

    private EngineFactory engineFactory;

    /**  Tekila JOBS runs on tekila_jobs branch  */ // @Schedule(hour = "06", minute = "02",  info = "cancelPrepaidJOB Works!!!!")        //ignoreline
    public void cancelPrepaidJOB() {
        log.info("JOB cancelPrepaid waked up and task is delegated to AsynchMiddleware");
        cancelPrepaid();
        log.info("JOB cancelPrepaid AsynchMiddleware successfully started to process on other Thread");
    }

//    @Asynchronous
    public void cancelPrepaid() {
        log.info("Starting cancel(finalize) process for prepaid Asynchronously");

        long sqlStart = System.currentTimeMillis();
        List<Subscription> subscriptions = subscriptionPersistenceFacade.findAllCanceledPrepaid();
        log.info("Found : {} prepaid subscriptions for cancellation and elapsed time for sql : {}", subscriptions.size(), (System.currentTimeMillis() - sqlStart) / 1000);

        long processStart = System.currentTimeMillis();
        subscriptions.parallelStream().forEach(sub ->
                processCancelPrepaidOneByOne(sub));
        log.info("cancelPrepaid successfully finished and elapsed time : {} sec", (System.currentTimeMillis() - processStart) / 1000);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void processCancelPrepaidOneByOne(Subscription subscription){
        log.info("cancelPrepaid - process begin on subscription agreement : {}", subscription.getAgreement());
        engineFactory.getBillingEngine(subscription).cancelPrepaid(subscription);
        log.info("cancelPrepaid - process end on subscription agreement : {}", subscription.getAgreement());
    }

    //(hour = "*", minute = "*", second = "*/10", info = "Every 10 seconds timer")
    @Scheduled(cron = "*/10 * * * * *")
    public void qoqo() {
        log.info("JOB cancelPrepaid waked up and task is delegated to AsynchMiddleware");
        ass();
        log.info("JOB cancelPrepaid AsynchMiddleware successfully started to process on other Thread");
    }

    @Scheduled(cron = "*/15 * * * * ?")
    public void qoqo2() {
        log.info("JOB cancelPrepaid waked up and task is delegated to AsynchMiddleware");

        log.info("JOB cancelPrepaid AsynchMiddleware successfully started to process on other Thread");
    }

    @Scheduled(fixedDelay=5000)
    public void fixedDelay() {
        log.info("JOB cancelPrepaid waked up and task is delegated to AsynchMiddleware fixedDelay");
        ass();
        log.info("JOB cancelPrepaid AsynchMiddleware successfully started to process on other Thread fixedDelay");
    }

    @Async
    public void ass(){
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("******************************  ASYNCH   ********************************");
    }

}
