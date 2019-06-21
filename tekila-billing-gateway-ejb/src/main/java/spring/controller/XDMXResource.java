package spring.controller;

import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.service.XDMXService;

import java.io.File;
import java.util.List;

/**
 * @author GurbanAz
 * @date 11/04/2019
 */

@RestController
public class XDMXResource {


    private static final Logger log = LoggerFactory.getLogger(XDMXResource.class);

    @Autowired
    private XDMXService xdmxService;

    @GetMapping("/xdmx/report")
    public List<File> getAllReport(){

        return xdmxService.getAllReport();
    }

    @GetMapping("/xdmx/count/test")
    public Long count(){
        return xdmxService.count();
    }

   

}
