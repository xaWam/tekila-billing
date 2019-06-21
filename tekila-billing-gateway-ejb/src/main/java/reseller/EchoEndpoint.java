package reseller;

import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.engines.ProvisioningEngine;
import com.jaravir.tekila.module.accounting.entity.Payment;
import com.jaravir.tekila.module.service.ResourceBucketType;
import com.jaravir.tekila.module.service.ServiceSettingType;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.service.entity.ServiceProperty;
import com.jaravir.tekila.module.service.entity.ServiceProvider;
import com.jaravir.tekila.module.service.entity.Zone;
import com.jaravir.tekila.module.service.persistence.manager.ServicePersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.ServicePropertyPersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.ServiceProviderPersistenceFacade;
import com.jaravir.tekila.module.stats.persistence.entity.OnlineBroadbandStats;
import com.jaravir.tekila.module.subscription.exception.DuplicateAgreementException;
import com.jaravir.tekila.module.subscription.persistence.entity.Reseller;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.management.ResellerPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import com.jaravir.tekila.provision.broadband.devices.manager.MiniPopPersistenceFacade;
import com.jaravir.tekila.provision.broadband.devices.manager.Providers;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import com.jaravir.tekila.provision.exception.ProvisioningException;
import io.jsonwebtoken.Jwts;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import reseller.vm.SubscriptionNewService;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.ws.spi.Provider;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

@Stateless
@Path("/echo")
public class EchoEndpoint {
    @EJB
    private ResellerPersistenceFacade resellerFacade;

    @EJB
    private EngineFactory provisioningFactory;

    private final static Logger log = Logger.getLogger(EchoEndpoint.class);
    @EJB
    private SubscriptionPersistenceFacade subscriptionFacade;

    @EJB
    private ServicePropertyPersistenceFacade servicePropertyPersistenceFacade;

    @EJB
    private MiniPopPersistenceFacade minipopFacade;

    @EJB
    private ServiceProviderPersistenceFacade serviceProviderPersistenceFacade;


    @EJB
    private ServicePersistenceFacade servicePersistenceFacade;

    private Reseller getReseller(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Extract the token from the HTTP Authorization header
        String token = authorizationHeader.substring("Bearer".length()).trim();
        log.debug(String.format("createSubscription.token = %s", token));

        String resellerId =
                Jwts.parser().setSigningKey("secretkey1").parseClaimsJws(token).getBody().getSubject();
        log.debug("#### valid token : " + token);
        log.debug(String.format("resellerId = %s", resellerId));
        Reseller reseller = resellerFacade.find(Long.parseLong(resellerId));
        log.debug(String.format("reseller name = %s, user name = %s", reseller.getName(), reseller.getUser().getUserName()));
        return reseller;
    }


    @GET
    @JWTTokenNeeded
    @Path("listusers2")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ArrayList<ListOfAllUsers> list_users2(@Context HttpServletRequest request, @QueryParam("pageSize") int pageSize,
                                                 @QueryParam("page") int page) {
        Reseller reseller = getReseller(request);

        int totalCount = 0;
        int startLimit = pageSize * (page - 1) + 1;
        int endLimit = startLimit + pageSize;

        ArrayList<ListOfAllUsers> listOfAllUserss = new ArrayList<>();


        System.out.println("Group " + reseller.getUser().getGroup().getGroupName());
        List<Subscription> subscriptions;
        if (reseller.getUser().getGroup().getGroupName().equals("DataPlus CallCenter") || reseller.getUser().getGroup().getGroupName().equals("DataPlus Admin")) {
            totalCount = subscriptionFacade.findAllDataPlusSubscriptionsC().intValueExact();
            subscriptions = subscriptionFacade.findAllDataPlusSubscriptions2(startLimit, endLimit);
        } else if (reseller.getUser().getGroup().getGroupName().equals("Global CallCenter") || reseller.getUser().getGroup().getGroupName().equals("Global Admin")) {
            totalCount = subscriptionFacade.findAllGlobalSubscriptionsC().intValueExact();
            subscriptions = subscriptionFacade.findAllGlobalSubscriptions2(startLimit, endLimit);
        } else if (reseller.getUser().getGroup().getGroupName().equals("CNC CallCenter") || reseller.getUser().getGroup().getGroupName().equals("CNC Admin")) {
            totalCount = subscriptionFacade.findAllCNCSubscriptionsC().intValueExact();
            subscriptions = subscriptionFacade.findAllCNCSubscriptions2(startLimit, endLimit);
        } else {
            totalCount = subscriptionFacade.findSubscriptionsByDealerC(String.valueOf(reseller.getId())).intValueExact();
            subscriptions = subscriptionFacade.findSubscriptionsByDealer2(String.valueOf(reseller.getId()), startLimit, endLimit);
        }
        log.debug("size of list " + subscriptions.size());
        ArrayList<UserSubscriber> listDeatils = new ArrayList<>();
        for (Subscription subscription : subscriptions
                ) {
            listDeatils.add(new UserSubscriber(subscription));

        }
        listOfAllUserss.add(new ListOfAllUsers(listDeatils, totalCount));
        return listOfAllUserss;

    }


