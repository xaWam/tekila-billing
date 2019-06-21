/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jaravir.tekila.module.service.persistence.manager;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.service.entity.ResourceBucket;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author sajabrayilov
 */
@Stateless
public class ResourceBucketPersistenceFacade extends AbstractPersistenceFacade<ResourceBucket> {
    @PersistenceContext
    private EntityManager em;
    @EJB private ServicePersistenceFacade serviceFacade;

    public ResourceBucketPersistenceFacade() {
        super(ResourceBucket.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
    
    public List<ResourceBucket> findAll() {
        return em.createQuery("select r from ResourceBucket r", ResourceBucket.class)
                .getResultList();
    }
    
    public List<ResourceBucket> findAllInList(List<String> pkList) {
        return em.createQuery("select r from ResourceBucket r where r.id in :idList ", ResourceBucket.class)
                .setParameter("idList", pkList)
                .getResultList();
    }
    
    public List<ResourceBucket> findAllNotInList(List<String> pkList) {
        return em.createQuery("select r from ResourceBucket r where r.id not in :idList ", ResourceBucket.class)
                .setParameter("idList", pkList)
                .getResultList();
    }

}
