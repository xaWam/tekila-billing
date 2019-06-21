package com.jaravir.tekila.engines;

import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;

import javax.ejb.*;

/**
 * Created by khsadigov on 17/05/2017.
 */
@Stateless(name = "NoBillingManager", mappedName = "NoBillingManager")
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class NoBillingManager implements BillingEngine {
    @Override
    public void billPrepaid(Subscription subscription) {

    }

    @Override
    public void billPostpaid(Subscription subscription) {

    }

    @Override
    public void manageLifeCyclePrepaid(Subscription subscription) {

    }

    @Override
    public void manageLifeCyclePostapid(Subscription subscription) {

    }

    @Override
    public void manageLifeCyclePrepaidGrace(Subscription subscription) {

    }

    @Override
    public void cancelPrepaid(Subscription subscription) {

    }

    @Override
    public void finalizePrepaid(Subscription subscription) {

    }

    @Override
    public void applyLateFeeOrFinalize(Subscription subscription) {

    }

    @Override
    public void sipCharge(Subscription subscription, double amount) {

    }
}
