package com.jaravir.tekila.module.queue;

import com.jaravir.tekila.module.accounting.entity.Invoice;
import com.jaravir.tekila.module.event.BillingEvent;
import com.jaravir.tekila.module.messages.MessageStatus;
import com.jaravir.tekila.module.messages.MessageType;
import com.jaravir.tekila.module.messages.PersistentMessage;
import com.jaravir.tekila.module.messages.PersistentMessagePersistenceFacade;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import org.apache.log4j.Logger;

import javax.ejb.*;
import javax.jms.*;
import java.util.Optional;

/**
 * Created by kmaharov on 19.09.2017.
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class PersistentQueueManager {
    @EJB
    private SubscriptionPersistenceFacade subscriptionFacade;
    @EJB
    private PersistentMessagePersistenceFacade messagePersistenceFacade;
    @EJB
    private SystemLogger systemLogger;
    @EJB
    private MessageIdGenerator messageIdGenerator;

    private static final Logger log = Logger.getLogger(PersistentQueueManager.class);

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean addToPaymentQueue(
            final Long subscriptionID,
            final Double amount,
            final Long paymentID
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
            if (paymentID != -1)
                message.setPaymentId(paymentID);
            messagePersistenceFacade.save(message);

            success = true;

            Optional<Long> realBalance = Optional.ofNullable(subscription.getBalance().getRealBalance());
            String msg = String.format("event=%s, subscription id=%d, real balance=%s", BillingEvent.PAYMENT, subscription.getId(), realBalance.orElse(null));
            systemLogger.success(SystemEvent.NOTIFICATION_ADDED, subscription, msg);
            log.info("Notification added: " + msg);
        } catch (Exception ex) {
            String msg = String.format("event=%s, subscription id=%d", BillingEvent.PAYMENT, subscription.getId());
            log.error("Cannot add notification: " + msg, ex);
            success = false;
            systemLogger.error(SystemEvent.NOTIFICATION_ADDED, subscription, msg);
        }

        return success;
    }

    public boolean addToPaymentQueue(
            final Long subscriptionID,
            final Double amount,
            String userN
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

            success = true;

            String msg = String.format("event=%s, subscription id=%d", BillingEvent.PAYMENT, subscription.getId());
            systemLogger.successExP(SystemEvent.NOTIFICATION_ADDED, subscription, msg, userN);
            log.info("Notification added: " + msg);
        } catch (Exception ex) {
            String msg = String.format("event=%s, subscription id=%d", BillingEvent.PAYMENT, subscription.getId());
            log.error("Cannot add notification: " + msg, ex);
            success = false;
            systemLogger.errorExP(SystemEvent.NOTIFICATION_ADDED, subscription, msg, userN);
        }

        return success;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void addToNotificationsQueue(BillingEvent event, Long subscriptionID, Double payment) throws Exception {
        Subscription subscription = subscriptionFacade.find(subscriptionID);
        sendToNotificatonsQueue(event, subscription, payment, null, null);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void sendInvoiceNotification(BillingEvent event, Long subscriptionID, Invoice invoice) throws Exception {
        Subscription subscription = subscriptionFacade.find(subscriptionID);
        sendToNotificatonsQueue(event, subscription, null, invoice, null);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void sendStatusNotification(BillingEvent event, Long subscriptionID) throws Exception {
        Subscription subscription = subscriptionFacade.find(subscriptionID);
        sendToNotificatonsQueue(event, subscription, null, null, null);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void sendServiceChangeNotification(BillingEvent event, Long subscriptionID) throws Exception {
        Subscription subscription = subscriptionFacade.find(subscriptionID);
        sendToNotificatonsQueue(event, subscription, null, null, null);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void sendVASNotification(BillingEvent event, Long subscriptionID, ValueAddedService vas) throws Exception {
        Subscription subscription = subscriptionFacade.find(subscriptionID);
        sendToNotificatonsQueue(event, subscription, null, null, vas);
    }


    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void addToNotificationsQueue(BillingEvent event, Subscription subscription, Double payment) throws Exception {
        sendToNotificatonsQueue(event, subscription, payment, null, null);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void sendInvoiceNotification(BillingEvent event, Subscription subscription, Invoice invoice) throws Exception {
        sendToNotificatonsQueue(event, subscription, null, invoice, null);
    }

    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void sendStatusNotification(BillingEvent event, Subscription subscription) throws Exception {
        sendToNotificatonsQueue(event, subscription, null, null, null);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void sendVASNotification(BillingEvent event, Subscription subscription, ValueAddedService vas) throws Exception {
        sendToNotificatonsQueue(event, subscription, null, null, vas);
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    private void sendToNotificatonsQueue(BillingEvent event, Subscription subscription, Double payment, Invoice invoice, ValueAddedService vas) throws Exception {
        Connection connection = null;

        try {
            if (event == null)
                throw new IllegalArgumentException("An event must be provided");
            else if (subscription == null)
                throw new IllegalArgumentException(String.format("No subscription with id=%d found", subscription));

            PersistentMessage message = new PersistentMessage();
            message.setStatus(MessageStatus.ADDED);
            message.setMessageType(MessageType.NOTIFICATION);

            message.addProperty("event", event.getCode());
            message.addProperty("subscriptionID", String.valueOf(subscription.getId()));

            Integer nextMessageId = messageIdGenerator.getNext();
            message.addProperty("messageID", String.valueOf(nextMessageId));
            log.info("sendToNotificatonsQueue for  "+subscription.getAgreement());
            log.info("messageId: " + nextMessageId);

            if (payment != null)
                message.addProperty("payment", String.valueOf(payment));
            if (invoice != null)
                message.addProperty("invoiceID", String.valueOf(invoice.getId()));
            if (vas != null)
                message.addProperty("vasID", String.valueOf(vas.getId()));
            message.setProperties(message.serializeProperties());

            messagePersistenceFacade.save(message);
        } catch (Exception ex) {
            String msg = String.format("event=%s, subscription id=%d", event, subscription.getId());
            log.error("Cannot add notification: " + msg, ex);
            throw new Exception(ex.getCause());
        }
    }

    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void sendToAfterBillingTopic(Subscription subscription) {
        try {
            PersistentMessage message = new PersistentMessage();
            message.setStatus(MessageStatus.ADDED);
            message.setMessageType(MessageType.CAMPAIGN);
            message.addProperty("subscriptionID", String.valueOf(subscription.getId()));
            message.setProperties(message.serializeProperties());
            log.debug("message going to be save for "+subscription.getAgreement());
            messagePersistenceFacade.save(message);
            log.debug("sendToAfterBillingTopic successfully worked for "+subscription.getAgreement());
            systemLogger.success(SystemEvent.SUBSCRIPTION_AFTER_BILLED, subscription, "");
        } catch (Exception ex) {
            log.error(String.format("Cannot send to AFTER BILLING Topic, subscription id=%d", subscription.getId()), ex);
        }
    }
}
