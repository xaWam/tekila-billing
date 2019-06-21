package com.jaravir.tekila.module.subscription.persistence.management;

import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscriber;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriberLifeCycleType;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;

/**
 *
 * @author sajabrayilov
 */
@Stateless
public class SubscriberPersistenceFacade extends AbstractPersistenceFacade<Subscriber> {

    @PersistenceContext
    private EntityManager em;


    public enum Filter implements Filterable {

        ID("id"),
        PASSPORTNUMBER("details.passportNumber"),
        NAME("details.firstName"),
        MIDDLENAME("details.middleName"),
        SURNAME("details.surname"),
        PIN("details.pinCode");

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

    public SubscriberPersistenceFacade() {
        super(Subscriber.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return this.em;
    }

    public void create() {
        Subscriber sub = new Subscriber();
    }

    public List<Subscriber> findAll() {
        return em.createQuery("select s from Subscriber s", Subscriber.class)
                .getResultList();
    }

    public SubscriberLifeCycleType getLifeCycleTypeBySubscriberId(long subscriber_id) {
        return em.createQuery("select s.lifeCycle from Subscriber s where s.id = :sub_id", SubscriberLifeCycleType.class)
                .setParameter("sub_id", subscriber_id).getSingleResult();
    }

    public String getSubsciberSqlQuery(Map<Filterable, Object> filters) {

        String sqlQuery = "select distinct sub from Subscriber sub ";
        String where = "where ";
        String and = "";

        if (filters.get(SubscriberPersistenceFacade.Filter.ID) != null) {
            where += "sub.id like '%" + filters.get(SubscriberPersistenceFacade.Filter.ID) + "%'";
            and = " and ";
            filters.remove(SubscriberPersistenceFacade.Filter.ID);
        }

        if (filters.get(SubscriberPersistenceFacade.Filter.PASSPORTNUMBER) != null) {
            where += and;
            where += "lower(sub.details.passportNumber) like '%" + filters.get(SubscriberPersistenceFacade.Filter.PASSPORTNUMBER).toString().toLowerCase() + "%'";
            and = " and ";
            filters.remove(SubscriberPersistenceFacade.Filter.PASSPORTNUMBER);
        }

        if (filters.get(SubscriberPersistenceFacade.Filter.NAME) != null) {
            where += and;
            where += "lower(sub.details.firstName) like '%" + filters.get(SubscriberPersistenceFacade.Filter.NAME).toString().toLowerCase() + "%'";
            and = " and ";
            filters.remove(SubscriberPersistenceFacade.Filter.NAME);
        }

        if (filters.get(Filter.MIDDLENAME) != null) {
            where += and;
            where += "lower(sub.details.middleName) like '%" + filters.get(Filter.MIDDLENAME).toString().toLowerCase() + "%'";
            and = " and ";
            filters.remove(Filter.MIDDLENAME);
        }

        if (filters.get(SubscriberPersistenceFacade.Filter.SURNAME) != null) {
            where += and;
            where += "lower(sub.details.surname) like '%" + filters.get(SubscriberPersistenceFacade.Filter.SURNAME).toString().toLowerCase() + "%'";
            filters.remove(SubscriberPersistenceFacade.Filter.SURNAME);
        }

        return sqlQuery + where;
    }

    public void removeIt(Subscriber subscriber) {
        Subscriber forRemove = em.merge(subscriber);
        em.remove(forRemove);
    }

    public Subscriber updateIt(Subscriber subscriber){
        return update(subscriber);
    }

}
