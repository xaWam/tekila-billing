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
import com.jaravir.tekila.module.service.persistence.manager.VASPersistenceFacade;
import com.jaravir.tekila.module.store.equip.Equipment;
import com.jaravir.tekila.module.store.equip.EquipmentStatus;
import com.jaravir.tekila.module.store.ip.persistence.IpAddress;
import com.jaravir.tekila.module.subscription.exception.DuplicateAgreementException;
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
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import spring.controller.vm.SubscriptionCreationVM;
import spring.exceptions.BadRequestAlertException;
import spring.mapper.subscription.SubscriptionDetailMapper;

import javax.ejb.*;
import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * Created by kmaharov on 18.05.2017.
 */
@Stateless(name = "GeneralOperationsEngine", mappedName = "GeneralOperationsEngine")
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class GeneralOperationsEngine implements OperationsEngine {
    private final static Logger log = Logger.getLogger(GeneralOperationsEngine.class);

    @javax.annotation.Resource
    private SessionContext ctx;
    @EJB
    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;
    @EJB
    private ServicePersistenceFacade serviceFacade;
    @EJB
    private ServiceSettingPersistenceFacade serviceSettingFacade;
    @EJB
    private SystemLogger systemLogger;
    @EJB
    private MiniPopPersistenceFacade miniPopFacade;
    @EJB
    private CampaignJoinerBean campaignJoinerBean;
    @EJB
    private CampaignRegisterPersistenceFacade campaignRegisterFacade;
    @EJB
    private InvoicePersistenceFacade invoiceFacade;
    @EJB
    private ChargePersistenceFacade chargeFacade;
    @EJB
    private TransactionPersistenceFacade transFacade;
    @EJB
    private PersistentQueueManager queueManager;
    @EJB
    private BillingSettingsManager billSettings;
    @EJB
    private BillingModelPersistenceFacade modelFacade;
    @EJB
    private EquipmentPersistenceFacade equipmentFacade;
    @EJB
    private NotificationSettingPersistenceFacade notifSettingFacade;
    @EJB
    private EngineFactory provisioningFactory;
    @EJB
    private CommonOperationsEngine commonOperationsEngine;
    @EJB
    private NarHomeCommonEngine narHomeCommonEngine;
    @EJB
    private VASPersistenceFacade vasPersistenceFacade;




    @Override
    public Subscription createSubscriptionSpring(SubscriptionCreationVM subscriptionCreationVM,
                                                 Subscriber selectedSubscriber,
                                                 Subscription subscription,
                                                 Service service, User user) {
        log.info("GeneralOperationsEngine createSubscriptionSpring method starts..");


        log.info("~~~SPRING SUBSCRIPTION CREATION FOR NARHOME~~~");




        Equipment equipment = null;
        MiniPop miniPop = null;
        SubscriptionVAS subscriptionVAS = null;
        ValueAddedService vas = null;
        int cableCharge = 0;

        if(subscriptionCreationVM.getEquipmentID() != null){
            equipment = equipmentFacade.findById(subscriptionCreationVM.getEquipmentID());
        }

        if(subscriptionCreationVM.getMinipopId() != null){
            miniPop = miniPopFacade.find(subscriptionCreationVM.getMinipopId());
        }

        if(subscriptionCreationVM.getSubscriptionDto().isUseEquipmentFromStock() && equipment == null){
            log.debug("~~~Cannot create subscription. Equipment is not selected");
            throw new BadRequestAlertException("Cannot create subscription. Equipment is not selected");
        }

        if (service != null && service.getSettingByType(ServiceSettingType.BROADBAND_SWITCH) != null && miniPop == null) {
            log.debug("~~~Cannot create subscription. Minipop not selected");
            throw new BadRequestAlertException("Cannot create subscription. Minipop not selected");
        }

        if (service != null && service.getSettingByType(ServiceSettingType.BROADBAND_SWITCH) != null
                && miniPop != null && miniPop.getPreferredPort() != null
                && !miniPop.checkPort(miniPop.getPreferredPort())) {
            log.debug("~~~Cannot create subscription. Minipop's preferred port is invalid. " + miniPop);
            throw new BadRequestAlertException("Cannot create subscription. Minipop's preferred port is invalid.");
        }

        //Dandiq solution for adding default VASes...
        if(subscriptionCreationVM.getSubscriptionVASSettingsVM() != null){

            log.info("~~~SUBSCRIPTION-VAS-SETTING-VM-TO-STRING-START~~~");
            log.info(subscriptionCreationVM.getSubscriptionVASSettingsVM().toString());
            log.info("~~~SUBSCRIPTION-VAS-SETTING-VM-TO-STRING-END~~~");

            if(subscriptionCreationVM.getSubscriptionVASSettingsVM().getId() == null
                    || subscriptionCreationVM.getSubscriptionVASSettingsVM().getValue() == null
                    || subscriptionCreationVM.getSubscriptionVASSettingsVM().getValue().isEmpty()
                    || subscriptionCreationVM.getSubscriptionVASSettingsVM().getLength() == null
                    || subscriptionCreationVM.getSubscriptionVASSettingsVM().getLength().isEmpty()){
                throw new BadRequestAlertException("Default VAS cannot be added.  Please fill price and length/count fields");
            }

            subscriptionVAS = new SubscriptionVAS();




            for(VASSetting set : service.getDefaultVasByType(ValueAddedServiceType.ONETIME_VARIABLE_CHARGE).getSettings()){
                if(set.getId() == subscriptionCreationVM.getSubscriptionVASSettingsVM().getId()
                    && subscriptionVAS.getSettingByName(set.getName()) == null){
                   SubscriptionVASSetting vs = new SubscriptionVASSetting(set.getName(),
                           subscriptionCreationVM.getSubscriptionVASSettingsVM().getValue(),
                           subscriptionCreationVM.getSubscriptionVASSettingsVM().getLength(), "");
                   subscriptionVAS.addSetting(vs);

                   cableCharge += vs.getTotal();
                }
            }

            //In creation only this vas is used...
            vas = vasPersistenceFacade.find(147);
            subscriptionVAS.setVas(vas);

//
//            subscriptionVASSetting.setName(subscriptionCreationVM.getSubscriptionVASSettingsVM().getName());
//            subscriptionVASSetting.setLength(subscriptionCreationVM.getSubscriptionVASSettingsVM().getLength());
//            subscriptionVASSetting.setDsc(subscriptionCreationVM.getSubscriptionVASSettingsVM().getDsc());
//            subscriptionVASSetting.setTotal(subscriptionCreationVM.getSubscriptionVASSettingsVM().getTotal());
//            subscriptionVASSetting.setValue(subscriptionCreationVM.getSubscriptionVASSettingsVM().getValue());
//            subscriptionVAS.addSetting(subscriptionVASSetting);
//            subscription.addVAS(subscriptionVAS);
        }



        log.info("~~~~~SUBSCRIPTION TO STRING START~~~~~");
        log.debug(subscription.toString());
        log.info("~~~~~SUBSCRIPTION TO STRING END~~~~~");
        log.info("~~~~~SUBSCRIPTION CREATION VM TO STRING START~~~~~");
        log.debug(subscriptionCreationVM.toString());
        log.info("~~~~~SUBSCRIPTION CREATION VM TO STRING END~~~~~");
        log.info("~~~~~SERVICE TO STRONG START~~~~~");
        log.debug(service.toString());
        log.info("~~~~~SERVICE TO STRING END~~~~~");

        log.info("~~~~~~~~~~~~~~~~SUBSCRIPTION-VAS-LIST-START~~~~~~~~~~~~~~~~~~~~");
        log.info(subscription.getVasList());
        log.info("~~~~~~~~~~~~~~~~SUBSCRIPTION-VAS-LIST-END~~~~~~~~~~~~~~~~~~~~");
        log.info("~~~~~~~~~~~~~~~~SERVICE-DEFAULT-VAS-LIST-START~~~~~~~~~~~~~~~");
        log.info("~~~~~~~~~~~~"+service.getDefaultVasList().toString());
        log.info("~~~~~~~~~~~~~~~~SERVICE-DEFAULT-VAS-LIST-END~~~~~~~~~~~~~~~~~");


        subscription.addVAS(subscriptionVAS);

        if(service != null && service.getDefaultVasList() != null &&
        (subscription.getVasList().size() < service.getDefaultVasList().size()) ){
            log.debug("~~~Not all charges for default VAS added: subscriptionVAS: " + subscriptionVAS.getSettings() + ", defaultVasList: " + service.getDefaultVasList());
            throw new BadRequestAlertException("Not all charges added");
        }



        log.debug("~~~check it "+subscription.getSettingByType(ServiceSettingType.USERNAME));

        subscription.setBillingModel(modelFacade.find(BillingPrinciple.CONTINUOUS));


        try{
            subscriptionPersistenceFacade.findByAgreement(subscription.getAgreement());
            SubscriptionDetails subscriptionDetails = new SubscriptionDetails();
            subscriptionDetails.setCity(subscriptionCreationVM.getSubscriptionDto().getDetails().getCity());
            subscriptionDetails.setAts(subscriptionCreationVM.getSubscriptionDto().getDetails().getAts());
            subscriptionDetails.setStreet(subscriptionCreationVM.getSubscriptionDto().getDetails().getStreet());
            subscriptionDetails.setBuilding(subscriptionCreationVM.getSubscriptionDto().getDetails().getBuilding());
            subscriptionDetails.setApartment(subscriptionCreationVM.getSubscriptionDto().getDetails().getApartment());
            subscriptionDetails.setEntrance(subscriptionCreationVM.getSubscriptionDto().getDetails().getEntrance());
            subscriptionDetails.setFloor(subscriptionCreationVM.getSubscriptionDto().getDetails().getFloor());
            subscriptionDetails.setLanguage(subscriptionCreationVM.getSubscriptionDto().getDetails().getLang());
            subscriptionDetails.setDesc(subscriptionCreationVM.getSubscriptionDto().getDetails().getDesc());
            subscriptionDetails.setName(subscriptionCreationVM.getSubscriptionDto().getDetails().getName());
            subscriptionDetails.setSurname(subscriptionCreationVM.getSubscriptionDto().getDetails().getSurname());
            subscriptionDetails.setPassword(subscriptionCreationVM.getSubscriptionDto().getDetails().getPassword());
            subscriptionDetails.setComments(subscriptionCreationVM.getSubscriptionDto().getDetails().getComments());

            subscription.setDetails(subscriptionDetails);

            subscription.setTaxFreeEnabled(subscriptionCreationVM.getSubscriptionDto().isTaxFreeEnabled());

            log.debug("~~~Selected minipop: " + miniPop);

            log.debug("~~~this is 0 "+subscription.getSettingByType(ServiceSettingType.USERNAME));

            log.debug("~~~this is 1 " + service.getProvider());


            //First null stands for notification settings...
            //Second null stands for campaign...
            //Third null stands for session...
            createSubscription(selectedSubscriber,
                    subscription,
                    String.valueOf(subscriptionCreationVM.getServiceId()),
                    miniPop,
                    subscriptionCreationVM.getSubscriptionDto().isUseEquipmentFromStock(),
                    equipment,
                    null,
                    subscriptionCreationVM.getSubscriptionDto().getInstallationFee(),
                    null,
                    null,
                    user
                    );

            log.debug("~~~this is 2");
        }catch (DuplicateAgreementException ex) {
            log.error("Cannot create subscriptipn: ", ex);
        } catch (NoFreePortLeftException ex) {
            log.error("Cannot create subscription: ", ex);
        } catch (PortAlreadyReservedException ex) {
            log.error("Cannot create subscription: ", ex);
        } catch (Exception ex) {
            log.error("Cannot create subscriptipn: ", ex);
        }



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

        log.info("~~~~~" + subscription.toString() + "~~~~~");

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
                                   User user,String ... additionalProperties) throws NoFreePortLeftException, PortAlreadyReservedException {
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
    }

    @Override
    public Subscription createSubscriptionSpring(Subscriber selectedSubscriber, Subscription subscription, Long serviceId, MiniPop miniPop,
                                                 boolean isUseStockEquipment, Equipment equipment, List<NotificationSettingRow> notificationSettings,
                                                 double installationFee, Campaign selectedCampaign, User user, String... additionalProperties)
            throws NoFreePortLeftException, PortAlreadyReservedException {
        narHomeCommonEngine.createSubscriptionSpring(selectedSubscriber,
                subscription,
                serviceId,
                miniPop,
                isUseStockEquipment,
                equipment,
                notificationSettings,
                installationFee,
                selectedCampaign,
                user,
                additionalProperties);

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
