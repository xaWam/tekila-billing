package com.jaravir.tekila.module.subscription.persistence.management;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.convergent.ConvergentQueue;

import javax.ejb.*;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;


/**
 * Created by khsadigov on 12/1/2016.
 */

@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class ConvergentQueueFacade extends AbstractPersistenceFacade<ConvergentQueue> {

    @PersistenceContext
    private EntityManager em;

    public ConvergentQueueFacade() {
        super(ConvergentQueue.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return this.em;
    }

    public List<ConvergentQueue> findAll() {
        return this.em.createQuery("select p from ConvergentQueue p where p.status=0 " +
                "order by p.lastUpdateDate", ConvergentQueue.class).getResultList();
    }
}
