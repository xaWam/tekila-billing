package spring.controller;

import org.apache.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.controller.util.HeaderUtil;
import spring.dto.AtsDTO;
import spring.service.AtsService;
import java.util.List;

@RestController
//@RequestMapping("/api")
public class AtsController {
    private static final Logger log = Logger.getLogger(AtsController.class);

    private static final String ENTITY_NAME = "ats";

    private final AtsService atsService;

    public AtsController(AtsService atsService) {
        this.atsService = atsService;
    }


    @PostMapping("/ats")
    public AtsDTO createAts(@RequestBody AtsDTO atsDTO){
        AtsDTO result = atsService.save(atsDTO);
        return result;
    }

    @PutMapping("/ats")
    public AtsDTO update(@RequestBody AtsDTO atsDTO){
        AtsDTO result = atsService.update(atsDTO);
        return result;
    }

    @DeleteMapping("/ats/{id}")
    public ResponseEntity<Void> removeAts(@PathVariable Long id){
        atsService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/ats")
    public List<AtsDTO> getAllAts() {
        return atsService.findAll();
        }

    @GetMapping("/ats/{id}")
    public AtsDTO getAts(@PathVariable Long id){
        return atsService.find(id);
    }

    @GetMapping("/search/ats")
    public List<AtsDTO> searchAts(@RequestParam(required=false) String name, @RequestParam(required=false) String atsIndex){
        return atsService.findAllByCustomCriteria(name, atsIndex);
    }



    @PostMapping("/ats/with-headers")
    public ResponseEntity<Void> createAtsH(@RequestBody AtsDTO atsDTO){
        atsService.save(atsDTO);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, null)).body(null);
    }
}
