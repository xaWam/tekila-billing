package com.jaravir.tekila.module.campaign;

import com.jaravir.tekila.base.entity.OnlineJoinerCampaign;
import com.jaravir.tekila.engines.ProvisioningEngine;
import com.jaravir.tekila.module.accounting.entity.Payment;
import com.jaravir.tekila.module.accounting.manager.PaymentPersistenceFacade;
import com.jaravir.tekila.module.service.ResourceBucketType;
import com.jaravir.tekila.module.service.ServiceSettingType;
import com.jaravir.tekila.module.service.ServiceType;
import com.jaravir.tekila.module.service.entity.ResourceBucket;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.service.entity.ServiceProperty;
import com.jaravir.tekila.module.service.persistence.manager.ServicePersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.ServicePropertyPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberType;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.broadband.devices.manager.Providers;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import javax.ejb.*;
import javax.swing.text.html.Option;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by kmaharov on 11.08.2016.
 */
//this class is used to join/register subscription to the campaign
@Stateless
public class CampaignJoinerBean {
    @EJB
    private SubscriptionPersistenceFacade subscriptionFacade;
    @EJB
    private SystemLogger systemLogger;
    @EJB
    private CampaignRegisterPersistenceFacade campaignRegisterFacade;
    @EJB
    private CampaignPersistenceFacade campaignPersistenceFacade;
    @EJB
    private PaymentPersistenceFacade paymentFacade;
    @EJB
    private ServicePropertyPersistenceFacade servicePropertyPersistenceFacade;
    @EJB
    private OnlineJoinerCampaignPf onlineJoinerCampaignPf;
    @EJB
    private ServicePersistenceFacade servicePersistenceFacade;

    private final static Logger log = Logger.getLogger(CampaignJoinerBean.class);

