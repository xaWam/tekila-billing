/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jaravir.tekila.base.auth.persistence.manager;

import com.jaravir.tekila.base.auth.entity.SessionInfo;
import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

/**
 *
 * @author sajabrayilov
 */
@Stateless
public class SessionFacade extends AbstractPersistenceFacade<SessionInfo>{
    @PersistenceContext
    private EntityManager em;    
    private final static Logger log = Logger.getLogger(SessionFacade.class);

    @EJB private UserPersistenceFacade userFacade;

    public SessionFacade () {
        super(SessionInfo.class);
    }
    
    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
    
    public void createSessionInfo(String username, String group) {
        SessionInfo session = new SessionInfo(username, generateKey());
        save(session);
    }

    public void createSessionInfo(String sessionID, String username, String group, String key, String ipAddress, String forwardedFor) {
        SessionInfo session = new SessionInfo(username, key);
        session.setIpAddress(ipAddress);
        session.setForwardedFor(forwardedFor);
        session.setExpires_timestamp(session.getLogin_timestamp().plusMinutes(60));
        session.setSessionID(sessionID);
        save(session);
    }

    public SessionInfo findActiveSession (String sessionID) {
        return getEntityManager().createQuery("select s from SessionInfo s where s.sessionID = :ses and s.login_timestamp > :date", SessionInfo.class)
            .setParameter("ses", sessionID).setParameter("date", DateTime.now().minusHours(24)).getSingleResult();
    }

    public SessionInfo reloginBySessionID(String newSessionID, String oldSessionID) {
        log.debug(String.format("reloginBySessionID: parameters[newSessionID=%s, oldSessionID=%s", newSessionID, oldSessionID));
        try {
            SessionInfo sessionInfo = findActiveSession(oldSessionID);
            log.debug("Found session: " + sessionInfo);
            sessionInfo.setLogin_timestamp(DateTime.now());
            sessionInfo.setExpires_timestamp(sessionInfo.getLogin_timestamp().plusMinutes(60));
            sessionInfo.setSessionID(newSessionID);
            sessionInfo.setKey(generateKey());
            sessionInfo.setSessionID(newSessionID);
            log.debug("Renewed SessionInfo=" + sessionInfo);

            return sessionInfo;
        }
        catch (Exception ex) {
            log.debug("Relogin failed", ex);
            return null;
        }
    }

    public SessionInfo findByUserID (Long userID, String sessionID) {
        User user = userFacade.find(userID);

        if (user == null)
            return null;

        return findByUsernameAndKey(user.getUserName(), sessionID);
    }
    public SessionInfo findByUsername(String username) {
        return em.createQuery("select s from SessionInfo s where s.username = :usr", SessionInfo.class)
                .setParameter("usr", username)
                .getSingleResult();
    }

    public SessionInfo findByUsernameAndKey (String username, String key) {
        return em.createQuery("select s from SessionInfo s where s.username = :usr and s.key = :key", SessionInfo.class)
                .setParameter("usr", username)
                .setParameter("key", key)
                .getSingleResult();
    }
    
    public void deleteByUsername (String username) {
        SessionInfo session = this.findByUsername(username);
        this.delete(session);
    }

    public void deleteByUsernameAndKey (String username, String key) {
        SessionInfo session = this.findByUsernameAndKey(username, key);
        this.delete(session);
    }
    
    public String generateKey() {
        try {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(DateTime.now().toString().getBytes());
        BigInteger hex = new BigInteger(1, md.digest());
        return hex.toString(16);
        }
        catch (NoSuchAlgorithmException ex) {
            log.error("Algorithm not found: ", ex);
            return null;
        }
    }

    public void removeExpiredSessions(long userID, int timeout) {
        User user = userFacade.find(userID);
        int count = em.createQuery("delete from SessionInfo s where s.username = :user and s.expires_timestamp < current_timestamp")
                .setParameter("user", user.getUserName()).executeUpdate();
        log.info(String.format("Deleted %d expired sessions", count));
    }
}
