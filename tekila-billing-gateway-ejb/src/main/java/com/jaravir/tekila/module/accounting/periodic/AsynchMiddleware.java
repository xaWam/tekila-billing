package com.jaravir.tekila.module.accounting.periodic;

import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.extern.sip.repository.ExtensionRepository;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import spring.controller.vm.SIPStatusesVM;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;

import static spring.util.Constants.INJECTION_POINT;


/**
 * @author MusaAl
 * @date 8/27/2018 : 2:06 PM
 */
@Service
public class AsynchMiddleware {

    private static final Logger log = LoggerFactory.getLogger(AsynchMiddleware.class);

    private final ExtensionRepository extensionRepository = new ExtensionRepository();

    @EJB(mappedName = INJECTION_POINT + "EngineFactory")
    private EngineFactory engineFactory;

    @EJB(mappedName = INJECTION_POINT+"SubscriptionPersistenceFacade")
    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;

    @Async
    public void sipUpdateSubscriptionOutgoingStatus(SIPStatusesVM vm){
        extensionRepository.udateSipSubscriptionOutgoingStatus(vm);
    }


//    You can't use Spring and JEE @Scheduling mechanism together.
////    @RolesAllowed("system")
//    @Scheduled(cron = "0 52 16 * * * *")
//    public void cancelPrepaidJOB() {
//        log.info("JOB cancelPrepaid waked up and task is delegated to AsynchMiddleware");
//        cancelPrepaid();
//        log.info("JOB cancelPrepaid AsynchMiddleware successfully started to process on other Thread");
//    }
//
//    @Async
//    public void cancelPrepaid() {
//        log.info("Starting cancel(finalize) process for prepaid Asynchronously");
//
//        long sqlStart = System.currentTimeMillis();
//        List<Subscription> subscriptions = subscriptionPersistenceFacade.findAllCanceledPrepaid();
//        log.info("Found : {} prepaid subscriptions for cancellation and elapsed time for sql : {}", subscriptions.size(), (System.currentTimeMillis() - sqlStart) / 1000);
//
//        long processStart = System.currentTimeMillis();
//        subscriptions.parallelStream().forEach(sub ->
//                processCancelPrepaidOneByOne(sub));
//        log.info("cancelPrepaid successfully finished and elapsed time : {} sec", (System.currentTimeMillis() - processStart) / 1000);
//    }
//
//    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
//    public void processCancelPrepaidOneByOne(Subscription subscription){
//        log.info("cancelPrepaid - process begin on subscription agreement : {}", subscription.getAgreement());
//        engineFactory.getBillingEngine(subscription).cancelPrepaid(subscription);
//        log.info("cancelPrepaid - process end on subscription agreement : {}", subscription.getAgreement());
//    }
//
//    @Scheduled(cron = "0/20 * * * * ?")
//    public void qoqo() {
//        log.info("JOB cancelPrepaid waked up and task is delegated to AsynchMiddleware");
////        cancelPrepaid();
//        log.info("JOB cancelPrepaid AsynchMiddleware successfully started to process on other Thread");
//    }

}
