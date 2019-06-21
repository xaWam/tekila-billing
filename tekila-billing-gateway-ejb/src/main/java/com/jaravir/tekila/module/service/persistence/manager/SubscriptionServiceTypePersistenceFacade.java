package com.jaravir.tekila.module.service.persistence.manager;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.service.entity.SubscriptionServiceType;
import com.jaravir.tekila.module.service.entity.Zone;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class SubscriptionServiceTypePersistenceFacade
        extends AbstractPersistenceFacade<SubscriptionServiceType> {
    @PersistenceContext
    private EntityManager em;

    public SubscriptionServiceTypePersistenceFacade() {
        super(SubscriptionServiceType.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
}
