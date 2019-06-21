package com.jaravir.tekila.module.store.midipop;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.provision.broadband.devices.Midipop;
import com.jaravir.tekila.provision.broadband.devices.MidipopPort;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Created by shnovruzov on 7/11/2016.
 */
@Stateless
public class MidipopPortPersistenceFacade extends AbstractPersistenceFacade<MidipopPort> {

    @PersistenceContext
    EntityManager em;

    public MidipopPortPersistenceFacade() {
        super(MidipopPort.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public List<MidipopPort> findPortByMidipop(Midipop midipop) {
        try {
            return em.createQuery("select pr from MidipopPort pr where pr.midipop = :midipop order by pr.number", MidipopPort.class)
                    .setParameter("midipop", midipop).getResultList();
        } catch (Exception ex) {
            return null;
        }
    }

    public MidipopPort findPortByMidipopAndPortNumber(Midipop midipop, Integer number, Integer slot) {
        try {
            return em.createQuery("select pr from MidipopPort pr where pr.midipop = :midipop and pr.midipopPort = :number and pr.midipopSlot = :slot", MidipopPort.class)
                    .setParameter("midipop", midipop)
                    .setParameter("number", number)
                    .setParameter("slot", slot)
                    .getSingleResult();
        } catch (Exception ex) {
            return null;
        }
    }

    public boolean isFreePort(Integer port, Midipop midipop) {
        try {
            Object res = em.createQuery("select mp from MidipopPort mp where mp.midipopPort = :port and mp.midipop = :midipop and mp.isOccupied = :status", MidipopPort.class)
                    .setParameter("port", port)
                    .setParameter("status", 1)
                    .setParameter("midipop", midipop).getSingleResult();
            return res == null;
        } catch (Exception ex) {
            return true;
        }
    }
}
