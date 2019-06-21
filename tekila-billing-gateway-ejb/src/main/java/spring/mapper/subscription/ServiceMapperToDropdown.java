package spring.mapper.subscription;

import com.jaravir.tekila.module.service.entity.Service;
import org.mapstruct.Mapper;
import spring.dto.ServiceDropdownDTO;
import spring.mapper.EntityMapper;

/**
 * @author ElmarMa on 3/30/2018
 */
@Mapper(componentModel = "spring")
public interface ServiceMapperToDropdown extends EntityMapper<ServiceDropdownDTO,Service> {
}
