package com.jaravir.tekila.module.auth;

import com.jaravir.tekila.base.auth.persistence.Module;
import com.jaravir.tekila.base.auth.persistence.SubModule;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by sajabrayilov on 12/8/2014.
 */
@Stateless
public class ModulePersistenceFacade extends AbstractPersistenceFacade<Module> {
    @PersistenceContext
    private EntityManager em;

    public ModulePersistenceFacade() {
        super(Module.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
}
