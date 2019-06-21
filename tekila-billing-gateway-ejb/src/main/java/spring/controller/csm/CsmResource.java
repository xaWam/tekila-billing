package spring.controller.csm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.controller.util.ErrorResponse;
import spring.dto.CsmAddVasRequest;
import spring.dto.CsmChargeRequest;
import spring.dto.CsmVasRequest;
import spring.dto.SubscriptionDTO;
import spring.service.SubscriptionService;
import spring.service.SubscriptionVasService;
import spring.service.finance.AdjustmentService;
import spring.service.finance.FinanceService;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * @author MushfigM on 5/8/2019
 */
@RestController
@RequestMapping("/csm")
public class CsmResource {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SubscriptionService subscriptionService;
    @Autowired
    private AdjustmentService adjustmentService;
    @Autowired
    private SubscriptionVasService subscriptionVasService;
    @Autowired
    private FinanceService financeService;


    @GetMapping("/subscription/{agreement}")
    public ResponseEntity<SubscriptionDTO> getSubscription(@PathVariable String agreement){
        log.debug("Rest request to get Subscription for CSM, agreement: {}", agreement);
        return ResponseEntity.ok().body(subscriptionService.getSubscription(agreement));
    }

    @PostMapping("/finance/charge")
    public ResponseEntity<ErrorResponse> doAdjustment(@Valid @RequestBody CsmChargeRequest chargeRequest) {
        log.debug("REST request to charge Subscription with amount: {}", chargeRequest);

        Long accountingCategoryId = 1L;
        String description = Optional.ofNullable(chargeRequest.getContentId()).orElse("CSM charge request for amount " + chargeRequest.getAmount());
        String balanceType = "REAL";
        String operationType = "DEBIT";

//        Long amountInLong = Double.valueOf(chargeRequest.getAmount() * 100000L).longValue();
        Long amountInLong = chargeRequest.getAmount().multiply(new BigDecimal("100000")).longValue();
        adjustmentService.doAdjustment(chargeRequest.getAgreement(),
                /*adjustment.getAccountCategoryId(),*/accountingCategoryId,
                amountInLong,
                /*adjustment.getDescription(),*/description,
                /*adjustment.getBalanceType(),*/balanceType,
                /*adjustment.getOperationType())*/operationType);

        return ResponseEntity.ok().body(new ErrorResponse("TEKILA BILLING API", 0, "ok"));
    }

    @PostMapping("/finance/service/charge")
    public ResponseEntity<ErrorResponse> chargeForService(@Valid @RequestBody CsmChargeRequest chargeRequest) {
        log.debug("REST request to charge Subscription for service: {}", chargeRequest);
        String desc = Optional.ofNullable(chargeRequest.getContentId()).orElse("CSM charge request for service id " + chargeRequest.getServiceId());
        financeService.chargeSubscriptionForService(chargeRequest.getAgreement(), chargeRequest.getServiceId(), desc);

        return ResponseEntity.ok().body(new ErrorResponse("TEKILA BILLING API", 0, "ok"));
    }

    //new
    @PostMapping("/subscription/vas")
    public ResponseEntity<ErrorResponse> modifySubscriptionVas(@Valid @RequestBody CsmVasRequest vasRequest){
        log.info("Rest request to modify CSM SubscriptionVAS : {}", vasRequest);
        subscriptionVasService.modifyCsmVas(vasRequest);
        return ResponseEntity.ok().body(new ErrorResponse("TEKILA BILLING API", 0, "ok"));
    }

    //****************************************************************************************************************//
    @PostMapping("/subscriptionVas/add")
    public ResponseEntity<Void> addSubscriptionVas(@RequestBody CsmAddVasRequest vasRequest){
        log.info("Rest request to add CSM SubscriptionVAS : {}", vasRequest);
        subscriptionVasService.addCsmVas(vasRequest);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/subscriptionVas/{agreement}/{vasId}")
    public ResponseEntity<Void> deleteSubscriptionVas(@PathVariable String agreement, @PathVariable Long vasId){
        log.info("Rest request to delete CSM SubscriptionVas, agreement : {}, vas id: {}", agreement, vasId);
        subscriptionVasService.delete(agreement, vasId);
        return ResponseEntity.ok().build();
    }

}

