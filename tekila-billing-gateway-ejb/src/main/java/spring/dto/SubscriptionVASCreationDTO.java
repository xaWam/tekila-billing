package spring.dto;

import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionVAS;
import dto.jpa.Attachable;
import org.joda.time.DateTime;

import java.io.Serializable;

public class SubscriptionVASCreationDTO extends BaseDTO implements Serializable, Attachable<SubscriptionVAS> {
    private Long vasId;
//    private SubscriptionDTO subscription; // Su
    private Long subscriptionId;
//    private List<SubscriptionVASSetting> settings;
//    private SubscriptionResource resource;
    private DateTime billedUpToDate;
    private DateTime expirationDate;
    private SubscriptionStatus status;
    private long price;
    private DateTime activeFromDate;
    private long serviceFeeRate;
    private double count;
    private Double remainCount;
    private String desc;
    private int  vasStatus;

    public Long getVasId() {
        return vasId;
    }

    public void setVasId(Long vasId) {
        this.vasId = vasId;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public DateTime getBilledUpToDate() {
        return billedUpToDate;
    }

    public void setBilledUpToDate(DateTime billedUpToDate) {
        this.billedUpToDate = billedUpToDate;
    }

    public DateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(DateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public DateTime getActiveFromDate() {
        return activeFromDate;
    }

    public void setActiveFromDate(DateTime activeFromDate) {
        this.activeFromDate = activeFromDate;
    }

    public long getServiceFeeRate() {
        return serviceFeeRate;
    }

    public void setServiceFeeRate(long serviceFeeRate) {
        this.serviceFeeRate = serviceFeeRate;
    }

    public double getCount() {
        return count;
    }

    public void setCount(double count) {
        this.count = count;
    }

    public Double getRemainCount() {
        return remainCount;
    }

    public void setRemainCount(Double remainCount) {
        this.remainCount = remainCount;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int getVasStatus() {
        return vasStatus;
    }

    public void setVasStatus(int vasStatus) {
        this.vasStatus = vasStatus;
    }
}
