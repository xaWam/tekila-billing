/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jaravir.tekila.module.accounting.manager;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.ExternalUserPersistenceFacade;
import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.module.accounting.InvoiceState;
import com.jaravir.tekila.module.accounting.entity.Invoice;
import com.jaravir.tekila.module.accounting.entity.Payment;
import com.jaravir.tekila.module.accounting.entity.Transaction;
import com.jaravir.tekila.module.accounting.entity.TransactionType;
import com.jaravir.tekila.module.campaign.Campaign;
import com.jaravir.tekila.module.campaign.CampaignPersistenceFacade;
import com.jaravir.tekila.module.campaign.CampaignRegister;
import com.jaravir.tekila.module.campaign.CampaignRegisterPersistenceFacade;
import com.jaravir.tekila.module.payment.PaymentOptionsPersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.ServicePersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberLifeCycleType;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.broadband.devices.manager.Providers;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * @author sajabrayilov
 */
@Stateless
public class PaymentPersistenceFacade extends AbstractPersistenceFacade<Payment> {

    @PersistenceContext
    private EntityManager em;

    @Resource
    private SessionContext ctx;

    @EJB
    private ExternalUserPersistenceFacade externalUserFacade;
    @EJB
    private SubscriptionPersistenceFacade subscriptionFacade;
    @EJB
    private ServicePersistenceFacade serviceFacade;
    @EJB
    private TransactionPersistenceFacade transactionFacade;
    @EJB
    private SystemLogger systemLogger;
    @EJB
    private EngineFactory provisioningFactory;
    @EJB
    private InvoicePersistenceFacade invoiceFacade;
    @EJB
    private CampaignPersistenceFacade campaignPersistenceFacade;
    @EJB
    private CampaignRegisterPersistenceFacade campaignRegisterPersistenceFacade;

    @EJB
    private PaymentOptionsPersistenceFacade paymentOptionsFacade;

    private long subscriberID;
    private final static Logger log = Logger.getLogger(PaymentPersistenceFacade.class);

    public enum Filter implements Filterable {

        DATE("fd"),
        USER("user.userName"),
        EXT_USER("ext_user.username"),
        ACCOUNT_ID("account.id"),
        SUBSCRIBER_ID("subscriber_id"),
        CHEQUE_ID("chequeID"),
        RRN("rrn"),
        STATUS("status"),
        SHOW_TEST("account.subscriber.fnCategory"),
        PROVIDER_ID("account.service.provider.id"),
        AGREEMENT("contract"),
        METHOD("method.name");

        private final String code;
        private MatchingOperation operation;

