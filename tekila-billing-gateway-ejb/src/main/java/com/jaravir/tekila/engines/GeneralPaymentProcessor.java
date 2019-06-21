package com.jaravir.tekila.engines;

import com.jaravir.tekila.base.persistence.manager.BillingSettingsManager;
import com.jaravir.tekila.module.accounting.entity.Invoice;
import com.jaravir.tekila.module.accounting.manager.InvoicePersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberLifeCycleType;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.broadband.devices.manager.Providers;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.List;

/**
 * Created by khsadigov on 5/16/2017.
 */
@Stateless(name = "GeneralPaymentProcessor", mappedName = "GeneralPaymentProcessor")
public class GeneralPaymentProcessor implements PaymentProcessorEngine {

    @EJB
    private EngineFactory engineFactory;

    @EJB
    private SystemLogger systemLogger;

    @EJB
    private InvoicePersistenceFacade invoiceFacade;
    @EJB
    private BillingSettingsManager billSettings;

    private final static Logger log = LoggerFactory.getLogger(CitynetPaymentProcessor.class);


    final DateTimeFormatter frm = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void process(Subscription subscription, long amount) {

        log.debug("==== GeneralPaymentProcessor for subscription " + subscription.getId());


        ProvisioningEngine provisioner = null;
        try {
            provisioner = engineFactory.getProvisioningEngine(subscription);
        } catch (ProvisionerNotFoundException e) {
//            e.printStackTrace();
            log.error("Provisioner not found when process payment, subscription id: "+subscription.getId(), e);
        }

        log.info("Subscription[ id: {}, status: {}, real balance: {}, provider: {} ]",
                subscription.getId(),
                subscription.getStatus(),
                subscription.getBalance().getRealBalance(),
                subscription.getService().getProvider().getId()
                );

        if ((subscription.getStatus() == SubscriptionStatus.INITIAL
                || subscription.getStatus() == SubscriptionStatus.BLOCKED
                || subscription.getStatus() == SubscriptionStatus.PARTIALLY_BLOCKED)
                && subscription.getBalance().getRealBalance() >= 0
                && (subscription.getService().getProvider().getId() != Providers.CITYNET.getId())) {

            log.debug("Pass if INITIAL,BLOCKED, agreement=" + subscription.getAgreement());

            if (subscription.getSubscriber().getLifeCycle() == SubscriberLifeCycleType.PREPAID
                    && provisioner.openService(subscription)) {
                log.info(String.format("subscription agreement=%s, status=%s activating", subscription.getAgreement(), subscription.getStatus()));
                engineFactory.getOperationsEngine(subscription).activatePrepaid(subscription);
            } // end prepaid
            else if (subscription.getSubscriber().getLifeCycle() == SubscriberLifeCycleType.POSTPAID) {
                log.info("LifeCycleType is POSTPAID for subscription "+subscription.getId());
                List<Invoice> openInvoices = invoiceFacade.findOpenBySubscriberForPayment(subscription.getSubscriber().getId());

                if (openInvoices.isEmpty() && provisioner.openService(subscription)) {

                    if (subscription.getStatus() == SubscriptionStatus.INITIAL) {
                        subscription.setActivationDate(DateTime.now());
                    }

                    subscription.setStatus(SubscriptionStatus.ACTIVE);
                    subscription.setLastStatusChangeDate(DateTime.now());

                    if (billSettings.getSettings().getPospaidLifeCycleLength() == 30) {
                        DateTime estimatedNextExpirationDate = DateTime.now().plusMonths(1);
                        if(estimatedNextExpirationDate.getDayOfMonth() < DateTime.now().getDayOfMonth()){
                            estimatedNextExpirationDate = estimatedNextExpirationDate.plusDays(1);
                        }
                        subscription.setExpirationDate(estimatedNextExpirationDate);
                    } else {
                        subscription.setExpirationDate(DateTime.now().plusDays(billSettings.getSettings().getPospaidLifeCycleLength()));
                    }

                    subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDate().plusDays(
                            billSettings.getSettings().getPostpaidDefaultGracePeriod()
                    ));
                    subscription.synchronizeExpiratioDates();

                    subscription.setBilledUpToDate(subscription.getExpirationDate());

                    log.info(
                            String.format("Subscription id=%d, agreement=%s, status=%s, biiledUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s successfully activated",
                                    subscription.getId(),
                                    subscription.getAgreement(),
                                    subscription.getStatus(),
                                    subscription.getBilledUpToDate() != null ? subscription.getBilledUpToDate().toString(frm) : null,
                                    subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                                    subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                            ));
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


    }
}
