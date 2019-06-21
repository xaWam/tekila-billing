package spring.service.finance;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.module.accounting.entity.AccountingCategory;
import com.jaravir.tekila.module.accounting.entity.AccountingTransaction;
import com.jaravir.tekila.module.accounting.entity.Operation;
import com.jaravir.tekila.module.accounting.entity.TransactionType;
import com.jaravir.tekila.module.accounting.manager.AccountingCategoryPersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.AccountingTransactionPersistenceFacade;
import com.jaravir.tekila.module.service.model.BillingPrinciple;
import com.jaravir.tekila.module.service.persistence.manager.ServiceProviderPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberLifeCycleType;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriberPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import spring.exceptions.AdjustmentException;
import spring.security.SecurityModuleUtils;

import static spring.util.Constants.INJECTION_POINT;

import javax.ejb.EJB;
import java.util.Arrays;

/**
 * @author ElmarMa on 3/15/2018
 */

@Service
public class RealBalanceAdjustmentService implements PaymentAdjuster {

    private final Logger log = LoggerFactory.getLogger(RealBalanceAdjustmentService.class);


    @EJB(mappedName = INJECTION_POINT + "AccountingTransactionPersistenceFacade")
    private AccountingTransactionPersistenceFacade accountingTransactionpf;

    @EJB(mappedName = INJECTION_POINT + "AccountingCategoryPersistenceFacade")
    private AccountingCategoryPersistenceFacade accountingCategorypf;

    @EJB(mappedName = INJECTION_POINT + "SubscriptionPersistenceFacade")
    private SubscriptionPersistenceFacade subscriptionpf;

    @EJB(mappedName = INJECTION_POINT + "ServiceProviderPersistenceFacade")
    private ServiceProviderPersistenceFacade serviceProviderpf;

    @EJB(mappedName = INJECTION_POINT + "SubscriberPersistenceFacade")
    private SubscriberPersistenceFacade subscriberpf;

    @EJB(mappedName = INJECTION_POINT + "UserPersistenceFacade")
    private UserPersistenceFacade userpf;

    @Override
    public void doCreditAdjustment(Subscription subscription, AccountingCategory accountingCategory, Long amount, String description) {
        try {
            String currentWorkingUserOnThreadLocal = SecurityModuleUtils.getCurrentUserLogin();

            User user = userpf.findByUserName(currentWorkingUserOnThreadLocal);
            Operation operation = new Operation();
            operation.setSubscription(subscription);
            operation.setUser(user);
            operation.setAmount(amount);
            operation.setDsc(description);
            operation.setCategory(accountingCategory);
            AccountingTransaction accountingTransaction = accountingTransactionpf.createFromForm(operation, TransactionType.CREDIT, null);

            if (subscription.getSubscriber().getLifeCycle() == SubscriberLifeCycleType.PREPAID
                    && subscription.getStatus() == SubscriptionStatus.BLOCKED && subscription.getBalance().getRealBalance() >= 0
                    && (subscription.getBillingModel() == null || subscription.getBillingModel().getPrinciple() != BillingPrinciple.GRACE)) {

                subscriptionpf.activatePrepaidOnAdjustment(subscription.getId(), accountingTransaction.getId(),
                        String.format("accounting trasaction id=%d", accountingTransaction.getId()));

            }

        } catch (Exception e) {
            log.error(e.getMessage(),Arrays.toString(e.getStackTrace()));
            throw new AdjustmentException("can not do credit adjustment", e);
        }
    }

    @Override
    public void doDebtAdjustment(Subscription subscription, AccountingCategory accountingCategory, Long amount, String description) {
        try {
            String currentWorkingUserOnThreadLocal = SecurityModuleUtils.getCurrentUserLogin();

            User user = userpf.findByUserName(currentWorkingUserOnThreadLocal);
            Operation operation = new Operation();
            operation.setSubscription(subscription);
            operation.setUser(user);
            operation.setAmount(amount);
            operation.setDsc(description);
            operation.setCategory(accountingCategory);
            accountingTransactionpf.createFromForm(operation, TransactionType.DEBIT, null);
        } catch (Exception e) {
            log.error(e.getMessage(),Arrays.toString(e.getStackTrace()));
            throw new AdjustmentException("can not do debt adjustment", e);
        }
    }
}
