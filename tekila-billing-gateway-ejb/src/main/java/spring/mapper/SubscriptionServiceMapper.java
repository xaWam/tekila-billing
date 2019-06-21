package spring.mapper;

import com.jaravir.tekila.module.service.entity.Service;
import org.mapstruct.Mapper;
import spring.dto.ServiceDTO;

/**
 * @author MusaAl
 * @date 4/2/2018 : 11:51 AM
 */
@Mapper(componentModel = "spring", uses = {})
public interface SubscriptionServiceMapper extends EntityMapper<ServiceDTO, Service> {

}
