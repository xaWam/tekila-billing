/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jaravir.tekila.base.auth.persistence.manager;

import com.jaravir.tekila.base.auth.Privilege;
import com.jaravir.tekila.base.auth.UserStatus;
import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.interceptor.SecurityInterceptor;
import com.jaravir.tekila.base.interceptor.annot.PermissionRequired;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.base.persistence.manager.BillingSettingsManager;
import com.jaravir.tekila.module.system.log.SystemLogger;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.interceptor.Interceptors;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author sajabrayilov
 */
@Stateless
public class UserPersistenceFacade extends AbstractPersistenceFacade<User> {

    @PersistenceContext
    private EntityManager em;
    private final static Logger log = Logger.getLogger(UserPersistenceFacade.class);
    @Resource(name = "mail/tekilaSession")
    private Session session;

    @EJB
    private BillingSettingsManager settingsManager;
    @EJB
    private SystemLogger systemLogger;

    public enum Filter implements Filterable {

        USERNAME("userName"),
        FIRSTNAME("firstName"),
        SURNAME("surname"),
        GROUP_ID("group.id"),
        EMAIL("email");

        private final String field;
        private MatchingOperation operation;

        Filter(String field) {
            this.field = field;
            operation = MatchingOperation.EQUALS;
        }

        @Override
        public String getField() {
            return field;
        }

        @Override
        public MatchingOperation getOperation() {
            return operation;
        }

        public void setOperation(MatchingOperation operation) {
            this.operation = operation;
        }
    }

