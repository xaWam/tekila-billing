package com.jaravir.tekila.base.persistence.facade;

import com.jaravir.tekila.base.entity.BaseEntity;
import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.Paginable;
import com.jaravir.tekila.module.accounting.entity.Payment;
import com.jaravir.tekila.module.service.ResourceBucketType;
import com.jaravir.tekila.module.subscription.persistence.management.SubscriptionPersistenceFacade;
import org.apache.log4j.Logger;
import org.eclipse.persistence.config.CascadePolicy;
import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;

import javax.persistence.*;
import javax.persistence.criteria.*;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import spring.Filters;

public abstract class AbstractPersistenceFacade<T extends BaseEntity> implements Paginable<T>, Serializable {

    private final static Logger log = Logger.getLogger(AbstractPersistenceFacade.class);
    Class<T> cl;
    private Map<Filterable, Object> filters;
    private PredicateJoinOperation predicateJoinOperation = PredicateJoinOperation.AND;
    private Ordering ordering = Ordering.DESC;
    private Map<String, Ordering> orderingMap;

    public enum Ordering {

        ASC, DESC;
    }

    public enum PredicateJoinOperation {

        AND, OR;
    }

    public Map<Filterable, Object> getFilters() {
        if (filters == null) {
            filters = new HashMap<>();
        }

        return filters;
    }

    public void setFilters(Map<Filterable, Object> filters) {

        this.filters = filters;
    }

    @Override
    public void addFilter(Filterable key, Object value) {

        getFilters().put(key, value);
    }

    @Override
    public void clearFilters() {
        try {
            if (getFilters().size() > 0) {
                getFilters().clear();
            }
        } catch (Exception e) {

        }
    }

    @Override
    public Filterable getFilterByCode(String code) {
        try {
            Class[] classList = getClass().getDeclaredClasses();
            Class filterClass = null;

            outer:
            for (int i = 0; i < classList.length; i++) {
                log.debug("getFilterByCode: declaredClass=" + classList[i].getSimpleName());

                Class[] interfaceList = classList[i].getInterfaces();

                for (int j = 0; j < interfaceList.length; j++) {
                    log.debug("getFilterByCode: interface=" + interfaceList[j].getSimpleName());
                    if (interfaceList[j].getSimpleName().equals("Filterable")) {
                        filterClass = classList[i];
                        break outer;
                    }
                }
            }
            log.debug("getFilterByCode: filterClass=" + filterClass);

            if (filterClass != null) {
                Filterable[] filterValues = (Filterable[]) filterClass.getMethod("values").invoke(null);
                log.debug("getFilterByCode: filterValues=" + filterValues);

                for (Filterable fl : filterValues) {
                    log.debug("getFilterByCode: filter=" + fl);
                    if (fl.getField().equals(code)) {
                        return fl;
                    }
                }
            }

            return null;
        } catch (Exception ex) {
            log.error("No such field found: " + code, ex);
            return null;
        }
    }

    public Ordering getOrdering() {
        return ordering;
    }

    public void setOrdering(Ordering ordering) {
        this.ordering = ordering;
    }

    public Map<String, Ordering> getOrderingMap() {
        if (orderingMap == null) {
            orderingMap = new HashMap<>();
        }
        return orderingMap;
    }

    public void setOrderingMap(Map<String, Ordering> orderingMap) {
        this.orderingMap = orderingMap;
    }

    public Map<String, Ordering> addOrdering(String property, Ordering ordering) {
        getOrderingMap().put(property, ordering);
        return getOrderingMap();
    }

    public PredicateJoinOperation getPredicateJoinOperation() {
        return predicateJoinOperation;
    }

    public void setPredicateJoinOperation(PredicateJoinOperation predicateJoinOperation) {
        this.predicateJoinOperation = predicateJoinOperation;
    }

    public AbstractPersistenceFacade(Class<T> cl) {
        this.cl = cl;
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+4"));
        System.setProperty("user.timezone", "GMT+4");
        DateTimeZone.setDefault(DateTimeZone.forTimeZone(TimeZone.getTimeZone("GMT+4")));
    }

    protected abstract EntityManager getEntityManager();

