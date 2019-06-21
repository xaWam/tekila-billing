package spring.mapper;

import com.jaravir.tekila.module.accounting.entity.Charge;
import org.mapstruct.Mapper;
import spring.dto.ChargeDTO;

/**
 * @author ElmarMa on 4/9/2018
 */
@Mapper(componentModel = "spring",
        uses = {TransactionMapper.class})
public interface ChargeMapper extends EntityMapper<ChargeDTO, Charge> {


}
