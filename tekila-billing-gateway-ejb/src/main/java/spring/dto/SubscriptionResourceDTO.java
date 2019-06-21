package spring.dto;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

import java.io.Serializable;
import java.util.List;

public class SubscriptionResourceDTO extends BaseDTO implements Serializable {

    private String name;
    private List<SubscriptionResourceBucketDTO> bucketList;
    private DateTime expirationDate;
    private DateTime activationDate;
    private LocalTime activeFrom;
    private LocalTime activeTill;
    private List<Integer> activeDaysOfWeekList;
    private boolean isActiveOnSpecialDays;
    private String dsc;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<SubscriptionResourceBucketDTO> getBucketList() {
        return bucketList;
    }

    public void setBucketList(List<SubscriptionResourceBucketDTO> bucketList) {
        this.bucketList = bucketList;
    }

    public DateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(DateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    public DateTime getActivationDate() {
        return activationDate;
    }

    public void setActivationDate(DateTime activationDate) {
        this.activationDate = activationDate;
    }

    public LocalTime getActiveFrom() {
        return activeFrom;
    }

    public void setActiveFrom(LocalTime activeFrom) {
        this.activeFrom = activeFrom;
    }

    public LocalTime getActiveTill() {
        return activeTill;
    }

    public void setActiveTill(LocalTime activeTill) {
        this.activeTill = activeTill;
    }

    public List<Integer> getActiveDaysOfWeekList() {
        return activeDaysOfWeekList;
    }

    public void setActiveDaysOfWeekList(List<Integer> activeDaysOfWeekList) {
        this.activeDaysOfWeekList = activeDaysOfWeekList;
    }

    public boolean isActiveOnSpecialDays() {
        return isActiveOnSpecialDays;
    }

    public void setActiveOnSpecialDays(boolean activeOnSpecialDays) {
        isActiveOnSpecialDays = activeOnSpecialDays;
    }

    public String getDsc() {
        return dsc;
    }

    public void setDsc(String dsc) {
        this.dsc = dsc;
    }
}
