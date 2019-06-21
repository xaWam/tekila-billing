package com.jaravir.tekila.engines;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.engines.*;
import com.jaravir.tekila.module.service.entity.ServiceProvider;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;

import javax.ejb.*;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by khsadigov on 5/16/2017.
 */

@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class EnginePersistenceFacade extends AbstractPersistenceFacade<Engine> {
    @PersistenceContext
    private EntityManager em;

    public EnginePersistenceFacade() {
        super(Engine.class);
    }


    @Override
    protected EntityManager getEntityManager() {
        return this.em;
    }


    public String getJndiName(ServiceProvider provider, EngineType type) {
        return em.createQuery("select e.jndiName from Engine e where e.type = :type and e.provider = :provider ").setParameter("type", type).setParameter("provider", provider).getSingleResult().toString();
    }

}
