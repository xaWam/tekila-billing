package com.jaravir.tekila.engines;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import javax.ejb.Stateless;

/**
 * Created by khsadigov on 5/16/2017.
 */

@Stateless(name = "QutuPaymentProcessor", mappedName = "QutuPaymentProcessor")
public class QutuPaymentProcessor implements PaymentProcessorEngine {

    @Override
    public void process(Subscription subscription, long amount) {

    }
}
