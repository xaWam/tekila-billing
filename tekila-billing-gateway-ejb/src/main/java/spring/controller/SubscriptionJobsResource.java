package spring.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.controller.vm.SubscriptionJobVM;
import spring.dto.SubscriptionJobsDTO;
import spring.service.SubscriptionJobsService;

import java.util.List;

@RestController
public class SubscriptionJobsResource {
    private static final Logger log = LoggerFactory.getLogger(SubscriptionJobsResource.class);

    private static final String ENTITY_NAME = "SUBSCRIPTION_JOBS";

    private final SubscriptionJobsService subscriptionJobsService;

    public SubscriptionJobsResource(SubscriptionJobsService subscriptionJobsService) {
        this.subscriptionJobsService = subscriptionJobsService;
    }

    @GetMapping("/subscriptions/{id}/jobs")
    public List<SubscriptionJobsDTO> getSubscriptionJob(@PathVariable Long id) {
        log.debug("Rest request for Getting Subscription Registered Tasks(Jobs) - Subscription id : {}", id);
        return subscriptionJobsService.findSubscriptionJobs(id);
    }

    @DeleteMapping("/subscriptions/jobs/{id}")
    public ResponseEntity<Void> removeSubscriptionJob(@PathVariable Long id){
        subscriptionJobsService.delete(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/subscriptions/jobs")
    public SubscriptionJobsDTO update(@RequestBody SubscriptionJobVM job){
        log.debug("Rest request to Updating Subscription Registered Task(Jobs) run time, Job id : {}", job.getId());
        return subscriptionJobsService.updateRunTime(job);
    }
}
