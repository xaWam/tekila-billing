package spring.mapper.subscription;

import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionDetails;
import org.mapstruct.Mapper;
import spring.dto.SubscriptionDTO;
import spring.mapper.EntityMapper;

@Mapper(componentModel = "spring")
public interface SubscriptionDetailMapper extends EntityMapper<SubscriptionDTO.SubscriptionDetailDTO, SubscriptionDetails> {
}