    /*public Query getPaginatedQueryWithFilters() {
     return getPaginatedQuery();
     }*/
    public T find(long pk) {
        return (T) this.getEntityManager().find(cl, pk);
    }

    public T find(long pk, LockModeType lockModeType) {
        return (T) getEntityManager().find(cl, pk, lockModeType);
    }

    public T findForceRefresh(long pk) {
        HashMap props = new HashMap();
        props.put(QueryHints.REFRESH, HintValues.TRUE);
        props.put(QueryHints.REFRESH_CASCADE, CascadePolicy.CascadeAllParts);
        return (T) this.getEntityManager().find(cl, pk, props);
    }

    public T findForceRefresh(long pk, LockModeType lockModeType) {
        HashMap props = new HashMap();
        props.put(QueryHints.REFRESH, HintValues.TRUE);
        props.put(QueryHints.REFRESH_CASCADE, CascadePolicy.CascadeAllParts);
        return (T) getEntityManager().find(cl, pk, lockModeType, props);
    }

    public T findReadFromStore(long pk) {
        try {
            return getEntityManager().createQuery(String.format("select e from %s e where e.id = :e_id", cl.getName()), cl)
                    .setParameter("e_id", pk)
                    .getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

    public T findByName(String modelName) {
        try {
            return getEntityManager().createQuery("select m from " + cl.getName() + " m where m.name = :name", cl)
                    .setParameter("name", modelName).getSingleResult();
        } catch (Exception ex) {
            return null;
        }
    }

    public void save(T entity) {
        this.getEntityManager().persist(entity);
    }

    public T update(T entity) {
        return this.getEntityManager().merge(entity);
    }

    public void reset(T entity) {
        this.getEntityManager().refresh(entity);
    }

    public void delete(T entity) {
        this.getEntityManager().remove(entity);
    }

    protected Query getPaginatedQuery() {
        log.debug(" cl.getName():  => " + cl.getName());
        return getEntityManager().createQuery("select s from " + cl.getName() + " s order by s.id desc", cl);
    }

    protected Query getPaginatedQueryDynamic(String sqlQuery) {
        return getEntityManager().createQuery(sqlQuery, cl);
    }

    private Query getPaginatedQueryWithFiltersDynamic(String sqlQuery) {
        Query query = getEntityManager().createQuery(sqlQuery, cl);

        log.debug("getPaginatedQueryWithFiltersDynamic filters: " + filters);

        for (Filterable filter : getFilters().keySet()) {
            try {
                query.getParameter(filter.getField());
                query.setParameter(filter.getField(), getFilters().get(filter));
            } catch (Exception e) {
                return null;
            }
        }

        return query;
    }

    private Query getPaginatedQueryWithFiltersDynamic(String sqlQuery, Filters filters) {
        Query query = getEntityManager().createQuery(sqlQuery, cl);

        log.debug("getPaginatedQueryWithFiltersDynamic filters: " + filters.getFilters());

        for (Filterable filter : filters.getFilters().keySet()) {
            try {
                query.getParameter(filter.getField());
                query.setParameter(filter.getField(), filters.getFilters().get(filter));
            } catch (Exception e) {
                return null;
            }
        }

        return query;
    }

    @Override
    public List<T> findAllPaginatedDynamic(int first, int pageSize, String sqlQuery) {

        Query query = !getFilters().isEmpty() ? getPaginatedQueryWithFiltersDynamic(sqlQuery) : getPaginatedQueryDynamic(sqlQuery);

        log.debug("findAllPaginatedDynamic query: " + query);

        if (query == null) {
            return null;
        }
        return query.setFirstResult(first)
                .setMaxResults(pageSize)
                .getResultList();
    }

    @Override
    public List<T> findAllPaginatedDynamic(int first, int pageSize, String sqlQuery, Filters filterSet) {

        Query query = !filterSet.getFilters().isEmpty() ? getPaginatedQueryWithFiltersDynamic(sqlQuery, filterSet) : getPaginatedQueryDynamic(sqlQuery);

        log.debug("findAllPaginatedDynamic query: " + query);

        if (query == null) {
            return null;
        }
        return query.setFirstResult(first)
                .setMaxResults(pageSize)
                .getResultList();
    }

    @Override
    public List<T> findAllPaginated(int first, int pageSize) {
        Query query = !getFilters().isEmpty() ? getPaginatedQueryWithFilters() : getPaginatedQuery();

        log.debug("findAllPaginated, JPA query: " + query);

        if (query == null) {
            return null;
        }

        List<T> data = query
                .setFirstResult(first)
                .setMaxResults(pageSize)
                .getResultList();

        //log.debug(String.format("findAllPaginated: paginated data - size=%d, data=%s", data.size(), data));
        return data;
    }

    @Override
    public List<T> findAllPaginated(int first, int pageSize, Filters filterSet) {
        Query query = !filterSet.getFilters().isEmpty() ? getPaginatedQueryWithFilters(filterSet) : getPaginatedQuery();

        log.debug("findAllPaginated, JPA query: " + query);

        if (query == null) {
            return null;
        }

        List<T> data = query
                .setFirstResult(first)
                .setMaxResults(pageSize)
                .getResultList();

        //log.debug(String.format("findAllPaginated: paginated data - size=%d, data=%s", data.size(), data));
        return data;
    }

    public List<T> findAll() {
        return getEntityManager().createQuery("select s from " + cl.getName() + " s order by s.id desc", cl)
                .getResultList();
    }

    public List<T> findAllforReseller() {
        return getEntityManager().createQuery("select s from " + cl.getName() + " s order by s.name", cl)
                .getResultList();
    }

    public List<T> findAllAscending() {
        return getEntityManager().createQuery("select s from " + cl.getName() + " s order by s.id asc", cl)
                .getResultList();
    }

    public List<T> findAllNotIn(Long pk) {
        return getEntityManager().createQuery("select s from " + cl.getName() + " s where s.id != :s_id order by s.id desc", cl)
                .setParameter("s_id", pk)
                .getResultList();
    }

    protected <Z, X> From<Z, X> getWhereRoot(CriteriaQuery query, From root) {
        return root;
    }

    protected <Z, X> From<Z, X> getWhereRootWithFilters(CriteriaQuery query, From root, Filters filters) {
        return root;
    }

    public Query getPaginatedQueryWithFilters() {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = cb.createQuery(cl);

        Root root = criteriaQuery.from(cl);
        //Root model = criteriaQuery.from(EquipmentModel.class);
        criteriaQuery.select(root);
        //Join<Equipment, ServiceProvider> providerJoin = root.join("provider");
        //Join<Equipment, EquipmentModel> modelJoin = root.join("model");
        log.debug("getPaginatedQueryWithFilters: Filters=" + getFilters());
        if (!getFilters().isEmpty()) {
            //log.debug("Filters: " + getFilters().toString());
            criteriaQuery = criteriaQuery.where(getPredicateWithFilters(cb, getWhereRoot(criteriaQuery, root)));
            criteriaQuery = getOrderByClause(cb, criteriaQuery, root);

            return getEntityManager().createQuery(criteriaQuery);
        } else {
            return getPaginatedQuery();
        }
    }

    public Query getPaginatedQueryWithFilters(Filters filterSet) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = cb.createQuery(cl);

        Root root = criteriaQuery.from(cl);
        //Root model = criteriaQuery.from(EquipmentModel.class);
        criteriaQuery.select(root);
        //Join<Equipment, ServiceProvider> providerJoin = root.join("provider");
        //Join<Equipment, EquipmentModel> modelJoin = root.join("model");
        log.debug("getPaginatedQueryWithFilters: Filters=" + filterSet.getFilters());
        if (!filterSet.getFilters().isEmpty()) {
            //log.debug("Filters: " + filterSet.getFilters().toString());
            criteriaQuery = criteriaQuery.where(getPredicateWithFilters(cb, getWhereRootWithFilters(criteriaQuery, root, filterSet), filterSet));
            criteriaQuery = getOrderByClause(cb, criteriaQuery, root);

            return getEntityManager().createQuery(criteriaQuery);
        } else {
            return getPaginatedQuery();
        }
    }

    protected CriteriaQuery getOrderByClause(CriteriaBuilder cb, CriteriaQuery query, Path path) {
        if (orderingMap != null && !orderingMap.isEmpty()) {
            List<Order> orderingList = new ArrayList<>();
            Order order = null;

            for (Map.Entry<String, Ordering> entry : orderingMap.entrySet()) {
                path = parsePath(path, entry.getKey());

                switch (entry.getValue()) {
                    case ASC:
                        order = cb.asc(path);
                        break;
                    case DESC:
                        order = cb.desc(path);
                        break;
                }

                orderingList.add(order);
            }

            return query.orderBy(orderingList);
        } else {
            Root root = (Root) path;
            return query.orderBy(ordering == Ordering.ASC ? cb.asc(root.get("id")) : cb.desc(root.get("id")));
        }
    }

    @Override
    public long countDynamic(String sqlQuery) {
        log.debug("countDynamic: " + sqlQuery);
        Query query = !getFilters().isEmpty() ? countAllWithFiltersDynamic(sqlQuery) : countAllDynamic(sqlQuery);
        if (query == null) {
            return 0;
        }
        long res = (long) query.getSingleResult();
        log.debug("Total count: " + res);
        return res;
    }

    @Override
    public long countDynamic(String sqlQuery, Filters filterSet) {
        log.debug("countDynamic: " + sqlQuery);
        Query query = !filterSet.getFilters().isEmpty() ? countAllWithFiltersDynamic(sqlQuery, filterSet) : countAllDynamic(sqlQuery);
        if (query == null) {
            return 0;
        }
        long res = (long) query.getSingleResult();
        log.debug("Total count: " + res);
        return res;
    }

    @Override
    public long count() {
        Query query = !getFilters().isEmpty() ? countAllWithFilters() : countAll();
        log.debug("count: are filters NOT empty? " + !getFilters().isEmpty());
        log.debug(String.format("Query is: %s", query));
        long res = (long) query.getSingleResult();
        log.debug("Total count: " + res);

        if (query != null) {
            return res;
        }
        return 0;
    }

    @Override
    public long count(Filters filterSet) {
        Query query = !filterSet.getFilters().isEmpty() ? countAllWithFilters(filterSet) : countAll();
        log.debug("count: are filters NOT empty? " + !filterSet.getFilters().isEmpty());
        log.debug(String.format("Query is: %s", query));
        long res = (long) query.getSingleResult();
        log.debug("Total count: " + res);

        if (query != null) {
            return res;
        }
        return 0;
    }

    private Query countAllWithFiltersDynamic(String sqlQuery) {

        Query query = getEntityManager().createQuery(sqlQuery, cl);

        for (Filterable filter : getFilters().keySet()) {
            if (sqlQuery.contains(filter.getField()))
                query.setParameter(filter.getField(), getFilters().get(filter));
        }

        log.debug("query: " + sqlQuery);

        return query;
    }

    private Query countAllWithFiltersDynamic(String sqlQuery, Filters filterSet) {

        Query query = getEntityManager().createQuery(sqlQuery, cl);

        for (Filterable filter : filterSet.getFilters().keySet()) {
            if (sqlQuery.contains(filter.getField()))
                query.setParameter(filter.getField(), filterSet.getFilters().get(filter));
        }

        log.debug("query: " + sqlQuery);

        return query;
    }

    public Query countAllWithFilters() {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = cb.createQuery(Long.class);

        Root root = criteriaQuery.from(cl);
        //Root model = criteriaQuery.from(EquipmentModel.class);
        criteriaQuery.select(cb.count(root));
        //Join<Equipment, ServiceProvider> providerJoin = root.join("provider");
        //Join<Equipment, EquipmentModel> modelJoin = root.join("model");
        log.debug("countAllWithFilters: Filters=" + getFilters());
        if (!getFilters().isEmpty()) {
            return getEntityManager().createQuery(criteriaQuery.where(getPredicateWithFilters(cb, getWhereRoot(criteriaQuery, root))));
        } else {
            return countAll();
        }
    }

    public Query countAllWithFilters(Filters filterSet) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = cb.createQuery(Long.class);

        Root root = criteriaQuery.from(cl);
        //Root model = criteriaQuery.from(EquipmentModel.class);
        criteriaQuery.select(cb.count(root));
        //Join<Equipment, ServiceProvider> providerJoin = root.join("provider");
        //Join<Equipment, EquipmentModel> modelJoin = root.join("model");
        log.debug("countAllWithFilters: Filters=" + filterSet.getFilters());
        if (!filterSet.getFilters().isEmpty()) {
            return getEntityManager().createQuery(criteriaQuery.where(getPredicateWithFilters(cb, getWhereRootWithFilters(criteriaQuery, root, filterSet), filterSet)));
        } else {
            return countAll();
        }
    }