    @GET
    @JWTTokenNeeded
    @Path("listusers")
    @Produces(MediaType.APPLICATION_JSON)
    public ArrayList<UserSubscriber> list_users(@Context HttpServletRequest request) {
        List<Object> finalData = new ArrayList<>();
        Reseller reseller = getReseller(request);

        System.out.println("Group " + reseller.getUser().getGroup().getGroupName());
        List<Subscription> subscriptions;
        if (reseller.getUser().getGroup().getGroupName().equals("DataPlus CallCenter") || reseller.getUser().getGroup().getGroupName().equals("DataPlus Admin")) {
            subscriptions = subscriptionFacade.findAllDataPlusSubscriptions();
        } else if (reseller.getUser().getGroup().getGroupName().equals("Global CallCenter") || reseller.getUser().getGroup().getGroupName().equals("Global Admin")) {
            subscriptions = subscriptionFacade.findAllGlobalSubscriptions();
        } else if (reseller.getUser().getGroup().getGroupName().equals("CNC CallCenter") || reseller.getUser().getGroup().getGroupName().equals("CNC Admin")) {
            subscriptions = subscriptionFacade.findAllCNCSubscriptions();
        } else {
            subscriptions = subscriptionFacade.findSubscriptionsByDealer(String.valueOf(reseller.getId()));
        }

        log.debug("size of list " + subscriptions.size());
        ArrayList<UserSubscriber> listDeatils = new ArrayList<>();
        for (Subscription subscription : subscriptions
                ) {
            listDeatils.add(new UserSubscriber(subscription));

        }

//        finalData.add(listDeatils);
//        finalData.add(subscriptions.size());
        return listDeatils;
    }


