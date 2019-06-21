package spring.mapper;

import com.jaravir.tekila.module.service.entity.ServiceProvider;
import org.mapstruct.Mapper;
import spring.dto.ServiceProviderDTO;

/**
 * @author MusaAl
 * @date 3/12/2018 : 3:31 PM
 */
@Mapper(componentModel = "spring", uses = {})
public interface ServiceProviderMapper extends EntityMapper<ServiceProviderDTO, ServiceProvider>{
}
