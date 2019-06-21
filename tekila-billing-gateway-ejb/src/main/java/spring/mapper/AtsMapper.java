package spring.mapper;

import com.jaravir.tekila.module.subscription.persistence.entity.Ats;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import spring.dto.AtsDTO;

/**
 * @author MusaAl
 * @date 3/12/2018 : 3:31 PM
 */
@Mapper(componentModel = "spring", uses = {})
public interface AtsMapper extends EntityMapper<AtsDTO, Ats>{

//    @Mapping(source = "coor", target = "coordinate")
//    AtsDTO toDto(Ats ats);
//
//    @Mapping(source = "coordinate", target = "coor")
//    Ats toEntity(AtsDTO atsDTO);

    default Ats fromId(Long id) {
        if (id == null) {
            return null;
        }
        Ats ats = new Ats();
        ats.setId(id);
        return ats;
    }

}