    @GET
    @JWTTokenNeeded
    @Path("getSubscription")
    @Produces(MediaType.APPLICATION_JSON)
    public SubscriptionInfo getSubscription(@Context HttpServletRequest request, @QueryParam("agreement") String agreement) {
        Reseller reseller = getReseller(request);
        String currentIP = "";
        String state = "OFFLINE";
        String region = " ";
        System.out.println("Group " + reseller.getUser().getGroup().getGroupName());
        Subscription subscription = null;
        log.debug("agreement is: " + agreement);
        try {
            if (reseller.getUser().getGroup().getGroupName().equals("DataPlus CallCenter") || reseller.getUser().getGroup().getGroupName().equals("DataPlus Admin")) {
                subscription = subscriptionFacade.findDPSubscriptionCC(agreement);
            }else    if (reseller.getUser().getGroup().getGroupName().equals("Global CallCenter") || reseller.getUser().getGroup().getGroupName().equals("Global Admin")) {
                subscription = subscriptionFacade.findGlobalSubscriptionCC(agreement);
            }else  if (reseller.getUser().getGroup().getGroupName().equals("CNC CallCenter") || reseller.getUser().getGroup().getGroupName().equals("CNC Admin")) {
                        subscription = subscriptionFacade.findCNCSubscriptionCC(agreement);
            } else {
                subscription = subscriptionFacade.findSubscription(agreement, String.valueOf(reseller.getId()));
            }
        } catch (NoResultException nre) {

            log.debug("No data found 183 " + nre);
        } catch (Exception ex) {
            log.debug("general exception " + ex);
            return null;
        }

        // radius
//        try {
//            ProvisioningEngine provisioner = provisioningFactory.getProvisioningEngine(subscription);
//            currentIP = provisioner.checkRadiusState(subscription);
//
//        } catch (ProvisionerNotFoundException e) {
//            log.error("ProvisionerNotFoundException 194" + e);
//        } catch (ProvisioningException e) {
//            log.error("ProvisioningException 196" + e);
//        }
        if (!currentIP.isEmpty()) {
            state = "ONLINE";
        }
        if (subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH) != null) {
            try {
                region = minipopFacade.find(Long.parseLong(subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH).getValue())).getSwitch_id();
            } catch (NullPointerException n) {

                log.debug("switch didnt found");
            }
        }
        log.debug("size of list " + subscription);
        return new SubscriptionInfo(subscription, currentIP, state, region);
    }


    @GET
    @JWTTokenNeeded
    @Path("onlineusers")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<OnlineUser> online_users(@Context HttpServletRequest request, @QueryParam("agreement") String agreement) {
        Reseller reseller = getReseller(request);
        int totalCount = 0;
        ArrayList<OnlineUser> listOfOnlineUser = new ArrayList<>();
        Subscription subscription = null;
        if (!reseller.getUser().getGroup().getGroupName().contains("CallCenter") && !reseller.getUser().getGroup().getGroupName().contains("Admin")) {

            subscription = subscriptionFacade.findSubscriptionByDealer(String.valueOf(reseller.getId()), agreement);
        }
        OnlineSessionObject onlineSessionObject = null;

        log.debug("agreement << " + subscription.getAgreement());
        OnlineBroadbandStats onlineBroadbandStats = null;
        try {
            ProvisioningEngine provisioner = provisioningFactory.getProvisioningEngine(subscription);
            try {
                onlineBroadbandStats = provisioner.collectOnlineStats(subscription);

                log.debug("onlineBroadbandStats:  " + onlineBroadbandStats);
                if (onlineBroadbandStats != null) {
                    onlineSessionObject = new OnlineSessionObject(subscription, onlineBroadbandStats);
                }
                //                    onlineSessionObjects.add(onlineSessionObject);

            } catch (NullPointerException poi) {
                log.debug("Null Pointer exception " + poi);
//                onlineSessionObject = new OnlineSessionObject(subscription, new OnlineBroadbandStats());
//                    onlineSessionObjects.add(onlineSessionObject);

            }
        } catch (ProvisionerNotFoundException e) {
            log.error(e);
        }


        listOfOnlineUser.add(new OnlineUser(onlineSessionObject, totalCount));
//        log.debug("size of list " + onlineSessionObjects.size());
//        log.debug("total size " + totalCount);

        return listOfOnlineUser;
    }


    @GET
    @JWTTokenNeeded
    @Path("onlineusers2")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ArrayList<ListOfOnlineUsers> online_users2(@Context HttpServletRequest request, @QueryParam("pageSize") int pageSize,
                                                      @QueryParam("page") int page) {
        log.debug("online_users2 is started");
        Reseller reseller = getReseller(request);
        int counter = 0;
        int startLimit = pageSize * (page - 1) + 1;
        int endLimit = startLimit + pageSize - 1;

        ArrayList<ListOfOnlineUsers> listOfOnlineUserss = new ArrayList<>();
        int totalCount = 0;
        ArrayList<OnlineSessionObject> onlineSessionObjects = new ArrayList<>();
        ArrayList<OnlineSessionObject> onlineSessionObjects2 = new ArrayList<>();
        List<OnlineBroadbandStats> onlineBroadbandStatses = null;
        OnlineBroadbandStats onlineBroadbandStats = null;
        log.debug("start");

        if (!reseller.getUser().getGroup().getGroupName().equals("DataPlus CallCenter") && !reseller.getUser().getGroup().getGroupName().equals("DataPlus Admin")) {


            List<Subscription> subscriptions = subscriptionFacade.findSubscriptionsByDealer3(String.valueOf(reseller.getId()));


            for (Subscription subscription : subscriptions
                    ) {

                try {
                    ProvisioningEngine provisioner = provisioningFactory.getProvisioningEngine(subscription);
                    try {
                        onlineBroadbandStats = provisioner.collectOnlineStats(subscription);

                        log.debug("onlineBroadbandStats:  " + onlineBroadbandStats);

                        OnlineSessionObject onlineSessionObject = new OnlineSessionObject(subscription, onlineBroadbandStats);
                        onlineSessionObjects.add(onlineSessionObject);

                    } catch (NullPointerException poi) {
                        log.debug("Null Pointer exception " + poi);
                        continue;
                    }
                } catch (ProvisionerNotFoundException e) {
                    log.error(e);
                }

            }

            log.debug(onlineSessionObjects.toString());
            log.debug("startLimit:" + startLimit);
            log.debug("endLimit:" + endLimit);
            log.debug("size of list " + onlineSessionObjects.size());
            log.debug("total size " + totalCount);
            for (int w = 0; w <= onlineSessionObjects.size(); w++) {
                if (w >= startLimit & w <= endLimit) {
                    onlineSessionObjects2.add(onlineSessionObjects.get(w));
                }

            }

            totalCount = onlineSessionObjects.size();
        } else {
            log.debug("Provider name " + Providers.DATAPLUS.toString());
            ServiceProvider serviceProvider = serviceProviderPersistenceFacade.find(Providers.DATAPLUS.getId());
            log.debug("Service Provider is :" + serviceProvider);

            try {

                ProvisioningEngine provisioner = provisioningFactory.getProvisioningEngine(serviceProvider);
                onlineBroadbandStatses = provisioner.collectOnlineStatswithPage(startLimit, endLimit);

                log.debug("onlineBroadbandStatses.size" + onlineBroadbandStatses.size());
                for (OnlineBroadbandStats onlineBroadbandStats1 : onlineBroadbandStatses
                        ) {
                    log.debug("onlineBroadbandStats1.getUser()" + onlineBroadbandStats1.getUser());
                    Subscription subscription = subscriptionFacade.findSubscriptionsByUsername(onlineBroadbandStats1.getUser());


                    OnlineSessionObject onlineSessionObject = new OnlineSessionObject(subscription, onlineBroadbandStats1);
                    onlineSessionObjects2.add(onlineSessionObject);

                }

                totalCount = onlineSessionObjects2.size();

                log.debug("call center totalCount " + totalCount);
                log.debug("onlineSessionObjects2:" + onlineSessionObjects2.toString());
            } catch (ProvisionerNotFoundException e) {
                e.printStackTrace();
            }

        }

        listOfOnlineUserss.add(new ListOfOnlineUsers(onlineSessionObjects2, totalCount));
        log.debug("online_users2 is finsihed");

//        return new ListOfOnlineUsers(onlineSessionObjects, totalCount);
        return listOfOnlineUserss;
    }





    @GET
    @JWTTokenNeeded
    @Path("onlineusersGlobal")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ArrayList<ListOfOnlineUsers> online_users_global(@Context HttpServletRequest request, @QueryParam("pageSize") int pageSize,
                                                      @QueryParam("page") int page) {
        Reseller reseller = getReseller(request);
        int counter = 0;
        int startLimit = pageSize * (page - 1) + 1;
        int endLimit = startLimit + pageSize - 1;

        ArrayList<ListOfOnlineUsers> listOfOnlineUserss = new ArrayList<>();
        int totalCount = 0;
        ArrayList<OnlineSessionObject> onlineSessionObjects = new ArrayList<>();
        ArrayList<OnlineSessionObject> onlineSessionObjects2 = new ArrayList<>();
        List<OnlineBroadbandStats> onlineBroadbandStatses = null;
        OnlineBroadbandStats onlineBroadbandStats = null;
        log.debug("start");

        if (!reseller.getUser().getGroup().getGroupName().equals("Global CallCenter")
                && !reseller.getUser().getGroup().getGroupName().equals("Global Admin")) {


            List<Subscription> subscriptions = subscriptionFacade.findSubscriptionsByDealer3(String.valueOf(reseller.getId()));


            for (Subscription subscription : subscriptions
                    ) {

                try {
                    ProvisioningEngine provisioner = provisioningFactory.getProvisioningEngine(subscription);
                    try {
                        onlineBroadbandStats = provisioner.collectOnlineStats(subscription);

                        log.debug("onlineBroadbandStats:  " + onlineBroadbandStats);

                        OnlineSessionObject onlineSessionObject = new OnlineSessionObject(subscription, onlineBroadbandStats);
                        onlineSessionObjects.add(onlineSessionObject);

                    } catch (NullPointerException poi) {
                        log.debug("Null Pointer exception " + poi);
                        continue;
                    }
                } catch (ProvisionerNotFoundException e) {
                    log.error(e);
                }

            }

            log.debug(onlineSessionObjects.toString());
            log.debug("startLimit:" + startLimit);
            log.debug("endLimit:" + endLimit);
            log.debug("size of list " + onlineSessionObjects.size());
            log.debug("total size " + totalCount);
            for (int w = 0; w <= onlineSessionObjects.size(); w++) {
                if (w >= startLimit & w <= endLimit) {
                    onlineSessionObjects2.add(onlineSessionObjects.get(w));
                }

            }

            totalCount = onlineSessionObjects.size();
        } else {
            log.debug("Provider name " + Providers.GLOBAL.toString());
            ServiceProvider serviceProvider = serviceProviderPersistenceFacade.find(Providers.GLOBAL.getId());
            log.debug("Service Provider is :" + serviceProvider);

            try {

                ProvisioningEngine provisioner = provisioningFactory.getProvisioningEngine(serviceProvider);
                onlineBroadbandStatses = provisioner.collectOnlineStatswithPage(startLimit, endLimit);

                log.debug("onlineBroadbandStatses.size" + onlineBroadbandStatses.size());
                for (OnlineBroadbandStats onlineBroadbandStats1 : onlineBroadbandStatses
                        ) {
                    log.debug("onlineBroadbandStats1.getUser()" + onlineBroadbandStats1.getUser());
                    Subscription subscription = subscriptionFacade.findSubscriptionsByUsername(onlineBroadbandStats1.getUser());


                    OnlineSessionObject onlineSessionObject = new OnlineSessionObject(subscription, onlineBroadbandStats1);
                    onlineSessionObjects2.add(onlineSessionObject);

                }

                totalCount = onlineSessionObjects2.size();

                log.debug("call center totalCount " + totalCount);
                log.debug("onlineSessionObjects2:" + onlineSessionObjects2.toString());
            } catch (ProvisionerNotFoundException e) {
                e.printStackTrace();
            }

        }

        listOfOnlineUserss.add(new ListOfOnlineUsers(onlineSessionObjects2, totalCount));


//        return new ListOfOnlineUsers(onlineSessionObjects, totalCount);
        return listOfOnlineUserss;
    }



    @GET
    @JWTTokenNeeded
    @Path("onlineusersCNC")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ArrayList<ListOfOnlineUsers> online_users_cnc(@Context HttpServletRequest request, @QueryParam("pageSize") int pageSize,
                                                            @QueryParam("page") int page) {
        Reseller reseller = getReseller(request);
        int counter = 0;
        int startLimit = pageSize * (page - 1) + 1;
        int endLimit = startLimit + pageSize - 1;

        ArrayList<ListOfOnlineUsers> listOfOnlineUserss = new ArrayList<>();
        int totalCount = 0;
        ArrayList<OnlineSessionObject> onlineSessionObjects = new ArrayList<>();
        ArrayList<OnlineSessionObject> onlineSessionObjects2 = new ArrayList<>();
        List<OnlineBroadbandStats> onlineBroadbandStatses = null;
        OnlineBroadbandStats onlineBroadbandStats = null;
        log.debug("start");

        if (!reseller.getUser().getGroup().getGroupName().equals("CNC CallCenter")
                && !reseller.getUser().getGroup().getGroupName().equals("CNC Admin")) {


            List<Subscription> subscriptions = subscriptionFacade.findSubscriptionsByDealer3(String.valueOf(reseller.getId()));


            for (Subscription subscription : subscriptions
                    ) {

                try {
                    ProvisioningEngine provisioner = provisioningFactory.getProvisioningEngine(subscription);
                    try {
                        onlineBroadbandStats = provisioner.collectOnlineStats(subscription);

                        log.debug("onlineBroadbandStats:  " + onlineBroadbandStats);

                        OnlineSessionObject onlineSessionObject = new OnlineSessionObject(subscription, onlineBroadbandStats);
                        onlineSessionObjects.add(onlineSessionObject);

                    } catch (NullPointerException poi) {
                        log.debug("Null Pointer exception " + poi);
                        continue;
                    }
                } catch (ProvisionerNotFoundException e) {
                    log.error(e);
                }

            }

            log.debug(onlineSessionObjects.toString());
            log.debug("startLimit:" + startLimit);
            log.debug("endLimit:" + endLimit);
            log.debug("size of list " + onlineSessionObjects.size());
            log.debug("total size " + totalCount);
            for (int w = 0; w <= onlineSessionObjects.size(); w++) {
                if (w >= startLimit & w <= endLimit) {
                    onlineSessionObjects2.add(onlineSessionObjects.get(w));
                }

            }

            totalCount = onlineSessionObjects.size();
        } else {
            log.debug("Provider name " + Providers.CNC.toString());
            ServiceProvider serviceProvider = serviceProviderPersistenceFacade.find(Providers.CNC.getId());
            log.debug("Service Provider is :" + serviceProvider);

            try {

                ProvisioningEngine provisioner = provisioningFactory.getProvisioningEngine(serviceProvider);
                onlineBroadbandStatses = provisioner.collectOnlineStatswithPage(startLimit, endLimit);

                log.debug("onlineBroadbandStatses.size" + onlineBroadbandStatses.size());
                for (OnlineBroadbandStats onlineBroadbandStats1 : onlineBroadbandStatses
                        ) {
                    log.debug("onlineBroadbandStats1.getUser()" + onlineBroadbandStats1.getUser());
                    Subscription subscription = subscriptionFacade.findSubscriptionsByUsername(onlineBroadbandStats1.getUser());


                    OnlineSessionObject onlineSessionObject = new OnlineSessionObject(subscription, onlineBroadbandStats1);
                    onlineSessionObjects2.add(onlineSessionObject);

                }

                totalCount = onlineSessionObjects2.size();

                log.debug("call center totalCount " + totalCount);
                log.debug("onlineSessionObjects2:" + onlineSessionObjects2.toString());
            } catch (ProvisionerNotFoundException e) {
                e.printStackTrace();
            }

        }

        listOfOnlineUserss.add(new ListOfOnlineUsers(onlineSessionObjects2, totalCount));


//        return new ListOfOnlineUsers(onlineSessionObjects, totalCount);
        return listOfOnlineUserss;
    }





    @GET
    @JWTTokenNeeded
    @Path("listpayments")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PaymentList> list_payments(@Context HttpServletRequest request, @QueryParam("agreement") String agreement) {
        Reseller reseller = getReseller(request);
        log.debug("agreement " + agreement);
        List<Payment> payments = subscriptionFacade.findPaymentsList(agreement);


        log.debug("size of list " + payments.size());
        ArrayList<PaymentList> listDeatils = new ArrayList<>();
        for (Payment payment : payments
                ) {
            try {
                subscriptionFacade.findByAgreement(payment.getContract());
            } catch (DuplicateAgreementException e) {
                e.printStackTrace();
            }
            listDeatils.add(new PaymentList(payment));

        }

        return listDeatils;
    }


    @GET
    @JWTTokenNeeded
    @Path("listpayments2")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PaymentListwithSize> list_payments2(@Context HttpServletRequest request, @QueryParam("agreement") String agreement,
                                                    @QueryParam("pageSize") int pageSize,
                                                    @QueryParam("page") int page) {
        Reseller reseller = getReseller(request);
        log.debug("agreement " + agreement);
        ArrayList<PaymentListwithSize> paymentListwithSizes = new ArrayList<>();
        int totalSize = 0;
        int startLimit = pageSize * (page - 1) + 1;
        int endLimit = startLimit + pageSize - 1;
        log.debug("startLimit:" + startLimit);
        log.debug("endLimit:" + endLimit);
        totalSize = subscriptionFacade.findPaymentsListC(agreement).byteValueExact();
        List<Payment> payments = subscriptionFacade.findPaymentsListR(agreement, startLimit, endLimit);


        log.debug("size of list " + payments.size());
        ArrayList<PaymentList> listDeatils = new ArrayList<>();
        for (Payment payment : payments
                ) {
//            try {
//                subscriptionFacade.findByAgreement(payment.getContract());
//            } catch (DuplicateAgreementException e) {
//                e.printStackTrace();
//            }
            listDeatils.add(new PaymentList(payment));

        }

        paymentListwithSizes.add(new PaymentListwithSize(listDeatils, totalSize));
        return paymentListwithSizes;
    }


    @GET
    @JWTTokenNeeded
    @Path("twodaysbefore")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ExpSubscriptions> twoDaysBeforeBlocking(@Context HttpServletRequest request) {
        Reseller reseller = getReseller(request);
        List<Subscription> subscriptions = subscriptionFacade.findSubscriptionsByDealerLast2Days(String.valueOf(reseller.getId()));
        log.debug("twodaysbefore");
        log.debug("subscriptions.size()   " + subscriptions.size());
        List<ExpSubscriptions> expSubscriptionss = new ArrayList<>();
        for (Subscription subscription : subscriptions
                ) {

            ServiceProperty serviceProperty = servicePropertyPersistenceFacade.find(subscription.getService(),
                    subscription.getSettingByType(ServiceSettingType.ZONE));

            log.debug("serviceProperty.getServicePriceInDouble() " + serviceProperty.getServicePriceInDouble());
            log.debug("subscription.getBalance().getRealBalance() " + subscription.getBalance().getRealBalance());
            if (serviceProperty.getServicePriceInDouble() > subscription.getBalance().getRealBalance() / 100000) {
                expSubscriptionss.add(new ExpSubscriptions(subscription.getAgreement(),
                        subscription.getSettingByType(ServiceSettingType.USERNAME).getValue(),
                        subscription.getSubscriber().getDetails().getPhoneMobile(),
                        serviceProperty.getServicePriceInDouble(),
                        subscription.getBalance().getRealBalance() / 100000,
                        subscription.getLastPaymentDate(),
                        subscription.getExpirationDateWithGracePeriod().toString()));
            }

        }
        log.debug(expSubscriptionss.toString());
        return expSubscriptionss;
    }


    @GET
    @JWTTokenNeeded
    @Path("CNCandGlobaltwodaysbefore")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ExpSubscriptions> twoDaysBeforeCNCBlocking(@Context HttpServletRequest request) {
        Reseller reseller = getReseller(request);
        List<Subscription> subscriptions = subscriptionFacade.findSubscriptionsByDealerLast2Days(String.valueOf(reseller.getId()));
        log.debug("twodaysbefore");
        log.debug("subscriptions.size()   " + subscriptions.size());
        List<ExpSubscriptions> expSubscriptionss = new ArrayList<>();
        for (Subscription subscription : subscriptions
                ) {


            log.debug("serviceProperty.getServicePriceInDouble() " + subscription.getService().getServicePrice());
            log.debug("subscription.getBalance().getRealBalance() " + subscription.getBalance().getRealBalance());
            if (subscription.getService().getServicePrice() > subscription.getBalance().getRealBalance() / 100000) {
                expSubscriptionss.add(new ExpSubscriptions(subscription.getAgreement(),
                        subscription.getSettingByType(ServiceSettingType.USERNAME).getValue(),
                        subscription.getSubscriber().getDetails().getPhoneMobile(),
                        subscription.getService().getServicePrice(),
                        subscription.getBalance().getRealBalance() / 100000,
                        subscription.getLastPaymentDate(),
                        subscription.getExpirationDateWithGracePeriod().toString()));
            }

        }
        log.debug(expSubscriptionss.toString());
        return expSubscriptionss;
    }



    @GET
    @JWTTokenNeeded
    @Path("disconnect")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseMessage disconnectSubscription(@Context HttpServletRequest request, @QueryParam("agreement") String agreement) {

        Subscription subscription = subscriptionFacade.findByAgreementOrdinary(agreement);

        boolean operation = false;
        ResponseMessage responseMessage = null;
        log.debug(subscription);
        try {
            ProvisioningEngine provisioner = provisioningFactory.getProvisioningEngine(subscription);
            operation = provisioner.disconnect(subscription);
            if (operation) {
                responseMessage = new ResponseMessage("SUCCESS");
            } else {
                responseMessage = new ResponseMessage("FAILED (LINE:434)");
            }
        } catch (ProvisionerNotFoundException e) {
            log.error(e);
            responseMessage = new ResponseMessage("FAILED (LINE:438)");
        } catch (ProvisioningException e) {
            log.error(e);
            responseMessage = new ResponseMessage("FAILED (LINE:441)");
        }
        return responseMessage;
    }


    @GET
    @JWTTokenNeeded
    @Path("getMinipopIp")
    @Produces(MediaType.APPLICATION_JSON)
    public ArrayList<MinipopIP> getMinipop(@Context HttpServletRequest request, @QueryParam("agreement") String agreement) {
        ArrayList<MinipopIP> minipopIPS = new ArrayList<>();
        Subscription subscription = subscriptionFacade.findByAgreementOrdinary(agreement);
        MiniPop miniPop = null;
        MinipopIP minipopIP = null;
        try {
            miniPop = minipopFacade.find(Long.parseLong(subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH).getValue()));
            minipopIP = new MinipopIP(subscription, miniPop.getSwitch_id(), miniPop.getIp(), miniPop.getModel());

        } catch (NullPointerException n) {
            miniPop = new MiniPop();
            minipopIP = new MinipopIP(subscription, "", "", "");

        }
        minipopIPS.add(minipopIP);
        return minipopIPS;

    }




    @GET
    @JWTTokenNeeded
    @Path("getdatapServicePrice")
    @Produces(MediaType.TEXT_PLAIN)
    public  long getDataPlusServices(@Context HttpServletRequest request, @QueryParam("agreement") String agreement,
                                     @QueryParam("service") String service){

        Subscription subscription = subscriptionFacade.findByAgreementOrdinary(agreement);

        String zone = subscription.getSettingByType(ServiceSettingType.ZONE).getValue();

        ServiceProperty serviceProperty = servicePropertyPersistenceFacade.find(service, zone);

        return serviceProperty.getPrice();
    }