    /**
     * i don't like hardcode , you know sometimes you have to sacrifice something
     */
    public CampaignRegister tryAddCampaignOnOnlinePayments(Long subsId, Long paymId) {
        log.info("tryAddCampaignOnOnlinePayments method starts for subscription id: " + subsId + ", payment id: " + paymId);

        try {
            Subscription subscription = subscriptionFacade.find(subsId);

            // hack for comfort~2
            Service service = servicePersistenceFacade.find(7434122101L);
            Payment payment = paymentFacade.find(paymId);
            long previousBalance = subscription.getBalance().getRealBalance() - payment.getAmountAsLong();
            long monthlyDebt = subscription.getService().getServicePrice();
            long vasPrice = subscription
                    .getVasList()
                    .stream()
//                    .filter(subscriptionVAS -> subscriptionVAS.getVasStatus() == 1)//filter only active ones
//                    .filter(subscriptionVAS -> subscriptionVAS.getVas().getCode().getType() == ValueAddedServiceType.PERIODIC_STATIC) //this condition is taken from BillingManager
                    .filter(subscriptionVAS -> !subscriptionVAS.getVas().isSip())  //this condition is taken from SubscriptionManager
                    .map(subscriptionVAS -> subscriptionVAS.getCount() * subscriptionVAS.getVas().getPrice())
                    .collect(Collectors.summingLong(value -> value.longValue()));
            monthlyDebt += vasPrice;

            log.info("try to add online payments for subscription id " + subsId + ", previous balance is " + previousBalance + ", monthly debt is " + monthlyDebt +
                    ", vas count is " + subscription.getVasList().size() + ", vas price is " + vasPrice);

            if (subscription.getService().getProvider().getId() == Providers.CITYNET.getId() && previousBalance >= -monthlyDebt) {

                /***  HACK FOR CONFORT CAMPAIGNS ONLINE ACTIVATIONS  ***/
                if (subscription.getService().getId() == service.getId()) {
                    List<OnlineJoinerCampaign> campaigns = onlineJoinerCampaignPf.findAll();
                    Optional<OnlineJoinerCampaign> optcamps = campaigns
                            .stream()
                            .filter(onlineJoinerCampaign -> onlineJoinerCampaign.getAmount() == payment.getAmountAsLong())
                            .findFirst();

                    if (optcamps.isPresent()) {
                        log.info("CONFORT CAMPAIGNS ONLINE ACTIVATIONS condition works for subscription " + subsId + ", payment is " + payment.getAmountAsLong());
                        Campaign targetCampaign = campaignPersistenceFacade.find(optcamps.get().getCampaignId());
                        List<CampaignRegister> addedRegisters = this.tryAddToCampaigns(subscription,
                                targetCampaign,
                                true,
                                false,
                                "campaign added on online payment " + paymId);
                        if (!addedRegisters.isEmpty()) {
                            CampaignRegister register = addedRegisters.get(0);
                            return register;
                        }
                    }
                }


                /***  HACK FOR 12+1 CAMPAIGN ONLINE ACTIVATIONS  ***/
//                long vasPrice = subscription
//                        .getVasList()
//                        .stream()
//                        .filter(subscriptionVAS -> subscriptionVAS.getVasStatus() == 1)//filter only active ones
//                        .map(subscriptionVAS -> subscriptionVAS.getCount() * subscriptionVAS.getVas().getPrice())
//                        .collect(Collectors.summingLong(value -> value));
                if (payment.getAmountAsLong() == monthlyDebt * 12) {
                    log.info("'Citynet 12+1 campaign' condition works for subscription " + subsId + ", payment is " + payment.getAmountAsLong());
                    //Citynet 12+1 campaign id hack
                    Campaign targetCampaign = campaignPersistenceFacade.find(29150593L);
                    List<CampaignRegister> addedRegisters = this.tryAddToCampaigns(subscription,
                            targetCampaign,
                            true,
                            false,
                            "campaign added on online payment " + paymId);
                    if (!addedRegisters.isEmpty()) {
                        CampaignRegister register = addedRegisters.get(0);
                        register.setBonusAmount(register.getBonusAmount() + vasPrice);
                        return register;
                    }
                }


                /***   CityNet 2x speed campaign  ***/
                if ((payment.getAmountAsLong() == monthlyDebt * 3) || (payment.getAmountAsLong() == monthlyDebt * 6)) {
                    log.info("adding online campaign citynet X2 to " + subsId + ", monthly debt is " + monthlyDebt + ", payment is " + payment.getAmountAsLong());
                    campaignRegisterFacade.tryActivateCampaignOnManualPayment(subsId, 122L, paymId);

                    if(previousBalance >= 0){
                        log.debug(String.format("Previous balance of subscription %s is positive, so campaign %s will suspend..", subsId, 122));
                        campaignRegisterFacade.suspendCampaignUntilBillingTime(subscription, 122L);
                    }

                    return null;
                }

            }


            /***  HACK FOR "10% discount for 12month campaign"  ***/
            if ((subscription.getService().getProvider().getId() == Providers.CITYNET.getId() || subscription.getService().getProvider().getId() == Providers.UNINET.getId()) && previousBalance >= -monthlyDebt) {
                long yearlyDebt = monthlyDebt * 12;
                long minCondition = (long) (yearlyDebt * 0.9);
                long targetCampId = subscription.getService().getProvider().getId() == Providers.CITYNET.getId() ? 28867235L : 32454743L;
                double minConditionToDouble = Math.ceil(minCondition/100_000D);    //checking ceil of minimum condition
                log.info("10% discount for 12month campaign for subscription " + subsId + " -> "
                        + "\nyearlyDebt = " + yearlyDebt
                        + "\nminCondition = " + minCondition
                        + "\ntargetCampId = " + targetCampId
                        + "\npayment = " + payment.getAmount()
                        + "\npaymentToLong = " + payment.getAmountAsLong()
                        + "\nminConditionToDouble = " + minConditionToDouble);

                //10% discount for 12month campaign
                if (payment.getAmountAsLong() == minCondition || payment.getAmount() == minConditionToDouble) {
                    Campaign targetCampaign = campaignPersistenceFacade.find(targetCampId);
                    log.info("adding online 10% discount for 12month campaign [id: "+targetCampaign.getId()+"] to " + subsId + ", monthly debt is " + monthlyDebt + ", payment is " + payment.getAmountAsLong());
                    campaignRegisterFacade.tryActivateCampaignOnManualPayment(subsId, targetCampaign.getId(), paymId);
                    return null;
                }
            }


            /***  DP,CNC,GLOBAL -> 3+1  ***/
            long providerId = subscription.getService().getProvider().getId();
            if (providerId == Providers.DATAPLUS.getId() || providerId == Providers.GLOBAL.getId() || providerId == Providers.CNC.getId()) {
                long dp3 = 24546858L, gl3 = 30328035L, cnc3 = 30327965L;
                long dp6 = 24546863L, gl6 = 30328012L, cnc6 = 30327886L;
                long targetCampId3 = providerId == Providers.GLOBAL.getId() ? gl3 : cnc3;//3+1
                long targetCampId6 = providerId == Providers.GLOBAL.getId() ? gl6 : cnc6;//6+2
                monthlyDebt = subscription.getServiceFeeRate();

                if (providerId == Providers.DATAPLUS.getId()) {
                    targetCampId3 = dp3;
                    targetCampId6 = dp6;
                    ServiceProperty prop = servicePropertyPersistenceFacade.find(
                            subscription.getService(),
                            subscription.getSettingByType(ServiceSettingType.ZONE)
                    );
                    if (prop != null) monthlyDebt = prop.getPrice();
                    else return null;

                }
                Campaign targetCampaign = null;

                if (payment.getAmountAsLong() == monthlyDebt * 3) {
                    targetCampaign = campaignPersistenceFacade.find(targetCampId3);
                } else if (payment.getAmountAsLong() == monthlyDebt * 6) {
                    targetCampaign = campaignPersistenceFacade.find(targetCampId6);
                } else {
                    return null;
                }

                List<CampaignRegister> addedRegisters = this.tryAddToCampaigns(subscription,
                        targetCampaign,
                        true,
                        false,
                        "campaign added on online payment " + paymId);
                if (!addedRegisters.isEmpty()) {
                    CampaignRegister register = addedRegisters.get(0);
                    return register;
                }
            }

        } catch (Exception e) {
            log.error("Error occurs in tryAddCampaignOnOnlinePayments method for subscription id: " + subsId, e);
        }

        log.info("tryAddCampaignOnOnlinePayments method ends for subscription id: " + subsId);

        return null;
    }

