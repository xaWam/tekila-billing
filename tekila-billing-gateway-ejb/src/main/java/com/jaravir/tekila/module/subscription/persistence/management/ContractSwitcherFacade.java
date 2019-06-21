package com.jaravir.tekila.module.subscription.persistence.management;

import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.engines.OperationsEngine;
import com.jaravir.tekila.module.accounting.entity.Charge;
import com.jaravir.tekila.module.accounting.entity.Payment;
import com.jaravir.tekila.module.accounting.entity.Transaction;
import com.jaravir.tekila.module.accounting.manager.AsyncTaskPersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.ChargePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.PaymentPersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.TransactionPersistenceFacade;
import com.jaravir.tekila.module.service.ServiceSettingType;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.service.entity.ServiceProperty;
import com.jaravir.tekila.module.service.entity.SubscriptionServiceType;
import com.jaravir.tekila.module.service.persistence.manager.ServicePersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.ServicePropertyPersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.SubscriptionServiceTypePersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.*;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.broadband.devices.manager.Providers;
import com.jaravir.tekila.tools.WrapperAgreementChangeBatch;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import javax.ejb.*;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author ElmarMa on 5/15/2018
 */
@Stateless
public class ContractSwitcherFacade implements ContractSwitcherFacadeLocal {

    private final static Logger logger = Logger.getLogger(ContractSwitcherFacade.class);


    @EJB
    private SubscriptionPersistenceFacade subscriptionPf;

    @EJB
    private PaymentPersistenceFacade paymentPf;

    @EJB
    private ChargePersistenceFacade chargePf;

    @EJB
    private TransactionPersistenceFacade transactionPf;

    @EJB
    private SystemLogger systemLogger;

    @EJB
    private EngineFactory engineFactory;

    @EJB
    private ServicePersistenceFacade servicePersistenceFacade;

    @EJB
    private AsyncTaskPersistenceFacade asyncTaskPersistenceFacade;

    @EJB
    private SubscriptionServiceTypePersistenceFacade serviceTypePersistenceFacade;

    @EJB
    private ServicePropertyPersistenceFacade servicePropertyPersistenceFacade;

