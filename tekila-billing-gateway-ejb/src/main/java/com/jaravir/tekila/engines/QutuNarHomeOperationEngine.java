package com.jaravir.tekila.engines;


import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.model.BillingModelPersistenceFacade;
import com.jaravir.tekila.base.persistence.manager.BillingSettingsManager;
import com.jaravir.tekila.equip.EquipmentPersistenceFacade;
import com.jaravir.tekila.module.accounting.InvoiceState;
import com.jaravir.tekila.module.accounting.entity.Charge;
import com.jaravir.tekila.module.accounting.entity.Invoice;
import com.jaravir.tekila.module.accounting.entity.Transaction;
import com.jaravir.tekila.module.accounting.entity.TransactionType;
import com.jaravir.tekila.module.accounting.manager.ChargePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.InvoicePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.TransactionPersistenceFacade;
import com.jaravir.tekila.module.campaign.*;
import com.jaravir.tekila.module.event.BillingEvent;
import com.jaravir.tekila.module.queue.PersistentQueueManager;
import com.jaravir.tekila.module.service.*;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.service.entity.ServiceSetting;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.service.model.BillingPrinciple;
import com.jaravir.tekila.module.service.persistence.manager.NotificationSettingPersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.ServicePersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.ServiceSettingPersistenceFacade;
import com.jaravir.tekila.module.store.equip.Equipment;
import com.jaravir.tekila.module.store.equip.EquipmentStatus;
import com.jaravir.tekila.module.subscription.persistence.entity.*;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import com.jaravir.tekila.provision.broadband.devices.Port;
import com.jaravir.tekila.provision.broadband.devices.exception.NoFreePortLeftException;
import com.jaravir.tekila.provision.broadband.devices.exception.PortAlreadyReservedException;
import com.jaravir.tekila.provision.broadband.devices.manager.MiniPopPersistenceFacade;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import com.jaravir.tekila.provision.exception.ProvisioningException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spring.controller.vm.SubscriptionCreationVM;

import javax.ejb.*;
import javax.servlet.http.HttpSession;
import java.util.List;


/**
 * Created by ShakirG on 10/09/2018.
 */

@Stateless(name = "QutuNarHomeOperationEngine", mappedName = "QutuNarHomeOperationEngine")
public class QutuNarHomeOperationEngine implements OperationsEngine {
    private final static Logger log = LoggerFactory.getLogger(QutuNarHomeOperationEngine.class);

    @javax.annotation.Resource
    private SessionContext ctx;
    @EJB
    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;
    @EJB
    private SystemLogger systemLogger;
    @EJB
    private BillingSettingsManager billSettings;
    @EJB
    private EngineFactory provisioningFactory;
    @EJB
    private CommonOperationsEngine commonOperationsEngine;
    @EJB
    private NarHomeCommonEngine narHomeCommonEngine;


    @Override
    public Subscription createSubscriptionSpring(SubscriptionCreationVM subscriptionCreationVM, Subscriber selectedSubscriber, Subscription subscription, Service service, User user) {

        log.info("QutuNarHome createSubscriptionSpring method starts..");

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
    public void createSubscription(Subscriber selectedSubscriber,
                                   Subscription subscription,
                                   String serviceId,
                                   MiniPop miniPop,
                                   boolean isUseStockEquipment,
                                   Equipment equipment,
                                   List<NotificationSettingRow> notificationSettings,
                                   double installationFeeRate,
                                   Campaign selectedCampaign,
                                   HttpSession session,
                                   User user, String ... additionalProperties)

            throws NoFreePortLeftException, PortAlreadyReservedException {
                            narHomeCommonEngine.createSubscription(selectedSubscriber,
                                                                    subscription,
                                                                    serviceId,
                                                                    miniPop,
                                                                    isUseStockEquipment,
                                                                    equipment,
                                                                    notificationSettings,
                                                                    installationFeeRate,
                                                                    selectedCampaign,
                                                                    session,
                                                                    user,
                                                                    additionalProperties);

        log.debug("end of QutuNarHomeOperationsEngine");
    }

    @Override
    public Subscription createSubscriptionSpring(Subscriber selectedSubscriber, Subscription subscription, Long serviceId, MiniPop miniPop, boolean isUseStockEquipment, Equipment equipment, List<NotificationSettingRow> notificationSettings, double installationFee, Campaign selectedCampaign, User user, String... additionalProperties) throws NoFreePortLeftException, PortAlreadyReservedException {
        return subscription;
    }

    @Override
    public Subscription changeService(Subscription subscription, Service targetService, boolean upCharge, boolean downCharge) {
        return narHomeCommonEngine.changeService(subscription, targetService, upCharge, downCharge);
    }

    @Override
    public Subscription addVAS(VASCreationParams params) {
        return commonOperationsEngine.addVAS(params);
    }

    @Override
    public Subscription removeVAS(Subscription subscription, SubscriptionVAS vas) {
        return commonOperationsEngine.removeVAS(subscription, vas);
    }

    @Override
    public Subscription removeVAS(Subscription subscription, ValueAddedService vasID) {
        return commonOperationsEngine.removeVAS(subscription, vasID);
    }

    @Override
    public Subscription editVAS(VasEditParams params) {
        return commonOperationsEngine.editVAS(params);
    }

    @Override
    public void activatePrepaid(Subscription subscription) {
        narHomeCommonEngine.activatePrepaid(subscription);

    }

    @Override
    public Subscription prolongPrepaid(Subscription subscription) {
        return commonOperationsEngine.prolongPrepaid(subscription);
    }

    @Override
    public boolean activatePostpaid(Subscription subscription) throws ProvisionerNotFoundException {
        return commonOperationsEngine.activatePostpaid(subscription);
    }

    @Override
    public Subscription changeStatus(Subscription subscription, SubscriptionStatus newStatus) {
        return commonOperationsEngine.changeStatus(subscription, newStatus);
    }

    @Override
    public String generateAgreement(AgreementGenerationParams params) {
        if (params.agreement != null) {
            return params.agreement;
        }
        return null;
    }

}

