package com.jaravir.tekila.module.web.service.endpoint;

import com.jaravir.tekila.base.auth.exception.ParseErrorException;
import com.jaravir.tekila.base.auth.persistence.ExternalUserType;
import com.jaravir.tekila.base.auth.persistence.manager.ExternalSessionPersistenceFacade;
import com.jaravir.tekila.base.auth.persistence.manager.ExternalUserPersistenceFacade;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.base.entity.Language;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.engines.OperationsEngine;
import com.jaravir.tekila.engines.VASCreationParams;
import com.jaravir.tekila.extern.tt.TroubleTicket;
import com.jaravir.tekila.extern.tt.manager.TroubleTicketPersistenceFacade;
import com.jaravir.tekila.module.accounting.entity.*;
import com.jaravir.tekila.module.accounting.manager.ChargePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.InvoicePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.PaymentPersistenceFacade;
import com.jaravir.tekila.module.admin.AdminSettingPersistenceFacade;
import com.jaravir.tekila.module.auth.security.EncryptorAndDecryptor;
import com.jaravir.tekila.module.campaign.*;
import com.jaravir.tekila.module.event.BillingEvent;
import com.jaravir.tekila.module.event.notification.channell.NotificationChannell;
import com.jaravir.tekila.module.payment.PaymentOptionsPersistenceFacade;
import com.jaravir.tekila.module.queue.PersistentQueueManager;
import com.jaravir.tekila.module.service.NotificationSetting;
import com.jaravir.tekila.module.service.NotificationSettingRow;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.service.entity.ValueAddedService;
import com.jaravir.tekila.module.service.persistence.manager.ServicePersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.VASPersistenceFacade;
import com.jaravir.tekila.module.stats.persistence.entity.OfflineBroadbandStats;
import com.jaravir.tekila.module.stats.persistence.manager.OfflineStatsPersistenceFacade;
import com.jaravir.tekila.module.store.ScratchCardPersistenceFacade;
import com.jaravir.tekila.module.store.ScratchCardSessionPersistenceFacade;
import com.jaravir.tekila.module.store.SerialPersistenceFacade;
import com.jaravir.tekila.module.store.scratchcard.persistence.entity.ScratchCard;
import com.jaravir.tekila.module.store.scratchcard.persistence.entity.ScratchCardSession;
import com.jaravir.tekila.module.store.scratchcard.persistence.entity.Serial;
import com.jaravir.tekila.module.subscription.persistence.entity.*;
import com.jaravir.tekila.module.subscription.persistence.entity.transition.StatusChangeRule;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriberPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.module.web.service.exception.NoInvoiceFoundException;
import com.jaravir.tekila.module.web.service.exception.NoSuchSubscriberException;
import com.jaravir.tekila.module.web.service.exception.NoSuchSubscriptionException;
import com.jaravir.tekila.module.web.service.provider.BillingServiceProvider;
import com.jaravir.tekila.module.web.soap.*;
import com.jaravir.tekila.provision.broadband.devices.exception.NoFreePortLeftException;
import com.jaravir.tekila.provision.broadband.devices.exception.PortAlreadyReservedException;
import com.jaravir.tekila.provision.exception.ProvisioningException;

import java.text.SimpleDateFormat;
import java.util.*;
import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jws.WebMethod;
import javax.jws.WebParam;

import static javax.jws.WebParam.Mode.OUT;

import javax.jws.WebService;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Minutes;
import org.joda.time.Months;
import org.joda.time.format.DateTimeFormat;

/**
 * @author sgulmammadov
 */
@DeclareRoles({"system"})
@RunAs("system")
@WebService
@Stateless

//@SOAPBinding(style = SOAPBinding.Style.DOCUMENT, use = SOAPBinding.Use.LITERAL, parameterStyle = SOAPBinding.ParameterStyle.WRAPPED)
public class BillingGateway {

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    //TODO : update to 30 minutes
    private static final int LOGIN_EXPIRATION_TIME = 2;

    private static final int MAX_SMS_CODE_ATTEMPTS = 3;
    //TODO : update to 60 minutes
    private static final int SMS_CODE_EXPIRATION_TIME = 2;


    private static final int MAX_SMS_SEND_COUNT = 3;
    //TODO : update to 60 minutes
    private static final int SMS_SENT_EXPIRATION_TIME = 2;
    private static final DateTimeZone BAKU_TIME_ZONE = DateTimeZone.forOffsetHours(4);

    @Resource
    WebServiceContext wsCtxt;

    @EJB
    private BillingServiceProvider serviceProvider;
    @EJB
    private PaymentPersistenceFacade paymentFacade;
    @EJB
    private ChargePersistenceFacade chargeFacade;
    @EJB
    private CampaignPersistenceFacade campaignFacade;
    @EJB
    private CampaignJoinerBean campaignJoinerBean;
    @EJB
    private CampaignRegisterPersistenceFacade campaignRegisterFacade;
    @EJB
    private SubscriberPersistenceFacade subscriberFacade;
    @EJB
    private SubscriptionPersistenceFacade subscriptionFacade;
    @EJB
    private EngineFactory engineFactory;
    @EJB
    private UserPersistenceFacade userFacade;
    @EJB
    private VASPersistenceFacade vasFacade;
    @EJB
    private ServicePersistenceFacade serviceFacade;
    @EJB
    private ExternalUserPersistenceFacade ExternalUserFacade;
    @EJB
    private ExternalSessionPersistenceFacade ExternalSessionFacade;
    @EJB
    private OfflineStatsPersistenceFacade statsFacade;
    @EJB
    private TroubleTicketPersistenceFacade ticketFacade;
    @EJB
    private InvoicePersistenceFacade invoiceFacade;
    @EJB
    private SystemLogger systemLogger;
    @EJB
    private ScratchCardPersistenceFacade scratchCardFacade;
    @EJB
    private ScratchCardSessionPersistenceFacade sessionFacade;
    @EJB
    private AdminSettingPersistenceFacade adminSettingFacade;
    @EJB
    private EncryptorAndDecryptor encryptorAndDecryptor;
    @EJB
    private SerialPersistenceFacade serialFacade;
    @EJB
    private PaymentOptionsPersistenceFacade paymentOptionsFacade;
    @EJB
    private PersistentQueueManager queueManager;


    private static final Logger log = Logger.getLogger(BillingGateway.class);

