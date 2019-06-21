package spring.mapper.subscription;

import com.jaravir.tekila.module.subscription.persistence.entity.Reseller;
import org.mapstruct.Mapper;
import spring.dto.ResellerDropdownDTO;
import spring.mapper.EntityMapper;

/**
 * @author ElmarMa on 3/30/2018
 */
@Mapper(componentModel = "spring")
public interface ResellerMapperDropdown extends EntityMapper<ResellerDropdownDTO,Reseller> {
}
