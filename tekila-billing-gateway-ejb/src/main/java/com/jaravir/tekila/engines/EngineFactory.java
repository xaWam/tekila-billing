package com.jaravir.tekila.engines;

import com.jaravir.tekila.module.engines.EngineType;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.service.entity.ServiceProvider;
import com.jaravir.tekila.module.stats.persistence.manager.TechnicalStatusPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import org.apache.log4j.Logger;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Created by khsadigov on 5/16/2017.
 */
@Stateless
@LocalBean
public class EngineFactory {
    private final static Logger log = Logger.getLogger(EngineFactory.class);

    @EJB
    EnginePersistenceFacade enginePersistenceFacade;

    public PaymentProcessorEngine getPaymentEngine(Subscription subscription) {
        String jndiName = enginePersistenceFacade.getJndiName(subscription.getService().getProvider(), EngineType.PaymentProcessor);
        log.info(jndiName);
        try {
            return (PaymentProcessorEngine) new InitialContext().lookup("java:module/" + jndiName);
        } catch (NamingException e) {
            log.error(jndiName);
            log.error(e);
            e.printStackTrace();
        }

        return null;

    }


    public BillingEngine getBillingEngine(Subscription subscription) {
        String jndiName = enginePersistenceFacade.getJndiName(subscription.getService().getProvider(), EngineType.Billing);

        try {
            log.info(jndiName);

            return (BillingEngine) new InitialContext().lookup("java:module/" + jndiName);
        } catch (NamingException e) {
            log.error(jndiName);
            log.error(e);
            e.printStackTrace();
        }

        return null;

    }

    public ProvisioningEngine getProvisioningEngine(Subscription subscription) throws ProvisionerNotFoundException {
        String jndiName = enginePersistenceFacade.getJndiName(subscription.getService().getProvider(), EngineType.Provisioning);
        log.info(jndiName);

        try {
            return (ProvisioningEngine) new InitialContext().lookup("java:module/" + jndiName);
        } catch (NamingException e) {
            e.printStackTrace();
            log.error(jndiName);
            log.error(e);
            throw new ProvisionerNotFoundException("Cannot find provider");
        }
    }

    public ProvisioningEngine getProvisioningEngine(ServiceProvider provider) throws ProvisionerNotFoundException {
        String jndiName = enginePersistenceFacade.getJndiName(provider, EngineType.Provisioning);
        log.info(jndiName);

        try {
            return (ProvisioningEngine) new InitialContext().lookup("java:module/" + jndiName);
        } catch (NamingException e) {
            log.error(jndiName);
            log.error(e);
            e.printStackTrace();

            throw new ProvisionerNotFoundException("Cannot find provider");
        }
    }

    public OperationsEngine getOperationsEngine(Subscription subscription) {
        String jndiName = enginePersistenceFacade.getJndiName(subscription.getService().getProvider(), EngineType.Operations);
        log.info(jndiName);

        try {
            return (OperationsEngine) new InitialContext().lookup("java:module/" + jndiName);
        } catch (NamingException e) {
            log.error(jndiName);
            log.error(e);
            e.printStackTrace();
        }

        return null;

    }

    public OperationsEngine getOperationsEngine(ServiceProvider provider) {
        String jndiName = enginePersistenceFacade.getJndiName(provider, EngineType.Operations);
        log.info(jndiName);

        try {
            return (OperationsEngine) new InitialContext().lookup("java:module/" + jndiName);
        } catch (NamingException e) {
            log.error(jndiName);
            log.error(e);
            e.printStackTrace();
        }

        return null;

    }
}
