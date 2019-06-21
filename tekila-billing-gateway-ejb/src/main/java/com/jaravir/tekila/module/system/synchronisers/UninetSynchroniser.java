package com.jaravir.tekila.module.system.synchronisers;

import com.jaravir.tekila.base.persistence.manager.Register;
import com.jaravir.tekila.engines.*;
import com.jaravir.tekila.module.service.ServiceSettingType;
import com.jaravir.tekila.module.service.ServiceType;
import com.jaravir.tekila.module.service.entity.ServiceSetting;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionSetting;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionSettingPersistenceFacade;
import com.jaravir.tekila.module.system.log.SystemLogger;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import com.jaravir.tekila.provision.broadband.devices.manager.MiniPopPersistenceFacade;
import com.jaravir.tekila.provision.broadband.devices.manager.Providers;
import org.apache.log4j.Logger;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.annotation.security.RunAs;
import javax.ejb.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import javax.transaction.TransactionSynchronizationRegistry;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by elmarmammadov on 7/3/2017.
 */

@DeclareRoles({"system"})
@RunAs("system")
@Singleton
public class UninetSynchroniser {

    public static class SyncResult {
        public final String mac;
        public final String slot;
        public final String port;
        public final String dslam;
        public final String username;
        public final String password;

        SyncResult(String mac,
                   String slot,
                   String port,
                   String dslam,
                   String username,
                   String password) {
            this.mac = mac;
            this.slot = slot;
            this.port = port;
            this.dslam = dslam;
            this.username = username;
            this.password = password;
        }
    }

    private static final Logger log = Logger.getLogger(UninetSynchroniser.class);

    @EJB
    private SubscriptionPersistenceFacade subscriptionFacade;

    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */ // @Schedule(hour = "*", minute = "*/5")
    public void syncUninet() {
        log.info("syncUninet started.");

        List<SyncResult> settingsOnRadius = new ArrayList<>();

        try {
            for (Object[] syncResult : subscriptionFacade.findUnsynchronized()) {
                settingsOnRadius.add(
                        new SyncResult(
                                (String) syncResult[0],
                                syncResult[1].toString(),
                                syncResult[2].toString(),
                                (String) syncResult[3],
                                (String) syncResult[4],
                                (String) syncResult[5]));
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        log.info(String.format("# of subscriptions to be updated : %d", settingsOnRadius.size()));
        if (!settingsOnRadius.isEmpty()) {
            for (final SyncResult syncResult : settingsOnRadius) {
                try {
                    log.info("Synchronizing username = " + syncResult.username);
                    Subscription subscription = subscriptionFacade.findByAgreementOrdinary(syncResult.username);
                    subscriptionFacade.synchronizeSubscription(syncResult, subscription);
                    log.info(String.format("Synchronized subscription username = %s", syncResult.username));
                } catch (Exception ex) {
                    log.error(String.format("Could not synchronize subscription username = %s", syncResult.username), ex);
                }
            }
        }
        log.info("syncUninet finished");
    }
}
