package com.jaravir.tekila.module.store.mobile;

import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import org.apache.log4j.Logger;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by sajabrayilov on 01.04.2015.
 */
@Stateless
public class CommercialCategoryPersistenceFacade extends AbstractPersistenceFacade<CommercialCategory>{
    @PersistenceContext
    private EntityManager em;
    private final static Logger log = Logger.getLogger(CommercialCategoryPersistenceFacade.class);

    public enum Filter implements Filterable {
        TAX_CATEGORY("taxCategory.id"),
        NAME("name");

        private final String field;
        private MatchingOperation operation;

        Filter (String field) {
            this.field = field;
            operation = MatchingOperation.EQUALS;
        }

        @Override
        public String getField() {
            return field;
        }

        @Override
        public MatchingOperation getOperation() {
            return operation;
        }

        public void setMatchingOperation (MatchingOperation operation) {
            this.operation = operation;
        }
    }

    public CommercialCategoryPersistenceFacade() {
        super(CommercialCategory.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
}
