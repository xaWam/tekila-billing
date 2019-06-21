package spring.mapper;


import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberDetails;
import org.mapstruct.Mapper;
import spring.dto.SubscriberDetailsDTO;

/**
 * @author MusaAl
 * @date 4/2/2018 : 11:51 AM
 */
@Mapper(componentModel = "spring", uses = {})
public interface SubscriberDetailsMapper extends EntityMapper<SubscriberDetailsDTO, SubscriberDetails> {
}
