package spring.mapper;

import com.jaravir.tekila.module.periodic.Job;
import org.mapstruct.Mapper;
import spring.dto.SubscriptionJobsDTO;

/**
 * @author MusaAl
 * @date 1/17/2019 : 11:22 PM
 */
@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface SubscriptionJobsMapper extends EntityMapper<SubscriptionJobsDTO, Job> {
}
