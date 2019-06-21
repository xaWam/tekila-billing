package reseller;

import com.jaravir.tekila.base.model.BillingModelPersistenceFacade;
import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.engines.OperationsEngine;
import com.jaravir.tekila.module.accounting.manager.TaxCategoryPeristenceFacade;
import com.jaravir.tekila.module.campaign.Campaign;
import com.jaravir.tekila.module.campaign.CampaignPersistenceFacade;
import com.jaravir.tekila.module.service.ServiceSettingType;
import com.jaravir.tekila.module.service.entity.ProfileType;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.service.entity.SubscriptionServiceType;
import com.jaravir.tekila.module.service.model.BillingModel;
import com.jaravir.tekila.module.service.model.BillingPrinciple;
import com.jaravir.tekila.module.service.persistence.manager.ServicePersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.ServicePropertyPersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.SubscriptionServiceTypePersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.*;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriberPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import com.jaravir.tekila.provision.broadband.devices.manager.MiniPopPersistenceFacade;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import java.text.SimpleDateFormat;
import java.util.Date;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Stateless
@Path("subscription")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Transactional
public class SubscriptionEndpoint {
    private final static Logger log = Logger.getLogger(SubscriptionEndpoint.class);

    @EJB
    private EngineFactory engineFactory;
    @EJB
    private ServicePersistenceFacade servicePersistenceFacade;
    @EJB
    private TaxCategoryPeristenceFacade taxCategoryPeristenceFacade;
    @EJB
    private SubscriberPersistenceFacade subscriberPersistenceFacade;
    @EJB
    private SubscriptionServiceTypePersistenceFacade serviceTypePersistenceFacade;
    @EJB
    private MiniPopPersistenceFacade minipopFacade;
    @EJB
    private ServicePropertyPersistenceFacade servicePropertyPersistenceFacade;
    @EJB
    private BillingModelPersistenceFacade modelFacade;
    @EJB
    private CampaignPersistenceFacade campaignPersistenceFacade;
    @EJB
    private ResellerFetcher resellerFetcher;
    @EJB
    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;

    @POST
    @JWTTokenNeeded
    @Path("/create")
    public Response createSubscription(
            SubscriptionCreationParams creationParams,
            @Context HttpServletRequest request) {
        try {
            Reseller reseller = resellerFetcher.getReseller(request);

            request.login(reseller.getUsername(), reseller.getPassword());
            request.getSession().setAttribute("userID", reseller.getUser().getId());

            Subscriber subscriber = createSubscriber(creationParams);
            createSubscription(
                    creationParams,
                    subscriber,
                    request,
                    reseller);

            return Response.ok().entity("Subscription has been created successfully").build();
        } catch (Exception ex) {
            return Response.serverError().entity(ex.getMessage()).build();
        } finally {
            try {
                request.logout();
                request.getSession().removeAttribute("userID");
                request.getSession().invalidate();
            } catch (ServletException exc) {
                log.error("exception during request logout", exc);
            }
        }
    }




    @POST
    @JWTTokenNeeded
    @Path("/CNCcreate")
    public Response createCNCSubscription(
            SubscriptionCreationParams creationParams,
            @Context HttpServletRequest request) {
        try {
            Reseller reseller = resellerFetcher.getReseller(request);

            request.login(reseller.getUsername(), reseller.getPassword());
            request.getSession().setAttribute("userID", reseller.getUser().getId());

            Subscriber subscriber = createSubscriber(creationParams);
            createCNCSubscription(
                    creationParams,
                    subscriber,
                    request,
                    reseller);

            return Response.ok().entity("Subscription has been created successfully").build();
        } catch (Exception ex) {
            log.error("rest request exception  createCNCSubscription  line 131"+ex);
            return Response.serverError().entity(ex.getMessage()).build();

        } finally {
            try {
                request.logout();
                request.getSession().removeAttribute("userID");
                request.getSession().invalidate();
            } catch (ServletException exc) {
                log.error("exception during request logout", exc);
            }
        }
    }



