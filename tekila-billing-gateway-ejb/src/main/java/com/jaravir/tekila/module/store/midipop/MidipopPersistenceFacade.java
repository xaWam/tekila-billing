package com.jaravir.tekila.module.store.midipop;

import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.store.ats.AtsPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.Ats;
import com.jaravir.tekila.provision.broadband.devices.Midipop;
import org.apache.log4j.Logger;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Created by shnovruzov on 7/8/2016.
 */
@Stateless
public class MidipopPersistenceFacade extends AbstractPersistenceFacade<Midipop> {

    private final static Logger log = Logger.getLogger(MidipopPersistenceFacade.class);

    @PersistenceContext
    private EntityManager em;

    @Resource
    private EJBContext ctx;

    public MidipopPersistenceFacade() {
        super(Midipop.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public enum Filter implements Filterable {
        STATUS("status"),
        IP("switchIp"),
        ATS("ats"),
        VLAN("vlan");


        private final String field;
        private MatchingOperation operation;

        Filter(String field) {
            this.field = field;
            this.operation = MatchingOperation.LIKE;
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

    public void create(String name, int status, int cable, int nodes, String ip, int port, int vlan, Ats ats) {
        Midipop midipop = new Midipop();
        midipop.setName(name);
        midipop.setStatus(status);
        midipop.setCable(cable);
        midipop.setNodes(nodes);
        midipop.setSwitchPort(port);
        midipop.setSwitchIp(ip);
        midipop.setVlan(vlan);
        midipop.setAts(ats);
        save(midipop);
    }

    public boolean findMidipopByVlan(Integer vlan) {
        try {
            Object res = em.createQuery("select mdp from Midipop mdp where mdp.vlan = :vlan", Midipop.class)
                    .setParameter("vlan", vlan).getSingleResult();
            return res != null;
        } catch (Exception ex) {
            return false;
        }
    }

    public List<Midipop> findMidipopByAts(Ats ats) {
        try {
            return em.createQuery("select mdp from Midipop mdp where mdp.ats = :ats", Midipop.class)
                    .setParameter("ats", ats).getResultList();
        } catch (Exception ex) {
            return null;
        }
    }
}
