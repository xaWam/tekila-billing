package spring.dto;

import java.io.Serializable;

/**
 * @author ElmarMa on 3/15/2018
 */
public class AdjustmentRequestDTO implements Serializable {

    private double amount;
    private String balanceType;
    private String operationType;
    private Long accountCategoryId;
    private Long subscriptionId;
    private String description;

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getBalanceType() {
        return balanceType;
    }

    public void setBalanceType(String balanceType) {
        this.balanceType = balanceType;
    }

    public Long getAccountCategoryId() {
        return accountCategoryId;
    }

    public void setAccountCategoryId(Long accountCategoryId) {
        this.accountCategoryId = accountCategoryId;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "AdjustmentRequestDTO{" +
                "amount=" + amount +
                ", operationType='" + operationType + '\'' +
                ", balanceType='" + balanceType + '\'' +
                ", accountCategoryId=" + accountCategoryId +
                ", subscriptionId=" + subscriptionId +
                ", description='" + description + '\'' +
                '}';
    }
}
