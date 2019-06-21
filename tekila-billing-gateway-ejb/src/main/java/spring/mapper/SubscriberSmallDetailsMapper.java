package spring.mapper;

import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberDetails;
import org.mapstruct.Mapper;
import spring.dto.SubscriberDetailsSmallDTO;

@Mapper(componentModel = "spring")
public interface SubscriberSmallDetailsMapper extends EntityMapper<SubscriberDetailsSmallDTO, SubscriberDetails>{
}
