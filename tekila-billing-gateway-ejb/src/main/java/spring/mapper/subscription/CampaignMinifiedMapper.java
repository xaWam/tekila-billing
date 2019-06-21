package spring.mapper.subscription;

import com.jaravir.tekila.module.campaign.Campaign;
import org.mapstruct.Mapper;
import spring.dto.CampaignMinifiedDTO;
import spring.mapper.EntityMapper;

/**
 * @author ElmarMa on 3/28/2018
 */
@Mapper(componentModel = "spring")
public interface CampaignMinifiedMapper extends EntityMapper<CampaignMinifiedDTO,Campaign> {
}
