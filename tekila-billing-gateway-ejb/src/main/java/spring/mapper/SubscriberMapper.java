package spring.mapper;

import com.jaravir.tekila.module.subscription.persistence.entity.Subscriber;
import org.mapstruct.Mapper;
import spring.dto.SubscriberDTO;
import spring.mapper.subscription.SubscriptionMapper;

/**
 * @author MusaAl
 * @date 4/2/2018 : 11:51 AM
 */
@Mapper(componentModel = "spring", uses = {SubscriberDetailsMapper.class, SubscriptionMapper.class})
public interface SubscriberMapper extends EntityMapper<SubscriberDTO, Subscriber> {

}
