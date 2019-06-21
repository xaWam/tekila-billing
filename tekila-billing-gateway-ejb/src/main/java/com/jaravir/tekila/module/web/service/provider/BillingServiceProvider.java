/*
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jaravir.tekila.module.web.service.provider;

import com.jaravir.tekila.base.persistence.manager.BillingSettingsManager;
import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.module.accounting.entity.Payment;
import com.jaravir.tekila.module.accounting.manager.InvoicePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.PaymentPersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.TransactionPersistenceFacade;
import com.jaravir.tekila.module.event.BillingEvent;
import com.jaravir.tekila.module.messages.MessageStatus;
import com.jaravir.tekila.module.messages.MessageType;
import com.jaravir.tekila.module.messages.PersistentMessage;
import com.jaravir.tekila.module.messages.PersistentMessagePersistenceFacade;
import com.jaravir.tekila.module.queue.PersistentQueueManager;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriberPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.module.web.service.exception.NoInvoiceFoundException;
import com.jaravir.tekila.module.web.service.exception.NoSuchSubscriberException;
import com.jaravir.tekila.module.web.service.exception.NoSuchSubscriptionException;
import org.apache.log4j.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author sajabrayilov
 */
@Stateless
public class BillingServiceProvider {
    @EJB private SubscriberPersistenceFacade subscriberFacade;
    @EJB private SubscriptionPersistenceFacade subscriptionFacade;
    @EJB private TransactionPersistenceFacade transactionFacade;
    @EJB private EngineFactory provisioningFactory;
    @EJB private InvoicePersistenceFacade invoiceFacade;
    @EJB private PaymentPersistenceFacade paymentFacade;
    @EJB private BillingSettingsManager billingSettings;
    @EJB private PersistentQueueManager queueManager;
    @EJB private SystemLogger systemLogger;
    @EJB private PersistentMessagePersistenceFacade messagePersistenceFacade;

    private static final Logger log = Logger.getLogger(BillingServiceProvider.class);

    /**
     * Web service operation
     * @param paymentID
     * @return boolean
     * @param subscriberID
     * @param subscriptionID
     * @param amount
     * @throws com.jaravir.tekila.module.web.service.exception.NoSuchSubscriberException
     * @throws com.jaravir.tekila.module.web.service.exception.NoSuchSubscriptionException
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean settlePayment(
            final Long subscriberID,
            final Long subscriptionID,
            final Double amount,
            final Long paymentID
            )
            throws NoSuchSubscriberException, NoSuchSubscriptionException, NoInvoiceFoundException {

        if ((subscriberID != null && subscriberID <= 0) ||
                subscriptionID == null || subscriptionID <= 0 ||
                amount == null || amount < 0
                ) {
            throw new IllegalArgumentException("Please provide all parameters");
        }

        log.info("Bus entered: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
        Float am = amount.floatValue() * 100000;
        try {
            Payment payment = subscriptionFacade.settlePayment(subscriptionID, am.longValue(), amount, paymentID);

            queueManager.addToPaymentQueue(subscriptionID, amount, paymentID);
            queueManager.addToNotificationsQueue(BillingEvent.PAYMENT, subscriptionID, payment.getAmount());
        }
        catch (NoSuchSubscriptionException ex) {
            log.error(String.format("Subscription id=%d not found", subscriptionID), ex);
            return false;
        }
        catch (Exception ex) {
            log.error(
                String.format("Cannot settle payment for subscriberID=%d, subscriptionID=%d, paymentID=%d, amount=%f",
                        subscriberID, subscriptionID, paymentID, amount), ex);
            log.error(ex);
            return false;
        }
       
        return true;
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean settlePayment(
            final Long subscriberID,
            final Long subscriptionID,
            final Double amount,
            final Long paymentID,
            final String userN
    )
            throws NoSuchSubscriberException, NoSuchSubscriptionException, NoInvoiceFoundException {

        if ((subscriberID != null && subscriberID <= 0) ||
                subscriptionID == null || subscriptionID <= 0 ||
                amount == null || amount < 0
                ) {
            throw new IllegalArgumentException("Please provide all parameters");
        }

        log.info("Bus entered: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
        Float am = amount.floatValue() * 100000;
        try {
            Payment payment = subscriptionFacade.settlePayment(subscriptionID, am.longValue(), amount, paymentID, userN);

            queueManager.addToPaymentQueue(subscriptionID, amount, userN);
//            queueManager.addToNotificationsQueue(BillingEvent.PAYMENT, subscriptionID, payment.getAmount());
        }
        catch (NoSuchSubscriptionException ex) {
            log.error(String.format("Subscription id=%d not found", subscriptionID), ex);
            return false;
        }
        catch (Exception ex) {
            log.error(
                    String.format("Cannot settle payment for subscriberID=%d, subscriptionID=%d, paymentID=%d, amount=%f",
                            subscriberID, subscriptionID, paymentID, amount), ex);
            log.error(ex);
            return false;
        }

        return true;
    }
    /**
     * Add payment to {@code proccessedPaymentQueue} which is monitored by <b>Provisioning Gateway</b>
     * triggering provisioning attempt
     *
     */

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean addToPaymentQueue(
            final Long subscriberID,
            final Long subscriptionID,
            final Double amount
    ) {
        boolean success = false;

        Subscription subscription = subscriptionFacade.find(subscriptionID);


        try {
            Float am = amount.floatValue() * 100000;

            PersistentMessage message = new PersistentMessage();
            message.setStatus(MessageStatus.ADDED);
            message.setMessageType(MessageType.PAYMENT);

            message.addProperty("amount", String.valueOf(am.longValue()));
            message.addProperty("subscriptionID", String.valueOf(subscriptionID));
            message.addProperty("subscriberID", String.valueOf(subscription.getSubscriber().getId()));
            message.setProperties(message.serializeProperties());
            messagePersistenceFacade.save(message);

            StringBuilder stringBuilder = new StringBuilder("ADD_TO_PAYMENT_QUEUE");
            stringBuilder.append(", subscriberID = " + subscriberID);
            stringBuilder.append(", subscriptionID = " + subscriptionID);
            stringBuilder.append(", amount = " + amount);
            log.info(stringBuilder.toString());

            success = true;
        } catch (Exception ex) {
            success = false;
        }

        return success;
    }
}
