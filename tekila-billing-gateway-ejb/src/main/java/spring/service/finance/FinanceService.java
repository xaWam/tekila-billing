package spring.service.finance;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.module.accounting.entity.Charge;
import com.jaravir.tekila.module.accounting.entity.Invoice;
import com.jaravir.tekila.module.accounting.entity.Payment;
import com.jaravir.tekila.module.accounting.manager.ChargePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.InvoicePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.PaymentPersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.TransactionPersistenceFacade;
import com.jaravir.tekila.module.campaign.CampaignRegisterPersistenceFacade;
import com.jaravir.tekila.module.service.persistence.manager.ServicePersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import spring.dto.ChargeDTO;
import spring.dto.InvoiceResponse;
import spring.dto.PaymentDTO;
import spring.exceptions.BadRequestAlertException;
import spring.exceptions.CustomerOperationException;
import spring.exceptions.FinancialException;
import spring.exceptions.SubscriptionNotFoundException;
import spring.mapper.ChargeMapper;
import spring.mapper.PaymentMapper;
import spring.mapper.TransactionMapper;
import spring.security.SecurityModuleUtils;

import javax.ejb.EJB;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static spring.util.Constants.INJECTION_POINT;

/**
 * @author ElmarMa on 4/9/2018
 */

@Service
public class FinanceService {

    private final Logger log = LoggerFactory.getLogger(FinanceService.class);


