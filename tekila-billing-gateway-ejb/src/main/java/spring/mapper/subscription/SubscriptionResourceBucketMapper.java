package spring.mapper.subscription;

import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionResourceBucket;
import org.mapstruct.Mapper;
import spring.dto.SubscriptionResourceBucketDTO;
import spring.mapper.EntityMapper;

/**
 * @author GurbanAz
 */
@Mapper(componentModel = "spring")
public interface SubscriptionResourceBucketMapper extends EntityMapper<SubscriptionResourceBucketDTO, SubscriptionResourceBucket> {


}
