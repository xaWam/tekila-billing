package com.jaravir.tekila.module.campaign;

import com.jaravir.tekila.module.accounting.listener.BeforeBillingListener;
import com.jaravir.tekila.module.accounting.periodic.BillingManager;
import com.jaravir.tekila.module.service.ResourceBucketType;
import com.jaravir.tekila.module.service.entity.ResourceBucket;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberLifeCycleType;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import org.apache.log4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.annotation.security.RunAs;
import javax.ejb.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by sajabrayilov on 5/29/2015.
 */
@DeclareRoles({"system"})
@RunAs("system")
@Singleton
/**  Tekila JOBS runs on tekila_jobs branch  */ // @Startup
@DependsOn({"BillingManager"})
public class CampaignManager implements BeforeBillingListener {
    @EJB
    private CampaignRegisterPersistenceFacade campaignRegisterFacade;
    @EJB
    private SystemLogger systemLogger;
    private final static Logger log = Logger.getLogger(CampaignManager.class);
    @EJB
    private BillingManager billingManager;

    @EJB
    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;

    @PostConstruct
    public void init() {
        log.debug("adding a before billing listener...");
        billingManager.addBeforeBillingListener(this);
        //throw new RuntimeException("cannot init CampaignManager");
    }

    @Override
    public void beforeBilling(Subscription subscription) throws Exception {
        processPrepaid(subscription);
        activateSuspendedCampaign(subscription);
    }

    @RolesAllowed("system")
    //@Schedule(hour = "6", minute = "0")
//    @Schedule (hour = "*", minute = "*/5")
    public void processCampaignsPrepaid() {
        log.info("processCampaignPrepaid: starting processing prepaid campaign registers");
        List<CampaignRegister> registerList = campaignRegisterFacade.findAllActive(SubscriberLifeCycleType.PREPAID);
        log.info(String.format("processCampaign: Found %d campaign registers", registerList != null ? registerList.size() : 0));
        Long lifeCycleCount = null;

        if (registerList != null) {
            for (CampaignRegister reg : registerList) {
                log.info(String.format("processCampaignsPrepaid: starting processing campaign register id=%d", reg.getId()));
                lifeCycleCount = Long.valueOf(reg.getLifeCycleCount() - 1);

                if (lifeCycleCount <= 0) {
                    try {
                        campaignRegisterFacade.process(reg);
                    } catch (Exception ex) {
                        log.error(String.format("Cannot process campaign: campaign register id=%d", reg.getId()), ex);
                    }
                } else {
                    reg.decrementLifecycleCount();
                }
                log.info(String.format("processCampaignsPrepaid: finished processing campaign register id=%d", reg.getId()));
            }
        }
        log.info("processCampaignPrepaid: finished processing prepaid campaign registers");
    }

    //@RolesAllowed("system")
    //@Schedule(hour = "14", minute = "30")
    public void fixNonActivatedFirstMonthFree() {
        log.info("***************************** PROBLEMDATAPLUS CAMPAIGN FIX STARTED TO ACTIVATE ****************************************");
        List<CampaignRegister> failedCampaignRegister = campaignRegisterFacade.findAllDataplusCampaignProblemNonActivated(SubscriberLifeCycleType.PREPAID);
        log.debug("size of campaign register to go to activation is " + failedCampaignRegister.size());
        try {
            for (CampaignRegister campaignRegister : failedCampaignRegister) {
                try {
                    campaignRegisterFacade.tryActivateCampaign(campaignRegister);
                } catch (Exception e) {
                    log.error("FAILED TO ACTIVATE CAMPAIGN REGISTER DATAPLUS FIRST MONTH FREE " + campaignRegister.getId());
                }
            }
        } catch (Exception e) {
            log.error("FAILED TO ACTIVATE CAMPAIGN REGISTER DATAPLUS FIRST MONTH FREE");
            log.error(e);
        }
        log.info("*****************************DATAPLUS CAMPAIGN PROBLEM FIX ENDED ****************************************");
    }

