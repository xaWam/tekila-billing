package spring.mapper.subscription;

import com.jaravir.tekila.module.service.entity.Service;
import org.mapstruct.Mapper;
import spring.dto.SubscriptionDTO;
import spring.mapper.EntityMapper;

@Mapper(componentModel = "spring")
public interface ServiceMapper extends EntityMapper<SubscriptionDTO.ServiceDTO, Service> {
}