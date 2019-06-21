package com.jaravir.tekila.provision.broadband.devices.manager;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.provision.broadband.devices.Port;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by sajabrayilov on 1/6/2015.
 */
@Stateless
public class PortPersistenceFacade extends AbstractPersistenceFacade<Port> {
    @PersistenceContext
    private EntityManager em;

    public PortPersistenceFacade() {
        super(Port.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
}
