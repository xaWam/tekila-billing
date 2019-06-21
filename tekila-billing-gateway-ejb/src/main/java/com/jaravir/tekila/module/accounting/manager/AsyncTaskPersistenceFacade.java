package com.jaravir.tekila.module.accounting.manager;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.AsyncProvisioningTask;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * @author ElmarMa on 6/13/2018
 */
@Stateless
public class AsyncTaskPersistenceFacade extends AbstractPersistenceFacade<AsyncProvisioningTask> {

    @PersistenceContext
    private EntityManager em;

    public AsyncTaskPersistenceFacade() {
        super(AsyncProvisioningTask.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
}