    private Query countAll() {
        return getEntityManager().createQuery("select count(s) from " + cl.getName() + " s", Long.class);
    }

    private Query countAllDynamic(String sqlQuery) {
        log.debug("query: " + sqlQuery);
        return getEntityManager().createQuery(sqlQuery, cl);
    }

    protected Path parsePath(Path path, String property) {
        //log.debug("Property: " + property + " - " + property.contains("\\."));

        if (property.contains(".")) {
            String[] properties = property.split("\\.");
            //log.debug("Properties: " + Arrays.toString(properties));
            for (String prop : properties) {
                path = path.get(prop);
            }
        } else {
            path = path.get(property);
        }

        return path;
    }

    protected Predicate getPredicateWithFilters(CriteriaBuilder cb, From root) {
        Predicate predicate = null;
        Path path = null;
        log.debug("getPredicateWithFilters: Filters=" + getFilters());

        if (!getFilters().isEmpty()) {
            for (Map.Entry<Filterable, Object> entry : getFilters().entrySet()) {
                path = root;

                /*if (entry.getKey().getField().contains(".")) {
                 String[] properties = entry.getKey().getField().split("\\.");
                 //log.debug("Properties: " + Arrays.toString(properties));
                 for (String prop : properties) {
                 path = path.get(prop);
                 }
                 }
                 else {
                 path = path.get(entry.getKey().getField());
                 }*/
                path = parsePath(root, entry.getKey().getField());

                Predicate tempPredicate = null;
                //Expression expr = cb.lower(path);
                //Expression expr = path;

                if (entry.getKey().getOperation() == MatchingOperation.NOT_EQUALS) {
                    if (entry.getValue() instanceof String) {
                        tempPredicate = cb.notEqual(cb.lower(path), ((String) entry.getValue()).toLowerCase());
                    } else {
                        tempPredicate = cb.notEqual(path, entry.getValue());
                    }
                } else if (entry.getKey().getOperation() == MatchingOperation.EQUALS) {
                    if (entry.getValue() instanceof String) {
                        tempPredicate = cb.equal(cb.lower(path), ((String) entry.getValue()).toLowerCase());
                    } else {
                        tempPredicate = cb.equal(path, entry.getValue());
                    }
                } else if (entry.getValue() instanceof String) {
                    switch (entry.getKey().getOperation()) {
                        case LIKE:
                            String value = new StringBuilder("%").append(((String) entry.getValue()).toLowerCase()).append("%").toString();
                            tempPredicate = cb.like(cb.lower(path), value);
                            //     log.debug(String.format("getQueryWithFilters: operation=LIKE, field=%s, value=%s", path, value));
                            break;
                    }
                    /*
                     predicate = (predicate == null) ?
                     cb.like(cb.lower(path), value)
                     : ( predicateJoinOperation == PredicateJoinOperation.OR ?
                     cb.or(predicate, cb.like(cb.lower(path), value)) : cb.and(predicate, cb.like(cb.lower(path), value))
                     );*/
                } /*else {
                 predicate = (predicate == null) ?
                 cb.equal(cb.lower(path),
                 entry.getValue())
                 : (predicateJoinOperation == PredicateJoinOperation.OR ?
                 cb.or(predicate, cb.equal(cb.lower(path), entry.getValue()))
                 : cb.and(predicate, cb.equal(cb.lower(path), entry.getValue()))
                 );
                 }*/ else if (entry.getKey() instanceof Number) {
                    if (entry.getKey().getOperation() == MatchingOperation.GREATER) {
                        tempPredicate = cb.gt(path, (Number) entry.getValue());
                    } else if (entry.getKey().getOperation() == MatchingOperation.LESS) {
                        tempPredicate = cb.lt(path, (Number) entry.getValue());
                    }
                } else if (entry.getValue() instanceof Map && entry.getKey().getOperation() == MatchingOperation.BETWEEN) {
                    Map<String, Date> dateList = (Map) entry.getValue();
                    tempPredicate = cb.between(path, dateList.get("from"), dateList.get("to"));
                } else if (entry.getKey().getOperation() == MatchingOperation.IN) {

                    List<Long> TList = (List<Long>) entry.getValue();
                    log.debug("TList size: " + TList.size());

                    for (Long l : TList) {
                        log.debug("l: " + l);
                        tempPredicate = cb.equal(path, l);

                        if (predicate != null) {
                            predicate = cb.or(predicate, tempPredicate);
                        } else {

                            predicate = tempPredicate;
                        }
                    }
                    log.debug("End of IN ");
                } else if (entry.getValue() instanceof Map && entry.getKey().getOperation() == MatchingOperation.BETWEEN_STRING) {
                    Map<String, String> dateList = (Map) entry.getValue();
                    tempPredicate = cb.between(path, dateList.get("from"), dateList.get("to"));
                } else {
                    tempPredicate = cb.equal(path, entry.getValue());
                }

                if (predicate != null) {
                    switch (predicateJoinOperation) {
                        case OR:
                            predicate = cb.or(predicate, tempPredicate);
                            break;
                        case AND:
                            predicate = cb.and(predicate, tempPredicate);
                            break;
                    }
                } else {
                    predicate = tempPredicate;
                }
            }
        }
        return predicate;
    }

