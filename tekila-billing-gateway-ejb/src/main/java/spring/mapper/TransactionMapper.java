package spring.mapper;

import com.jaravir.tekila.module.accounting.entity.Transaction;
import org.mapstruct.Mapper;
import spring.dto.PaymentDTO;

/**
 * @author ElmarMa on 4/9/2018
 */
@Mapper(componentModel = "spring")
public interface TransactionMapper extends EntityMapper<PaymentDTO.TransactionDTO, Transaction> {
}
