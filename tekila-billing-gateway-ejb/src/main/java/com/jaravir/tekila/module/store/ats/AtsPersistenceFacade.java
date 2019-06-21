package com.jaravir.tekila.module.store.ats;

import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.equip.EquipmentPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.Ats;
import com.jaravir.tekila.module.subscription.persistence.entity.AtsStatus;
import org.apache.log4j.Logger;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by shnovruzov on 5/19/2016.
 */
@Stateless
public class AtsPersistenceFacade extends AbstractPersistenceFacade<Ats> {

    private final static Logger log = Logger.getLogger(AtsPersistenceFacade.class);

    @PersistenceContext
    private EntityManager em;
    @Resource
    private EJBContext ctx;

    public enum Filter implements Filterable {
        NAME("name"),
        ATSINDEX("atsIndex"),
        STATUS("status");

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


    public AtsPersistenceFacade() {
        super(Ats.class);
    }

    protected EntityManager getEntityManager() {
        return em;
    }

    public List<Ats> findAllAts() {
        Query query = em.createQuery("select ats from Ats ats", Ats.class);
        return query.getResultList();
    }

    public Ats findByIndex(String index) {
        try {
            return em.createQuery("select ats from Ats ats where ats.atsIndex = '" + index + "'", Ats.class).getSingleResult();
        } catch (Exception ex) {
            return null;
        }
    }

    public List<Ats> findAllByCustomCriteria(String name, String atsIndex){

        String sql = "select ats from Ats ats " +
                "where ats.atsIndex like '%"+atsIndex+"%' " +
                "or ats.name like '%"+name+"%'";
        try{
            return em.createQuery(sql, Ats.class).getResultList();
        }catch (Exception ex){
            log.error(ex);
            return null;
        }

    }

    public void removeIt(Ats ats) {
        Ats forRemove = em.merge(ats);
        em.remove(forRemove);
    }

    public List<Ats> findByProvider(Long providerId) {
        try {
            return em.createQuery("select ats from Ats ats where ats.provider.id = :providerId", Ats.class).
                    setParameter("providerId", providerId).
                    getResultList();
        } catch (Exception ex) {
            log.error("Exception : {}", ex);
            return new ArrayList<>();
        }
    }
    // for citynet
    public List<Ats> findByProviderByDandigCase() {
        try {
            return em.createQuery("select ats from " +
                    "Ats ats where ats.status = :status and ats.gov <> 0 and (ats.provider.id = 0 or ats.provider IS NULL)", Ats.class).
                    setParameter("status", AtsStatus.ACTIVE).
                    getResultList();
        } catch (Exception ex) {
            log.error("Exception : {}", ex);
            return new ArrayList<>();
        }
    }

    public Ats create(String atsName, String atsIndex, String coor, int dayOfBilling, AtsStatus status) {
        Ats ats = new Ats();
        ats.setName(atsName);
        ats.setAtsIndex(atsIndex);
        ats.setCoor(coor);
        ats.setStatus(status);
        save(ats);
        return update(ats);
    }


}