    //@RolesAllowed("system")
    //@Schedule(hour = "15", minute = "00")
    public void fixFirstMonthProblem() {
        log.info("*****************************DATAPLUS CAMPAIGN PROBLEM FIX STARTED First month ****************************************");
        try {
            List<Subscription> problemmedSubscription = subscriptionPersistenceFacade.findAllDataPlusFirstMonthCampaignProblem();
            List<Subscription> filtered = problemmedSubscription.stream().filter(subscription -> {
                try {
                    return subscription.getCampaignRegisters().stream().filter(campaignRegister ->
                            campaignRegister.getCampaign().getId() == 24526517//first month free
                                    && campaignRegister.getStatus() == CampaignStatus.ACTIVE
                                    && campaignRegister.getProcessedDate() == null).findAny().isPresent() &&
                            subscription.getLastPaymentDate() != null;
                } catch (Exception e) {
                    return false;
                }
            }).collect(Collectors.toList());

            problemmedSubscription = null;
            log.debug("first month free filtered subscriptions size is " + filtered.size());

            for (Subscription subscription : filtered) {
                if (subscription.getStatus() == SubscriptionStatus.BLOCKED && subscription.getBilledUpToDate().isAfterNow() && subscription.getExpirationDate().isBeforeNow()) {

                    processPrepaid(subscription);

                }
            }
        } catch (Exception e) {
            log.error(e);
            log.error("*****************************DATAPLUS CAMPAIGN PROBLEM FIX ENDED First month error ****************************************");
        }
        log.info("*****************************DATAPLUS CAMPAIGN PROBLEM FIX ENDED First month ****************************************");
    }

    public void processPrepaidForAfterBilling(Subscription subscription) {
        campaignRegisterFacade.processPrepaidForAfterBilling(subscription);
    }

    public void processPrepaid(Subscription subscription) {
        log.info("processCampaignPrepaid: starting processing prepaid campaign registers for subscription "+subscription.getId());
        List<CampaignRegister> registerList = campaignRegisterFacade.findActiveForProcessing(subscription);
        log.info(String.format("processCampaignPrepaid: Found %d campaign registers for subscription %s", (registerList != null ? registerList.size() : 0), subscription.getId()));
        Long lifeCycleCount = null;

        if (registerList != null) {
            for (CampaignRegister reg : registerList) {
                log.info(String.format("processCampaignsPrepaid: starting processing campaign register id=%d for subscription %s", reg.getId(), subscription.getId()));
                try {
                    if (reg.getCampaign().isPartialPromoTransfer()) {
                        campaignRegisterFacade.partialProcess(reg);
                    } else {
                        campaignRegisterFacade.process(reg);
                    }
                } catch (Exception ex) {
                    log.error(String.format("Cannot process campaign: campaign register id=%d for subscription %s", reg.getId(), subscription.getId()), ex);
                }
                log.info(String.format("processCampaignsPrepaid: finished processing campaign register id=%d for subscription %s", reg.getId(), subscription.getId()));
            }
        }
        log.info("processCampaignPrepaid: finished processing prepaid campaign registers for subscription "+subscription.getId());
    }

    public void activateSuspendedCampaign(Subscription subscription){
        log.debug("activateSuspendedCampaign starts for subscription "+subscription.getId());
        try {
            List<CampaignRegister> registerList = campaignRegisterFacade.findNotActiveBySubscription(subscription);

            // Hack CityNet 2x speed campaign
            long campaignId = 122L;
            Optional<CampaignRegister> citynet2X = registerList
                    .stream()
                    .filter(r -> r.getCampaign().getId() == campaignId)
                    .findAny();

            if (citynet2X.isPresent()) {
                CampaignRegister register = citynet2X.get();
                register.setStatus(CampaignStatus.ACTIVE);
                register.setLastUpdateDate();
                register.decrementLifecycleCount();
                register.setCampaignNotes("Suspended campaign activated in " + LocalDate.now());
                campaignRegisterFacade.update(register);

                if (register.getBonusAmount() > 0) {
                    subscription.setBucketCapacityByType(ResourceBucketType.INTERNET_DOWN, String.valueOf(register.getBonusAmount()));
//                    subscription.setBucketCapacityByType(ResourceBucketType.INTERNET_UP, String.valueOf(register.getBonusAmount()));
                    subscriptionPersistenceFacade.update(subscription);
                }

                systemLogger.success(SystemEvent.ACTIVATE_SUSPENDED_CAMPAIGN, subscription, String.format("Campaign id %s activated", campaignId));
                log.info(String.format("Suspended campaign [id: %s] is activated for subscription %s", campaignId, subscription.getId()));
            }
        } catch(Exception ex){
            log.error(ex.getMessage() + " -> exception occurs when activate suspended campaign for subscription "+subscription.getId(), ex);
        }
    }

}
