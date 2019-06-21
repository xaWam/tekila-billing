package spring.mapper.subscription;

import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionDetails;
import org.mapstruct.Mapper;
import spring.dto.SubscriptionDetailsDTO;
import spring.mapper.EntityMapper;

@Mapper(componentModel = "spring")
public interface SubscriptionDetailsMapper extends EntityMapper<SubscriptionDetailsDTO, SubscriptionDetails> {
}
