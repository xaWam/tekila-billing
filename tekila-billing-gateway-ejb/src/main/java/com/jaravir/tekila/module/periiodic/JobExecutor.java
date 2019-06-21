package com.jaravir.tekila.module.periiodic;

import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.engines.OperationsEngine;
import com.jaravir.tekila.engines.VASCreationParams;
import com.jaravir.tekila.module.campaign.CampaignRegisterPersistenceFacade;
import com.jaravir.tekila.module.periodic.Job;
import com.jaravir.tekila.module.periodic.JobProperty;
import com.jaravir.tekila.module.periodic.JobPropertyType;
import com.jaravir.tekila.module.periodic.JobStatus;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.service.persistence.manager.ServicePersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.VASPersistenceFacade;
import com.jaravir.tekila.module.store.RangePersistenceFacade;
import com.jaravir.tekila.module.store.ip.persistence.IpAddress;
import com.jaravir.tekila.module.store.ip.persistence.IpAddressRange;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionVAS;
import com.jaravir.tekila.module.subscription.persistence.entity.transition.StatusChangeRule;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sajabrayilov on 5/4/2015.
 */
@DeclareRoles({"system"})
@RunAs("system")
@Singleton
public class JobExecutor {
    @EJB private SystemLogger systemLogger;
    @EJB private JobPersistenceFacade jobFacade;
    @EJB private SubscriptionPersistenceFacade subscriptionFacade;
    @EJB private EngineFactory provisioningFactory;
    @EJB private ServicePersistenceFacade servicePersistenceFacade;
    @EJB private VASPersistenceFacade vasPersistenceFacade;
    @EJB private CampaignRegisterPersistenceFacade campaignRegisterFacade;
    @EJB private RangePersistenceFacade rangePersistenceFacade;
    @EJB private EngineFactory engineFactory;

    private final static Logger log = LoggerFactory.getLogger(JobExecutor.class);

    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */ // @Schedule (hour = "*", minute = "*/30")
    public void executeJobs () {
        log.info("executeJobs: starting");
        List<Job> jobList = jobFacade.findCurrentJobs();
        log.info(String.format("executeJobs: found %d jobs", jobList != null ? jobList.size() : 0));

        long start = System.currentTimeMillis();
        if (jobList != null && !jobList.isEmpty()) {
            for (Job job : jobList) {
                try {
                    log.info("executeJobs: executing job id=" + job.getId());

                    switch (job.getCategory()) {
                        case SUBSCRIPTION_STATUS_CHANGE:
                            changeStatus(job);
                            break;
                        case SUBSCRIPTION_SERVICE_CHANGE:
                            changeService(job);
                            break;
                        case SUBSCRIPTION_CAMPAIGN_ADD:
                            addCampaign(job);
                            break;
                        case SUBSCRIPTION_VAS_ADD:
                            addVas(job);
                            break;
                    }

                    job.setStatus(JobStatus.SUCCESS);
                    String msg = String.format("Job id=%d executed successfully", job.getId());
                    log.info(msg);
                    systemLogger.success(SystemEvent.JOB_EXECUTION, null, msg);
                }
                catch (Exception ex) {
                    job.setStatus(JobStatus.FAILED);
                    String msg = String.format("Cannot execute job id=%d", job.getId());
                    log.error(msg, ex);
                    systemLogger.error(SystemEvent.JOB_EXECUTION, null, msg);
                }
            }
        }

        log.info("executeJobs  elapsed time : {}",(System.currentTimeMillis()-start)/1000);
        log.info("executeJobs: finishing");
    }

