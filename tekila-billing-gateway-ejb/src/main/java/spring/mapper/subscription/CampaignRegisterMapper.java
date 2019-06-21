package spring.mapper.subscription;

import com.jaravir.tekila.module.campaign.CampaignRegister;
import org.mapstruct.Mapper;
import spring.dto.CampaignRegisterDTO;
import spring.mapper.EntityMapper;

/**
 * @author ElmarMa on 3/27/2018
 */
@Mapper(componentModel = "spring", uses = CampaignMapper.class)
public interface CampaignRegisterMapper extends EntityMapper<CampaignRegisterDTO, CampaignRegister> {
}
