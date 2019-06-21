package com.jaravir.tekila.module.store;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.store.scratchcard.persistence.entity.ScratchCardBlockingSetting;
import org.apache.log4j.Logger;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by shnovruzov on 8/15/2016.
 */
@Stateless
public class ScratchCardSettingFacade extends AbstractPersistenceFacade<ScratchCardBlockingSetting>{

    @Resource
    private SessionContext ctx;
    @PersistenceContext
    private EntityManager em;
    private final static Logger log = Logger.getLogger(ScratchCardSettingFacade.class);

    public ScratchCardSettingFacade() {
        super(ScratchCardBlockingSetting.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

}