    @EJB(mappedName = INJECTION_POINT + "PaymentPersistenceFacade")
    private PaymentPersistenceFacade paymentPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "ChargePersistenceFacade")
    private ChargePersistenceFacade chargePersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "TransactionPersistenceFacade")
    private TransactionPersistenceFacade transactionPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "InvoicePersistenceFacade")
    private InvoicePersistenceFacade invoicePersistenceFacade;

    @EJB(mappedName = INJECTION_POINT + "SubscriptionPersistenceFacade")
    private SubscriptionPersistenceFacade subscriptionpf;

    @EJB(mappedName = INJECTION_POINT + "CampaignRegisterPersistenceFacade")
    private CampaignRegisterPersistenceFacade campaignRegisterFacade;

    @EJB(mappedName = INJECTION_POINT+"UserPersistenceFacade")
    private UserPersistenceFacade userPersistenceFacade;

    @EJB(mappedName = INJECTION_POINT+"ServicePersistenceFacade")
    private ServicePersistenceFacade servicePersistenceFacade;

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private ChargeMapper chargeMapper;

    @Autowired
    private TransactionMapper transactionMapper;


    public PaymentDTO getPayment(Long paymentId) {

        Payment payment = paymentPersistenceFacade.find(paymentId);

        if (payment == null)
            throw new FinancialException(" can not find payment");

        return mapCustomizedFields(paymentMapper.toDto(payment), payment);
    }

    public List<PaymentDTO> getPayments(String agreement) {

        if (agreement.equals("0"))
            throw new FinancialException("wrong agreement number");

        List<Payment> payments = paymentPersistenceFacade.findAllByAgreement(agreement);
        return payments == null ? new ArrayList<>() :
                payments.stream().map(payment -> mapCustomizedFields(paymentMapper.toDto(payment), payment))
                        .collect(Collectors.toList());
    }

    public void cancelPayment(Long paymentId) {
        try {
            paymentPersistenceFacade.cancelPayment(paymentId);
        } catch (Exception ex) {
            throw new FinancialException("can not cancel payment ," + ex.getMessage(), ex);
        }
    }

    private PaymentDTO mapCustomizedFields(PaymentDTO dto, Payment payment) {
        dto.setAgreement(payment.getAccount() == null ? null : payment.getAccount().getAgreement());
        dto.setUsername(payment.getUser() == null ? null : payment.getUser().getUserName());
        dto.setServiceName(payment.getService() == null ? null : payment.getService().getName());
        dto.setMethodName(payment.getMethodForUI());
        return dto;
    }


    public List<ChargeDTO> getChargesOfSubscription(String agreement) {
        if (agreement.equals("0"))
            throw new FinancialException("wrong agreement number");
        try {
            return chargePersistenceFacade.findByAgreement(agreement).stream()
                    .map(charge -> mapCustomizedFields(chargeMapper.toDto(charge), charge)).collect(Collectors.toList());
        } catch (Exception ex) {
            throw new FinancialException("can not load charges ," + ex.getMessage());
        }
    }

    private ChargeDTO mapCustomizedFields(ChargeDTO dto, Charge payment) {
        dto.setAgreement(payment.getSubscription() == null ? null : payment.getSubscription().getAgreement());
        dto.setUsername(payment.getUser() == null ? null : payment.getUser().getUserName());
        dto.setServiceName(payment.getService() == null ? null : payment.getService().getName());
        return dto;
    }

    public void revertCharge(Long chargeId) {
        try {
            chargePersistenceFacade.cancelCharge(chargeId);
        } catch (Exception ex) {
            throw new FinancialException("can not revert charge ," + ex.getMessage(), ex);
        }
    }

    public List<PaymentDTO.TransactionDTO> getTransactionBySubscription(String agreement) {
        if (agreement.equals("0"))
            throw new FinancialException("wrong agreement number");
        try {
            return transactionPersistenceFacade.findByAgreement(agreement).stream()
                    .map(transaction -> transactionMapper.toDto(transaction))
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            throw new FinancialException("can not load transactions ," + ex.getMessage(), ex);
        }
    }

    public List<InvoiceResponse> getInvoicesBySubscription(String agreement, String startDate, String endDate) {
        if (agreement.equals("0"))
            throw new FinancialException("wrong agreement number");
        try {

            return invoicePersistenceFacade.findByAgreementAndDate(agreement, startDate, endDate).stream()
                    .map(InvoiceResponse::from).collect(Collectors.toList());
        } catch (Exception ex) {
            throw new FinancialException("can not load invoices from " + startDate + " to " + endDate + " for " + agreement
                    + " ," + ex.getMessage(), ex);
        }
    }

    public List<PaymentDTO> getPaymentsByPeriod(String agreement, String startDate, String endDate) {
        if (agreement.equals("0"))
            throw new FinancialException("wrong agreement number");

        try {
            List<Payment> payments = paymentPersistenceFacade.findAllByAgreementAndDates(agreement, startDate, endDate);
            return payments == null ? new ArrayList<>() : payments.stream()
                    .map(payment -> mapCustomizedFields(paymentMapper.toDto(payment), payment))
                    .collect(Collectors.toList());
        } catch (Exception ex){
            throw new FinancialException("can not load payments from " + startDate + " to " + endDate + " for " + agreement
                    + " ," + ex.getMessage(), ex);
        }
    }

    public List<ChargeDTO> getChargesOfSubscriptionByPeriod(String agreement, String startDate, String endDate) {
        if (agreement.equals("0"))
            throw new FinancialException("wrong agreement number");
        try {
            return chargePersistenceFacade.findByAgreement(agreement, startDate, endDate)
                    .stream()
                    .map(charge -> mapCustomizedFields(chargeMapper.toDto(charge), charge))
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            throw new FinancialException(String.format("can not load charges from %s to %s for %s -> %s", startDate, endDate, agreement, ex.getMessage()), ex);
        }
    }

    public List<PaymentDTO.TransactionDTO> getTransactionOfSubscriptionByPeriod(String agreement, String startDate, String endDate) {
        if (agreement.equals("0"))
            throw new FinancialException("wrong agreement number");

        try {
            return transactionPersistenceFacade.findByAgreementAndDates(agreement, startDate, endDate).stream()
                    .map(transaction -> transactionMapper.toDto(transaction))
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            throw new FinancialException(String.format("can not load transactions from %s to %s for %s -> %s", startDate, endDate, agreement, ex.getMessage()), ex);
        }
    }

    public void chargeSubscriptionForService(String agreement, Long serviceId, String desc){
//        Long subscriptionId = subscriptionpf.findSubscriptionIdByAgreement(agreement);
//        Subscription subscription = subscriptionpf.find(subscriptionId);

        Subscription subscription = Optional.ofNullable(subscriptionpf.findByAgreementOrdinary(agreement))
                .orElseThrow(() -> new SubscriptionNotFoundException("Subscription not found!"));

        String currentWorkingUserOnThreadLocal = Optional.ofNullable(SecurityModuleUtils.getCurrentUserLogin())
                .orElseThrow(() -> new BadRequestAlertException("Security User not found on Thread Local. May be token is not valid or empty"));

        long servicePrice = Optional.ofNullable(servicePersistenceFacade.getServiceRate(serviceId))
                .orElseThrow(() -> new FinancialException("Service not found!"));
        log.info("CSM request -> service price: {}", servicePrice);

        try {
            Invoice invoice = invoicePersistenceFacade.findOpenBySubscriberForCharging(subscription.getSubscriber().getId());
            log.debug("[CSM] Subscriber invoice: {}", invoice);

            Long rate = null;
            Double bonusDiscount = campaignRegisterFacade.getBonusDiscount(subscription);
            if (bonusDiscount != null) {
                log.info("[subscription: {}] has bonus discount: {}", subscription.getId(), bonusDiscount);
                rate = Double.valueOf(Math.ceil(servicePrice * bonusDiscount)).longValue();
            }
            if (rate == null)
                rate = servicePrice;
            rate = subscription.rerate(rate);
            log.info("CSM request -> service rate: {}", rate);

            Long userId = userPersistenceFacade.getIdByUserName(currentWorkingUserOnThreadLocal);
            invoicePersistenceFacade.addServiceChargeToInvoiceForCsm(invoice, subscription, rate, userId, desc);
        } catch (Exception ex){
            log.error("Error occurs when charge subscription, agreement: "+agreement, ex);
            throw new FinancialException(ex.getMessage(), ex);
        }
    }

}
