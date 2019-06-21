package spring.dto;

import java.io.Serializable;

/**
 * @author ElmarMa on 3/16/2018
 */
public class StatusChangeRequestDTO implements Serializable {

    private Long subscriptionId;
    private Integer statusId;
    private String startDate;
    private String endDate;
    private boolean applyReversal;

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Integer getStatusId() {
        return statusId;
    }

    public void setStatusId(Integer statusId) {
        this.statusId = statusId;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public boolean isApplyReversal() {
        return applyReversal;
    }

    public void setApplyReversal(boolean applyReversal) {
        this.applyReversal = applyReversal;
    }

    @Override
    public String toString() {
        return "StatusChangeRequestDTO{" +
                "subscriptionId=" + subscriptionId +
                ", statusId=" + statusId +
                ", startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                ", applyReversal=" + applyReversal +
                '}';
    }
}
