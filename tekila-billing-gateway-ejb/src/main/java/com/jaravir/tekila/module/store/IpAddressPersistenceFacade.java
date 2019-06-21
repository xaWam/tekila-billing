package com.jaravir.tekila.module.store;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.store.ip.persistence.IpAddress;
import org.apache.log4j.Logger;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Created by shnovruzov on 10/22/2016.
 */
@Stateless
public class IpAddressPersistenceFacade extends AbstractPersistenceFacade<IpAddress>{
    @PersistenceContext
    private EntityManager em;
    private final static Logger log = Logger.getLogger(IpAddressPersistenceFacade.class);

    public IpAddressPersistenceFacade() {
        super(IpAddress.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public boolean isExist(String address){
        try {
            List<IpAddress> ipList = em.createQuery("select a from IpAddress a where a.addressAsString = :addr", IpAddress.class).setParameter("addr", address).getResultList();
            return (ipList != null && !ipList.isEmpty());
        }catch (Exception ex){
            return false;
        }
    }

    public IpAddress find(String address){
        try {
            List<IpAddress> ipList = em.createQuery("select a from IpAddress a where a.addressAsString = :addr", IpAddress.class).setParameter("addr", address).getResultList();
            return (ipList != null && !ipList.isEmpty()) ? ipList.get(0) : null;
        }catch (Exception ex){
            return null;
        }
    }
}
