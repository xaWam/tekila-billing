package spring.service.finance;


import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.module.accounting.entity.*;
import com.jaravir.tekila.module.accounting.manager.InvoicePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.PaymentPersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.TransactionPersistenceFacade;
import com.jaravir.tekila.module.campaign.CampaignJoinerBean;
import com.jaravir.tekila.module.payment.PaymentOptionsPersistenceFacade;
import com.jaravir.tekila.module.service.ServiceSettingType;
import com.jaravir.tekila.module.subscription.exception.DuplicateAgreementException;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.module.web.service.exception.NoInvoiceFoundException;
import com.jaravir.tekila.module.web.service.exception.NoSuchSubscriberException;
import com.jaravir.tekila.module.web.service.exception.NoSuchSubscriptionException;
import com.jaravir.tekila.module.web.service.provider.BillingServiceProvider;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import com.jaravir.tekila.provision.broadband.devices.manager.MiniPopPersistenceFacade;
import com.jaravir.tekila.provision.broadband.devices.manager.Providers;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import spring.controller.FinanceController;
import spring.dto.KapitalSubscriptionDTO;

import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import static spring.util.Constants.INJECTION_POINT;

/**
 * Created by ShakirG on 02/07/2018.
 */

@Service
public class ExternalPayment {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ExternalPayment.class);


    @EJB(mappedName = INJECTION_POINT + "TransactionPersistenceFacade")
    private TransactionPersistenceFacade transFacade;

    @EJB(mappedName = INJECTION_POINT + "PaymentPersistenceFacade")
    private PaymentPersistenceFacade paymentFacade;

    @EJB(mappedName = INJECTION_POINT + "PaymentOptionsPersistenceFacade")
    private PaymentOptionsPersistenceFacade paymentOptionsPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "UserPersistenceFacade")
    private UserPersistenceFacade userFacade;

    @EJB(mappedName = INJECTION_POINT + "SystemLogger")
    private SystemLogger systemLogger;

    @EJB(mappedName = INJECTION_POINT + "BillingServiceProvider")
    private BillingServiceProvider billingGateway;

    @EJB(mappedName = INJECTION_POINT + "CampaignJoinerBean")
    private CampaignJoinerBean campaignJoinerBean;

    @EJB(mappedName = INJECTION_POINT + "SubscriptionPersistenceFacade")
    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "InvoicePersistenceFacade")
    private InvoicePersistenceFacade invoicePersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "MiniPopPersistenceFacade")
    private MiniPopPersistenceFacade miniPopPersistenceFacade;

    User user;




    public String makeBBTVPayment(String agreement, String manats, long userID, Long extUserID, String rrn) {

        log.debug("aggre :{}, amount:{}", agreement, manats);
        Payment payment = new Payment();
        Subscription subscription = null;


        subscription = subscriptionPersistenceFacade.findByAgreementOrdinary(agreement);


        log.debug(" ds  :" + subscription.toString());
        if (subscription == null) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "No subscription selected", "Please select subscription to add cash payment");
            FacesContext.getCurrentInstance().addMessage(null, message);
            return null;
        }

        if (manats == null) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Amount is required", "Amount is required");
            FacesContext.getCurrentInstance().addMessage(null, message);
            return null;
        }

        if (manats.contains(",") || manats.contains(" ")) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Amount pattern is illegal", "Amount pattern is illegal");
            FacesContext.getCurrentInstance().addMessage(null, message);
            return null;
        }


        Double amount = null;

        try {
            amount = Double.parseDouble(manats);
        } catch (NumberFormatException ex) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Amount pattern is illegal", "Amount pattern is illegal");
            FacesContext.getCurrentInstance().addMessage(null, message);
            return null;
        }

        if (amount <= 0) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Not valid amount", "Not valid amount");
            FacesContext.getCurrentInstance().addMessage(null, message);
            return null;
        }

        if ((subscription.getService().getProvider().getId() == Providers.CITYNET.getId() ||
                subscription.getService().getProvider().getId() == Providers.UNINET.getId()) && amount > 10000.0) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Amount cannot be more than 10000 AZN", "Amount cannot be more than 10000 AZN");
            FacesContext.getCurrentInstance().addMessage(null, message);
            return null;
        }

        if ((subscription.getService().getProvider().getId() != Providers.CITYNET.getId() &&
                subscription.getService().getProvider().getId() != Providers.UNINET.getId() &&
                subscription.getService().getProvider().getId() != Providers.AZERTELECOMPOST.getId()) && amount > 250.0) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Amount cannot be more than 250 AZN", "Amount cannot be more than 250 AZN");
            FacesContext.getCurrentInstance().addMessage(null, message);
            return null;
        }

        Integer pCode = 10;


        if (pCode == null) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Payment method should be selected", "Payment method should be selected");
            FacesContext.getCurrentInstance().addMessage(null, message);
            return null;
        }

        String message = null;
        try {
//            HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
            user = userFacade.find(userID);

            payment.setMethod(paymentOptionsPersistenceFacade.find(pCode));
            payment = paymentFacade.create(amount, extUserID, subscription.getService().getId(), subscription.getId(), agreement, subscription.getSubscriber().getId(),
                    "AZN", null, rrn, null, "BBTV Payment", "");
            log.info(String.format("Payment after persist: %s. Settling....", payment));
            try{
                log.debug("Chckpoint exception "+subscription+" - "+payment.getId()+ " = "+payment.getAmount());
            }catch(Exception ex){
                log.debug("Chckpoint exception "+ex);
                return null;
            }
            systemLogger.successExP(SystemEvent.PAYMENT, subscription,
                    String.format("payment id=%d, amount=%f, bank=%s",
                            payment.getId(), payment.getAmount(), "Kapital Bank"), user != null ? user.getUserName(): null
            );

            boolean result = billingGateway.settlePayment(payment.getSubscriber_id(), payment.getAccount().getId(), payment.getAmount(), payment.getId(), user.getUserName());
            log.info(String.format("settlePayment result: %b", result));

            if (!result) {
                throw new Exception();
            } else {
                return "SUCCESS";
            }


        } catch (NoSuchSubscriberException e) {
            message = String.format("Payment %d not settled - subscriber %d not found", payment.getId(), payment.getSubscriber_id());
            log.error(message, e);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Cannot process payment - subscriber unknown", "Cannot process payment - subscriber unknown"));
            return null;
        } catch (NoSuchSubscriptionException e) {
            message = String.format("Payment %d not settled - subscription %d not found", payment.getId(), payment.getAccount().getId());
            log.error(message, e);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Cannot process payment - subscription unknown", "Cannot process payment - subscription unknown"));
            return null;
        } catch (NoInvoiceFoundException e) {
            message = String.format("Payment %d not settled - invoice not found", payment.getId());
            log.error(message, e);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Cannot process payment - invoice not found", "Cannot process payment - invoice not found"));
            return null;
        } catch (Exception e) {
            message = String.format("Payment %d not settled", payment.getId());
            log.error(message, e);
            systemLogger.errorExP(SystemEvent.PAYMENT, subscription, "amount=" + manats, user.getUserName());
//            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Cannot process payment", "Cannot process payment"));
            return null;
        }


    }








    public String makeKapitalBankPayment(String agreement, String manats, long userID) {

        log.debug("aggre :{}, amount:{}", agreement, manats);
        Payment payment = new Payment();
        Subscription subscription = null;


        subscription = subscriptionPersistenceFacade.findByAgreementOrdinary(agreement);


        log.debug(" ds  :" + subscription.toString());
        if (subscription == null) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "No subscription selected", "Please select subscription to add cash payment");
            FacesContext.getCurrentInstance().addMessage(null, message);
            return null;
        }

        if (manats == null) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Amount is required", "Amount is required");
            FacesContext.getCurrentInstance().addMessage(null, message);
            return null;
        }

        if (manats.contains(",") || manats.contains(" ")) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Amount pattern is illegal", "Amount pattern is illegal");
            FacesContext.getCurrentInstance().addMessage(null, message);
            return null;
        }


        Double amount = null;

        try {
            amount = Double.parseDouble(manats);
        } catch (NumberFormatException ex) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Amount pattern is illegal", "Amount pattern is illegal");
            FacesContext.getCurrentInstance().addMessage(null, message);
            return null;
        }

        if (amount <= 0) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Not valid amount", "Not valid amount");
            FacesContext.getCurrentInstance().addMessage(null, message);
            return null;
        }

        if ((subscription.getService().getProvider().getId() == Providers.CITYNET.getId() ||
                subscription.getService().getProvider().getId() == Providers.UNINET.getId()) && amount > 10000.0) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Amount cannot be more than 10000 AZN", "Amount cannot be more than 10000 AZN");
            FacesContext.getCurrentInstance().addMessage(null, message);
            return null;
        }

        if ((subscription.getService().getProvider().getId() != Providers.CITYNET.getId() &&
                subscription.getService().getProvider().getId() != Providers.UNINET.getId() &&
                subscription.getService().getProvider().getId() != Providers.AZERTELECOMPOST.getId()) && amount > 250.0) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Amount cannot be more than 250 AZN", "Amount cannot be more than 250 AZN");
            FacesContext.getCurrentInstance().addMessage(null, message);
            return null;
        }

        Integer pCode = 10;


        if (pCode == null) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Payment method should be selected", "Payment method should be selected");
            FacesContext.getCurrentInstance().addMessage(null, message);
            return null;
        }

        String message = null;
        try {
//            HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
            user = userFacade.find(userID);

            payment.setMethod(paymentOptionsPersistenceFacade.find(pCode));
            payment = paymentFacade.create(payment, subscription, amount, user);
            log.info(String.format("Payment after persist: %s. Settling....", payment));
            try{
                log.debug("Chckpoint exception "+subscription+" - "+payment.getId()+ " = "+payment.getAmount());
            }catch(Exception ex){
                log.debug("Chckpoint exception "+ex);
                return null;
            }
            systemLogger.successExP(SystemEvent.PAYMENT, subscription,
                    String.format("payment id=%d, amount=%f, bank=%s",
                            payment.getId(), payment.getAmount(), "Kapital Bank"), user != null ? user.getUserName(): null
            );

            boolean result = billingGateway.settlePayment(payment.getSubscriber_id(), payment.getAccount().getId(), payment.getAmount(), payment.getId(), user.getUserName());
            log.info(String.format("settlePayment result: %b", result));

            if (!result) {
                throw new Exception();
            } else {
                return "SUCCESS";
            }


        } catch (NoSuchSubscriberException e) {
            message = String.format("Payment %d not settled - subscriber %d not found", payment.getId(), payment.getSubscriber_id());
            log.error(message, e);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Cannot process payment - subscriber unknown", "Cannot process payment - subscriber unknown"));
            return null;
        } catch (NoSuchSubscriptionException e) {
            message = String.format("Payment %d not settled - subscription %d not found", payment.getId(), payment.getAccount().getId());
            log.error(message, e);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Cannot process payment - subscription unknown", "Cannot process payment - subscription unknown"));
            return null;
        } catch (NoInvoiceFoundException e) {
            message = String.format("Payment %d not settled - invoice not found", payment.getId());
            log.error(message, e);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Cannot process payment - invoice not found", "Cannot process payment - invoice not found"));
            return null;
        } catch (Exception e) {
            message = String.format("Payment %d not settled", payment.getId());
            log.error(message, e);
            systemLogger.errorExP(SystemEvent.PAYMENT, subscription, "amount=" + manats, user.getUserName());
//            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Cannot process payment", "Cannot process payment"));
            return null;
        }


    }


    public KapitalSubscriptionDTO getSubscriptionKapital(String agreement) {

        Subscription subscription = subscriptionPersistenceFacade.findSubscsforKapital(agreement);
log.info("getSubscriptionKapital "+subscription.toString());
        MiniPop miniPop = getMinipopKapital(Long.parseLong(subscription.getSettingByType(ServiceSettingType.BROADBAND_SWITCH).getValue().toString()));

        KapitalSubscriptionDTO kapitalSubscriptionDTO = new KapitalSubscriptionDTO();
        kapitalSubscriptionDTO.setAgreement(subscription.getAgreement());
        kapitalSubscriptionDTO.setName(subscription.getDetails().getName());
        kapitalSubscriptionDTO.setSurname(subscription.getDetails().getSurname());
        kapitalSubscriptionDTO.setService(subscription.getService().getName());
        kapitalSubscriptionDTO.setStatus(subscription.getStatus().name());
        kapitalSubscriptionDTO.setRegion(miniPop.getSwitch_id());


        return kapitalSubscriptionDTO;

    }


    public MiniPop getMinipopKapital(long minipop) {

        return miniPopPersistenceFacade.find(minipop);

    }
}
