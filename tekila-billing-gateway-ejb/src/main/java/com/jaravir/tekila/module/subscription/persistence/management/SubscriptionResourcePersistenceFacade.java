/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jaravir.tekila.module.subscription.persistence.management;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 *
 * @author sajabrayilov
 */
@Stateless
public class SubscriptionResourcePersistenceFacade extends AbstractPersistenceFacade<SubscriptionResource>{

    private static final Logger log = LoggerFactory.getLogger(SubscriptionResourcePersistenceFacade.class);

    @PersistenceContext
    private EntityManager em;
    
    public SubscriptionResourcePersistenceFacade() {
        super(SubscriptionResource.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }


    public List<SubscriptionResource> findSubscriptionResourceBySubscriptionId(Long subscriptionId) {
        try {

            List<SubscriptionResource> subscriptionResources = em.createQuery("select sr from Subscription sub " +
                    "left join fetch sub.resources sr " +
                    "left join fetch sr.bucketList " +
                    "where sub.id = :subscriptionId " , SubscriptionResource.class)
                    .setParameter("subscriptionId", subscriptionId).getResultList();

            return subscriptionResources;
        } catch (NoResultException ex) {
            log.error("SubscriptionResource => findSubscriptionResourceBySubscription "+ex);
            return null;
        }
    }


    
}