    public void tryAddToCampaigns(Subscription subscription) {
        //add all automatic campaigns for this subscription
        //and set their status to NOT_ACTIVE
        tryAddToCampaigns(subscription, null, true, true, null);
    }

    public List<CampaignRegister> tryAddToCampaigns(
            Subscription subscription,
            Campaign selectedCampaign,
            boolean isNotActivate,
            boolean isAutomatic,
            String campaignNotes) {
        String logHeader = String.format("addToServiceFeeCampaign: subscription agreement=%s, id=%d", subscription.getAgreement(), subscription.getId());

        log.debug("Start tryAddToCampaigns");

        List<Campaign> campaignList = null;
        List<CampaignRegister> result = new ArrayList<>();

        if (selectedCampaign == null) {
            log.debug(">>>>>>>>>>>1");
            campaignList = campaignPersistenceFacade.findAllActive(subscription.getService(), isAutomatic);
        } else {
            log.debug(">>>>>>>>>>>2");
            campaignList = Arrays.asList(selectedCampaign);
        }

        Long bonusAmount = null;
        CampaignRegister register = null;

        log.debug("campaignList before IF");

        if (campaignList != null && !campaignList.isEmpty()) {
            log.info(String.format("%s found campaigns: %d", logHeader, campaignList.size()));

            for (Campaign campaign : campaignList) {
                /*if (campaign.getTarget() == CampaignTarget.PAYMENT && !campaign.isCompound())
                    continue;
                */
                log.info(String.format("%s processing campaign id: %d", logHeader, campaign.getId()));

                if (campaign.isCompound() && campaign.getCampaignList() != null && !campaignList.isEmpty()) {
                    for (Campaign subCampaign : campaign.getCampaignList()) {
                        log.info(String.format("%s campaign id=%d subcampaign id: %d", logHeader, campaign.getId(), subCampaign.getId()));
                        register = registerCampaign(subscription, subCampaign, isNotActivate, campaignNotes);
                    }
                } else {
                    register = registerCampaign(subscription, campaign, isNotActivate, campaignNotes);
                }


                if (register != null) {
                    result.add(register);
                    register = null;
                }
            }
        } else {
            log.info(String.format("%s no campaigns found", logHeader));
        }

        return result;
    }

