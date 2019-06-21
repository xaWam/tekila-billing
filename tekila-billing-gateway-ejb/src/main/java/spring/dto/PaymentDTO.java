package spring.dto;

import com.jaravir.tekila.base.auth.persistence.ExternalUser;
import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.jsonview.JsonViews;
import com.jaravir.tekila.module.accounting.PaymentPurpose;
import com.jaravir.tekila.module.accounting.entity.PaymentOption;
import com.jaravir.tekila.module.accounting.entity.Transaction;
import com.jaravir.tekila.module.accounting.entity.TransactionType;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Date;

/**
 * @author ElmarMa on 4/9/2018
 */
public class PaymentDTO implements Serializable {

    private Long id;
    private String rrn;
    private String rid;
    private String sessid;
    private String agreement;// only agreement of Subscription
    private String contract;
    private String serviceName;
    private Double amount;
    private String currency;
    private String datetime;
    private String dsc;
    private String username; //only username of User
    private int status;
    private ExternalUser ext_user;
    private int processed;
    private long user_id;
    private String methodName;
    private String chequeID;
    private Date fd;
    private Long subscriber_id;
    private PaymentPurpose purpose;
    private TransactionDTO transaction;
    private String internalDsc;
    private boolean testPayment;
    private Long campaignId;

    public static class TransactionDTO implements Serializable {
        private Long id;
        private TransactionType type;
        private long startBalance;
        private long endBalance;
        private long amount;
        private String dsc;
        private DateTime lastUpdateDate;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public DateTime getLastUpdateDate() {
            return lastUpdateDate;
        }

        public void setLastUpdateDate(DateTime lastUpdateDate) {
            this.lastUpdateDate = lastUpdateDate;
        }

        public TransactionType getType() {
            return type;
        }

        public void setType(TransactionType type) {
            this.type = type;
        }

        public long getStartBalance() {
            return startBalance;
        }

        public void setStartBalance(long startBalance) {
            this.startBalance = startBalance;
        }

        public long getEndBalance() {
            return endBalance;
        }

        public void setEndBalance(long endBalance) {
            this.endBalance = endBalance;
        }

        public long getAmount() {
            return amount;
        }

        public void setAmount(long amount) {
            this.amount = amount;
        }

        public String getDsc() {
            return dsc;
        }

        public void setDsc(String dsc) {
            this.dsc = dsc;
        }
    }


    public String getRrn() {
        return rrn;
    }

    public void setRrn(String rrn) {
        this.rrn = rrn;
    }

    public String getRid() {
        return rid;
    }

    public void setRid(String rid) {
        this.rid = rid;
    }

    public String getSessid() {
        return sessid;
    }

    public void setSessid(String sessid) {
        this.sessid = sessid;
    }

    public String getAgreement() {
        return agreement;
    }

    public void setAgreement(String agreement) {
        this.agreement = agreement;
    }

    public String getContract() {
        return contract;
    }

    public void setContract(String contract) {
        this.contract = contract;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    public String getDsc() {
        return dsc;
    }

    public void setDsc(String dsc) {
        this.dsc = dsc;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public ExternalUser getExt_user() {
        return ext_user;
    }

    public void setExt_user(ExternalUser ext_user) {
        this.ext_user = ext_user;
    }

    public int getProcessed() {
        return processed;
    }

    public void setProcessed(int processed) {
        this.processed = processed;
    }

    public long getUser_id() {
        return user_id;
    }

    public void setUser_id(long user_id) {
        this.user_id = user_id;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getChequeID() {
        return chequeID;
    }

    public void setChequeID(String chequeID) {
        this.chequeID = chequeID;
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

    public PaymentPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(PaymentPurpose purpose) {
        this.purpose = purpose;
    }

    public TransactionDTO getTransaction() {
        return transaction;
    }

    public void setTransaction(TransactionDTO transaction) {
        this.transaction = transaction;
    }

    public String getInternalDsc() {
        return internalDsc;
    }

    public void setInternalDsc(String internalDsc) {
        this.internalDsc = internalDsc;
    }

    public boolean isTestPayment() {
        return testPayment;
    }

    public void setTestPayment(boolean testPayment) {
        this.testPayment = testPayment;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
