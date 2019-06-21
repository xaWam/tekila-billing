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
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.List;

/**
 * Created by khsadigov on 5/16/2017.
 */
@Stateless(name = "AzPostPaymentProcessor", mappedName = "AzPostPaymentProcessor")
public class AzPostPaymentProcessor implements PaymentProcessorEngine {
    @EJB
    private EngineFactory engineFactory;

    @EJB
    private SubscriptionPersistenceFacade subscriptionFacade;

    @EJB
    private SystemLogger systemLogger;

    @EJB
    private InvoicePersistenceFacade invoiceFacade;
    @EJB
    private BillingSettingsManager billSettings;

    private final static Logger log = Logger.getLogger(CitynetPaymentProcessor.class);


    DateTimeFormatter frm = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void process(Subscription subscription, long amount) {

        log.debug("==== AzPostPaymentProcessor ====");


        ProvisioningEngine provisioner = null;
        try {
            provisioner = engineFactory.getProvisioningEngine(subscription);
        } catch (ProvisionerNotFoundException e) {
            e.printStackTrace();
        }
        List<Invoice> openInvoices = invoiceFacade.findOpenBySubscriberForPayment(subscription.getSubscriber().getId());
        log.debug("(subscription.getStatus()-"+subscription.getStatus()+"   subscription.getBalance().getRealBalance()-"+ subscription.getBalance().getRealBalance()
        +"  subscription.getService().getProvider().getId()-"+subscription.getService().getProvider().getId() + "  subscription.getBilledUpToDate().isBeforeNow()"+
                subscription.getBilledUpToDate().isBeforeNow() + "    openInvoices.isEmpty()   "+openInvoices.isEmpty());

        if ((subscription.getStatus() == SubscriptionStatus.INITIAL
                || subscription.getStatus() == SubscriptionStatus.BLOCKED
                || subscription.getStatus() == SubscriptionStatus.PARTIALLY_BLOCKED)
                && subscription.getBalance().getRealBalance() >= 0
                && (subscription.getService().getProvider().getId() == Providers.AZERTELECOMPOST.getId())) {

            log.debug("Pass if INITIAL,BLOCKED, agreement=" + subscription.getAgreement());


            if (subscription.getSubscriber().getLifeCycle() == SubscriberLifeCycleType.PREPAID
                    && provisioner.openService(subscription)) {
                log.info(String.format("subscritiption agreement=%s, status=%s activating", subscription.getAgreement(), subscription.getStatus()));
                engineFactory.getOperationsEngine(subscription).activatePrepaid(subscription);
            } // end prepaid
            else if (subscription.getSubscriber().getLifeCycle() == SubscriberLifeCycleType.POSTPAID) {
log.debug("Inside POSTPAID");


                if (openInvoices.isEmpty()) {
                log.debug("empty invoices");
                    if (subscription.getStatus() == SubscriptionStatus.INITIAL) {
                        subscription.setActivationDate(DateTime.now());
                    }

                    subscription.setStatus(SubscriptionStatus.ACTIVE);
                    log.debug(" //////////subscription.getStatus()/////   "+subscription.getStatus());
                    subscription.setLastStatusChangeDate(DateTime.now());
log.debug("billSettings.getSettings().getPospaidLifeCycleLength()- "+billSettings.getSettings().getPospaidLifeCycleLength());


//if (billSettings.getSettings().getPospaidLifeCycleLength() == 30) {
                        subscription.setExpirationDate(DateTime.now().plusMonths(1).withDayOfMonth(4));
//                    } else {
//                        subscription.setExpirationDate(DateTime.now().plusDays(billSettings.getSettings().getPospaidLifeCycleLength()));
//                    }

                    subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDate().plusDays(
                            billSettings.getSettings().getPostpaidDefaultGracePeriod()
                    ));
                    subscription.synchronizeExpiratioDates();

                    subscription.setBilledUpToDate(subscription.getExpirationDate());


                    subscription = subscriptionFacade.update(subscription);
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
        }else if (subscription.getStatus() == SubscriptionStatus.ACTIVE && subscription.getBilledUpToDate().isBeforeNow() && openInvoices.isEmpty()){
log.debug("Have to prolongate ");
subscription.setBilledUpToDate(DateTime.now().plusMonths(1).withDayOfMonth(4));
        subscription.setExpirationDate(DateTime.now().plusMonths(1).withDayOfMonth(4));
            subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDate()
                    .plusDays(billSettings.getSettings().getPostpaidDefaultGracePeriod()));

            subscription = subscriptionFacade.update(subscription);

        }


    }
}
