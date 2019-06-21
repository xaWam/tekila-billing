package spring.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @author MushfigM on 5/14/2019
 */
public class CsmChargeRequest {

    @NotNull
    private String agreement;
//    @DecimalMin(value = "0.1", inclusive = true)
    private BigDecimal amount;
    private Long serviceId;
    private Long serviceProviderId;
    private String categoryId;
    private String contentId;
    private String cdrSpname;

    public String getAgreement() {
        return agreement;
    }

    public void setAgreement(String agreement) {
        this.agreement = agreement;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public Long getServiceProviderId() {
        return serviceProviderId;
    }

    public void setServiceProviderId(Long serviceProviderId) {
        this.serviceProviderId = serviceProviderId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public String getCdrSpname() {
        return cdrSpname;
    }

    public void setCdrSpname(String cdrSpname) {
        this.cdrSpname = cdrSpname;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CsmChargeRequest{");
        sb.append("agreement='").append(agreement).append('\'');
        sb.append(", amount=").append(amount);
        sb.append(", serviceId=").append(serviceId);
        sb.append(", serviceProviderId=").append(serviceProviderId);
        sb.append(", categoryId='").append(categoryId).append('\'');
        sb.append(", contentId='").append(contentId).append('\'');
        sb.append(", cdrSpname='").append(cdrSpname).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