    private Subscription createSubscription (
            SubscriptionCreationParams creationParams,
            Subscriber subscriber,
            HttpServletRequest request,
            Reseller reseller) throws Exception  {
        if (creationParams.zoneId == null || creationParams.serviceTypeId == null) {
            log.debug("Cannot create subscription. Zone and service type must be specified.");
            throw new RuntimeException("Cannot create subscription. Zone and service type must be specified.");
        }
        if (creationParams.serviceId == null) {
            log.debug("Cannot create subscription. Service id must be specified.");
            throw new RuntimeException("Cannot create subscription. Service id must be specified.");
        }
        if (servicePropertyPersistenceFacade.find(
                String.valueOf(creationParams.serviceId),
                String.valueOf(creationParams.zoneId)) == null) {
            log.debug("Cannot create subscription. The combination of service and zone is not valid");
            throw new RuntimeException("Cannot create subscription. The combination of service and zone is not valid");
        }
        if (subscriptionPersistenceFacade.findByAgreementOrdinary(creationParams.agreement) != null) {
            log.debug("Cannot create subscription. Agreement already exists");
            throw new RuntimeException("Cannot create subscription. Agreement already exists");
        }

        Subscription subscription = new Subscription();
        subscription.setPaymentType(PaymentTypes.values()[creationParams.paymentId]);

        BillingModel billingModel = modelFacade.find(BillingPrinciple.CONTINUOUS);
        log.debug("Billing model: " + billingModel);
        subscription.setBillingModel(billingModel);

        SubscriptionServiceType serviceType =
                serviceTypePersistenceFacade.find(creationParams.serviceTypeId);

        Service service = servicePersistenceFacade.find(creationParams.serviceId);
        MiniPop selectedMiniPop = null;

        if (serviceType.getProfile().getProfileType().equals(ProfileType.IPoE)) { //IPoE
            selectedMiniPop = minipopFacade.find(creationParams.regionId);

            Integer portId = Integer.parseInt(creationParams.username.substring(
                    creationParams.username.lastIndexOf('/')+1,
                    creationParams.username.lastIndexOf(':')
                    ));
            selectedMiniPop.setNextAvailablePortHintAsNumber(portId);

            if (service != null && service.getSettingByType(ServiceSettingType.BROADBAND_SWITCH) != null
                    && selectedMiniPop != null && selectedMiniPop.getPreferredPort() != null
                    && !selectedMiniPop.checkPort(selectedMiniPop.getPreferredPort())) {
                log.debug("Cannot create subscription. Minipop's preferred port is invalid. " + selectedMiniPop);
                throw new RuntimeException("Cannot create subscription. Minipop's preferred port is invalid.");
            }
        } else { //PPPoE
            if (creationParams.regionId != null) {
                log.debug("Cannot create subscription. Minipop/port not needed for PPPoE");
                throw new RuntimeException("Cannot create subscription. Minipop/port not needed for PPPoE");
            }
        }

        Campaign selectedCampaign = null;
        if (creationParams.modemCampaign) {
            for (final Campaign campaign : campaignPersistenceFacade.findAllActive(service, false)) {
                if (campaign.isAvailableOnCreation()) {
                    selectedCampaign = campaign;
                    break;
                }
            }
        } else {
            for (final Campaign campaign : campaignPersistenceFacade.findAllActive(service, true)) {
                if (!campaign.isActivateOnPayment()) {
                    selectedCampaign = campaign;
                    break;
                }
            }
        }
        subscription.setAgreement(creationParams.agreement);

        SubscriptionDetails details = new SubscriptionDetails();
        details.setName(creationParams.fullName);
        details.setApartment(creationParams.address);

        subscription.setDetails(details);

        OperationsEngine operationsEngine = engineFactory.getOperationsEngine(service.getProvider());
        operationsEngine.createSubscription(
                subscriber,
                subscription,
                String.valueOf(creationParams.serviceId),
                selectedMiniPop,
                false,
                null,
                null,
                0D,
                selectedCampaign, //campaign
                request.getSession(),
                reseller.getUser(),
                String.valueOf(creationParams.zoneId),
                String.valueOf(creationParams.serviceTypeId),
                String.valueOf(reseller.getId()),
                String.valueOf(creationParams.username)
        );
        return subscription;
    }





