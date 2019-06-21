package com.jaravir.tekila.module.system.reproccessing;

import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.engines.ProvisioningEngine;
import com.jaravir.tekila.module.accounting.entity.Payment;
import com.jaravir.tekila.module.accounting.manager.PaymentPersistenceFacade;
import com.jaravir.tekila.module.campaign.CampaignRegisterPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.module.system.operation.OperationResult;
import com.jaravir.tekila.module.system.reproccessing.log.ReproccessingLogger;
import com.jaravir.tekila.module.web.service.exception.NoInvoiceFoundException;
import com.jaravir.tekila.module.web.service.exception.NoSuchSubscriberException;
import com.jaravir.tekila.module.web.service.exception.NoSuchSubscriptionException;
import com.jaravir.tekila.module.web.service.provider.BillingServiceProvider;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.annotation.security.RunAs;
import javax.ejb.*;
import java.util.List;

/**
 * Created by sajabrayilov on 24.01.2015.
 */
@DeclareRoles({"system"})
@Singleton
@RunAs("system")
public class Reproccessor {
    @EJB
    private ReproccessingLogger reproccessingLogger;
    @EJB
    private PaymentPersistenceFacade paymentFacade;
    @EJB
    private BillingServiceProvider billingGateway;
    @EJB
    private SubscriptionPersistenceFacade subFacade;
    @EJB
    private EngineFactory provisioningFactory;
    @EJB
    private SystemLogger systemLogger;
    @EJB
    private EngineFactory engineFactory;
    @EJB
    private CampaignRegisterPersistenceFacade campaignRegisterPersistenceFacade;

    @Resource
    private SessionContext ctx;

