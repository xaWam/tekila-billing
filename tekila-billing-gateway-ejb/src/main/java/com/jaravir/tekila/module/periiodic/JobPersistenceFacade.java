package com.jaravir.tekila.module.periiodic;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.periodic.*;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionVAS;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by sajabrayilov on 5/4/2015.
 */
@Stateless
public class JobPersistenceFacade extends AbstractPersistenceFacade<Job> {
    @PersistenceContext
    private EntityManager em;
    @Resource
    private EJBContext ctx;
    @EJB
    private UserPersistenceFacade userFacade;
    @EJB
    private SystemLogger systemLogger;
    private final static Logger log = Logger.getLogger(JobPersistenceFacade.class);
    private final static List<JobStatus> statusList = new ArrayList<>();
    private static final long MS_IN_DAY = (24L * 60L * 60L * 1000L);

    static {
        statusList.add(JobStatus.ACTIVE);
        statusList.add(JobStatus.FAILED);
    }

    static final String DATETIME_FORMAT = "MM/dd/yyyy HH:mm:ss";

    public JobPersistenceFacade() {
        super(Job.class);
    }

    public enum Filter implements Filterable {
        PROPERTY_TYPE("property_type"),
        PROPERTY_VALUE("property_value");

        private final String field;
        private MatchingOperation operation;

        Filter(String field) {
            this.field = field;
            this.operation = MatchingOperation.EQUALS;
        }

        public String getField() {
            return field;
        }

        @Override
        public MatchingOperation getOperation() {
            return operation;
        }

        public void setOperation(MatchingOperation operation) {
            this.operation = operation;
        }
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public List<Job> findCurrentJobs() {
        return em.createQuery("select j from Job j where j.startTime <= current_timestamp and j.deadline >= current_timestamp and j.status in :stat  order by j.id")
                .setParameter("stat", statusList).getResultList();
    }

    public Job findActualJob(Subscription subscription) {
        try {
            DateTime now = DateTime.now();
            DateTime yesterday = now.minusDays(1);
            DateTime after = now.plusDays(5);
            return em.createQuery("select j from Job j join j.propertyList p " +
                    "where p.type = :propertyType and p.value = :propertyValue and " +
                    "j.status <> :jobStatus and j.startTime >= :starttime and " +
                    "j.startTime <= :endtime", Job.class)
                    .setParameter("propertyType", JobPropertyType.TARGET)
                    .setParameter("propertyValue", String.valueOf(subscription.getId()))
                    .setParameter("jobStatus", JobStatus.SUCCESS)
                    .setParameter("starttime", yesterday)
                    .setParameter("endtime", after)
                    .getSingleResult();
        } catch (Exception ex) {
            return null;
        }
    }

    public List<Job> findActualJobs(Subscription subscription) {
        try {
            DateTime now = DateTime.now();
            DateTime yesterday = now.minusDays(1);
            DateTime after = now.plusDays(5);
            return em.createQuery("select j from Job j join j.propertyList p " +
                    "where p.type = :propertyType and p.value = :propertyValue and " +
                    "j.status <> :jobStatus and j.startTime >= :starttime and " +
                    "j.startTime <= :endtime", Job.class)
                    .setParameter("propertyType", JobPropertyType.TARGET)
                    .setParameter("propertyValue", String.valueOf(subscription.getId()))
                    .setParameter("jobStatus", JobStatus.SUCCESS)
                    .setParameter("starttime", yesterday)
                    .setParameter("endtime", after)
                    .getResultList();
        } catch (Exception ex) {
            return null;
        }
    }

    public List<Job> findSubscriptionJobs(Long subscriptionId) {
        try {
            return em.createQuery("select j from Job j join j.propertyList p " +
                    "where p.type = :propertyType and p.value = :propertyValue ", Job.class)
                    .setParameter("propertyType", JobPropertyType.TARGET)
                    .setParameter("propertyValue", String.valueOf(subscriptionId))
                    .getResultList();
        } catch (Exception ex) {
            return null;
        }
    }