    private CampaignRegister registerCampaign(
            Subscription subscription,
            Campaign campaign,
            boolean isNotActivate,
            String campaignNotes) {
        String logHeader = String.format("addToServiceFeeCampaign: subscription agreement=%s, id=%d", subscription.getAgreement(), subscription.getId());
        Long bonusAmount = null;
        Long bonusLimit = campaign.getBonusLimitByService(subscription.getService());
        DateTime bonusDate = null;

        switch (campaign.getTarget()) {
            case RESOURCE_INTERNET_BANDWIDTH:
                ResourceBucket bucket = subscription.getService().getResourceBucketByType(ResourceBucketType.INTERNET_DOWN);

                if (bucket != null) {
                    bonusAmount = Long.valueOf(bucket.getCapacity()) * campaign.getBonusCount().longValue();
                    log.info(String.format("%s bandwidth returned: %d", logHeader, bonusAmount));
                }
                break;
            case SERVICE_RATE:
                bonusAmount = Double.valueOf(Math.ceil(subscription.getService().getServicePrice() * campaign.getBonusCount())).longValue();
                log.info(String.format("%s service rate returned: %d", logHeader, bonusAmount));

                if (subscription.getService().getServiceType() == ServiceType.BROADBAND //&& subscription.getStatus() == SubscriptionStatus.ACTIVE
                        && campaign.isCancelInvoice()) {
                    subscription = subscriptionFacade.overwriteInvoice(subscription, bonusAmount, "");
                }
                break;
            case PAYMENT:
                long serviceFee = subscription.getService().getServicePrice();
                if (subscription.getService().getProvider().getId() == Providers.DATAPLUS.getId()) {
                    ServiceProperty prop = servicePropertyPersistenceFacade.find(
                            subscription.getService(),
                            subscription.getSettingByType(ServiceSettingType.ZONE)
                    );
                    serviceFee = prop.getPrice();
                }
                bonusAmount = serviceFee * campaign.getBonusCount().longValue();
                break;
            case EXPIRATION_DATE:
                bonusDate = campaign.getBonusDate();
                log.info(String.format("EXPIRATION_DATE campaign has been chosen." +
                                "subscription id = %d, campaign id = %d, bonus date = %s",
                        subscription.getId(),
                        campaign.getId(),
                        bonusDate.toString()));
                break;
        }

        //hack for TV payment campaigns
        if (subscription.getService().getServiceType() == ServiceType.TV) {
            if (campaign.getTarget() == CampaignTarget.PAYMENT) {
                long serviceFee = subscription.getService().getServicePrice();
                bonusAmount = serviceFee * campaign.getBonusCount().longValue();
            }
        }

        if (bonusLimit != null && bonusAmount > bonusLimit) {
            log.info(String.format("%s bonus amount=%d exceeds bonus limit=%d", logHeader, bonusAmount, bonusLimit));
            bonusAmount = bonusLimit;
        }

        CampaignRegister register = addToCampaign(subscription, campaign, bonusAmount, bonusDate, isNotActivate, campaignNotes);
        log.info(String.format("%s subscription agreement %s successfully joined campaign id=%d", logHeader,
                subscription.getAgreement(), register.getCampaign().getId()));

        systemLogger.success(SystemEvent.SUBSCRIPTION_CAMPAIGN_JOINED, subscription, null,
                String.format("campaign id=%d, bonusAmount=%d, bonusDate=%s",
                        campaign.getId(),
                        (bonusAmount != null ? bonusAmount : 0L),
                        (bonusDate != null ? bonusDate.toString() : "")));

        return register;
    }

    private CampaignRegister addToCampaign(
            Subscription subscription,
            Campaign campaign,
            Long bonusAmount,
            DateTime bonusDate,
            boolean isNotActivate,
            String campaignNotes) {
        return campaignRegisterFacade.add(subscription, campaign, bonusAmount, bonusDate, isNotActivate, null, null, campaignNotes);
    }

