package spring.dto;

import com.jaravir.tekila.module.accounting.entity.TaxationCategory;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberFunctionalCategory;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberLifeCycleType;
import org.joda.time.DateTime;

import java.util.Date;
import java.util.List;

/**
 * @author MusaAl
 * @date 4/2/2018 : 10:36 AM
 * it uses on Creating Customer -> Subscriber in our case (without subscriptions)
 */
public class SubscriberDTO extends BaseDTO {

    private Long master_account;
    private SubscriberDetailsDTO details;
//    private List<SubscriptionDTO> subscriptions;
    private boolean isBilledByLifeCycle;
    private SubscriberLifeCycleType lifeCycle;
    private DateTime lastPaymentDate;
    private Date creationDate;
//    private List<Invoice> invoices;
//    private TaxationCategory taxCategory;
    private SubscriberFunctionalCategory fnCategory;
//    private SubscriberRuntimeDetails runtimeDetails;


    public Long getMaster_account() {
        return master_account;
    }

    public void setMaster_account(Long master_account) {
        this.master_account = master_account;
    }

    public SubscriberDetailsDTO getDetails() {
        return details;
    }

    public void setDetails(SubscriberDetailsDTO details) {
        this.details = details;
    }


    public boolean isBilledByLifeCycle() {
        return isBilledByLifeCycle;
    }

    public void setBilledByLifeCycle(boolean billedByLifeCycle) {
        isBilledByLifeCycle = billedByLifeCycle;
    }

    public SubscriberLifeCycleType getLifeCycle() {
        return lifeCycle;
    }

    public void setLifeCycle(SubscriberLifeCycleType lifeCycle) {
        this.lifeCycle = lifeCycle;
    }

    public DateTime getLastPaymentDate() {
        return lastPaymentDate;
    }

    public void setLastPaymentDate(DateTime lastPaymentDate) {
        this.lastPaymentDate = lastPaymentDate;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public SubscriberFunctionalCategory getFnCategory() {
        return fnCategory;
    }

    public void setFnCategory(SubscriberFunctionalCategory fnCategory) {
        this.fnCategory = fnCategory;
    }

    @Override
    public String toString() {
        return "SubscriberDTO{" +
                "master_account=" + master_account +
                ", details=" + details +
                ", isBilledByLifeCycle=" + isBilledByLifeCycle +
                ", lifeCycle=" + lifeCycle +
                ", lastPaymentDate=" + lastPaymentDate +
                ", creationDate=" + creationDate +
                ", fnCategory=" + fnCategory +
                '}';
    }
}
