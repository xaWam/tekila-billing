package com.jaravir.tekila.module.stats.persistence.manager;

import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.engines.ProvisioningEngine;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import org.apache.log4j.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by elmarmammadov on 6/2/2017.
 */
@Stateless
public class AuthRejectionReasonPersistenceFacade {
private final static Logger log = Logger.getLogger(AuthRejectionReasonPersistenceFacade.class);
    @EJB
    EngineFactory provisioningFactory;
    public List<String> getAuthRejectionReasons(Subscription subscription){
        try {
            ProvisioningEngine provisioner = provisioningFactory.getProvisioningEngine(subscription);
            return provisioner.getAuthRejectionReasons(subscription);
        }catch (ProvisionerNotFoundException pe){
            pe.printStackTrace(System.err);
            log.error(pe);
        }
        return new ArrayList<>();
    }
}
