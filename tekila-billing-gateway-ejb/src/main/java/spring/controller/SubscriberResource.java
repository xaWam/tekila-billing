package spring.controller;

import org.apache.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.controller.util.HeaderUtil;
import spring.controller.vm.SubscriberExistVM;
import spring.dto.SubscriberDTO;
import spring.exceptions.BadRequestAlertException;
import spring.service.SubscriberService;

import java.util.List;

@RestController
//@RequestMapping("/api")
public class SubscriberResource {
    private static final Logger log = Logger.getLogger(SubscriberResource.class);

    private static final String ENTITY_NAME = "subscriber";

    private final SubscriberService subscriberService;

    public SubscriberResource(SubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }


    @PostMapping("/subscribers")
    public SubscriberDTO createSubscriber(@RequestBody SubscriberDTO subscriberDTO) throws BadRequestAlertException {
        log.debug("Post request for creating Subscriber: ");
        SubscriberDTO result = subscriberService.save(subscriberDTO);
        return result;
    }

    @PutMapping("/subscribers")
    public SubscriberDTO update(@RequestBody SubscriberDTO subscriberDTO){
        SubscriberDTO result = subscriberService.update(subscriberDTO);
        return result;
    }

    @DeleteMapping("/subscribers/{id}")
    public ResponseEntity<Void> removeSubscriber(@PathVariable Long id){
        subscriberService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/subscribers")
    public List<SubscriberDTO> getAllSubscriber() {
        return subscriberService.findAll();
        }

    @GetMapping("/subscribers/{id}")
    public SubscriberDTO getSubscriber(@PathVariable Long id){
        log.debug("Get request for retrieve subscribers id: "+id);
        return subscriberService.find(id);
    }

//    @GetMapping("/search/subscriber")
//    public List<SubscriberDTO> searchSubscriber(@RequestParam(required=false) String name, @RequestParam(required=false) String subscriberIndex){
//        return subscriberService.findAllByCustomCriteria(name, subscriberIndex);
//    }

    @PostMapping("/subscribers/with-headers")
    public ResponseEntity<Void> createSubscriberH(@RequestBody SubscriberDTO subscriberDTO) throws BadRequestAlertException {
        subscriberService.save(subscriberDTO);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, null)).body(null);
    }

    @PostMapping("/subscribers/is-exist")
    public Boolean isExist(@RequestBody SubscriberExistVM subscriberExistVM){
        return subscriberService.subscriberIsExist(subscriberExistVM);
    }

    @PostMapping("/subscribers/find-matching")
    public List<SubscriberDTO> getAllMatching(@RequestBody SubscriberExistVM subscriberExistVM){
        return subscriberService.findAllMatching(subscriberExistVM);
    }
}
