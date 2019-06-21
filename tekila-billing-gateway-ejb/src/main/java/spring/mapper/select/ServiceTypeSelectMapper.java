package spring.mapper.select;

import com.jaravir.tekila.module.service.entity.SubscriptionServiceType;
import org.mapstruct.Mapper;
import spring.dto.ServiceTypeSelectDTO;
import spring.mapper.EntityMapper;

/**
 * @author MusaAl
 * @date 4/5/2018 : 2:23 PM
 */
@Mapper(componentModel = "spring", uses = {})
public interface ServiceTypeSelectMapper extends EntityMapper<ServiceTypeSelectDTO, SubscriptionServiceType>{
}
