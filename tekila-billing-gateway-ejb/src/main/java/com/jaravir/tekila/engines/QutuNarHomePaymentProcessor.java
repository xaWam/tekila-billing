
package com.jaravir.tekila.engines;

import com.jaravir.tekila.base.persistence.manager.BillingSettingsManager;
import com.jaravir.tekila.module.accounting.entity.Invoice;
import com.jaravir.tekila.module.accounting.manager.InvoicePersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberLifeCycleType;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionStatus;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.broadband.devices.manager.Providers;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.List;


/**
 * Created by ShakirG on 10/09/2018.
 */
@Stateless(name = "QutuNarHomePaymentProcessor", mappedName = "QutuNarHomePaymentProcessor")
public class QutuNarHomePaymentProcessor implements PaymentProcessorEngine {


    private final static Logger log = Logger.getLogger(CitynetPaymentProcessor.class);


    @Override
    public void process(Subscription subscription, long amount) {

    }
}
