package com.jaravir.tekila.extern.azertelecom.payment;

import com.jaravir.tekila.base.entity.BaseEntity;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by sajabrayilov on 04.02.2015.
 */
@Stateless
public class FttxPaymentRecordFacade extends AbstractPersistenceFacade<FttxPaymentRecord> {
    @PersistenceContext
    private EntityManager em;

    public FttxPaymentRecordFacade () {
        super(FttxPaymentRecord.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
}
