package com.jaravir.tekila.module.campaign;

import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.engines.ProvisioningEngine;
import com.jaravir.tekila.module.accounting.AccountingCategoryType;
import com.jaravir.tekila.module.accounting.AccountingTransactionType;
import com.jaravir.tekila.module.accounting.entity.*;
import com.jaravir.tekila.module.accounting.manager.AccountingCategoryPersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.AccountingTransactionPersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.PaymentPersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.TransactionPersistenceFacade;
import com.jaravir.tekila.module.service.ResourceBucketType;
import com.jaravir.tekila.module.service.ServiceType;
import com.jaravir.tekila.module.service.ValueAddedServiceType;
import com.jaravir.tekila.module.service.entity.ResourceBucket;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.subscription.persistence.entity.*;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionVASPersistenceFacade;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.broadband.devices.manager.Providers;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Minutes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by sajabrayilov on 5/29/2015.
 */
@Stateless
public class CampaignRegisterPersistenceFacade extends AbstractPersistenceFacade<CampaignRegister> {
    @PersistenceContext
    private EntityManager em;
    @EJB
    private CampaignPersistenceFacade campaignFacade;
    @EJB
    private SubscriptionPersistenceFacade subscriptionFacade;
    @EJB
    private EngineFactory provisioningFactory;
    @EJB
    private PaymentPersistenceFacade paymentFacade;
    @EJB
    private SystemLogger systemLogger;
    @EJB
    private SubscriptionVASPersistenceFacade vasPersistenceFacade;
    @EJB
    private TransactionPersistenceFacade transFacade;
    @EJB
    private UserPersistenceFacade userFacade;
    @EJB
    private AccountingTransactionPersistenceFacade accTransFacade;
    @EJB
    private AccountingCategoryPersistenceFacade accCatFacade;

    private final static Logger log = Logger.getLogger(CampaignRegisterPersistenceFacade.class);

    private final static long VAS_CAMPAIGN_ACTIVATION_THRESHOLD = 3;

    public enum Filter implements Filterable {
        SUBSCRIPTION("subscription.id");
        private final String field;
        private MatchingOperation operation;

        Filter(String field) {
            this.field = field;
            this.operation = MatchingOperation.EQUALS;
        }

        @Override
        public String getField() {
            return field;
        }

        @Override
        public MatchingOperation getOperation() {
            return this.operation;
        }

        public void setOperation(MatchingOperation operation) {
            this.operation = operation;
        }
    }

    public CampaignRegisterPersistenceFacade() {
        super(CampaignRegister.class);
    }

    @Override
    public EntityManager getEntityManager() {
        return em;
    }

    public CampaignRegister add(
            Subscription subscription,
            Campaign campaign,
            Long bonusAmount,
            DateTime bonusDate,
            boolean isNotActivate,
            List<ValueAddedService> appliedVasList,
            Double serviceRateBonusAmount,
            String campaignNotes) {
        CampaignRegister campaignRegister = new CampaignRegister();
        save(campaignRegister);
        campaignRegister.setCampaignNotes(campaignNotes);
        campaignRegister.setSubscription(subscription);
        campaignRegister.setCampaign(campaign);
        campaignRegister.setJoinDate(DateTime.now());
        campaignRegister.setLifeCycleCount(campaign.getLifeCycleCount());

        if (isNotActivate)
            campaignRegister.setStatus(CampaignStatus.NOT_ACTIVE);
        else
            campaignRegister.setStatus(CampaignStatus.ACTIVE);

        campaignRegister.setBonusAmount(bonusAmount);
        campaignRegister.setBonusDate(bonusDate);
        campaignRegister.setVasList(appliedVasList);
        campaignRegister.setServiceRateBonusAmount(serviceRateBonusAmount);
        return campaignRegister;
    }

    public List<CampaignRegister> findAllActiveOld(SubscriberLifeCycleType lyfeCycleType) {
        return getEntityManager().createQuery("select c from CampaignRegister c where c.status = :status and c.subscription.lastBilledDate between :start and :fin and c.subscription.subscriber.lifeCycle = :cycle")
                .setParameter("status", CampaignStatus.ACTIVE)
                .setParameter("start", DateTime.now().minusHours(4))
                .setParameter("fin", DateTime.now())
                .setParameter("cycle", lyfeCycleType)
                .getResultList();
    }

    public List<CampaignRegister> findAllActive(SubscriberLifeCycleType lyfeCycleType) {
        return getEntityManager().createQuery("select c from CampaignRegister c where c.status = :status and c.subscription.subscriber.lifeCycle = :cycle")
                .setParameter("status", CampaignStatus.ACTIVE)
                .setParameter("cycle", lyfeCycleType)
                .getResultList();
    }

    public List<CampaignRegister> findAllDataplusCampaignProblemNonActivated(SubscriberLifeCycleType lyfeCycleType) {
        return getEntityManager().createQuery("select c from CampaignRegister c where " +
                "c.status = :status " +
                "and c.subscription.subscriber.lifeCycle = :cycle " +
                "and c.subscription.service.provider.id = :pid " +
                "and c.campaign.id = :cid")
                .setParameter("status", CampaignStatus.NOT_ACTIVE)
                .setParameter("cycle", lyfeCycleType)
                .setParameter("pid", Providers.DATAPLUS.getId())
                .setParameter("cid", 24526517L)
                .getResultList();
    }

    public List<CampaignRegister> findActiveForAfterBilling(Subscription subscription) {
        return getEntityManager().createQuery("select c from CampaignRegister c where c.status = :status and c.subscription.id = :sbnID and c.lifeCycleCount >= 1")
                .setParameter("status", CampaignStatus.ACTIVE)
                .setParameter("sbnID", subscription.getId())
                .getResultList();
    }

