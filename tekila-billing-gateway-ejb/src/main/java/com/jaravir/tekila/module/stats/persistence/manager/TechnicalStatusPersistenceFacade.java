package com.jaravir.tekila.module.stats.persistence.manager;

import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.engines.ProvisioningEngine;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.external.TechnicalStatus;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import org.apache.log4j.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Created by kmaharov on 28.07.2016.
 */
@Stateless
public class TechnicalStatusPersistenceFacade {
    private final static Logger log = Logger.getLogger(TechnicalStatusPersistenceFacade.class);
    @EJB
    EngineFactory provisioningFactory;

    public TechnicalStatus getTechnicalStatus(Subscription subscription) {
        try {
            ProvisioningEngine provisioner = provisioningFactory.getProvisioningEngine(subscription);
            return provisioner.getTechnicalStatus(subscription);
        } catch (Exception e) {
            log.error(e);
        }
        return null;
    }
}
