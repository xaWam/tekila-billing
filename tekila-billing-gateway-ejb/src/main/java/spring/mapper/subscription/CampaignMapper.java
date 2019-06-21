package spring.mapper.subscription;

import com.jaravir.tekila.module.campaign.Campaign;
import org.mapstruct.Mapper;
import spring.dto.CampaignRegisterDTO;
import spring.mapper.EntityMapper;

/**
 * @author ElmarMa on 3/27/2018
 */
@Mapper(componentModel = "spring")
public interface CampaignMapper extends EntityMapper<CampaignRegisterDTO.CampaignDTO,Campaign>{
}
