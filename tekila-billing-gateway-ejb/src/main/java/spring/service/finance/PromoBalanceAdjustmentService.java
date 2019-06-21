package spring.service.finance;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.module.accounting.entity.AccountingCategory;
import com.jaravir.tekila.module.accounting.entity.Operation;
import com.jaravir.tekila.module.accounting.entity.TransactionType;
import com.jaravir.tekila.module.accounting.manager.AccountingCategoryPersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.AccountingTransactionPersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.ServiceProviderPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriberPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import spring.exceptions.AdjustmentException;
import spring.security.SecurityModuleUtils;

import javax.ejb.EJB;

import java.util.Arrays;

import static spring.util.Constants.INJECTION_POINT;

/**
 * @author ElmarMa on 3/15/2018
 */

@Service
public class PromoBalanceAdjustmentService implements PaymentAdjuster {
    private final Logger log = LoggerFactory.getLogger(PromoBalanceAdjustmentService.class);

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
            accountingTransactionpf.createFromFormPromo(operation, TransactionType.CREDIT_PROMO, null);
        }catch (Exception e){
            log.error(e.getMessage(), Arrays.toString(e.getStackTrace()));
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
            accountingTransactionpf.createFromFormPromo(operation, TransactionType.DEBIT_PROMO, null);
        }catch (Exception e){
            log.error(e.getMessage(), Arrays.toString(e.getStackTrace()));
            throw new AdjustmentException("can not do debit adjustment", e);
        }
    }
}
