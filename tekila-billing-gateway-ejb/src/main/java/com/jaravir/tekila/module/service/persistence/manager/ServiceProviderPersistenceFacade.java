/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jaravir.tekila.module.service.persistence.manager;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.service.entity.ServiceProvider;

import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * @author sajabrayilov
 */
@Stateless
public class ServiceProviderPersistenceFacade extends AbstractPersistenceFacade<ServiceProvider> {
    @PersistenceContext
    private EntityManager em;

    public ServiceProviderPersistenceFacade() {
        super(ServiceProvider.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return this.em;
    }

    public List<ServiceProvider> findAll() {
        return em.createQuery("select p from ServiceProvider p", ServiceProvider.class)
                .getResultList();
    }

    public List<ServiceProvider> findAllNotIn(long id) {
        return em.createQuery("select p from ServiceProvider p where p.id != :p_id", ServiceProvider.class)
                .setParameter("p_id", id)
                .getResultList();
    }

    public ServiceProvider findByName(String name) {
        return em.createQuery("select p from ServiceProvider p where p.name=:name", ServiceProvider.class)
                .setParameter("name", name)
                .getSingleResult();
    }

}
