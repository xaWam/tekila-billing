package spring.service;


import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.module.system.log.SystemLogRecord;
import com.jaravir.tekila.module.system.log.SystemLogger;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import spring.controller.LoggingResource;

import javax.ejb.EJB;
import java.util.List;

import static spring.util.Constants.INJECTION_POINT;

@Service
public class LoggingService {

    @EJB(mappedName = INJECTION_POINT+"SystemLogger")
    private SystemLogger systemLogger;


    private static final Logger log = org.slf4j.LoggerFactory.getLogger(LoggingService.class);

    public List<SystemLogRecord> findAllLogsByAgreement(String agreement, int startPage, int endPage){

        List<SystemLogRecord> systemLogRecords = systemLogger.getSystemLogRecordsByAgreement(agreement, startPage, endPage);

        for (SystemLogRecord systemLogRecord: systemLogRecords
             ) {
            log.debug("  00000    => "+systemLogRecord.toString());
        }


        return systemLogRecords;
    }

}
