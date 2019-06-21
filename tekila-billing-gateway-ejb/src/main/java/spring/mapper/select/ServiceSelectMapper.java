package spring.mapper.select;

import com.jaravir.tekila.module.service.entity.Service;
import org.mapstruct.Mapper;
import spring.dto.ServiceSelectDTO;
import spring.mapper.EntityMapper;

/**
 * @author MusaAl
 * @date 4/5/2018 : 2:23 PM
 */
@Mapper(componentModel = "spring", uses = {})
public interface ServiceSelectMapper extends EntityMapper<ServiceSelectDTO, Service>{
}