    public UserPersistenceFacade() {
        super(User.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    //@PermissionRequired(module = "Admin", subModule = "users", privilege = Privilege.READ)
    //@Interceptors(SecurityInterceptor.class)
    @Override
    public User findForceRefresh(long pk) {
        return super.findForceRefresh(pk);
    }

    public User findByUserName(String userName) {
        return getEntityManager().createQuery("select u from User u where u.userName = :name", User.class)
                .setParameter("name", userName).getSingleResult();
    }

    public long getIdByUserName(String userName) {
        return getEntityManager().createQuery("select u.id from User u where u.userName = :name", Long.class)
                .setParameter("name", userName).getSingleResult();
    }

    public List<User> findAllByGroupID(Long groupID) {
        return getEntityManager().createQuery("select u from User u where u.group.id = :groupID", User.class)
                .setParameter("groupID", groupID).getResultList();
    }


    public List<User> findStartsWith(String query) {
        return getEntityManager().createQuery("select u from User u where u.userName like '" + query + "%'", User.class)
                .getResultList();
    }

    public List<User> findAllActive() {
        return getEntityManager().createQuery("select u from User u where u.status = :sts", User.class)
                .setParameter("sts", UserStatus.ACTIVE)
                .getResultList();
    }

    public User check(String username, String password) {
        try {
            User user = getEntityManager().createQuery("select u from User u where u.userName = :user and u.password = :pass", User.class)
                    .setParameter("user", username)
                    .setParameter("pass", password)
                    .getSingleResult();

            return user != null ? user : null;
        } catch (Exception ex) {
            log.error(String.format("User not found: username=%s, password=%s", username, password), ex);
            return null;
        }
    }

    public void save(User user, String rawPass, boolean isDebug) {
        super.save(user);
        notifyByEmail(user, rawPass, isDebug);
    }


    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void saveAutonomously(User user, String rawPass, boolean isDebug) {
        save(user, rawPass, isDebug);
    }

    public User update(User user, String rawPass) {
        user = super.update(user);
        notifyByEmailonUpdate(user, rawPass);
        return user;
    }

    private void notifyByEmail(User user, String rawPass, boolean isDebug) {
        String txt = String.format("<p>Dear %s %s,</p>\n"
                        + "\n"
                        + "<p>You account to access <a href=\"http://tekila.azerconnect.az/\">TEKILA Billing & TT</a> (http://tekila.azerconnect.az/) "
                        + "has been created. You can access the system using the following credentials:</p>\n"
                        + "\n"
                        + "username: %s</br>\n"
                        + "password: %s</br>\n"
                        + "\n"
                        + "<p>Upon logon you can <a href=\"http://tekila.azerconnect.az/pages/self/change_password.xhtml\">change the password</a> via <b>Actions -> Change password</b> menu item on the top right corner of the screen.</p>\n"
                        + "\n"
                        + "<p>For addition information please contact the following mail group:</p>\n"
                        + "\n"
                        + "Narhome Billing (Narhomebilling@azerconnect.az)\n</br>",
                user.getFirstName(), user.getSurname(), user.getUserName(), rawPass
        );

        try {
            MimeMessage message = new MimeMessage(session);
            String email = user.getEmail();

            if (isDebug) {
                email = "sajabrayilov@azerfon.az";
            }

            InternetAddress emailAddress = new InternetAddress(email);
            message.setRecipient(Message.RecipientType.TO, emailAddress);

            if (!isDebug) {
                message.setRecipients(Message.RecipientType.CC, new InternetAddress[]{
                        new InternetAddress("fibayev@azerfon.az"),
                        new InternetAddress("sajabrayilov@azerfon.az")
                });
            }

            message.setSubject("Account created");
            message.setFrom(new InternetAddress("no-reply@azerconnect.az", "NarHome Billing"));
            message.setContent(txt, "text/html; charset=utf-8");

            Transport.send(message);
            log.debug("email sent successfully");
        } catch (AddressException ex) {
            log.error("Cannot send email", ex);
        } catch (MessagingException ex) {
            log.error("Cannot send email", ex);

        } catch (UnsupportedEncodingException ex) {
            log.error("Cannot send email", ex);
        }
    }

    @Asynchronous
    public void sendBulkEmailNotification(List<User> targets, String messageText, String subjectText) {
        String ms;
        for (User u : targets) {
            if (u.getStatus() == UserStatus.BLOCKED)
                continue;
            ms = String.format(messageText, u.getFirstName(), u.getSurname());
            try {
                MimeMessage message = new MimeMessage(session);
                InternetAddress emailAddress = new InternetAddress(u.getEmail());
                message.setRecipient(Message.RecipientType.TO, emailAddress);
                message.setSubject(subjectText);
                message.setFrom(new InternetAddress("no-reply@azerconnect.az", "NarHome Billing"));
                message.setContent(ms, "text/html; charset=utf-8");
                Transport.send(message);
                log.info("email sent successfully to " + u);
            } catch (AddressException ex) {
                log.error("Cannot send email", ex);
            } catch (MessagingException ex) {
                log.error("Cannot send email", ex);

            } catch (UnsupportedEncodingException ex) {
                log.error("Cannot send email", ex);
            }

        }
    }

    private void notifyByEmailonUpdate(User user, String rawPass) {
        String txt = String.format("<p>Dear %s %s,</p>\n"
                        + "\n"
                        + "<p>Your password has been successfully changed. You can access the system using the following credentials:</p>\n"
                        + "\n"
                        + "username: %s</br>\n"
                        + "password: %s</br>\n"
                        + "\n"
                        + "<p>Upon logon you can <a href=\"http://tekila.azerconnect.az/pages/self/change_password.xhtml\">change the password</a> via <b>Actions -> Change password</b> menu item on the top right corner of the screen.</p>\n"
                        + "\n"
                        + "<p>For addition information please contact the following mail group:</p>\n"
                        + "\n"
                        + "Narhome Billing (Narhomebilling@azerconnect.az)\n</br>",
                user.getFirstName(), user.getSurname(), user.getUserName(), rawPass
        );

        try {
            MimeMessage message = new MimeMessage(session);
            InternetAddress emailAddress = new InternetAddress(user.getEmail());
            message.setRecipient(Message.RecipientType.TO, emailAddress);
            /* message.setRecipients(Message.RecipientType.CC, new InternetAddress[]{
             new InternetAddress("fibayev@azerfon.az"),
             new InternetAddress("sajabrayilov@azerfon.az")
             });*/
            message.setSubject("Password changed");
            message.setFrom(new InternetAddress("no-reply@azerconnect.az", "NarHome Billing"));
            message.setContent(txt, "text/html; charset=utf-8");

            Transport.send(message);
            log.debug("email sent successfully");
        } catch (AddressException ex) {
            log.error("Cannot send email", ex);
        } catch (MessagingException ex) {
            log.error("Cannot send email", ex);

        } catch (UnsupportedEncodingException ex) {
            log.error("Cannot send email", ex);
        }
    }

    public void block(String userName) {
        try {
            User user = findByUserName(userName);

            if (user.getLoginRetryCounter() >= settingsManager.getSettings().getMaximumLoginRetryCount()) {
                user.setStatus(UserStatus.BLOCKED);
                user.setBlockedDate(DateTime.now());
                user.setBlockedTillDate(user.getBlockedDate().plusHours(
                        settingsManager.getSettings().getUserBlockPeriodInHours()
                ));
                update(user);
            }

        } catch (Exception ex) {
            log.error(ex);
        }
    }


    public void forceBlock(String userName) {
        User user = findByUserName(userName);
        user.setStatus(UserStatus.BLOCKED);
        user.setBlockedDate(DateTime.now());
        update(user);
    }

    public void forceBlock(User user) {
        user.setStatus(UserStatus.BLOCKED);
        user.setBlockedDate(DateTime.now());
        update(user);
    }

    public void forceUnblock(String userName) {
        User user = findByUserName(userName);
        user.setLoginRetryCounter(0);
        user.setStatus(UserStatus.ACTIVE);
        user.setBlockedDate(null);
        update(user);
    }

    public String getTheme(String userName) {
        try {
            String theme = getEntityManager().createQuery("select u.theme from User u where u.userName = :name", String.class)
                    .setParameter("name", userName).getSingleResult();

            if (theme.equals("")) {
                theme = "cupertino";
            }
            return theme;
        } catch (Exception e) {
            return "cupertino";
        }
    }

    public List<User> getUsers() {
        try {
            return getEntityManager().createQuery("select u from User u ", User.class).getResultList();
        } catch (Exception e) {
            log.error(e);
            return new ArrayList<>();
        }
    }
}
