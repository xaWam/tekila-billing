package spring.service;


import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.module.accounting.entity.Payment;
import com.jaravir.tekila.module.accounting.entity.PaymentOption;
import com.jaravir.tekila.module.accounting.manager.PaymentPersistenceFacade;
import com.jaravir.tekila.module.payment.PaymentOptionsPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.web.service.exception.NoInvoiceFoundException;
import com.jaravir.tekila.module.web.service.exception.NoSuchSubscriberException;
import com.jaravir.tekila.module.web.service.exception.NoSuchSubscriptionException;
import com.jaravir.tekila.module.web.service.provider.BillingServiceProvider;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import spring.dto.ManualPaymentDTO;
import spring.security.SecurityModuleUtils;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static spring.util.Constants.INJECTION_POINT;

/**
 * @author ShakirG on 2/4/2019
 */

@Service
public class PaymentService {


    @EJB(mappedName = INJECTION_POINT +"PaymentOptionsPersistenceFacade")
    private PaymentOptionsPersistenceFacade paymentOptionsPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT +"PaymentPersistenceFacade")
    private PaymentPersistenceFacade paymentPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT +"SubscriptionPersistenceFacade")
    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT +"UserPersistenceFacade")
    private UserPersistenceFacade userPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT +"BillingServiceProvider")
    private BillingServiceProvider billingGateway;


    private static final Logger log = Logger.getLogger(PaymentService.class);

  public void doPayment(ManualPaymentDTO manualPaymentDTO){
      System.out.println("fdfdfd "+manualPaymentDTO.toString());
      Subscription subscription = subscriptionPersistenceFacade.findSubscriptionByAgreement(manualPaymentDTO.getAgreementId());

            Payment payment = new Payment();
            payment.setChequeID(manualPaymentDTO.getChequeId());
            payment.setSubscriber_id(subscription.getSubscriber().getId());
            payment.setAccount(subscription);
            payment.setContract(manualPaymentDTO.getAgreementId());
            payment.setAmount(manualPaymentDTO.getAmount());
            payment.setFd(DateTime.now().toDate());
            payment.setDsc(manualPaymentDTO.getDesc());
            payment.setInternalDsc(manualPaymentDTO.getInternalDesc());
            PaymentOption paymentOption = paymentOptionsPersistenceFacade.getOptionByName(manualPaymentDTO.getMethodName());
            payment.setMethod(paymentOption);
log.debug("Payment is "+payment.toString());
      String currentWorkingUserOnThreadLocal = SecurityModuleUtils.getCurrentUserLogin();

      log.debug("security module "+currentWorkingUserOnThreadLocal.toString());

      User user = userPersistenceFacade.findByUserName(currentWorkingUserOnThreadLocal);


payment = paymentPersistenceFacade.create(payment, subscription, manualPaymentDTO.getAmount(), user);

      boolean result = false;
      try {
          result = billingGateway.settlePayment(payment.getSubscriber_id(), payment.getAccount().getId(), payment.getAmount(), payment.getId(), user.getUserName());
      } catch (NoSuchSubscriberException e) {
log.debug("Subscriber not found "+e);
      } catch (NoSuchSubscriptionException e) {
          log.debug("subscription not found "+e);
      } catch (NoInvoiceFoundException e) {
          log.debug("Invoice not found "+e);
      }

  }



}
