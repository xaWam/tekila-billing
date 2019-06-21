package spring.controller;


import com.jaravir.tekila.module.accounting.periodic.BillingManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ejb.EJB;
import javax.servlet.http.HttpServletRequest;

import static spring.util.Constants.INJECTION_POINT;

/**
 * Created by KamranMa, MusaAl on 13.12.2017.
 */
@RestController
@RequestMapping("/tekila-jobs")
public class JobsController {

    @EJB(mappedName = INJECTION_POINT + "BillingManager")
    private BillingManager billingManager;

    @Autowired
    HttpServletRequest request;

//    private final BillingManagerService billingManagerService;
//
//    public JobsController(BillingManagerService billingManagerService) {
//        this.billingManagerService = billingManagerService;
//    }
//
//    @PostMapping("/test")
//    public ResponseEntity<Void> testJob(){
//        billingManagerService.testJobs();
//        return ResponseEntity.ok().build();
//    }

    @PostMapping("/doPeriodicBillingPostPaid")
    public ResponseEntity<Void> doPeriodicBillingPostPaid() {
        billingManager.doPeriodicBillingPostPaid();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/narhomeProductPortfolioChange")
    public ResponseEntity<Void> narhomeProductPortfolioChange() {
        billingManager.narhomeProductPortfolioChange();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/fixDataplusConcurrencyIssue")
    public ResponseEntity<Void> fixDataplusConcurrencyIssue() {
        billingManager.fixDataplusConcurrencyIssue();
        return ResponseEntity.ok().build();
    }


    @PostMapping("/activateNonBilled")
    public ResponseEntity<Void> activateNonBilled() {
       try {
           request.login("elmarmammadov","ElmarMa123=");
           billingManager.activateNonBilledForWebService();
       }catch (Exception e){
           e.printStackTrace(System.err);
           return ResponseEntity.ok().build();
       }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/dechargeNonBilled")
    public ResponseEntity<Void> dechargeNonBilled() {
        billingManager.dechargeNonBilled();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/doPeriodicBillingPrepaid")
    public ResponseEntity<Void> doPeriodicBillingPrepaid() {
        billingManager.doPeriodicBillingPrepaid();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/doPeriodicBillingPrepaidAzertelekom")
    public ResponseEntity<Void> doPeriodicBillingPrepaidAzertelekom() {
        billingManager.doPeriodicBillingPrepaidAzertelekom();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/manageLifeCyclePostpaid")
    public ResponseEntity<Void> manageLifeCyclePostpaid() {
        billingManager.manageLifeCyclePostpaid();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/manageLifeCyclePrepaidNonAzertelekom")
    public ResponseEntity<Void> manageLifeCyclePrepaidNonAzertelekom() {
        billingManager.manageLifeCyclePrepaidNonAzertelekom();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/manageLifeCyclePrepaidAzertelekom")
    public ResponseEntity<Void> manageLifeCyclePrepaidAzertelekom() {
        billingManager.manageLifeCyclePrepaidAzertelekom();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/manageLifeCyclePrepaidGraceNonAzertelekom")
    public ResponseEntity<Void> manageLifeCyclePrepaidGraceNonAzertelekom() {
        billingManager.manageLifeCyclePrepaidGraceNonAzertelekom();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/manageLifeCyclePrepaidGraceAzertelekom")
    public ResponseEntity<Void> manageLifeCyclePrepaidGraceAzertelekom() {
        billingManager.manageLifeCyclePrepaidGraceAzertelekom();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/manageActivationCityNet")
    public ResponseEntity<Void> manageActivationCityNet() {
        billingManager.manageActivationCityNet();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/manageActivationNarFix")
    public ResponseEntity<Void> manageActivationNarFix() {
        billingManager.manageActivationNarFix();
        return ResponseEntity.ok().build();
    }

//    @PostMapping("/cancelPrepaid")
//    public ResponseEntity<Void> cancelPrepaid() {
//        billingManager.cancelPrepaid();
//        return ResponseEntity.ok().build();
//    }

    @PostMapping("/finalizePrepaid")
    public ResponseEntity<Void> finalizePrepaid() {
        billingManager.finalizePrepaid();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/applyLateFeeOrFinalize")
    public ResponseEntity<Void> applyLateFeeOrFinalize() {
        billingManager.applyLateFeeOrFinalize();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/manageLifeCycleVAS")
    public ResponseEntity<Void> manageLifeCycleVAS() {
        billingManager.manageLifeCycleVAS();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/convergentQueueProcessing")
    public ResponseEntity<Void> convergentQueueProcessing() {
        billingManager.convergentQueueProcessing();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/doPeriodicSipCharges")
    public ResponseEntity<Void> doPeriodicSipCharges() {
        billingManager.doPeriodicSipCharges();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/test-ejb")
    public ResponseEntity<Void> testJobEJB() {
        billingManager.testJobs();
        return ResponseEntity.ok().build();
    }

}
