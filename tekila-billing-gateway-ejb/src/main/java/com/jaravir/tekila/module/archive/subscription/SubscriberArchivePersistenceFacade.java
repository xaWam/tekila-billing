package com.jaravir.tekila.module.archive.subscription;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberDetails;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 * Created by sajabrayilov on 22.01.2015.
 */
@Stateless
public class SubscriberArchivePersistenceFacade extends AbstractPersistenceFacade<SubscriberArchived> {
    @PersistenceContext
    private EntityManager em;
    @Resource private SessionContext ctx;
    @EJB private UserPersistenceFacade userFacade;

    public enum Filter implements Filterable {
        SUBSCRIBER_ID("subscriber.id");

        private final String code;
        private MatchingOperation operation;

        Filter (final String code) {
            this.code = code;
            this.operation = MatchingOperation.EQUALS;
        }

        @Override
        public String getField() {
            return code;
        }

        @Override
        public MatchingOperation getOperation() {
            return operation;
        }

        public void setOperation(MatchingOperation operation) {
            this.operation = operation;
        }
    }

    public SubscriberArchivePersistenceFacade() {
        super(SubscriberArchived.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public void archive (SubscriberDetails entity) {
        SubscriberArchived archivedEntity = new SubscriberArchived(entity);
        User user = userFacade.findByUserName(ctx.getCallerPrincipal().getName());
        archivedEntity.setUser(user);
        save(archivedEntity);
    }

    public SubscriberArchived find (Long subscriberID, Long version) {
        return em.createQuery("select s from SubscriberArchived s where s.subscriber.id = :sub_id and s.objectVersion = :ver", SubscriberArchived.class)
                .setParameter("sub_id", subscriberID)
                .setParameter("ver", version)
                .getSingleResult();
    }
   /* @Override
    public Query getPaginatedQueryWithFilters() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<SubscriberArchived> query = cb.createQuery(SubscriberArchived.class);
        Root<SubscriberArchived> root = query.from(SubscriberArchived.class);

        if (getFilters() != null && !getFilters().isEmpty())
            return em.createQuery(query.where(getPredicateWithFilters(cb, root)));

        return getPaginatedQuery();
    }*/


}
