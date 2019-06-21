package com.jaravir.tekila.module.subscription.persistence.management;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionVAS;
import org.apache.log4j.Logger;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Created by shnovruzov on 9/27/2016.
 */
@Stateless
public class SubscriptionVASPersistenceFacade extends AbstractPersistenceFacade<SubscriptionVAS>{

    private final static Logger log = Logger.getLogger(SubscriptionVASPersistenceFacade.class);

    @PersistenceContext
    private EntityManager em;
    @Resource
    private SessionContext ctx;
    public SubscriptionVASPersistenceFacade(){super(SubscriptionVAS.class);}

    @Override
    protected EntityManager getEntityManager() {
        return this.em;
    }

    public List<SubscriptionVAS> findBySubscription(long subscriptionId) {
        EntityManager em = getEntityManager();
        return em.createQuery("select svas from SubscriptionVAS svas where svas.subscription.id = :subscriptionId")
                .setParameter("subscriptionId", subscriptionId)
                .getResultList();
    }

    public void removeIt(Long id) {
        SubscriptionVAS subscriptionVAS = find(id);
        SubscriptionVAS forRemove = em.merge(subscriptionVAS);
        em.remove(forRemove);
    }
}
