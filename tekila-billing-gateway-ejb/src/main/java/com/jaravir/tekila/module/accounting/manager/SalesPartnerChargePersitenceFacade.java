package com.jaravir.tekila.module.accounting.manager;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.accounting.entity.SalesPartnerCharge;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by sajabrayilov on 11/30/2015.
 */
@Stateless
public class SalesPartnerChargePersitenceFacade extends AbstractPersistenceFacade<SalesPartnerCharge>{
    @PersistenceContext
    private EntityManager em;

    public SalesPartnerChargePersitenceFacade () {
        super(SalesPartnerCharge.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
}
