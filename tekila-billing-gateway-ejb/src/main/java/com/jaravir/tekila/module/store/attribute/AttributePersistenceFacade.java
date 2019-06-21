package com.jaravir.tekila.module.store.attribute;

import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.engines.ProvisioningEngine;
import com.jaravir.tekila.module.service.entity.ServiceProvider;
import com.jaravir.tekila.module.service.persistence.manager.ServiceProviderPersistenceFacade;
import com.jaravir.tekila.module.store.nas.Attribute;
import com.jaravir.tekila.provision.broadband.devices.manager.Providers;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import org.apache.log4j.Logger;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by shnovruzov on 6/8/2016.
 */
@Stateless
public class AttributePersistenceFacade extends AbstractPersistenceFacade<Attribute> {
    private final static Logger log = Logger.getLogger(AttributePersistenceFacade.class);

    @PersistenceContext
    private EntityManager em;
    @Resource
    private EJBContext ctx;
    @EJB
    private EngineFactory provisioningFactory;
    @EJB
    private ServiceProviderPersistenceFacade serviceProviderPersistenceFacade;

    public enum Filter implements Filterable {
        NAME("name"),
        STATUS("status"),
        VALUE("value"),
        TAG("tag"),
        DESCRIPTION("description"),
        NAS("nas"),
        ID("id");

        private final String field;
        private MatchingOperation operation;

        Filter(String field) {
            this.field = field;
            this.operation = MatchingOperation.LIKE;
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

    public AttributePersistenceFacade() {
        super(Attribute.class);
    }

    protected EntityManager getEntityManager() {
        return em;
    }

    public void create(String name, String value, int tag, String desc, int status) {
        Attribute attribute = new Attribute();
        attribute.setName(name);
        attribute.setValue(value);
        attribute.setTag(tag);
        attribute.setDescription(desc);
        attribute.setStatus(status);
        save(attribute);
    }

    public Attribute update(Attribute attribute){
        attribute = this.getEntityManager().merge(attribute);
        ServiceProvider citynetProvider = serviceProviderPersistenceFacade.find(Providers.CITYNET.getId());
        ProvisioningEngine provisioner = null;
        try {
            provisioner = provisioningFactory.getProvisioningEngine(citynetProvider);
            provisioner.updateAttribute(attribute);
        } catch (ProvisionerNotFoundException e) {
            log.error("Error on attribute update.", e);
        }
        return attribute;
    }
}
