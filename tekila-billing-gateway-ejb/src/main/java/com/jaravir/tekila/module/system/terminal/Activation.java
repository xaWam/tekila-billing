package com.jaravir.tekila.module.system.terminal;

import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.engines.ProvisioningEngine;
import com.jaravir.tekila.module.accounting.periodic.BillingManager;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.engines.CitynetProvisioner;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import javax.ejb.*;
import java.io.Serializable;

/**
 * Created by khsadigov on 7/24/2016.
 */

@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class Activation extends Command implements Serializable {
    private static Logger log = Logger.getLogger(Activation.class);

    @EJB
    private BillingManager billingManager;
    @EJB
    private EngineFactory provisioningFactory;
    @EJB
    private SubscriptionPersistenceFacade subscriptionFacade;

    public Activation() {
        super();
    }


    public String run() {
        try {
//            billingManager.manageActivationForCitynet();
            return "Ok";
        } catch (Exception ex) {
            ex.printStackTrace();
            return "Error : " + ex;
        }

    }


    public String run(String[] params) {

        try {
            Subscription subscription = null;
            try {
                log.debug("subscriptionFacade: " + subscriptionFacade);
                subscription = subscriptionFacade.findByAgreementOrdinary(params[0]);
            } catch (Exception ex) {
                ex.printStackTrace();
                return "Subscription not found : " + ex;
            }

            if (subscription.getActivationDate() != null) {
                return "Subscription already activated ! Activation Date: " + subscription.getActivationDate();
            }

            ProvisioningEngine provisioner;
            try {
                provisioner = provisioningFactory.getProvisioningEngine(subscription);
            } catch (ProvisionerNotFoundException ex) {
                ex.printStackTrace();
                return "ProvisionerNotFoundException";
            }
            DateTime radiusFD = ((CitynetProvisioner) provisioner).getActivationDate(subscription);
            subscription.setActivationDate(radiusFD);
            subscriptionFacade.update(subscription);
            return "Subscription " + subscription.getAgreement() + " has been Activated";
        } catch (Exception ex) {
            ex.printStackTrace();
            return "Error : " + ex;
        }
    }
}