        Filter(String code) {
            this.code = code;
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

    public PaymentPersistenceFacade() {
        super(Payment.class);
    }

    public long getSubscriberID() {
        return subscriberID;
    }

    public void setSubscriberID(long subscriberID) {
        this.subscriberID = subscriberID;
    }

    @Override
    protected EntityManager getEntityManager() {
        return this.em;
    }

    public double sumWithFilters() {
        if (getFilters().isEmpty()) {
            throw new UnsupportedOperationException("At least one FILTER must provided");
        }

        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<BigDecimal> criteriaQuery = cb.createQuery(BigDecimal.class);

        Root root = criteriaQuery.from(Payment.class);
        //Root model = criteriaQuery.from(EquipmentModel.class);
        // CriteriaBuilder.Coalesce<Double> coalesce = cb.<Double>coalesce().value(cb.<Double>sum(root.get("amount"))).value(0.00);
        //cb.sum(root.get("amount")
        criteriaQuery.select(cb.<BigDecimal>coalesce().value(cb.sum(root.get("amount"))).value(new BigDecimal(0d)));
        //Join<Equipment, ServiceProvider> providerJoin = root.join("provider");
        //Join<Equipment, EquipmentModel> modelJoin = root.join("model");
        criteriaQuery.where(getPredicateWithFilters(cb, root));

        TypedQuery<BigDecimal> sumQuery = getEntityManager().createQuery(criteriaQuery);

        log.debug("sumWithFilters: sumQuery=" + sumQuery);
        Double result = sumQuery.getSingleResult().doubleValue();
        log.debug("sumWIthFilters: Result=" + result);
        return result;
    }

    public boolean isChequeExists(String chequue) {
        return getEntityManager()
                .createQuery("select count(p) from Payment p where p.chequeID = :cheque", Long.class)
                .setParameter("cheque", chequue).getSingleResult() > 0 ? true : false;
    }

    public List<Payment> findAllUnproccessedUninet(DateTime lastUpdateDate) {
        return getEntityManager().createQuery("select p from Payment p where (p.processed = :proc or p.processed is null) and p.lastUpdateDate < :lastDate" +
                " and p.account.service.provider.id = :providerId")
                .setParameter("proc", 0).setParameter("lastDate", lastUpdateDate).setParameter("providerId", Providers.UNINET.getId()).getResultList();
    }

    public List<Payment> findAllUnproccessedDataplus(DateTime lastUpdateDate) {
        return getEntityManager().createQuery("select p from Payment p where (p.processed = :proc or p.processed is null) and p.lastUpdateDate < :lastDate" +
                " and p.account.service.provider.id = :providerId")
                .setParameter("proc", 0).setParameter("lastDate", lastUpdateDate).setParameter("providerId", Providers.DATAPLUS.getId()).getResultList();
    }

    public List<Payment> findAllUnproccessed(DateTime lastUpdateDate) {
        return getEntityManager().createQuery("select p from Payment p where (p.processed = :proc or p.processed is null) and p.lastUpdateDate < :lastDate"+" and p.account.service.provider.id <> :providerId")
                .setParameter("proc", 0).setParameter("lastDate", lastUpdateDate).setParameter("providerId", Providers.DATAPLUS.getId()).getResultList();
    }

    public List<Payment> findAllByDateAndRRN(String date, String rrn) {
        return getEntityManager().createQuery("select p from Payment p where (substring(p.datetime,1 ,8) = :date and p.rrn = :rrn and p.status <> -1) ")
                .setParameter("date", date).setParameter("rrn", rrn).getResultList();
    }

    public List<Payment> findAllByAgreement(String agreement) {
        return getEntityManager().createQuery("select p from Payment p where p.contract = :agreement")
                .setParameter("agreement", agreement).getResultList();
    }

    public List<Payment> findAllByAgreement(String agreement, DateTime startDate, DateTime endDate) {
        log.debug("%%%%%%%%%%%%%%%    inside find");
        return em.createQuery("select p from Payment p "
                + "where p.contract =:agreement "
                + "and p.fd>=:start_date and p.fd<=:end_date", Payment.class)
                .setParameter("agreement", agreement)
                .setParameter("start_date", startDate.toDate())
                .setParameter("end_date", endDate.toDate())
                .getResultList();
    }

    public List<Payment> findAllByAgreementAndDates(String agreement, String startDate, String endDate) {

        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd.MM.yyyy");
        Date start_date = formatter.parseDateTime(startDate).toDate();
        Date end_date = formatter.parseDateTime(endDate).toDate();

        return getEntityManager().
                createQuery("select p from Payment p where ((p.fd is null) or (p.fd>=:startDate and p.fd<=:endDate)) and p.contract = :agreement and p.status <> -1 ")
                .setParameter("startDate", start_date).setParameter("endDate", end_date).setParameter("agreement", agreement).getResultList();
    }

    public Payment create(Payment payment, final Subscription selectedSubscription, final Double amount, final User user) {
        log.debug(String.format("Create received arguments: payment=%s, subscription=%d, amount=%f, user=%s", payment, selectedSubscription.getId(), amount, user));

        payment.setUser(user);
        payment.setServiceId(selectedSubscription.getService());
        payment.setAccount(selectedSubscription);
        payment.setContract(selectedSubscription.getAgreement());
        payment.setCurrency("AZN");
        payment.setSubscriber_id(selectedSubscription.getSubscriber().getId());
        payment.setFd(DateTime.now().toDate());
        payment.setRrn(String.valueOf(System.currentTimeMillis()));
        //String amount = (this.cents != null && !this.cents.isEmpty() && this.cents.length() == 2) ? this.manats + '.' + this.cents : this.manats;
        //String amount = manats;
        payment.setAmount(amount);
        save(payment);
        log.info("payment.getId=" + payment.getId());
        return payment;
    }

    public Payment create(final Subscription selectedSubscription, final Double amount, final long extUserID) {
        log.debug(String.format("Create received arguments: subscription=%d, amount=%f, extUuserID=%s", selectedSubscription.getId(), amount, extUserID));

        Payment payment = new Payment();

        payment.setMethod(paymentOptionsFacade.getOptionByName("CASH"));
        payment.setExtUser(externalUserFacade.find(extUserID));
        payment.setServiceId(selectedSubscription.getService());
        payment.setAccount(selectedSubscription);
        payment.setContract(selectedSubscription.getAgreement());
        payment.setCurrency("AZN");
        payment.setSubscriber_id(selectedSubscription.getSubscriber().getId());
        payment.setAmount(amount);
        payment.setFd(DateTime.now().toDate());
        save(payment);
        log.info("payment.getId=" + payment.getId());
        return payment;
    }

    public Payment create(Payment payment, final Subscription selectedSubscription, final Double amount, final long extUserID) {
        log.debug(String.format("Create received arguments: subscription=%d, amount=%f, extUuserID=%s", selectedSubscription.getId(), amount, extUserID));

        payment.setExtUser(externalUserFacade.find(extUserID));
        payment.setServiceId(selectedSubscription.getService());
        payment.setAccount(selectedSubscription);
        payment.setContract(selectedSubscription.getAgreement());
        payment.setCurrency("AZN");
        payment.setSubscriber_id(selectedSubscription.getSubscriber().getId());
        payment.setAmount(amount);
        save(payment);
        log.info("payment.getId=" + payment.getId());
        return payment;
    }

    public Payment create(final Double amount,
                          final long extUserID,
                          long serviceID,
                          long accountID,
                          String contract,
                          long subscriberID,
                          String currency,
                          String rid,
                          String rrn,
                          String sessID,
                          String dsc,
                          String dateTime
    ) {

        log.debug(String.format("Create received arguments: amount=%f, extUserID=%d, serviceID=%d, accountID=%d, contract=%s, subscriberID=%d, rid=%s, rrn=%s, sessID=%s, dsc=%s",
                amount, extUserID, serviceID, accountID, contract, subscriberID, rid, rrn, sessID, dsc));
        Subscription subscription = subscriptionFacade.find(accountID);

        Payment payment = new Payment();
        payment.setExtUser(externalUserFacade.find(extUserID));
        payment.setServiceId(serviceFacade.find(serviceID));
        payment.setAccount(subscription);
        payment.setContract(subscription.getAgreement());
        payment.setCurrency(currency);
        payment.setSubscriber_id(subscription.getSubscriber().getId());
        payment.setRid(rid);
        payment.setRrn(rrn);
        payment.setSessid(sessID);
        payment.setDsc(dsc);
        payment.setDateTime(dateTime);
        payment.setFd(DateTime.now().toDate());
        //String amount = (this.cents != null && !this.cents.isEmpty() && this.cents.length() == 2) ? this.manats + '.' + this.cents : this.manats;
        //String amount = manats;
        payment.setAmount(amount);
        payment.setMethod(paymentOptionsFacade.getOptionByName("BANK"));
        save(payment);
        log.info("payment.getId=" + payment.getId());
        return payment;
    }

    public void cancelPayment(long id) throws Exception {
        String message = null;
        Transaction transaction = null;

        final Payment payment = find(id);

        if (payment == null || payment.getStatus() == -1) {
            throw new IllegalArgumentException(String.format("Payment id=%d not found"));
        }

        long amount = payment.getAmountAsLong();
        log.info("Starting payment cancellation procedure for payment id=" + id + ": " + payment);

        try {
            transaction = transactionFacade.createTransation(
                    TransactionType.DEBIT,
                    payment.getAccount(),
                    amount,
                    String.format("Payment id=%d is voided", payment.getId()));

            message = String.format("Payment %d voided with transaction %d", payment.getId(), transaction.getId());
            log.info(message);
            systemLogger.success(SystemEvent.PAYMENT_VOIDED, payment.getAccount(), transaction, message);
            Subscription subscription = payment.getAccount();

            if (subscription.getSubscriber().getLifeCycle() == SubscriberLifeCycleType.PREPAID
                    && subscription.getBalance().getRealBalance() < 0) { //block subscription
                if (subscription.getBillingModel().getGracePeriodInDays() == 0) {
                    log.info(String.format("Subscription %s - status changing to blocked because of insufficient funds", subscription));
                    subscription.setStatus(SubscriptionStatus.BLOCKED);
                    boolean provisioningResult = provisioningFactory.getProvisioningEngine(subscription).closeService(subscription);

                    if (provisioningResult) {
                        subscriptionFacade.shiftBackExpirationDate(subscription);
                        subscription.setLastStatusChangeDate(DateTime.now());
                        message = "Subscription status changed to block for subscription id=" + subscription.getId() + " after cancelling payment id=" + payment.getId();
                        log.info(message);
                        systemLogger.success(SystemEvent.SUBSCRIPTION_STATUS_BLOCK, subscription, message);
                    } else {
                        message = String.format("Subscription status for id=%d cannot be changed: provisioning result=%b", subscription.getId(), provisioningResult);
                        log.error(message);
                        systemLogger.error(SystemEvent.SUBSCRIPTION_STATUS_BLOCK, subscription, message);
                    }
                } else {
                    subscription.setExpirationDate(subscription.getExpirationDate().minusMonths(1));
                    subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDateWithGracePeriod().
                            minusMonths(1));
                    if (subscription.getExpirationDateWithGracePeriod().isBefore(DateTime.now())) {
                        subscription.setStatus(SubscriptionStatus.BLOCKED);
                        boolean provisioningResult = provisioningFactory.getProvisioningEngine(subscription).closeService(subscription);
                        if (provisioningResult) {
                            subscription.setLastStatusChangeDate(DateTime.now());
                            message = "Subscription status changed to block for subscription(GRACE) id=" + subscription.getId() + " after cancelling payment id=" + payment.getId();
                            log.info(message);
                            systemLogger.success(SystemEvent.SUBSCRIPTION_STATUS_BLOCK, subscription, message);
                        } else {
                            subscription.setExpirationDate(subscription.getExpirationDate().plusMonths(1));
                            subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDateWithGracePeriod().
                                    plusMonths(1));
                            message = String.format("Subscription status for id=%d cannot be changed , GRACE: provisioning result=%b", subscription.getId(), provisioningResult);
                            log.error(message);
                            systemLogger.error(SystemEvent.SUBSCRIPTION_STATUS_BLOCK, subscription, message);
                        }
                    } else if (subscription.getBalance().getRealBalance() < 0) {
                        message = " Subscription goes to PARTIALLY_BLOCKED , SUBSCRIPTION = " + subscription.getId() +
                                " , " + subscription.getBalance().getRealBalance() + " , GraceEXP = " + subscription.getExpirationDateWithGracePeriod();
                        subscription.setStatus(SubscriptionStatus.PARTIALLY_BLOCKED);
                        systemLogger.success(SystemEvent.SUBSCRIPTION_STATUS_PARTIALLY_BLOCKED, subscription, message);
                    }
                }
            }

            //recalculate inoices that used the payment
            List<Invoice> invoices = invoiceFacade.findInvoicesByPayment(subscription, payment);
            log.info(String.format("Found invoice - total: %d. %s", invoices.size(), invoices));
            List<Invoice> openInvoiceList = new ArrayList<>();

            if (invoices != null && !invoices.isEmpty()) {
                InvoiceState oldState = null;
                for (Invoice invoice : invoices) {
                    if (invoice.getState() == InvoiceState.OPEN) {//if invoice is open just cancel the payment
                        invoice.cancelPayment(amount);
                        break;
                    }

                    oldState = invoice.getState();

                    if (invoice.getState() == InvoiceState.CLOSED) {
                        amount = invoice.cancelPaymentWithResidual(payment);
                    }

                    if (invoice.getState() == InvoiceState.OPEN && !oldState.equals(invoice.getState())) {
                        openInvoiceList.add(invoice);//add invoice to list of open invoices
                        message = String.format("Invoice state changed from CLOSED to OPEN: id=%d, state=%s, sumCharged=%d, sumPaid=%d, balance=%d",
                                invoice.getId(), invoice.getState(), invoice.getSumCharged(), invoice.getSumPaid(), invoice.getBalance());
                        log.info(message);
                        systemLogger.success(SystemEvent.INVOICE_STATE_CHANGED, subscription, message);
                    }
                }
            }

            if (payment.getCampaignId() != null) {
                Campaign campaign = campaignPersistenceFacade.find(payment.getCampaignId());
                if (campaign != null) {
                    List<CampaignRegister> registers =
                            campaignRegisterPersistenceFacade.findNotProcessedBySubscriptionAndCampaign(subscription, campaign);
                    if (registers != null && !registers.isEmpty()) {
                        log.info(String.format(
                                "deleting register id=%d from subscription id=%d",
                                registers.get(0).getId(),
                                registers.get(0).getSubscription().getId()));
                        campaignRegisterPersistenceFacade.deleteRegister(registers.get(0));
                    }
                }
            }

            payment.setStatus(-1);

            List<Payment> unappliedPaymentList = findAllNotOnInvoices(payment.getSubscriber_id(), payment.getId());

            if (unappliedPaymentList != null && !unappliedPaymentList.isEmpty()) {
                log.info(String.format("Found unapplied payments: total=%d, list=%s", unappliedPaymentList.size(), unappliedPaymentList));
                long residual = 0;

                InvoiceState oldState = null;

                for (Payment pmt : unappliedPaymentList) {
                    for (Invoice inv : openInvoiceList) {
                        if (inv.getState() == InvoiceState.CLOSED) //skip already closed invoices
                        {
                            continue;
                        }

                        oldState = inv.getState();

                        residual = inv.addPaymentToList(pmt, residual);

                        if (inv.getState() == InvoiceState.CLOSED && !oldState.equals(inv.getState())) {
                            //openInvoiceList.add(inv);//add invoice to list of open invoices
                            message = String.format("Invoice state changed from OPEN to CLOSED: id=%d, state=%s, sumCharged=%d, sumPaid=%d, balance=%d",
                                    inv.getId(), inv.getState(), inv.getSumCharged(), inv.getSumPaid(), inv.getBalance());
                            log.info(message);
                            systemLogger.success(SystemEvent.INVOICE_STATE_CHANGED, subscription, message);
                        }

                        if (residual <= 0) {
                            break;
                        }
                    }
                }
            }

            log.info("Ending payment cancellation procedure for payment id= " + id + " with SUCCESS");
        } catch (Exception ex) {
            message = String.format("Cannot void payment payment %d with transaction %d", payment.getId(), transaction != null ? transaction.getId() : -1);
            log.error(message, ex);
            systemLogger.error(SystemEvent.PAYMENT_VOIDED, payment.getAccount(), transaction, message + " " + ex.getCause().getMessage());
            log.error("Ending payment cancellation procedure for payment id= " + id + " with ERROR");
            throw new Exception();
        }

    }

