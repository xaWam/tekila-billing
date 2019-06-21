/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jaravir.tekila.module.store;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.store.scratchcard.persistence.entity.BatchID;
import com.jaravir.tekila.module.store.scratchcard.persistence.entity.ScratchCard;
import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.apache.log4j.Logger;

/**
 *
 * @author shnovruzov
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class BatchIDPersistenceFacade extends AbstractPersistenceFacade<BatchID> {

    @Resource
    private SessionContext ctx;
    @PersistenceContext
    private EntityManager em;
    private final static Logger log = Logger.getLogger(BatchIDPersistenceFacade.class);

    public BatchIDPersistenceFacade() {
        super(BatchID.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public long checkCount() {
        Query query = em.createQuery("select count(btc.id) from BatchID btc", BatchID.class);
        long res = (long) query.getSingleResult();
        return res;
    }

    public long getBatchID() {

        Query query = em.createQuery("select max(btc.batchID) from BatchID btc", BatchID.class);
        long batchID = (long) query.getSingleResult();
        log.debug("batchID: "+batchID);
        batchID++;
        BatchID bt = new BatchID();
        bt.setBatchID(batchID);
        save(bt);
        return batchID;

    }
}
