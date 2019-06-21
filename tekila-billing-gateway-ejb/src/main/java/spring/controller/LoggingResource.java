package spring.controller;

import com.jaravir.tekila.module.system.log.SystemLogRecord;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import spring.service.LoggingService;

import java.util.List;

@RestController
//@RequestMapping("/logging")
public class LoggingResource {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(LoggingResource.class);

    @Autowired
    LoggingService loggingService;

    @GetMapping("/systemlog/{agreement}")
    public List<SystemLogRecord> getSystemLogData(@PathVariable String agreement,
                                                  @RequestParam Integer startPage,
                                                  @RequestParam Integer count ){
log.debug("started 666");


        return loggingService.findAllLogsByAgreement(agreement, startPage, count);
    }




}