    private Subscription createCNCSubscription (
            SubscriptionCreationParams creationParams,
            Subscriber subscriber,
            HttpServletRequest request,
            Reseller reseller) throws Exception  {
        if (creationParams.zoneId == null || creationParams.serviceTypeId == null) {
            log.debug("Cannot create subscription. Zone and service type must be specified.");
            throw new RuntimeException("Cannot create subscription. Zone and service type must be specified.");
        }
        if (creationParams.serviceId == null) {
            log.debug("Cannot create subscription. Service id must be specified.");
            throw new RuntimeException("Cannot create subscription. Service id must be specified.");
        }
        if (subscriptionPersistenceFacade.findByAgreementOrdinary(creationParams.agreement) != null) {
            log.debug("Cannot create subscription. Agreement already exists");
            throw new RuntimeException("Cannot create subscription. Agreement already exists");
        }

        Subscription subscription = new Subscription();
        subscription.setPaymentType(PaymentTypes.values()[creationParams.paymentId]);

        BillingModel billingModel = modelFacade.find(BillingPrinciple.CONTINUOUS);
        log.debug("Billing model: " + billingModel);
        subscription.setBillingModel(billingModel);

        SubscriptionServiceType serviceType =
                serviceTypePersistenceFacade.find(creationParams.serviceTypeId);
        log.debug("serviceType:   "+subscription.getAgreement()+" = "+serviceType.toString());
        Service service = servicePersistenceFacade.find(creationParams.serviceId);
        log.debug("service 282: "+service);
        MiniPop selectedMiniPop = null;

//            if (creationParams.regionId != null) {
//                log.debug("Cannot create subscription. Minipop/port not needed for PPPoE");
//                throw new RuntimeException("Cannot create subscription. Minipop/port not needed for PPPoE");
//            }

        Campaign selectedCampaign = null;
        if (creationParams.modemCampaign) {
            for (final Campaign campaign : campaignPersistenceFacade.findAllActive(service, false)) {
                if (campaign.isAvailableOnCreation()) {
                    selectedCampaign = campaign;
                    break;
                }
            }
        } else {
            for (final Campaign campaign : campaignPersistenceFacade.findAllActive(service, true)) {
                if (!campaign.isActivateOnPayment()) {
                    selectedCampaign = campaign;
                    break;
                }
            }
        }
        subscription.setAgreement(creationParams.agreement);

        SubscriptionDetails details = new SubscriptionDetails();
        details.setName(creationParams.fullName);
        details.setApartment(creationParams.address);

        subscription.setDetails(details);

        OperationsEngine operationsEngine = engineFactory.getOperationsEngine(service.getProvider());
        operationsEngine.createSubscription(
                subscriber,
                subscription,
                String.valueOf(creationParams.serviceId),
                selectedMiniPop,
                false,
                null,
                null,
                0D,
                selectedCampaign, //campaign
                request.getSession(),
                reseller.getUser(),
                String.valueOf(creationParams.serviceTypeId),
                String.valueOf(reseller.getId()),
                String.valueOf(creationParams.username)
        );
        return subscription;
    }





    private Subscriber createSubscriber(SubscriptionCreationParams creationParams) {
        Subscriber subscriber = new Subscriber();
        subscriber.setMasterAccount(Long.valueOf(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())));
        subscriber.setBilledByLifeCycle(true);
        subscriber.setLifeCycle(SubscriberLifeCycleType.PREPAID);
        subscriber.setFnCategory(SubscriberFunctionalCategory.COMMERCIAL);
        subscriber.setTaxCategory(taxCategoryPeristenceFacade.findDefault());

        SubscriberDetails details = new SubscriberDetails();
        details.setFirstName(creationParams.fullName);
        details.setApartment(creationParams.address);
        details.setPhoneMobile(creationParams.phoneMobile);
        details.setType(SubscriberType.INDV);
        details.setEntryDate(DateTime.now().toDate());

        details.setSubscriber(subscriber);
        subscriber.setDetails(details);
        subscriberPersistenceFacade.save(subscriber);
        return subscriber;
    }
}