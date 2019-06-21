package com.jaravir.tekila.module.system.synchronisers;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.base.persistence.manager.BillingSettingsManager;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Days;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@DeclareRoles({"system"})
@RunAs("system")
@Singleton
public class UserLifeCycleManager {

    private static final Logger log = Logger.getLogger(UserLifeCycleManager.class);

    @EJB
    private BillingSettingsManager settingsManager;
    @EJB
    private UserPersistenceFacade userPersistenceFacade;
    @EJB
    private SystemLogger systemLogger;


    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */ // @Schedule(hour = "02", minute = "00")        //ignoreline
    public void checkExpiredPasswords() {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>USER CREDENTIAL CHECK BEGAN >>>>>>>>>>>>>>>>>>>>>>>>");
        List<User> users = new ArrayList<>();
        try {
            users = userPersistenceFacade.findAllActive();
        } catch (Exception e) {
            log.error("Error occurs while finding all active users for checking expired password event", e);
        }
        users = getNonResellerUsers(users);  //remove Dataplus, Global, CNC resellers and admins from list
        for (User u : users) {
            checkUser(u);
        }
        log.debug("Check expired password job finished");
    }

    @RolesAllowed("system")
    /**  Tekila JOBS runs on tekila_jobs branch  */ // @Schedule(hour = "12", minute = "30")        //ignoreline
    public void blockPassiveUsers() {
        log.info("=======starting check dor pass===========");
        List<User> users = new ArrayList<>();
        try {
            users = userPersistenceFacade.findAllActive();
        } catch (Exception e) {
            log.error("Error occurs while finding all active users for blocking passive user event", e);
        }
        users = getNonResellerUsers(users); //remove Dataplus, Global, CNC resellers and admins from list
        for (User u : users) {
            checkUserStillIdle(u);
        }
        log.debug("Block passive users job finished");
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void checkUser(User u) {
        try {
            int daysPast = Days.daysBetween(u.getLastPasswordChanged(), DateTime.now()).getDays();
            int remainingDays = settingsManager.getSettings().getPasswordChangeInterval() - daysPast;
            log.debug(u + " remaining days >>>> " + remainingDays);
            if (remainingDays <= 5 && remainingDays >= 0) {
                userPersistenceFacade.sendBulkEmailNotification(Arrays.asList(u), "Your password will expire in " + remainingDays
                        + " days .\nPlease change your password or we will block your account after password expired", "PASSWORD CHANGE REMINDER");
                log.info("PASSWORD CHANGE REMINDER sent to " + u.getUserName());
            } else if (remainingDays < 0) {
                userPersistenceFacade.sendBulkEmailNotification(Arrays.asList(u), "You didn't change your old password despite of email warnings, that is why we are going to to block you." +
                        "In order to activate your account contact Administrator", "You are blocked by system");
                log.info("User " + u.getUserName() + " are going to block for expired password ...");
                userPersistenceFacade.forceBlock(u);
                systemLogger.success(SystemEvent.USER_BLOCKED, null, "User " + u.getUserName() + " goes to block because of expired password");
                log.debug(u + " is blocked from system job");
            }
        } catch (Exception ex) {
            log.error("Error occurs while checking expired password of user " + u.getUserName(), ex);
        }
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void checkUserStillIdle(User user) {
        try {
            int daysPassed = -1;
            if (user.getLastLoginDate() != null) {
                daysPassed = Days.daysBetween(user.getLastLoginDate(), DateTime.now()).getDays();
            } else {
                log.info("user created but never logined , username = " + user.getUserName());
            }

            if (daysPassed > settingsManager.getSettings().getUserMaxIdleDays()) {
                userPersistenceFacade.sendBulkEmailNotification(Arrays.asList(user),
                        "You are not using Tekila for a while, that is why we are going to to block you." +
                                "In order to activate your account contact Administrator", "You are blocked by system");
                log.info("User " + user.getUserName() + " are going to block for not using tekila for a while ...");
                userPersistenceFacade.forceBlock(user);
                systemLogger.success(SystemEvent.USER_BLOCKED, null, "User :" + user.getUserName() + " goes to block because of passive " + daysPassed + " days");
            }
        } catch (Exception ex) {
            log.error("Error occurs while blocking passive user " + user.getUserName(), ex);
        }
    }


    private List<User> getNonResellerUsers(List<User> users) {
        final List<String> resellers = Arrays.asList("DataPlus Reseller", "Global Reseller", "CNC Reseller", "DataPlus Admin", "Global Admin", "CNC Admin");
//        Predicate<User> isNotReseller = u -> !resellers.contains(u.getGroup().getGroupName());
//        return users.stream().filter(isNotReseller).collect(Collectors.<User>toList());

        String groupName = "";
        List<User> userListWithoutReseller = new ArrayList<>();
        for (User user : users) {
            try {
                groupName = user.getGroup().getGroupName();
                if (!resellers.contains(groupName))
                    userListWithoutReseller.add(user);
            } catch (Exception e) {
                log.error(e.getClass() + " occurs while getting group name of user " + user.getUserName(), e);
            }
        }

        log.info("Nonreseller user list: "+userListWithoutReseller);

        return userListWithoutReseller;
    }


    //This method isn't used yet, but it can be used if any problems occurs with the above method.
    private boolean isReseller(User user) {
        final List<String> resellers = Arrays.asList("DataPlus Reseller", "Global Reseller", "CNC Reseller", "DataPlus Admin", "Global Admin", "CNC Admin");
        String groupName = "";
        try {
            groupName = user.getGroup().getGroupName();
        } catch (Exception e) {
            log.error(e.getClass() + " occurs while getting group name of user " + user.getUserName(), e);
        }
        return resellers.contains(groupName);
    }

}
