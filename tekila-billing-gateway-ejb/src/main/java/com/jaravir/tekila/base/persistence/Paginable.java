package com.jaravir.tekila.base.persistence;

import com.jaravir.tekila.base.filter.Filterable;
import spring.Filters;

import java.util.List;

/**
 * Created by sajabrayilov on 04.02.2015.
 */
public interface Paginable<T> {
    public List<T> findAllPaginatedDynamic (int first, int pageSize, String query);
    public List<T> findAllPaginatedDynamic (int first, int pageSize, String query, Filters filters);
    public List<T> findAllPaginated (int first, int pageSize);
    public List<T> findAllPaginated (int first, int pageSize, Filters filterSet);
    public long countDynamic (String sqlQuery);
    public long countDynamic (String sqlQuery, Filters filterSet);
    public long count ();
    public long count (Filters filterSet);
    public void addFilter(Filterable filterable, Object object);
    public void clearFilters();
    Filterable getFilterByCode (String code);
}
