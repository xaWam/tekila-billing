package spring.controller;

import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionResourceBucket;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionResourcePersistenceFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import spring.dto.SubscriptionResourceBucketDTO;
import spring.dto.SubscriptionResourceDTO;
import spring.mapper.subscription.SubscriptionResourceMapper;
import spring.service.SubscriptionResourceService;

import javax.ejb.EJB;

import java.util.List;

/**
 * @author GurbanAz
 * @date 01/02/2019 11:32 AM
 */

@RestController
public class ResourceController {


    private static final Logger log = LoggerFactory.getLogger(ResourceController.class);


    private SubscriptionResourceService service;

    public ResourceController(SubscriptionResourceService service) {
        this.service = service;
    }

    @GetMapping("/subscription/{id}/subscriptionResource")
    public List<SubscriptionResourceDTO> findSubscriptionResourceBySubscriptionId(@PathVariable("id") Long id){
        log.info("Rest request for Resource of Subscription by id : {}", id);
        return service.findSubscriptionResourceBySubscriptionId(id);
    }

    @GetMapping("/resourceBucket/{id}")
    public SubscriptionResourceBucketDTO findResourceBucketById(@PathVariable Long id){
        log.info("Rest request for SubscriptionBucket by id : {}",  id);
        return service.findResourceBucketById(id);

    }

    @PutMapping("/resourceBucket")
    public SubscriptionResourceBucket updateSubscriptionResourceBucketById(@RequestBody SubscriptionResourceBucketDTO subscriptionResourceBucketDTO){
        log.info("Rest request for updating ResourceBucket with id : {}", subscriptionResourceBucketDTO.getId());
        return service.updateSubscriptionResourceBucket(subscriptionResourceBucketDTO);
    }

    @DeleteMapping("/resourceBucket/{id}")
    public void deleteResourceBucketById(@PathVariable Long id){
        log.info("Rest request for deleting resource with id : {}",id);
        service.deleteResourceBucketById(id);
    }

}
