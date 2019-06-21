package com.jaravir.tekila.engines;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.module.campaign.*;
import com.jaravir.tekila.module.service.*;
import com.jaravir.tekila.module.service.entity.*;
import com.jaravir.tekila.module.store.equip.Equipment;
import com.jaravir.tekila.module.store.ip.persistence.IpAddress;
import com.jaravir.tekila.module.subscription.persistence.entity.*;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import com.jaravir.tekila.provision.broadband.devices.exception.NoFreePortLeftException;
import com.jaravir.tekila.provision.broadband.devices.exception.PortAlreadyReservedException;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spring.controller.vm.SubscriptionCreationVM;

import javax.ejb.*;
import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * Created by kmaharov on 18.05.2017.
 */
@Stateless(name = "NarfixOperationsEngine", mappedName = "NarfixOperationsEngine")
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class NarfixOperationsEngine implements OperationsEngine {
    private final Logger log = LoggerFactory.getLogger(NarfixOperationsEngine.class);

    @EJB
    private CitynetCommonEngine citynetCommonEngine;
    @EJB
    private CommonOperationsEngine commonOperationsEngine;

    @Override
    public Subscription createSubscriptionSpring(SubscriptionCreationVM subscriptionCreationVM, Subscriber selectedSubscriber, Subscription subscription, Service service, User user) {

        log.info("Narfix createSubscriptionSpring method starts..");

        /*
        ...

       //parametrler providera uygunlasdirilmalidir..
        createSubscription(
                selectedSubscriber,
                subscription,
                serviceId,
                selectedMiniPop,
                isUseStockEquipment,
                selectedEquipment,
                notificationSettings,
                installFee,
                selectedCampaign,
                null,
                user
        );

        ...
        */

        return subscription;
    }

    @Override
    public void createSubscription(Subscriber selectedSubscriber, Subscription subscription, String serviceId, MiniPop miniPop, boolean isUseStockEquipment, Equipment equipment, List<NotificationSettingRow> notificationSettings, double installationFeeRate, Campaign selectedCampaign, HttpSession session, User user,String ... additionalProperties) throws NoFreePortLeftException, PortAlreadyReservedException {
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
        return "nf" + params.ats + "00" + (params.str != null ? params.str.getStreetIndex() : "") + "00" + params.building + "00" + params.apartment;
    }
}
