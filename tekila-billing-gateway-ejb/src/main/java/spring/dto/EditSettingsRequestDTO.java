package spring.dto;

import java.io.Serializable;

/**
 * @author ElmarMa on 3/29/2018
 */
public class EditSettingsRequestDTO implements Serializable {

    private Long serviceId;
    private Long serviceTypeId;
    private Long zoneId;
    private String ats;
    private Long minipopId;
    private Long portId;
    private Long resellerId;
    private String username;
    private String password;

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public Long getServiceTypeId() {
        return serviceTypeId;
    }

    public void setServiceTypeId(Long serviceTypeId) {
        this.serviceTypeId = serviceTypeId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public String getAts() {
        return ats;
    }

    public void setAts(String ats) {
        this.ats = ats;
    }

    public Long getMinipopId() {
        return minipopId;
    }

    public void setMinipopId(Long minipopId) {
        this.minipopId = minipopId;
    }

    public Long getPortId() {
        return portId;
    }

    public void setPortId(Long portId) {
        this.portId = portId;
    }

    public Long getResellerId() { return resellerId; }

    public void setResellerId(Long resellerId) {
        this.resellerId = resellerId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    @Override
    public String toString() {
        return "EditSettingsRequestDTO{" +
                "serviceId=" + serviceId +
                ", serviceTypeId=" + serviceTypeId +
                ", zoneId=" + zoneId +
                ", ats='" + ats + '\'' +
                ", minipopId=" + minipopId +
                ", portId=" + portId +
                ", resellerId=" + resellerId +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
