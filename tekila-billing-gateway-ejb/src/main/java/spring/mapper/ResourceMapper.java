package spring.mapper;

import com.jaravir.tekila.module.service.entity.Resource;
import com.jaravir.tekila.module.subscription.persistence.entity.Ats;
import org.mapstruct.Mapper;
import spring.dto.AtsDTO;
import spring.dto.ResourceDTO;

/**
 * @author MusaAl
 * @date 3/12/2018 : 3:31 PM
 */
@Mapper(componentModel = "spring", uses = {})
public interface ResourceMapper extends EntityMapper<ResourceDTO, Resource>{

}