    public void changeStatus (Job job) {
        SubscriptionStatus initStatus = null;
        SubscriptionStatus targetStatus = null;
        Long subscriptionID = null;
        Long vasId = null;
        DateTime vasExpirationDate = null;

        log.debug("changeStatus: properties size " + job.getPropertyList().size());

        for (JobProperty property : job.getPropertyList()) {
            switch (property.getType()) {
                case INITIAL_STATUS:
                    initStatus = SubscriptionStatus.valueOf(property.getValue());
                    break;
                case FINAL_STATUS:
                    targetStatus = SubscriptionStatus.valueOf(property.getValue());
                    break;
                case TARGET:
                    subscriptionID = Long.valueOf(property.getValue());
                    break;
                case VAS_ID:
                    vasId = Long.valueOf(property.getValue());
                    break;
                case VAS_EXPIRATION_DATE:
                    vasExpirationDate =
                            DateTimeFormat.forPattern(jobFacade.DATETIME_FORMAT).parseDateTime(property.getValue());
                    break;
            }
        }

        log.debug(String.format("changeStatus: subscription id=%d, fromStatus=%s, targetSatus=%s, vas id=%d",
                subscriptionID, initStatus, targetStatus, (vasId != null ? vasId : -1)));
        Subscription subscription = subscriptionFacade.find(subscriptionID);

        if (targetStatus == SubscriptionStatus.SUSPENDED) {
            List<ValueAddedService> suspensionVasList = vasPersistenceFacade.findSuspensionVasListBySubscription(subscription);
            if (suspensionVasList != null) {
                for (final ValueAddedService vas : suspensionVasList) {
                    subscription = engineFactory.getOperationsEngine(subscription).addVAS(
                            new VASCreationParams.Builder().
                                    setSubscription(subscription).
                                    setValueAddedService(vas).
                                    setStartDate(DateTime.now()).
                                    setExpiresDate(vasExpirationDate).
                                    build()
                    );
                }
            }
        } else if (subscription.getStatus() == SubscriptionStatus.SUSPENDED) {
            List<ValueAddedService> suspensionVasList = vasPersistenceFacade.findSuspensionVasListBySubscription(subscription);
            if (suspensionVasList != null) {
                for (final ValueAddedService vas : suspensionVasList) {
                    SubscriptionVAS sbnVas = subscription.getVASByServiceId(vas.getId());
                    if (sbnVas != null) {
                        engineFactory.getOperationsEngine(subscription).
                                removeVAS(subscription, sbnVas);
                    }
                }
            }
        }

        List<StatusChangeRule> rules = subscription.getService().getStatusChangeRules();

        if (!job.isCounter()) { //vas should be added here
            if (vasId != null && targetStatus != SubscriptionStatus.SUSPENDED) {
                ValueAddedService vas = vasPersistenceFacade.find(vasId);
                subscription = engineFactory.getOperationsEngine(subscription).addVAS(
                        new VASCreationParams.Builder().
                                setSubscription(subscription).
                                setValueAddedService(vas).
                                setStartDate(DateTime.now()).
                                setExpiresDate(vasExpirationDate).
                                build()
                );
            }
        }

        //if (!rules.isEmpty() || job.isCounter()) { //change status
        {
            subscription = engineFactory.getOperationsEngine(subscription).changeStatus(subscription, targetStatus);
            log.debug(String.format("changeStatus: subscription id = %d, agreement = %s, status = %s", subscription.getId(),
                    subscription.getAgreement(), subscription.getStatus()));
        }

        if (job.isCounter()) //vas will be finalized here
            counterChangeStatus(job, subscription);
        subscriptionFacade.update(subscription);
    }

    private void counterChangeStatus (Job job, Subscription subscription) {
        log.debug(String.format("counterChangeStatus: starting... subscription id = %d, agreement = %s, status = %s", subscription.getId(),
                subscription.getAgreement(), subscription.getStatus()));

        StatusChangeRule rule = null;

        JobProperty startStatusProperty = null;
        JobProperty endStatusProperty = null;
        JobProperty vasIDProperty = null;

        for (JobProperty prop : job.getPropertyList()) {
            if (prop.getType() == JobPropertyType.INITIAL_STATUS) {
                startStatusProperty = prop;
            }
            else if (prop.getType() == JobPropertyType.FINAL_STATUS) {
                endStatusProperty = prop;
            }
            else if (prop.getType() == JobPropertyType.VAS_ID) {
                vasIDProperty = prop;
            }
        }

        if (startStatusProperty == null || endStatusProperty == null)
            return;

        //find initial status change rule to finanlize its service
        rule = subscription.getService().findRule(SubscriptionStatus.valueOf(endStatusProperty.getValue()),
                SubscriptionStatus.valueOf(startStatusProperty.getValue()));

        if (rule != null) {
            ValueAddedService vas = rule.getVas();

            if (vasIDProperty != null) {
                Long vasId = Long.parseLong(vasIDProperty.getValue());
                SubscriptionVAS sbnVAS = subscription.getLastVASByServiceID(vasId);

                if (sbnVAS != null) {
                    SubscriptionStatus vasStatus = sbnVAS.getStatus();
                    String msg = String.format("sbn vas id=%d, vas id=%d chaanged from status=%s", sbnVAS.getId(), sbnVAS.getVas().getId(), vasStatus);
                    sbnVAS.setStatus(SubscriptionStatus.FINAL);
                    log.info("couterStatusChange: " + msg);
                    systemLogger.success(SystemEvent.VAS_STATUS_FINAL, subscription, msg);
                }
            }
        }

        //if there is a rule for charging for this transition, find it
        StatusChangeRule changeRule = subscription.getService().findRule(SubscriptionStatus.valueOf(startStatusProperty.getValue()),
                SubscriptionStatus.valueOf(endStatusProperty.getValue()));

        if (changeRule != null) {
            ValueAddedService changeVAS = changeRule.getVas();

            if (changeVAS != null) {
                engineFactory.getOperationsEngine(subscription).addVAS(
                        new VASCreationParams.Builder().
                                setSubscription(subscription).
                                setValueAddedService(changeVAS).
                                build()
                );
            }
        }

        log.debug(String.format("counterChangeStatus: finishing... subscription id = %d, agreement = %s, status = %s", subscription.getId(),
                subscription.getAgreement(), subscription.getStatus()));
    }