    public List<CampaignRegister> findActive(Subscription subscription) {
        return getEntityManager().createQuery("select c from CampaignRegister c where c.status = :status and c.subscription.id = :sbnID and c.lifeCycleCount <= 1")
                .setParameter("status", CampaignStatus.ACTIVE)
                .setParameter("sbnID", subscription.getId())
                .getResultList();
    }

    public List<CampaignRegister> findActiveForProcessing(Subscription subscription) {
        return getEntityManager().createQuery("select c from CampaignRegister c where c.status = :status and c.subscription.id = :sbnID and " +
                "c.lifeCycleCount < 1")
                .setParameter("status", CampaignStatus.ACTIVE)
                .setParameter("sbnID", subscription.getId())
                .getResultList();
    }

    public List<CampaignRegister> findActive(Subscription subscription, CampaignTarget target) {
        return getEntityManager().createQuery("select c from CampaignRegister c where c.status = :status and c.subscription.id = :sbnID and c.lifeCycleCount >= 1 and c.campaign.target = :target")
                .setParameter("status", CampaignStatus.ACTIVE)
                .setParameter("sbnID", subscription.getId())
                .setParameter("target", target)
                .getResultList();
    }

    public List<CampaignRegister> findAllBySubscription(Subscription subscription) {
        return getEntityManager().createQuery("select c from CampaignRegister c where  c.subscription.id = :sbnID ")
                .setParameter("sbnID", subscription.getId())
                .getResultList();
    }

    public List<CampaignRegister> findAllBySubscription(Subscription subscription, CampaignTarget target) {
        return getEntityManager().createQuery("select c from CampaignRegister c where  c.subscription.id = :sbnID " +
                " and c.campaign.target = :target and c.lifeCycleCount >=1 ")
                .setParameter("sbnID", subscription.getId())
                .setParameter("target", target)
                .getResultList();
    }

    public List<CampaignRegister> findNotActiveBySubscription(Subscription subscription) {
        return getEntityManager().createQuery("select c from CampaignRegister c where c.subscription.id = :sbnID " +
                " and c.status = :status")
                .setParameter("sbnID", subscription.getId())
                .setParameter("status", CampaignStatus.NOT_ACTIVE)
                .getResultList();
    }

    public List<CampaignRegister> findNotActiveBySubscriptionAndCampaign(Subscription subscription, Campaign campaign) {
        return getEntityManager().createQuery("select c from CampaignRegister c where  c.subscription.id = :sbnID " +
                " and c.campaign.id = :campId and c.status = :status")
                .setParameter("sbnID", subscription.getId())
                .setParameter("campId", campaign.getId())
                .setParameter("status", CampaignStatus.NOT_ACTIVE)
                .getResultList();
    }

    public List<CampaignRegister> findNotProcessedBySubscription(Subscription subscription) {
        return getEntityManager().createQuery("select c from CampaignRegister c where c.subscription.id = :sbnId " +
                " and c.status <> :status")
                .setParameter("sbnId", subscription.getId())
                .setParameter("status", CampaignStatus.PROCESSED)
                .getResultList();
    }

    public List<CampaignRegister> findNotProcessedBySubscriptionAndCampaign(Subscription subscription, Campaign campaign) {
        return getEntityManager().createQuery("select c from CampaignRegister c where c.subscription.id = :sbnId " +
                " and c.status <> :status and c.campaign.id = :campaignId")
                .setParameter("sbnId", subscription.getId())
                .setParameter("status", CampaignStatus.PROCESSED)
                .setParameter("campaignId", campaign.getId())
                .getResultList();
    }