    private boolean addToPaymentCampaign(
            Subscription subscription,
            Campaign campaign,
            long paymentID,
            boolean isNotActivate,
            boolean isAddable) {
        /*if (subscription.getStatus().equals(SubscriptionStatus.ACTIVE)) {
            log.info(String.format("cannot activate auto campaign for active subscription, id = %d", subscription.getId()));
            return;
        }*/

        if (campaign.isCompound()) {
            return false;
        }

        if (subscription.getService().getServiceType() == ServiceType.TV) {
            return false;
        }

        Payment payment = paymentFacade.find(paymentID);
        String msg = String.format("subscription id=%d, payment id=%d", subscription.getId(), paymentID);
        long amount = payment.getAmountAsLong();
        long serviceFee = subscription.getService().getServicePrice();
        if (subscription.getService().getProvider().getId() == Providers.DATAPLUS.getId()) {
            ServiceProperty prop = servicePropertyPersistenceFacade.find(
                    subscription.getService(),
                    subscription.getSettingByType(ServiceSettingType.ZONE)
            );
            serviceFee = prop.getPrice();
        }
        long installFee = subscription.getService().getInstallFee();

        if (subscription.getService().getServiceType() == ServiceType.TV) {
            installFee = 25 * 100000;
        }

        if (amount >= (campaign.getCount() * serviceFee + installFee)) { //qualifies
            long bonusAmount = serviceFee * campaign.getBonusCount().longValue();
            List<CampaignRegister> registerList = campaignRegisterFacade.findNotActiveBySubscriptionAndCampaign(subscription, campaign);
            if (registerList == null || registerList.isEmpty()) {
                if (isAddable) {
                    CampaignRegister registered =
                            addToCampaign(subscription, campaign, bonusAmount, null, isNotActivate, null);
                    registered.decrementLifecycleCount();
                    subscriptionFacade.increasePromoBalance(subscription, bonusAmount);
                    log.info("addToPaymentCampaign: successfully increment promo balance by " + bonusAmount + " " + msg);
                    systemLogger.success(SystemEvent.SUBSCRIPTION_TOPUP_PROMO_BALANCE, subscription, msg);
                    return true;
                }
            } else {
                CampaignRegister registered = registerList.get(0);
                if (!isNotActivate) {
                    registered.setStatus(CampaignStatus.ACTIVE);
                }
                registered.decrementLifecycleCount();
                campaignRegisterFacade.update(registered);

                subscriptionFacade.increasePromoBalance(subscription, bonusAmount);
                log.info("addToPaymentCampaign: successfully increment promo balance by " + bonusAmount + " " + msg);
                systemLogger.success(SystemEvent.SUBSCRIPTION_TOPUP_PROMO_BALANCE, subscription, msg);
                return true;
            }
        }
        return false;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void tryAddToCampaign(long subscriptionID, long paymentID, boolean isNotActivate) {
        Subscription subscription = subscriptionFacade.find(subscriptionID);

        if (subscription.getSubscriber().getDetails().getType().equals(SubscriberType.CORP)) {
            log.info("Cannot add auto campaigns for corporate subscribers, subscription id "+subscriptionID);
            return;
        }

        log.debug(String.format("tryAddToCampaign: checking for campaigns for subscription id=%d, paymentID=%d, service id=%d", subscription.getId(), paymentID, subscription.getService().getId()));
        List<Campaign> campaignList = campaignPersistenceFacade.findBySubscription(subscription);


        if (campaignList == null) {
            log.debug("tryAddToCampaign: no campaigns found for subscription "+subscriptionID);
            return;
        }

        log.debug("tryAddToCampaign: found campaigns " + campaignList.size()+" for subscription "+subscriptionID);

        List<CampaignRegister> ongoingCampaigns = campaignRegisterFacade.findNotProcessedBySubscription(subscription);
        if (ongoingCampaigns != null && !ongoingCampaigns.isEmpty()) {
            for (final CampaignRegister register : ongoingCampaigns) {
                if (register.getCampaign().isAvailableOnCreation()) {
                    log.debug(
                            String.format("tryAddToCampaign: found creation campaign id = %d for subscription id = %d", register.getCampaign().getId(), subscription.getId()));
                    return;
                }
            }
        }

        campaignList.sort((c1, c2) -> {
            if (c1.getCount() > c2.getCount()) {
                return -1;
            } else if (c1.getCount() == c2.getCount()) {
                return 0;
            } else {
                return 1;
            }
        });

        for (Campaign campaign : campaignList) {
            if (campaign.getTarget().equals(CampaignTarget.PAYMENT) && campaign.isAutomatic()) {
                if (paymentFacade.isFirstPayment(subscription, paymentID)) {
                    if (!campaign.isActivateOnPayment()) {
                        if (addToPaymentCampaign(subscription, campaign, paymentID, isNotActivate, false)) {
                            break;
                        }
                    }
                } else {
                    if (campaign.isActivateOnPayment()) {
                        if (addToPaymentCampaign(subscription, campaign, paymentID, isNotActivate, true)) {
                            break;
                        }
                    } else {
                        log.info("addToPaymentCampaign: not first payment. exiting...");
                    }
                }
            }
        }
    }
}
