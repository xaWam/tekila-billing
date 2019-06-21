package spring.mapper.subscription;

import com.jaravir.tekila.module.service.entity.VASCode;
import org.mapstruct.Mapper;
import spring.dto.VasCodeDTO;
import spring.mapper.EntityMapper;

@Mapper(componentModel = "spring" , uses = {VasCodeSequenceMapper.class})
public interface VasCodeMapper extends EntityMapper<VasCodeDTO, VASCode> {
}