    //            jobPersistenceFacade.clearFilters();
//            String sqlQuery = "SELECT DISTINCT j FROM Job j JOIN j.propertyList plist "
//                    + "WHERE plist.type = :property_type and plist.value = :property_value";
//            jobPersistenceFacade.addFilter(JobPersistenceFacade.Filter.PROPERTY_VALUE, String.valueOf(subscriptionId));
//            jobPersistenceFacade.addFilter(JobPersistenceFacade.Filter.PROPERTY_TYPE, JobPropertyType.TARGET);
//            jobPersistenceFacade.find
//        return ;

    public void createStatusChangeJob(
            Subscription subscription,
            DateTime startDate,
            SubscriptionStatus fromStatus,
            SubscriptionStatus newStatus,
            Long vasId,
            DateTime expiresDate,
            boolean counter
    ) {
        createSingleStatusChangeJob(subscription, startDate, fromStatus, newStatus, vasId, expiresDate, counter);
    }

    private Job createSingleStatusChangeJob(
            Subscription subscription,
            DateTime startDate,
            SubscriptionStatus fromStatus,
            SubscriptionStatus newStatus,
            Long vasId,
            DateTime expiresDate,
            boolean counter) {
        User user = userFacade.findByUserName(ctx.getCallerPrincipal().getName());


        Job job = new Job();
        job.setCategory(JobCategory.SUBSCRIPTION_STATUS_CHANGE);
        job.setStartTime(startDate);
        job.setDeadline(startDate.plusDays(1));
        job.setStatus(JobStatus.ACTIVE);
        job.setUser(user);

        if (counter)
            job.setCounter(counter);

        JobProperty property = new JobProperty();
        property.setType(JobPropertyType.TARGET);
        property.setValue(String.valueOf(subscription.getId()));
        job.addProperty(property);

        JobProperty initStatusProperty = new JobProperty();
        initStatusProperty.setType(JobPropertyType.INITIAL_STATUS);
        initStatusProperty.setValue(fromStatus.toString());
        job.addProperty(initStatusProperty);

        JobProperty finalStatusProperty = new JobProperty();
        finalStatusProperty.setType(JobPropertyType.FINAL_STATUS);
        finalStatusProperty.setValue(newStatus.toString());
        job.addProperty(finalStatusProperty);

        if (vasId != null) {
            JobProperty vasProperty = new JobProperty();
            vasProperty.setType(JobPropertyType.VAS_ID);
            vasProperty.setValue(String.valueOf(vasId));
            job.addProperty(vasProperty);
        }

        if (expiresDate != null) {
            JobProperty vasExpirationDate = new JobProperty();
            vasExpirationDate.setType(JobPropertyType.VAS_EXPIRATION_DATE);
            vasExpirationDate.setValue(expiresDate.toString(DateTimeFormat.forPattern(DATETIME_FORMAT)));
            job.addProperty(vasExpirationDate);
        }
        save(job);
        String msg = String.format("created status change job for subscription id=%d, job id=%d, from status=%s, new status=%s", subscription.getId(), job.getId(), fromStatus, newStatus);
        log.info("addChangeStatus: " + msg);
        systemLogger.success(SystemEvent.JOB_ADDED, subscription, msg);

        return job;
    }

    public void createCampaignAddJob(
            Subscription subscription,
            Long campaignId,
            Long paymentId,
            DateTime campaignAddStartTime) {
        User user = userFacade.findByUserName(ctx.getCallerPrincipal().getName());

        Job job = new Job();
        job.setCategory(JobCategory.SUBSCRIPTION_CAMPAIGN_ADD);
        job.setStartTime(campaignAddStartTime);
        job.setDeadline(campaignAddStartTime.plusDays(1));
        job.setStatus(JobStatus.ACTIVE);
        job.setUser(user);
        job.setCounter(false);

        JobProperty sProperty = new JobProperty();
        sProperty.setType(JobPropertyType.TARGET);
        sProperty.setValue(String.valueOf(subscription.getId()));
        job.addProperty(sProperty);

        JobProperty cProperty = new JobProperty();
        cProperty.setType(JobPropertyType.CAMPAIGN_ID);
        cProperty.setValue(String.valueOf(campaignId));
        job.addProperty(cProperty);

        JobProperty pProperty = new JobProperty();
        pProperty.setType(JobPropertyType.PAYMENT_ID);
        pProperty.setValue(String.valueOf(paymentId));
        job.addProperty(pProperty);

        save(job);
        String msg = String.format("created campaign add job for subscription id=%d, job id=%d, campaign id=%d", subscription.getId(), job.getId(), campaignId);
        log.info("addCampaignJob: " + msg);
        systemLogger.success(SystemEvent.JOB_ADDED, subscription, msg);
    }

