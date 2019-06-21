/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jaravir.tekila.module.service.persistence.manager;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.service.entity.Resource;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author sajabrayilov
 */
@Stateless
public class ResourcePersistenceFacade extends AbstractPersistenceFacade<Resource> {
    @PersistenceContext
    private EntityManager em;
    
    public ResourcePersistenceFacade() {
        super(Resource.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
    
    public List<Resource> findAll() {
        return em.createQuery("select r from Resource r")
                .getResultList();
    }   
    
    public List<Resource> findAllInList(List<String> idList) {
        return em.createQuery("select r from Resource r where r.id in :idList", Resource.class)
                .setParameter("idList", idList)
                .getResultList();
    }
}
