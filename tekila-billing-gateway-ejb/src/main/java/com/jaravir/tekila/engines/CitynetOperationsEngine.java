package com.jaravir.tekila.engines;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.model.BillingModelPersistenceFacade;
import com.jaravir.tekila.base.persistence.manager.BillingSettingsManager;
import com.jaravir.tekila.equip.EquipmentPersistenceFacade;
import com.jaravir.tekila.module.campaign.*;
import com.jaravir.tekila.module.event.BillingEvent;
import com.jaravir.tekila.module.queue.PersistentQueueManager;
import com.jaravir.tekila.module.service.*;
import com.jaravir.tekila.module.service.entity.*;
import com.jaravir.tekila.module.service.model.BillingModel;
import com.jaravir.tekila.module.service.model.BillingPrinciple;
import com.jaravir.tekila.module.store.ats.AtsPersistenceFacade;
import com.jaravir.tekila.module.store.equip.Equipment;
import com.jaravir.tekila.module.store.ip.persistence.IpAddress;
import com.jaravir.tekila.module.subscription.exception.DuplicateAgreementException;
import com.jaravir.tekila.module.subscription.persistence.entity.*;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import com.jaravir.tekila.provision.broadband.devices.MinipopCategory;
import com.jaravir.tekila.provision.broadband.devices.Port;
import com.jaravir.tekila.provision.broadband.devices.exception.NoFreePortLeftException;
import com.jaravir.tekila.provision.broadband.devices.exception.PortAlreadyReservedException;
import com.jaravir.tekila.provision.broadband.devices.manager.MiniPopPersistenceFacade;
import com.jaravir.tekila.provision.broadband.devices.manager.Providers;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spring.controller.vm.SubscriptionCreationVM;
import spring.dto.SubscriptionDetailsDTO;
import spring.exceptions.BadRequestAlertException;

import javax.ejb.*;
import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * Created by kmaharov on 18.05.2017.
 */