//    @PUT
//    @JWTTokenNeeded
//    @Path("changedatapService")
//    @Produces(MediaType.TEXT_PLAIN)
//    @Consumes(MediaType.APPLICATION_JSON)
//    public String changeDataPlusService(SubscriptionNewService subscriptionNewService){
//log.debug(subscriptionNewService.toString());
//        Subscription subscription = subscriptionFacade.findByAgreementOrdinary(subscriptionNewService.agreement);
//
////        String zone = subscription.getSettingByType(ServiceSettingType.ZONE).getValue();
//        log.debug("Current service "+subscription.toString());
//        log.debug("Desired service "+  subscriptionNewService.serviceID);
//        if (subscription.getService().getId() == subscriptionNewService.serviceID){
//            return "Same Service";
//        }
//        Service newService = servicePersistenceFacade.find(subscriptionNewService.serviceID);
//
//        subscription.setService(newService);
//
//        return "Service has changed successfully.";
//    }




    @GET
    @JWTTokenNeeded
    @Path("showautherror")
    @Produces(MediaType.APPLICATION_JSON)
    public ArrayList<RadiusErrorMessage> authError(@Context HttpServletRequest request, @QueryParam("agreement") String agreement) {
        ArrayList<RadiusErrorMessage> errorMessageList = new ArrayList<>();

        Subscription subscription = subscriptionFacade.findByAgreementOrdinary(agreement);
        ProvisioningEngine provisioner = null;
        try {
            provisioner = provisioningFactory.getProvisioningEngine(subscription);
        } catch (ProvisionerNotFoundException e) {
            e.printStackTrace();
        }
        try {
            List<String> errorList = provisioner.getAuthRejectionReasons(subscription);
            for (String error : errorList
                    ) {
                errorMessageList.add(new RadiusErrorMessage(error));
            }
        } catch (NullPointerException npe) {
            log.debug("There is not any errors:" + npe);
        }
        return errorMessageList;

    }


    @GET
    @Path("tempPayment")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseMessage addDataplusPayment(@Context HttpServletRequest request, @QueryParam("UserName") String agreement,
                                              @QueryParam("pay_date") String paymentDate, @QueryParam("payment") String amount,
                                              @QueryParam("source") String bank,
                                              @QueryParam("transactionid") String rrn, @QueryParam("id") int id) {

        DataSource dataSource = null;
        Connection connection = null;

        try {
            InitialContext initialContext = new InitialContext();
            dataSource = (DataSource) initialContext.lookup("jdbc/tekilaPool");
            connection = dataSource.getConnection();
            log.debug("successfully connected");
        } catch (NamingException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        DateFormat format = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ENGLISH);
        Date date = null;
        try {
            date = format.parse(paymentDate);
        } catch (ParseException e) {
            log.debug("ParseException EchoEndpoint:508:" + e);
        }

        String sql = "insert into dataplus_payments values (?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, id);
            preparedStatement.setDate(2, new java.sql.Date(date.getTime()));
            preparedStatement.setString(3, agreement);
            preparedStatement.setString(4, amount);
            preparedStatement.setString(5, rrn);
            preparedStatement.setString(6, bank);


            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            log.debug("SQLEXCEPTION EchoEndPoint:525" + e);
            ResponseMessage responseMessage = new ResponseMessage("Failed");
            return responseMessage;
        }

        log.debug("Insert finished successfully");
        ResponseMessage responseMessage = new ResponseMessage("Success");
        return responseMessage;
    }


    @GET
    @Path("jwt")
    @JWTTokenNeeded
    public Response echoWithJWTToken(@QueryParam("message") String message) {
        return Response.ok().entity(message == null ? "no message" : message).build();
    }


    // Entities
    public static class UserSubscriber {
        public String name;
        public String surname;
        public long user_id;
        public String username;
        public String tarif;
        public String connect_date;
        public String static_ip;
        public String contract_number;
        public String status;
        //        public DateTime last_payment_from;
        public String mobile_number;
        public String last_payment;
        public String stop_date;

        UserSubscriber() {
        }

        UserSubscriber(Subscription subscription) {
            this.name = subscription.getDetails().getName();
            this.surname = subscription.getDetails().getSurname();
            this.user_id = subscription.getId();
            this.username = subscription.getSettingByType(ServiceSettingType.USERNAME).getValue();
            this.tarif = subscription.getService().getName();
            log.debug("subscription.getActivationDate()" + subscription.getActivationDate());
            if (subscription.getActivationDate() != null) {
                this.connect_date = subscription.getActivationDate().toString();
            }
            if (subscription.getSettingByType(ServiceSettingType.IP_ADDRESS) == null) {
                this.static_ip = "";
            } else {
                this.static_ip = subscription.getSettingByType(ServiceSettingType.IP_ADDRESS).getValue();
            }
            this.contract_number = subscription.getAgreement();
            this.status = subscription.getStatus().name();
            this.mobile_number = subscription.getSubscriber().getDetails().getPhoneMobile();

            if (subscription.getLastPaymentDate() != null) {
                this.last_payment = subscription.getLastPaymentDate().toString();
            }
            if (subscription.getExpirationDateWithGracePeriod() != null) {
                this.stop_date = subscription.getExpirationDateWithGracePeriod().toString();
            }

        }
    }


    public static class SubscriptionInfo {
        public String name = " ";
        public String surname = " ";
        public double balance;
        public String connectionDate;
        public String username;
        public String password;
        public String speed;
        public String lastUsageDate;
        public String lastPaymentDate;
        public String mac;
        public String ip;
        public String expirationDate;
        public String staticIp;
        public String radiusState;
        public String status;
        public String mobileNumber;
        public String region;
        public String contract = "contract";

        SubscriptionInfo() {
        }

        SubscriptionInfo(Subscription subscription, String currentIP, String radiusState, String region) {
            this.name = subscription.getDetails().getName();
            this.surname = subscription.getDetails().getSurname();
            this.balance = subscription.getBalance().getRealBalance() / 100000;

            if (subscription.getSettingByType(ServiceSettingType.USERNAME) != null
                    || subscription.getSettingByType(ServiceSettingType.PASSWORD) != null) {
                this.username = subscription.getSettingByType(ServiceSettingType.USERNAME).getValue();
                this.password = subscription.getSettingByType(ServiceSettingType.PASSWORD).getValue();
            }


            if (subscription.getActivationDate() != null) {
                this.connectionDate = subscription.getActivationDate().toString();
            }

            if (subscription.getService() != null) {
                this.speed = subscription.getService().getName();
            }
            if (subscription.getActivationDate() != null)
                this.lastUsageDate = subscription.getActivationDate().toString();
            if (subscription.getLastPaymentDate() != null)
                this.lastPaymentDate = subscription.getLastPaymentDate().toString();
            if (subscription.getSettingByType(ServiceSettingType.MAC_ADDRESS) != null) {
                this.mac = subscription.getSettingByType(ServiceSettingType.MAC_ADDRESS).getValue();
            }
            this.ip = currentIP;
            if (subscription.getExpirationDateWithGracePeriodAsDate() != null) {
                this.expirationDate = subscription.getExpirationDateWithGracePeriodAsDate().toString();
            }
            if (subscription.getSettingByType(ServiceSettingType.IP_ADDRESS) != null) {
                this.staticIp = subscription.getSettingByType(ServiceSettingType.IP_ADDRESS).getValue();
            }
            this.radiusState = radiusState;
            this.status = subscription.getStatus().name();
            this.mobileNumber = subscription.getSubscriber().getDetails().getPhoneMobile();
            this.region = region;

        }
    }


    public static class PaymentList {
        public String user_id;
        public String payment_date;
        public Double payment_amount;
        public String rrn;
        public String payment_operator;

        public PaymentList() {
        }

        public PaymentList(Payment payment) {
            this.user_id = payment.getContract();

            this.payment_date = payment.getFd().toString();
            this.payment_amount = payment.getAmount();
            if (payment.getRrn() == null) {
                this.rrn = " ";
            } else {
                this.rrn = payment.getRrn();
            }
            if (payment.getExtUser() == null) {
                this.payment_operator = "";
            } else {
                this.payment_operator = payment.getExtUser().getUsername();
            }
        }


    }


    public static class MinipopIP {
        public String minipopIP = " ";
        public String minipopName = " ";
        public String minipopModel = " ";
        public String subscriptionPORT = " ";
        public String subscriptionSLOT = " ";
        public String subscriptionMAC = " ";
        public String subscriptionTARIFF = " ";


        public MinipopIP() {
        }

        public MinipopIP(Subscription subscription, String minipopName, String minipopIP, String minipopModel) {

            if (subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH_SLOT) != null)
                this.subscriptionSLOT = subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH_SLOT).getValue();

            this.minipopName = minipopName;
            this.minipopIP = minipopIP;

            if (subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH_PORT) != null)
                this.subscriptionPORT = subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH_PORT).getValue();

            if (subscription.getSettingByType(ServiceSettingType.MAC_ADDRESS) != null)
                this.subscriptionMAC = subscription.getSettingByType(ServiceSettingType.MAC_ADDRESS).getValue();

            this.subscriptionTARIFF = subscription.getService().getName();

            this.minipopModel = minipopModel;
        }
    }


    public static class RadiusErrorMessage {
        public String errorTEXT = " ";

        public RadiusErrorMessage() {
        }

        public RadiusErrorMessage(String message) {
            this.errorTEXT = message;

        }
    }


    public static class ResponseMessage {
        public String responseTEXT = " ";

        public ResponseMessage() {
        }

        public ResponseMessage(String message) {
            this.responseTEXT = message;

        }
    }


    public static class ExpSubscriptions {
        public String agreement;
        public String userName;
        public String mobileNumber;
        public double monthlyFee;
        public long balance;
        public String lastPaymentDate = "";
        public String expirationDate = "";

        public ExpSubscriptions() {
        }


        public ExpSubscriptions(String agreement, String userName, String mobileNumber, double monthlyFee, long balance, DateTime lastPaymentDate, String expirationDate) {
            this.agreement = agreement;
            this.userName = userName;
            this.mobileNumber = mobileNumber;
            this.monthlyFee = monthlyFee;
            this.balance = balance;
            if (lastPaymentDate != null)
                this.lastPaymentDate = lastPaymentDate.toString();

            this.expirationDate = expirationDate;
        }

        @Override
        public String toString() {
            return "ExpSubscriptions{" +
                    "agreement='" + agreement + '\'' +
                    "userName='" + userName + '\'' +
                    "mobileNumber='" + mobileNumber + '\'' +
                    ", mathlyFee=" + monthlyFee +
                    ", balance=" + balance +
                    ", lastPaymentDate=" + lastPaymentDate +
                    ", expirationDate=" + expirationDate +
                    '}';
        }
    }


}