    /**
     * Web service operation
     *
     * @param paymentID
     * @param subscriberID
     * @param subscriptionID
     * @param amount
     * @return boolean
     * @throws com.jaravir.tekila.module.web.service.exception.NoSuchSubscriberException
     * @throws com.jaravir.tekila.module.web.service.exception.NoSuchSubscriptionException
     */
    @WebMethod(operationName = "settlePayment")
    public boolean settlePayment(
            @WebParam(name = "subscriberID") final Long subscriberID,
            @WebParam(name = "subscriptionID") final Long subscriptionID,
            @WebParam(name = "amount") final Double amount,
            @WebParam(name = "paymentID") final Long paymentID
    )
            throws NoSuchSubscriberException, NoSuchSubscriptionException, NoInvoiceFoundException {

        if ((subscriberID != null && subscriberID <= 0)
                || subscriptionID == null || subscriptionID <= 0
                || amount == null || amount < 0) {
            throw new IllegalArgumentException("Please provide all parameters");
        }

        log.info("settle payment process starts for subscription "+subscriptionID + " and payment id: "+paymentID);
        long start  = System.currentTimeMillis();

        /*Subscription subscription = subscriptionFacade.find(subscriptionID);
        
         if (subscription == null) {
         throw new NoSuchSubscriptionException("subscription id " + subscription + " not found");
         }
        
         Subscriber subscriber = (subscriberID != null) ? subscriberFacade.find(subscriberID.longValue()) : subscription.getSubscriber();
        
         if (subscriber == null) {
         throw new NoSuchSubscriberException("Subscriber id " + subscriberID + " not found");
         }
         */
        log.info("Bus entered: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
        Float am = amount.floatValue() * 100000;

        boolean res = serviceProvider.settlePayment(subscriberID, subscriptionID, amount, paymentID);
        try {
            campaignJoinerBean.tryAddToCampaign(subscriptionID, paymentID, false);
        } catch (Exception ex) {
            log.error(String.format("settlePayment: error while searching for campaign, subscription id = %d, payment id=%d", subscriptionID, paymentID), ex);
        }

        try {
            campaignRegisterFacade.tryActivateCampaign(subscriptionID, paymentID);
        } catch (Exception ex) {
            log.error(String.format("settlePayment: error while searching for campaigns awaiting activation, subscription id = %d, payment id=%d", subscriptionID, paymentID), ex);
        }

        try {
            log.info("try to find matching campaign " + subscriptionID + " | " + paymentID);
            CampaignRegister campaignRegister = campaignJoinerBean.tryAddCampaignOnOnlinePayments(subscriptionID, paymentID);
//            log.info("[subscription id: "+subscriptionID+"] found campaign " + campaignRegister != null ? campaignRegister.getId() : "not found matching");
            if (campaignRegister != null) {
                log.info("online payment activated campaign register " + campaignRegister.getId() + " for subscription "+subscriptionID);
                campaignRegisterFacade.tryActivateCampaign(campaignRegister,false);
            }
        } catch (Exception ex) {
            log.error("settlePayment: error occurs while trying to add campaign on online payments for subscription "+subscriptionID, ex);
        }
        log.info("settle payment process ends for subscription "+subscriptionID + " and payment id: "+paymentID + " [elapsed time: "+(System.currentTimeMillis()-start)/1000.+" seconds]");

        return res;
    }

    @WebMethod
    public String ping() {
        return "success";
    }

    @WebMethod
    public long makeExternalPayment(
            @WebParam(name = "amount") final Double amount,
            @WebParam(name = "userID") final long extUserID,
            @WebParam(name = "serviceID") long serviceID,
            @WebParam(name = "accountID") long accountID,
            @WebParam(name = "contract") final String contract,
            @WebParam(name = "subscriberID") final long subscriberID,
            @WebParam(name = "rid") String rid,
            @WebParam(name = "rrn") String rrn,
            @WebParam(name = "sessID") String sessID,
            @WebParam(name = "dsc") String dsc,
            @WebParam(name = "currency") String currency,
            @WebParam(name = "dateTime") String dateTime) {

        try {
            log.info("external payment request received for contract " + contract + ", rrn = " + rrn);
            long start  = System.currentTimeMillis();
            Payment payment = paymentFacade.create(amount, extUserID, serviceID, accountID, contract, subscriberID, currency, rid, rrn, sessID, dsc, dateTime);
            log.info("makeExternalPayment: success [elapsed time: " + (System.currentTimeMillis()-start)/1000. + " seconds]. Payment after save: " + payment);
            return payment.getId();
        } catch (Exception ex) {
            log.error("Cannot make external payment for subscription "+accountID+", rrn: "+rrn, ex);
            return -1;
        }
    }



    /**
     * Add payment to {@code proccessedPaymentQueue} which is monitored by
     * <b>Provisioning Gateway</b>
     * triggering provisioning attempt
     *
     * @param subscriberID
     * @param subscriptionID
     * @param amount
     * @return
     */
    @WebMethod(operationName = "addToPaymentQueue")
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean addToPaymentQueue(
            @WebParam(partName = "subscriberID") final Long subscriberID,
            @WebParam(partName = "subscriptionID") final Long subscriptionID,
            @WebParam(partName = "amount") final Double amount
    ) {
        StringBuilder stringBuilder = new StringBuilder("ADD_TO_PAYMENT_QUEUE");
        stringBuilder.append(", subscriberID = " + subscriberID);
        stringBuilder.append(", subscriptionID = " + subscriptionID);
        stringBuilder.append(", amount = " + amount);
        log.debug(stringBuilder.toString());
        return serviceProvider.addToPaymentQueue(subscriberID, subscriptionID, amount);
    }

    @WebMethod
    public long makeExternalPaymentReverse(
            @WebParam(name = "rid") String rid,
            @WebParam(name = "rrn") String rrn,
            @WebParam(name = "sessID") String sessID,
            @WebParam(name = "dsc") String dsc,
            @WebParam(name = "dateTime") String dateTime) throws Exception {

        try {
            List<Payment> paymentList = paymentFacade.findAllByDateAndRRN(dateTime, rrn);

            if (paymentList.size() < 1) {
                log.error("Can not make external payment reverse. Payment not found or with status = -1 (reversed): rrn = " + rrn + ", datetime = " + dateTime);
                throw new Exception("No such payment. It may be reversed before this attempt.");
            }

            if (paymentList.size() > 1) {
                log.error("Can not make external payment reverse");
                throw new Exception("Can not make external payment reverse. Too many payments found. rrn = " + rrn + ", datetime = " + dateTime);
            }

            if (paymentList.size() == 1) {
                paymentFacade.cancelPayment(paymentList.get(0).getId());
            }
            log.info("makeExternalPaymentReverse: success. Payment after save: " + (Payment) paymentList.get(0));
            return ((Payment) paymentList.get(0)).getId();

        } catch (Exception ex) {
            log.error("Cannot make external payment reverse", ex);
            return -1;
        }
    }

    /* ---------------------------------------------------------------------------------------------------------*/
    private boolean checkInput(Object... objects) {
        try {
            for (Object o : objects) {
                if (o == null) {
                    return true;
                }
            }
            return false;
        } catch (NullPointerException n) {
            return true;
        }
    }

    /**
     * @param username
     * @param password
     * @param session
     * @param retval
     * @param retmsg
     */
    @WebMethod
    public void signIn(
            @WebParam(name = "username") String username,
            @WebParam(name = "password") String password,
            @WebParam(name = "session", mode = OUT) Holder<String> session,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) {

        if (checkInput(username, password)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        MessageContext msgCtxt = wsCtxt.getMessageContext();
        HttpServletRequest req = (HttpServletRequest) msgCtxt.get(MessageContext.SERVLET_REQUEST);
        String remoteAddress = req.getRemoteAddr();

        try {

            if (!ExternalUserFacade.isAllowed(username, remoteAddress)) {
                retval.value = 0;
                retmsg.value = "Not allowed from : " + remoteAddress;
                return;
            }

            if (ExternalUserFacade.authenticate(username, password)) {
                session.value = ExternalSessionFacade.add(ExternalUserType.GATEWAY.getCode(), remoteAddress);
                retval.value = 1;
                retmsg.value = "Operation was successfull";
                return;
            }

            retval.value = 0;
            retmsg.value = "Authentification failed";
        } catch (Exception e) {
            retval.value = 0;
            retmsg.value = "Exception";
            e.printStackTrace();
        }
    }

    /**
     * @param session
     * @param retval
     * @param retmsg
     */
    @WebMethod
    public void signOut(
            @WebParam(name = "session") String session,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) {

        if (checkInput(session)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        try {
            if (ExternalSessionFacade.findGatewaySession(session) == null) {
                retval.value = 0;
                retmsg.value = "Session not found";
                return;
            }

            ExternalSessionFacade.delete(ExternalSessionFacade.findGatewaySession(session));
            retval.value = 1;
            retmsg.value = "Operation was successfull";

        } catch (Exception e) {
            retval.value = 0;
            retmsg.value = "Exception";
            e.printStackTrace();
        }
    }

    /**
     * @param session
     * @param agreement
     * @param shaHash
     * @param retval
     * @param retmsg
     */
    @WebMethod
    public void setAuthenticationDetails(
            @WebParam(name = "session") String session,
            @WebParam(name = "agreement") String agreement,
            @WebParam(name = "shaHash") String shaHash,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) {
        if (checkInput(session, agreement, shaHash)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        if (ExternalSessionFacade.findGatewaySession(session) == null) {
            retval.value = 0;
            retmsg.value = "Session not found";
            return;
        }

        Subscription subscription = subscriptionFacade.findByAgreementOrdinary(agreement);
        if (subscription != null) {
            SubscriptionRuntimeDetails runtimeDetails = subscription.getRuntimeDetails();
            if (runtimeDetails == null) {
                runtimeDetails = new SubscriptionRuntimeDetails();
                runtimeDetails.setFirstVisitTime(DateTime.now());
            }
            runtimeDetails.setShaHash(shaHash);
            runtimeDetails.setExpired(false);
            runtimeDetails.setPasswordResetTime((new DateTime().withZone(BAKU_TIME_ZONE)));
            subscription.setRuntimeDetails(runtimeDetails);
            subscriptionFacade.update(subscription);
            retval.value = 1;
            retmsg.value = "Password reset details have been persisted in data store";
        } else {
            retval.value = 0;
            retmsg.value = "Subscription not found";
        }
    }

    /**
     * @param session
     * @param shaHash
     * @param retval
     * @param retmsg
     * @param agreement
     * @param expired
     * @param passwordResetTime
     */
    @WebMethod
    public void getAuthenticationDetails(
            @WebParam(name = "session") String session,
            @WebParam(name = "shaHash") String shaHash,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg,
            @WebParam(name = "agreement", mode = OUT) Holder<String> agreement,
            @WebParam(name = "expired", mode = OUT) Holder<Boolean> expired,
            @WebParam(name = "passwordResetTime", mode = OUT) Holder<String> passwordResetTime
    ) {
        if (checkInput(session, shaHash)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        if (ExternalSessionFacade.findGatewaySession(session) == null) {
            retval.value = 0;
            retmsg.value = "Session not found";
            return;
        }

        Subscription subscription = subscriptionFacade.findByRuntimeShaHash(shaHash);
        if (subscription != null && subscription.getRuntimeDetails() != null) {
            agreement.value = subscription.getAgreement();
            expired.value = subscription.getRuntimeDetails().isExpired();
            passwordResetTime.value =
                    DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss").print(subscription.getRuntimeDetails().getPasswordResetTime().withZone(BAKU_TIME_ZONE));
            retval.value = 1;
            retmsg.value = "Password reset details have been fetched from data store";
        } else {
            retval.value = 0;
            retmsg.value = "Invalid sha hash value specified";
        }
    }

    /**
     * @param session
     * @param shaHash
     * @param retval
     * @param retmsg
     */
    @WebMethod
    public void setExpired(
            @WebParam(name = "session") String session,
            @WebParam(name = "shaHash") String shaHash,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) {
        if (checkInput(session, shaHash)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        if (ExternalSessionFacade.findGatewaySession(session) == null) {
            retval.value = 0;
            retmsg.value = "Session not found";
            return;
        }

        Subscription subscription = subscriptionFacade.findByRuntimeShaHash(shaHash);
        if (subscription != null && subscription.getRuntimeDetails() != null) {
            subscription.getRuntimeDetails().setExpired(true);
            subscriptionFacade.update(subscription);
            retval.value = 1;
            retmsg.value = "Password reset hash value is expired";
        } else {
            retval.value = 0;
            retmsg.value = "Invalid sha hash value specified";
        }
    }

    /**
     * @param session
     * @param agreement
     * @param retval
     * @param retmsg
     * @param firstname
     * @param surname
     * @param birthDate
     * @param email
     */
    @WebMethod
    public void getAgreementDetails(
            @WebParam(name = "session") String session,
            @WebParam(name = "agreement") String agreement,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg,
            @WebParam(name = "firstname", mode = OUT) Holder<String> firstname,
            @WebParam(name = "surname", mode = OUT) Holder<String> surname,
            @WebParam(name = "birthDate", mode = OUT) Holder<String> birthDate,
            @WebParam(name = "email", mode = OUT) Holder<String> email) {

        if (checkInput(session, agreement)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        try {
            if (ExternalSessionFacade.findGatewaySession(session) == null) {
                retval.value = 0;
                retmsg.value = "Session not found";
                return;
            }

            Subscription subscription = subscriptionFacade.findByAgreementOrdinary(agreement);
            if (subscription != null) {
                retval.value = 1;
                retmsg.value = "Agreement exists";
                if (subscription.getDetails() != null && subscription.getDetails().getName() != null) {
                    firstname.value = subscription.getDetails().getName();
                    surname.value = subscription.getDetails().getSurname();
                } else if (subscription.getSubscriber() != null &&
                        subscription.getSubscriber().getDetails() != null) {
                    firstname.value = subscription.getSubscriber().getDetails().getFirstName();
                    surname.value = subscription.getSubscriber().getDetails().getSurname();
                }

                if (subscription.getSubscriber() != null &&
                        subscription.getSubscriber().getDetails() != null &&
                        subscription.getSubscriber().getDetails().getDateOfBirth() != null) {
                    SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy");
                    birthDate.value = df.format(subscription.getSubscriber().getDetails().getDateOfBirth());
                }

                /*if (subscription.getRuntimeDetails() != null) {
                    email.value = subscription.getRuntimeDetails().getEmail();
                }*/
                if (subscription.getSubscriber() != null &&
                        subscription.getSubscriber().getDetails() != null) {
                    email.value = subscription.getSubscriber().getDetails().getEmail();
                }
            } else {
                retval.value = 0;
                retmsg.value = "Agreement does not exist";
            }
        } catch (Exception e) {
            retval.value = 0;
            retmsg.value = "Exception";
            e.printStackTrace();
        }
    }

    /**
     * @param session
     * @param contract
     * @param password
     * @param retval
     * @param retmsg
     */
    @WebMethod
    public void setSubscriptionPassword(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "password") String password,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) {
        //update subscription password using global session
        if (checkInput(session, contract, password)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        if (ExternalSessionFacade.findGatewaySession(session) == null) {
            retval.value = 0;
            retmsg.value = "Session not found";
            return;
        }

        Subscription subscription = subscriptionFacade.findByAgreementOrdinary(contract);
        if (subscription != null) {
            subscription.getDetails().setPassword(password);
            if (subscription.getRuntimeDetails() != null) {
                subscription.getRuntimeDetails().setShaHash(null);
            }
            subscriptionFacade.update(subscription);
            retval.value = 1;
            retmsg.value = "Subscription password has been updated";
        } else {
            retval.value = 0;
            retmsg.value = "Subscription not found for this agreement number";
        }
    }

    /**
     * @param session
     * @param contract
     * @param email
     * @param emailHash
     * @param retval
     * @param retmsg
     */
    @WebMethod
    public void setEmailActivationDetails(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "email") String email,
            @WebParam(name = "emailHash") String emailHash,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) {
        //update subscription runtime details for email
        if (checkInput(session, contract, email, emailHash)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        if (ExternalSessionFacade.findSubscriberSession(session) == null) {
            retval.value = 0;
            retmsg.value = "Session not found";
            return;
        }

        Subscription subscription = subscriptionFacade.findByAgreementOrdinary(contract);
        if (subscription == null) {
            retval.value = 0;
            retmsg.value = "Subscription not found";
            return;
        }
        SubscriptionRuntimeDetails runtimeDetails = subscription.getRuntimeDetails();
        if (runtimeDetails == null) {
            runtimeDetails = new SubscriptionRuntimeDetails();
            runtimeDetails.setFirstVisitTime(DateTime.now());
        }
        //runtimeDetails.setEmail(email);
        if (subscription.getSubscriber() != null &&
                subscription.getSubscriber().getDetails() != null) {
            subscription.getSubscriber().getDetails().setEmail(email);
        }
        runtimeDetails.setEmailChecksum(emailHash);
        runtimeDetails.setEmailActivated(false);
        subscription.setRuntimeDetails(runtimeDetails);
        subscriptionFacade.update(subscription);
        retval.value = 1;
        retmsg.value = "Subscription email and checksum has been set";
    }

    /**
     * @param session
     * @param emailHash
     * @param retval
     * @param retmsg
     */
    @WebMethod
    public void activateEmail(
            @WebParam(name = "session") String session,
            @WebParam(name = "emailHash") String emailHash,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) {
        if (checkInput(session, emailHash)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        if (ExternalSessionFacade.findGatewaySession(session) == null) {
            retval.value = 0;
            retmsg.value = "Session not found";
            return;
        }

        Subscription subscription = subscriptionFacade.findByEmailHash(emailHash);
        if (subscription != null && subscription.getRuntimeDetails() != null) {
            subscription.getRuntimeDetails().setEmailActivated(true);
            subscription.getRuntimeDetails().setRegistrationTime(DateTime.now());
            subscription.getRuntimeDetails().setEmailChecksum(null);
            subscriptionFacade.update(subscription);
            retval.value = 1;
            retmsg.value = "Subscription email has been activated";
        } else {
            retval.value = 0;
            retmsg.value = "Invalid email hash has been specified";
        }
    }

    /**
     * @param session
     * @param contract
     * @param password
     * @param email
     * @param contactNumber
     * @param birthDate
     * @param retval
     * @param retmsg
     */
    @WebMethod
    public void setSubscriberDetails(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "password") String password,
            @WebParam(name = "email") String email,
            @WebParam(name = "contactNumber") String contactNumber,
            @WebParam(name = "birthDate") String birthDate,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) {
        if (ExternalSessionFacade.findSubscriberSession(session) == null) {
            retval.value = 0;
            retmsg.value = "Session not found";
            return;
        }
        try {
            Subscription subscription = subscriptionFacade.findByAgreementOrdinary(contract);
            if (subscription == null) {
                retval.value = 0;
                retmsg.value = "Subscription not found";
                return;
            }

            if (password != null) {
                subscription.getDetails().setPassword(password);
            }
            if (email != null) {
                subscription.getSubscriber().getDetails().setEmail(email);
            }
            if (contactNumber != null) {
                subscription.getSubscriber().getDetails().setPhoneMobile(contactNumber);

                SubscriptionRuntimeDetails runtimeDetails = subscription.getRuntimeDetails();
                if (runtimeDetails == null) {
                    runtimeDetails = new SubscriptionRuntimeDetails();
                    runtimeDetails.setFirstVisitTime(DateTime.now());
                }
                runtimeDetails.setPhoneNumber(contactNumber);
                runtimeDetails.setPhoneLastUpdateDate(DateTime.now());
                subscription.setRuntimeDetails(runtimeDetails);
            }
            if (birthDate != null) {
                log.info(String.format("setSubscriberDetails() called. birthDate=%s, agreement=%s", birthDate, contract));
                SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
                subscription.getSubscriber().getDetails().setDateOfBirth(format.parse(birthDate));
            }
            subscriptionFacade.update(subscription);
            retval.value = 1;
            retmsg.value = "Changes have been persisted in data store";
        } catch (Exception ex) {
            retval.value = 0;
            retmsg.value = "Exception occurred, could not persist";
        }
    }

    /**
     * @param gatewaySession
     * @param contract
     * @param password
     * @param remoteAddress
     * @param session
     * @param retval
     * @param retmsg
     */
    @WebMethod
    public void subscriberLogin(
            @WebParam(name = "gatewaySession") String gatewaySession,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "password") String password,
            @WebParam(name = "remoteAddress") String remoteAddress,
            @WebParam(name = "session", mode = OUT) Holder<String> session,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) {

        if (checkInput(gatewaySession, contract, password, remoteAddress)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        try {
            if (ExternalSessionFacade.findGatewaySession(gatewaySession) == null) {
                retval.value = 0;
                retmsg.value = "Session not found";
                return;
            }
            if (!subscriptionFacade.isAvailableOnEcare(contract)) {
                retval.value = 0;
                retmsg.value = "Subscriprion is not available for eCare";
                return;
            }

            String persistedPassword = subscriptionFacade.findPasswordByAgreement(contract);
            Subscription subscription = subscriptionFacade.findByAgreementOrdinary(contract);
            SubscriptionRuntimeDetails runtimeDetails = subscription.getRuntimeDetails();

            if (runtimeDetails == null || !runtimeDetails.isEmailActivated()) { //email not activated
                if (!(persistedPassword != null && persistedPassword.equals(password) && persistedPassword.equals(contract))) { // not password reset
                    retval.value = 7;
                    retmsg.value = "Please activate email to login";
                    return;
                }
            }

            if (runtimeDetails != null
                    && runtimeDetails.getLoginAttempt() > MAX_LOGIN_ATTEMPTS
                    && Minutes.minutesBetween(runtimeDetails.getLastUpdateDate(), new DateTime()).getMinutes() < LOGIN_EXPIRATION_TIME) {
                retval.value = 4;
                retmsg.value = String.format("login expiration time %d minutes has not been elapsed since last unsuccesfull login", LOGIN_EXPIRATION_TIME);
                return;
            }

            if (password == null || !password.equals(persistedPassword)) {
                if (runtimeDetails == null) {
                    runtimeDetails = new SubscriptionRuntimeDetails();
                    runtimeDetails.setFirstVisitTime(DateTime.now());
                }
                int loginAttempt = runtimeDetails.getLoginAttempt();
                ++loginAttempt;
                runtimeDetails.setLoginAttempt(loginAttempt);
                subscription.setRuntimeDetails(runtimeDetails);
                subscriptionFacade.update(subscription);
                if (loginAttempt > MAX_LOGIN_ATTEMPTS) {
                    retval.value = 2;
                    retmsg.value = String.format("number of login attempts exceeded for %s", subscription.getAgreement());
                    return;
                } else {
                    retval.value = 3;
                    retmsg.value = String.format("current number of login attempt = %d, agreement = %s", loginAttempt, subscription.getAgreement());
                    return;
                }
            } else {
                if (runtimeDetails != null) {
                    runtimeDetails.setLastLoginTime(DateTime.now());
                    runtimeDetails.setLoginAttempt(0);
                    subscriptionFacade.update(subscription);
                }
            }

            if (persistedPassword == null) {
                retval.value = 5; //DONT CHANGE 5! client code is expecting
                retmsg.value = "Password is not defined for this user. Please call center to reset password";
                return;
            } else if (persistedPassword.equals(password) && persistedPassword.equals(contract)) {
                session.value = ExternalSessionFacade.add(ExternalUserType.SUBSCRIBER.getCode(), remoteAddress);
                retval.value = 6; //DONT CHANGE 6! client code is expecting
                // if no password is specified in data store, we are allowing the user in
                // to handle the case of first-time login to the system(to specify password)
                retmsg.value = "Password is equal to the default resetted value. Please reset password";
                return;
            } else if (persistedPassword.equals(password)) {
                session.value = ExternalSessionFacade.add(ExternalUserType.SUBSCRIBER.getCode(), remoteAddress);
                retval.value = 1;
                retmsg.value = "Operation was successfull";
                return;
            }

            retval.value = 0;
            retmsg.value = "Authentification failed";
        } catch (Exception e) {
            retval.value = 0;
            retmsg.value = "Exception";
            e.printStackTrace();
        }
    }

    /**
     * @param session
     * @param retval
     * @param retmsg
     */
    @WebMethod
    public void subscriberLogout(
            @WebParam(name = "session") String session,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) {

        if (checkInput(session)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        try {
            if (ExternalSessionFacade.findSubscriberSession(session) == null) {
                retval.value = 0;
                retmsg.value = "Session not found";
                return;
            }

            ExternalSessionFacade.delete(ExternalSessionFacade.findSubscriberSession(session));
            retval.value = 1;
            retmsg.value = "Operation was successfull";

        } catch (Exception e) {
            retval.value = 0;
            retmsg.value = "Exception";
            e.printStackTrace();
        }
    }

    /**
     * @param contract
     * @param session
     * @param subscription
     * @param retval
     * @param retmsg
     */
    @WebMethod
    public void getSubscription(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "subscription", mode = OUT) Holder<SubscriptionResponse> subscription,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) {

        if (checkInput(session, contract)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        try {
            if (ExternalSessionFacade.findSubscriberSession(session) == null) {
                retval.value = 0;
                retmsg.value = "Session not found";
                return;
            }

            subscription.value = new SubscriptionResponse(subscriptionFacade.findByAgreementOrdinary(contract));

        } catch (Exception e) {
            retval.value = 0;
            retmsg.value = "Exception";
            e.printStackTrace();
        }
    }

    /**
     * @param contract
     * @param session
     * @param subscriber
     * @param retval
     * @param retmsg
     */
    @WebMethod
    public void getSubscriber(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "subscriber", mode = OUT) Holder<SubscriberResponse> subscriber,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) {

        if (checkInput(session, contract)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        try {

            if (ExternalSessionFacade.findSubscriberSession(session) == null) {
                retval.value = 0;
                retmsg.value = "Session not found";
                return;
            }

            subscriber.value = new SubscriberResponse(subscriptionFacade.findByAgreementOrdinary(contract).getSubscriber());

        } catch (Exception e) {
            retval.value = 0;
            retmsg.value = "Exception";
            e.printStackTrace();
        }
    }

    /**
     * @param contract
     * @param session
     * @param start_date
     * @param end_date
     * @param retval
     * @param retmsg
     * @param payments
     */
    @WebMethod
    public void getPayments(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "start_date") String start_date,
            @WebParam(name = "end_date") String end_date,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg,
            @WebParam(name = "payment", mode = OUT) Holder<List<PaymentResponse>> payments
    ) {

        if (checkInput(session, contract, start_date, end_date)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        try {

            if (ExternalSessionFacade.findSubscriberSession(session) == null) {
                retval.value = 0;
                retmsg.value = "Session not found";
                return;
            }

            List<Payment> systemPayments = paymentFacade.findAllByAgreementAndDates(contract, start_date, end_date);
            List<PaymentResponse> returnPayments = new ArrayList<>();
            for (Payment p : systemPayments) {
                returnPayments.add(new PaymentResponse(p));
            }
            payments.value = returnPayments;

        } catch (Exception e) {
            retval.value = 0;
            retmsg.value = "Exception";
            e.printStackTrace();
        }

    }

    /**
     * @param contract
     * @param session
     * @param start_date
     * @param end_date
     * @param retval
     * @param retmsg
     * @param charges
     */
    @WebMethod
    public void getCharges(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "start_date") String start_date,
            @WebParam(name = "end_date") String end_date,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg,
            @WebParam(name = "charge", mode = OUT) Holder<List<ChargeResponse>> charges
    ) {

        if (checkInput(session, contract, start_date, end_date)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        try {

            if (ExternalSessionFacade.findSubscriberSession(session) == null) {
                retval.value = 0;
                retmsg.value = "Session not found";
                return;
            }

            List<Charge> systemCharges = chargeFacade.findByAgreement(contract, start_date, end_date);
            List<ChargeResponse> returnCharges = new ArrayList<>();
            for (Charge c : systemCharges) {
                returnCharges.add(new ChargeResponse(c));
            }
            charges.value = returnCharges;

        } catch (Exception e) {
            retval.value = 0;
            retmsg.value = "Exception";
            e.printStackTrace();
        }

    }

    /**
     * @param contract
     * @param session
     * @param start_date
     * @param end_date
     * @param retval
     * @param retmsg
     * @param usages
     */
    @WebMethod
    public void getUsages(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "start_date") String start_date,
            @WebParam(name = "end_date") String end_date,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg,
            @WebParam(name = "Usage", mode = OUT) Holder<List<OfflineBroadbandStats>> usages
    ) {

        if (checkInput(session, contract, start_date, end_date)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        try {

            if (ExternalSessionFacade.findSubscriberSession(session) == null) {
                retval.value = 0;
                retmsg.value = "Session not found";
                return;
            }

            List<OfflineBroadbandStats> returnUsages = statsFacade.findByAgreement(contract, start_date, end_date);
            usages.value = returnUsages;

        } catch (Exception e) {
            retval.value = 0;
            retmsg.value = "Exception";
            e.printStackTrace();
        }

    }

    /**
     * @param contract
     * @param session
     * @param start_date
     * @param end_date
     * @param retval
     * @param retmsg
     * @param invoices
     */
    @WebMethod
    public void getInvoices(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "start_date") String start_date,
            @WebParam(name = "end_date") String end_date,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg,
            @WebParam(name = "Invoice", mode = OUT) Holder<List<InvoiceResponse>> invoices
    ) {

        if (checkInput(session, contract, start_date, end_date)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        try {

            if (ExternalSessionFacade.findSubscriberSession(session) == null) {
                retval.value = 0;
                retmsg.value = "Session not found";
                return;
            }

            List<Invoice> systemInvoices = subscriptionFacade.findInvoicesForSubscriber(contract, start_date, end_date);
            log.info("Invoice count for subscription " + contract + " = " +
                    (systemInvoices == null ? 0 : systemInvoices.size()));
            if (systemInvoices == null) {
                retval.value = 0;
                retmsg.value = "Invoices not found for given subscriber";
                return;
            }
            List<InvoiceResponse> returnInvoices = new ArrayList<>();
            for (Invoice i : systemInvoices) {
                returnInvoices.add(new InvoiceResponse(i));
            }
            invoices.value = returnInvoices;

        } catch (Exception e) {
            retval.value = 0;
            retmsg.value = "Exception";
            e.printStackTrace();
        }

    }

    /**
     * @param contract
     * @param session
     * @param start_date
     * @param end_date
     * @param retval
     * @param retmsg
     * @param tickets
     */
    @WebMethod
    public void getTickets(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "start_date") String start_date,
            @WebParam(name = "end_date") String end_date,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg,
            @WebParam(name = "Ticket", mode = OUT) Holder<List<TroubleTicket>> tickets
    ) {

        if (checkInput(session, contract, start_date, end_date)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        try {

            if (ExternalSessionFacade.findSubscriberSession(session) == null) {
                retval.value = 0;
                retmsg.value = "Session not found";
                return;
            }

            List<TroubleTicket> returnTickets = ticketFacade.findByAgreementAndDates(contract, start_date, end_date);
            tickets.value = returnTickets;

        } catch (Exception e) {
            retval.value = 0;
            retmsg.value = "Exception";
            e.printStackTrace();
        }

    }

    /**
     * @param contract
     * @param session
     * @param retval
     * @param retmsg
     * @param campaigns
     */
    @WebMethod
    public void getSubscriptionCampaigns(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg,
            @WebParam(name = "Campaign", mode = OUT) Holder<List<CampaignResponse>> campaigns
    ) {

        if (checkInput(session, contract)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        try {

            if (ExternalSessionFacade.findSubscriberSession(session) == null) {
                retval.value = 0;
                retmsg.value = "Session not found";
                return;
            }

            List<Campaign> systemCampaigns = campaignFacade.findBySubscription(subscriptionFacade.findByAgreementOrdinary(contract));
            List<CampaignResponse> returnCampaigns = new ArrayList<>();
            for (Campaign item : systemCampaigns) {
                returnCampaigns.add(new CampaignResponse(item));
            }
            campaigns.value = returnCampaigns;

        } catch (Exception e) {
            retval.value = 0;
            retmsg.value = "Exception";
            e.printStackTrace();
        }

    }

    /**
     * @param contract
     * @param session
     * @param retval
     * @param retmsg
     * @param vas
     */
    @WebMethod
    public void getSubscriptionAllowedVas(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg,
            @WebParam(name = "Vas", mode = OUT) Holder<List<ValueAddedServiceResponse>> vas
    ) {

        if (checkInput(session, contract)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        try {

            if (ExternalSessionFacade.findSubscriberSession(session) == null) {
                retval.value = 0;
                retmsg.value = "Session not found";
                return;
            }

            List<ValueAddedService> systemVas = subscriptionFacade.findByAgreementOrdinary(contract).getService().getAllowedVASList();
            List<ValueAddedServiceResponse> returnVas = new ArrayList<>();
            for (ValueAddedService item : systemVas) {
                returnVas.add(new ValueAddedServiceResponse(item));
            }
            vas.value = returnVas;

        } catch (Exception e) {
            retval.value = 0;
            retmsg.value = "Exception";
            e.printStackTrace();
        }

    }

    /**
     * @param contract
     * @param session
     * @param retval
     * @param retmsg
     * @param vas
     */
    @WebMethod
    public void getSubscriptionActiveVas(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg,
            @WebParam(name = "Vas", mode = OUT) Holder<List<ValueAddedServiceResponse>> vas
    ) {

        if (checkInput(session, contract)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        try {

            if (ExternalSessionFacade.findSubscriberSession(session) == null) {
                retval.value = 0;
                retmsg.value = "Session not found";
                return;
            }

            List<SubscriptionVAS> subscriptionVas = subscriptionFacade.findByAgreementOrdinary(contract).getVasList();
            List<ValueAddedServiceResponse> returnActiveVas = new ArrayList<>();
            for (SubscriptionVAS item : subscriptionVas) {
                returnActiveVas.add(new ValueAddedServiceResponse(item.getVas()));
            }
            vas.value = returnActiveVas;

        } catch (Exception e) {
            retval.value = 0;
            retmsg.value = "Exception";
            e.printStackTrace();
        }

    }

    /**
     * @param contract
     * @param session
     * @param retval
     * @param retmsg
     * @param campaigns
     */
    @WebMethod
    public void getSubscriptionAllowedCampaigns(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg,
            @WebParam(name = "Campaign", mode = OUT) Holder<List<CampaignResponse>> campaigns
    ) {

        if (checkInput(session, contract)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        try {

            if (ExternalSessionFacade.findSubscriberSession(session) == null) {
                retval.value = 0;
                retmsg.value = "Session not found";
                return;
            }

            List<Campaign> availableCampaignList;
            Subscription selected = subscriptionFacade.findByAgreementOrdinary(contract);
            List<CampaignRegister> regList = campaignRegisterFacade.findActive(selected);
            List<Campaign> cmpList = new ArrayList<>();

            if (regList != null && !regList.isEmpty()) {
                for (CampaignRegister reg : regList) {
                    cmpList.add(reg.getCampaign());
                }
            }

            availableCampaignList = campaignFacade.findAllActive(selected.getService(), cmpList);
            List<CampaignRegister> campRegList = campaignRegisterFacade.findAllBySubscription(selected);
            boolean found = false;

            if (campRegList == null || campRegList.isEmpty()) {
                return;
            }

            Campaign cmp = null;
            Iterator<Campaign> it = availableCampaignList.iterator();

            while (it.hasNext()) {
                cmp = it.next();

                log.debug(String.format("getAvailableCampaignList: analyzing campaign id=%d", cmp.getId()));
                for (CampaignRegister reg : campRegList) {
                    if (reg.getCampaign().getId() == cmp.getId()) {
                        log.debug(String.format("getAvailableCampaignList: found match, campaign id=%d, regcampid=%d",
                                cmp.getId(), reg.getCampaign().getId()));
                        found = true;
                    } else if (cmp.isCompound()) {
                        for (Campaign subCampaign : cmp.getCampaignList()) {
                            log.debug(String.format("getAvailableCampaignList: analyzing subcampaign id=%d", subCampaign.getId()));
                            if (reg.getCampaign().getId() == subCampaign.getId()) {
                                log.debug(String.format("getAvailableCampaignList: found match, subcampaign id=%d, regcampid=%d",
                                        subCampaign.getId(), reg.getCampaign().getId()));
                                found = true;
                            }
                        }
                    }

                    if (found) {
                        log.debug(String.format("getAvailableCampaignList: removing campaign id=%d", cmp.getId()));
                        try {
                            it.remove();
                        } catch (Exception ex) {
                            log.error(ex);
                        }
                        found = false;
                    }

                }

            }

            List<CampaignResponse> returnCampaigns = new ArrayList<>();
            for (Campaign item : availableCampaignList) {
                returnCampaigns.add(new CampaignResponse(item));
            }

            campaigns.value = returnCampaigns;

        } catch (Exception e) {
            retval.value = 0;
            retmsg.value = "Exception";
            e.printStackTrace();
        }

    }

    /**
     * @param contract
     * @param session
     * @param retval
     * @param retmsg
     * @param services
     */
    @WebMethod
    public void getSubscriptionAllowedServices(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg,
            @WebParam(name = "Service", mode = OUT) Holder<List<ServiceResponse>> services
    ) {

        if (checkInput(session, contract)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        try {

            if (ExternalSessionFacade.findSubscriberSession(session) == null) {
                retval.value = 0;
                retmsg.value = "Session not found";
                return;
            }

            Subscription selected = subscriptionFacade.findByAgreementOrdinary(contract);
            serviceFacade.clearFilters();
            serviceFacade.addFilter(ServicePersistenceFacade.Filter.PROVIDER_ID, selected.getService().getProvider().getId());
            serviceFacade.addFilter(ServicePersistenceFacade.Filter.TYPE, selected.getService().getServiceType());
            ServicePersistenceFacade.Filter serviceIDfilter = ServicePersistenceFacade.Filter.SERVICE_ID;
            serviceIDfilter.setOperation(MatchingOperation.NOT_EQUALS);
            serviceFacade.addFilter(serviceIDfilter, selected.getService().getId());
            serviceFacade.addFilter(ServicePersistenceFacade.Filter.SUBGROUP_ID, selected.getService().getSubgroup().getId());

            List<Service> systemServices = serviceFacade.findAllPaginated(0, 1000);

            List<ServiceResponse> returnServices = new ArrayList<>();
            if (selected.getService() != null && selected.getService().isEligibleForServiceChange()) {
                for (Service item : systemServices) {
                    if (item.isAvailableOnEcare() &&
                            !(item.getSubgroup() != null && item.getSubgroup().getName().equals("OLD"))) {
                        returnServices.add(new ServiceResponse(item));
                    }
                }
            }
            Collections.sort(returnServices);
            services.value = returnServices;

        } catch (Exception e) {
            retval.value = 0;
            retmsg.value = "Exception";
            e.printStackTrace();
        }

    }

    /**
     * @param contract
     * @param session
     * @param email
     * @param mobile1
     * @param mobile2
     * @param landline
     * @param retval
     * @param retmsg
     */
    @WebMethod
    public void updateSubscriber(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "Email") String email,
            @WebParam(name = "PhoneMobile") String mobile1,
            @WebParam(name = "PhoneMobileAlt") String mobile2,
            @WebParam(name = "PhoneLandline") String landline,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) {

        String logMessage = "updateSubscriber. ";

        if (checkInput(session, contract)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        if (ExternalSessionFacade.findSubscriberSession(session) == null) {
            retval.value = 0;
            retmsg.value = "Session not found";
            return;
        }

        Subscription subscription = subscriptionFacade.findByAgreementOrdinary(contract);
        Subscriber subscriber = subscriberFacade.find(subscription.getSubscriber().getId());

        if (email != null) {
            logMessage += "oldEmail:" + subscriber.getDetails().getEmail() + ", newEmail:" + email + "; ";
            subscriber.getDetails().setEmail(email);
        }

        if (mobile1 != null) {
            logMessage += "oldPhoneMobile:" + subscriber.getDetails().getPhoneMobile() + ", newPhoneMobile:" + mobile1 + "; ";
            subscriber.getDetails().setPhoneMobile(mobile1);
        }

        if (mobile2 != null) {
            logMessage += "oldPhoneMobileAlt:" + subscriber.getDetails().getPhoneMobileAlt() + ", newPhoneMobileAlt:" + mobile2 + "; ";
            subscriber.getDetails().setPhoneMobileAlt(mobile2);
        }

        if (landline != null) {
            logMessage += "oldPhoneLandline:" + subscriber.getDetails().getPhoneLandline() + ", newPhoneLandline:" + landline + "; ";
            subscriber.getDetails().setPhoneLandline(landline);
        }

        subscriberFacade.save(subscriber);

        retval.value = 1;
        retmsg.value = "Operation was successfull";

        systemLogger.success(SystemEvent.SOAP, subscription, logMessage);

    }

    /**
     * @param session
     * @param contract
     * @param retval
     * @param retmsg
     */
    @WebMethod
    public void sendServiceChangeSMS(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) {
        if (checkInput(session, contract)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }
        if (ExternalSessionFacade.findSubscriberSession(session) == null) {
            retval.value = 0;
            retmsg.value = "Session not found";
            return;
        }
        Subscription subscription = subscriptionFacade.findByAgreementOrdinary(contract);
        Subscriber subscriber = subscription.getSubscriber();

        if (subscriber.getRuntimeDetails() == null) {
            SubscriberRuntimeDetails runtimeDetails = new SubscriberRuntimeDetails();
            subscriber.setRuntimeDetails(runtimeDetails);
        }

        int attempt = subscriber.getRuntimeDetails().getSmsAttempt() + 1;
        if (attempt > MAX_SMS_CODE_ATTEMPTS
                && subscriber.getRuntimeDetails() != null
                && subscriber.getRuntimeDetails().getSmsCodeExpiredTime() != null
                && Minutes.minutesBetween(subscriber.getRuntimeDetails().getSmsCodeExpiredTime(), new DateTime()).getMinutes() < SMS_CODE_EXPIRATION_TIME) {
            retval.value = 5;
            retmsg.value = String.format("sms code expiration time %d minutes has not been elapsed since last unsuccesfull attempt", SMS_CODE_EXPIRATION_TIME);
            return;
        }

        if (subscription.getSubscriber().getDetails() != null) {
            String mobilePhone = subscription.getSubscriber().getDetails().getPhoneMobile();
            if (mobilePhone == null || mobilePhone.isEmpty()) {
                retval.value = 2;
                retmsg.value = "Mobile phone is not set for this user.";
                return;
            }
        }

        if (subscriber.getRuntimeDetails().isSmsSent()
                && subscriber.getRuntimeDetails().getSmsSentCount() >= MAX_SMS_SEND_COUNT
                && Minutes.minutesBetween(subscriber.getRuntimeDetails().getSmsSentTime(), new DateTime()).getMinutes() < SMS_SENT_EXPIRATION_TIME) {
            retval.value = 3;
            retmsg.value = "Sms has already been sent to subscriber's phone number";
            return;
        }

        try {
            log.info(String.format("Sending service change notification for agreement = %s", subscription.getAgreement()));
            queueManager.sendServiceChangeNotification(BillingEvent.CITYNET_SERVICE_CHANGE, subscription.getId());
            subscriber.getRuntimeDetails().setSmsSent(true);
            subscriber.getRuntimeDetails().setSmsSentTime(DateTime.now());
            int count = subscriber.getRuntimeDetails().getSmsSentCount() + 1;
            if (count > MAX_SMS_SEND_COUNT) {
                count %= MAX_SMS_SEND_COUNT;
            }
            subscriber.getRuntimeDetails().setSmsSentCount(count);
            retval.value = 1;
            retmsg.value = String.format("SMS has been sent for agreement %s. Attempt number %d.", subscription.getAgreement(), count);
        } catch (Exception e) {
            log.error(String.format("Error on sending service change notification for agreement = %s", subscription.getAgreement()));
            retval.value = 4;
            retmsg.value = String.format("Error happened during sending sms for agreement = %s", subscription.getAgreement());
        }
        subscriptionFacade.update(subscription);
        subscriberFacade.update(subscriber);
    }

    /**
     * @param session
     * @param contract
     * @param smsCode
     * @param retval
     * @param retmsg
     */
    @WebMethod
    public void checkSmsCode(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "smsCode") String smsCode,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) {
        if (checkInput(session, contract, smsCode)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        if (ExternalSessionFacade.findSubscriberSession(session) == null) {
            retval.value = 0;
            retmsg.value = "Session not found";
            return;
        }
        Subscription subscription = subscriptionFacade.findByAgreementOrdinary(contract);
        Subscriber subscriber = subscription.getSubscriber();
        if (subscriber.getRuntimeDetails() == null
                || subscriber.getRuntimeDetails().getSmsCode() == null) {
            retval.value = 10;
            retmsg.value = "Sms code not found";
        }
        int attempt = subscriber.getRuntimeDetails().getSmsAttempt() + 1;
        if (attempt > MAX_SMS_CODE_ATTEMPTS
                && Minutes.minutesBetween(subscriber.getRuntimeDetails().getSmsCodeExpiredTime(), new DateTime()).getMinutes() < SMS_CODE_EXPIRATION_TIME) {
            retval.value = 3;
            retmsg.value = String.format("sms code expiration time %d minutes has not been elapsed since last unsuccesfull attempt", SMS_CODE_EXPIRATION_TIME);
            return;
        }
        String persistedCode = subscriber.getRuntimeDetails().getSmsCode();

        if (smsCode.equals(persistedCode)) {
            subscriber.getRuntimeDetails().setSmsAttempt(0);
            retval.value = 1;
            retmsg.value = "Sms confirmed";
        } else {
            if (attempt > MAX_SMS_CODE_ATTEMPTS) {
                attempt %= MAX_SMS_CODE_ATTEMPTS;
            } else if (attempt == MAX_SMS_CODE_ATTEMPTS) {
                subscriber.getRuntimeDetails().setSmsCodeExpiredTime(DateTime.now());
            }
            subscriber.getRuntimeDetails().setSmsAttempt(attempt);
            retval.value = 2;
            retmsg.value = String.format("Wrong sms code. Attempt number %d", attempt);
        }
        subscriptionFacade.update(subscription);
    }

    /**
     * @param session
     * @param contract
     * @param retval
     * @param retmsg
     */
    @WebMethod
    public void isBlockedForTariffChange(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) {
        if (checkInput(session, contract)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        if (ExternalSessionFacade.findSubscriberSession(session) == null) {
            retval.value = 0;
            retmsg.value = "Session not found";
            return;
        }
        Subscription subscription = subscriptionFacade.findByAgreementOrdinary(contract);
        Subscriber subscriber = subscription.getSubscriber();
        if (subscriber.getRuntimeDetails() != null) {
            if (subscriber.getRuntimeDetails().getSmsAttempt() >= MAX_SMS_CODE_ATTEMPTS
                    && subscriber.getRuntimeDetails().getSmsCodeExpiredTime() != null
                    && Minutes.minutesBetween(subscriber.getRuntimeDetails().getSmsCodeExpiredTime(), new DateTime()).getMinutes() < SMS_CODE_EXPIRATION_TIME) {
                retval.value = 2;
                retmsg.value = String.format("sms code expiration time %d minutes has not been elapsed since last unsuccesfull attempt", SMS_CODE_EXPIRATION_TIME);
                return;
            }

            if (subscriber.getRuntimeDetails().isSmsSent()
                    && subscriber.getRuntimeDetails().getSmsSentCount() >= MAX_SMS_SEND_COUNT
                    && subscriber.getRuntimeDetails().getSmsSentTime() != null
                    && Minutes.minutesBetween(subscriber.getRuntimeDetails().getSmsSentTime(), new DateTime()).getMinutes() < SMS_SENT_EXPIRATION_TIME) {
                retval.value = 3;
                retmsg.value = "Sms has already been sent to subscriber's phone number";
                return;
            }
        }
        retval.value = 1;
        retmsg.value = "Subscription is eligible for tariff change.";
    }

    /**
     * @param session
     * @param contract
     * @param desc
     * @param retval
     * @param retmsg
     */
    @WebMethod
    public void updateSubscriptionDescription(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "desc") String desc,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) {
        if (checkInput(session, contract, desc)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }
        if (ExternalSessionFacade.findSubscriberSession(session) == null) {
            retval.value = 0;
            retmsg.value = "Session not found";
            return;
        }
        Subscription subscription = subscriptionFacade.findByAgreementOrdinary(contract);
        subscription.getDetails().setDesc(desc);
        subscriptionFacade.update(subscription);

        retval.value = 1;
        retmsg.value = "Subscription description has been updated";
    }

    /**
     * @param session
     * @param contract
     * @param retval
     * @param retmsg
     */
    @WebMethod
    public void hasRegisteredCampaigns(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) {
        if (checkInput(session, contract)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        if (ExternalSessionFacade.findSubscriberSession(session) == null) {
            retval.value = 0;
            retmsg.value = "Session not found";
            return;
        }

        Subscription subscription = subscriptionFacade.findByAgreementOrdinary(contract);
        if (subscription.getCampaignRegisters() != null &&
                !subscription.getCampaignRegisters().isEmpty()) {
            retval.value = 1;
            retmsg.value = "Subscriber has several campaigns";
        } else {
            retval.value = 2;
            retmsg.value = "Subscriber does not have campaigns";
        }
    }

    /**
     * @param contract
     * @param session
     * @param retval
     * @param retmsg
     */
    @WebMethod
    public void isEligibleForServiceChange(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) {
        if (checkInput(session, contract)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        if (ExternalSessionFacade.findSubscriberSession(session) == null) {
            retval.value = 0;
            retmsg.value = "Session not found";
            return;
        }

        Subscription subscription = subscriptionFacade.findByAgreementOrdinary(contract);
        if (subscription.getActivationDate() != null &&
                Months.monthsBetween(subscription.getActivationDate(), DateTime.now()).getMonths() < 2) {
            retval.value = 2;
            retmsg.value = String.format("Contract number: %s is not eligible for service change", subscription.getAgreement());
        } else {
            retval.value = 1;
            retmsg.value = String.format("Contract number: %s is eligible for service change", subscription.getAgreement());
        }
    }

    /**
     * @param contract
     * @param session
     * @param serviceID
     * @param language
     * @param statusID
     * @param retval
     * @param retmsg
     */
    @WebMethod
    public void updateSubscription(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "Service") String serviceID,
            @WebParam(name = "Language") String language,
            @WebParam(name = "Status") String statusID,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) {

        String logMessage = "updateSubscription. ";

        if (checkInput(session, contract)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        if (ExternalSessionFacade.findSubscriberSession(session) == null) {
            retval.value = 0;
            retmsg.value = "Session not found";
            return;
        }

        Subscription subscription = subscriptionFacade.findByAgreementOrdinary(contract);

        if (language != null) {

            Language lang = null;

            switch (language) {
                case "ENG":
                    lang = Language.ENGLISH;
                    break;
                case "RUS":
                    lang = Language.RUSSIAN;
                    break;
                case "AZE":
                    lang = Language.AZERI;
                    break;
            }

            logMessage += "oldLanguage:" + subscription.getDetails().getLanguage() + ", newLanguage:" + lang + "; ";
            subscription.getDetails().setLanguage(lang);
        }

        if (statusID != null) {

            SubscriptionStatus status = null;
            StatusChangeRule statusChangeRule;
            DateTime tmp = DateTime.now();
            Date statusChangeStartDate = tmp.toDate();
            Date statusChangeEndDate = tmp.plusMonths(1).toDate();

            switch (Integer.parseInt(statusID)) {
                case 99:
                    status = SubscriptionStatus.BLOCKED;
                    break;
                case 9:
                    status = SubscriptionStatus.PARTIALLY_BLOCKED;
                    break;
                case 1:
                    status = SubscriptionStatus.SUSPENDED;
                    break;
                case 0:
                    status = SubscriptionStatus.ACTIVE;
                    break;
                case 6:
                    status = SubscriptionStatus.INITIAL;
                    break;
                case 999:
                    status = SubscriptionStatus.FINAL;
                    break;

            }

            if (subscription.getService()
                    .getStatusChangeRules() != null) {

                if (status == SubscriptionStatus.BLOCKED) {
                    retval.value = 0;
                    retmsg.value = "This status is not available for manual assignation";
                    return;
                }

                if (status == SubscriptionStatus.FINAL) {

                    if (subscription.getBalance().getRealBalance() < 0) {
                        retval.value = 0;
                        retmsg.value = "Subscription cannot be FINALIZED before all debt is paid up";
                        return;
                    } else {
                        try {
                            subscriptionFacade.finalizeSubscription(subscription);
                        } catch (Exception ex) {
                            retval.value = 0;
                            retmsg.value = "Cannot finalize subscription";
                            log.error(String.format("onChangeStatus: %s id=%d", "Cannot finalize subscription", subscription.getId()), ex);
                            return;

                        }
                    }

                }

                statusChangeRule = subscription.getService().findRule(subscription.getStatus(), status);

                if (statusChangeRule != null) {
                    try {

                        subscription = subscriptionFacade.addChangeStatusJob(subscription, status, new DateTime(statusChangeStartDate), new DateTime(statusChangeEndDate));

                        logMessage += "oldStatus:" + subscription.getStatus() + ", newStatus:" + status + "; ";

                    } catch (Exception ex) {
                        retval.value = 0;
                        retmsg.value = "Cannot execute operation";
                        log.error("changeStatus: " + "Cannot execute operation", ex);
                        return;
                    }
                } else {
                    String msg = String.format("No rule found for transition from %s to %s status", subscription.getStatus(), status);
                    retval.value = 0;
                    retmsg.value = msg;
                    log.error("changeStatus: " + msg);
                    return;
                }
            } else {
                String msg = String.format("No rules found for service %s (%s)", subscription.getService().getName(), subscription.getService().getId());
                retval.value = 0;
                retmsg.value = msg;
                log.error("changeStatus: " + msg);
                return;
            }
        }

        if (serviceID != null) {
            Service service = serviceFacade.find(Long.parseLong(serviceID));
            logMessage += "oldService:" + subscription.getService().getName() + "(" + subscription.getService().getId() + ")" + ", newService:" + service.getName() + "(" + service.getId() + ")" + "; ";
            OperationsEngine operationsEngine = engineFactory.getOperationsEngine(subscription);
            subscription = operationsEngine.changeService(subscription, service, true, true);
            subscription.getSubscriber().getRuntimeDetails().setSmsSent(false);
            subscription.getSubscriber().getRuntimeDetails().setSmsSentCount(0);
            subscription.getSubscriber().getRuntimeDetails().setSmsCode(SubscriberRuntimeDetails.DEFAULT_SMS_CODE);
            subscription.getSubscriber().getRuntimeDetails().setSmsAttempt(0);
            //subscription.getSubscriber().setRuntimeDetails(new SubscriberRuntimeDetails());
        }

        subscriptionFacade.save(subscription);
        subscriberFacade.update(subscription.getSubscriber());

        retval.value = 1;
        retmsg.value = "Operation was successfull";

        systemLogger.success(SystemEvent.SOAP, subscription, logMessage);

    }

    private List<NotificationSettingRow> parseUpdateNotificationString(String updateString) throws ParseErrorException {
        //Example Input:
        //SUBSCRIPTION_ADDED:EMAIL,SMS,SCREEN;STATUS_CHANGED:SMS new format

        List<NotificationSettingRow> NotificationSettings = new ArrayList<>();
        NotificationSettingRow NotificationRow;
        String[] notifications = updateString.split(";");
        String event;
        String[] channels;
        BillingEvent billingEvent = null;
        List<String> selectedList;
        List<NotificationChannell> chanelList = Arrays.asList(NotificationChannell.values());

        for (String notif : notifications) {
            event = notif.split(":")[0];
            try {
                channels = notif.split(":")[1].split(",");
                selectedList = new ArrayList<>();
                for (String s : channels) {
                    selectedList.add(NotificationChannell.valueOf(s).getCode());
                }
            } catch (ArrayIndexOutOfBoundsException a) {
                selectedList = new ArrayList<>();
            }

            billingEvent = BillingEvent.valueOf(event);
            if (billingEvent == null) {
                throw new ParseErrorException("Cannot parse!");
            }

            NotificationRow = new NotificationSettingRow(billingEvent, chanelList);
            NotificationRow.setSelectedChannelList(selectedList);

            NotificationSettings.add(NotificationRow);
        }

        return NotificationSettings;

    }

    /**
     * @param contract
     * @param session
     * @param updateString
     * @param retval
     * @param retmsg
     * @throws com.jaravir.tekila.provision.broadband.devices.exception.NoFreePortLeftException
     * @throws com.jaravir.tekila.provision.broadband.devices.exception.PortAlreadyReservedException
     * @throws com.jaravir.tekila.provision.exception.ProvisioningException
     */
    @WebMethod
    public void updateNotifications(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "updateString") String updateString,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) throws NoFreePortLeftException, PortAlreadyReservedException, ProvisioningException {

        String logMessage = "updateNotifications. Notifications updated: " + updateString;

        List<NotificationSettingRow> notificationSettings;

        try {
            notificationSettings = parseUpdateNotificationString(updateString);
        } catch (ParseErrorException ex) {
            retval.value = 0;
            retmsg.value = "Wrong updateString format! Must be like PYMT:sms,email;INV:email;SUB_ADD:sms;VAS_ADD:;PASS:;STATUS:;SUB_SOON_EXP:;";
            return;
        }

        log.debug(String.format("updateNotifications: notificationSettings=%s", notificationSettings));

        if (checkInput(session, contract)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        if (ExternalSessionFacade.findSubscriberSession(session)
                == null) {
            retval.value = 0;
            retmsg.value = "Session not found";
            return;
        }

        Subscription subscription = subscriptionFacade.findByAgreementOrdinary(contract);
        subscription = subscriptionFacade.update(subscription, notificationSettings, null, null);

        retval.value = 1;
        retmsg.value = "Operation was successfull";

        systemLogger.success(SystemEvent.SOAP, subscription, logMessage);

    }

    /**
     * @param contract
     * @param session
     * @param eventList
     * @param retval
     * @param retmsg
     */
    @WebMethod
    public void getNotifications(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "eventList", mode = OUT) Holder<List<BillingEventResponse>> eventList,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) {
        log.debug(String.format("getNotifications called for subscription %s", contract));

        if (checkInput(session, contract)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        if (ExternalSessionFacade.findSubscriberSession(session) == null) {
            retval.value = 0;
            retmsg.value = "Session not found";
            return;
        }

        Subscription subscription = subscriptionFacade.findByAgreementOrdinary(contract);
        EnumMap<BillingEvent, List<NotificationChannell>> eventToChannels =
                new EnumMap<>(BillingEvent.class);


        List<NotificationSetting> settingsVar = subscription.getNotificationSettings();

        for (NotificationSetting setting : subscription.getNotificationSettings()) {
            if (setting.getChannelList() != null && !setting.getChannelList().isEmpty()) {
                eventToChannels.put(setting.getEvent(), setting.getChannelList());
            }
        }

        List<BillingEventResponse> eventResponseList = new ArrayList<>();
        for (BillingEvent event : BillingEvent.values()) {
            List<NotificationChannell> eventChannels = eventToChannels.get(event);
            List<NotificationChannelResponse> channelResponseList = new ArrayList<>();
            for (NotificationChannell channell : NotificationChannell.values()) {
                boolean enabled = false;
                if (eventChannels != null && eventChannels.indexOf(channell) >= 0) { //enabled
                    enabled = true;
                }
                channelResponseList.add(new NotificationChannelResponse(channell, enabled));
            }
            eventResponseList.add(new BillingEventResponse(event, channelResponseList));
        }

        eventList.value = eventResponseList;
        retval.value = 1;
        retmsg.value = "Operation was successfull";
    }

    /**
     * @param contract
     * @param session
     * @param vasID
     * @param retval
     * @param retmsg
     */
    @WebMethod
    public void addSubscriptionVas(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "vas") String vasID,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) {

        String logMessage = "addSubscriptionVas. ";

        if (checkInput(session, contract)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        if (ExternalSessionFacade.findSubscriberSession(session) == null) {
            retval.value = 0;
            retmsg.value = "Session not found";
            return;
        }

        Subscription subscription = subscriptionFacade.findByAgreementOrdinary(contract);
        ValueAddedService vas = vasFacade.find(Long.parseLong(vasID));

        if (!subscription.getService().getAllowedVASList().contains(vas)) {
            retval.value = 0;
            retmsg.value = "Service " + vas.getName() + "(" + vas.getId() + ") cannot be added";
            return;
        }

        subscription = engineFactory.getOperationsEngine(subscription).addVAS(
                new VASCreationParams.Builder().
                        setSubscription(subscription).
                        setValueAddedService(vas).
                        build()
        );
        subscriptionFacade.save(subscription);

        retval.value = 1;
        retmsg.value = "Operation was successfull";
        logMessage += "Service " + vas.getName() + "(" + vas.getId() + ") added";
        systemLogger.success(SystemEvent.SOAP, subscription, logMessage);

    }

    /**
     * @param contract
     * @param session
     * @param vasID
     * @param retval
     * @param retmsg
     */
    @WebMethod
    public void removeSubscriptionVas(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "vas") String vasID,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) {

        String logMessage = "addSubscriptionVas. ";

        if (checkInput(session, contract)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        if (ExternalSessionFacade.findSubscriberSession(session) == null) {
            retval.value = 0;
            retmsg.value = "Session not found";
            return;
        }

        Subscription subscription = subscriptionFacade.findByAgreementOrdinary(contract);
        ValueAddedService vas = vasFacade.find(Long.parseLong(vasID));

        subscription = engineFactory.getOperationsEngine(subscription).removeVAS(subscription, vas);
        subscriptionFacade.save(subscription);

        retval.value = 1;
        retmsg.value = "Operation was successfull";
        logMessage += "Service " + vas.getName() + "(" + vas.getId() + ") added";
        systemLogger.success(SystemEvent.SOAP, subscription, logMessage);

    }

    /**
     * @param contract
     * @param session
     * @param campaignID
     * @param retval
     * @param retmsg
     */
    @WebMethod
    public void addSubscriptionCampaign(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") String contract,
            @WebParam(name = "campaign") String campaignID,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) {

        String logMessage = "addSubscriptionCampaign. ";

        if (checkInput(session, contract)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        if (ExternalSessionFacade.findSubscriberSession(session) == null) {
            retval.value = 0;
            retmsg.value = "Session not found";
            return;
        }

        Subscription subscription = subscriptionFacade.findByAgreementOrdinary(contract);
        Campaign campaign = campaignFacade.find(Long.parseLong(campaignID));

        campaignJoinerBean.tryAddToCampaigns(subscription, campaign, true, false, null);

        retval.value = 1;
        retmsg.value = "Operation was successfull";
        logMessage += "campaign " + campaign.getName() + "(" + campaign.getId() + ") added";
        systemLogger.success(SystemEvent.SOAP, subscription, logMessage);

    }

    /**
     * @param session
     * @param contractFrom
     * @param contractTo
     * @param amount
     * @param retval
     * @param retmsg
     * @throws java.lang.Exception
     */
    @WebMethod
    public void transferBalance(
            @WebParam(name = "session") String session,
            @WebParam(name = "contractFrom") String contractFrom,
            @WebParam(name = "contractTo") String contractTo,
            @WebParam(name = "amount") int amount,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) throws Exception {

        String logMessage = "transferBalance. ";

        if (checkInput(session, contractFrom, contractTo)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        if (ExternalSessionFacade.findSubscriberSession(session) == null) {
            retval.value = 0;
            retmsg.value = "Session not found";
            return;
        }

        Subscription subscriptionFrom = subscriptionFacade.findByAgreementOrdinary(contractFrom);
        Subscription subscriptionTo = subscriptionFacade.findByAgreementOrdinary(contractTo);

        if (!subscriptionFrom.getSubscriber().getSubscriptions().contains(subscriptionTo)) {
            retval.value = 0;
            retmsg.value = "Transfer balance from " + contractFrom + " to " + contractTo + " is not allowed!";
            return;
        }

        if (subscriptionFrom.getBalance().getRealBalance() <= 0) {
            retval.value = 0;
            retmsg.value = "Subscription " + contractFrom + " does not have enough balance";
            return;
        }

        subscriptionFacade.transferBalanceForFinance(subscriptionFrom.getId(), subscriptionTo.getId());
        logMessage += "The Balance of " + subscriptionFrom.getAgreement() + " has been tranfered to " + subscriptionTo.getAgreement();

        systemLogger.success(SystemEvent.SOAP, subscriptionFrom, logMessage);
    }

    @WebMethod
    public long scratchCardMakePayment(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") final String contract,
            @WebParam(name = "extUserID") final long extUserID,
            @WebParam(name = "pin") final String pin,
            @WebParam(name = "serial") final long serialID,
            @WebParam(name = "retval", mode = OUT) Holder<Long> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) throws Exception {

        if (checkInput(session, contract, pin, serialID)) {
            retval.value = 0L;
            retmsg.value = "Please provide all parameters";
            return -1;
        }

        if (ExternalSessionFacade.findGatewaySession(session) == null) {
            retval.value = 0L;
            retmsg.value = "Session not found";
            return -1;
        }

        Subscription subscription = subscriptionFacade.findByAgreementOrdinary(contract);
        String res = "";

        if (subscription == null) {
            retval.value = 0L;
            retmsg.value = "The contract doesn't exist";
            return -1;
        }

        ScratchCardSession scratchCardSession;
        List<ScratchCardSession> sessions = scratchCardFacade.getSessionList(subscription);

        if (sessions != null && sessions.size() > 0) {
            scratchCardSession = sessions.get(0);
            if (scratchCardSession.getIsBlocked()) {
                retval.value = 0L;
                retmsg.value = "The subscription is blocked";
                return -1;
            }
        }

        Serial serial = serialFacade.find(serialID);

        if (!scratchCardFacade.checkSerial(serial)) {
            scratchCardSession = new ScratchCardSession();
            if (sessions != null && sessions.size() + 1 == adminSettingFacade.getBlockingSetting().getMaxAttemptCount()) {
                scratchCardSession.setIsBlocked(true);
                retval.value = 0L;
                retmsg.value = "3 wrong attempts, The subscription has been blocked for 1 hour";
            } else {
                scratchCardSession.setIsBlocked(false);
                retval.value = 0L;
                retmsg.value = "Wrong serial number";
            }

            sessionFacade.create(scratchCardSession, null, null, subscription, null, serialID, 1, subscription.getAgreement(), null);
            return -1;
        }

        ScratchCard scratchCard;
        scratchCard = scratchCardFacade.findScratchCardByPin(encryptorAndDecryptor.encrypt(pin));
        if (scratchCard == null) {

            scratchCardSession = new ScratchCardSession();
            if (sessions != null && sessions.size() + 1 >= adminSettingFacade.getBlockingSetting().getMaxAttemptCount()) {
                scratchCardSession.setIsBlocked(true);
                retval.value = 0L;
                retmsg.value = "3 wrong attempts, The subscription has been blocked for 1 hour";
            } else {
                scratchCardSession.setIsBlocked(false);
                retval.value = 0L;
                retmsg.value = "Wrong pin";
            }

            sessionFacade.create(scratchCardSession, null, null, subscription, encryptorAndDecryptor.encrypt(pin), 0, 1, subscription.getAgreement(), null);
            return -1;
        }


        scratchCard = scratchCardFacade.find(scratchCard.getId());
        scratchCard.setStatus(1);

        log.debug("scratchCard: " + scratchCard.getId());

        scratchCardFacade.update(scratchCard);

        log.debug("pin: " + encryptorAndDecryptor.decrypt(scratchCard.getPin()));

        Payment payment = new Payment();
        double amount = scratchCard.getAmount();
        try {
            payment.setMethod(paymentOptionsFacade.getOptionByName("SCRATCHCARD"));
            payment = paymentFacade.create(payment, subscription, amount, extUserID);
        } catch (Exception ex) {
            log.debug("cannot create payment");
            retval.value = 0L;
            retmsg.value = "Cannot make payment";
            return -1;
        }
        log.debug("after payment facade: " + payment);

        scratchCardSession = new ScratchCardSession();
        scratchCardSession.setIsBlocked(false);
        sessionFacade.create(scratchCardSession, payment, scratchCard, subscription, null, 0, 0, subscription.getAgreement(), scratchCard.getSerial().getId());

        amount = payment.getAmount();

        log.debug("make payment successful: " + amount);

        retval.value = payment.getId();
        retmsg.value = "Successful payment";

        return payment.getId();
    }

    @WebMethod
    public void scratchCardSettlePayment(
            @WebParam(name = "session") String session,
            @WebParam(name = "contract") final String contract,
            @WebParam(name = "paymentID") final long paymentID,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) throws Exception {

        if (checkInput(session, contract, paymentID)) {
            retval.value = 0;
            retmsg.value = "Please provide all parameters";
            return;
        }

        if (ExternalSessionFacade.findGatewaySession(session) == null) {
            retval.value = 0;
            retmsg.value = "Session not found";
            return;
        }

        Subscription subscription = subscriptionFacade.findByAgreementOrdinary(contract);
        String res = "";

        if (subscription == null) {
            retval.value = 0;
            retmsg.value = "The contract doesn't exist";
            return;
        }

        Payment payment = paymentFacade.find(paymentID);

        if (payment == null) {
            retval.value = 0;
            retmsg.value = "The payment doesn't exist";
            return;
        }

        boolean result = false;
        try {
            result = serviceProvider.settlePayment(subscription.getSubscriber().getId(), subscription.getId(), payment.getAmount(), paymentID);
        } catch (Exception ex) {
            log.debug("cannot settle payment: " + payment);
            retval.value = 0;
            retmsg.value = "Cannot settle payment: " + payment.getId();
            return;
        }
        if (!result) {
            return;
        } else {
            try {
                campaignJoinerBean.tryAddToCampaign(subscription.getId(), payment.getId(), false);
            } catch (Exception ex) {
                log.error(String.format("settlePayment: error while searching for campaign, subscription id = %d, payment id=%d",
                        subscription.getId(), payment.getId()), ex);
            }

            try {
                campaignRegisterFacade.tryActivateCampaign(subscription.getId(), payment.getId());
            } catch (Exception ex) {
                log.error(String.format("settlePayment: error while searching for campaigns awaiting activation, subscription id = %d, payment id=%d", subscription.getId(), payment.getId()), ex);
            }
        }

        retval.value = 1;
        retmsg.value = "Operation is successfull";
    }

    @WebMethod
    public String Encryptor(
            @WebParam(name = "session") String session,
            @WebParam(name = "pin") final String pin,
            @WebParam(name = "retval", mode = OUT) Holder<Integer> retval,
            @WebParam(name = "retmsg", mode = OUT) Holder<String> retmsg
    ) throws Exception {
        if (ExternalSessionFacade.findGatewaySession(session) == null) {
            retval.value = 0;
            retmsg.value = "Session not found";
            return null;
        }
        return encryptorAndDecryptor.encrypt(pin);
    }
}
