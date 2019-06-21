package com.jaravir.tekila.module.subscription.persistence.management;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.module.subscription.persistence.entity.reactivation.ReactivationStatus;
import com.jaravir.tekila.module.subscription.persistence.entity.reactivation.SubscriptionReactivation;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Created by sajabrayilov on 7/3/2015.
 */
@Stateless
public class SubscriptionReactivationPersistenceFacade extends AbstractPersistenceFacade<SubscriptionReactivation>{
    @PersistenceContext
    private EntityManager em;

    private final static Logger log = Logger.getLogger(SubscriptionReactivationPersistenceFacade.class);

    public SubscriptionReactivationPersistenceFacade() {
        super(SubscriptionReactivation.class);
    }

    @Override
    public EntityManager getEntityManager() {
        return em;
    }

    public SubscriptionReactivation findPendingReactivation (Subscription subscription) {
        List<SubscriptionReactivation> reactivationList = em.createQuery("select r from SubscriptionReactivation r where r.subscription.id = :id and r.status =:status order by r.id desc")
            .setParameter("id", subscription.getId()).setParameter("status", ReactivationStatus.PENDING).getResultList();

        return reactivationList != null && !reactivationList.isEmpty() ? reactivationList.get(0) : null;
    }

    public void add (Subscription subscription) {
        SubscriptionReactivation reactivation = new SubscriptionReactivation();
        reactivation.setCreatedDate(DateTime.now());
        reactivation.setSubscription(subscription);
        reactivation.setStatus(ReactivationStatus.PENDING);
        save(reactivation);
    }

    public void reactivate (SubscriptionReactivation reactivation) {
        reactivation.setStatus(ReactivationStatus.SUCCESS);
        reactivation.setReactivatedDate(DateTime.now());
        update(reactivation);
    }

    public SubscriptionReactivation findLast (Subscription subscription) {
        List<SubscriptionReactivation> reactivationList = em.createQuery("select r from SubscriptionReactivation r where r.subscription.id = :sbn_id order by r.id desc")
                .setParameter("sbn_id", subscription.getId()).getResultList();
        return reactivationList != null && !reactivationList.isEmpty() ? reactivationList.get(0) : null;
    }
}
