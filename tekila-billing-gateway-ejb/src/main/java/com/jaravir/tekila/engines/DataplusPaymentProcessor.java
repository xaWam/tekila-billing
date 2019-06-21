package com.jaravir.tekila.engines;

import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberLifeCycleType;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;


/**
 * Created by khsadigov on 5/16/2017.
 */

@Stateless(name = "DataplusPaymentProcessor", mappedName = "DataplusPaymentProcessor")
public class DataplusPaymentProcessor implements PaymentProcessorEngine {
    @EJB
    private EngineFactory engineFactory;

    private final static Logger log = LoggerFactory.getLogger(DataplusPaymentProcessor.class);


    DateTimeFormatter frm = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void process(Subscription subscription, long amount) {
        log.debug("==== DataplusPaymentProcessor for subscription " + subscription.getId());

        ProvisioningEngine provisioner = null;
        try {
            provisioner = engineFactory.getProvisioningEngine(subscription);
        } catch (ProvisionerNotFoundException e) {
//            e.printStackTrace();
            log.error("Dataplus Provisioner not found when process payment, subscription id: "+subscription.getId(), e);
        }

        log.info("Subscription[ id: {}, status: {}, real balance: {} ]", subscription.getId(), subscription.getStatus(), subscription.getBalance().getRealBalance());

        if ((subscription.getStatus() == SubscriptionStatus.INITIAL
                || subscription.getStatus() == SubscriptionStatus.BLOCKED)
                && subscription.getBalance().getRealBalance() >= 0) {
            log.debug("Pass if INITIAL,BLOCKED, id=" + subscription.getId());

            if (subscription.getSubscriber().getLifeCycle() == SubscriberLifeCycleType.PREPAID
                    && provisioner.openService(subscription)) {
                log.info(String.format("subscription id=%s, status=%s activating", subscription.getId(), subscription.getStatus()));
                engineFactory.getOperationsEngine(subscription).activatePrepaid(subscription);
            } else {
                log.info("Condition is false for subscription {}, so activatePrepaid() method didn't call.", subscription.getId());
            } // end prepaid

        }

    }
}
