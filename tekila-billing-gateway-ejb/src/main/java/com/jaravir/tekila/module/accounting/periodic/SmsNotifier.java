package com.jaravir.tekila.module.accounting.periodic;

import com.jaravir.tekila.module.campaign.CampaignRegisterPersistenceFacade;
import com.jaravir.tekila.module.campaign.CampaignTarget;
import com.jaravir.tekila.module.event.BillingEvent;
import com.jaravir.tekila.module.queue.PersistentQueueManager;
import com.jaravir.tekila.module.service.ServiceSettingType;
import com.jaravir.tekila.module.service.ValueAddedServiceType;
import com.jaravir.tekila.module.service.entity.ServiceProperty;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.service.model.BillingPrinciple;
import com.jaravir.tekila.module.service.persistence.manager.ServicePropertyPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionVAS;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.provision.broadband.devices.manager.Providers;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kmaharov on 30.01.2017.
 */
@DeclareRoles({"system"})
@RunAs("system")
/**  Tekila JOBS runs on tekila_jobs branch  */ // @Startup
@Singleton
public class SmsNotifier {
    private final static Logger log = LoggerFactory.getLogger(SmsNotifier.class);

    @EJB
    private SubscriptionPersistenceFacade subscriptionFacade;
    @EJB
    private PersistentQueueManager queueManager;
    @EJB
    private ServicePropertyPersistenceFacade servicePropertyPersistenceFacade;
    @EJB
    private CampaignRegisterPersistenceFacade campaignRegisterFacade;

    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */ // @Schedule(hour = "13", minute = "00")
    public void notifyCitynetUsersForDeadline() {
        log.info("Sending notifications for grace subscriptions which are about to be partially blocked started.");

        List<Subscription> subscriptions = subscriptionFacade.findAllExpiredWithSoonPartBlock();
        log.info(String.format("Found %d prepaid subscriptions(grace) for notification", subscriptions.size()));

        long start = System.currentTimeMillis();
        subscriptions.stream().forEach(sub -> {
//            Long serviceFee = sub.getService().getServicePrice();
            log.info("SOON_PARTIAL_BLOCKED. Trying to send notification to "+sub.getAgreement());

            try {
                Long serviceFee = getSubscriptionMonthlyServiceFee(sub, CampaignTarget.SERVICE_RATE, false);
                long promoBalance = (sub.getBalance().getPromoBalance() >= 0 ? sub.getBalance().getPromoBalance() : 0);
                long subscriptionBalance = sub.getBalance().getRealBalance() + promoBalance;
                subscriptionBalance = (subscriptionBalance + 500) / 1000 * 1000; // avoid floating-point number...
                log.info(String.format("[Subscription: %d] -> total balance [%d] < total service fee [%d], condition %b", sub.getId(), subscriptionBalance, serviceFee, subscriptionBalance < serviceFee));
                if (sub.getExpirationDate().isBefore(sub.getExpirationDateWithGracePeriod())
                        && sub.getBillingModel() != null
                        && (sub.getBillingModel().getPrinciple() == BillingPrinciple.GRACE
                        || sub.getBillingModel().getPrinciple() == BillingPrinciple.GRACE_MONTH)
                        && (sub.getService().getProvider().getId() == Providers.CITYNET.getId()
                        || sub.getService().getProvider().getId() == Providers.UNINET.getId())
                        && subscriptionBalance < serviceFee) {
                    log.info(String.format("Sending notification for active -> partial blocked status change. subscription id = %d", sub.getId()));
                    queueManager.sendStatusNotification(BillingEvent.SOON_PARTIAL_BLOCKED, sub.getId());
                } else{
                    log.info(String.format("Sending notification dont sitified condition . subscription id = %d", sub.getId()));
                }
            } catch (Exception ex) {
                log.error(String.format("Cannot send notification: event=%s, subscription id=%d", BillingEvent.SOON_PARTIAL_BLOCKED, sub.getId()), ex);
            }
        });// end FOR LOOP


//        log.info("Sending notifications for grace subscriptions which are about to be blocked started.");
//        subscriptions = subscriptionFacade.findAllExpiredWithGracePrepaidForNotification();
//        log.info(String.format("Found %d expired prepaid subscriptions(grace) for notification: %s", subscriptions.size(), subscriptions.toString()));
//        subscriptions.parallelStream().forEach(sub -> {
//            try {
//                long promobalance = (sub.getBalance().getPromoBalance() >= 0 ? sub.getBalance().getPromoBalance() : 0);
//                if (sub.getExpirationDate().isBefore(sub.getExpirationDateWithGracePeriod())
//                        && sub.getBillingModel() != null
//                        && (sub.getBillingModel().getPrinciple() == BillingPrinciple.GRACE
//                        || sub.getBillingModel().getPrinciple() == BillingPrinciple.GRACE_MONTH)
//                        && (sub.getService().getProvider().getId() == Providers.CITYNET.getId()
//                        || sub.getService().getProvider().getId() == Providers.UNINET.getId())
//                        && (sub.getBalance().getRealBalance() + promobalance <= 0)) {
//                    log.info(String.format("Sending notification for partial blocked -> blocked status change. subscription id = %d", sub.getId()));
//                    queueManager.sendStatusNotification(BillingEvent.SOON_BLOCKED_GRACE, sub.getId());
//                }
//            } catch (Exception ex) {
//                log.error(String.format("Cannot send notification: event=%s, subscription id=%d", BillingEvent.SOON_BLOCKED_GRACE, sub.getId()), ex);
//            }
//        });// end FOR LOOP


        log.info("Sending notifications for continuous subscriptions which are about to be blocked started.");
        subscriptions = subscriptionFacade.findAllExpiredWithBLOCKCOUNTandPARTBLOCKforUNICITY();
        log.info(String.format("Found %d expired prepaid subscriptions(nograce) for notification: %s", subscriptions.size(), subscriptions.toString()));
        subscriptions.parallelStream().forEach(sub ->{
            try {
                Long serviceFee = getSubscriptionMonthlyServiceFee(sub, CampaignTarget.SERVICE_RATE, false);
                long promoBalance = (sub.getBalance().getPromoBalance() >= 0 ? sub.getBalance().getPromoBalance() : 0);
                long subscriptionBalance = sub.getBalance().getRealBalance() + promoBalance;
                subscriptionBalance = (subscriptionBalance + 500) / 1000 * 1000; // avoid floating-point number...
                log.info(String.format("[Subscription: %d] -> total balance [%d] < total service fee [%d], condition %b", sub.getId(), subscriptionBalance, serviceFee, subscriptionBalance < serviceFee));
                if (sub.getBillingModel() != null && sub.getBillingModel().getPrinciple() == BillingPrinciple.CONTINUOUS
                        && (sub.getService().getProvider().getId() == Providers.CITYNET.getId() || sub.getService().getProvider().getId() == Providers.UNINET.getId())
                        && subscriptionBalance < serviceFee) {
                    log.info(String.format("Sending notification for active -> blocked status change. subscription id = %d", sub.getId()));
                    queueManager.sendStatusNotification(BillingEvent.SOON_BLOCKED_CONTINUOUS, sub.getId());
                }
            } catch (Exception ex) {
                log.error(String.format("Cannot send notification: event=%s, subscription id=%d", BillingEvent.SOON_BLOCKED_CONTINUOUS, sub.getId()), ex);
            }
        });// end FOR LOOP

        log.info("notifyCitynetUsersForDeadline  elapsed time : {}", (System.currentTimeMillis()-start)/1000);
    }


