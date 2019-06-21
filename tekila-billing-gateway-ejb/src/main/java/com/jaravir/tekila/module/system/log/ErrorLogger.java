package com.jaravir.tekila.module.system.log;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.accounting.entity.Transaction;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.operation.OperationResult;
import org.apache.log4j.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by khsadigov on 22.08.2016.
 */
@Stateless
public class ErrorLogger extends AbstractPersistenceFacade<ErrorLogRecord> {
    @PersistenceContext
    private EntityManager em;
    @Resource
    private SessionContext ctx;

    private final static Logger log = Logger.getLogger(ErrorLogger.class);

    public ErrorLogger() {
        super(ErrorLogRecord.class);
    }

    public EntityManager getEntityManager() {
        return em;
    }

    public void create(Exception ex) {

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);

        ErrorLogRecord record = new ErrorLogRecord();
        record.setClassName(ex.getStackTrace()[0].getClassName());
        record.setMethodName(ex.getStackTrace()[0].getMethodName());
        record.setLine(ex.getStackTrace()[0].getLineNumber());
        record.setMessage(ex.getMessage());
        record.setInfo(sw.toString());

        save(record);


    }


}
