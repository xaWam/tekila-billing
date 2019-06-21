package com.jaravir.tekila.module.accounting.listener;

import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;

/**
 * Created by sajabrayilov on 6/1/2015.
 */
public interface BeforeBillingListener {
    void beforeBilling(Subscription subscription) throws Exception;
}
