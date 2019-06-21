package com.jaravir.tekila.engines;

import com.jaravir.tekila.module.service.model.BillingPrinciple;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberLifeCycleType;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.broadband.devices.manager.Providers;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Created by khsadigov on 5/16/2017.
 */

@Stateless(name = "NarfixPaymentProcessor", mappedName = "NarfixPaymentProcessor")
public class NarfixPaymentProcessor implements PaymentProcessorEngine {

    @EJB
    private EngineFactory engineFactory;

    @EJB
    private SubscriptionPersistenceFacade subscriptionFacade;

    @EJB
    private SystemLogger systemLogger;

    private final static Logger log = Logger.getLogger(NarfixPaymentProcessor.class);


    DateTimeFormatter frm = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void process(Subscription subscription, long amount) {

        log.debug("==== NarfixPaymentProcessor ====");


        ProvisioningEngine provisioner = null;
        try {
            provisioner = engineFactory.getProvisioningEngine(subscription);
        } catch (ProvisionerNotFoundException e) {
            e.printStackTrace();
        }


        if (subscription.getSubscriber().getLifeCycle() == SubscriberLifeCycleType.PREPAID
                && subscription.getStatus() == SubscriptionStatus.ACTIVE
                && !subscription.getBilledUpToDate().equals(subscription.getExpirationDateWithGracePeriod())
                && subscription.getBilledUpToDate().isAfter(DateTime.now())
                && ((DateTime.now().withTimeAtStartOfDay().plusDays(6).isAfter(subscription.getExpirationDateWithGracePeriod())
                && subscription.getExpirationDateWithGracePeriod().isAfter(DateTime.now()))
                || subscription.getExpirationDateWithGracePeriod().isBeforeNow()
                || subscription.getBilledUpToDate().isAfter(subscription.getExpirationDateWithGracePeriod()))
                && subscription.getBalance().getRealBalance() >= 0) {
            subscription = engineFactory.getOperationsEngine(subscription).prolongPrepaid(subscription);
            if (provisioner.openService(subscription, subscription.getExpirationDateWithGracePeriod())) {
                systemLogger.success(
                        SystemEvent.SUBSCRIPTION_PROLONGED,
                        subscription,
                        String.format("subscription id=%d switched from initial to active, prolonged on radius db", subscription.getId()));
            }
        } else if (subscription.getService().getProvider().getId() == Providers.NARFIX.getId()) {

            log.debug(String.format("Processing of onPayment for NARFIX. Subscription id = %d", subscription.getId()));

            if (subscription.getStatus() == SubscriptionStatus.CANCEL) {
                log.debug("Subscription's status is CANCEL. Skipping ... ");
                return;
            }

            if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
                log.debug(String.format("Subscription's status is ACTIVE. Subscription id = %d. Skipping ... ", subscription.getId()));
                return;
            }

            if (subscription.getStatus() == SubscriptionStatus.FINAL) {
                log.debug("Subscription's status is FINAL. Skipping ... ");
                return;
            }

            if (subscription.getStatus() == SubscriptionStatus.SUSPENDED) {
                log.debug("Subscription's status is SUSPENDED. Skipping ... ");
                return;
            }

            if (subscription.getStatus() == SubscriptionStatus.PRE_FINAL) {
                log.debug("Subscription's status is PRE_FINAL. Skipping ... ");
                return;
            }

            if (subscription.getActivationDate() == null) {
                DateTime fd = null;
                try {
                    fd = provisioner.getActivationDate(subscription);
                } catch (Exception ex) {
                    log.debug("Exception while getting FD from RADIUS: " + ex);
                }

                if (subscription.getActivationDate() == null && fd == null) {
                    log.debug("Subscription " + subscription.getAgreement() + " is not Activated on Radius. Skipping...");
                    return;
                }

                if (fd != null && subscription.getActivationDate() == null) {
                    subscription.setActivationDate(fd);
                }
            }

            log.debug(subscription);

            if (subscription.getStatus() == SubscriptionStatus.INITIAL) {
                log.debug("Subscription's status is INITIAL.");
                if (subscription.getBalance().getRealBalance() >= 0) {
                    DateTime estimatedNextBillDate = subscription.getActivationDate().plusMonths(1).withTime(23,59,59,999);
                    if(estimatedNextBillDate.getDayOfMonth() < subscription.getActivationDate().getDayOfMonth()){
                        estimatedNextBillDate = estimatedNextBillDate.plusDays(1);
                    }
                    subscription.setBilledUpToDate(estimatedNextBillDate);
                    subscription.setExpirationDate(subscription.getBilledUpToDate());
                    subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDate().plusDays(subscription.getBillingModel().getGracePeriodInDays()));

                    subscription.setStatus(SubscriptionStatus.ACTIVE);
                    subscription.setLastStatusChangeDate(DateTime.now());
                    subscription = subscriptionFacade.update(subscription);

                    if (provisioner.openService(subscription, subscription.getExpirationDateWithGracePeriod())) {
                        systemLogger.success(
                                SystemEvent.SUBSCRIPTION_PROLONGED,
                                subscription,
                                String.format("subscription id=%d switched from initial to active, prolonged on radius db", subscription.getId()));
                    }
                } else {
                    log.debug("Subscription's status is INITIAL and Balance is insufficient or cannot open Service! " + subscription.getAgreement());
                }
                return;
            }

