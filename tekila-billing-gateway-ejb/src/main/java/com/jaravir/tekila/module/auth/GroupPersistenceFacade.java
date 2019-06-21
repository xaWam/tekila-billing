package com.jaravir.tekila.module.auth;

import com.jaravir.tekila.base.auth.persistence.Group;
import com.jaravir.tekila.base.auth.persistence.Module;
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
public class GroupPersistenceFacade extends AbstractPersistenceFacade<Group> {
    @PersistenceContext
    private EntityManager em;

    public GroupPersistenceFacade() {
        super(Group.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public Group findByName (String name) {
        return getEntityManager().createQuery("select g from Group g where g.groupName = :name", Group.class)
        .setParameter("name", name).getSingleResult();
    }

    public enum Filter implements Filterable {
        ID("id"),
        GROUPNAME("groupName"),
        EMAIL("email");

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
