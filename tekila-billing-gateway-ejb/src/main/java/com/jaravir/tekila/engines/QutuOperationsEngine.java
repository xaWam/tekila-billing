package com.jaravir.tekila.engines;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.model.BillingModelPersistenceFacade;
import com.jaravir.tekila.module.campaign.Campaign;
import com.jaravir.tekila.module.service.NotificationSettingRow;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.service.entity.ServiceProvider;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.service.model.BillingPrinciple;
import com.jaravir.tekila.module.store.equip.Equipment;
import com.jaravir.tekila.module.store.ip.persistence.IpAddress;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscriber;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionVAS;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriberPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import com.jaravir.tekila.provision.broadband.devices.MinipopCategory;
import com.jaravir.tekila.provision.broadband.devices.exception.NoFreePortLeftException;
import com.jaravir.tekila.provision.broadband.devices.exception.PortAlreadyReservedException;
import com.jaravir.tekila.provision.broadband.devices.manager.MiniPopPersistenceFacade;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import spring.controller.vm.SubscriptionCreationVM;
import spring.mapper.SubscriberMapper;
import spring.mapper.subscription.SubscriptionMapper;
import com.jaravir.tekila.provision.broadband.devices.manager.MiniPopPersistenceFacade;


import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Random;

/**
 * Created by kmaharov on 22.05.2017.
 */
@Stateless(name = "QutuOperationsEngine", mappedName = "QutuOperationsEngine")
public class QutuOperationsEngine implements OperationsEngine {
    private final static Logger log = LoggerFactory.getLogger(QutuOperationsEngine.class);

    @EJB
    private CitynetCommonEngine citynetCommonEngine;
    @EJB
    private CommonOperationsEngine commonOperationsEngine;

    @EJB
    private MiniPopPersistenceFacade minipopFacade;

    @EJB
    private BillingModelPersistenceFacade modelFacade;

    @Override
    public Subscription createSubscriptionSpring(SubscriptionCreationVM subscriptionCreationVM,
                                                 Subscriber selectedSubscriber,
                                                 Subscription subscription,
                                                 Service service,
                                                 User user) {


        log.info("~~~~~~SPRING SUBSCRIPTION CREATION FOR QUTUCITYNET~~~~~~~~~");

        log.info("~~~~SERVICE : " + service.toString());

        MiniPop minipop = minipopFacade.find(subscriptionCreationVM.getMinipopId());

        if(minipop == null){
            log.info("Minipop not selected finding temporary minipop....");
            minipop = findTempMinipop(service.getProvider());
        }

        subscription.setBillingModel(modelFacade.find(BillingPrinciple.GRACE));


        try {
            createSubscription(selectedSubscriber,
                    subscription,
                    String.valueOf(subscriptionCreationVM.getServiceId()),
                    minipop,
                    false,
                    null,
                    null,
                    0D,
                    null,
                    null,
                    user
            );
        } catch (NoFreePortLeftException e) {
            e.printStackTrace();
        } catch (PortAlreadyReservedException e) {
            e.printStackTrace();
        }

        return subscription;
    }


//    @Override
//    public Subscription createSubscriptionSpring(SubscriptionCreationVM subscriptionCreationVM,
//                                                 Subscriber selectedSubscriber,
//                                                 Subscription subscription,
//                                                 Service service,
//                                                 User user) {
//
//        log.info("Qutu createSubscriptionSpring method starts..");
//        MiniPop minipop = null;
//        if (minipop == null) {
//            minipop = findTempMinipop(service.getProvider());
//            if (minipop == null) {
//                throw new RuntimeException("Minipop is null for Qutu");
//            }
//        }
//
//        try {
//            createSubscription(selectedSubscriber,
//                    subscription,
//                    String.valueOf(service.getId()),
//                    minipop,
//                    false,
//                    null,
//                    null,
//                    0D,
//                    null,
//                    null,
//                    user
//            );
//        } catch (NoFreePortLeftException e) {
//            log.error("No free port left for this minipop ", e);
//        } catch (PortAlreadyReservedException e) {
//            log.error("The port is already reserved ", e);
//        }
//
//        log.debug("~~~~~End of subscription creation for QutuOperationsEngine....");
//
//        return subscription;
//    }

