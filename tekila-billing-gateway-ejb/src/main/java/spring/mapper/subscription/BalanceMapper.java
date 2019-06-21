package spring.mapper.subscription;

import com.jaravir.tekila.module.subscription.persistence.entity.Balance;
import org.mapstruct.Mapper;
import spring.dto.SubscriptionDTO;
import spring.mapper.EntityMapper;

@Mapper(componentModel = "spring")
public interface BalanceMapper extends EntityMapper<SubscriptionDTO.BalanceDTO,Balance> {}