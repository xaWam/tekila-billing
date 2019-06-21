package spring.mapper.manual;

import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import spring.dto.SubscriptionSmallDTO;

/**
 * @author MusaAl
 * @date 10/12/2018 : 11:56 AM
 */
public class SubscriptionMapper {

    public SubscriptionSmallDTO toSmallDTO(Subscription subscription){
        SubscriptionSmallDTO dto = new SubscriptionSmallDTO();

        dto.setId(subscription.getId());
        dto.setAgreement(subscription.getAgreement());
        dto.setIdentifier(subscription.getIdentifier());
        dto.setStatus(subscription.getStatus());

        return dto;
    }

}
