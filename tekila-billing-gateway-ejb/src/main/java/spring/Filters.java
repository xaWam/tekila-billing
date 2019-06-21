package spring;

import com.jaravir.tekila.base.filter.Filterable;

import java.util.HashMap;
import java.util.Map;

public class Filters {
    private Map<Filterable, Object> filters;

    public Map<Filterable, Object> getFilters() {
        if (filters == null) {
            filters = new HashMap<>();
        }

        return filters;
    }

    public void clearFilters() {
        if (getFilters().size() > 0) {
            getFilters().clear();
        }
    }

    public void addFilter(Filterable key, Object value) {
        getFilters().put(key, value);
    }
}
