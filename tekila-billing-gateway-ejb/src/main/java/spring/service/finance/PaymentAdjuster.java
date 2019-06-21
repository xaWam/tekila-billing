package spring.service.finance;

import com.jaravir.tekila.module.accounting.entity.AccountingCategory;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;

public interface PaymentAdjuster {

    void doCreditAdjustment(Subscription subscription, AccountingCategory accountingCategory,Long amount,String description);

    void doDebtAdjustment(Subscription subscription, AccountingCategory accountingCategory,Long amount,String description);

}