    public void createServiceChangeJob(Subscription selected, Service targetService, Date startDate) {
        User user = userFacade.findByUserName(ctx.getCallerPrincipal().getName());

        Job job = new Job();
        job.setCategory(JobCategory.SUBSCRIPTION_SERVICE_CHANGE);
        job.setStartTime(new DateTime(startDate));
        job.setDeadline(job.getStartTime().plusHours(12));
        job.setStatus(JobStatus.ACTIVE);
        job.setUser(user);

        {
            //set subscription id
            JobProperty sbnProperty = new JobProperty();
            sbnProperty.setType(JobPropertyType.TARGET);
            sbnProperty.setValue(String.valueOf(selected.getId()));
            job.addProperty(sbnProperty);
        }

        {
            //set target service id
            JobProperty serviceProperty = new JobProperty();
            serviceProperty.setType(JobPropertyType.SUBSCRIPTION_SERVICE_ID);
            serviceProperty.setValue(String.valueOf(targetService.getId()));
            job.addProperty(serviceProperty);
        }

        save(job);
        String msg = String.format("job id = %d, subscription id = %d, from service_id = %d, to service_id = %d",
                job.getId(), selected.getId(), selected.getService().getId(), targetService.getId());
        log.info(msg);
        systemLogger.success(SystemEvent.JOB_ADDED, selected, msg);
    }

    public void createVasAddJob(
            Subscription selected,
            Long vasId,
            Date vasExpirationDate,
            String ipAddressString,
            double vasAddCount,
            DateTime vasAddDate
    ) {
        log.info(String.format("creating vas add job for subscription id = %d. vas id = %d.", selected.getId(), vasId));

        User user = userFacade.findByUserName(ctx.getCallerPrincipal().getName());

        Job job = new Job();
        job.setCategory(JobCategory.SUBSCRIPTION_VAS_ADD);
        job.setStartTime(vasAddDate);
        job.setDeadline(vasAddDate.plusDays(1));
        job.setStatus(JobStatus.ACTIVE);
        job.setUser(user);
        job.setCounter(false);

        JobProperty sProperty = new JobProperty();
        sProperty.setType(JobPropertyType.TARGET);
        sProperty.setValue(String.valueOf(selected.getId()));
        job.addProperty(sProperty);

        JobProperty vProperty = new JobProperty();
        vProperty.setType(JobPropertyType.VAS_ID);
        vProperty.setValue(String.valueOf(vasId));
        job.addProperty(vProperty);

        if (vasExpirationDate != null) {
            JobProperty vasExpProperty = new JobProperty();
            vasExpProperty.setType(JobPropertyType.VAS_EXPIRATION_DATE);
            vasExpProperty.setValue(new DateTime(vasExpirationDate).toString(DateTimeFormat.forPattern(DATETIME_FORMAT)));
            job.addProperty(vasExpProperty);
        }

        if (ipAddressString != null) {
            JobProperty ipAddressProperty = new JobProperty();
            ipAddressProperty.setType(JobPropertyType.IP_ADDRESS_STR);
            ipAddressProperty.setValue(ipAddressString);
            job.addProperty(ipAddressProperty);
        }

        JobProperty vasCountProperty = new JobProperty();
        vasCountProperty.setType(JobPropertyType.VAS_COUNT);
        vasCountProperty.setValue(String.valueOf(vasAddCount));
        job.addProperty(vasCountProperty);

        save(job);
        String msg = String.format("job id = %d, subscription id = %d, vas id = %d",
                job.getId(), selected.getId(), vasId);
        log.info(msg);
        systemLogger.success(SystemEvent.JOB_ADDED, selected, msg);
    }

    public void removeIt(Long id) {
        Job subscriptionJob = find(id);
        Job forRemove = em.merge(subscriptionJob);
        em.remove(forRemove);
    }
}
