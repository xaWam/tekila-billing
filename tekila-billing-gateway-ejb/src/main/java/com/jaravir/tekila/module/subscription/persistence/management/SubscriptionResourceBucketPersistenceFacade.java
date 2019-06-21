/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jaravir.tekila.module.subscription.persistence.management;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionResourceBucket;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author sajabrayilov
 */
@Stateless
public class SubscriptionResourceBucketPersistenceFacade extends AbstractPersistenceFacade<SubscriptionResourceBucket>{
    @PersistenceContext
    private EntityManager em;

    public SubscriptionResourceBucketPersistenceFacade() {
        super(SubscriptionResourceBucket.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
}
