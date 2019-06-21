package com.jaravir.tekila.module.notification;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.event.notification.channell.NotificationChannelStatus;
import com.jaravir.tekila.module.event.notification.channell.NotificationChannell;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

/**
 * Created by kmaharov on 12.07.2016.
 */
@Stateless
public class NotificationChannelPersistenceFacade extends AbstractPersistenceFacade<NotificationChannelStatus> {
    @PersistenceContext
    EntityManager em;

    public NotificationChannelPersistenceFacade() {
        super(NotificationChannelStatus.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public NotificationChannelStatus getChannelStatus(NotificationChannell channell) {
        EntityManager em = getEntityManager();
        Object channelStatus = null;
        try {
            channelStatus = em.createQuery("SELECT n FROM " + NotificationChannelStatus.class.getName() +
                    " n WHERE n.channell = :channell")
                    .setParameter("channell", channell)
                    .getSingleResult();
        } catch (NoResultException ex) {
            channelStatus = null;
        }
        if (channelStatus != null) {
            return (NotificationChannelStatus) channelStatus;
        }
        return null;
    }
}