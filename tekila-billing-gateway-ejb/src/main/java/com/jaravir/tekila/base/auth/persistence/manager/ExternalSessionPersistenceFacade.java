package com.jaravir.tekila.base.auth.persistence.manager;

import com.jaravir.tekila.base.auth.persistence.ExternalSession;
import com.jaravir.tekila.base.auth.persistence.ExternalUserType;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import org.apache.commons.lang3.RandomStringUtils;

/**
 *
 * @author khsadigov
 */
@Stateless
public class ExternalSessionPersistenceFacade extends AbstractPersistenceFacade<ExternalSession> {

    @PersistenceContext
    private EntityManager em;

    public ExternalSessionPersistenceFacade() {
        super(ExternalSession.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @Override
    public void save(ExternalSession session) {
        super.save(session);
    }

    @Override
    public void delete(ExternalSession session) {
        super.delete(session);
    }

    private ExternalSession findBySessionAndUser(String session, Integer userType) {
        try {
            return (ExternalSession) getEntityManager().createQuery("select u from ExternalSession u where u.session = :sessionId and u.userType = :userType")
                    .setParameter("sessionId", session)
                    .setParameter("userType", userType)
                    .getSingleResult();
        } catch (NoResultException n) {
            return null;
        }
    }

    public ExternalSession findGatewaySession(String session) {
        return findBySessionAndUser(session, ExternalUserType.GATEWAY.getCode());
    }

    public ExternalSession findSubscriberSession(String session) {
        return findBySessionAndUser(session, ExternalUserType.SUBSCRIBER.getCode());
    }

    public String add(int userType, String remoteAddress) {
        String ID = RandomStringUtils.randomAlphanumeric(20);

        ExternalSession session = new ExternalSession();
        session.setSession(ID);
        session.setUserType(userType);
        session.setRemoteAddress(remoteAddress);
        save(session);

        return ID;

    }

}
