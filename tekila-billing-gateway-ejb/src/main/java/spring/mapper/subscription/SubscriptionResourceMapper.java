package spring.mapper.subscription;

import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionResource;
import org.mapstruct.Mapper;
import spring.dto.SubscriptionResourceDTO;
import spring.mapper.EntityMapper;

/**
 * @author GurbanAz
 */
@Mapper(componentModel = "spring", uses = {SubscriptionResourceBucketMapper.class})
public interface SubscriptionResourceMapper extends EntityMapper<SubscriptionResourceDTO, SubscriptionResource> {
}
