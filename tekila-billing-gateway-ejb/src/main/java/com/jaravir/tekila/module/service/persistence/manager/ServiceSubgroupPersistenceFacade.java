package com.jaravir.tekila.module.service.persistence.manager;

import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.service.ServiceType;
import com.jaravir.tekila.module.service.entity.ServiceGroup;
import com.jaravir.tekila.module.service.entity.ServiceProvider;
import com.jaravir.tekila.module.service.entity.ServiceSubgroup;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by shnovruzov on 8/25/2016.
 */
@Stateless
public class ServiceSubgroupPersistenceFacade extends AbstractPersistenceFacade<ServiceSubgroup>{
    @PersistenceContext
    private EntityManager em;

    public enum Filter implements Filterable {

        PROVIDER("provider");

        private final String field;
        private MatchingOperation operation;

        private Filter(String code) {
            this.field = code;
            this.operation = MatchingOperation.EQUALS;
        }


        @Override
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


    public ServiceSubgroupPersistenceFacade () {
        super(ServiceSubgroup.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public void create(String name, ServiceProvider provider, ServiceType type){
        ServiceSubgroup subgroup = new ServiceSubgroup();
        subgroup.setName(name);
        subgroup.setProvider(provider);
        subgroup.setType(type);
        save(subgroup);
    }

    public ServiceSubgroup findSubgroupByName(String name){
        try {
            return em.createQuery("select s from ServiceSubgroup s where s.name = :name", ServiceSubgroup.class)
                    .setParameter("name", name)
                    .getSingleResult();
        }catch (Exception ex){
            return null;
        }
    }
}
