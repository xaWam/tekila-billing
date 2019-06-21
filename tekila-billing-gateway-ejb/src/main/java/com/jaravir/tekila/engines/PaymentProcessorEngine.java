package com.jaravir.tekila.engines;

import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;

/**
 * Created by khsadigov on 5/16/2017.
 */
public interface PaymentProcessorEngine extends BaseEngine {

    void process(Subscription subscription, long amount);
}
