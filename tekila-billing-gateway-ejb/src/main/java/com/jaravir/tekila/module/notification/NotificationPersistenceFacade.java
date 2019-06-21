package com.jaravir.tekila.module.notification;

import com.jaravir.tekila.base.entity.Language;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.event.BillingEvent;
import com.jaravir.tekila.module.event.notification.Notification;
import com.jaravir.tekila.module.event.notification.channell.NotificationChannell;
import com.jaravir.tekila.module.service.persistence.manager.NotificationSettingPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberLifeCycleType;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberType;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import org.apache.log4j.Logger;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Arrays;
import java.util.List;

/**
 * Created by sajabrayilov on 12/1/2014.
 */
@Stateless
public class NotificationPersistenceFacade extends AbstractPersistenceFacade<Notification> {
   @PersistenceContext
    private EntityManager em;
    private final static Logger log = Logger.getLogger(NotificationSettingPersistenceFacade.class);

    public NotificationPersistenceFacade () {
        super(Notification.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public Notification findNotification (BillingEvent event, NotificationChannell channell, Language lang, SubscriberLifeCycleType lifeCycleType, SubscriberType subscriberType) {
        List<Notification> notificationList = em.createQuery("select n from Notification n where n.event = :event and n.channell = :chan and n.lang = :lang")
                .setParameter("event", event).setParameter("chan", channell).setParameter("lang", lang)
                .getResultList();
        return !notificationList.isEmpty() ? notificationList.get(0) : null;
    }

    public List<Notification> findNotification (BillingEvent event, Subscription subscription) {
        if (event == null)
            throw new IllegalArgumentException("findNotifiction: event cannot be null");

        if (subscription == null)
            throw new IllegalArgumentException("findNotifiction: subscription cannot be null");

        log.debug(String.format("Parameters: event=%s, subscription=%d",
                event.toString(),
                subscription.getId()
        ));
        List<Notification> notificationList = em.createQuery("select n from Notification n left join  n.serviceList s" +
                " left join n.providerList p " +
                " where n.event = :event and n.lang = :lang "
                + " and (s.id = :serv or p.id = :provID) "
                // + " and (n.service.id = :srvID or n.service.id is null) "
                + " and (n.subscriberType = :subType or n.subscriberType is null) "
                + "and (n.lifeCycleType = :cycle or n.lifeCycleType is null)"
                , Notification.class)
                .setParameter("event", event)
                .setParameter("lang", subscription.getSubscriber().getDetails().getLanguage())
                .setParameter("provID", subscription.getService().getProvider().getId())
                .setParameter("serv", subscription.getService().getId())
                //.setParameter("srvID", subscription.getService().getId())
                .setParameter("subType", subscription.getSubscriber().getDetails().getType())
                .setParameter("cycle", subscription.getSubscriber().getLifeCycle())
                .getResultList();

        return !notificationList.isEmpty() ? notificationList : null;
    }

    @Override
    public void save(Notification entity) {
        super.save(entity);
        log.debug("Notification after creattion: " + entity);
    }
}
