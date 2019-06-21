package spring.controller;

import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import spring.dto.ValueAddedServiceDTO;
import spring.service.VASServices;

import java.util.List;

@RestController
public class VASResource {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(VASResource.class);

    private final VASServices VASServices;

    public VASResource(VASServices VASServices) {
        this.VASServices = VASServices;
    }

    @GetMapping("/vas")
    public List<ValueAddedServiceDTO> getAllVAS() {
        log.info("Rest request for getting all vas list");
        return VASServices.findAll();
    }

    @GetMapping("/vas/{id}")
    public ValueAddedServiceDTO getVAS(@PathVariable Long id) {
        log.info("Rest request for getting vas id : {}", id);
        return VASServices.findOne(id);
    }

    @GetMapping("/providers/{providerId}/vas")
    public List<ValueAddedServiceDTO> getPaginatedVASByProvider(@PathVariable Long providerId,
                                                          @RequestParam(required = false) String name,
                                                          @RequestParam(required = false, defaultValue = "1") Integer pageId,
                                                          @RequestParam(required = false, defaultValue = "20") Integer rowsPerPage){
        log.info("Rest request for retrieving VAS list by provider and other filters");
        return VASServices.findAllPaginatedByProvider(providerId, name, pageId, rowsPerPage);
    }

    @GetMapping("/providers/{providerId}/vas/count")
    public Long getPaginatedVASByProviderCount(@PathVariable Long providerId,
                                          @RequestParam(required = false) String name){
        log.info("Rest request for retrieving Count by provider and other filters");
        return VASServices.findAllPaginatedByProviderCount(providerId, name);
    }
}
