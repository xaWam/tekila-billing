package spring.dto;

import com.jaravir.tekila.base.auth.persistence.ExternalUser;
import com.jaravir.tekila.module.accounting.PaymentPurpose;
import com.jaravir.tekila.module.accounting.entity.TransactionType;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Date;

/**
 * @author ElmarMa on 4/9/2018
 */
public class ManualPaymentDTO implements Serializable {

    private Long id;
    private String agreementId;// only agreement of Subscription
    private Double amount;
    private String desc;
    private String username; //only username of User
    private String methodName;
    private String chequeId;
    private Date fd;
    private Long subscriber_id;
    private Long subscriptionId;
    private String internalDesc;
    private boolean testPayment;
    private String bank;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAgreementId() {
        return agreementId;
    }

    public void setAgreementId(String agreementId) {
        this.agreementId = agreementId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getChequeId() {
        return chequeId;
    }

    public void setChequeId(String chequeId) {
        this.chequeId = chequeId;
    }

    public Date getFd() {
        return fd;
    }

    public void setFd(Date fd) {
        this.fd = fd;
    }

    public Long getSubscriber_id() {
        return subscriber_id;
    }

    public void setSubscriber_id(Long subscriber_id) {
        this.subscriber_id = subscriber_id;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getInternalDesc() {
        return internalDesc;
    }

    public void setInternalDesc(String internalDesc) {
        this.internalDesc = internalDesc;
    }

    public boolean isTestPayment() {
        return testPayment;
    }

    public void setTestPayment(boolean testPayment) {
        this.testPayment = testPayment;
    }

    public String getBank() {
        return bank;
    }

    public void setBank(String bank) {
        this.bank = bank;
    }

    @Override
    public String toString() {
        return "ManualPaymentDTO{" +
                "id=" + id +
                ", agreementId='" + agreementId + '\'' +
                ", amount=" + amount +
                ", desc='" + desc + '\'' +
                ", username='" + username + '\'' +
                ", methodName='" + methodName + '\'' +
                ", chequeId='" + chequeId + '\'' +
                ", fd=" + fd +
                ", subscriber_id=" + subscriber_id +
                ", subscriptionId=" + subscriptionId +
                ", internalDesc='" + internalDesc + '\'' +
                ", testPayment=" + testPayment +
                ", bank='" + bank + '\'' +
                '}';
    }
}
