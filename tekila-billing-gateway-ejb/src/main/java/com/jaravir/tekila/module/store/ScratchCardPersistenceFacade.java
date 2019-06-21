/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jaravir.tekila.module.store;

import com.jaravir.tekila.base.auth.persistence.manager.ExternalUserPersistenceFacade;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.accounting.manager.PaymentPersistenceFacade;
import com.jaravir.tekila.module.admin.AdminSettingPersistenceFacade;
import com.jaravir.tekila.module.auth.security.EncryptorAndDecryptor;
import com.jaravir.tekila.module.campaign.CampaignPersistenceFacade;
import com.jaravir.tekila.module.campaign.CampaignRegisterPersistenceFacade;
import com.jaravir.tekila.module.store.scratchcard.persistence.entity.ScratchCard;
import com.jaravir.tekila.module.store.scratchcard.persistence.entity.Serial;
import com.jaravir.tekila.module.store.scratchcard.persistence.entity.ScratchCardSession;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriberPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;

import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.ejb.*;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import com.jaravir.tekila.module.web.service.provider.BillingServiceProvider;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import java.lang.Object;

/**
 * @author shnovruzov
 */
@Stateless
public class ScratchCardPersistenceFacade extends AbstractPersistenceFacade<ScratchCard> {

    @PersistenceContext
    private EntityManager em;
    @Resource
    private SessionContext ctx;
    @Resource
    private EJBContext ctxEJB;
    private final static Logger log = Logger.getLogger(ScratchCardPersistenceFacade.class);
    @EJB
    private UserPersistenceFacade userFacade;
    @EJB
    private EncryptorAndDecryptor encryptorAndDecryptor;
    @EJB
    private ExternalUserPersistenceFacade externalUserFacade;
    @EJB
    private BillingServiceProvider serviceProvider;
    @EJB
    private PaymentPersistenceFacade paymentFacade;
    @EJB
    private CampaignPersistenceFacade campaignFacade;
    @EJB
    private CampaignRegisterPersistenceFacade campaignRegisterFacade;
    @EJB
    private SubscriptionPersistenceFacade subscriptionPersistenceFacade;
    @EJB
    private SubscriberPersistenceFacade subscriberPersistenceFacade;
    @EJB
    private AdminSettingPersistenceFacade adminSettingPersistenceFacade;

    public enum Filter implements Filterable {

        AMOUNT("amount"),
        BATCH_ID("batchID"),
        STATUS("status"),
        SERIAL("serial.id"),
        VALID_FROM("validFrom"),
        VALID_TO("validTo");

        private final String field;
        private MatchingOperation operation;

        Filter(String field) {
            this.field = field;
            this.operation = MatchingOperation.LIKE;
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

    public ScratchCardPersistenceFacade() {
        super(ScratchCard.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int create(List<ScratchCard> scratchCard) {

        int resCount = 0;
        try {
            for (ScratchCard sc : scratchCard) {
                save(sc);
                resCount++;
            }
            return resCount;
        } catch (Exception ex) {
            ctx.setRollbackOnly();
            log.error(String.format("%s: cannot create scratchCard", ""), ex);
            return 0;
        }
    }

    public String getSqlSearchQuery(Map<Filterable, Object> filters) {

        String sqlQuery = "select distinct sc from ScratchCard sc ";
        String where = " where 1=1";
        String and = " and ";

        if (filters.get(ScratchCardPersistenceFacade.Filter.AMOUNT) != null) {
            where += and;
            where += "sc.amount =" + filters.get(ScratchCardPersistenceFacade.Filter.AMOUNT);
            filters.remove(ScratchCardPersistenceFacade.Filter.AMOUNT);
        }

        if (filters.get(ScratchCardPersistenceFacade.Filter.BATCH_ID) != null) {
            where += and;
            where += "sc.batchID = " + filters.get(ScratchCardPersistenceFacade.Filter.BATCH_ID);
            filters.remove(ScratchCardPersistenceFacade.Filter.BATCH_ID);
        }

        if (filters.get(ScratchCardPersistenceFacade.Filter.SERIAL) != null) {
            where += and;
            where += "sc.serial.id = " + filters.get(ScratchCardPersistenceFacade.Filter.SERIAL);
            filters.remove(ScratchCardPersistenceFacade.Filter.SERIAL);
        }

        if (filters.get(ScratchCardPersistenceFacade.Filter.STATUS) != null) {
            where += and;
            where += "sc.status = " + filters.get(ScratchCardPersistenceFacade.Filter.STATUS);
            filters.remove(ScratchCardPersistenceFacade.Filter.STATUS);
        }

        if (filters.get(ScratchCardPersistenceFacade.Filter.VALID_FROM) != null) {
            where += and;
            where += "sc.validFrom >= :validFrom";
        }

        if (filters.get(ScratchCardPersistenceFacade.Filter.VALID_TO) != null) {
            where += and;
            where += "sc.validTo <= :validTo";
        }

        return sqlQuery + where;

    }

    public ScratchCard findScratchCardByPin(String pin) {
        log.debug("findScratch: " + pin);
        Query query = em.createQuery("select sc from ScratchCard sc where sc.pin = :pin and sc.status = :status and sc.validTo >= CURRENT_TIMESTAMP", ScratchCard.class);
        try {
            query.setParameter("pin", pin);
            query.setParameter("status", 0);
            Object res = query.getSingleResult();
            return res == null ? null : (ScratchCard) res;
        } catch (Exception ex) {
            return null;
        }
    }

    public boolean checkSerial(Serial serial) {
        Query query = em.createQuery("select sc.serial from ScratchCard sc where sc.serial = :serial", ScratchCard.class);
        try {
            query.setParameter("serial", serial);
            Object res = query.getSingleResult();
            log.debug("res: " + res);
            return res == null ? false : true;
        } catch (Exception ex) {
            return false;
        }
    }

    public List<ScratchCardSession> getSessionList(Subscription subscription) {
        Query query = em.createQuery("select ss from ScratchCardSession ss where ss.subscription = :subscription and ss.lastAttemptTime >= :oneHourAgo order by ss.lastAttemptTime desc", ScratchCardSession.class);
        try {
            query.setParameter("subscription", subscription);
            query.setParameter("oneHourAgo", DateTime.now().minusHours(adminSettingPersistenceFacade.getBlockingSetting().getBlockingHours()));
            return query.getResultList();
        } catch (Exception ex) {
            return null;
        }
    }

    public ScratchCardSession findSessionByCard(ScratchCard selectedCard) {
        try {
            Query query = em.createQuery("select ss from ScratchCardSession ss where ss.scratchCard = :selectedCard and ss.wrongAttempt = 0", ScratchCardSession.class);
            query.setParameter("selectedCard", selectedCard);
            Object res = query.getSingleResult();
            return res == null ? null : (ScratchCardSession) res;
        } catch (Exception ex) {
            return null;
        }
    }

    public ScratchCard findBySerial(Serial serial) {
        try {
            Query query = em.createQuery("select sc from ScratchCard sc where sc.serial = :serial", ScratchCard.class);
            query.setParameter("serial", serial);
            Object res = query.getSingleResult();
            return res == null ? null : (ScratchCard) res;
        } catch (Exception ex) {
            return null;
        }
    }

}
