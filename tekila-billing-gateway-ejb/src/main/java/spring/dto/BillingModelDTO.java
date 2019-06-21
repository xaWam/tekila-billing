package spring.dto;

import com.jaravir.tekila.module.service.model.BillingPrinciple;

/**
 * @author MusaAl
 * @date 3/14/2019 : 2:43 PM
 */
public class BillingModelDTO {

    private BillingPrinciple principle;
//    private boolean applyLateFee;
//    private String name;
//    private String dsc;
//    private int gracePeriodInDays;

    public BillingPrinciple getPrinciple() {
        return principle;
    }

    public void setPrinciple(BillingPrinciple principle) {
        this.principle = principle;
    }
}
