/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jaravir.tekila.module.accounting.manager;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.accounting.AccountingCategoryType;
import com.jaravir.tekila.module.accounting.AccountingTransactionType;
import com.jaravir.tekila.module.accounting.entity.*;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * @author sajabrayilov
 */
@Stateless
public class AccountingTransactionPersistenceFacade extends AbstractPersistenceFacade<AccountingTransaction> {
    @PersistenceContext
    private EntityManager em;
    @Resource
    private EJBContext ctx;
    @EJB
    private AccountingCategoryPersistenceFacade accCatFacade;
    @EJB
    private TransactionPersistenceFacade transFacade;
    @EJB
    private SubscriptionPersistenceFacade subscriptionFacade;
    @EJB
    private RefundPersistenceFacade refundFacade;
    @EJB
    private PaymentPersistenceFacade paymentFacade;
    @EJB
    private SystemLogger systemLogger;

    private static final Logger log = Logger.getLogger(AccountingTransactionPersistenceFacade.class);

    public AccountingTransactionPersistenceFacade() {
        super(AccountingTransaction.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }


    public enum Filter implements Filterable {

        ID("id"),
        USER("user.userName"),
        TYPE("type"),
        DESCRIPTION("dsc");

        private final String field;
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

        public void setOperation(MatchingOperation operation) {
            this.operation = operation;
        }
    }


    public AccountingTransaction createFromForm(Operation operation, TransactionType type, Long accCatID) throws Exception {
        try {
            if (accCatID != null) {
                AccountingCategory accountingCategory = null;
                accountingCategory = accCatFacade.find(accCatID);
                operation.setCategory(accountingCategory);
            }

            AccountingTransaction accTransaction = new AccountingTransaction();
            accTransaction.setType(AccountingTransactionType.ADJUSTMENT);
            String dsc = String.format("Adjustment by %s", operation.getUser().getUserName());
            if (operation.getDsc() != null) {
                dsc = dsc + String.format(": %s", (operation.getDsc() != null ? operation.getDsc() : ""));
            }
            Transaction trans = transFacade.createTransation(type, operation.getSubscription(), operation.getAmount(), dsc);

            operation.setTransaction(trans);
            operation.setProvider(operation.getSubscription().getService().getProvider());
            operation.setAccTransaction(accTransaction);

            accTransaction.addOperation(operation);
            accTransaction.setUser(operation.getUser());
            accTransaction.setProvider(operation.getProvider());
            accTransaction.setDsc(operation.getDsc());
            save(accTransaction);

            operation.setSubscription(subscriptionFacade.update(operation.getSubscription()));

            return accTransaction;
        } catch (Exception ex) {
            ctx.getRollbackOnly();
            String msg = String.format("Cannot create AccountingTransaction: Operation=%s, TransactionType=%s, accCatID=%d", operation, type, accCatID);
            log.error(msg, ex);
            throw new Exception(msg, ex);
        }
    }

    public AccountingTransaction createFromFormPromo(Operation operation, TransactionType type, Long accCatID) throws Exception {
        try {
            if (accCatID != null) {
                AccountingCategory accountingCategory = null;
                accountingCategory = accCatFacade.find(accCatID);
                operation.setCategory(accountingCategory);
            }

            AccountingTransaction accTransaction = new AccountingTransaction();
            accTransaction.setType(AccountingTransactionType.PROMO_ADJUSTMENT);
            String dsc = String.format("Promo adjustment by %s", operation.getUser().getUserName());
            if (operation.getDsc() != null) {
                dsc = dsc + String.format(": %s", (operation.getDsc() != null ? operation.getDsc() : ""));
            }
            Transaction trans = transFacade.createTransationForPromo(type,
                    operation.getSubscription(),
                    operation.getAmount(),
                    dsc);

            operation.setTransaction(trans);
            operation.setProvider(operation.getSubscription().getService().getProvider());
            operation.setAccTransaction(accTransaction);

            accTransaction.addOperation(operation);
            accTransaction.setUser(operation.getUser());
            accTransaction.setProvider(operation.getProvider());
            accTransaction.setDsc(operation.getDsc());
            save(accTransaction);

            subscriptionFacade.update(operation.getSubscription());

            return accTransaction;
        } catch (Exception ex) {
            ctx.getRollbackOnly();
            String msg = String.format("Cannot create AccountingTransaction: Operation=%s, TransactionType=%s, accCatID=%d", operation, type, accCatID);
            log.error(msg, ex);
            throw new Exception(msg, ex);
        }
    }

    public AccountingTransaction transferPayment(Subscription target, Subscription source, Payment payment, User user) throws Exception {
        try {

            log.debug("Start AccountingTransaction. Source: " + source.getAgreement() + " Target: " + target.getAgreement());

            AccountingTransaction accTransaction = new AccountingTransaction();
            accTransaction.setType(AccountingTransactionType.PAYMENT_TRANSFER);
            //Transaction trans = transFacade.createTransation(TransactionType.DEBIT, fromOperation.getSubscription(), fromOperation.getAmount(), String.format("Adjustment by %s", fromOperation.getUser().getUserName()));

            Transaction fromTransaction = transFacade.createTransation(
                    TransactionType.DEBIT,
                    source, payment.getAmountAsLong(),
                    String.format("Payment id=%d transfer to subscription id=%d", payment.getId(), target.getId()));
            subscriptionFacade.update(source);

            Operation fromOperation = new Operation();
            fromOperation.setSubscription(source);
            fromOperation.setAmount(payment.getAmountAsLong());
            fromOperation.setUser(user);
            fromOperation.setCategory(accCatFacade.findByType(AccountingCategoryType.PAYMENT_ADJUSTMENT));
            fromOperation.setTransaction(fromTransaction);
            fromOperation.setProvider(fromOperation.getSubscription().getService().getProvider());
            fromOperation.setAccTransaction(accTransaction);

            Transaction toTransaction = transFacade.createTransation(
                    TransactionType.PAYMENT,
                    target, payment.getAmountAsLong(),
                    String.format("Payment id=%d transfer from subscription id=%d", payment.getId(), payment.getAccount().getId())
            );
            subscriptionFacade.update(target);

            Operation toperation = new Operation();
            toperation.setSubscription(target);
            toperation.setAmount(payment.getAmountAsLong());
            toperation.setUser(user);
            toperation.setCategory(accCatFacade.findByType(AccountingCategoryType.PAYMENT_ADJUSTMENT));
            toperation.setTransaction(toTransaction);
            toperation.setProvider(toTransaction.getSubscription().getService().getProvider());
            toperation.setAccTransaction(accTransaction);

            accTransaction.addOperation(fromOperation);
            accTransaction.addOperation(toperation);
            accTransaction.setUser(fromOperation.getUser());
            accTransaction.setProvider(fromOperation.getProvider());

            save(accTransaction);

//            subscriptionFacade.update(fromOperation.getSubscription());
//            subscriptionFacade.update(toperation.getSubscription());

            return accTransaction;
        } catch (Exception ex) {
            ctx.getRollbackOnly();
            String msg = String.format("Cannot create AccountingTransaction: ");
            log.error(msg, ex);
            throw new Exception(msg, ex);
        }
    }

    public AccountingTransaction transferBalance(Subscription target, Subscription source, User user) throws Exception {
        try {

            AccountingTransaction accTransaction = new AccountingTransaction();
            accTransaction.setType(AccountingTransactionType.BALANCE_TRANSFER);
            //Transaction trans = transFacade.createTransation(TransactionType.DEBIT, fromOperation.getSubscription(), fromOperation.getAmount(), String.format("Adjustment by %s", fromOperation.getUser().getUserName()));
            long amount = source.getBalance().getRealBalance();
            Transaction fromTransaction = transFacade.createTransation(
                    TransactionType.DEBIT,
                    source, amount,
                    String.format("Balance transfer amount=%d from subscription id=%d to subscription id=%d", amount, source.getId(), target.getId()));

            Operation fromOperation = new Operation();
            fromOperation.setSubscription(source);
            fromOperation.setAmount(source.getBalance().getRealBalance());
            fromOperation.setUser(user);
            fromOperation.setCategory(accCatFacade.findByType(AccountingCategoryType.BALANCE_TRANSFER));
            fromOperation.setTransaction(fromTransaction);
            fromOperation.setProvider(fromOperation.getSubscription().getService().getProvider());
            fromOperation.setAccTransaction(accTransaction);

            Transaction toTransaction = transFacade.createTransation(
                    TransactionType.CREDIT,
                    target, amount,
                    String.format("Balance transfer amount=%d to subscription id=%d from subscription id=%d", amount, target.getId(), source.getId())
            );

            Operation toperation = new Operation();
            toperation.setSubscription(target);
            toperation.setAmount(source.getBalance().getRealBalance());
            toperation.setUser(user);
            toperation.setCategory(accCatFacade.findByType(AccountingCategoryType.SPECIAL));
            toperation.setTransaction(toTransaction);
            toperation.setProvider(toTransaction.getSubscription().getService().getProvider());
            toperation.setAccTransaction(accTransaction);

            accTransaction.addOperation(fromOperation);
            accTransaction.addOperation(toperation);
            accTransaction.setUser(fromOperation.getUser());
            accTransaction.setProvider(fromOperation.getProvider());

            save(accTransaction);

            subscriptionFacade.update(fromOperation.getSubscription());
            subscriptionFacade.update(toperation.getSubscription());

            return accTransaction;
        } catch (Exception ex) {
            ctx.getRollbackOnly();
            String msg = String.format("Cannot create AccountingTransaction: ");
            log.error(msg, ex);
            throw new Exception(msg, ex);
        }
    }

    public AccountingTransaction transferBalance(Subscription target, Subscription source, long amount, User user) throws Exception {
        try {

            if (amount > source.getBalance().getRealBalance()) {
                String msg = String.format("Subscription does not have enough balance!");
                log.error(msg);
                throw new Exception(msg);
            }

            AccountingTransaction accTransaction = new AccountingTransaction();
            accTransaction.setType(AccountingTransactionType.BALANCE_TRANSFER);
            //Transaction trans = transFacade.createTransation(TransactionType.DEBIT, fromOperation.getSubscription(), fromOperation.getAmount(), String.format("Adjustment by %s", fromOperation.getUser().getUserName()));
            Transaction fromTransaction = transFacade.createTransation(
                    TransactionType.DEBIT,
                    source, amount,
                    String.format("Balance transfer amount=%d from subscription id=%d to subscription id=%d", amount, source.getId(), target.getId()));

            Operation fromOperation = new Operation();
            fromOperation.setSubscription(source);
            fromOperation.setAmount(amount);
            fromOperation.setUser(user);
            fromOperation.setCategory(accCatFacade.findByType(AccountingCategoryType.BALANCE_TRANSFER));
            fromOperation.setTransaction(fromTransaction);
            fromOperation.setProvider(fromOperation.getSubscription().getService().getProvider());
            fromOperation.setAccTransaction(accTransaction);

            Transaction toTransaction = transFacade.createTransation(
                    TransactionType.CREDIT,
                    target, amount,
                    String.format("Balance transfer amount=%d to subscription id=%d from subscription id=%d", amount, target.getId(), source.getId())
            );

            Operation toperation = new Operation();
            toperation.setSubscription(target);
            toperation.setAmount(amount);
            toperation.setUser(user);
            toperation.setCategory(accCatFacade.findByType(AccountingCategoryType.SPECIAL));
            toperation.setTransaction(toTransaction);
            toperation.setProvider(toTransaction.getSubscription().getService().getProvider());
            toperation.setAccTransaction(accTransaction);

            accTransaction.addOperation(fromOperation);
            accTransaction.addOperation(toperation);
            accTransaction.setUser(fromOperation.getUser());
            accTransaction.setProvider(fromOperation.getProvider());

            save(accTransaction);

            subscriptionFacade.update(fromOperation.getSubscription());
            subscriptionFacade.update(toperation.getSubscription());

            return accTransaction;
        } catch (Exception ex) {
            ctx.getRollbackOnly();
            String msg = String.format("Cannot create AccountingTransaction: ");
            log.error(msg, ex);
            throw new Exception(msg, ex);
        }
    }

    public AccountingTransaction settlePayment(Subscription target, Subscription source, Payment payment, User user) throws Exception {
        try {

            AccountingTransaction accTransaction = new AccountingTransaction();
            accTransaction.setType(AccountingTransactionType.PAYMENT_TRANSFER);
            //Transaction trans = transFacade.createTransation(TransactionType.DEBIT, fromOperation.getSubscription(), fromOperation.getAmount(), String.format("Adjustment by %s", fromOperation.getUser().getUserName()));

            Transaction fromTransaction = transFacade.createTransation(
                    TransactionType.DEBIT,
                    payment.getAccount(), payment.getAmountAsLong(),
                    String.format("Payment id=%d transfer to subscription id=%d", payment.getId(), target.getId()));

            Operation fromOperation = new Operation();
            fromOperation.setSubscription(source);
            fromOperation.setAmount(payment.getAmountAsLong());
            fromOperation.setUser(user);
            fromOperation.setCategory(accCatFacade.findByType(AccountingCategoryType.PAYMENT_ADJUSTMENT));
            fromOperation.setTransaction(fromTransaction);
            fromOperation.setProvider(fromOperation.getSubscription().getService().getProvider());
            fromOperation.setAccTransaction(accTransaction);

            Transaction toTransaction = transFacade.createTransation(
                    TransactionType.PAYMENT,
                    target, payment.getAmountAsLong(),
                    String.format("Payment id=%d transfer from subscription id=%d", payment.getId(), payment.getAccount().getId())
            );

            Operation toperation = new Operation();
            toperation.setSubscription(target);
            toperation.setAmount(payment.getAmountAsLong());
            toperation.setUser(user);
            toperation.setCategory(accCatFacade.findByType(AccountingCategoryType.PAYMENT_ADJUSTMENT));
            toperation.setTransaction(toTransaction);
            toperation.setProvider(toTransaction.getSubscription().getService().getProvider());
            toperation.setAccTransaction(accTransaction);

            accTransaction.addOperation(fromOperation);
            accTransaction.addOperation(toperation);
            accTransaction.setUser(fromOperation.getUser());
            accTransaction.setProvider(fromOperation.getProvider());

            save(accTransaction);

            subscriptionFacade.update(fromOperation.getSubscription());
            subscriptionFacade.update(toperation.getSubscription());

            return accTransaction;
        } catch (Exception ex) {
            ctx.getRollbackOnly();
            String msg = String.format("Cannot create AccountingTransaction: ");
            log.error(msg, ex);
            throw new Exception(msg, ex);
        }
    }

    public AccountingTransaction refund(Refund refund, long amount, Subscription subscription, User user) throws Exception {
        try {
            refund.setAmount(amount);
            refund.setSubscriber(subscription.getSubscriber());
            refund.setService(subscription.getService());
            refund.setDatetime(DateTime.now());
            refund.setUser_id(user.getId());

            AccountingTransaction accTransaction = new AccountingTransaction();
            accTransaction.setType(AccountingTransactionType.ADJUSTMENT);
            //Transaction trans = transFacade.createTransation(TransactionType.DEBIT, fromOperation.getSubscription(), fromOperation.getAmount(), String.format("Adjustment by %s", fromOperation.getUser().getUserName()));
            Transaction fromTransaction = transFacade.createTransation(
                    TransactionType.DEBIT,
                    subscription, amount,
                    String.format("Refund amount=%d to subscription id=%d", amount, subscription.getId()));

            Operation fromOperation = new Operation();
            fromOperation.setSubscription(subscription);
            fromOperation.setAmount(subscription.getBalance().getRealBalance());
            fromOperation.setUser(user);
            fromOperation.setCategory(accCatFacade.findByType(AccountingCategoryType.BALANCE_REFUND));
            fromOperation.setTransaction(fromTransaction);
            fromOperation.setProvider(fromOperation.getSubscription().getService().getProvider());
            fromOperation.setAccTransaction(accTransaction);

            accTransaction.addOperation(fromOperation);
            accTransaction.setUser(fromOperation.getUser());
            accTransaction.setProvider(fromOperation.getProvider());

            save(accTransaction);

            refundFacade.save(refund);

            subscriptionFacade.update(fromOperation.getSubscription());

            return accTransaction;
        } catch (Exception ex) {
            ctx.getRollbackOnly();
            String msg = String.format("Cannot create AccountingTransaction: ");
            log.error(msg, ex);
            throw new Exception(msg, ex);
        }
    }

    public AccountingTransaction makePayment(Payment payment, Subscription subscription, Long amount, User user) throws Exception {
        log.debug("makePayment process starts for Subscription "+subscription.getId());
        try {
            AccountingTransaction accTransaction = new AccountingTransaction();
            accTransaction.setType(AccountingTransactionType.PAYMENT);
            //Transaction trans = transFacade.createTransation(TransactionType.DEBIT, fromOperation.getSubscription(), fromOperation.getAmount(), String.format("Adjustment by %s", fromOperation.getUser().getUserName()));
            Transaction fromTransaction = transFacade.createTransation(
                    TransactionType.PAYMENT,
                    subscription, amount,
                    String.format("Payment of %s AZN for Subscription %s of Subscriber %s", payment.getAmount(), subscription.getAgreement(), subscription.getSubscriber().getMasterAccount()));
            log.info(String.format("Transaction id = %s, start balance=%s, end balance=%s, real balance=%s for Subscription %s",
                                                                                                fromTransaction.getId(),
                                                                                                fromTransaction.getStartBalance(),
                                                                                                fromTransaction.getEndBalance(),
                                                                                                subscription.getBalance().getRealBalance(),
                                                                                                subscription.getId()));
            payment.setTransaction(fromTransaction);

            Operation fromOperation = new Operation();
            fromOperation.setSubscription(subscription);
            fromOperation.setAmount(subscription.getBalance().getRealBalance());
            fromOperation.setUser(user);
            fromOperation.setCategory(accCatFacade.findByType(AccountingCategoryType.PAYMENT));
            fromOperation.setTransaction(fromTransaction);
            fromOperation.setProvider(fromOperation.getSubscription().getService().getProvider());
            fromOperation.setAccTransaction(accTransaction);

            accTransaction.addOperation(fromOperation);
            accTransaction.setUser(fromOperation.getUser());
            accTransaction.setProvider(fromOperation.getProvider());

            save(accTransaction);

            subscription.setLastPaymentDate(DateTime.now());
            subscription.getSubscriber().setLastPaymentDate(DateTime.now());
            subscriptionFacade.update(fromOperation.getSubscription());

            payment.setProcessed(1);

            systemLogger.success(SystemEvent.PAYMENT_PROCCESSED, subscription, fromTransaction,
                    String.format(
                            "transaction id=%d, payment id=%d, amount=%f, accounting tranaction id=%d",
                            fromTransaction.getId(),
                            payment.getId(),
                            payment.getAmount(),
                            accTransaction.getId()
                    ));

            return accTransaction;
        } catch (Exception ex) {
            ctx.getRollbackOnly();
            String msg = String.format("Cannot create AccountingTransaction for subscription: %s", subscription.getId());
            log.error(msg, ex);
            systemLogger.error(SystemEvent.PAYMENT_PROCCESSED, subscription,
                    String.format("payment id=%d, amount=%f", payment.getId(), payment.getAmount())
            );
            throw new Exception(msg, ex);
        }
    }



    public AccountingTransaction makePaymentExP(Payment payment, Subscription subscription, Long amount, User user) throws Exception {
        try {
            AccountingTransaction accTransaction = new AccountingTransaction();
            accTransaction.setType(AccountingTransactionType.PAYMENT);
            //Transaction trans = transFacade.createTransation(TransactionType.DEBIT, fromOperation.getSubscription(), fromOperation.getAmount(), String.format("Adjustment by %s", fromOperation.getUser().getUserName()));
            Transaction fromTransaction = transFacade.createTransation(
                    TransactionType.PAYMENT,
                    subscription, amount,
                    String.format("Payment of %f AZN for Subscription %s of Subscriber %s",
                            payment.getAmount(), subscription.getAgreement(), subscription.getSubscriber().getMasterAccount()));

            log.info(String.format("Transaction id = %d, after balance = %d",
                    fromTransaction.getId(), subscription.getBalance().getRealBalance()));
            payment.setTransaction(fromTransaction);

            Operation fromOperation = new Operation();
            fromOperation.setSubscription(subscription);
            fromOperation.setAmount(subscription.getBalance().getRealBalance());
            fromOperation.setUser(user);
            fromOperation.setCategory(accCatFacade.findByType(AccountingCategoryType.PAYMENT));
            fromOperation.setTransaction(fromTransaction);
            fromOperation.setProvider(fromOperation.getSubscription().getService().getProvider());
            fromOperation.setAccTransaction(accTransaction);

            accTransaction.addOperation(fromOperation);
            accTransaction.setUser(fromOperation.getUser());
            accTransaction.setProvider(fromOperation.getProvider());

            save(accTransaction);

            subscription.setLastPaymentDate(DateTime.now());
            subscription.getSubscriber().setLastPaymentDate(DateTime.now());
            subscriptionFacade.update(fromOperation.getSubscription());

            payment.setProcessed(1);

            systemLogger.successExP(SystemEvent.PAYMENT_PROCCESSED, subscription, fromTransaction,
                    String.format(
                            "transaction id=%d, payment id=%d, amount=%f, accounting tranaction id=%d",
                            fromTransaction.getId(),
                            payment.getId(),
                            payment.getAmount(),
                            accTransaction.getId()
                    ), user.getUserName());

            return accTransaction;
        } catch (Exception ex) {
            ctx.getRollbackOnly();
            String msg = String.format("Cannot create AccountingTransaction: ");
            log.error(msg, ex);
            systemLogger.errorExP(SystemEvent.PAYMENT_PROCCESSED, subscription,
                    String.format("payment id=%d, amount=%f", payment.getId(), payment.getAmount()), user.getUserName()
            );
            throw new Exception(msg, ex);
        }
    }
}
