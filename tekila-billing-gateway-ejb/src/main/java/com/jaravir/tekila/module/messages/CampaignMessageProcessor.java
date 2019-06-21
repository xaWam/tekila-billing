package com.jaravir.tekila.module.messages;

import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.module.campaign.CampaignRegisterPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
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
public class CampaignMessageProcessor {
    @EJB
    private PersistentMessagePersistenceFacade messagePersistenceFacade;
    @EJB
    private SubscriptionPersistenceFacade subscriptionFacade;
    @EJB
    private CampaignRegisterPersistenceFacade campaignRegisterFacade;
    @EJB
    private EngineFactory engineFactory;
    @EJB
    private SystemLogger systemLogger;

    private ExecutorService campaignExecutor;
    private volatile AtomicBoolean consumeQueues = new AtomicBoolean(true);

    private static final Logger log = Logger.getLogger(CampaignMessageProcessor.class);

    private class CampaignProcessor implements Runnable {
        private final List<PersistentMessage> messages;
        private final int messageIdx;

        public CampaignProcessor(final List<PersistentMessage> messages, final int messageIdx) {
            this.messages = messages;
            this.messageIdx = messageIdx;
        }

        @Override
        public void run() {
            PersistentMessage message = messages.get(messageIdx);
            log.info("CampaignMessageProcessor|CampaignProcessor received message " + message);
            try {
                Long subscriptionID = Long.parseLong(message.getProperty("subscriptionID"));

                if (subscriptionID != null) {
                    Subscription subscription = subscriptionFacade.find(subscriptionID);
                    campaignRegisterFacade.processPrepaidForAfterBilling(subscription);
                }
                message.setStatus(MessageStatus.EXECUTED);
                messagePersistenceFacade.update(message);
            } catch (Exception ex) {
                log.error(String.format("Cannot parse AFTER BILLING messag = %s", message), ex);
            }
        }
    }

    private class CampaignExecutor implements Runnable {
        @Override
        public void run() {
            log.info("CampaignMessageProcessor|CampaignExecutor started");
            ExecutorService executorService = Executors.newFixedThreadPool(5);

            while (consumeQueues.get()) {
                List<PersistentMessage> messages = messagePersistenceFacade.findNewCampaignMessages();

                if (messages != null && !messages.isEmpty()) {
                    log.info("CampaignMessageProcessor|CampaignExecutor iteration");
                    for (PersistentMessage message : messages) {
                        log.info(String.format("CampaignMessageProcessor|CampaignExecutor message id = %d is being scheduled", message.getId()));
                        message.setStatus(MessageStatus.SCHEDULED);
                        messagePersistenceFacade.update(message);
                    }
                    for (int i=0; i<messages.size(); ++i) {
                        executorService.execute(new CampaignProcessor(messages, i));
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
            log.info("CampaignMessageProcessor|CampaignExecutor end");
        }
    }

    @PostConstruct
    public void signalUp() {
        log.info("CampaignMessageProcessor|signalUp started");
        campaignExecutor = Executors.newSingleThreadExecutor();
        consumeQueues.set(true);
        campaignExecutor.execute(new CampaignExecutor());
        log.info("CampaignMessageProcessor|signalUp finished");
    }

    @PreDestroy
    public void signalShutdown() {
        log.info("CampaignMessageProcessor|signalShutdown started");
        consumeQueues.set(false);
        campaignExecutor.shutdown();
        log.info("CampaignMessageProcessor|signalShutdown finished");
    }
}
