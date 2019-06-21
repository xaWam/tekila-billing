package spring.controller;


import com.jaravir.tekila.provision.broadband.devices.MinipopCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import spring.controller.vm.SubscriptionMinipopVM;
import spring.dto.MiniPopDTO;
import spring.service.MiniPopService;

import java.util.List;

/**
 * @author Gurban Azimli
 * @date 19/02/2019 10:58 AM
 */
@RestController
public class MiniPopResource {

    private static final Logger log = LoggerFactory.getLogger(MiniPopResource.class);

    private MiniPopService miniPopService;

    public MiniPopResource(MiniPopService miniPopService) {
        this.miniPopService = miniPopService;
    }

    @GetMapping("/minipops")
    public List<MiniPopDTO> findAllMiniPops(){
        log.info("Rest request for all MiniPops");
        return miniPopService.findAllMiniPops();
    }

    @GetMapping("/providers/minipops/{providerId}")
    public List<MiniPopDTO> findAllMiniPopsByProviderId(@PathVariable("providerId") Long providerId){
        log.info("Rest request for MiniPops for provider by id {}: " ,  providerId);
        return miniPopService.findAllMiniPopsByProvider(providerId);
    }

    @GetMapping("/pagination/minipops")
    public List<MiniPopDTO> findAllPaginated(@RequestParam(required = false) String macAddress,
                                             @RequestParam(required = false) String ip,
                                             @RequestParam(required = false) String switch_id,
                                             @RequestParam(required = false) String address,
                                             @RequestParam(required = false) MinipopCategory category,
                                             @RequestParam(required = false, defaultValue = "1") Integer pageId,
                                             @RequestParam(required = false, defaultValue = "20") Integer rowsPerPage){

        log.info("~~~Rest request for all minipops by pagination~~~");
        return miniPopService.findAllPaginated(macAddress, ip, switch_id, address, category, pageId, rowsPerPage);
    }


    @GetMapping("/pagination/providers/{providerId}/minipops")
    public List<MiniPopDTO> findAllPaginatedByProviderId(@PathVariable Long providerId,
                                                         @RequestParam(required = false) String macAddress,
                                                         @RequestParam(required = false) String ip,
                                                         @RequestParam(required = false) String switch_id,
                                                         @RequestParam(required = false) String address,
                                                         @RequestParam(required = false) MinipopCategory category,
                                                         @RequestParam(required = false, defaultValue = "1") Integer pageId,
                                                         @RequestParam(required = false, defaultValue = "20") Integer rowsPerPage){

        /**
         * TODO :
         */
        return miniPopService.findPaginatedByProviderId(providerId, macAddress, ip, switch_id, address, category, pageId, rowsPerPage);

    }

    @PutMapping("/subscriptions/minipops")
    public void setNextAvailablePort(@RequestBody SubscriptionMinipopVM subscriptionMiniPopVM){

        log.info("~~~~~" +subscriptionMiniPopVM.toString()+"~~~~"  );

        log.debug("~~~Rest Request for updating subscription's MiniPop~~~");
        miniPopService.setNextAvailablePort(subscriptionMiniPopVM);
    }
    @GetMapping("/pagination/providers/{providerId}/minipops/count")
    public Long findAllPaginatedByProviderIdCount(@PathVariable Long providerId,
                                                         @RequestParam(required = false) String macAddress,
                                                         @RequestParam(required = false) String ip,
                                                         @RequestParam(required = false) String switch_id,
                                                         @RequestParam(required = false) String address,
                                                         @RequestParam(required = false) MinipopCategory category){
        return miniPopService.findPaginatedByProviderIdCount(providerId, macAddress, ip, switch_id, address, category);

    }

}


