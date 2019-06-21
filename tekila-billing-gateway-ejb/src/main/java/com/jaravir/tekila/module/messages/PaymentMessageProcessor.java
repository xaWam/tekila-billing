package com.jaravir.tekila.module.messages;

import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import org.apache.log4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by kmaharov on 19.09.2017.
 */
@DeclareRoles({"system"})
@RunAs("system")
@Singleton
/**  Tekila JOBS runs on tekila_jobs branch  */ // @Startup
public class PaymentMessageProcessor {
    @EJB
    private PersistentMessagePersistenceFacade messagePersistenceFacade;
    @EJB
    private SubscriptionPersistenceFacade subscriptionFacade;
    @EJB
    private EngineFactory engineFactory;
    @EJB
    private SystemLogger systemLogger;

    private ExecutorService paymentExecutor;
    private volatile AtomicBoolean consumeQueues = new AtomicBoolean(true);

    private static final Logger log = Logger.getLogger(PaymentMessageProcessor.class);

    private class PaymentProcessor implements Runnable {
        private final List<PersistentMessage> messages;
        private final int messageIdx;

        public PaymentProcessor(final List<PersistentMessage> messages, final int messageIdx) {
            this.messages = messages;
            this.messageIdx = messageIdx;
        }

        @Override
        public void run() {
            PersistentMessage message = messages.get(messageIdx);
            Subscription subscription = null;
            Long subscriptionID = null;
            Long amount = null;

            try {
                subscriptionID = Long.parseLong(message.getProperty("subscriptionID"));
                amount = Long.parseLong(message.getProperty("amount"));

                log.debug("PaymentMessageProcessor|PaymentMessage received: " + message);
                subscription = subscriptionFacade.find(subscriptionID);

                engineFactory.getPaymentEngine(subscription).process(subscription, amount);
                message.setStatus(MessageStatus.EXECUTED);
                messagePersistenceFacade.update(message);
            } catch (Exception ex) {
                log.error("Exception while prossessing message: ", ex);
                log.error(
                        String.format("Cannot perform provisioning - subscription id=%d, agreement=%s",
                                subscriptionID,
                                subscription != null ? subscription.getAgreement() : "")
                );
                systemLogger.error(SystemEvent.PROVISIONING, subscription, ex.getCause().getMessage());
            }
        }
    }

    private class PaymentExecutor implements Runnable {
        @Override
        public void run() {
            log.info("PaymentMessageProcessor|PaymentExecutor started");
            ExecutorService executorService = Executors.newFixedThreadPool(5);

            while (consumeQueues.get()) {
                List<PersistentMessage> messages = messagePersistenceFacade.findNewPaymentMessages();

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    log.error(e);
                }

                if (messages != null && !messages.isEmpty()) {
                    log.info("PaymentMessageProcessor|PaymentExecutor iteration");
                    for (PersistentMessage message : messages) {
                        log.info(String.format("PaymentMessageProcessor|PaymentExecutor message id = %d is being scheduled", message.getId()));
                        message.setStatus(MessageStatus.SCHEDULED);
                        messagePersistenceFacade.update(message);
                    }
                    for (int i=0; i<messages.size(); ++i) {
                        executorService.execute(new PaymentProcessor(messages, i));
                    }
                }
            }
            executorService.shutdown();
            while (!executorService.isTerminated()) {
            }
            log.info("PaymentMessageProcessor|PaymentExecutor end");
        }
    }

    private class PaymentReprocessor implements Runnable {
        @Override
        public void run() {
            log.info("PaymentMessageProcessor|PaymentReprocessor started");
            ExecutorService executorService = Executors.newFixedThreadPool(3);

            while (consumeQueues.get()) {
                List<PersistentMessage> messages = messagePersistenceFacade.findFailedPaymentMessages();
                if (messages != null && !messages.isEmpty()) {
                    log.info("PaymentMessageProcessor|PaymentReprocessor iteration");
                    for (PersistentMessage message : messages) {
                        log.info(String.format("PaymentMessageProcessor|PaymentReprocessor message id = %d is being scheduled", message.getId()));
                        message.setStatus(MessageStatus.SCHEDULED);
                        message.setLastUpdateDate();
                        messagePersistenceFacade.update(message);
                    }
                    for (int i=0; i<messages.size(); ++i) {
                        executorService.execute(new PaymentProcessor(messages, i));
                    }
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    log.error(e);
                }
            }
            executorService.shutdown();
            while (!executorService.isTerminated()) {
            }
            log.info("PaymentMessageProcessor|PaymentReprocessor end");
        }
    }

    @PostConstruct
    public void signalUp() {
        log.info("PaymentMessageProcessor|signalUp started");
        paymentExecutor = Executors.newFixedThreadPool(2);
        consumeQueues.set(true);
        paymentExecutor.execute(new PaymentExecutor());
        paymentExecutor.execute(new PaymentReprocessor());
        log.info("PaymentMessageProcessor|signalUp finished");
    }

    @PreDestroy
    public void signalShutdown() {
        log.info("PaymentMessageProcessor|signalShutdown started");
        consumeQueues.set(false);
        paymentExecutor.shutdown();
        log.info("PaymentMessageProcessor|signalShutdown finished");
    }
}
