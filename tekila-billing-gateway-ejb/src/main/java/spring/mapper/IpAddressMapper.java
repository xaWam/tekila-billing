package spring.mapper;

import com.jaravir.tekila.module.store.ip.persistence.IpAddress;
import org.mapstruct.Mapper;
import spring.dto.IpAddressDTO;

/**
 * @author MusaAl
 * @date 3/12/2018 : 3:31 PM
 */
@Mapper(componentModel = "spring", uses = {})
public interface IpAddressMapper extends EntityMapper<IpAddressDTO, IpAddress>{

}