    public List<Payment> findAllNotOnInvoices(long subscriberID, long paymentID) {
        List<Long> paymentsInInvoice = em.createQuery("select pmt.id from Invoice inv  join inv.payments pmt where inv.subscriber.id = :sub_id", Long.class)
                .setParameter("sub_id", subscriberID)
                .getResultList();
        if (paymentsInInvoice == null) {
            paymentsInInvoice = new ArrayList<>();
        }
        if (paymentsInInvoice.isEmpty()) {
            paymentsInInvoice.add(-1L);
        }
        log.debug(String.format("paymentsInInvoice = %s", paymentsInInvoice));
        return em.createQuery(
                "select p from Payment p where p.subscriber_id = :sub_id and p.status <> -1 and p.id > :pid and p.id not in :inInvoice",
                Payment.class)
                .setParameter("sub_id", subscriberID)
                .setParameter("pid", paymentID)
                .setParameter("inInvoice", paymentsInInvoice)
                .getResultList();
    }

    public boolean isFirstPayment(Subscription subscription, long paymentID) {
        try {
            long paymentIDFromDB = getEntityManager().createQuery("select MIN(p.id) from Payment p where p.contract = :agr", Long.class)
                    .setParameter("agr", subscription.getAgreement()).getSingleResult();
            return paymentIDFromDB == paymentID;
        } catch (Exception ex) {
            log.error(ex);
            return false;
        }
    }


