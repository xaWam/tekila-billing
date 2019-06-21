package spring.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import javax.validation.constraints.NotNull;

/**
 * @author MushfigM on 5/31/2019
 */
public class CsmVasRequest {
    @NotNull
    private String agreement;
    @NotNull
    private Long vasId;
    private Long serviceProviderId;
    @NotNull
//    private VasActionType actionType;
    private Integer actionType;
//    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String timestamp;
    private String channel;
    private String operatorId;

    public String getAgreement() {
        return agreement;
    }

    public void setAgreement(String agreement) {
        this.agreement = agreement;
    }

    public Long getVasId() {
        return vasId;
    }

    public void setVasId(Long vasId) {
        this.vasId = vasId;
    }

    public Long getServiceProviderId() {
        return serviceProviderId;
    }

    public void setServiceProviderId(Long serviceProviderId) {
        this.serviceProviderId = serviceProviderId;
    }

    public Integer getActionType() {
        return actionType;
    }

    public void setActionType(Integer actionType) {
        this.actionType = actionType;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CsmVasRequest{");
        sb.append("agreement='").append(agreement).append('\'');
        sb.append(", vasId=").append(vasId);
        sb.append(", serviceProviderId=").append(serviceProviderId);
        sb.append(", actionType=").append(actionType);
        sb.append(", timestamp=").append(timestamp);
        sb.append(", channel='").append(channel).append('\'');
        sb.append(", operatorId='").append(operatorId).append('\'');
        sb.append('}');
        return sb.toString();
    }

//    public enum VasActionType {
//        SUBSCRIBE, UNSUBSCRIBE, SUSPEND, RESUME
//    }
}
