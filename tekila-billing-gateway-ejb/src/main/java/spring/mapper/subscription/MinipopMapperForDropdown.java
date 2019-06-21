package spring.mapper.subscription;

import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import org.mapstruct.Mapper;
import spring.dto.MinipopDropdownDTO;
import spring.mapper.EntityMapper;

/**
 * @author ElmarMa on 3/30/2018
 */
@Mapper(componentModel = "spring")
public interface MinipopMapperForDropdown extends EntityMapper<MinipopDropdownDTO,MiniPop> {
}
