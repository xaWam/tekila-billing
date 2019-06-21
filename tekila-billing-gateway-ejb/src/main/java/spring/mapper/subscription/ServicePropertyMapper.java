package spring.mapper.subscription;

import com.jaravir.tekila.module.service.entity.ServiceProperty;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import org.mapstruct.Mapper;
import spring.dto.ServicePropertyDTO;
import spring.dto.SubscriptionDTO;
import spring.mapper.EntityMapper;

@Mapper(componentModel = "spring")
public interface ServicePropertyMapper extends EntityMapper<ServicePropertyDTO, ServiceProperty> {
}
