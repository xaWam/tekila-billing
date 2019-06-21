[1mdiff --git a/tekila-billing-gateway-ejb/src/main/java/com/jaravir/tekila/module/system/synchronisers/UserLifeCycleManager.java b/tekila-billing-gateway-ejb/src/main/java/com/jaravir/tekila/module/system/synchronisers/UserLifeCycleManager.java[m
[1mindex 7ab8e7e3..e1be13b1 100644[m
[1m--- a/tekila-billing-gateway-ejb/src/main/java/com/jaravir/tekila/module/system/synchronisers/UserLifeCycleManager.java[m
[1m+++ b/tekila-billing-gateway-ejb/src/main/java/com/jaravir/tekila/module/system/synchronisers/UserLifeCycleManager.java[m
[36m@@ -71,16 +71,12 @@[m [mpublic class UserLifeCycleManager {[m
         log.info(u + " remaining days >>>> " + remainingDays);[m
         log.debug(u + " remaining days >>>> " + remainingDays);[m
         if (remainingDays <= 5 && remainingDays >= 0) {[m
[31m-            List<User> users = new ArrayList<>();[m
[31m-            users.add(u);[m
[31m-            userPersistenceFacade.sendBulkEmailNotification(users, "Your password will expire in " + remainingDays[m
[32m+[m[32m            userPersistenceFacade.sendBulkEmailNotification(Arrays.asList(u), "Your password will expire in " + remainingDays[m
                     + " days .\nPlease change yor password or we will block your account after password expired", "PASSWORD CHANGE REMINDER");[m
[31m-            users.clear();[m
[32m+[m[32m            log.info("PASSWORD CHANGE REMINDER sent to " + u.getUserName());[m
         } else if (remainingDays < 0) {[m
[31m-            userPersistenceFacade.sendBulkEmailNotification(Arrays.asList(u),[m
[31m-                    "You are blocked by system",[m
[31m-                    "You didn't change your old password despite of email warnings, that is why we are going to to block you." +[m
[31m-                            "In order to activate your account contact Administrator");[m
[32m+[m[32m            userPersistenceFacade.sendBulkEmailNotification(Arrays.asList(u), "You didn't change your old password despite of email warnings, that is why we are going to to block you." +[m
[32m+[m[32m                            "In order to activate your account contact Administrator", "You are blocked by system");[m
             log.info("User " + u.getUserName() + " are going to block ...");[m
             userPersistenceFacade.forceBlock(u);[m
             systemLogger.success(SystemEvent.USER_BLOCKED, null, "User " + u.getUserName() + " goes to block because of expired password");[m
