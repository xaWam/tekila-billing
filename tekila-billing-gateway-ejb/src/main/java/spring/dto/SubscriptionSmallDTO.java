package spring.dto;

import com.jaravir.tekila.base.entity.Language;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;

import java.io.Serializable;

/**
 * @author MusaAl
 * @date 4/8/2018 : 4:18 PM
 */
public class SubscriptionSmallDTO implements Serializable {

    private Long id;
    private String agreement;
    private String identifier;
    private SubscriptionStatus status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAgreement() {
        return agreement;
    }

    public void setAgreement(String agreement) {
        this.agreement = agreement;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }
}
