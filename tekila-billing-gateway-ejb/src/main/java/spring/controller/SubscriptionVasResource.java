package spring.controller;


import com.jaravir.tekila.module.store.ip.persistence.IpAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.controller.vm.SubscriptionVASCreationVM;
import spring.dto.IpAddressDTO;
import spring.dto.SubscriptionVASCreationDTO;
import spring.dto.SubscriptionVASDTO;
import spring.service.SubscriptionVasService;

import java.util.List;

@RestController
public class SubscriptionVasResource {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionVasResource.class);

    private final SubscriptionVasService subscriptionVasService;

    public SubscriptionVasResource(SubscriptionVasService subscriptionVasService) {
        this.subscriptionVasService = subscriptionVasService;
    }

    @PostMapping("/subscriptionVas")
    public SubscriptionVASDTO createSubscriptionVas(@RequestBody SubscriptionVASDTO subscriptionVASDTO){
        log.info("Rest request for creating SubscriptionVAS : {}", subscriptionVASDTO);
        return subscriptionVasService.save(subscriptionVASDTO);
    }

    @PostMapping("/subscriptionVas/add")
    public SubscriptionVASDTO addSubscriptionVas(@RequestBody SubscriptionVASCreationVM subscriptionVASCreationVM){
        log.info("Rest request for creating SubscriptionVAS : {}", subscriptionVASCreationVM);
        return subscriptionVasService.add(subscriptionVASCreationVM);
    }

    @PutMapping("/subscriptionVas")
    public SubscriptionVASDTO updateSubscriptionVas(@RequestBody SubscriptionVASDTO subscriptionVASDTO){
        log.info("Rest request for creating SubscriptionVAS : {}", subscriptionVASDTO);
        return subscriptionVasService.update(subscriptionVASDTO);
    }

    @DeleteMapping("/subscriptionVas/{id}")
    public ResponseEntity<Void> deleteSubscriptionVas(@PathVariable Long id){
        log.info("Rest request for delete SubscriptionVas id : {}", id);
        subscriptionVasService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/subscriptionVas/{id}")
    public SubscriptionVASDTO getOneSubscriptionVas(@PathVariable Long id){
        log.info("Rest request for retrieving One SubscriptionVas id : {}", id);
        return subscriptionVasService.findOne(id);
    }

    @GetMapping("/subscriptions/{id}/subscriptionVas")
    public List<SubscriptionVASDTO> getAllSubscriptionVasBySubscription(@PathVariable Long id) {
        log.info("Rest request for retrieving All SubscriptionVas by Subscription id : {}", id);
        return subscriptionVasService.findAllBySubscription(id);
    }

    @GetMapping("/subscriptions/{id}/ips")
    public List<IpAddressDTO> getIpAdressesBySubscription(@PathVariable Long id){
        log.info("Rest request for retrieving (FREE) IP list by Subscription ID");
        return subscriptionVasService.getFreeIpList(id);
    }

}
