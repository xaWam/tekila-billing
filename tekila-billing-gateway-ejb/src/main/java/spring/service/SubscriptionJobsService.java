package spring.service;

import com.jaravir.tekila.module.periiodic.JobPersistenceFacade;
import com.jaravir.tekila.module.periodic.Job;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import spring.controller.vm.SubscriptionJobVM;
import spring.dto.SubscriptionJobsDTO;
import spring.mapper.SubscriptionJobsMapper;

import javax.ejb.EJB;
import java.util.List;

import static spring.util.Constants.INJECTION_POINT;

/**
 * @author MusaAl
 * @date 1/17/2019 : 11:20 PM
 */
@Service
public class SubscriptionJobsService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionJobsService.class);

    @EJB(mappedName = INJECTION_POINT+"JobPersistenceFacade")
    private JobPersistenceFacade jobPersistenceFacade;

    private final SubscriptionJobsMapper subscriptionJobsMapper;

    public SubscriptionJobsService(SubscriptionJobsMapper subscriptionJobsMapper) {
        this.subscriptionJobsMapper = subscriptionJobsMapper;
    }

    public List<SubscriptionJobsDTO> findAll(){
        return null;
    }

    public List<SubscriptionJobsDTO> findSubscriptionJobs(Long subscriptionId){
        return subscriptionJobsMapper.toDto(jobPersistenceFacade.findSubscriptionJobs(subscriptionId));
    }

    public void delete(Long id){
        log.debug("Request to delete Subscriptions Job: {} ", id);
        jobPersistenceFacade.removeIt(id);
    }

    public SubscriptionJobsDTO updateRunTime(SubscriptionJobVM subscriptionJobVM){
        Job job = jobPersistenceFacade.find(subscriptionJobVM.getId());
        if(subscriptionJobVM.getRunTime()!=null){
            DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
            DateTime runTime = formatter.parseDateTime(subscriptionJobVM.getRunTime());
            job.setStartTime(runTime);
            job.setDeadline(runTime.plusDays(1));
            job = jobPersistenceFacade.update(job);
        }
        return subscriptionJobsMapper.toDto(job);
    }
}
