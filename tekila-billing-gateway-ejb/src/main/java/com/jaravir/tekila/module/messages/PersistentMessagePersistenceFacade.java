package com.jaravir.tekila.module.messages;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import org.joda.time.DateTime;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Created by kmaharov on 20.09.2017.
 */
@Stateless
public class PersistentMessagePersistenceFacade extends AbstractPersistenceFacade<PersistentMessage> {
    @PersistenceContext
    private EntityManager em;

    public PersistentMessagePersistenceFacade() {
        super(PersistentMessage.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return this.em;
    }

    public List<PersistentMessage> findNewPaymentMessages() {
        return this.getEntityManager().createQuery("select m from PersistentMessage m where m.status = :addedStatus and m.messageType = :paymentType")
                .setParameter("addedStatus", MessageStatus.ADDED)
                .setParameter("paymentType", MessageType.PAYMENT)
                .getResultList();
    }

    public List<PersistentMessage> findFailedPaymentMessages() {
        return this.getEntityManager().createQuery("select m from PersistentMessage m where m.status = :scheduledStatus and m.messageType = :paymentType" +
                " and m.lastUpdateDate <= :rerunTime")
                .setParameter("scheduledStatus", MessageStatus.SCHEDULED)
                .setParameter("paymentType", MessageType.PAYMENT)
                .setParameter("rerunTime", DateTime.now().minusMinutes(5))
                .getResultList();
    }

    public List<PersistentMessage> findNewNotificationMessages() {
        return this.getEntityManager().createQuery("select m from PersistentMessage m where m.status = :addedStatus and m.messageType = :notificationType")
                .setParameter("addedStatus", MessageStatus.ADDED)
                .setParameter("notificationType", MessageType.NOTIFICATION)
                .getResultList();
    }

    public List<PersistentMessage> findFailedNotificationMessages() {
        return this.getEntityManager().createQuery("select m from PersistentMessage m where m.status = :scheduledStatus and m.messageType = :notificationType" +
                " and m.lastUpdateDate <= :rerunTime")
                .setParameter("scheduledStatus", MessageStatus.SCHEDULED)
                .setParameter("notificationType", MessageType.NOTIFICATION)
                .setParameter("rerunTime", DateTime.now().minusMinutes(5))
                .getResultList();
    }

    public List<PersistentMessage> findNewCampaignMessages() {
        return this.getEntityManager().createQuery("select m from PersistentMessage m where m.status = :addedStatus and m.messageType = :campaignType")
                .setParameter("addedStatus", MessageStatus.ADDED)
                .setParameter("campaignType", MessageType.CAMPAIGN)
                .getResultList();
    }
}
