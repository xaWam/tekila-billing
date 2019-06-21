package spring.service.finance;

import com.jaravir.tekila.module.accounting.entity.AccountingCategory;
import com.jaravir.tekila.module.accounting.entity.TransactionType;
import com.jaravir.tekila.module.accounting.manager.AccountingCategoryPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.provision.broadband.devices.manager.Providers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spring.exceptions.AdjustmentException;
import spring.exceptions.CustomerOperationException;
import spring.exceptions.SubscriptionNotFoundException;

import javax.ejb.EJB;

import java.util.Optional;

import static spring.util.Constants.INJECTION_POINT;

/**
 * @author ElmarMa on 3/15/2018
 */

@Service
public class AdjustmentService {
    private final Logger log = LoggerFactory.getLogger(AdjustmentService.class);

    @EJB(mappedName = INJECTION_POINT + "AccountingCategoryPersistenceFacade")
    private AccountingCategoryPersistenceFacade accountingCategorypf;
    @EJB(mappedName = INJECTION_POINT + "SubscriptionPersistenceFacade")
    private SubscriptionPersistenceFacade subscriptionpf;

    @Autowired
    private PromoBalanceAdjustmentService promoBalanceAdjustmentService;

    @Autowired
    private RealBalanceAdjustmentService realBalanceAdjustmentService;

    public void doAdjustment(Long subscriptionId,
                             Long accountingCategoryId,
                             Long amount,
                             String description,
                             String balanceTypeAsText,
                             String transactionTypeAsText) {

        if (amount <= 0)
            throw new AdjustmentException("Amount must be positive");

        BalanceTypes balanceType = null;
        TransactionType transactionType = null;

        try {
            balanceType = BalanceTypes.valueOf(balanceTypeAsText);
            transactionType = TransactionType.valueOf(transactionTypeAsText);
        } catch (Exception e) {
            throw new AdjustmentException(e.getMessage());
        }

        Subscription subscription = null;
        AccountingCategory accountingCategory = null;

        try {
            log.debug("subscriptionId: {}, accountingCategory: {}", subscriptionId, accountingCategoryId);
            subscription = subscriptionpf.find(subscriptionId);
            accountingCategory = accountingCategorypf.find(accountingCategoryId);
            log.debug("subscription: {}, accountingCategory: {}", subscription, accountingCategory);
            if (subscription == null || accountingCategory == null)
                throw new RuntimeException("subscription or account category can not be find");
        } catch (Exception e) {
            log.error("Adjustment error happened [subscription id: "+subscriptionId+"], error message: " + e.getMessage(), e);
            throw new AdjustmentException("Adjustment error happened, error message: " + e.getMessage(), e);
        }

        PaymentAdjuster paymentAdjuster = null;
        switch (balanceType) {
            case REAL:
                paymentAdjuster = realBalanceAdjustmentService;
                break;
            case PROMO:
                paymentAdjuster = promoBalanceAdjustmentService;
                if (subscription.getService().getProvider().getId() == Providers.DATAPLUS.getId())
                    throw new CustomerOperationException("This operation does not support currently for your provider");
                break;
            default:
                throw new AdjustmentException("balance type must be specified");
        }


        if (paymentAdjuster instanceof PromoBalanceAdjustmentService) {

            switch (transactionType) {
                case DEBIT_PROMO:
                    paymentAdjuster.doDebtAdjustment(subscription, accountingCategory, amount, description);
                    break;
                case CREDIT_PROMO:
                    paymentAdjuster.doCreditAdjustment(subscription, accountingCategory, amount, description);
                    break;
                default:
                    throw new AdjustmentException("operation type must be specified correctly ");

            }

        } else if (paymentAdjuster instanceof RealBalanceAdjustmentService) {

            switch (transactionType) {
                case DEBIT:
                    paymentAdjuster.doDebtAdjustment(subscription, accountingCategory, amount, description);
                    break;
                case CREDIT:
                    paymentAdjuster.doCreditAdjustment(subscription, accountingCategory, amount, description);
                    break;
                default:
                    throw new AdjustmentException("operation type must be specified correctly ");

            }

        } else {
            throw new AdjustmentException("adjustment unsuccessfull");
        }
    }

    public void doAdjustment(String agreement,
                             Long accountingCategoryId,
                             Long amount,
                             String description,
                             String balanceTypeAsText,
                             String transactionTypeAsText) {
        Long id = Optional.ofNullable(subscriptionpf.findSubscriptionIdByAgreement(agreement))
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found!"));
        doAdjustment(id, accountingCategoryId, amount, description, balanceTypeAsText, transactionTypeAsText);
    }


}
