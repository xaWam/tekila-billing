/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jaravir.tekila.module.accounting.manager;

import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.base.persistence.manager.BillingSettingsManager;
import com.jaravir.tekila.module.accounting.AccountingStatus;
import com.jaravir.tekila.module.accounting.InvoiceState;
import com.jaravir.tekila.module.accounting.entity.*;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.store.equip.Equipment;
import com.jaravir.tekila.module.store.equip.price.EquipmentPrice;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscriber;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberLifeCycleType;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import java.util.List;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import java.util.Date;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 *
 * @author sajabrayilov
 */
@Stateless
public class InvoicePersistenceFacade extends AbstractPersistenceFacade<Invoice> {

    @PersistenceContext
    private EntityManager em;
    @Resource
    private SessionContext ctx;

    @EJB
    private BillingSettingsManager billSettings;
    @EJB
    private ChargePersistenceFacade chargeFacade;
    @EJB
    private TransactionPersistenceFacade transFacade;
    @EJB
    private SystemLogger systemLogger;
    @EJB
    private UserPersistenceFacade userFacade;
    @EJB
    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;

    private static final Logger log = Logger.getLogger(InvoicePersistenceFacade.class);
    private Long subscriber;

    public enum Filter implements Filterable {

        ID("id"),
        STATE("state"),
        SUBSCRIBER_ID("subscriber.id"),
        SUBSCRIBER_NAME("subscriber.details.firstName"),
        SUBSCRIBER_MIDDLENAME("subscriber.details.middleName"),
        SUBSCRIBER_SURNAME("subscriber.details.surname");

        private String field;
        private MatchingOperation operation;

        Filter(String field) {
            this.field = field;
            operation = MatchingOperation.LIKE;
        }

        @Override
        public String getField() {
            return field;
        }

        @Override
        public MatchingOperation getOperation() {
            return operation;
        }

        public void setMatchingOperation(MatchingOperation operation) {
            this.operation = operation;
        }
    }

