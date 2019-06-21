package spring.mapper.subscription;

import com.jaravir.tekila.module.service.entity.ServiceSetting;
import org.mapstruct.Mapper;
import spring.dto.SubscriptionSettingDTO;
import spring.mapper.EntityMapper;

/**
 * @author ElmarMa on 3/29/2018
 */
@Mapper(componentModel = "spring")
public interface ServiceSettingMapper extends EntityMapper<SubscriptionSettingDTO.ServiceSettingDTO,ServiceSetting> {
}
