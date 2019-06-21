package com.jaravir.tekila.module.subscription.persistence.management;

import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.Dealer;
import org.apache.log4j.Logger;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Created by elmarmammadov on 6/1/2017.
 */
@Stateless
public class DealerPersistanceFacade extends AbstractPersistenceFacade<Dealer> {
    public DealerPersistanceFacade() {
        super(Dealer.class);
    }

    @PersistenceContext
    private EntityManager em;
    private final static Logger log = Logger.getLogger(DealerPersistanceFacade.class);

    public enum Filter implements Filterable {
        NAME("name");
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
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }


    public List<Dealer> findByFilters(String name) {
        return em.createQuery("select d  from Dealer d where  d.name = :name", Dealer.class)
                .setParameter("name", name).getResultList();
    }

    public List<Dealer> findByProvider(Long providerId) {
        return em.createQuery("select d  from Dealer d where  d.provider.id = :provId", Dealer.class)
                .setParameter("provId", providerId).getResultList();
    }

}
