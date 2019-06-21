package com.jaravir.tekila.module.store.nas;

import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.engines.ProvisioningEngine;
import com.jaravir.tekila.module.service.entity.ServiceProvider;
import com.jaravir.tekila.module.service.persistence.manager.ServiceProviderPersistenceFacade;
import com.jaravir.tekila.module.system.log.SystemLogger;
import org.apache.log4j.Logger;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

/**
 * Created by shnovruzov on 6/2/2016.
 */
@Stateless
public class NasPersistenceFacade extends AbstractPersistenceFacade<Nas> {
    private final static Logger log = Logger.getLogger(NasPersistenceFacade.class);

    @PersistenceContext
    private EntityManager em;
    @Resource
    private EJBContext ctx;
    @EJB
    private SystemLogger systemLogger;
    @EJB
    private EngineFactory provisioningFactory;

    public enum Filter implements Filterable {
        ID("id"),
        NAME("name"),
        IP("IP"),
        PROVIDER("provider"),
        desc("desc");

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


    public NasPersistenceFacade() {
        super(Nas.class);
    }

    protected EntityManager getEntityManager() {
        return em;
    }

    public List<Nas> findAllNas() {
        Query query = em.createQuery("select nas from Nas nas", Nas.class);
        return query.getResultList();
    }

    public Nas create(String name, ServiceProvider provider, String ip, String secretKey, String desc, List<Attribute> attributeList) {
        Nas nas = new Nas();
        nas.setName(name);
        nas.setProvider(provider);
        nas.setIP(ip);
        nas.setSecretKey(secretKey);
        nas.setDesc(desc);
        nas.setAttributeList(attributeList);
        save(nas);
        return update(nas);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void save(Nas entity) {
        try {
            this.getEntityManager().persist(entity);
            entity = this.getEntityManager().merge(entity);
            ProvisioningEngine provisioner = provisioningFactory.getProvisioningEngine(entity.getProvider());
            boolean res = provisioner.createNas(entity);
            if (!res) throw new Exception();
            log.debug("New nas created: " + entity);
        } catch (Exception ex) {
            ctx.getRollbackOnly();
            log.debug(String.format("%s: cannot create nas", "creation new nas"), ex);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Nas update(Nas entity) {
        try {
            entity = this.getEntityManager().merge(entity);
            ProvisioningEngine provisioner = provisioningFactory.getProvisioningEngine(entity.getProvider());
            boolean res = provisioner.updateNas(entity);
            if (!res) throw new Exception();
            log.debug("nas updated: " + entity);
            return entity;
        } catch (Exception ex) {
            ctx.getRollbackOnly();
            log.debug(String.format("%s: cannot update nas", "update nas"), ex);
            return null;
        }
    }

}
