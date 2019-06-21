/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jaravir.tekila.provision.broadband.devices.manager;

import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.common.device.DeviceStatus;
import com.jaravir.tekila.module.service.entity.ServiceProvider;
import com.jaravir.tekila.module.store.equip.Equipment;
import com.jaravir.tekila.module.subscription.persistence.entity.Ats;
import com.jaravir.tekila.provision.broadband.devices.Midipop;
import com.jaravir.tekila.provision.broadband.devices.MiniPop;
import com.jaravir.tekila.provision.broadband.devices.MinipopCategory;
import com.jaravir.tekila.provision.broadband.devices.Port;
import com.jaravir.tekila.provision.broadband.devices.exception.NoFreePortLeftException;
import com.jaravir.tekila.provision.broadband.devices.exception.PortAlreadyReservedException;
import org.apache.log4j.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

/**
 * @author sajabrayilov
 */
@Stateless
public class MiniPopPersistenceFacade extends AbstractPersistenceFacade<MiniPop> {

    @PersistenceContext
    private EntityManager em;
    private final static Logger log = Logger.getLogger(MiniPopPersistenceFacade.class);
    @EJB
    private PortPersistenceFacade portFacade;

    public enum Filter implements Filterable {

        MAC_ADDRESS("mac"),
        ADDRESS("address"),
        IP("ip"),
        SWITCH_ID("switch_id"),
        STATUS("deviceStatus"),
        PORT("ports_number"),
        ATS("ats"),
        NAS("nas"),
        MIDIPOP("midipop"),
        MASTERVLAN("masterVlan"),
        CATEGORY("category"),
        HOUSE("houses"),
        PROVIDER("provider");

        private final String field;
        private MatchingOperation operation;

        Filter(String field) {
            this.field = field;
            operation = MatchingOperation.LIKE;
        }

        @Override
        public String getField() {
            return field;
        }

        @Override
        public MatchingOperation getOperation() {
            return operation;
        }

        public void setOperation(MatchingOperation operation) {
            this.operation = operation;
        }
    }

    public MiniPopPersistenceFacade() {
        super(MiniPop.class);
    }

    public Port getAvailablePort(MiniPop miniPop) throws NoFreePortLeftException, PortAlreadyReservedException {
        Port port = null;
//        log.debug("Minipop received: " + miniPop); // Logu doldurur
        log.debug("Reserved port list: " + miniPop.getReservedPortsAsText());
        //if (miniPop != null ) return null;
        if (miniPop.getPreferredPort() != null && miniPop.checkPort(miniPop.getPreferredPort())) {
            port = miniPop.reserve(miniPop.getPreferredPort());
        } else {
            port = miniPop.getNextAvailablePort();
        }
        portFacade.save(port);
        update(miniPop);
        return port;
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @Override
    public void save(MiniPop entity) {
        entity.setDeviceStatus(DeviceStatus.INITIAL);
        super.save(entity);
    }

    public boolean isDuplicate(String mac) {
        long count = em.createQuery("select count(m) from MiniPop m where m.mac = :mac", Long.class)
                .setParameter("mac", mac).getSingleResult();

        return count > 0;
    }

    public boolean isMidipopPortReserved(Midipop midipop, Integer slot, Integer port) {
        try {
            return em.createQuery("select m from MiniPop m where m.midipop = :midipop and m.midipopSlot = :slot and m.midipopPort = :port", MiniPop.class)
                    .setParameter("midipop", midipop)
                    .setParameter("slot", slot)
                    .setParameter("port", port)
                    .getResultList().size() > 1;
        } catch (Exception ex) {
            log.debug("Exception: " + ex);
            return false;
        }
    }

    public List<MiniPop> findByFilters(ServiceProvider provider, Ats ats, DeviceStatus status) {
        if (ats == null)
            return em.createQuery("select m  from MiniPop m where m.provider = :provider and m.deviceStatus = :status", MiniPop.class)
                    .setParameter("provider", provider)
                    .setParameter("status", status).getResultList();
        else
            return em.createQuery("select m  from MiniPop m where m.provider = :provider and m.ats = :ats and m.deviceStatus = :status", MiniPop.class)
                    .setParameter("provider", provider)
                    .setParameter("ats", ats)
                    .setParameter("status", status).getResultList();

    }

    public List<MiniPop> findByAts(Ats ats, DeviceStatus status) {
            return em.createQuery("select m  from MiniPop m where m.ats = :ats and m.deviceStatus = :status", MiniPop.class)
                    .setParameter("ats", ats)
                    .setParameter("status", status).getResultList();

    }

    public List<MiniPop> findByCategory(ServiceProvider provider, MinipopCategory category) {
        try {
            return em.createQuery("select m from MiniPop m where m.provider = :provider and m.category = :category and m.deviceStatus = :status", MiniPop.class)
                    .setParameter("provider", provider)
                    .setParameter("category", category)
                    .setParameter("status", DeviceStatus.ACTIVE)
                    .getResultList();
        } catch (Exception ex) {
            return null;
        }
    }

    public MiniPop findBySwitchId(String switchId) {
        try {
            return em.createQuery("select m from MiniPop m where m.switch_id = :switchId", MiniPop.class)
                    .setParameter("switchId", switchId)
                    .getSingleResult();
        } catch (Exception ex) {
            return null;
        }
    }

//
//    public List<MiniPop> findByProviderId(Long providerId){
//        try{
//            return em.createQuery("select m from MiniPop m where m.provider_id = :providerId", MiniPop.class)
//                    .setParameter("providerId", providerId)
//                    .getResultList();
//        }catch (Exception ex){
//            return null;
//        }
//    }

    public List<MiniPop> findByProviderId(Long providerId){
            log.debug("~~~~" + providerId + "~~~~");
        try{
            return em.createQuery("select m from MiniPop m where m.provider.id = :providerId ", MiniPop.class)
                    .setParameter("providerId", providerId)
                    .getResultList();
        }catch (Exception ex){
            return null;
        }
    }

}
