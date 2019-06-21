package spring.controller;

import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.module.accounting.entity.TransactionType;
import com.jaravir.tekila.module.accounting.manager.InvoicePersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.PaymentPersistenceFacade;
import com.jaravir.tekila.module.service.ServiceSettingType;
import com.jaravir.tekila.module.store.ScratchCardPersistenceFacade;
import com.jaravir.tekila.module.store.ScratchCardSessionPersistenceFacade;
import com.jaravir.tekila.module.store.SerialPersistenceFacade;
import com.jaravir.tekila.module.store.scratchcard.persistence.entity.ScratchCard;
import com.jaravir.tekila.module.store.scratchcard.persistence.entity.ScratchCardSession;
import com.jaravir.tekila.module.store.scratchcard.persistence.entity.Serial;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionSetting;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.Filters;
import spring.dto.*;
import spring.service.PaymentService;
import spring.service.SubscriptionService;
import spring.service.finance.AdjustmentService;
import spring.service.finance.ExternalPayment;
import spring.service.finance.FinanceService;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static spring.util.Constants.INJECTION_POINT;

/**
 * Created by KamranMa on 25.12.2017.
 */
@RestController
public class FinanceController {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(FinanceController.class);

    @EJB(mappedName = INJECTION_POINT + "PaymentPersistenceFacade")
    private PaymentPersistenceFacade paymentPersistenceFacade;
    @EJB(mappedName = INJECTION_POINT + "InvoicePersistenceFacade")
    private InvoicePersistenceFacade invoicePersistenceFacade;
    @EJB(mappedName = INJECTION_POINT + "SerialPersistenceFacade")
    private SerialPersistenceFacade serialFacade;
    @EJB(mappedName = INJECTION_POINT + "ScratchCardPersistenceFacade")
    private ScratchCardPersistenceFacade scratchCardFacade;
    @EJB(mappedName = INJECTION_POINT + "ScratchCardSessionPersistenceFacade")
    private ScratchCardSessionPersistenceFacade scratchCardSessionPersistenceFacade;


    @Autowired
    private AdjustmentService adjustmentService;

    @Autowired
    private FinanceService financeService;

    @Autowired
    private ExternalPayment externalPayment;

    @Autowired
    HttpServletRequest request;

    @Autowired
    PaymentService paymentService;


    @RequestMapping(method = RequestMethod.GET, value = "/search/payments")
    public List<PaymentResponse> searchPayments(
            @RequestParam(required = false) String cheque,
            @RequestParam(required = false) String rrn,
            @RequestParam Integer pageId,
            @RequestParam(required = false, defaultValue = "10") Integer rowsPerPage) {
        Filters filters = new Filters();
        filters.clearFilters();
        log.debug("-----------search() start----------");
        log.debug(String.format("search() method. cheque = %s, rrn = %s, pageId = %d, rowsPerPage = %d",
                cheque, rrn, pageId, rowsPerPage));

        if (rrn != null && !rrn.isEmpty()) {
            PaymentPersistenceFacade.Filter rrnFilter = PaymentPersistenceFacade.Filter.RRN;
            rrnFilter.setOperation(MatchingOperation.EQUALS);
            filters.addFilter(rrnFilter, rrn);
        }

        if (cheque != null && !cheque.isEmpty()) {
            PaymentPersistenceFacade.Filter chequeFilter = PaymentPersistenceFacade.Filter.CHEQUE_ID;
            chequeFilter.setOperation(MatchingOperation.EQUALS);
            filters.addFilter(chequeFilter, cheque);
        }

        log.debug("-----------search end()----------");
        return paymentPersistenceFacade.findAllPaginated((pageId - 1) * rowsPerPage, rowsPerPage, filters).stream().
                map(PaymentResponse::from).collect(Collectors.toList());
    }


    @PostMapping("/loginKapital")
    public void loginKapital(@RequestBody UserDTO incomingUser) {
        log.debug(incomingUser.getUsername());
        log.debug(incomingUser.getPassword());
        try {
            request.login(incomingUser.getUsername().toString(), incomingUser.getPassword().toString());
            log.debug("Login success");
        } catch (ServletException e) {
            e.printStackTrace();
        }

    }

    @GetMapping("/makeBBTVPayment")
    public long makeBBTVPayment(
            @RequestParam String agreement,
            @RequestParam String amount,
            @RequestParam String rrn,
            @RequestParam Long externalUser
            ) {
        log.debug("aggre :{}, amount:{}", agreement, amount);


      String result =  externalPayment.makeBBTVPayment(agreement, amount, 34681323, externalUser, rrn);
        if (result.equals("SUCCESS")){

            return 1;
        }
log.debug("Unsuccessfully payment "+agreement);
        return 0;
    }


