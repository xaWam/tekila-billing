package spring.mapper.subscription;

import com.jaravir.tekila.module.subscription.persistence.entity.Dealer;
import org.mapstruct.Mapper;
import spring.dto.DealerDropdownDTO;
import spring.mapper.EntityMapper;

/**
 * @author MushfigM
 */
@Mapper(componentModel = "spring")
public interface DealerMapper extends EntityMapper<DealerDropdownDTO, Dealer> {
}
