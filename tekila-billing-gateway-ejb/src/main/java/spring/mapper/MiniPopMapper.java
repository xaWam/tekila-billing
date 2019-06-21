package spring.mapper;

import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import org.mapstruct.Mapper;
import spring.dto.MiniPopDTO;
import spring.mapper.subscription.HouseMapper;

/**
 * @author gurbanAz
 */
@Mapper(componentModel = "spring")
public interface MiniPopMapper extends EntityMapper<MiniPopDTO, MiniPop> {

}
