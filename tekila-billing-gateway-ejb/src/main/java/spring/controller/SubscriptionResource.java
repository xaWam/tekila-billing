package spring.controller;

import org.apache.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.controller.util.HeaderUtil;

import spring.controller.vm.SubscriptionCreationVM;
import spring.dto.SubscriptionDTO;
import spring.exceptions.BadRequestAlertException;
import spring.service.SubscriptionService;


@RestController
//@RequestMapping("/api")
public class SubscriptionResource {
    private static final Logger log = Logger.getLogger(SubscriptionResource.class);

    private static final String ENTITY_NAME = "subscription";

    private final SubscriptionService subscriptionService;

    public SubscriptionResource(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }


    @PostMapping("/subscriptionss")
    public Long createSubscription(@RequestBody SubscriptionCreationVM subscriptionCreationVM) throws BadRequestAlertException {
        log.debug("Post request for creating Subscription: ");
        log.debug(subscriptionCreationVM.toString());
//        return subscriptionService.save(subscriptionCreationVM);
        return subscriptionService.saveNewSubscription(subscriptionCreationVM); //alpha version - will work for all providers
    }

    @PostMapping("/bbtvnakhchivansubscriptionss")
    public Long createBBTVNakhchivanSubscription(@RequestBody SubscriptionCreationVM subscriptionCreationVM) throws BadRequestAlertException {
        log.debug("Post request for creating Subscription: ");
        return subscriptionService.saveBBTVNkhchivan(subscriptionCreationVM);
    }


    @PostMapping("/subscriptionss2")
    public ResponseEntity<Void> createSubscription2(@RequestBody SubscriptionCreationVM subscriptionCreationVM) throws BadRequestAlertException {
        log.debug("Post request for creating Subscription 2: ");
        log.info(subscriptionService.save(subscriptionCreationVM));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/subscriptionss")
    public SubscriptionDTO update(@RequestBody SubscriptionDTO subscriptionDTO){
        SubscriptionDTO result = subscriptionService.update(subscriptionDTO);
        return result;
    }

    @DeleteMapping("/subscriptionss/{id}")
    public ResponseEntity<Void> removeSubscription(@PathVariable Long id){
        subscriptionService.delete(id);
        return ResponseEntity.ok().build();
    }


    /**
     *   just for testing what kind of object I need to transfer for creating
     * */
    @GetMapping("/subscriptionss/vm/test")
    public SubscriptionCreationVM testIt(){
        return new SubscriptionCreationVM();
    }


//    @GetMapping("/search/subscription")
//    public List<SubscriptionDTO> searchSubscription(@RequestParam(required=false) String name, @RequestParam(required=false) String subscriptionIndex){
//        return subscriptionService.findAllByCustomCriteria(name, subscriptionIndex);
//    }

//    @PostMapping("/subscriptionss/with-headers")
//    public ResponseEntity<Void> createSubscriptionH(@RequestBody SubscriptionCreationVM subscriptionCreationVM) throws BadRequestAlertException {
//        subscriptionService.save(subscriptionCreationVM);
//        return ResponseEntity.ok().headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, null)).body(null);
//    }


    /**** it doesn't need yet ****
    @PostMapping("/subscriptionss/global")
    public Long createGlobalSubscription(@RequestBody SubscriptionCreationVM subscriptionCreationVM) throws BadRequestAlertException {
        log.debug("Post request for creating Subscription global..: ");
        return subscriptionService.saveNewSubscription(subscriptionCreationVM);
    }
    */
}