    protected Predicate getPredicateWithFilters(CriteriaBuilder cb, From root, Filters filterSet) {
        Predicate predicate = null;
        Path path = null;
        log.debug("getPredicateWithFilters: Filters=" + filterSet.getFilters());

        if (!filterSet.getFilters().isEmpty()) {
            for (Map.Entry<Filterable, Object> entry : filterSet.getFilters().entrySet()) {
                path = root;

                /*if (entry.getKey().getField().contains(".")) {
                 String[] properties = entry.getKey().getField().split("\\.");
                 //log.debug("Properties: " + Arrays.toString(properties));
                 for (String prop : properties) {
                 path = path.get(prop);
                 }
                 }
                 else {
                 path = path.get(entry.getKey().getField());
                 }*/
                path = parsePath(root, entry.getKey().getField());

                Predicate tempPredicate = null;
                //Expression expr = cb.lower(path);
                //Expression expr = path;

                if (entry.getKey().getOperation() == MatchingOperation.NOT_EQUALS) {
                    if (entry.getValue() instanceof String) {
                        tempPredicate = cb.notEqual(cb.lower(path), ((String) entry.getValue()).toLowerCase());
                    } else {
                        tempPredicate = cb.notEqual(path, entry.getValue());
                    }
                } else if (entry.getKey().getOperation() == MatchingOperation.EQUALS) {
                    if (entry.getValue() instanceof String) {
                        tempPredicate = cb.equal(cb.lower(path), ((String) entry.getValue()).toLowerCase());
                    } else {
                        tempPredicate = cb.equal(path, entry.getValue());
                    }
                } else if (entry.getValue() instanceof String) {
                    switch (entry.getKey().getOperation()) {
                        case LIKE:
                            String value = new StringBuilder("%").append(((String) entry.getValue()).toLowerCase()).append("%").toString();
                            tempPredicate = cb.like(cb.lower(path), value);
                            //     log.debug(String.format("getQueryWithFilters: operation=LIKE, field=%s, value=%s", path, value));
                            break;
                    }
                    /*
                     predicate = (predicate == null) ?
                     cb.like(cb.lower(path), value)
                     : ( predicateJoinOperation == PredicateJoinOperation.OR ?
                     cb.or(predicate, cb.like(cb.lower(path), value)) : cb.and(predicate, cb.like(cb.lower(path), value))
                     );*/
                } /*else {
                 predicate = (predicate == null) ?
                 cb.equal(cb.lower(path),
                 entry.getValue())
                 : (predicateJoinOperation == PredicateJoinOperation.OR ?
                 cb.or(predicate, cb.equal(cb.lower(path), entry.getValue()))
                 : cb.and(predicate, cb.equal(cb.lower(path), entry.getValue()))
                 );
                 }*/ else if (entry.getKey() instanceof Number) {
                    if (entry.getKey().getOperation() == MatchingOperation.GREATER) {
                        tempPredicate = cb.gt(path, (Number) entry.getValue());
                    } else if (entry.getKey().getOperation() == MatchingOperation.LESS) {
                        tempPredicate = cb.lt(path, (Number) entry.getValue());
                    }
                } else if (entry.getValue() instanceof Map && entry.getKey().getOperation() == MatchingOperation.BETWEEN) {
                    Map<String, Date> dateList = (Map) entry.getValue();
                    tempPredicate = cb.between(path, dateList.get("from"), dateList.get("to"));
                } else if (entry.getKey().getOperation() == MatchingOperation.IN) {

                    List<Long> TList = (List<Long>) entry.getValue();
                    log.debug("TList size: " + TList.size());

                    for (Long l : TList) {
                        log.debug("l: " + l);
                        tempPredicate = cb.equal(path, l);

                        if (predicate != null) {
                            predicate = cb.or(predicate, tempPredicate);
                        } else {

                            predicate = tempPredicate;
                        }
                    }
                    log.debug("End of IN ");
                } else if (entry.getValue() instanceof Map && entry.getKey().getOperation() == MatchingOperation.BETWEEN_STRING) {
                    Map<String, String> dateList = (Map) entry.getValue();
                    tempPredicate = cb.between(path, dateList.get("from"), dateList.get("to"));
                } else {
                    tempPredicate = cb.equal(path, entry.getValue());
                }

                if (predicate != null) {
                    switch (predicateJoinOperation) {
                        case OR:
                            predicate = cb.or(predicate, tempPredicate);
                            break;
                        case AND:
                            predicate = cb.and(predicate, tempPredicate);
                            break;
                    }
                } else {
                    predicate = tempPredicate;
                }
            }
        }
        return predicate;
    }

