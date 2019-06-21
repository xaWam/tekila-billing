package spring.mapper.subscription;

import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionSetting;
import org.mapstruct.Mapper;
import spring.dto.SubscriptionSettingDTO;
import spring.mapper.EntityMapper;

/**
 * @author ElmarMa on 3/29/2018
 */
@Mapper(componentModel = "spring",
        uses = ServiceSettingMapper.class)
public interface SubscriptionSettingMapper extends EntityMapper<SubscriptionSettingDTO, SubscriptionSetting> {
}
