package com.jaravir.tekila.base.model;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.service.model.BillingModel;
import com.jaravir.tekila.module.service.model.BillingPrinciple;
import org.apache.log4j.Logger;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by shnovruzov on 8/9/2016.
 */
@Stateless
public class BillingModelPersistenceFacade extends AbstractPersistenceFacade<BillingModel> {

    @PersistenceContext
    private EntityManager em;
    @Resource
    private EJBContext ctx;

    private static final Logger log = Logger.getLogger(BillingModelPersistenceFacade.class);

    public BillingModelPersistenceFacade() {
        super(BillingModel.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public BillingModel find(BillingPrinciple principle) {
        return em.createQuery("select t from BillingModel t where  t.principle=:p", BillingModel.class).setParameter("p", principle).getSingleResult();
    }
}
