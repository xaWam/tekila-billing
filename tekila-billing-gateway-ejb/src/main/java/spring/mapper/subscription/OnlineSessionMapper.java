package spring.mapper.subscription;

import com.jaravir.tekila.module.stats.persistence.entity.OnlineBroadbandStats;
import org.mapstruct.Mapper;
import spring.dto.OnlineSessionDTO;
import spring.mapper.EntityMapper;

/**
 * @author ElmarMa on 3/27/2018
 */
@Mapper(componentModel = "spring")
public interface OnlineSessionMapper extends EntityMapper<OnlineSessionDTO,OnlineBroadbandStats> {
}
