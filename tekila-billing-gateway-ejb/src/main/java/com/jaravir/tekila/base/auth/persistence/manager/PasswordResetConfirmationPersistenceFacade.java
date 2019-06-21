/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jaravir.tekila.base.auth.persistence.manager;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.entity.PasswordResetConfirmation;
import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.base.persistence.manager.BillingSettingsManager;
import com.jaravir.tekila.module.system.log.SystemLogger;
import org.apache.log4j.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 *
 * @author sajabrayilov
 */
@Stateless
public class PasswordResetConfirmationPersistenceFacade extends AbstractPersistenceFacade<PasswordResetConfirmation> {

    @PersistenceContext
    private EntityManager em;
    private final static Logger log = Logger.getLogger(PasswordResetConfirmationPersistenceFacade.class);
    @Resource(name = "mail/tekilaSession")
    private Session session;

    @EJB
    private BillingSettingsManager settingsManager;
    @EJB
    private SystemLogger systemLogger;

    @EJB
    private UserPersistenceFacade userPersistenceFacade;

    public enum Filter implements Filterable {

        USERNAME("userName"),
        KEY("key");

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

    public PasswordResetConfirmationPersistenceFacade() {
        super(PasswordResetConfirmation.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public PasswordResetConfirmation getByUserName(String userName) {
        return getEntityManager().createQuery("select u from PasswordResetConfirmation u where u.userName = :name and u.status=0", PasswordResetConfirmation.class)
                .setParameter("name", userName).getSingleResult();
    }

    public List<PasswordResetConfirmation> getAllByUserName(String userName) {
        return getEntityManager().createQuery("select u from PasswordResetConfirmation u where u.userName = :name  and u.status=0", PasswordResetConfirmation.class)
                .setParameter("name", userName).getResultList();
    }

    public String getKeyByUserName(String userName) {
        return getEntityManager().createQuery("select u.key from PasswordResetConfirmation u where u.userName = :name  and u.status=0", String.class)
                .setParameter("name", userName).getSingleResult();
    }

    public String getUserNameByKey(String key) {
        return getEntityManager().createQuery("select u.username from PasswordResetConfirmation u where u.key = :key and u.status=0", String.class)
                .setParameter("key", key).getSingleResult();
    }

    @Override
    public void save(PasswordResetConfirmation reset) {
        super.save(reset);
        notifyByEmail(reset.getUserName(), false);
    }

    public void delete(String username) {
        try {
            super.delete(getByUserName(username));
        } catch (Exception ex) {
            log.error(String.format("User not found: username=%s", username), ex);
        }
    }

    public void processed(PasswordResetConfirmation reset) {
        reset.setStatus(1);
        super.update(reset);
    }

    public void processedByUserName(String UserName) {

        List<PasswordResetConfirmation> resets = getAllByUserName(UserName);

        for (PasswordResetConfirmation reset : resets) {
            processed(reset);
        }

    }

    public PasswordResetConfirmation check(String key) {
        try {
            PasswordResetConfirmation reset = getEntityManager().createQuery("select u from PasswordResetConfirmation u where u.key = :key  and u.status=0", PasswordResetConfirmation.class)
                    .setParameter("key", key)
                    .getSingleResult();

            return reset != null ? reset : null;
        } catch (Exception ex) {
            log.error(String.format("User not found: key=%s", key), ex);
            return null;
        }
    }

    private void notifyByEmail(String username, boolean isDebug) {
        User user = userPersistenceFacade.findByUserName(username);

        String txt = String.format("<p>Dear %s %s,</p>\n"
                + "\n"
                + "<p>You have requested password reset to <a href=\"http://tekila.azerconnect.az/\">TEKILA Billing & TT</a> (http://tekila.azerconnect.az/) "
                + ". Please click the link below in order to reset your password:</p>\n"
                + "\n"
                + "https://tekila.azerconnect.az/login.xhtml?key=" + getKeyByUserName(username)
                + "\n"
                + "<p>For addition information please contact the following mail group:</p>\n"
                + "Narhome Billing (Narhomebilling@azerconnect.az)\n</br>",
                user.getFirstName(), user.getSurname(), user.getUserName()
        );

        try {
            MimeMessage message = new MimeMessage(session);
            String email = user.getEmail();

            if (isDebug) {
//                email = "khsadigov@azerfon.az";
                email = "KhayyamS@azerconnect.az";
            }

            InternetAddress emailAddress = new InternetAddress(email);
            message.setRecipient(Message.RecipientType.TO, emailAddress);

            message.setSubject("Password Reset Confirmation");
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

}
