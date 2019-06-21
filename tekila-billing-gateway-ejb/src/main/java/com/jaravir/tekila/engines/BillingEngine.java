package com.jaravir.tekila.engines;

import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;

/**
 * Created by khsadigov on 5/16/2017.
 */
public interface BillingEngine extends BaseEngine {

    void billPrepaid(Subscription subscription);

    void billPostpaid(Subscription subscription);

    void manageLifeCyclePrepaid(Subscription subscription);

    void manageLifeCyclePostapid(Subscription subscription);

    void manageLifeCyclePrepaidGrace(Subscription subscription);

    void cancelPrepaid(Subscription subscription);

    void finalizePrepaid(Subscription subscription);
    
    void applyLateFeeOrFinalize(Subscription subscription);

    void sipCharge(Subscription subscription, double amount);
}
