/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jaravir.tekila.module.accounting.manager;

import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.accounting.entity.Payment;
import com.jaravir.tekila.module.accounting.entity.Transaction;
import com.jaravir.tekila.module.accounting.entity.TransactionType;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.jaravir.tekila.module.system.log.SystemLogger;
import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 *
 * @author sajabrayilov
 */
@Stateless
public class TransactionPersistenceFacade extends AbstractPersistenceFacade<Transaction> {
    
    @PersistenceContext
    private EntityManager em;

    private final static Logger log = Logger.getLogger(TransactionPersistenceFacade.class);
    
    @EJB
    private SubscriptionPersistenceFacade subscriptionFacade;
    @EJB
    private SystemLogger systemLogger;
    @EJB
    private ChargePersistenceFacade chargePersistenceFacade;
    
    public enum Filter implements Filterable {
        
        ID("id"),
        AGREEMENT("subscription.agreement"),
        SUBSCRIPTION_ID("subscription.id"),
        TYPE("type");
        
        private final String code;
        private MatchingOperation operation;
        
        Filter(String code) {
            this.code = code;
            this.setOperation(MatchingOperation.EQUALS);
        }
        
        public MatchingOperation getOperation() {
            return operation;
        }
        
        public void setOperation(MatchingOperation operation) {
            this.operation = operation;
        }
        
        @Override
        public String getField() {
            return code;
        }
    }
    
    public TransactionPersistenceFacade() {
        super(Transaction.class);
    }
    
    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
    
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Transaction createTransation(TransactionType type, Subscription subscription, long rate, String desc) {
        Transaction transaction = new Transaction(type, subscription, rate, desc);
        transaction.execute();
        save(transaction);
        return transaction;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Transaction createTransationForPromo(TransactionType type, Subscription subscription, long rate, String desc) {
        Transaction transaction = new Transaction(type, subscription, rate, desc);
        transaction.executePromo();
        save(transaction);
        return transaction;
    }

    public void transferPaymentToSubscription(Payment payment, Subscription target) {
        createTransation(
                TransactionType.DEBIT,
                payment.getAccount(), payment.getAmountAsLong(),
                String.format("Payment id=%d transfer to subscription id=%d", payment.getId(), target.getId()));
        
        createTransation(
                TransactionType.PAYMENT,
                target, payment.getAmountAsLong(),
                String.format("Payment id=%d transfer from subscription id=%d", payment.getId(), payment.getAccount().getId())
        );
    }

    public List<Transaction> getTransactionsBySubscriptionId(long subscription_id) {
        return getEntityManager().createQuery("select t from Transaction t where t.subscription.id = :id").
                setParameter("id", subscription_id).getResultList();
    }

    public String getTransactionsBySubscriberID(long subscriber_id) {
        List<Long> subscriptionsList = subscriptionFacade.findSubscriptionIDsBySubscriberId(subscriber_id);
        
        String query = "select t from Transaction t where t.subscription.id in (";
        
        query += subscriptionsList.get(0);
        
        for (int i = 1; i < subscriptionsList.size(); i++) {
            query += "," + subscriptionsList.get(i);
        }
        
        query += ")";
        
        Log.debug("Query from getTransactionsBySubscriberID: " + query);
        
        return query;
    }

    public void removeDetached(Transaction transaction) {
        try {
            log.info(String.format("removing transaction id = %d.....", transaction.getId()));
            Transaction entity = getEntityManager().getReference(Transaction.class, transaction.getId());
            chargePersistenceFacade.removeCharges(entity);
            getEntityManager().remove(entity);
            log.info(String.format("removing transaction id = %d.....", transaction.getId()));
        } catch (Exception ex) {
            log.error(ex);
        }
    }

    public List<Transaction> findByAgreement(String agreement) {

        return em.createQuery("select t from Transaction t "
                + "where t.subscription.agreement=:agreement ", Transaction.class)
                .setParameter("agreement", agreement)
                .getResultList();
    }


    public List<Transaction> findByAgreementAndDates(String agreement, String startDate, String endDate) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd.MM.yyyy");
        DateTime start_date = formatter.parseDateTime(startDate);
        DateTime end_date = formatter.parseDateTime(endDate);

        return em.createQuery("select t from Transaction t "
                + "where t.subscription.agreement=:agreement "
                + "and t.lastUpdateDate>=:start_date and t.lastUpdateDate<=:end_date", Transaction.class)
                .setParameter("agreement", agreement)
                .setParameter("start_date", start_date)
                .setParameter("end_date", end_date)
                .getResultList();
    }


}
