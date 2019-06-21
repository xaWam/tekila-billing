package spring.mapper.subscription;

import com.jaravir.tekila.module.service.entity.VASCodeSequence;
import org.mapstruct.Mapper;
import spring.dto.VasCodeSequenceDTO;
import spring.mapper.EntityMapper;

@Mapper(componentModel = "spring")
public interface VasCodeSequenceMapper extends EntityMapper<VasCodeSequenceDTO , VASCodeSequence> {
}
