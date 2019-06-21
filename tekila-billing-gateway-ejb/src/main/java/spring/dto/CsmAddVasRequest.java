package spring.dto;

/**
 * @author MushfigM on 5/15/2019
 */
public class CsmAddVasRequest {

    private String agreement;
    private Long vasId;

    public CsmAddVasRequest() {
    }

    public CsmAddVasRequest(String agreement, Long vasId) {
        this.agreement = agreement;
        this.vasId = vasId;
    }

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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CsmAddVasRequest{");
        sb.append("agreement='").append(agreement).append('\'');
        sb.append(", vasId=").append(vasId);
        sb.append('}');
        return sb.toString();
    }
}
