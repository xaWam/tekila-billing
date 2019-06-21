package com.jaravir.tekila.module.system.synchronisers;

import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.provision.broadband.entity.BackProvisionDetails;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import java.util.List;

@DeclareRoles({"system"})
@RunAs("system")
@Singleton
public class DataplusSynchronizer {
    private static final Logger log = LoggerFactory.getLogger(DataplusSynchronizer.class);
    @EJB
    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;
    @EJB
    private EngineFactory engineFactory;

    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */ // @Schedule(hour = "*", minute = "35")
    public void syncDataplus() {
        log.debug("syncDataplus.Start Selection");
        List<Subscription> subscriptionList = subscriptionPersistenceFacade.findUnsynchronizedDataplus();
        log.debug("syncDataplus.Finalize Selection");
        log.debug(String.format("syncDataplus. Selection Size = %d", subscriptionList.size()));

//        for (final Subscription subscription : subscriptionList) {
//            log.debug("syncDataplus. agreement without switch setting = " + subscription.getId());
//            try {
//                BackProvisionDetails details = engineFactory.getProvisioningEngine(subscription).getBackProvisionDetails(subscription);
//                if (details != null && details.switchName != null && !details.switchName.isEmpty()) {
//                    subscriptionPersistenceFacade.synchronizeSubscription(details, subscription);
//                }
//                log.info(String.format("syncDataplus. details = %s", details));
//            } catch (ProvisionerNotFoundException e) {
//                log.error(String.format("Provisioner not found for subscription id = %d", subscription.getId()), e);
//            }
//        }
        long start = System.currentTimeMillis();
        subscriptionList.stream().forEach(subscription ->  {
            log.debug("syncDataplus. Subscription "+subscription.getId()+" without switch setting");
            try {
                BackProvisionDetails details = engineFactory.getProvisioningEngine(subscription).getBackProvisionDetails(subscription);
                if (details != null && details.switchName != null && !details.switchName.isEmpty()) {
                    subscriptionPersistenceFacade.synchronizeSubscription(details, subscription);
                }
                log.info(String.format("syncDataplus for subscription %s. Details = %s", subscription.getId(), details));
            } catch (ProvisionerNotFoundException e) {
                log.error(String.format("Provisioner not found for subscription id = %d", subscription.getId()), e);
            }
        });
        log.info("syncDataplus  elapsed time : {}", (System.currentTimeMillis()-start)/1000);
        log.debug("syncDataplus job finished.");
    }
}
