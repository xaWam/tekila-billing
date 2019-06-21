package com.jaravir.tekila.module.store;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.accounting.entity.Payment;
import com.jaravir.tekila.module.store.scratchcard.persistence.entity.ScratchCard;
import com.jaravir.tekila.module.store.scratchcard.persistence.entity.ScratchCardSession;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by shnovruzov on 5/6/2016.
 */
@Stateless
public class ScratchCardSessionPersistenceFacade extends AbstractPersistenceFacade<ScratchCardSession> {

    @PersistenceContext
    private EntityManager em;
    @Resource
    private SessionContext ctx;
    @Resource
    private EJBContext ctxEJB;
    private final static Logger log = Logger.getLogger(ScratchCardSessionPersistenceFacade.class);

    public ScratchCardSessionPersistenceFacade() {
        super(ScratchCardSession.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public void create(ScratchCardSession scratchCardSession, Payment payment, ScratchCard scratchCard, Subscription sbn, String wrongPin, long wrongSerial, int wrongAttempt, String agreement, Long serial) {
        scratchCardSession.setPayment(payment);
        scratchCardSession.setScratchCard(scratchCard);
        scratchCardSession.setSubscription(sbn);
        scratchCardSession.setwrongAttempt(wrongAttempt);
        scratchCardSession.setWrongPin(wrongPin);
        scratchCardSession.setWrongSerial(wrongSerial);
        scratchCardSession.setlastAttemptTime(DateTime.now());
        scratchCardSession.setAgreement(agreement);
        scratchCardSession.setSerial(serial);
        save(scratchCardSession);
    }

    public String getSessionQuery(long id) {
        String query = "select distinct ss from ScratchCardSession ss where ss.id = " + id;
        return query;
    }

}
