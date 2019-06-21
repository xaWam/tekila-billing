package spring.dto;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.module.accounting.AccountingStatus;
import com.jaravir.tekila.module.accounting.entity.Payment;
import com.jaravir.tekila.module.accounting.entity.Transaction;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscriber;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import org.joda.time.DateTime;

import java.io.Serializable;

/**
 * @author ElmarMa on 4/9/2018
 */
public class ChargeDTO implements Serializable {

    private Long id;
    private String serviceName;
    private long amount;
    private DateTime datetime;
    private String dsc;
    private String username;
    private String agreement;
    private AccountingStatus status;
    private PaymentDTO.TransactionDTO transaction;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public DateTime getDatetime() {
        return datetime;
    }

    public void setDatetime(DateTime datetime) {
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

    public String getAgreement() {
        return agreement;
    }

    public void setAgreement(String agreement) {
        this.agreement = agreement;
    }

    public AccountingStatus getStatus() {
        return status;
    }

    public void setStatus(AccountingStatus status) {
        this.status = status;
    }

    public PaymentDTO.TransactionDTO getTransaction() {
        return transaction;
    }

    public void setTransaction(PaymentDTO.TransactionDTO transaction) {
        this.transaction = transaction;
    }
}
