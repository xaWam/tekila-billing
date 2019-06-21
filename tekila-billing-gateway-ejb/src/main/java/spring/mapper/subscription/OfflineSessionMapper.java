package spring.mapper.subscription;

import com.jaravir.tekila.module.stats.persistence.entity.OfflineBroadbandStats;
import org.mapstruct.Mapper;
import spring.dto.OfflineSessionDTO;
import spring.mapper.EntityMapper;

/**
 * @author ElmarMa on 3/27/2018
 */
@Mapper(componentModel = "spring")
public interface OfflineSessionMapper extends EntityMapper<OfflineSessionDTO,OfflineBroadbandStats> {
}
