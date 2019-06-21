package com.jaravir.tekila.module.system.synchronisers;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.base.persistence.manager.BillingSettingsManager;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Days;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.annotation.security.RunAs;
import javax.ejb.*;
import java.util.ArrayList;
import java.util.List;

@DeclareRoles({"system"})
@RunAs("system")
@Singleton
public class UserCredentialKnight {

    private static final Logger log = Logger.getLogger(UserCredentialKnight.class);

    @EJB
    private BillingSettingsManager settingsManager;


    @EJB
    private UserPersistenceFacade userPersistenceFacade;

    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */ // @Schedule(hour = "2")
    public void checkExpiredPasswords() {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>USER CREDENTIAL CHECK BEGAN >>>>>>>>>>>>>>>>>>>>>>>>");
        try {
            List<User> users = userPersistenceFacade.findAll();
            for (User u : users){
                checkUser(u);
            }
        } catch (Exception e) {
            log.error(e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void checkUser(User u) {
        int daysPast = Days.daysBetween(u.getLastPasswordChanged(), DateTime.now()).getDays();
        int remainingDays = settingsManager.getSettings().getPasswordChangeInterval() - daysPast;
        log.info(u+" remaining days >>>> " + remainingDays);
        log.debug(u+" remaining days >>>> " + remainingDays);
        if (remainingDays <= 5 && remainingDays >= 0) {
            List<User> users = new ArrayList<>();
            users.add(u);
            userPersistenceFacade.sendBulkEmailNotification(users, "Your password will expire in " + remainingDays
                    + " days .\nPlease change yor password or we will block your account after password expired", "PASSWORD CHANGE REMINDER");
            users.clear();
        } else if (remainingDays < 0) {
            userPersistenceFacade.forceBlock(u.getUserName());
            log.info(u+" is blocked from system job");
            log.debug(u+" is blocked from system job");
        }
    }

}
