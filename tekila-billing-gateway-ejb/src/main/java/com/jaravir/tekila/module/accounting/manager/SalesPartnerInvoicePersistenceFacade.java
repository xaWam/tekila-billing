package com.jaravir.tekila.module.accounting.manager;

import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.accounting.entity.SalesPartnerInvoice;
import com.jaravir.tekila.module.store.SalesPartnerStore;
import com.jaravir.tekila.module.store.SalesPartnerStorePersistenceFacade;
import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by sajabrayilov on 11/26/2015.
 */

@Stateless
public class SalesPartnerInvoicePersistenceFacade extends AbstractPersistenceFacade<SalesPartnerInvoice>{
    @PersistenceContext
    private EntityManager em;
    private final static Logger log = Logger.getLogger(SalesPartnerInvoice.class);

    public enum Filter implements Filterable {
        PARTNER_ID ("partner.id");
        private String field;
        private MatchingOperation operation;

        Filter (String field) {
            this.field = field;
            operation = MatchingOperation.EQUALS;
        }

        @Override
        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        @Override
        public MatchingOperation getOperation() {
            return operation;
        }

        public void setOperation(MatchingOperation operation) {
            this.operation = operation;
        }
    }

    public SalesPartnerInvoicePersistenceFacade () {
        super(SalesPartnerInvoice.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

}