    private MiniPop findTempMinipop(ServiceProvider provider) {
        MiniPop tempMinipop = null;
        List<MiniPop> testMinipops = minipopFacade.findByCategory(provider, MinipopCategory.TEST);
        if (testMinipops == null) {
            log.debug("Cannot find test minipop");
            return null;
        }
        for (MiniPop miniPop : testMinipops) {
            if (miniPop.getNextAvailablePortHintAsNumber() != null && miniPop.getNextAvailablePortHintAsNumber() <= miniPop.getNumberOfPorts()) {
                log.debug("Found temp minipop: " + miniPop);
                tempMinipop = miniPop;
                break;
            }
        }
        return tempMinipop;
    }


    @Override
    public void createSubscription(Subscriber selectedSubscriber,
                                   Subscription subscription,
                                   String serviceId,
                                   MiniPop miniPop,
                                   boolean isUseStockEquipment,
                                   Equipment equipment,
                                   List<NotificationSettingRow> notificationSettings,
                                   double installationFeeRate, Campaign selectedCampaign,
                                   HttpSession session,
                                   User user,String ... additionalProperties) throws NoFreePortLeftException, PortAlreadyReservedException {
        citynetCommonEngine.createSubscription(
                selectedSubscriber,
                subscription,
                serviceId,
                miniPop,
                isUseStockEquipment,
                equipment,
                notificationSettings,
                installationFeeRate,
                selectedCampaign,
                session,
                user,additionalProperties);
    }

    @Override
    public Subscription createSubscriptionSpring(Subscriber selectedSubscriber, Subscription subscription, Long serviceId, MiniPop miniPop, boolean isUseStockEquipment, Equipment equipment, List<NotificationSettingRow> notificationSettings, double installationFee, Campaign selectedCampaign, User user, String... additionalProperties) throws NoFreePortLeftException, PortAlreadyReservedException {
        return subscription;
    }

    @Override
    public Subscription changeService(Subscription subscription, Service service, boolean upCharge, boolean downCharge) {
        return citynetCommonEngine.changeService(subscription, service, upCharge, downCharge);
    }

    @Override
    public Subscription addVAS(VASCreationParams params) {
        return citynetCommonEngine.addVAS(params);
    }

    @Override
    public Subscription removeVAS(Subscription subscription, SubscriptionVAS vas) {
        return citynetCommonEngine.removeVAS(subscription, vas);
    }

    @Override
    public Subscription removeVAS(Subscription subscription, ValueAddedService vasID) {
        return citynetCommonEngine.removeVAS(subscription, vasID);
    }

    @Override
    public Subscription editVAS(VasEditParams params) {
        return citynetCommonEngine.editVAS(params);
    }

    @Override
    public void activatePrepaid(Subscription subscription) {
        citynetCommonEngine.activatePrepaid(subscription);
    }

    @Override
    public Subscription prolongPrepaid(Subscription subscription) {
        return citynetCommonEngine.prolongPrepaid(subscription);
    }

    @Override
    public boolean activatePostpaid(Subscription subscription) throws ProvisionerNotFoundException {
        return citynetCommonEngine.activatePostpaid(subscription);
    }

    @Override
    public Subscription changeStatus(Subscription subscription, SubscriptionStatus newStatus) {
        return commonOperationsEngine.changeStatus(subscription, newStatus);
    }

    @Override
    public String generateAgreement(AgreementGenerationParams params) {
        return params.msisdn + "Q" + (Integer.parseInt(params.agreement.substring(10)) + 1);
    }
}