    private Long getSubscriptionMonthlyServiceFee(Subscription subscription, CampaignTarget target, boolean isDecrementLyfecycleCount){
        //This logic was got from CitynetBillingManager

        /*
           // getBonusAmount() metodu oz daxilinde systemLogger chagirib 'SUBSCRIPTION_CAMPAIGN_BONUS_USED' edir, ona gore de
           // chagirmaq duzgun deyil, hem SERVICE_RATE kompaniyalarinda logic olaraq bonusAmount null olmalidir, o sebebden hele ki istifade edilmeyecek

        Long rate = campaignRegisterFacade.getBonusAmount(subscription, target, isDecrementLyfecycleCount);
        if (rate == null) {
            Double bonusDiscount = campaignRegisterFacade.getBonusDiscount(subscription);
            if (bonusDiscount != null)
                rate = Double.valueOf(Math.ceil(subscription.getService().getServicePrice() * bonusDiscount)).longValue();
        }
        */

        Long rate = null;
        Double bonusDiscount = campaignRegisterFacade.getBonusDiscount(subscription);
        if (bonusDiscount != null){
            log.info("[subscription: {}] has bonus discount: {}", subscription.getId(), bonusDiscount);
            rate = Double.valueOf(Math.ceil(subscription.getService().getServicePrice() * bonusDiscount)).longValue();
        }

        if (rate == null)
            rate = subscription.getService().getServicePrice();

        rate = subscription.rerate(rate); //Deqiqleshdirmek lazimdi ehtiyac varmi buna

        log.info(String.format("Monthly service fee (without vas) is %d for subscription %d", rate, subscription.getId()));

        rate += getSubscriptionMonthlyVasFee(subscription);

        return rate;
    }

