package com.jaravir.tekila.module.system.reproccessing.log;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.system.reproccessing.ReproccessorLogRecord;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by sajabrayilov on 24.01.2015.
 */
@Stateless
public class ReproccessingLogger extends AbstractPersistenceFacade<ReproccessorLogRecord> {
    @PersistenceContext private EntityManager em;

    public ReproccessingLogger () {
        super(ReproccessorLogRecord.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }


}
