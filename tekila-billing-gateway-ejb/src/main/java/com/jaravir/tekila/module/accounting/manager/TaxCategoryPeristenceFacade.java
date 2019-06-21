/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jaravir.tekila.module.accounting.manager;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.accounting.entity.TaxationCategory;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
/**
 *
 * @author sajabrayilov
 */
@Stateless
public class TaxCategoryPeristenceFacade extends AbstractPersistenceFacade<TaxationCategory>{
    @PersistenceContext
    private EntityManager em;
    
    public TaxCategoryPeristenceFacade() {
        super(TaxationCategory.class);
    }
    
    @Override
    protected EntityManager getEntityManager() {
        return this.em;
    }
    
    @Override
    public List<TaxationCategory> findAll() {
        return em.createQuery("select c from TaxationCategory c order by c.isDefault desc, c.id desc", TaxationCategory.class)
                .getResultList();
    }

    @Override
    public List<TaxationCategory> findAllNotIn(Long pk) {
        return em.createQuery("select c from TaxationCategory c where c.id != :c_id order by c.isDefault desc, c.id desc", TaxationCategory.class)
                .setParameter("c_id", pk)
                .getResultList();
    }

    public TaxationCategory findDefault() {
        return em.createQuery("select c from TaxationCategory c where c.isDefault = True", TaxationCategory.class)
                .getSingleResult();
    }
}
