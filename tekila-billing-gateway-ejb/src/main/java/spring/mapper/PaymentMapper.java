package spring.mapper;

import com.jaravir.tekila.module.accounting.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import spring.dto.PaymentDTO;

/**
 * @author ElmarMa on 4/9/2018
 */
@Mapper(componentModel = "spring",
        uses = {TransactionMapper.class})
public interface PaymentMapper extends EntityMapper<PaymentDTO, Payment> {


}
