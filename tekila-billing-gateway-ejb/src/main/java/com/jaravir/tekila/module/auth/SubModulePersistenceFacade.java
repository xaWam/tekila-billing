package com.jaravir.tekila.module.auth;

import com.jaravir.tekila.base.auth.persistence.Module;
import com.jaravir.tekila.base.auth.persistence.SubModule;
import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by sajabrayilov on 12/8/2014.
 */
@Stateless
public class SubModulePersistenceFacade extends AbstractPersistenceFacade<SubModule> {
    @PersistenceContext
    private EntityManager em;

    public SubModulePersistenceFacade() {
        super(SubModule.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public SubModule find (String name) {
        return getEntityManager().createQuery("select sub from SubModule sub where LOWER(sub.name) = :sub", SubModule.class)
            .setParameter("sub", name).getSingleResult();
    }

    public enum Filter implements Filterable {
        ID("id"),
        MODULE("module.name"),
        NAME("name");

        private final String field;
        private MatchingOperation operation;

        Filter(String field) {
            this.field = field;
            this.operation = MatchingOperation.LIKE;
        }

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
}
