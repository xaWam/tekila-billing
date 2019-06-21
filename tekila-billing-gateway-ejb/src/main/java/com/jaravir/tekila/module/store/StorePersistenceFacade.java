package com.jaravir.tekila.module.store;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.store.ip.persistence.IpAddress;
import com.jaravir.tekila.module.store.ip.IpAddressStatus;
import org.apache.log4j.Logger;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by sajabrayilov on 11/14/2014.
 */
@Stateless
public class StorePersistenceFacade extends AbstractPersistenceFacade<IpAddress>{
    @PersistenceContext
    private EntityManager em;
    private static final Logger log = Logger.getLogger(StorePersistenceFacade.class);

    public StorePersistenceFacade() {
        super(IpAddress.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public IpAddress findOneAvailable() {
        return em.createQuery("select ad from IpAddress where ad.code = :code order by id", IpAddress.class)
           .setParameter("code", IpAddressStatus.AVAILABLE)
           .setMaxResults(1)
           .getSingleResult();
    }

    
}