    public void changeService(Job job) {
        Long subscriptionId = null;
        Long serviceId = null;
        for (final JobProperty prop : job.getPropertyList()) {
            switch (prop.getType()) {
                case TARGET:
                    subscriptionId = Long.valueOf(prop.getValue());
                    break;
                case SUBSCRIPTION_SERVICE_ID:
                    serviceId = Long.valueOf(prop.getValue());
                    break;
            }
        }

        if (subscriptionId == null || serviceId == null) {
            log.error(String.format("Cannot execute job %d. Either subscription id or service id is null", job.getId()));
            return;
        }
        Subscription subscription = subscriptionFacade.find(subscriptionId);
        OperationsEngine operationsEngine = engineFactory.getOperationsEngine(subscription);
        operationsEngine.changeService(subscription, servicePersistenceFacade.find(serviceId), true, true);
    }

    public void addCampaign(Job job) {
        Long subscriptionId = null;
        Long campaignId = null;
        Long paymentId = null;
        for (final JobProperty prop : job.getPropertyList()) {
            switch (prop.getType()) {
                case TARGET:
                    subscriptionId = Long.valueOf(prop.getValue());
                    break;
                case CAMPAIGN_ID:
                    campaignId = Long.valueOf(prop.getValue());
                    break;
                case PAYMENT_ID:
                    paymentId = Long.valueOf(prop.getValue());
                    break;
            }
        }
        if (subscriptionId == null || campaignId == null || paymentId == null) {
            log.error(String.format("Job id = %d cannot be executed, because of insufficient parameters", job.getId()));
        }
        campaignRegisterFacade.tryActivateCampaignOnManualPayment(subscriptionId, campaignId, paymentId);
    }

    public void addVas(Job job) {
        Long subscriptionId = 0L;
        Long vasId = 0L;
        DateTime vasExpirationDate = null;
        String ipAddressStr = null;
        int vasCount = 1;
        for (final JobProperty prop : job.getPropertyList()) {
            switch (prop.getType()) {
                case TARGET:
                    subscriptionId = Long.valueOf(prop.getValue());
                    break;
                case VAS_ID:
                    vasId = Long.valueOf(prop.getValue());
                    break;
                case VAS_EXPIRATION_DATE:
                    vasExpirationDate =
                            DateTimeFormat.forPattern(JobPersistenceFacade.DATETIME_FORMAT).parseDateTime(prop.getValue());
                    break;
                case IP_ADDRESS_STR:
                    ipAddressStr = prop.getValue();
                    break;
                case VAS_COUNT:
                    vasCount = Integer.parseInt(prop.getValue());
                    break;
            }
        }
        Subscription selected = subscriptionFacade.find(subscriptionId);
        ValueAddedService selectedVas = vasPersistenceFacade.find(vasId);

        if (selected == null || selectedVas == null) {
            log.error(String.format("Subscription or vas not found for job id = %d", job.getId()));
        }

        List<IpAddress> freeIpList = new ArrayList<>();
        MiniPop miniPop = subscriptionFacade.findMinipop(selected);
        List<IpAddressRange> ipRanges = rangePersistenceFacade.findRangesByNas(miniPop.getNas());
        for (IpAddressRange range : ipRanges) {
            freeIpList.addAll(range.findFreeAddresses());
        }
        subscriptionFacade.addVAS(
                selected,
                selectedVas,
                vasExpirationDate != null ? vasExpirationDate.toDate() : null,
                ipAddressStr,
                vasCount,
                freeIpList);
    }
}
