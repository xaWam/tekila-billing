package com.jaravir.tekila.module.store.street;

import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.store.ats.AtsPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.Ats;
import com.jaravir.tekila.module.subscription.persistence.entity.Streets;
import org.apache.log4j.Logger;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Created by shnovruzov on 8/4/2016.
 */
@Stateless
public class StreetPersistenceFacade extends AbstractPersistenceFacade<Streets> {

    private final static Logger log = Logger.getLogger(StreetPersistenceFacade.class);

    @PersistenceContext
    private EntityManager em;
    @Resource
    private EJBContext ctx;

    public StreetPersistenceFacade() {
        super(Streets.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public enum Filter implements Filterable {
        NAME("name"),
        ATSINDEX("atsIndex"),
        STREETINDEX("streetIndex");

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

    public void create(String name, String atsIndex, Long streetIndex) {
        Streets street = new Streets();
        street.setName(name);
        street.setAtsIndex(atsIndex);
        street.setStreetIndex(streetIndex);
        save(street);
    }

    public long getStreetIndex() {
        return (Long) em.createQuery("select MAX(st.streetIndex) from Streets st").getSingleResult();
    }

    @Override
    public Streets find(long id) {
        return em.createQuery("select s from Streets s where s.id=:i", Streets.class).setParameter("i", id).getSingleResult();
    }

    public List<Streets> findByATS(Ats ats) {
        return getEntityManager().createQuery("select s from Streets s where s.atsIndex = '" + ats.getId() + "' order by s.name asc", Streets.class)
                .getResultList();
    }

}
