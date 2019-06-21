package spring.mapper.subscription;

import com.jaravir.tekila.module.subscription.persistence.entity.House;
import org.mapstruct.Mapper;
import spring.dto.HouseDTO;
import spring.mapper.EntityMapper;
/**
 * @author GurbanAz
 */

@Mapper(componentModel = "spring")
public interface HouseMapper extends EntityMapper<HouseDTO, House> {

}