    public void removeFilter(Filterable filter) {
        if (filters.containsKey(filter)) {
            filters.remove(filter);
        }
    }

    public <Z> Object[] sumWithFilters(Class<Z> zClass, String... fields) throws Exception {
        if (getFilters().isEmpty()) {
            throw new UnsupportedOperationException("At least one FILTER must provided");
        }

        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Object[]> criteriaQuery = cb.createQuery(Object[].class);

        Root root = criteriaQuery.from(cl);
        //Root model = criteriaQuery.from(EquipmentModel.class);
        // CriteriaBuilder.Coalesce<Double> coalesce = cb.<Double>coalesce().value(cb.<Double>sum(root.get("amount"))).value(0.00);
        //cb.sum(root.get("amount")
        List<Selection<Z>> selection = new ArrayList<>();

        for (String fieldName : fields) {
            selection.add(cb.<Z>coalesce().value(cb.sum(root.get(fieldName))).<Z>value(zClass.getConstructor(String.class).newInstance("0")));
        }

        Selection[] selAr = new Selection[selection.size()];
        selection.toArray(selAr);

        criteriaQuery.select(cb.array(selAr));
        //Join<Equipment, ServiceProvider> providerJoin = root.join("provider");
        //Join<Equipment, EquipmentModel> modelJoin = root.join("model");
        criteriaQuery.where(getPredicateWithFilters(cb, root));

        TypedQuery<Object[]> sumQuery = getEntityManager().createQuery(criteriaQuery);

        log.debug("sumWithFilters: sumQuery=" + sumQuery);
        Object[] result = sumQuery.getSingleResult();
        log.debug("sumWIthFilters: Result=" + result);
        return result;
    }

    protected Class<T> getEntityClass() {
        return cl;
    }

}
