package spring.controller.vm;

import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionVAS;
import dto.jpa.Attachable;
import org.joda.time.DateTime;
import spring.dto.BaseDTO;

import java.io.Serializable;

public class SubscriptionVASCreationVM{
    private Long vasId;
//    private SubscriptionDTO subscription; // Su
    private Long subscriptionId;
//    private List<SubscriptionVASSetting> settings;
//    private SubscriptionResource resource;
    private String billedUpToDate;
    private String expirationDate;
    private String dateForTheFuture;
    private SubscriptionStatus status;
    private long price;
    private String activeFromDate;
    private long serviceFeeRate;
    private double count;
    private Double remainCount;
    private String desc;
    private int  vasStatus;
    private String addressAsString;

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

    public String getBilledUpToDate() {
        return billedUpToDate;
    }

    public void setBilledUpToDate(String billedUpToDate) {
        this.billedUpToDate = billedUpToDate;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
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

    public String getActiveFromDate() {
        return activeFromDate;
    }

    public void setActiveFromDate(String activeFromDate) {
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

    public String getAddressAsString() {
        return addressAsString;
    }

    public void setAddressAsString(String addressAsString) {
        this.addressAsString = addressAsString;
    }

    public String getDateForTheFuture() {
        return dateForTheFuture;
    }

    public void setDateForTheFuture(String dateForTheFuture) {
        this.dateForTheFuture = dateForTheFuture;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SubscriptionVASCreationVM{");
        sb.append("vasId=").append(vasId);
        sb.append(", subscriptionId=").append(subscriptionId);
        sb.append(", billedUpToDate='").append(billedUpToDate).append('\'');
        sb.append(", expirationDate='").append(expirationDate).append('\'');
        sb.append(", dateForTheFuture='").append(dateForTheFuture).append('\'');
        sb.append(", status=").append(status);
        sb.append(", price=").append(price);
        sb.append(", activeFromDate='").append(activeFromDate).append('\'');
        sb.append(", serviceFeeRate=").append(serviceFeeRate);
        sb.append(", count=").append(count);
        sb.append(", remainCount=").append(remainCount);
        sb.append(", desc='").append(desc).append('\'');
        sb.append(", vasStatus=").append(vasStatus);
        sb.append(", addressAsString='").append(addressAsString).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