    @GetMapping("/makeKapitalBankPayment")
    public void makeKapitalPayment(
            @RequestParam String agreement,
            @RequestParam String amount) {
        log.debug("aggre :{}, amount:{}", agreement, amount);

        externalPayment.makeKapitalBankPayment(agreement, amount, 30721126);
    }

    @GetMapping("/getKapitalSubs")
    public KapitalSubscriptionDTO getKapitalSubsciption(
            @RequestParam String agreement) {

        KapitalSubscriptionDTO kapitalSubscriptionDTO;
        kapitalSubscriptionDTO = externalPayment.getSubscriptionKapital(agreement);

        return kapitalSubscriptionDTO;
    }

//    @GetMapping("/getKapitalSubs")
//    public Subscription getKapitalSubsciption(
//            @RequestParam String agreement) {
//
//return  externalPayment.getSubscriptionKapital(agreement);
//
//    }

    @RequestMapping(method = RequestMethod.GET, value = "/search/payments/count")
    public long countPayments(
            @RequestParam(required = false) String cheque,
            @RequestParam(required = false) String rrn
    ) {
        Filters filters = new Filters();
        filters.clearFilters();
        log.debug("-----------count() start----------");
        log.debug(String.format("count() method. cheque = %s, rrn = %s",
                cheque, rrn));

        if (rrn != null && !rrn.isEmpty()) {
            PaymentPersistenceFacade.Filter rrnFilter = PaymentPersistenceFacade.Filter.RRN;
            rrnFilter.setOperation(MatchingOperation.EQUALS);
            filters.addFilter(rrnFilter, rrn);
        }

        if (cheque != null && !cheque.isEmpty()) {
            PaymentPersistenceFacade.Filter chequeFilter = PaymentPersistenceFacade.Filter.CHEQUE_ID;
            chequeFilter.setOperation(MatchingOperation.EQUALS);
            filters.addFilter(chequeFilter, cheque);
        }

        long cnt = paymentPersistenceFacade.count(filters);
        log.debug(String.format("count() method. count = %d", cnt));
        log.debug("-----------count end()----------");
        return cnt;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/search/invoices")
    public List<InvoiceResponse> searchInvoices(
            @RequestParam(required = false) Long invoiceId,
            @RequestParam Integer pageId,
            @RequestParam(required = false, defaultValue = "10") Integer rowsPerPage) {
        Filters filters = new Filters();
        filters.clearFilters();
        log.debug("-----------search() start----------");
        log.debug(String.format("search() method. invoiceId = %d, pageId = %d, rowsPerPage = %d",
                invoiceId, pageId, rowsPerPage));

        if (invoiceId != null) {
            InvoicePersistenceFacade.Filter invoiceIdFilter = InvoicePersistenceFacade.Filter.ID;
            invoiceIdFilter.setMatchingOperation(MatchingOperation.EQUALS);
            filters.addFilter(invoiceIdFilter, invoiceId);
        }

        log.debug("-----------search end()----------");
        return invoicePersistenceFacade.findAllPaginated((pageId - 1) * rowsPerPage, rowsPerPage, filters).stream().
                map(InvoiceResponse::from).collect(Collectors.toList());
    }

    @RequestMapping(method = RequestMethod.GET, value = "/search/invoices/count")
    public long countInvoices(
            @RequestParam(required = false) Long invoiceId
    ) {
        Filters filters = new Filters();
        filters.clearFilters();
        log.debug("-----------count() start----------");
        log.debug(String.format("count() method. invoiceId = %d", invoiceId));

        if (invoiceId != null) {
            InvoicePersistenceFacade.Filter invoiceIdFilter = InvoicePersistenceFacade.Filter.ID;
            invoiceIdFilter.setMatchingOperation(MatchingOperation.EQUALS);
            filters.addFilter(invoiceIdFilter, invoiceId);
        }

        long cnt = invoicePersistenceFacade.count(filters);
        log.debug(String.format("count() method. count = %d", cnt));
        log.debug("-----------count end()----------");
        return cnt;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/search/scratchcards")
    public List<ScratchCardResponse> searchScratchCards(
            @RequestParam(required = false) Long serialId,
            @RequestParam Integer pageId,
            @RequestParam(required = false, defaultValue = "10") Integer rowsPerPage) {
        Filters filters = new Filters();
        filters.clearFilters();
        log.debug("-----------search() start----------");
        log.debug(String.format("search() method. serialId = %d, pageId = %d, rowsPerPage = %d",
                serialId, pageId, rowsPerPage));

        List<ScratchCardResponse> responseList = null;
        if (serialId != null) {
            Serial serial = serialFacade.find(serialId);
            ScratchCard scratchCard = scratchCardFacade.findBySerial(serial);
            ScratchCardSession cardSession = scratchCardFacade.findSessionByCard(scratchCard);
            if (cardSession != null) {
                responseList = Arrays.asList(ScratchCardResponse.from(cardSession));
            } else {
                responseList = new ArrayList<>();
            }
        } else {
            responseList = scratchCardSessionPersistenceFacade.findAllPaginated((pageId - 1) * rowsPerPage, rowsPerPage, new Filters())
                    .stream().map(ScratchCardResponse::from).collect(Collectors.toList());
        }

        log.debug("-----------search end()----------");
        return responseList;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/search/scratchcards/count")
    public long countScratchCards(
            @RequestParam(required = false) Long serialId
    ) {
        Filters filters = new Filters();
        filters.clearFilters();
        log.debug("-----------count() start----------");
        log.debug(String.format("count() method. serialId = %d", serialId));

        long cnt;
        if (serialId != null) {
            Serial serial = serialFacade.find(serialId);
            ScratchCard scratchCard = scratchCardFacade.findBySerial(serial);
            ScratchCardSession cardSession = scratchCardFacade.findSessionByCard(scratchCard);
            cnt = (cardSession != null) ? 1 : 0;
        } else {
            cnt = scratchCardSessionPersistenceFacade.count(new Filters());
        }
        log.debug(String.format("count() method. serialId = %d", cnt));
        log.debug("-----------count end()----------");
        return cnt;
    }


    @RequestMapping(method = RequestMethod.PUT, value = "/finance/adjust")
    public ResponseEntity<Void> doAdjustment(@RequestBody AdjustmentRequestDTO adjustment) {
        log.debug("Rest Request for adjustment: {}", adjustment);
        Long amountInLong = Double.valueOf(adjustment.getAmount() * 100000L).longValue();
        adjustmentService.doAdjustment(adjustment.getSubscriptionId(),
                adjustment.getAccountCategoryId(),
                amountInLong,
                adjustment.getDescription(),
                adjustment.getBalanceType(),
                adjustment.getOperationType());
        return ResponseEntity.ok().build();
    }

    @RequestMapping(method = RequestMethod.GET, value = "/finance/payment")
    public ResponseEntity<PaymentDTO> getPayment(@RequestParam Long pid) {
        return ResponseEntity.ok(financeService.getPayment(pid));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/finance/payments")
    public ResponseEntity<List<PaymentDTO>> getPayments(@RequestParam String agr) {
        return ResponseEntity.ok(financeService.getPayments(agr));
    }


    @RequestMapping(method = RequestMethod.PUT, value = "/finance/payment/cancel")
    public ResponseEntity<Void> getPayments(@RequestParam Long pid) {
        financeService.cancelPayment(pid);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/finance/payment/makepayment")
    public void makePayment(@RequestBody ManualPaymentDTO manualPaymentDTO) {
log.debug("started");
        paymentService.doPayment(manualPaymentDTO);

    }

    @RequestMapping(method = RequestMethod.GET, value = "/finance/charges")
    public ResponseEntity<List<ChargeDTO>> getChargesOfSubscription(@RequestParam String agr) {
        return ResponseEntity.ok(financeService.getChargesOfSubscription(agr));
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/finance/charge/cancel")
    public ResponseEntity<Void> revertCharge(@RequestParam Long cid) {
        financeService.revertCharge(cid);
        return ResponseEntity.ok().build();
    }


    @RequestMapping(method = RequestMethod.GET, value = "/finance/transactions")
    public ResponseEntity<List<PaymentDTO.TransactionDTO>> getTransactionsOfSubscription(@RequestParam String agr) {
        return ResponseEntity.ok(financeService.getTransactionBySubscription(agr));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/finance/invoices")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesOfSubscription(@RequestParam String agr, @RequestParam String start, @RequestParam String end) {
        return ResponseEntity.ok(financeService.getInvoicesBySubscription(agr, start, end));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/finance/payments/period")
    public ResponseEntity<List<PaymentDTO>> getPaymentsByPeriod(@RequestParam String agr, @RequestParam String start, @RequestParam String end) {
        return ResponseEntity.ok(financeService.getPaymentsByPeriod(agr, start, end));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/finance/charges/period")
    public ResponseEntity<List<ChargeDTO>> getChargesOfSubscriptionByPeriod(@RequestParam String agr, @RequestParam String start, @RequestParam String end) {
        return ResponseEntity.ok(financeService.getChargesOfSubscriptionByPeriod(agr, start, end));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/finance/transactions/period")
    public ResponseEntity<List<PaymentDTO.TransactionDTO>> getTransactionsOfSubscriptionByPeriod(@RequestParam String agr, @RequestParam String start, @RequestParam String end) {
        return ResponseEntity.ok(financeService.getTransactionOfSubscriptionByPeriod(agr, start, end));
    }


}
