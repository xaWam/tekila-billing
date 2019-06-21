package spring.mapper;

import com.jaravir.tekila.module.subscription.persistence.entity.Streets;
import org.mapstruct.Mapper;
import spring.dto.StreetDTO;

/**
 * @author MusaAl
 * @date 2/12/2019 : 3:31 PM
 */
@Mapper(componentModel = "spring", uses = {})
public interface StreetMapper extends EntityMapper<StreetDTO, Streets>{

}
