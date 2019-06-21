/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jaravir.tekila.module.service.persistence.manager;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.service.entity.ServiceGroup;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author sajabrayilov
 */
@Stateless
public class ServiceGroupPersistenceFacade extends AbstractPersistenceFacade<ServiceGroup>{
    @PersistenceContext private EntityManager em;
    
    public ServiceGroupPersistenceFacade () {
        super(ServiceGroup.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
    
    public List<ServiceGroup> findAllByProviderID(Long providerID) {
        return em.createQuery("select g from ServiceGroup g join g.provider p where p.id = :providerID")
                .setParameter("providerID", providerID).getResultList();
    }
}