    public InvoicePersistenceFacade() {
        super(Invoice.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public Long getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(Long subscriber) {
        this.subscriber = subscriber;
    }

    /*@Override
     public List<Invoice> findAllPaginated(int first, int pageSize) {
     return (subscriber != null ? findAllPaginatedBySubscriber(first, pageSize) : super.findAllPaginated(first, pageSize));
     }
     */
    private List<Invoice> findAllPaginatedBySubscriber(int first, int pageSize) {
        List<Invoice> invoiceList = em.createQuery("select i from Invoice i join i.subscriber sub where sub.id = :sub_id order by i.id desc", Invoice.class)
                .setParameter("sub_id", subscriber)
                .setFirstResult(first)
                .setMaxResults(pageSize)
                .getResultList();
        log.debug("Invoices: " + invoiceList);
        return invoiceList;
    }
    /*
     @Override
     public long count() {
     return (subscriber != null) ? countFilteredBySubscriber() : countNoFilter();
     }
    
     private long countNoFilter() {
     return super.count();
     }
    
     private long countFilteredBySubscriber() {
     return em.createQuery("select count(i) from Invoice i join i.subscriber sub where sub.id = :sub_id", Long.class)
     .setParameter("sub_id", subscriber)
     .getSingleResult();
     }*/

    @Override
    public void save(Invoice invoice) {
        invoice.setCreationDate(DateTime.now().toDate());
        invoice.setUser(userFacade.findByUserName(ctx.getCallerPrincipal().getName()));
        em.persist(invoice);
    }

    @Override
    public Invoice update(Invoice entity) {
        entity.setUser(userFacade.findByUserName(ctx.getCallerPrincipal().getName()));
        return super.update(entity);
    }

    public List<Invoice> findBySubscriber(long subscriberID) {
        return em.createQuery("select i from Invoice i join i.subscriber sub where sub.id = :sub_id", Invoice.class)
                .setParameter("sub_id", subscriberID)
                .getResultList();
    }

    public List<Invoice> findByAgreementAndDate(String agreement, String startDate, String endDate) {

        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd.MM.yyyy");
        Date start_date = formatter.parseDateTime(startDate).toDate();
        Date end_date = formatter.parseDateTime(endDate).toDate();

        return em.createQuery("select i from Invoice i "
                + "where i.subscription.agreement = :agreement "
                + "and i.creationDate>=:start_date and i.creationDate<=:end_date", Invoice.class)
                .setParameter("agreement", agreement)
                .setParameter("start_date", start_date)
                .setParameter("end_date", end_date)
                .getResultList();
    }

    public List<Invoice> findOpenBySubscriber(long subscriberID) {
        return em.createQuery("select i from Invoice i join i.subscriber sub where sub.id = :sub_id and i.state = :state order by i.id desc", Invoice.class)
                .setParameter("sub_id", subscriberID)
                .setParameter("state", InvoiceState.OPEN)
                .getResultList();
    }

    public List<Invoice> findOpenBySubscriberForPayment(long subscriberID) {
        return em.createQuery("select i from Invoice i join i.subscriber sub where sub.id = :sub_id and i.state = :state order by i.id", Invoice.class)
                .setParameter("sub_id", subscriberID)
                .setParameter("state", InvoiceState.OPEN)
                .getResultList();
    }

    public Invoice findOpenBySubscriberForCharging(long subscriberID) {
        List<Invoice> invoiceList = em.createQuery("select i from Invoice i join i.subscriber sub where sub.id = :sub_id and i.state = :state order by i.id desc", Invoice.class)
                .setParameter("sub_id", subscriberID)
                .setParameter("state", InvoiceState.OPEN)
                .getResultList();
        Invoice inv = null;
        if (invoiceList.size() > 0) {
            inv = invoiceList.get(0);
        } else {
            inv = new Invoice();
            inv.setCreationDate(DateTime.now().toDate());
            inv.setState(InvoiceState.OPEN);
        }
        return inv;
    }

    public Invoice findOpenBySubscriberForCharging(long subscriberID, boolean isNotCreateInvoice) {
        List<Invoice> invoiceList = em.createQuery("select i from Invoice i join i.subscriber sub where sub.id = :sub_id and i.state = :state order by i.id desc", Invoice.class)
                .setParameter("sub_id", subscriberID)
                .setParameter("state", InvoiceState.OPEN)
                .getResultList();
        Invoice inv = null;

        if (invoiceList.size() > 0) {
            inv = invoiceList.get(0);
        }
        return inv;
    }

    public Invoice findOrCreateForPostpaid(Subscription subscription) {
        Invoice invoice = null;
        log.debug("Searching for invoice");
        /*if (subscription.getBilledUpToDate() == null) {
         createInvoiceForSubscriber(subscription.getSubscriber());
         }
         else {        */
        //try {
        DateTime prevDate
                = subscription.getSubscriber().getLifeCycle() == SubscriberLifeCycleType.POSTPAID
                        ? billSettings.getPostpaidPreviousLifeCycleDate(subscription.getBilledUpToDate())
                        : billSettings.getPrepaidPreviousLifeCycleDateTime(subscription.getBilledUpToDate());
        DateTime nextDate
                = subscription.getSubscriber().getLifeCycle() == SubscriberLifeCycleType.POSTPAID
                        ? billSettings.getPostpaidNextLifeCycleDate(subscription.getBilledUpToDate())
                        : billSettings.getPrepaidNextLifeCycleDateTime(subscription.getBilledUpToDate());
        log.debug(
                String.format("prevDate=%s, nextDate=%s", prevDate.withTimeAtStartOfDay().toDate(), nextDate.withTimeAtStartOfDay().toDate())
        );
        List<Invoice> invoiceList = em.createQuery(
                "select inv from Invoice inv join inv.subscriber sub "
                + "where sub.id = :sub_id  "
                + "and inv.state = :state  "
                + "and inv.creationDate between :prevCycle and :nextCycle", Invoice.class)
                .setParameter("sub_id", subscription.getSubscriber().getId())
                .setParameter("state", InvoiceState.OPEN)
                .setParameter("prevCycle", prevDate.withTimeAtStartOfDay().toDate())
                .setParameter("nextCycle", nextDate.withTimeAtStartOfDay().toDate())
                .getResultList();
        if (invoiceList.isEmpty()) {
            log.debug(String.format("No invoices retrieved for Subscription %s", subscription));
            //invoice = createInvoiceForSubscriber(subscription.getSubscriber());                    
        } else {
            invoice = invoiceList.get(0);
            log.debug(String.format("Invoice is: %s", invoice));
        }
           // }        
/*            catch (Exception ex) {
         log.error(String.format("No invoiceList retrieved for Subscription %s", subscription), ex);
         //invoice = createInvoiceForSubscriber(subscription.getSubscriber());
         }
         */
        //} // end ELSE
        return invoice;
    }

    public Invoice createInvoiceForSubscriber(Subscriber subscriber) {
        Invoice invoice = new Invoice();
        invoice.setState(InvoiceState.OPEN);
        invoice.setSubscriber(subscriber);
        invoice.setCreationDate(DateTime.now().toDate());
        save(invoice);
        return invoice;
    }




    public void addVasCharge(Subscription subscription, long rate, ValueAddedService vas, long userID, String desc) {
        rate = subscription.rerate(rate);
        Charge charge = chargeFacade.chargeForVAS(subscription, rate, desc, userID, vas);
        Transaction transaction = transFacade.createTransation(TransactionType.DEBIT, subscription, rate, desc);
        log.debug(String.format("Transaction is: %s", transaction));
        charge.setTransaction(transaction);


        systemLogger.success(SystemEvent.CHARGE, subscription, transaction,
                String.format("%s, charge id=%d for VAS id=%d, amount=%s", desc, charge.getId(), vas.getId(), charge.getAmountForView()));
    }


    public void addVasChargeToInvoice(Invoice invoice, Subscription subscription, long rate, ValueAddedService vas, long userID, String desc) {
        rate = subscription.rerate(rate);
        Charge charge = chargeFacade.chargeForVAS(subscription, rate, desc, userID, vas);
        Transaction transaction = transFacade.createTransation(TransactionType.DEBIT, subscription, rate, desc);
        log.debug(String.format("Transaction is: %s", transaction));
        charge.setTransaction(transaction);
        if (invoice != null) {
            invoice.addChargesToListWithCheck(charge, transaction.getStartBalance(), transaction.getEndBalance(), charge.getAmount());
        }

        systemLogger.success(SystemEvent.CHARGE, subscription, transaction,
                String.format("%s, charge id=%d for VAS id=%d, amount=%s", desc, charge.getId(), vas.getId(), charge.getAmountForView()));
    }

    public void addServiceChargeToInvoice(Invoice invoice, Subscription subscription, long rate, long userID, String desc) {
        Charge charge = chargeFacade.chargeForService(subscription, rate, desc, userID);
        Transaction transaction = transFacade.createTransation(TransactionType.DEBIT, subscription, rate, desc);
        charge.setTransaction(transaction);

        if (invoice != null) {
            invoice.addChargeToList(charge);
        }

        systemLogger.success(SystemEvent.CHARGE, subscription, transaction,
                String.format("%s, charge id=%d for service, amount=%s", desc, charge.getId(), charge.getAmountForView()));
    }

    public void addServiceChargeToInvoiceForCsm(Invoice invoice, Subscription subscription, long rate, long userID, String desc) {
        Charge charge = chargeFacade.chargeForService(subscription, rate, desc, userID);
        Transaction transaction = transFacade.createTransation(TransactionType.DEBIT, subscription, rate, desc);
        charge.setTransaction(transaction);

        if (invoice != null) {
            invoice.addChargesToListWithCheck(charge, transaction.getStartBalance(), transaction.getEndBalance(), charge.getAmount());
        }
        log.debug("Subscription real balance : "+subscription.getBalance().getRealBalanceForView());
        subscriptionPersistenceFacade.update(subscription);

        systemLogger.success(SystemEvent.CHARGE, subscription, transaction,
                String.format("%s, charge id=%d for service, amount=%s", desc, charge.getId(), charge.getAmountForView()));
    }

    public void addEquipmentChargeToInvoice(Invoice invoice, Equipment equipment, Subscription subscription, long userID) {
        addEquipmentChargeToInvoice(invoice, equipment, subscription, 0, userID);
    }

    public void addEquipmentChargeToInvoice(Invoice invoice, Equipment equipment, Subscription subscription, double discount, long userID) {
        String defaultDesc = String.format("Charge for equipment, part number: %s, price: %s, discount: %s%%",
                equipment.getPartNumber(), equipment.getPrice().getNominalForView(), discount > 0 ? String.format("%.2f", discount * 100) : equipment.getPrice().getDiscountForView());

        EquipmentPrice price = equipment.getPrice();
        long rate = price.getPrice();

        if (price.getDiscount() > 0) {
            rate = rate - Double.valueOf(rate * (price.getDiscount() / 100)).longValue();
        } else if (discount > 0) {
            rate = rate - Double.valueOf(rate * discount).longValue();
        }
        rate = subscription.rerate(rate);

        Charge charge = chargeFacade.chargeForEquipment(subscription, rate, discount, defaultDesc, userID);
        Transaction transaction = transFacade.createTransation(TransactionType.DEBIT, subscription, rate, defaultDesc);
        charge.setTransaction(transaction);

        invoice.addChargeToList(charge);

        systemLogger.success(
                SystemEvent.CHARGE,
                subscription,
                transaction,
                String.format("Charge id=%d for equipment id=%d, amount=%s",
                        charge.getId(), equipment.getId(), charge.getAmountForView()
                ));
    }

    public List<Invoice> findInvoicesByPayment(Subscription subscription, Payment payment) {
        return getEntityManager().createQuery("select inv from Invoice inv join inv.payments p where inv.subscriber.id = :sub_id and p.id = :pid order by inv.id", Invoice.class)
                .setParameter("sub_id", subscription.getSubscriber().getId())
                .setParameter("pid", payment.getId())
                .getResultList();
    }

    public List<Invoice> findInvoicesByCharge(Subscription subscription, Charge charge) {
        return getEntityManager().createQuery("select inv from Invoice inv where (inv.subscriber.id = :sub_id) and (:charge member of inv.charges) order by inv.id", Invoice.class)
                .setParameter("sub_id", subscription.getSubscriber().getId())
                .setParameter("charge", charge)
                .getResultList();
    }

    public long tryCloseInvoice(Invoice invoice, Payment payment, long residualValue) {
        return invoice.addPaymentToList(payment, residualValue);
    }

    public void cancelInvoice(Long invoiceID) {
        Invoice invoice = find(invoiceID);

        if (invoice == null) {
            log.debug(String.format("Invoice id %d not found", invoiceID));
            return;
        }

        List<Charge> chargeList = invoice.getCharges();

        if (chargeList != null) {
            for (Charge charge : chargeList) {
                chargeFacade.cancelCharge(charge);
            }
        }

    }
    //public void findLastClosed

    public Invoice cancelInvoice(Subscription subscription, String dsc) {
        /*if (subscription.getBalance().getRealBalance() >= 0 || subscription.getStatus() != SubscriptionStatus.BLOCKED) {
         throw new IllegalArgumentException(String.format("subscription agreement=%s, status=%s, balance=%d",
         subscription.getAgreement(), subscription.getStatus(), subscription.getBalance().getRealBalance()));
         }
         */
        long balanceBefore = subscription.getBalance().getRealBalance();
        //long amount = subscription.getService().getRateProfile().getActiveRate() * 100000;

        long amount = 0;

        log.info(String.format("cancelInvoice: subscription id=%d, agreement=%s, realBalance=%d, amount=%d before invoice cancellation",
                subscription.getId(), subscription.getAgreement(), balanceBefore, amount));

        Invoice openInvoice = findOpenBySubscriberForCharging(subscription.getSubscriber().getId(), true);

        Transaction transaction = null;

        if (openInvoice != null) {
            long charge = openInvoice.getSumCharged();
            openInvoice.setState(InvoiceState.CLOSED);
            openInvoice.setStatus(AccountingStatus.VOID);
            transaction = transFacade.createTransation(TransactionType.CREDIT, subscription, charge, String.format("returning charges of invoice id=%d", openInvoice.getId()));
        } else if (subscription.getBalance().getRealBalance() < 0) {
            transaction = transFacade.createTransation(TransactionType.OVERWRITE, subscription, amount, dsc);
        }

        log.info(String.format("cancelInvoice: subscription id=%d, agreement=%s, realBalance=%d, invoice id=%d after invoice cancellation",
                subscription.getId(), subscription.getAgreement(), subscription.getBalance().getRealBalance(), openInvoice != null ? openInvoice.getId() : 0));
        systemLogger.success(SystemEvent.INVOICE_VOIDED, subscription, transaction,
                String.format("realbalance before=%d, amount=%d, realBalance after=%d, transaction id=%d, invoice id=%d %s",
                        balanceBefore, amount, subscription.getBalance().getRealBalance(), transaction != null ? transaction.getId() : 0, openInvoice != null ? openInvoice.getId() : 0, dsc));

        return openInvoice;
    }

    public void removeDetached(Invoice invoice) {
        Invoice entity = getEntityManager().getReference(Invoice.class, invoice.getId());
        getEntityManager().remove(entity);
    }
}
