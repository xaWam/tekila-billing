package com.jaravir.tekila.module.service.persistence.manager;

import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.engines.ProvisioningEngine;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.service.entity.ServiceProfile;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import org.apache.log4j.Logger;

import javax.ejb.*;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by shnovruzov on 6/30/2016.
 */
@Stateless
public class ServiceProfilePersistenceFacade extends AbstractPersistenceFacade<ServiceProfile> {
    @PersistenceContext
    private EntityManager em;
    @javax.annotation.Resource
    private EJBContext ctx;
    private final static Logger log = Logger.getLogger(ServiceProfilePersistenceFacade.class);
    @EJB
    private EngineFactory provisioningFactory;

    public enum Filter implements Filterable {
        SERVICE("service");

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


    public ServiceProfilePersistenceFacade() {
        super(ServiceProfile.class);
    }

    public EntityManager getEntityManager() {
        return em;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ServiceProfile update(ServiceProfile serviceProfile) {
        try {
            serviceProfile = this.getEntityManager().merge(serviceProfile);
            Service service = serviceProfile.getService();
            log.debug("service: " + service);
            boolean res = provisioningFactory.getProvisioningEngine(serviceProfile.getService().getProvider()).updateServiceProfile(serviceProfile, 0);
            if (!res)
                throw new Exception();

            log.debug("Provisioning success");
            return serviceProfile;
        } catch (Exception ex) {
            ctx.getRollbackOnly();
            return serviceProfile;
        }
    }

    public ServiceProfile merge(ServiceProfile serviceProfile) {
        return this.getEntityManager().merge(serviceProfile);
    }

    public void remove(ServiceProfile serviceProfile){
        serviceProfile = em.merge(serviceProfile);
        em.remove(serviceProfile);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void addToRadius(ServiceProfile serviceProfile) throws ProvisionerNotFoundException {
        ProvisioningEngine provisioner = provisioningFactory.getProvisioningEngine(serviceProfile.getService().getProvider());
        boolean res = provisioner.createServiceProfile(serviceProfile);
        log.debug("adding to radius result: " + res);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void removeFromRadius(ServiceProfile serviceProfile) throws ProvisionerNotFoundException {
        ProvisioningEngine provisioner = provisioningFactory.getProvisioningEngine(serviceProfile.getService().getProvider());
        boolean res = provisioner.updateServiceProfile(serviceProfile, -1);
        log.debug("removing from radius result: " + res);
    }
}
