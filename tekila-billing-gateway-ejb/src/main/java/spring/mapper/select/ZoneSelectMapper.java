package spring.mapper.select;

import com.jaravir.tekila.module.service.entity.Zone;
import org.mapstruct.Mapper;
import spring.dto.ZoneSelectDTO;
import spring.mapper.EntityMapper;

/**
 * @author MusaAl
 * @date 4/5/2018 : 2:24 PM
 */
@Mapper(componentModel = "spring", uses = {})
public interface ZoneSelectMapper extends EntityMapper<ZoneSelectDTO, Zone>{
}
