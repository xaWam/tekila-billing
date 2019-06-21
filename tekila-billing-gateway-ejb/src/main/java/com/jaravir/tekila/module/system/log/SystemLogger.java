package com.jaravir.tekila.module.system.log;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.accounting.entity.Transaction;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.operation.OperationResult;
import org.apache.log4j.Logger;
import spring.security.SecurityModuleUtils;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Created by sajabrayilov on 06.02.2015.
 */
@Stateless
public class SystemLogger extends AbstractPersistenceFacade<SystemLogRecord> {
    @PersistenceContext
    private EntityManager em;
    @Resource
    private SessionContext ctx;
    @EJB
    private UserPersistenceFacade userFacade;
    private final static Logger log = Logger.getLogger(SystemLogger.class);

    public SystemLogger() {
        super(SystemLogRecord.class);
    }

    public EntityManager getEntityManager() {
        return em;
    }

    public void create(SystemEvent event, Subscription subscription, Transaction transaction, OperationResult result, String dsc) {
        SystemLogRecord record = new SystemLogRecord();
        String userName = ctx.getCallerPrincipal().getName();
        log.debug("userName "+userName);
        record.setEvent(event);
        record.setUser(userFacade.findByUserName(userName!=null ? userName : SecurityModuleUtils.getCurrentUserLogin()));
        if (subscription != null) {
            record.setSubscription(subscription);
            record.setAgreement(subscription.getAgreement());
            if(subscription.getService() != null) record.setProvider(subscription.getService().getProvider());
        }

        record.setResult(result);

        record.setDsc(createDesc(dsc));
        if (transaction != null)
            record.setTransaction(transaction);

        save(record);
    }

    public void createExP(SystemEvent event, Subscription subscription, Transaction transaction, OperationResult result, String dsc, String userN) {
        SystemLogRecord record = new SystemLogRecord();
        record.setEvent(event);
        record.setUser(userFacade.findByUserName(userN));

        if (subscription != null) {
            record.setSubscription(subscription);
            record.setAgreement(subscription.getAgreement());
            record.setProvider(subscription.getService().getProvider());
        }

        record.setResult(result);

        record.setDsc(createDesc(dsc));

        if (transaction != null)
            record.setTransaction(transaction);

        save(record);
    }


    public void success(SystemEvent event, Subscription subscription, Transaction transaction, String dsc) {
        create(event, subscription, transaction, OperationResult.SUCCESS, dsc);
    }

    public void success(SystemEvent event, Subscription subscription, String dsc) {
        create(event, subscription, OperationResult.SUCCESS, dsc);
    }

    public void successExP(SystemEvent event, Subscription subscription, String dsc, String userN) {
        createExP(event, subscription, null, OperationResult.SUCCESS, dsc, userN);
    }

    public void successExP(SystemEvent event, Subscription subscription, Transaction transaction, String dsc, String userN) {
        createExP(event, subscription, transaction, OperationResult.SUCCESS, dsc, userN);
    }


    public void error(SystemEvent event, Subscription subscription, Transaction transaction, String dsc) {
        create(event, subscription, transaction, OperationResult.FAILURE, dsc);
    }

    public void error(SystemEvent event, Subscription subscription, String dsc) {
        create(event, subscription, OperationResult.FAILURE, dsc);
    }



    public void errorExP(SystemEvent event, Subscription subscription, String dsc, String userN) {
        createExP(event, subscription, null, OperationResult.FAILURE, dsc, userN);
    }

    public void create(SystemEvent event, Subscription subscription, OperationResult result, String dsc) {
        create(event, subscription, null, result, dsc);
    }

    public void logLoginFailure(User user, String userName, String dsc) {
        SystemLogRecord record = new SystemLogRecord();
        record.setEvent(SystemEvent.LOGIN_FAILED);

        if (user != null)
            record.setUser(user);

        record.setDsc(createDesc(dsc));

        save(record);
    }

    private String createDesc(String dsc) {
        return dsc != null && dsc.length() >= 2000 ? dsc.substring(0, 2000) : dsc;
    }

    public void removeSystemLogs(Subscription subscription) {
        log.info(String.format("removing system log records for subscription id = %d.........", subscription.getId()));
        getEntityManager().createQuery("delete from SystemLogRecord s where s.subscription.id = :id")
                .setParameter("id", subscription.getId()).executeUpdate();
        log.info(String.format("removed system log records for subscription id = %d", subscription.getId()));
    }


    public boolean checkOneTimeProlongationIsApplied(String agreement) {
        try {
            getEntityManager().createQuery("select sl from SystemLogRecord sl where sl.event = :evt " +
                    "and sl.agreement = :agr")
                    .setParameter("evt", SystemEvent.SUBSCRIPTION_PROLONGED_FOR_ONE_TIME)
                    .setParameter("agr", agreement)
                    .getSingleResult();
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    public List<SystemLogRecord> getSystemLogRecordsByAgreement(String agreementId, int startPage, int endPage){

//        log.debug(getEntityManager().toString());
//        return  getEntityManager().createQuery("select s1 from SystemLogRecord s1 where s1.agreement = :agree order by s1.id")
//                .setParameter("agree", agreementId)
//                .setFirstResult(startPage)
//                .setMaxResults(endPage)
//                .getResultList();

        return em.createNativeQuery("select * from system_log where agreement='"+agreementId+"' and rownum < 5", SystemLogRecord.class).getResultList();
    }

}
