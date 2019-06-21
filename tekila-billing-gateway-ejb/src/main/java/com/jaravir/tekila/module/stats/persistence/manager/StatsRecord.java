package com.jaravir.tekila.module.stats.persistence.manager;

import java.util.Date;

/**
 * Created by sajabrayilov on 7/8/2015.
 */
public class StatsRecord {
    private long id;
    private String up;
    private String down;
    private String nasIpAddress;
    private String framedAddress;
    private String CallingStationID;
    private Date startDate;
    private Date stopDate;
    private String user;
    private String accountID;
    private String terminationCause;
    private String sessionDuration;
    private String accountSessionID;
    private String dslamAddress;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUp() {
        return up;
    }

    public void setUp(String up) {
        this.up = up;
    }

    public String getDown() {
        return down;
    }

    public void setDown(String down) {
        this.down = down;
    }

    public String getNasIpAddress() {
        return nasIpAddress;
    }

    public void setNasIpAddress(String nasIpAddress) {
        this.nasIpAddress = nasIpAddress;
    }

    public String getFramedAddress() {
        return framedAddress;
    }

    public void setFramedAddress(String framedAddress) {
        this.framedAddress = framedAddress;
    }

    public String getCallingStationID() {
        return CallingStationID;
    }

    public void setCallingStationID(String callingStationID) {
        CallingStationID = callingStationID;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getStopDate() {
        return stopDate;
    }

    public void setStopDate(Date stopDate) {
        this.stopDate = stopDate;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getAccountID() {
        return accountID;
    }

    public void setAccountID(String accountID) {
        this.accountID = accountID;
    }

    public String getTerminationCause() {
        return terminationCause;
    }

    public void setTerminationCause(String terminationCause) {
        this.terminationCause = terminationCause;
    }

    public String getSessionDuration() {
        return sessionDuration;
    }

    public void setSessionDuration(String sessionDuration) {
        this.sessionDuration = sessionDuration;
    }

    public String getAccountSessionID() {
        return accountSessionID;
    }

    public void setAccountSessionID(String accountSessionID) {
        this.accountSessionID = accountSessionID;
    }

    public String getDslamAddress() {
        return dslamAddress;
    }

    public void setDslamAddress(String dslamAddress) {
        this.dslamAddress = dslamAddress;
    }
}
