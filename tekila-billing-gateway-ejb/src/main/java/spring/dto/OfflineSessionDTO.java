package spring.dto;

import com.jaravir.tekila.module.service.entity.ServiceProvider;

import java.io.Serializable;
import java.util.Date;

/**
 * @author ElmarMa on 3/27/2018
 */
public class OfflineSessionDTO implements Serializable{

    private long radAccountID;
    private String accountSessionID;
    private String accountID;
    private String down;
    private String up;
    private String user;
    private Date startTime;
    private Date endTime;
    private String callingStationID;
    private String framedAddress;
    private String nasIpAddress;
    private String sessionDuration;
    private String terminationCause;
    private String service;
    private String dslamIpAddress;


    public String getDown() {
        return down;
    }

    public void setDown(String down) {
        this.down = down;
    }

    public String getUp() {
        return up;
    }

    public void setUp(String up) {
        this.up = up;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getCallingStationID() {
        return callingStationID;
    }

    public void setCallingStationID(String callingStationID) {
        this.callingStationID = callingStationID;
    }

    public String getFramedAddress() {
        return framedAddress;
    }

    public void setFramedAddress(String framedAddress) {
        this.framedAddress = framedAddress;
    }

    public String getNasIpAddress() {
        return nasIpAddress;
    }

    public void setNasIpAddress(String nasIpAddress) {
        this.nasIpAddress = nasIpAddress;
    }

    public String getSessionDuration() {
        return sessionDuration;
    }

    public void setSessionDuration(String sessionDuration) {
        this.sessionDuration = sessionDuration;
    }

    public String getTerminationCause() {
        return terminationCause;
    }

    public void setTerminationCause(String teminationCause) {
        this.terminationCause = teminationCause;
    }

    public String getAccountID() {
        return accountID;
    }

    public void setAccountID(String accountID) {
        this.accountID = accountID;
    }

    public long getRadAccountID() {
        return radAccountID;
    }

    public void setRadAccountID(long radAccountID) {
        this.radAccountID = radAccountID;
    }

    public String getAccountSessionID() {
        return accountSessionID;
    }

    public void setAccountSessionID(String accountSessionID) {
        this.accountSessionID = accountSessionID;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getDslamIpAddress() {
        return dslamIpAddress;
    }

    public void setDslamIpAddress(String dslamIpAddress) {
        this.dslamIpAddress = dslamIpAddress;
    }


}