    private final static Logger log = LoggerFactory.getLogger(Reproccessor.class);

    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */ // @Schedule(hour = "*", minute = "*/2")
    public void reproccessPaymentsDataplus() {
        //String principalName = ctx.getCallerPrincipal().getName();
        log.info("Starting reproccessing payments as principal: ");
        DateTime lastUpdateDate = DateTime.now().minusMinutes(20);
        List<Payment> paymentList = paymentFacade.findAllUnproccessedDataplus(lastUpdateDate);

        if (paymentList == null || paymentList.isEmpty()) {
            log.info("No unproccessed payments found");
            return;
        }
        log.info(String.format("Found unproccessed payments (total: %d) - %s", paymentList.size(), paymentList));

        String message = null;

        long start = System.currentTimeMillis();

        for (Payment paymentOriginal : paymentList) {
            Payment payment = paymentOriginal;
            try {
                payment = paymentFacade.findForceRefresh(payment.getId());
                if (payment.getProcessed() == 0) {
                    log.info("Reproccessing payment %s" + payment);

                    boolean result = billingGateway.settlePayment(payment.getSubscriber_id(), payment.getAccount().getId(), payment.getAmount(), payment.getId());
                    //log.debug("Result returned: " + result);
                    if (result) {
                        // TRY TO ACTIVATE FIRST MONTH FREE CAMPAIGNS
                        try {
                            if (paymentFacade.isFirstPayment(payment.getAccount(), payment.getId()))
                                campaignRegisterPersistenceFacade.tryActivateCampaign(payment.getAccount().getId(), payment.getId());
                        } catch (Exception e) {
                            systemLogger.error(SystemEvent.SUBSCRIPTION_CAMPAIGN_ACTIVE, payment.getAccount(), "can not activate campaign for data plus");
                        }
                        message = String.format("Payment %d successfully reproccessed", payment.getId());
                        log.info(message);
                        reproccessingLogger.save(new ReproccessorLogRecord(payment, OperationResult.SUCCESS, message));
                    } else {
                        message = String.format("Payment %d reproccessed unsuccessfully", payment.getId());
                        log.error(message);
                        reproccessingLogger.save(new ReproccessorLogRecord(payment, OperationResult.FAILURE, message));
                    }
                }
            } catch (NoSuchSubscriberException e) {
                message = String.format("Payment %d not reproccessed - subscriber %d not found", payment.getId(), payment.getSubscriber_id());
                log.error(message, e);
                reproccessingLogger.save(new ReproccessorLogRecord(payment, OperationResult.FAILURE, message + ": " + e.getCause().getMessage()));
            } catch (NoSuchSubscriptionException e) {
                message = String.format("Payment %d not reproccessed - subscription %d not found", payment.getId(), payment.getAccount().getId());
                log.error(message, e);
                reproccessingLogger.save(new ReproccessorLogRecord(payment, OperationResult.FAILURE, message + ": " + e.getCause().getMessage()));
            } catch (NoInvoiceFoundException e) {
                message = String.format("Payment %d not reproccessed - invoice not found", payment.getId());
                log.error(message, e);
                reproccessingLogger.save(new ReproccessorLogRecord(payment, OperationResult.FAILURE, message + ": " + e.getCause().getMessage()));
            } catch (Exception ex) {
                message = String.format("Payment %d reproccessed unsuccessfully", payment.getId(), ex);
                log.error(message);
                reproccessingLogger.save(new ReproccessorLogRecord(payment, OperationResult.FAILURE, message + ": " + ex.getCause().getMessage()));
            }
        }
        log.info("reproccessPaymentsDataplus  elapsed time : {}",(System.currentTimeMillis()-start)/1000);
        log.info("Finished reproccessing payments");
    }

    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */ // @Schedule(hour = "*", minute = "*/5")
    //@Schedule(hour = "*", minute = "*")
    public void reproccessPayments() {
        String principalName = ctx.getCallerPrincipal().getName();
        log.info(
                "Starting reproccessing payments as principal: ");
        DateTime lastUpdateDate = DateTime.now().minusMinutes(20);
        List<Payment> paymentList = paymentFacade.findAllUnproccessed(lastUpdateDate);

        if (paymentList == null || paymentList.isEmpty()) {
            log.info("No unproccessed payments found");
            return;
        }
        log.info(String.format("Found unproccessed payments (total: %d) - %s", paymentList.size(), paymentList));

        String message = null;

        try {
            Thread.sleep(60000);
        } catch (InterruptedException ex) {
            log.error("Exception on reproccessPayments", ex);
        }

        long start = System.currentTimeMillis();
        for (Payment paymentOriginal : paymentList) {
            Payment payment = paymentOriginal;
            try {
                payment = paymentFacade.findForceRefresh(payment.getId());
                if (payment.getProcessed() == 0) {
                    log.info("Reproccessing payment %s" + payment);

                    boolean result = billingGateway.settlePayment(payment.getSubscriber_id(), payment.getAccount().getId(), payment.getAmount(), payment.getId());
                    //log.debug("Result returned: " + result);
                    if (result) {
                        message = String.format("Payment %d successfully reproccessed", payment.getId());
                        log.info(message);
                        reproccessingLogger.save(new ReproccessorLogRecord(payment, OperationResult.SUCCESS, message));
                    } else {
                        message = String.format("Payment %d reproccessed unsuccessfully", payment.getId());
                        log.error(message);
                        reproccessingLogger.save(new ReproccessorLogRecord(payment, OperationResult.FAILURE, message));
                    }
                }
            } catch (NoSuchSubscriberException e) {
                message = String.format("Payment %d not reproccessed - subscriber %d not found", payment.getId(), payment.getSubscriber_id());
                log.error(message, e);
                reproccessingLogger.save(new ReproccessorLogRecord(payment, OperationResult.FAILURE, message + ": " + e.getCause().getMessage()));
            } catch (NoSuchSubscriptionException e) {
                message = String.format("Payment %d not reproccessed - subscription %d not found", payment.getId(), payment.getAccount().getId());
                log.error(message, e);
                reproccessingLogger.save(new ReproccessorLogRecord(payment, OperationResult.FAILURE, message + ": " + e.getCause().getMessage()));
            } catch (NoInvoiceFoundException e) {
                message = String.format("Payment %d not reproccessed - invoice not found", payment.getId());
                log.error(message, e);
                reproccessingLogger.save(new ReproccessorLogRecord(payment, OperationResult.FAILURE, message + ": " + e.getCause().getMessage()));
            } catch (Exception ex) {
                message = String.format("Payment %d reproccessed unsuccessfully", payment.getId(), ex);
                log.error(message);
                reproccessingLogger.save(new ReproccessorLogRecord(payment, OperationResult.FAILURE, message + ": " + ex.getCause().getMessage()));
            }
        }
        log.info("reproccessPayments  elapsed time : {}",(System.currentTimeMillis()-start)/1000);
        log.info("Finished reproccessing payments");
    }

    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */ // @Schedule(hour = "*", minute = "*/15")
    public void reprocessActivations() {
        log.info("reprocessActivations: Starting reproccessing subscriptions not prolonged on external systems");
        List<Subscription> subList = subFacade.findLatelyPaidAndNotProlonged();

        log.info(String.format("reprocessActivations: Found %d subscriptions", subList != null ? subList.size() : 0));
        ProvisioningEngine provisioner = null;
        String header = null;

        long start = System.currentTimeMillis();
        if (subList != null && !subList.isEmpty()) {

            try {
                Thread.sleep(60000);
            } catch (InterruptedException ex) {
                log.error("Exception on reproccessActivation", ex);
            }


            for (Subscription sub : subList) {
                Subscription subCheck = sub;
                subCheck = subFacade.findForceRefresh(subCheck.getId());

                if (subCheck.getExpirationDateWithGracePeriod().isBefore(subCheck.getBilledUpToDate())) {


                    header = String.format("reprocessActivations: agreement=%s, subscription id=%d", sub.getAgreement(), sub.getId());
                    log.info(String.format("%s starting reprocessing", header));
                    if (sub.getStatus().equals(SubscriptionStatus.FINAL)
                            || sub.getStatus().equals(SubscriptionStatus.CANCEL)
                            || sub.getStatus().equals(SubscriptionStatus.SUSPENDED)
                            || sub.getStatus().equals(SubscriptionStatus.PRE_FINAL)) {
                        log.info(String.format("Subscription id = %d. Not eligible for activation because of current status", sub.getId()));
                        continue;
                    }
                    try {
                        provisioner = provisioningFactory.getProvisioningEngine(sub);

                        if (provisioner.openService(sub)) {
                            engineFactory.getOperationsEngine(sub).prolongPrepaid(sub);
                            log.info(String.format("%s activation reprocessing successfull", header, sub.getAgreement(), sub.getId()));
                            systemLogger.success(SystemEvent.SUBSCRIPTION_ACTIVATION_REPROCESS, sub, "");
                        } else {
                            log.error(String.format("%s activation reprocessing failed", header, sub.getAgreement(), sub.getId()));
                            systemLogger.error(SystemEvent.SUBSCRIPTION_ACTIVATION_REPROCESS, sub, "");

                        }
                    } catch (Exception ex) {
                        log.error(String.format("%s activation reprocessing failed", header, sub.getAgreement(), sub.getId()), ex);
                        systemLogger.error(SystemEvent.SUBSCRIPTION_ACTIVATION_REPROCESS, sub, ex.getCause().getMessage());
                    }


                    log.info(String.format("%s finished reprocessing", header));
                }
            }
        }
        log.info("reprocessActivations  elapsed time : {}",(System.currentTimeMillis()-start)/1000);
        log.info("reprocessActivations: Finished reproccessing subscriptions not prolonged on external systems");
    }
}
