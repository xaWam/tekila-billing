/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jaravir.tekila.module.accounting.manager;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.accounting.entity.Charge;
import com.jaravir.tekila.module.accounting.entity.Refund;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;


/**
 *
 * @author sajabrayilov
 */
@Stateless
public class RefundPersistenceFacade extends AbstractPersistenceFacade<Refund>{
    @PersistenceContext
    private EntityManager em;
    private long subscriberId;
    
    public RefundPersistenceFacade() {
        super(Refund.class);
    }

    public long getSubscriberId() {
        return subscriberId;
    }

    public void setSubscriberId(long subscriberId) {
        this.subscriberId = subscriberId;
    }
    
    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
    
    @Override
    public List<Refund> findAllPaginated(int first, int pageSize) {
        List<Refund> charges = em.createQuery("select c from Refund c join c.subscriber sub where sub.id = :sub_id order by c.datetime desc, c.lastUpdateDate desc", Refund.class)
            .setParameter("sub_id", getSubscriberId())
            .setFirstResult(first)
            .setMaxResults(pageSize)
            .getResultList(); 

        return charges;
    }
    
    @Override
    public long count() {
        return em.createQuery("select count(c) from Refund c join c.subscriber sub where sub.id = :sub_id", Long.class)
                .setParameter("sub_id", getSubscriberId())
                .getSingleResult();
    }
}