    public List<Payment> findAllByDates(Date startDate, Date endDate) {

        return getEntityManager().
                createQuery("select p from Payment p where ((p.fd is null) or (p.fd>=:startDate and p.fd<=:endDate)) and p.status <> -1 ")
                .setParameter("startDate", startDate).setParameter("endDate", endDate).getResultList();
    }
    /*@Override
     public void save (Payment pm) {
     //pm.setFd(DateTime.now().toDate());
     //Logger log = LoggerFactory.getLogger(this.getClass());
     //log.debug("DATE="+DateTime.now().toDate());
     super.save(pm);
     }*/
 /*@Override
     public List<Payment> findAllPaginated(int first, int pageSize) {
     //  List<Payment> payments = em.createQuery("select c from Payment c join c.account sub join sub.subscriber sbr where sbr.id = :sub_id order by c.fd desc, c.lastUpdateDate desc", Payment.class)
     List<Payment> payments = em.createQuery("select c from Payment c where c.subscriber_id = :sub_id or c.id in (select c.id from Payment c join c.account sub join sub.subscriber sbr where sbr.id = :sub_id) order by c.fd desc, c.lastUpdateDate desc", Payment.class)
     .setParameter("sub_id", getSubscriberID())
     .setFirstResult(first)
     .setMaxResults(pageSize)
     .getResultList();

     return payments;
     }
     */
 /* @Override
     public long count() {
     return em.createQuery("select count(c) from Payment c where c.subscriber_id = :sub_id or c.id in (select c.id from Payment c join c.account sub join sub.subscriber sbr where sbr.id = :sub_id) order by c.fd desc, c.lastUpdateDate desc", Long.class)
     .setParameter("sub_id", getSubscriberID())
     .getSingleResult();
     }*/
}
