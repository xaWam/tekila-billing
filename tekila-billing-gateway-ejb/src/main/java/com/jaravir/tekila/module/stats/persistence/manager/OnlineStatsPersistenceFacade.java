/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jaravir.tekila.module.stats.persistence.manager;

import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.engines.EngineFactory;
import com.jaravir.tekila.engines.ProvisioningEngine;
import com.jaravir.tekila.module.service.entity.ServiceProvider;
import com.jaravir.tekila.module.stats.persistence.entity.OnlineBroadbandStats;
import com.jaravir.tekila.module.subscription.persistence.entity.Subscription;
import com.jaravir.tekila.provision.exception.ProvisionerNotFoundException;
import org.apache.log4j.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author sajabrayilov
 */
@Stateless
public class OnlineStatsPersistenceFacade extends AbstractPersistenceFacade<OnlineBroadbandStats>{
    @PersistenceContext
    private EntityManager em;
    @EJB
    private EngineFactory provisioningFactory;
    private final static Logger log = Logger.getLogger(OnlineStatsPersistenceFacade.class);

    public OnlineStatsPersistenceFacade () {
        super(OnlineBroadbandStats.class);
    }

    public enum Filter implements Filterable {
        USERNAME("user"),
        CALLING_STATION("callingStationID"),
        ACCOUNT("accountID"),
        PROVIDER("provider.id");

        private String field;
        private MatchingOperation operation;

        Filter (String field) {
            this.field = field;
            operation = MatchingOperation.LIKE;
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

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }    

    public OnlineBroadbandStats getOnlineSession (Subscription subscription) {
        try {
            ProvisioningEngine provisioner = provisioningFactory.getProvisioningEngine(subscription);
            return provisioner.collectOnlineStats(subscription);
        } catch (ProvisionerNotFoundException e) {
            log.error(e);
            return null;
        }
    }

    public int clear () {
        return em.createQuery("delete from OnlineBroadbandStats").executeUpdate();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public OnlineBroadbandStats create (StatsRecord statsRecord, ServiceProvider provider, long id) {
        OnlineBroadbandStats stats = new OnlineBroadbandStats();
        stats.setId(id);
        stats.setAccountID(statsRecord.getAccountID());
        stats.setUser(statsRecord.getUser());
        stats.setUp(statsRecord.getUp());
        stats.setDown(statsRecord.getDown());
        stats.setNasIpAddress(statsRecord.getNasIpAddress());
        stats.setFramedAddress(statsRecord.getFramedAddress());
        stats.setCallingStationID(statsRecord.getCallingStationID());
        stats.setStartTime(statsRecord.getStartDate());
        stats.setProvider(provider);
        save(stats);
        return stats;
    }
}
    