    @Override
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void switchToNewContract(WrapperAgreementChangeBatch batchRow) throws Exception {
        try {


            logger.info("=============== STARTING TO CHANGE AGREEMENT ==================" + batchRow);
            logger.info("SUBSCRIPTION : " + batchRow.getOldAgreement());
            logger.info("TARGET DATA : " + batchRow);

            Subscription subscription = null;

            try {
                subscription = subscriptionPf
                        .findSubscriptionByAgreementAndProviders(batchRow.getOldAgreement(),
                                Arrays.asList(
                                        Providers.DATAPLUS.getId(),
                                        Providers.CNC.getId(),
                                        Providers.GLOBAL.getId()
                                ), true);
            } catch (Exception e) {
                e.printStackTrace(System.err);
            } finally {
                if (subscription == null) {
                    try {
                        subscription = subscriptionPf
                                .findSubscriptionByAgreementAndProviders(batchRow.getOldAgreement(),
                                        Arrays.asList(
                                                Providers.DATAPLUS.getId(),
                                                Providers.CNC.getId(),
                                                Providers.GLOBAL.getId()
                                        ), false);
                        batchRow.setOldAgreement(subscription.getAgreement());
                        logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> like query worked " + batchRow.getOldAgreement() + " to " + subscription.getAgreement());
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                    }
                }
            }

            if (subscription == null)
                throw new EJBException("subscription not found " + batchRow.getOldAgreement());
            logger.info("batch change subscription founded " + subscription.getAgreement());
            //////////////IF HAS DEBT REFUND IT////////////////////

            if (subscription.getBalance().getRealBalance() < 0) {
                logger.info("batchrow " + batchRow + " real balance is " + subscription.getBalance().getRealBalance());
                long refund = 0L;

                if (subscription.getService().getProvider().getId() == Providers.CNC.getId() ||
                        subscription.getService().getProvider().getId() == Providers.GLOBAL.getId()) {
                    refund = subscription.getServiceFeeRate();
                    refund = refund == 0 ? subscription.getService().getServicePrice() : refund;
                } else if (subscription.getService().getProvider().getId() == Providers.DATAPLUS.getId()) {
                    ServiceProperty prop = servicePropertyPersistenceFacade.find(
                            subscription.getService(),
                            subscription.getSettingByType(ServiceSettingType.ZONE)
                    );
                    if (prop != null)
                        refund = prop.getPrice();
                }
                logger.info("batchrow " + batchRow + " real balance refund amount is " + refund);
                subscription.getBalance().creditReal(refund);
            }

            //////////////////////////////////////////////////////
            /////////////////////CHANGING DETAILS//////////////////////
            logger.info(">>>>>>>>>>>>> starting to modify subscriber details " + subscription.getAgreement() + " to " + batchRow);
            SubscriberDetails details = subscription.getSubscriber().getDetails();
            if (batchRow.getName() != null && !batchRow.getName().isEmpty())
                details.setFirstName(batchRow.getName());
            if (batchRow.getSurname() != null && !batchRow.getSurname().isEmpty())
                details.setSurname(batchRow.getSurname());
            if (batchRow.getFatherName() != null && !batchRow.getFatherName().isEmpty())
                details.setMiddleName(batchRow.getFatherName());
            if (batchRow.getMobile() != null && !batchRow.getMobile().isEmpty())
                details.setPhoneMobile(batchRow.getMobile());
            if (batchRow.getHomeNumber() != null && !batchRow.getHomeNumber().isEmpty())
                details.setPhoneLandline(batchRow.getHomeNumber());
            if (batchRow.getName() != null && !batchRow.getName().isEmpty())
                details.setPinCode(batchRow.getPinCode());
            if (batchRow.getIdSerialNumber() != null && !batchRow.getIdSerialNumber().isEmpty())
                details.setPassportSeries(batchRow.getIdSerialNumber());
            if (batchRow.getAddress() != null && !batchRow.getAddress().isEmpty())
                details.setStreet(batchRow.getAddress());
            SubscriptionDetails subscriptionDetails = subscription.getDetails();
            if (batchRow.getName() != null && !batchRow.getName().isEmpty())
                subscriptionDetails.setName(batchRow.getName());
            if (batchRow.getSurname() != null && !batchRow.getSurname().isEmpty())
                subscriptionDetails.setSurname(batchRow.getSurname());
            if (batchRow.getAddress() != null && !batchRow.getAddress().isEmpty())
                subscriptionDetails.setStreet(batchRow.getAddress());
            ///////////////////////////////////////////////////////////
            logger.info("batch change getting operation engine " + subscription.getAgreement());
            OperationsEngine operationsEngine = engineFactory.getOperationsEngine(subscription);
            Service targetService = servicePersistenceFacade
                    .findServiceByNameAndProvider(batchRow.getServiceName(), subscription.getService().getProvider().getId());

            if (targetService == null)
                throw new EJBException("can not find target service ,service name is " + batchRow.getServiceName());
            logger.info("batch change getting founding target service " + subscription.getAgreement() + " service is " + targetService.getName());

            logger.info("starting provision agreement >>>>>>>>>>>>>>>> " + batchRow);

            provisionAgreements(subscription, batchRow);

            subscription.setService(targetService);

            if (subscription.getService().getProvider().getId() == Providers.CNC.getId() ||
                    subscription.getService().getProvider().getId() == Providers.GLOBAL.getId()) {
                subscription.setServiceFeeRateonChange(targetService.getServicePrice());
            }

            logger.info("batch change try to prolong " + subscription.getAgreement());

            //**prolong prepaid subscription one month**//
            if (batchRow.getHasPromo())
                subscription = operationsEngine.prolongPrepaid(subscription);
            /////////////////////////////////////////////////////////////
            logger.info("batch change try to prolong finished " + subscription.getAgreement());

            subscription.setAgreement(batchRow.getAgreement());

            logger.info("batch change try to change charges " + subscription.getAgreement());

            List<Charge> charges = chargePf.findByAgreement(batchRow.getOldAgreement());
            for (Charge c : charges) {
                c.setSubscription(subscription);
                chargePf.update(c);
            }
            logger.info("batch change try to change finsihed " + subscription.getAgreement());

            logger.info("batch change try to change payments " + subscription.getAgreement());

            List<Payment> payments = paymentPf.findAllByAgreement(batchRow.getOldAgreement());
            for (Payment p : payments) {
                p.setAccount(subscription);
                p.setContract(batchRow.getAgreement());
                paymentPf.update(p);
            }


            logger.info("batch change try to change payments finished " + subscription.getAgreement());

            logger.info("batch change try to change transactions " + subscription.getAgreement());

            List<Transaction> transactions = transactionPf.findByAgreement(batchRow.getOldAgreement());
            for (Transaction t : transactions) {
                t.setSubscription(subscription);
                transactionPf.update(t);
            }

            logger.info("batch change try to change transactions finished " + subscription.getAgreement());


            subscription = subscriptionPf.update(subscription);

            logger.info("starting normal provisioning >>>>>>>>>>>>>>> " + subscription.getAgreement());
            provision(subscription);
            logger.info("starting normal provisioning >>>>>>>>>>>>>>> finished" + subscription.getAgreement());

            addSwitchingLog(batchRow.getOldAgreement(), batchRow.getAgreement(), subscription);

            logger.info("=============== CHANGING AGREEMENT FINISHED SUCCESSFULLY ==================");
        } catch (Exception e) {
            logger.info("error in batch " + batchRow);
//            logger.error("error in batch " + e);
            logger.error("Error occurs during switchToNewContract() method: "+e.getMessage(), e);
            throw new EJBException(e.getMessage(), e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void provisionAgreements(Subscription subscription, WrapperAgreementChangeBatch batchRow) {
        try {

//            if (subscription.getService().getProvider().getId() != Providers.DATAPLUS.getId()) {
//                AsyncProvisioningTask task = new AsyncProvisioningTask();
//                task.setProviderId(subscription.getService().getProvider().getId());
//                task.setAccount(batchRow.getOldAgreement());
//                task.setNewAccount(batchRow.getAgreement());
//                task.setEventTime(new Date());
//                task.setEventType(EventType.CHANGE_AGREEMENT);
//                task.setTaskStatus(0);
//                asyncTaskPersistenceFacade.save(task);
//            } else {
                engineFactory.getProvisioningEngine(subscription).provisionNewAgreements(batchRow.getOldAgreement(), batchRow.getAgreement());
//            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            logger.error(e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void provision(Subscription subscription) {

        try {

            engineFactory.getProvisioningEngine(subscription).reprovision(subscription);

        } catch (Exception e) {
            logger.error("error in provision >>>>>>>>>> " + e);
            e.printStackTrace(System.err);
        }
    }


    @TransactionAttribute(value = TransactionAttributeType.REQUIRED)
    private void addSwitchingLog(String oldAgreement, String targetAgreement, Subscription subscription) {
        systemLogger.success(SystemEvent.CONTRACT_UPDATES, subscription,
                "subscription agreement changed from " + oldAgreement + " , to " + targetAgreement);
    }


}