@Stateless(name = "CitynetOperationsEngine", mappedName = "CitynetOperationsEngine")
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class CitynetOperationsEngine implements OperationsEngine {
    private final static Logger log = LoggerFactory.getLogger(CitynetOperationsEngine.class);

    @EJB
    private CitynetCommonEngine citynetCommonEngine;

    @EJB
    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;

    @EJB
    private EngineFactory engineFactory;

    @EJB
    private PersistentQueueManager queueManager;

    @EJB
    private SystemLogger systemLogger;

    @EJB
    private MiniPopPersistenceFacade minipopFacade;

    @EJB
    private EquipmentPersistenceFacade equipmentPersistenceFacade;

    @EJB
    private AtsPersistenceFacade atsPersistenceFacade;

    @EJB
    private BillingModelPersistenceFacade modelFacade;

    @EJB
    private BillingSettingsManager billSettings;

    @Override
    public Subscription createSubscriptionSpring(SubscriptionCreationVM subscriptionCreationVM,
                                                 Subscriber selectedSubscriber,
                                                 Subscription subscription,
                                                 Service service,
                                                 User user) {

           log.info("Citynet createSubscriptionSpring method starts..");


           MiniPop selectedMiniPop = null;
           Campaign selectedCampaign = null;
           boolean isUseStockEquipment = false;

            if (service != null && service.getServiceType() == ServiceType.BROADBAND
                    && (service.getProvider().getId() == Providers.NARFIX.getId() || service.getProvider().getId() == Providers.CITYNET.getId())) {
                if (selectedMiniPop == null) {
                    selectedMiniPop = findTempMinipop(service.getProvider());
                    if (selectedMiniPop == null) {
                        throw new BadRequestAlertException("Cannot find default minipop");
                    }
                }
            }

            if (service != null && service.getServiceType() == ServiceType.BROADBAND && selectedMiniPop == null) {
                log.debug("Cannot create subscription. Minipop not selected");
                throw new BadRequestAlertException("Cannot create subscription. Minipop not selected");
            }

            if (service != null && service.getSettingByType(ServiceSettingType.BROADBAND_SWITCH) != null
                    && selectedMiniPop != null && selectedMiniPop.getPreferredPort() != null
                    && !selectedMiniPop.checkPort(selectedMiniPop.getPreferredPort())) {
                log.debug("Cannot create subscription. Minipop's preferred port is invalid. " + selectedMiniPop);
                throw new BadRequestAlertException("Cannot create subscription. Minipop's preferred port is invalid.");
            }


//            if (getSubscriptionVAS() != null && getSubscriptionVAS().getSettings().size() > 0) {
//                subscription.addVAS(subscriptionVAS);
//            }
            SubscriptionDetailsDTO subscriptionDetailsDTO = subscriptionCreationVM.getSubscriptionDto() != null ? subscriptionCreationVM.getSubscriptionDto().getDetails() : null;

            Ats ats = atsPersistenceFacade.find(subscriptionCreationVM.getAtsId());

            if(subscriptionDetailsDTO != null){
                if (!(ats != null
                        && subscriptionDetailsDTO.getApartment() != null
                        && subscriptionDetailsDTO.getBuilding() != null
                        && subscriptionDetailsDTO.getStreet() != null)) {
                    throw new BadRequestAlertException("Incorrect aggrement");
                }
            }


            if (subscriptionPersistenceFacade.findByAgreementOrdinary(subscription.getAgreement()) != null) {
                throw new BadRequestAlertException("Agreement exist, please provide another");
            }

            if (service != null && service.getDefaultVasList() != null
                    && (subscription.getVasList().size() < service.getDefaultVasList().size())) {
//                log.debug("Not all charges for default VAS added: subscriptionVas: " + subscriptionVAS.getSettings() + ", defaultVasList: " + service.getDefaultVasList());
                throw new BadRequestAlertException("Not all charges added");
            }


            try {


                subscriptionPersistenceFacade.findByAgreement(subscription.getAgreement());
//                details.setName(name);
//                details.setSurname(surname);
//                subscription.setDetails(details);
//                log.debug("Billing model: " + billingModel);
                BillingModel billingModel = modelFacade.find(subscriptionCreationVM.getBillingPrinciple());
                subscription.setBillingModel(billingModel);
//                subscription.getDetails().setLanguage(language);
//                log.debug("Selected minipop: " + selectedMiniPop);
//                 Port port = minipopFacade.getAvailablePort(selectedMiniPop);
                // log.debug("Resulting port: " + port);


                OperationsEngine operationsEngine = engineFactory.getOperationsEngine(service.getProvider());
                operationsEngine.createSubscription(
                        selectedSubscriber,
                        subscription,
                        String.valueOf(service.getId()), // Yes WTF - Poltergeist
                        selectedMiniPop,
                        isUseStockEquipment,
                        subscriptionCreationVM.getEquipmentID() != null
                                ? equipmentPersistenceFacade.find(subscriptionCreationVM.getEquipmentID())
                                : null, // Yes WTF again ;) -> Why not pass simple ID? especially Java pass by value -> it means -> createSubscription will create another instance, which is a memory issue.
                        null,
                        subscription.getInstallationFee(),
                        selectedCampaign,
                        null,
                        user
                );
                systemLogger.success(SystemEvent.SUBSCRIPTION_CREATED, subscription,
                        "Subscription created"
                );
                try {
                    log.info("Adding created subscription to Notification queue, subscription id: {}", subscription.getId());
                    queueManager.addToNotificationsQueue(
                            BillingEvent.SUBSCRIPTION_ADDED,
                            subscription.getId(),
                            null);
                    systemLogger.success(SystemEvent.NOTIFICATION_SCHEDULED, subscription,
                            String.format("Event=" + BillingEvent.SUBSCRIPTION_ADDED)
                    );
                } catch (Exception ex) {
                    log.error("Cannot add to notification queue: ", ex);
                    systemLogger.error(SystemEvent.NOTIFICATION_SCHEDULED, subscription, ex.getCause().getMessage());
                }

                log.info("Subscription created successfully. Subscription agreement =" + subscription.getAgreement());

            } catch (DuplicateAgreementException ex) {
                log.error("Cannot create subscriptipn: ", ex);
                throw new BadRequestAlertException("Agreement already exists");
            } catch (NoFreePortLeftException ex) {
                log.error("Cannot create subscription: ", ex);
                throw new BadRequestAlertException("Minipop has no free ports left");
            } catch (PortAlreadyReservedException ex) {
                String msg = String.format("Port %d already reserved or illegal", selectedMiniPop.getPreferredPort());
                log.error("Cannot create subscription: ", ex);
                throw new BadRequestAlertException(msg);
            } catch (Exception ex) {
                log.error("Cannot create subscriptipn: ", ex);
                systemLogger.error(
                        SystemEvent.SUBSCRIPTION_CREATED,
                        subscription,
                        ex.getCause().getMessage());
                throw new BadRequestAlertException("Cannot create subscriptipn");
            }

        return subscription;
    }

    public MiniPop findTempMinipop(ServiceProvider provider) {
        MiniPop tempMinipop = null;
        List<MiniPop> testMinipops = minipopFacade.findByCategory(provider, MinipopCategory.TEST);
        if (testMinipops == null) {
            log.debug("Cannot find test minipop");
            return null;
        }
        for (MiniPop miniPop : testMinipops) {
            if (miniPop.getNextAvailablePortHintAsNumber() != null && miniPop.getNextAvailablePortHintAsNumber() <= miniPop.getNumberOfPorts()) {
//                log.debug("Found temp minipop: " + miniPop);
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
                                   double installationFeeRate,
                                   Campaign selectedCampaign,
                                   HttpSession session,
                                   User user,
                                   String ... additionalProperties) throws NoFreePortLeftException, PortAlreadyReservedException {
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
        subscription = subscriptionPersistenceFacade.update(subscription);
        log.debug("ActivatePrepaid, agreement=" + subscription.getAgreement()+ ", "+subscription.toString());

        if (subscription.getStatus() == SubscriptionStatus.INITIAL) {
            subscription.setActivationDate(DateTime.now());
        }

        DateTime expDate = (subscription.getBillingModel().getPrinciple() == BillingPrinciple.GRACE) ? subscription.getExpirationDate()
                : DateTime.now();

        if (billSettings.getSettings().getPrepaidlifeCycleLength() == 30) {
            DateTime estimatedNextExpirationDate = expDate.plusMonths(1);
            if (estimatedNextExpirationDate.getDayOfMonth() < expDate.getDayOfMonth()) {
                estimatedNextExpirationDate = estimatedNextExpirationDate.plusDays(1);
            }
            subscription.setExpirationDate(estimatedNextExpirationDate);
        } else {
            subscription.setExpirationDate(expDate.plusDays(billSettings.getSettings().getPrepaidlifeCycleLength()));
        }

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setLastStatusChangeDate(DateTime.now());
        }

        subscription.synchronizeExpiratioDates();

        subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDate().plusDays(
                subscription.getBillingModel().getGracePeriodInDays()
        ));
        //fix to new billing model
        if (subscription.getBillingModel().getPrinciple() == BillingPrinciple.GRACE_MONTH)
            subscription.setExpirationDateWithGracePeriod(subscription.getExpirationDate().plusMonths(1));

        DateTimeFormatter frm = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

        subscription.setBilledUpToDate(subscription.getExpirationDate());
        log.info(
                String.format("Subscription id=%d, agreement=%s, status=%s, biiledUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s successfully activated",
                        subscription.getId(),
                        subscription.getAgreement(),
                        subscription.getStatus(),
                        subscription.getBilledUpToDate() != null ? subscription.getBilledUpToDate().toString(frm) : null,
                        subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                        subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                ));
        systemLogger.success(
                SystemEvent.SUBSCRIPTION_STATUS_ACTIVE,
                subscription,
                String.format("status=%s, biiledUpToDate=%s, expirationDate=%s, expirationDateWithGP=%s successfully activated",
                        subscription.getStatus(),
                        subscription.getBilledUpToDate() != null ? subscription.getBilledUpToDate().toString(frm) : null,
                        subscription.getExpirationDate() != null ? subscription.getExpirationDate().toString(frm) : null,
                        subscription.getExpirationDateWithGracePeriod() != null ? subscription.getExpirationDateWithGracePeriod().toString(frm) : null
                ));
        return subscription;
    }

    @Override
    public boolean activatePostpaid(Subscription subscription) throws ProvisionerNotFoundException {
        return citynetCommonEngine.activatePostpaid(subscription);
    }

    @Override
    public Subscription changeStatus(Subscription subscription, SubscriptionStatus newStatus) {
        return citynetCommonEngine.changeStatus(subscription, newStatus);
    }

    @Override
    public String generateAgreement(AgreementGenerationParams params) {
        return params.ats + "00" + (params.str != null ? params.str.getStreetIndex() : "") + "00" + params.building + "00" + params.apartment;
    }
}
