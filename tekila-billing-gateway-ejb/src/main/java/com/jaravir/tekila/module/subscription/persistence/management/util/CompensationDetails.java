package com.jaravir.tekila.module.subscription.persistence.management.util;


import java.util.Date;

/**
 * Created by kmaharov on 23.06.2016.
 */
public class CompensationDetails {
    private Date fromDate;
    private Integer dayCount;
    private String ticketId;
    private String comments;

    public CompensationDetails() {

    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Integer getDayCount() {
        return dayCount;
    }

    public void setDayCount(Integer dayCount) {
        this.dayCount = dayCount;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public boolean isFull() {
        return (fromDate != null &&
                dayCount != null &&
                ticketId != null && !ticketId.isEmpty() &&
                comments != null && !comments.isEmpty());
    }
}
