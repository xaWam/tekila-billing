/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jaravir.tekila.module.accounting.manager;

import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.accounting.AccountingStatus;
import com.jaravir.tekila.module.accounting.InvoiceState;
import com.jaravir.tekila.module.accounting.entity.*;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.store.equip.price.EquipmentPrice;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberLifeCycleType;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;

import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;

import java.util.Date;

import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import spring.model.helper.DoubleCharge;

/**
 * @author sajabrayilov
 */
@Stateless
public class ChargePersistenceFacade extends AbstractPersistenceFacade<Charge> {
    private static Logger log = Logger.getLogger(ChargePersistenceFacade.class);

    @PersistenceContext
    private EntityManager em;
    private long subscriberId;

    @EJB
    private TransactionPersistenceFacade transFacade;
    @EJB
    private SubscriptionPersistenceFacade subscriptionFacade;
    @EJB
    private SystemLogger systemLogger;
    @EJB
    private InvoicePersistenceFacade invoiceFacade;

    public enum Filter implements Filterable {
        AGREEMENT("subscription.agreement"),
        SUBSCRIBER_ID("subscriber.id");

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

    public ChargePersistenceFacade() {
        super(Charge.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public List<Charge> findByAgreement(String agreement) {

        return em.createQuery("select c from Charge c "
                + "where c.subscription.agreement=:agreement ", Charge.class)
                .setParameter("agreement", agreement)
                .getResultList();
    }

    public List<Charge> findByAgreement(String agreement, String startDate, String endDate) {

        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd.MM.yyyy");
        DateTime start_date = formatter.parseDateTime(startDate);
        DateTime end_date = formatter.parseDateTime(endDate);

        return em.createQuery("select c from Charge c "
                + "where c.subscription.agreement=:agreement "
                + "and c.datetime>=:start_date and c.datetime<=:end_date", Charge.class)
                .setParameter("agreement", agreement)
                .setParameter("start_date", start_date)
                .setParameter("end_date", end_date)
                .getResultList();
    }

    public List<Charge> findByAgreement(String agreement, DateTime startDate, DateTime endDate) {


        return em.createQuery("select c from Charge c "
                + "where c.subscription.agreement=:agreement "
                + "and c.datetime>=:start_date and c.datetime<=:end_date", Charge.class)
                .setParameter("agreement", agreement)
                .setParameter("start_date", startDate)
                .setParameter("end_date", endDate)
                .getResultList();
    }

    public Charge chargeForService(Subscription subscription, long rate, String desc, long userID) {
        Charge charge = createCharge(subscription, rate, desc, userID);
        save(charge);
        return charge;
    }

    public Charge chargeForVAS(Subscription subscription, long rate, String desc, long userID, ValueAddedService vas) {
        Charge charge = createCharge(subscription, rate, desc, userID);
        charge.setVas(vas);
        save(charge);
        return charge;
    }

    /*public Charge chargeForEquipment(Subscription subscription, EquipmentPrice price, String desc, long userID) {
        return chargeForEquipment(subscription, price, 0, desc, userID);
    }*/

    public Charge chargeForEquipment(Subscription subscription, long rate, double discount, String desc, long userID) {
        Charge charge = createCharge(subscription, rate, desc, userID);
        save(charge);
        return charge;
    }

    private Charge createCharge(Subscription subscription, long rate, String desc, long userID) {
        Charge charge = new Charge();
        charge.setAmount(rate);
        charge.setSubscriber(subscription.getSubscriber());
        charge.setService(subscription.getService());
        charge.setUser_id(userID);
        charge.setDsc(desc);
        charge.setDatetime(DateTime.now());
        charge.setSubscription(subscription);
        return charge;
    }

    public void cancelCharge(Charge charge) {
        throw new UnsupportedOperationException();
    }

    public void cancelCharge(long chargeId) throws Exception {
        String message = null;
        Transaction transaction = null;

        final Charge charge = find(chargeId);

        if (charge == null || (charge.getStatus() != null && charge.getStatus().equals(AccountingStatus.VOID))) {
            throw new IllegalArgumentException(String.format("Charge id=%d not found or cancelled", chargeId));
        }
        if (charge.getSubscription() == null) {
            throw new IllegalArgumentException(String.format("Charge id=%d has not been assigned to any subscription", chargeId));
        }

        long amount = charge.getAmount();
        log.info("Starting charge cancellation procedure for charge id=" + chargeId + ": " + charge);

        try {
            transaction = transFacade.createTransation(
                    TransactionType.CREDIT,
                    charge.getSubscription(),
                    amount,
                    String.format("Charge id=%d is cancelled through UI", charge.getId()));

            message = String.format("Transaction %d has been created during cancellation of charge %d", transaction.getId(), charge.getId());
            log.info(message);
            Subscription subscription = charge.getSubscription();

            List<Invoice> invoices = invoiceFacade.findInvoicesByCharge(subscription, charge);
            log.info(String.format("Found invoice - total: %d. %s", invoices.size(), invoices));

            if (!invoices.isEmpty()) {
                InvoiceState oldState = null;
                for (Invoice invoice : invoices) {
                    oldState = invoice.getState();
                    invoice.reduceDebt(charge.getAmount());
                    invoice.close();

                    if (invoice.getState() == InvoiceState.CLOSED && !oldState.equals(invoice.getState())) {
                        message = String.format("Invoice state changed from OPEN to CLOSED: id=%d, state=%s, sumCharged=%d, sumPaid=%d, balance=%d",
                                invoice.getId(), invoice.getState(), invoice.getSumCharged(), invoice.getSumPaid(), invoice.getBalance());
                        log.info(message);
                        systemLogger.success(SystemEvent.INVOICE_STATE_CHANGED, subscription, message);
                    }
                }
            }

            charge.setStatus(AccountingStatus.VOID);
            systemLogger.success(SystemEvent.CHARGE_CANCELLED, charge.getSubscription(), transaction, String.format("charge id=%d", charge.getId()));
            log.info("Ending charge cancellation procedure for charge id= " + chargeId + " with SUCCESS");
        } catch (Exception ex) {
            message = String.format("Cannot cancel charge. Charge %d with transaction %d", charge.getId(), transaction != null ? transaction.getId() : -1);
            log.error(message, ex);
            systemLogger.error(SystemEvent.CHARGE_CANCELLED, charge.getSubscription(), transaction, message + " " + ex.getCause().getMessage());
            log.error("Ending charge cancellation procedure for charge id= " + chargeId + " with ERROR");
            throw new Exception();
        }
    }

    public void removeCharges(Transaction transaction) {
        log.info(String.format("removing charges for transaction id = %d.........", transaction.getId()));
        getEntityManager().createQuery("delete from Charge c where c.transaction.id = :id")
                .setParameter("id", transaction.getId()).executeUpdate();
        log.info(String.format("removed charges for transaction id = %d", transaction.getId()));
    }

    public void addVasCharge(Subscription subscription, long rate, ValueAddedService vas, long userID, String desc) {
        rate = subscription.rerate(rate);
        Charge charge = chargeForVAS(subscription, rate, desc, userID, vas);
        Transaction transaction = transFacade.createTransation(TransactionType.DEBIT, subscription, rate, desc);
        log.debug(String.format("Transaction is: %s", transaction));
        charge.setTransaction(transaction);


        systemLogger.success(SystemEvent.CHARGE, subscription, transaction,
                String.format("%s, charge id=%d for VAS id=%d, amount=%s", desc, charge.getId(), vas.getId(), charge.getAmountForView()));
    }

    public List<Object[]> cityNetDoubleCharge(){
      List<Object[]>  doubleCharges =
              em.createNativeQuery("select c.* from citynet_doublecharge_detail4 c where c.status is null").getResultList();

      log.debug("result of double charges "+doubleCharges.size());

        return doubleCharges;
    }


    public boolean updatecityNetDoubleCharge(int chid){
        try {
            String a = "ok";
            Query updateQuery =
                    em.createNativeQuery("update tekila.citynet_doublecharge_detail2 set status = 'OK' where chargeid = "+chid);

               int resut =      updateQuery.executeUpdate();
            log.debug("result of update "+resut);
        }catch (Exception ex){

            log.debug("exception  - "+ex );
            return false;
        }


        return true;
    }
}