    private long getSubscriptionMonthlyVasFee(Subscription subscription){
        long totalVasRate = 0, vasRate = 0, vasPrice = 0;
        try {
            if (subscription.getVasList() != null && !subscription.getVasList().isEmpty()) {
                log.info(String.format("SmsNotifier -> subscription id=%d, vas count = %s", subscription.getId(), subscription.getVasList().size()));

                Double bonusDiscount = campaignRegisterFacade.getBonusDiscount(subscription);
                List<ValueAddedService> applicableVasList = campaignRegisterFacade.getApplicableVasList(subscription);

                for (SubscriptionVAS vas : subscription.getVasList()) {
                    log.info(String.format("Subscription id=%d, agreement=%s vas id=%d, name=%s, status=%s, type=%s, price=%d",
                            subscription.getId(), subscription.getAgreement(), vas.getId(), vas.getVas().getName(), vas.getStatus(),
                            vas.getVas().getCode().getType(), vas.getVas().getPrice()));

                    if (vas.getVas().getCode().getType() == ValueAddedServiceType.PERIODIC_STATIC) {
                        vasPrice = vas.getVas().getPrice();
                        if ((vas.getRemainCount() != null && vas.getRemainCount() == -99L)
                                || vas.getStatus() == SubscriptionStatus.FINAL
                                || (vas.getExpirationDate() != null && vas.getExpirationDate().isBefore(DateTime.now().plusDays(1))))
                            continue;

                        vasRate = (long)(vasPrice * vas.getCount());
                        if (bonusDiscount != null) {
                            if (applicableVasList != null) {
                                for (ValueAddedService campaignVas : applicableVasList) {
                                    if (campaignVas.getId() == vas.getVas().getId()) {
                                        vasRate = (long) (1.0 * vasRate * bonusDiscount);  //is applicable
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    log.info(String.format("vas price: %d, vas rate: %d for subscription id %d", vasPrice, vasRate, subscription.getId()));
                    totalVasRate += vasRate;
                }
            }
        } catch(Exception ex){
            log.error("Exception occurs while calculate monthly vas fee for subscription " + subscription.getId(), ex);
        }
        log.info(String.format("Total vas price: %d  for subscription id %d", totalVasRate, subscription.getId()));

        return totalVasRate;
    }


    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */ // @Schedule(hour = "14", minute = "00")
    public void notifyDataplusUsersForDeadline() {
        log.info("Starting sending notifications for dataplus subscriptions which are about to be blocked.");
        List<Subscription> subscriptions = subscriptionFacade.findAllExpiredWithContinuousPrepaidForNotification();
        log.info(String.format("Found %d expired prepaid subscriptions(nograce) for notification: %s", subscriptions.size(), subscriptions.toString()));

        long start = System.currentTimeMillis();
//        for (Subscription sub : subscriptions) {
//            try {
//                if (sub.getService().getProvider().getId() != Providers.DATAPLUS.getId()) {
//                    continue;
//                }
//
//                long serviceFee = 0L;
//                ServiceProperty prop = servicePropertyPersistenceFacade.find(
//                        sub.getService(),
//                        sub.getSettingByType(ServiceSettingType.ZONE)
//                );
//                if (prop != null) {
//                    serviceFee = prop.getPrice();
//                }
//                long promobalance = (sub.getBalance().getPromoBalance() >= 0 ? sub.getBalance().getPromoBalance() : 0);
//                if ((sub.getBalance().getRealBalance() + promobalance) < serviceFee) {
//                    log.info(String.format("Sending notification for active -> blocked status change. subscription id = %d", sub.getId()));
//                    queueManager.sendStatusNotification(BillingEvent.SOON_BLOCKED_CONTINUOUS, sub.getId());
//                }
//            } catch (Exception ex) {
//                log.error(String.format("Cannot send notification: event=%s, subscription id=%d", BillingEvent.SOON_BLOCKED_CONTINUOUS, sub.getId()), ex);
//            }
//        }// end FOR LOOP

        //DataPlus SMS event
        subscriptions.stream().filter(sub -> sub.getService().getProvider().getId() == Providers.DATAPLUS.getId()).forEach(subs ->{
            try {
                long serviceFee = 0L;
                ServiceProperty prop = servicePropertyPersistenceFacade.find(
                        subs.getService(),
                        subs.getSettingByType(ServiceSettingType.ZONE)
                );
                if (prop != null) {
                    serviceFee = prop.getPrice();
                }
                long promobalance = (subs.getBalance().getPromoBalance() >= 0 ? subs.getBalance().getPromoBalance() : 0);
                if ((subs.getBalance().getRealBalance() + promobalance) < serviceFee) {
                    log.info(String.format("Sending notification for active -> blocked status change. subscription id = %d", subs.getId()));
                    queueManager.sendStatusNotification(BillingEvent.SOON_BLOCKED_CONTINUOUS, subs.getId());
                }
            } catch (Exception ex) {
                log.error(String.format("Cannot send notification: event=%s, subscription id=%d", BillingEvent.SOON_BLOCKED_CONTINUOUS, subs.getId()), ex);
            }
        });// end FOR LOOP
        log.info("notifyDataplusUsersForDeadline  elapsed time : {}", (System.currentTimeMillis()-start)/1000);


//       // CNC SMS event
//        log.info("CNC subscription broadcast starts");
//        subscriptions.stream().filter(sub -> sub.getService().getProvider().getId() == Providers.CNC.getId()).forEach(subs ->{
//            try {
//                long serviceFee = 0L;
//                ServiceProperty prop = servicePropertyPersistenceFacade.find(
//                        subs.getService(),
//                        subs.getSettingByType(ServiceSettingType.ZONE)
//                );
//                if (prop != null) {
//                    serviceFee = prop.getPrice();
//                }
//                long promobalance = (subs.getBalance().getPromoBalance() >= 0 ? subs.getBalance().getPromoBalance() : 0);
//                if ((subs.getBalance().getRealBalance() + promobalance) < serviceFee) {
//                    log.info(String.format("Sending CNC notification for active -> blocked status change. subscription id = %d", subs.getId()));
//                    queueManager.sendStatusNotification(BillingEvent.SOON_BLOCKED_CONTINUOUS, subs.getId());
//                }
//            } catch (Exception ex) {
//                log.error(String.format("Cannot send CNC notification: event=%s, subscription id=%d", BillingEvent.SOON_BLOCKED_CONTINUOUS, subs.getId()), ex);
//            }
//        });// end FOR LOOP
//        log.info("notifyCNCUsersForDeadline  elapsed time : {}", (System.currentTimeMillis()-start)/1000);
//

        log.info("Finished sending notifyDataplusUsersForDeadline notifications for subscriptions which are about to be blocked.");
    }


    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */ // @Schedule(hour = "11", minute = "00")
    public void notifyGobalCNCUsersForDeadline(){
    log.info("Starting sending notifications for Global CNC subscriptions which are about to be blocked.");
    List<Subscription> subscriptions = subscriptionFacade.findAllExpiredGlobalAndCNCForNotification();
    log.info(String.format("Found %d Global CNC subscriptions(nograce) for notification: %s", subscriptions.size(), subscriptions.toString()));
    long start = System.currentTimeMillis();

        log.info("Global subscription broadcast starts");
        subscriptions.stream().forEach(subs ->{

                try {
                    long serviceFee = 0L;
                    serviceFee = subs.getServiceFeeRate();

                    long promobalance = (subs.getBalance().getPromoBalance() >= 0 ? subs.getBalance().getPromoBalance() : 0);
                    if ((subs.getBalance().getRealBalance() + promobalance) < serviceFee) {
                        log.info(String.format("Sending Global CNC notification for active -> blocked status change. subscription id = %d", subs.getId()));
                        queueManager.sendStatusNotification(BillingEvent.SOON_BLOCKED_CONTINUOUS, subs.getId());
                    }
                } catch(Exception ex){
                    log.error(String.format("Cannot send Global CNC notification: event=%s, subscription id=%d", BillingEvent.SOON_BLOCKED_CONTINUOUS, subs.getId()), ex);
                }

        });// end FOR LOOP

        log.info("notifyGlobalCNCUsersForDeadline  elapsed time : {}", (System.currentTimeMillis()-start)/1000);
    }

}




