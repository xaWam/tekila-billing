package com.jaravir.tekila.module.accounting.manager;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.accounting.entity.ChequeSequence;
import org.apache.log4j.Logger;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by shnovruzov on 8/15/2016.
 */
@Stateless
public class ChequeSequencePersistenceFacade extends AbstractPersistenceFacade<ChequeSequence> {

    @PersistenceContext
    private EntityManager em;
    @Resource
    private EJBContext ctx;

    private static final Logger log = Logger.getLogger(ChequeSequencePersistenceFacade.class);

    public ChequeSequencePersistenceFacade() {
        super(ChequeSequence.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public synchronized Long findAndUpdateChequeNum() {
        ChequeSequence sequence;
        Long num;
        try {
            sequence = (ChequeSequence) em.createQuery("select ch from ChequeSequence ch").getSingleResult();
        } catch (Exception ex) {
            sequence = null;
        }
        if (sequence == null) {
            num = 128763654L;
            sequence = new ChequeSequence();
            sequence.setChequeNum(num + 1);
            save(sequence);
        } else {
            num = sequence.getChequeNum();
            sequence.setChequeNum(num + 1);
            update(sequence);
        }
        return num;
    }
}
