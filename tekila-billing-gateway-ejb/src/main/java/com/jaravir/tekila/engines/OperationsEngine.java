package com.jaravir.tekila.engines;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.module.campaign.Campaign;
import com.jaravir.tekila.module.service.NotificationSettingRow;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.store.equip.Equipment;
import com.jaravir.tekila.module.store.ip.persistence.IpAddress;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscriber;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionVAS;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import com.jaravir.tekila.provision.broadband.devices.exception.NoFreePortLeftException;
import com.jaravir.tekila.provision.broadband.devices.exception.PortAlreadyReservedException;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import spring.controller.vm.SubscriptionCreationVM;
import spring.exceptions.BadRequestAlertException;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * Created by khsadigov on 5/16/2017.
 */
public interface OperationsEngine extends BaseEngine {
    void createSubscription(Subscriber selectedSubscriber,
                            Subscription subscription,
                            String serviceId,
                            MiniPop miniPop,
                            boolean isUseStockEquipment,
                            Equipment equipment,
                            List<NotificationSettingRow> notificationSettings,
                            double installationFee,
                            Campaign selectedCampaign,
                            HttpSession session,
                            User user,
                            String... additionalProperties) throws NoFreePortLeftException, PortAlreadyReservedException;

    Subscription createSubscriptionSpring(Subscriber selectedSubscriber,
                            Subscription subscription,
                            Long serviceId,
                            MiniPop miniPop,
                            boolean isUseStockEquipment,
                            Equipment equipment,
                            List<NotificationSettingRow> notificationSettings,
                            double installationFee,
                            Campaign selectedCampaign,
                            User user,
                            String... additionalProperties) throws NoFreePortLeftException, PortAlreadyReservedException;

//    default Subscription createSubscriptionSpring(SubscriptionCreationVM subscriptionCreationVM, Subscriber selectedSubscriber, Subscription subscription, Service service){
//        return subscription;
//    }

    Subscription createSubscriptionSpring(SubscriptionCreationVM subscriptionCreationVM, Subscriber selectedSubscriber, Subscription subscription, Service service, User user);


    Subscription changeService(Subscription subscription, Service service, boolean upCharge, boolean downCharge);

    Subscription addVAS(VASCreationParams params);

    Subscription removeVAS(Subscription subscription, SubscriptionVAS vas);

    Subscription removeVAS(Subscription subscription, ValueAddedService vasID);

    Subscription editVAS(VasEditParams params);

    void activatePrepaid(Subscription subscription);

    Subscription prolongPrepaid(Subscription subscription);

    boolean activatePostpaid(Subscription subscription) throws ProvisionerNotFoundException;

    Subscription changeStatus(Subscription subscription, SubscriptionStatus newStatus);

    String generateAgreement(AgreementGenerationParams params);
}
