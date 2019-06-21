package com.jaravir.tekila.module.store.mobile;

import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import org.apache.log4j.Logger;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by sajabrayilov on 13.03.2015.
 */
@Stateless
public class ImsiPersistenceFacade extends AbstractPersistenceFacade<Imsi> {
    @PersistenceContext private EntityManager em;

    private final static Logger log = Logger.getLogger(ImsiPersistenceFacade.class);

    public enum Filter implements Filterable {
        CATEGORY("category"),
        STATUS("status"),
        MSISDN ("msisdn.value"),
        ICCID("iccid"),
        IDENTIFIER("identifier"),
        PROVIDER("provider.id")
        ;

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

        public void setOperation (MatchingOperation operation) {
            this.operation = operation;
        }
    }

    public ImsiPersistenceFacade () {
        super(Imsi.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
}