    public void deleteRegister(final CampaignRegister register) {
        if (register.getCampaign().getTarget() == CampaignTarget.PAYMENT) {
            subscriptionFacade.removePromo(register);
        } else if (register.getCampaign().getTarget() == CampaignTarget.EXPIRATION_DATE) {
            subscriptionFacade.rollbackBonusDate(register);
        }
        removeDetached(register);

        String dsc = String.format("subscription id=%d, campaign id=%d, register id=%d",
                register.getSubscription().getId(),
                register.getCampaign().getId(),
                register.getId());
        systemLogger.success(SystemEvent.SUBSCRIPTION_CAMPAIGN_DELETED, register.getSubscription(), dsc);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void partialProcess(CampaignRegister reg) {
        Long transferAmount = reg.getCampaign().getPromoTransferLimit();
        Subscription subscription = reg.getSubscription();
        if (subscription.getBalance().getPromoBalance() < transferAmount) {
            transferAmount = subscription.getBalance().getPromoBalance();
        }
        if (subscription.getBalance().getPromoBalance() >= transferAmount) {
            subscriptionFacade.usePromoBalance(reg.getSubscription(), transferAmount);
            String msg = String.format("campaing register id = %d, promo amount = %d", reg.getId(), transferAmount);
            log.info(String.format("Partial transferation of promo to real balance: %s", msg));
            systemLogger.success(SystemEvent.PARTIAL_PROMO_TRANSFER, reg.getSubscription(), msg);
        }
        if (subscription.getBalance().getPromoBalance() == 0) {
            reg.setStatus(CampaignStatus.PROCESSED);
            reg.setProcessedDate(DateTime.now());
            String msg = String.format("campaign register id=%d, bonus amount=%d", reg.getId(), reg.getBonusAmount());
            log.info(String.format("process: campaign processed successfully, subscription id =%d, %s", reg.getSubscription().getId(), msg));
            systemLogger.success(SystemEvent.SUBSCRIPTION_CAMPAIGN_PROCESSED, reg.getSubscription(), msg);
        }
        update(reg);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void process(CampaignRegister reg) {
        campaignFacade.process(reg.getSubscription(), reg.getCampaign(), reg.getBonusAmount());
        reg.setStatus(CampaignStatus.PROCESSED);
        reg.setProcessedDate(DateTime.now());
        String msg = String.format("campaign register id=%d, bonus amount=%d", reg.getId(), reg.getBonusAmount());
        log.info(String.format("process: campaign processed successfully, subscription id =%d, %s", reg.getSubscription().getId(), msg));
        systemLogger.success(SystemEvent.SUBSCRIPTION_CAMPAIGN_PROCESSED, reg.getSubscription(), msg);
        update(reg);
    }

    public void processPrepaidForAfterBilling(Subscription subscription) {
        log.info("processPrepaidForAfterBilling: starting processing prepaid campaign registers");
        List<CampaignRegister> registerList = findActiveForAfterBilling(subscription);
        log.info(String.format("processPrepaidForAfterBilling: Found %d campaign registers", registerList != null ? registerList.size() : 0));
        Long lifeCycleCount = null;

        if (registerList != null) {
            for (CampaignRegister reg : registerList) {
                log.info(String.format("processPrepaidForAfterBilling: starting processing campaign register id=%d", reg.getId()));
                reg.decrementLifecycleCount();

                log.info(String.format("processPrepaidForAfterBilling: finished processing campaign register id=%d", reg.getId()));
            }
        }
        log.info("processPrepaidForAfterBilling: finished processing prepaid campaign registers");
    }

    public List<CampaignRegister> findActiveByTarget(Subscription subscription, CampaignTarget target) {
        return getEntityManager().createQuery("select c from CampaignRegister c where c.status = :status and c.subscription.id = :sbnID and c.lifeCycleCount >= 1 and c.campaign.target = :target")
                .setParameter("status", CampaignStatus.ACTIVE)
                .setParameter("sbnID", subscription.getId())
                .setParameter("target", target)
                .getResultList();
    }

    public List<CampaignRegister> findByTarget(Subscription subscription, CampaignTarget target, CampaignStatus status) {
        return getEntityManager().createQuery("select c from CampaignRegister c where c.status = :status and c.subscription.id = :sbnID and c.lifeCycleCount >= 1 and c.campaign.target = :target")
                .setParameter("status", status)
                .setParameter("sbnID", subscription.getId())
                .setParameter("target", target)
                .getResultList();
    }

    public List<CampaignRegister> findByTarget(Subscription subscription, CampaignStatus status) {
        return getEntityManager().createQuery("select c from CampaignRegister c where c.status = :status and c.subscription.id = :sbnID")
                .setParameter("status", status)
                .setParameter("sbnID", subscription.getId())
                .getResultList();
    }

    public Long getCampaignBonus(Subscription subscription, CampaignTarget target) {
        String logHeader = String.format("getCampaignBonus: subscription agreement=%s, id=%d", subscription.getAgreement(), subscription.getId());
        List<CampaignRegister> campaignList = findActiveByTarget(subscription, target);

        if (campaignList != null && !campaignList.isEmpty()) {
            CampaignRegister register = campaignList.get(0);

            return register.getBonusAmount();
        }

        return null;
    }

    public Long getBonusAmount(Subscription subscription, CampaignTarget target) {
        return getBonusAmount(subscription, target, true);
    }

    public Long getBonusAmount(Subscription subscription, CampaignTarget target, boolean isDecrementLyfecycleCount) {
        String logHeader = String.format("getBonusAmount: subscription agreement=%s, id=%d", subscription.getAgreement(), subscription.getId());

        List<CampaignRegister> campaignList = findActiveByTarget(subscription, target);

        if (campaignList != null && !campaignList.isEmpty()) {
            log.info(String.format("%s found %d campaigns", logHeader, campaignList.size()));

            CampaignRegister register = campaignList.get(0);
            log.info(String.format("%s selected register id=%d,bonusAmount=%d", logHeader, register.getId(), register.getBonusAmount()));

            if (isDecrementLyfecycleCount)
                register.decrementLifecycleCount();

            systemLogger.success(SystemEvent.SUBSCRIPTION_CAMPAIGN_BONUS_USED, subscription, null,
                    String.format("campaign register=%d, bonusAmount=%d", register.getId(), register.getBonusAmount()));
            return register.getBonusAmount();
        } else {
            log.info(String.format("%s found no campaigns", logHeader));
        }
        return null;
    }

    public Double getBonusDiscount(Subscription subscription) {
        List<CampaignRegister> campaignRegisters = findActiveByTarget(subscription, CampaignTarget.SERVICE_RATE);
        if (campaignRegisters != null && !campaignRegisters.isEmpty()) {
            return campaignRegisters.get(0).getServiceRateBonusAmount();
        }
        return null;
    }

    public List<ValueAddedService> getApplicableVasList(Subscription subscription) {
        List<ValueAddedService> vasList = null;

        List<CampaignRegister> campaignRegisters = findActiveByTarget(subscription, CampaignTarget.SERVICE_RATE);
        if (campaignRegisters != null && !campaignRegisters.isEmpty()) {
            vasList = campaignRegisters.get(0).getVasList();
        }
        return vasList;
    }

    //@Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void tryActivateCampaign(long subscriptionId, long paymentID) {
        String logHeader = String.format("tryActivateCampaign: subscription id=%d, paymentid=%d", subscriptionId, paymentID);
        Subscription subscription = subscriptionFacade.find(subscriptionId);

        if (subscription.getSubscriber().getDetails().getType().equals(SubscriberType.CORP)) {
            log.info("It is not allowed to activate campaigns for corporate subscribers");
            return;
        }
        Payment payment = paymentFacade.find(paymentID);

        if (payment == null)
            throw new IllegalArgumentException(String.format("%s payment not found", logHeader));

        List<CampaignRegister> registerList = findByTarget(subscription, CampaignStatus.NOT_ACTIVE);
        log.info(String.format("%s - found 0 campaigns awaiting activatation", logHeader, registerList.size()));

        if (registerList != null && !registerList.isEmpty()) {
            log.info(String.format("%s - found %d campaign awaiting activatation", logHeader, registerList.size()));
            Long bonusAmount = null;
            //Long paymentAsLong = Float.valueOf(Double.valueOf(payment.getAmount()).floatValue() * 100000).longValue();
            //Long serviceFeeRate = subscription.getService().getRateProfile().getActiveRate();
            Double serviceFeeRate = subscription.getService().getServicePriceInDouble();
            log.info(String.format("%s - service rate=%f, payment amount=%f", logHeader, serviceFeeRate, payment.getAmount()));

            for (CampaignRegister register : registerList) {
                if (register.getCampaign().isActivateOnManualPaymentOnly()) {
                    continue;
                }
                if (subscription.getService().getServiceType() == ServiceType.BROADBAND) {
                    log.debug(String.format("%s register id = %d, campaign count = %d",
                            logHeader, register.getId(), register.getCampaign().getCount()));

                    if (register.getCampaign().getCount() > 0
                            && payment.getAmount() < (Math.ceil(serviceFeeRate / 2.0) * register.getCampaign().getCount())) {
                        log.debug(String.format("%s register id = %d, campaign count = %d, payment not adequate for configured number of months case",
                                logHeader, register.getId(), register.getCampaign().getCount()));
                        continue;
                    } else if (register.getCampaign().getCount() <= 0 && payment.getAmount() < (Math.ceil(serviceFeeRate / 2.0) * 6)) {
                        log.debug(String.format("%s register id = %d, campaign count = %d, payment not adequate for hard 6 months case",
                                logHeader, register.getId(), register.getCampaign().getCount()));
                        continue;
                    }
                }

                bonusAmount = register.getBonusAmount();

                switch (register.getCampaign().getTarget()) {
                    case RESOURCE_INTERNET_BANDWIDTH:
                        if (subscription.getService().getServiceType() == ServiceType.BROADBAND) {
                            ResourceBucket bucket = subscription.getService().getResourceBucketByType(ResourceBucketType.INTERNET_DOWN);

                            if (bucket != null) {
                                //bonusAmount = Long.valueOf(bucket.getCapacity()) * register.getCampaign().getBonusCount().longValue();
                                log.info(String.format("%s bandwidth returned: %d", logHeader, bonusAmount));

                                log.info(String.format("create: subscription agreement=%s, found resource campaign, new bandwidth=%s", subscription.getAgreement(), bonusAmount));

                                if (register.getLifeCycleCount() == -1) { //indicates that lifecycle depends on the payment amount
                                    long amount = payment.getAmountAsLong();
                                    long lifeCycleCount = amount / (subscription.getService().getServicePrice());
                                    if (register.getCampaign().getCount() <= lifeCycleCount) {
                                        register.setLifeCycleCount((int) lifeCycleCount);
                                    }
                                }
                                if (register.getLifeCycleCount() > 0) {
                                    if (bonusAmount != null) {
                                        subscription.setBucketCapacityByType(ResourceBucketType.INTERNET_DOWN, String.valueOf(bonusAmount));
                                        subscription.setBucketCapacityByType(ResourceBucketType.INTERNET_UP, String.valueOf(bonusAmount));
                                    }
                                }
                            }
                        }
                        break;
                    case SERVICE_RATE:
                        if (payment.getAmountAsLong() < register.getBonusAmount()) {
                            log.info(String.format("%s payment amount=%d less than discounted service fee=%d",
                                    logHeader, payment.getAmountAsLong(), register.getBonusAmount()));
                            continue;
                        }
                        register.setStatus(CampaignStatus.ACTIVE);
                        register.decrementLifecycleCount();
                        break;
                    case PAYMENT:
                        if (subscription.getService().getServiceType() != ServiceType.TV) { // for TV, there is separate logic below
                            long amount = payment.getAmountAsLong();
                            long serviceFee = subscription.getService().getServicePrice();
                            Campaign campaign = register.getCampaign();
                            String msg = String.format("subscription id=%d, payment id=%d", subscription.getId(), paymentID);

                            if (amount >= (campaign.getCount() * serviceFee) && subscription.getBalance().getRealBalance() >= 0) { //qualifies
                                bonusAmount = register.getBonusAmount();
                                register.setStatus(CampaignStatus.ACTIVE);
                                register.decrementLifecycleCount();
                                subscriptionFacade.increasePromoBalance(subscription, bonusAmount);
                                log.info("tryActivateCampaign: successfully increment promo balance by " + bonusAmount + " " + msg);
                                systemLogger.success(SystemEvent.SUBSCRIPTION_TOPUP_PROMO_BALANCE, subscription, msg);
                            } else {
                                continue;
                            }
                            break;
                        }
                }

                if (subscription.getService().getServiceType() == ServiceType.TV) {
                    switch (register.getCampaign().getTarget()) {
                        case PAYMENT:
                            Campaign campaign = register.getCampaign();
                            if (!paymentFacade.isFirstPayment(subscription, paymentID)) {
                                log.info("tryActivateCampaign: not first payment. exiting...");
                                return;
                            }

                            if (campaign.isCompound()) {
                                return;
                            }

                            String msg = String.format("subscription id=%d, payment id=%d", subscription.getId(), paymentID);
                            long amount = payment.getAmountAsLong();
                            long serviceFee = subscription.getService().getServicePrice();
                            long installFee = subscription.getService().getInstallFee();

                            if (subscription.getService().getServiceType() == ServiceType.TV) {
                                //installFee = 25 * 100000;
                                installFee = 0;
                            }

                            if (amount >= (campaign.getCount() * serviceFee + installFee) && subscription.getBalance().getRealBalance() >= 0) { //qualifies
                                bonusAmount = register.getBonusAmount();
                                subscriptionFacade.increasePromoBalance(subscription, bonusAmount);
                                register.setStatus(CampaignStatus.ACTIVE);
                                register.decrementLifecycleCount();
                                log.info("tryActivateCampaign: successfully increment promo balance by " + bonusAmount + " " + msg);
                                systemLogger.success(SystemEvent.SUBSCRIPTION_TOPUP_PROMO_BALANCE, subscription, msg);
                            }
                    }
                }

                if (subscription.getService().getServiceType() != ServiceType.TV && register.getCampaign().getTarget() != CampaignTarget.PAYMENT) {
                    register.setStatus(CampaignStatus.ACTIVE);
                    register.decrementLifecycleCount();
                }

                log.info(String.format("%s subscription agreement %s successfully joined campaign id=%d", logHeader,
                        subscription.getAgreement(), register.getCampaign().getId()));
                systemLogger.success(SystemEvent.SUBSCRIPTION_CAMPAIGN_JOINED, subscription, null,
                        String.format("campaign id=%d, bonusAmount=%d", register.getCampaign().getId(), bonusAmount));
            } // end LOOP
            try {
                provisioningFactory.getProvisioningEngine(subscription).openService(subscription, subscription.getExpirationDateWithGracePeriod());
            } catch (Exception ex) {
                log.error("Cannot open service: subscription id=" + subscription.getId(), ex);
            }
        } // end IF
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void tryActivateCampaignOnVasAdd(Subscription subscription, ValueAddedService vas) {
        List<Campaign> campaigns = campaignFacade.findVasOnly(subscription, vas);
        if (campaigns != null) {
            List<CampaignRegister> campaignRegisters = subscription.getCampaignRegisters();
            for (final Campaign campaign : campaigns) {
                boolean addable = true;

                if (campaignRegisters != null) {
                    for (final CampaignRegister register : campaignRegisters) {
                        if (register.getCampaign().equals(campaign) &&
                                Minutes.minutesBetween(register.getJoinDate(), DateTime.now()).toStandardDays().getDays() > VAS_CAMPAIGN_ACTIVATION_THRESHOLD) {
                            addable = false;
                            break;
                        }
                    }
                }

                List<SubscriptionVAS> archivedVasList = vasPersistenceFacade.findBySubscription(subscription.getId());
                log.info(String.format("archived vas count = %s", archivedVasList.size()));
                if (archivedVasList != null && campaign.getVasList() != null) {
                    List<ValueAddedService> campaignVasList = campaign.getVasList();
                    for (final SubscriptionVAS archiveVas : archivedVasList) {
                        if (archiveVas.getVas().getId() == vas.getId()) {
                            addable = false;
                            break;
                        }
                        if (Minutes.minutesBetween(archiveVas.getActiveFromDate(), DateTime.now()).toStandardDays().getDays() < VAS_CAMPAIGN_ACTIVATION_THRESHOLD) {
                            continue;
                        }
                        for (final ValueAddedService campaignVas : campaignVasList) {
                            if (archiveVas.getVas().getId() == campaignVas.getId()) {
                                addable = false;
                                break;
                            }
                        }
                        if (!addable) {
                            break;
                        }
                    }
                }

                if (campaign.isActivateOnManualPaymentOnly()) {
                    addable = false;
                }
                if (!addable) {
                    continue;
                }

                if (campaign.getTarget().equals(CampaignTarget.PAYMENT)) {
                    long bonusAmount = campaign.getBonusCount().longValue() *
                            vas.getPrice();

                    subscriptionFacade.increasePromoBalance(subscription, bonusAmount);
                    String dsc = String.format(
                            "promo balance added=%d, agreement=%s, added vas=%d",
                            bonusAmount,
                            subscription.getAgreement(),
                            vas.getId());
                    log.info(dsc);
                    systemLogger.success(SystemEvent.SUBSCRIPTION_TOPUP_PROMO_BALANCE, subscription, dsc);

                    CampaignRegister register = add(subscription, campaign, bonusAmount, null, false, null, null, null); //activate campaign
                    register.setStatus(CampaignStatus.ACTIVE);
                    register.decrementLifecycleCount();
                    String desc = String.format(
                            "agreement=%s, campaign id=%d, bonus amount=%d",
                            subscription.getAgreement(),
                            register.getCampaign().getId(),
                            bonusAmount);
                    systemLogger.success(SystemEvent.SUBSCRIPTION_CAMPAIGN_JOINED, subscription, null, desc);
                    subscriptionFacade.update(subscription);
                    update(register);
                }
            }
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void tryActivateCampaignOnManualPayment(Long subscriptionId, Long campaignId, Long paymentId) {
        log.info(String.format("tryActivateCampaignOnManualPayment method starts for subscriptionId: %s, campaignId: %s, paymentId: %s", subscriptionId, campaignId, paymentId));
        long start = System.currentTimeMillis();
        Campaign campaign = campaignFacade.find(campaignId);
        Payment payment = paymentFacade.find(paymentId);
        Subscription subscription = subscriptionFacade.find(subscriptionId);
        //Subscription subscription = register.getSubscription();
        if (subscription.getSubscriber().getDetails().getType().equals(SubscriberType.CORP) &&
                !campaign.isCorporateEnabled()) {
            log.error("Cannot add manual campaigns for corporate subscribers");
            return;
        }
        List<CampaignRegister> actives = findNotProcessedBySubscription(subscription);
        if (actives != null) {
            for (CampaignRegister register : actives) {
                if (register.getCampaign().getId() == campaign.getId()) {
                    log.error(
                            String.format(
                                    "The campaign %d has already been added(or activated) with agreement %s [subscription: %s]. Cannot re-assign.",
                                    campaign.getId(),
                                    subscription.getAgreement(),
                                    subscriptionId)
                    );
                    return;
                }
            }
        }

        //long monthlyFee = 1000000;
        long monthlyFee = 0;
        if (!campaign.isVasOnly()) {
            monthlyFee = subscription.getService().getServicePrice();
        }
        List<SubscriptionVAS> vasList = subscription.getVasList();
        List<ValueAddedService> appliedVasList = new ArrayList<>();
        if (vasList != null) {
            List<ValueAddedService> campaignVasList = campaign.getVasList();
            for (SubscriptionVAS vas : vasList) {
                boolean addable = false;
                if (campaignVasList != null) {
                    for (ValueAddedService campaignVas : campaignVasList) {
                        if (campaignVas.getId() == vas.getVas().getId()) {
                            addable = true;
                        }
                    }
                }
                if (addable) {
                    appliedVasList.add(vas.getVas());
                    monthlyFee += vas.getVas().getPrice();
                }
            }
        }

        if (appliedVasList.isEmpty() && campaign.isVasOnly()) {
            log.debug(
                    String.format(
                            "Vas only campaign %d cannot be added because there is no vas for subscription %d",
                            campaign.getId(),
                            subscription.getId()));
            return;
        }

        long amount = payment.getAmountAsLong();
        Long bonusAmount = null;
        Double serviceRateBonusAmount = 0.0;
        switch (campaign.getTarget()) {
            case RESOURCE_INTERNET_BANDWIDTH:
                ResourceBucket bucket = subscription.getService().getResourceBucketByType(ResourceBucketType.INTERNET_DOWN);

                if (bucket != null) {
                    bonusAmount = Long.valueOf(bucket.getCapacity()) * campaign.getBonusCount().longValue();
                }
                break;
            case SERVICE_RATE:
                serviceRateBonusAmount = campaign.getBonusCount();
                break;
            case PAYMENT:
                bonusAmount = (long) (1.0 * monthlyFee * campaign.getBonusCount());
                break;
        }

        monthlyFee = subscription.rerate(monthlyFee);
        boolean activationSuccess = false;
        long updatedLifecycleCount = -1L;

        switch (campaign.getTarget()) {
            case PAYMENT:
                if (amount >= (campaign.getCount() * monthlyFee)) { //qualifies
                    subscriptionFacade.increasePromoBalance(subscription, bonusAmount);
                    activationSuccess = true;
                    String dsc = String.format(
                            "promo balance added = %d, agreement = %s, payment amount = %d",
                            bonusAmount,
                            subscription.getAgreement(),
                            amount);
                    log.info(dsc);
                    systemLogger.success(SystemEvent.SUBSCRIPTION_TOPUP_PROMO_BALANCE, subscription, dsc);
                } else {
                    FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "could not activate campaign", "payment amount not sufficient"));
                }
                break;
            case SERVICE_RATE:
                log.debug("[" + subscriptionId + "] SERVICE_RATE_DEBUG: payment = " + (amount + 100000.0));//add another 1 azn
                log.debug("[" + subscriptionId + "] SERVICE_RATE_DEBUG: threshhold = " + (serviceRateBonusAmount * campaign.getCount() * monthlyFee));
                if ((amount + 100000.0) >= (serviceRateBonusAmount * campaign.getCount() * monthlyFee)) {
                    activationSuccess = true;
                }
                break;
            case RESOURCE_INTERNET_BANDWIDTH:
                if (subscription.getService().getServiceType() == ServiceType.BROADBAND) {
                    ResourceBucket bucket = subscription.getService().getResourceBucketByType(ResourceBucketType.INTERNET_DOWN);

                    if (bucket != null) {
                        //bonusAmount = Long.valueOf(bucket.getCapacity()) * register.getCampaign().getBonusCount().longValue();
                        if (campaign.getLifeCycleCount() == -1) { //indicates that lifecycle depends on the payment amount
                            updatedLifecycleCount = amount / monthlyFee;
                        }
                        if (campaign.getLifeCycleCount() > 0 || updatedLifecycleCount >= campaign.getCount()) {
                            if (bonusAmount > 0) {
                                subscription.setBucketCapacityByType(ResourceBucketType.INTERNET_DOWN, String.valueOf(bonusAmount));
                                subscription.setBucketCapacityByType(ResourceBucketType.INTERNET_UP, String.valueOf(bonusAmount));
                                activationSuccess = true;
                            }
                        }
                    }
                }
                break;
        }

        if (activationSuccess) {
            CampaignRegister register = add(subscription, campaign, bonusAmount, null, false, appliedVasList, serviceRateBonusAmount, null); //activate campaign
            if (updatedLifecycleCount > 0) {
                register.setLifeCycleCount((int) updatedLifecycleCount);
            }
            register.setStatus(CampaignStatus.ACTIVE);
            register.decrementLifecycleCount();
            String desc = String.format(
                    "agreement = %s, campaign id = %d, bonus amount = %d",
                    subscription.getAgreement(),
                    register.getCampaign().getId(),
                    bonusAmount);
            if (campaign.getTarget().equals(CampaignTarget.SERVICE_RATE)) {
                refundSubscription(register, subscription);
            }
            if (campaign.getId() == 16569987L && campaign.isPartialPromoTransfer()) { //hack for STB_6_months free citynet campaign
                subscriptionFacade.usePromoBalance(subscription, campaign.getPromoTransferLimit());
                String msg = String.format("campaing register id = %d, promo amount = %d", register.getId(), campaign.getPromoTransferLimit());
                log.info(String.format("Partial transferation of promo to real balance: %s", msg));
                systemLogger.success(SystemEvent.PARTIAL_PROMO_TRANSFER, register.getSubscription(), msg);
            }
            payment.setCampaignId(campaignId);
            paymentFacade.update(payment);
//            ProvisioningEngine provisioner = null;
            try {
                log.info("Provisioning process starts for subscription [" + subscriptionId + "] after campaign [" + campaignId + "] joined");
                ProvisioningEngine provisioner = provisioningFactory.getProvisioningEngine(subscription);
                boolean res = provisioner.reprovision(subscription);
                if (res) {
                    systemLogger.success(SystemEvent.REPROVISION, subscription, String.format("status=%s", subscription.getStatus()));
                } else {
                    systemLogger.error(SystemEvent.REPROVISION, subscription, String.format("status=%s", subscription.getStatus()));
                }
            } catch (ProvisionerNotFoundException e) {
                log.error("Error occurs during provisioning process for subscription " + subscriptionId, e);
            }
            systemLogger.success(SystemEvent.SUBSCRIPTION_CAMPAIGN_JOINED, subscription, null, desc);
            log.info("Campaign join process completed successfully for subscription " + subscriptionId);
        }
        log.info(String.format("tryActivateCampaignOnManualPayment method ends for subscriptionId: %s, campaignId: %s, paymentId: %s [elapsed time: %s seconds]", subscriptionId, campaignId, paymentId, (System.currentTimeMillis() - start) / 1000.));
    }

    private void refundSubscription(CampaignRegister campaignRegister, Subscription subscription) {
        log.info(String.format("refundSubscription started. subscription id = %d", subscription.getId()));

        Long monthlyRate = subscription.rerate(subscription.getService().getServicePrice());
        Long discountedRate = Double.valueOf(Math.ceil(monthlyRate * campaignRegister.getServiceRateBonusAmount())).longValue();
        Long diffRate = monthlyRate - discountedRate;

        log.info(String.format("diffRate = %d for subscription id = %d", diffRate, subscription.getId()));

        creditDiff(subscription, diffRate);

        log.debug("vas list size: " + subscription.getVasList() == null ? 0 : subscription.getVasList().size());
        if (subscription.getVasList() != null && !subscription.getVasList().isEmpty()) {
            log.info(String.format("Subscription id=%d, agreement=%s vas count = %s",
                    subscription.getId(), subscription.getAgreement(), subscription.getVasList().size()));

            Double bonusDiscount = campaignRegister.getServiceRateBonusAmount();
            List<ValueAddedService> applicableVasList = campaignRegister.getVasList();

            for (SubscriptionVAS vas : subscription.getVasList()) {
                log.info(String.format("Subscription id=%d, agreement=%s vas id=%d, name=%s, status=%s, type=%s, price=%d",
                        subscription.getId(), subscription.getAgreement(), vas.getId(), vas.getVas().getName(), vas.getStatus(), vas.getVas().getCode().getType(),
                        vas.getPrice()));

                if (vas.getVas().getCode().getType() == ValueAddedServiceType.PERIODIC_STATIC) {
                    long vasPrice = vas.getVas().getPrice();

                    if ((vas.getRemainCount() != null && vas.getRemainCount() == -99L) || vas.getStatus() == SubscriptionStatus.FINAL) {
                        continue;
                    }

                    long vasRate = (long) (vasPrice * vas.getCount());
                    vasRate = subscription.rerate(vasRate);
                    long discountedVasRate = vasRate;
                    if (bonusDiscount != null) {
                        boolean applicable = false;
                        if (applicableVasList != null) {
                            for (ValueAddedService campaignVas : applicableVasList) {
                                if (campaignVas.getId() == vas.getVas().getId()) {
                                    applicable = true;
                                    break;
                                }
                            }
                        }
                        if (applicable) {
                            discountedVasRate = (long) (1.0 * vasRate * bonusDiscount);
                        }
                    }

                    long rateDiff = vasRate - discountedVasRate;
                    if (rateDiff > 0) {
                        log.info(String.format("vasDiffRate = %d for subscription id = %d", rateDiff, subscription.getId()));
                        creditDiff(subscription, rateDiff);
                    }
                }
            }
        }
        log.info(String.format("refundSubscription ended. subscription id = %d", subscription.getId()));
    }

    private void creditDiff(Subscription subscription, long diffRate) {
        AccountingTransaction accTransaction = new AccountingTransaction();
        accTransaction.setType(AccountingTransactionType.BATCH_ADJUSTMENT);
        Transaction fromTransaction = transFacade.createTransation(
                TransactionType.CREDIT,
                subscription, diffRate,
                String.format("Credit adjustment of %f AZN for Subscription %s of Subscriber %s",
                        1.0 * diffRate / 100000, subscription.getAgreement(), subscription.getSubscriber().getMasterAccount()));

        log.info(String.format("Transaction id = %d, after balance = %d",
                fromTransaction.getId(), subscription.getBalance().getRealBalance()));

        Operation fromOperation = new Operation();
        fromOperation.setSubscription(subscription);
        fromOperation.setAmount(subscription.getBalance().getRealBalance());
        fromOperation.setUser(userFacade.find(20000L));
        fromOperation.setCategory(accCatFacade.findByType(AccountingCategoryType.BALANCE_REFUND));
        fromOperation.setTransaction(fromTransaction);
        fromOperation.setProvider(fromOperation.getSubscription().getService().getProvider());
        fromOperation.setAccTransaction(accTransaction);

        accTransaction.addOperation(fromOperation);
        accTransaction.setUser(fromOperation.getUser());
        accTransaction.setProvider(fromOperation.getProvider());

        accTransFacade.save(accTransaction);

        systemLogger.success(SystemEvent.BALANCE_TRANSFER, subscription, fromTransaction,
                String.format(
                        "transaction id=%d, amount=%f, accounting tranaction id=%d",
                        fromTransaction.getId(),
                        1.0 * diffRate / 100000,
                        accTransaction.getId()
                ));
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void tryActivateCampaign(CampaignRegister register, boolean... update) {
        String logHeader = String.format("tryActivateCampaign: register id=%d for subscription %s", register.getId(), register.getSubscription().getId());
        if (update.length == 0)
            register = update(register);

        if (register.getStatus() != CampaignStatus.NOT_ACTIVE) {
            throw new IllegalArgumentException(String.format("%s register status is %s", logHeader, register.getStatus()));
        }

        Long bonusAmount = null;
        //Long paymentAsLong = Float.valueOf(Double.valueOf(payment.getAmount()).floatValue() * 100000).longValue();

        bonusAmount = register.getBonusAmount();
        Subscription subscription = register.getSubscription();

        DateTime bonusDate = register.getBonusDate();

        switch (register.getCampaign().getTarget()) {
            case RESOURCE_INTERNET_BANDWIDTH:
                if (subscription.getService().getServiceType() == ServiceType.BROADBAND) {
                    ResourceBucket bucket = subscription.getService().getResourceBucketByType(ResourceBucketType.INTERNET_DOWN);

                    if (bucket != null) {
                        //bonusAmount = Long.valueOf(bucket.getCapacity()) * register.getCampaign().getBonusCount().longValue();
                        log.info(String.format("%s bandwidth returned: %d", logHeader, bonusAmount));

                        log.info(String.format("create: subscription agreement=%s, found resource campaign, new bandwidth=%s", subscription.getAgreement(), bonusAmount));

                        if (bonusAmount > 0) {
                            subscription.setBucketCapacityByType(ResourceBucketType.INTERNET_DOWN, String.valueOf(bonusAmount));
                            subscription.setBucketCapacityByType(ResourceBucketType.INTERNET_UP, String.valueOf(bonusAmount));
                        }
                    }
                }
                break;
            case PAYMENT:
                String msg = String.format("subscription id=%d, register id=%d, promo amount = %d", subscription.getId(), register.getId(), bonusAmount);
                subscriptionFacade.increasePromoBalance(subscription, bonusAmount);
                log.info("tryActivateCampaign: successfully increment promo balance by " + bonusAmount);
                systemLogger.success(SystemEvent.SUBSCRIPTION_TOPUP_PROMO_BALANCE, subscription, msg);
                break;
            case SERVICE_RATE:
                break;
            case EXPIRATION_DATE:
                register.setNobonusDate(subscription.getExpirationDate());
                subscription.setExpirationDate(bonusDate);
                subscription.setBilledUpToDate(subscription.getExpirationDate());
                subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDate().plusDays(
                        subscription.getBillingModel().getGracePeriodInDays()));
                break;
        }

        register.setStatus(CampaignStatus.ACTIVE);
        if (register.getCampaign().getTarget() != CampaignTarget.SERVICE_RATE) {
            register.decrementLifecycleCount();
        }

        if (register.getCampaign().isCancelInvoice()) {
            subscriptionFacade.removeDebt(subscription, String.format("Campaign register id=%d", register.getId()));
        }

        systemLogger.success(SystemEvent.SUBSCRIPTION_CAMPAIGN_JOINED, subscription, null,
                String.format(
                        "campaign id=%d, bonusAmount=%d, bonusDate=%s, status=%s",
                        register.getCampaign().getId(),
                        (bonusAmount != null ? bonusAmount : 0L),
                        (bonusDate != null ? bonusDate.toString() : ""),
                        register.getStatus()));
        subscription = subscriptionFacade.update(subscription);
        try {
            provisioningFactory.getProvisioningEngine(subscription).openService(subscription, subscription.getExpirationDateWithGracePeriod());
        } catch (Exception ex) {
            log.error("Cannot open service: subscription id=" + subscription.getId(), ex);
        }
    }

    public void removeDetached(CampaignRegister campaignRegister) {
        CampaignRegister entity = getEntityManager().getReference(CampaignRegister.class, campaignRegister.getId());
        getEntityManager().remove(entity);
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void suspendCampaignUntilBillingTime(Subscription subscription, long campaignId) {
        try {
            Optional<CampaignRegister> registerOptional = findAllBySubscription(subscription)
                    .stream()
                    .peek(c -> System.out.println("Subscription " + subscription.getId() + " active campaign: " + c))
                //  .filter(r -> r.getStatus() == CampaignStatus.ACTIVE && r.getCampaign().getId() == campaignId && r.getJoinDate().toLocalDate().equals(LocalDate.now()))
                    .filter(r -> r.getStatus() == CampaignStatus.ACTIVE && r.getCampaign().getId() == campaignId)
                    .findAny();

            if (registerOptional.isPresent()) {
                CampaignRegister register = registerOptional.get();
                register.setStatus(CampaignStatus.NOT_ACTIVE);
                register.setLifeCycleCount(register.getLifeCycleCount() + 1);
                update(register);

                // Hack CityNet 2x speed campaign
                ResourceBucket down = subscription.getService().getResourceBucketByType(ResourceBucketType.INTERNET_DOWN);
                ResourceBucket up = subscription.getService().getResourceBucketByType(ResourceBucketType.INTERNET_UP);
                subscription.setBucketCapacityByType(ResourceBucketType.INTERNET_DOWN, down.getCapacity());
                subscription.setBucketCapacityByType(ResourceBucketType.INTERNET_UP, up.getCapacity());
                subscriptionFacade.update(subscription);
                systemLogger.success(SystemEvent.SUBSCRIPTION_CAMPAIGN_SUSPENDED, subscription, String.format("Campaign id %s suspended", campaignId));
                log.info(String.format("Campaign with id %s of Subscription %s will suspend until billing time, campaign register: %s", campaignId, subscription.getId(), register));

                log.info("Reprovisioning process starts for subscription [" + subscription.getId() + "] after campaign [" + campaignId + "] suspended");
                ProvisioningEngine provisioner = provisioningFactory.getProvisioningEngine(subscription);
                boolean res = provisioner.reprovision(subscription);
                if (res) {
                    systemLogger.success(SystemEvent.REPROVISION, subscription, String.format("Campaign with id %s suspended until billing time, status=%s", campaignId, subscription.getStatus()));
                } else {
                    systemLogger.error(SystemEvent.REPROVISION, subscription, String.format("Campaign with id %s suspended until billing time, status=%s", campaignId, subscription.getStatus()));
                }
            } else {
                log.info("There is no active campaign of subscription " + subscription.getId());
            }
        } catch (ProvisionerNotFoundException e) {
            log.error("Error occurs during reprovisioning process of suspended campaign for subscription " + subscription.getId(), e);
        } catch (Exception ex) {
            log.error(ex.getMessage() + " -> exception occurs when suspending campaign "+campaignId+" of subscription "+subscription.getId(), ex);
        }
    }
}
