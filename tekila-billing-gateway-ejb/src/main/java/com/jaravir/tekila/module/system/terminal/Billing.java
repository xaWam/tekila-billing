package com.jaravir.tekila.module.system.terminal;

import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.module.accounting.periodic.BillingManager;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.provision.ProvisioningFactory;
import org.apache.log4j.Logger;

import javax.ejb.*;

/**
 * Created by khsadigov on 7/24/2016.
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class Billing extends Command {
    private static Logger log = Logger.getLogger(Billing.class);
    @EJB
    private BillingManager billingManager;
    @EJB
    private EngineFactory provisioningFactory;
    @EJB
    private SubscriptionPersistenceFacade subscriptionFacade;

    @Override
    public String run() {
        return "Please specify an agreement";
    }

    @Override
    public String run(String[] params) {

        try {
            Subscription subscription = null;
            try {
                subscription = subscriptionFacade.findByAgreementOrdinary(params[0]);
            } catch (Exception ex) {
                ex.printStackTrace();
                return "Subscription not found : " + ex;
            }

//            citynetBilling.billing(subscription);
            return "Ok";

        } catch (Exception ex) {
            ex.printStackTrace();
            return "Error : " + ex;
        }
    }
}