            systemLogger.success(
                    SystemEvent.SUBSCRIPTION_STATUS_ACTIVE,
                    subscription,
                    String.format("status=%s, biiledUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s before activation attempt after payment.",
                            subscription.getStatus(),
                            subscription.getBilledUpToDate() != null ? subscription.getBilledUpToDate().toString(frm) : null,
                            subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                            subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                    ));

            if (subscription.getBillingModel().getPrinciple() == BillingPrinciple.CONTINUOUS) {

                if (subscription.getStatus() == SubscriptionStatus.BLOCKED
                        && subscription.getBalance().getRealBalance() >= 0) {

                    log.debug("CONTINUOUS. Status is BLOCKED and Balance is >= 0");
                    DateTime estimatedNextExpirationDate = DateTime.now().plusMonths(1).withTime(23, 59, 59, 999);
                    if(estimatedNextExpirationDate.getDayOfMonth() < DateTime.now().getDayOfMonth()){
                        estimatedNextExpirationDate = estimatedNextExpirationDate.plusDays(1);
                    }
                    subscription.setExpirationDate(estimatedNextExpirationDate);
                    subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDate());
                    subscription.setBilledUpToDate(subscription.getExpirationDate());
                    subscription.setStatus(SubscriptionStatus.ACTIVE);
                    subscription.setLastStatusChangeDate(DateTime.now());
                    subscription = subscriptionFacade.update(subscription);

                    if (provisioner.openService(subscription, subscription.getExpirationDateWithGracePeriod())) {
                        systemLogger.success(
                                SystemEvent.SUBSCRIPTION_PROLONGED,
                                subscription,
                                String.format("subscription id=%d switched from blocked to active, prolonged on radius db", subscription.getId()));
                    }
                }

            } else if (subscription.getStatus() == SubscriptionStatus.BLOCKED
                    && subscription.getBalance().getRealBalance() >= 0) {

                log.debug("GRACE. Status is BLOCKED and Balance is >= 0");
                if (subscription.getBilledUpToDate().isAfterNow()) {
                    subscription.setExpirationDate(subscription.getBilledUpToDate());
                    subscription.setExpirationDateWithGracePeriod(subscription.getBilledUpToDate().plusDays(subscription.getBillingModel().getGracePeriodInDays()));
                } else {
                    DateTime estimatedNextExpirationDate = subscription.getBilledUpToDate().plusMonths(1).withTime(23, 59, 59, 999);
                    if(estimatedNextExpirationDate.getDayOfMonth() < subscription.getBilledUpToDate().getDayOfMonth()){
                        estimatedNextExpirationDate = estimatedNextExpirationDate.plusDays(1);
                    }
                    subscription.setExpirationDate(estimatedNextExpirationDate);
                    subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDate().plusDays(subscription.getBillingModel().getGracePeriodInDays()));
                    subscription.setBilledUpToDate(subscription.getExpirationDate());
                }
                subscription.setStatus(SubscriptionStatus.ACTIVE);
                subscription.setLastStatusChangeDate(DateTime.now());
                subscription = subscriptionFacade.update(subscription);

                if (provisioner.openService(subscription, subscription.getExpirationDateWithGracePeriod())) {
                    systemLogger.success(
                            SystemEvent.SUBSCRIPTION_PROLONGED,
                            subscription,
                            String.format("subscription id=%d switched from blocked to active, prolonged on radius db", subscription.getId()));
                }
            } else if (subscription.getStatus() == SubscriptionStatus.PARTIALLY_BLOCKED
                    && subscription.getBalance().getRealBalance() >= 0) {
                log.debug("GRACE. Status is PARTIALLY_BLOCKED and Balance is >= 0");
                if (!subscription.getExpirationDate().isAfterNow()) {
                    DateTime estimatedNextBillDate = subscription.getExpirationDate().plusMonths(1).withTime(23, 59, 59, 999);
                    if(estimatedNextBillDate.getDayOfMonth() < subscription.getExpirationDate().getDayOfMonth()){
                        estimatedNextBillDate = estimatedNextBillDate.plusDays(1);
                    }
                    subscription.setBilledUpToDate(estimatedNextBillDate);
                    subscription.setExpirationDate(subscription.getBilledUpToDate());
                    subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDate().plusDays(subscription.getBillingModel().getGracePeriodInDays()));
                }

                subscription.setStatus(SubscriptionStatus.ACTIVE);
                subscription.setLastStatusChangeDate(DateTime.now());
                subscription = subscriptionFacade.update(subscription);

                if (provisioner.openService(subscription, subscription.getExpirationDateWithGracePeriod())) {
                    systemLogger.success(
                            SystemEvent.SUBSCRIPTION_PROLONGED,
                            subscription,
                            String.format("subscription id=%d switched from partial blocked to active, prolonged on radius db", subscription.getId()));
                }
            }
            systemLogger.success(
                    SystemEvent.SUBSCRIPTION_STATUS_ACTIVE,
                    subscription,
                    String.format("status=%s, biiledUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s successfully activated",
                            subscription.getStatus(),
                            subscription.getBilledUpToDate() != null ? subscription.getBilledUpToDate().toString(frm) : null,
                            subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                            subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                    ));
        }

    }
}
