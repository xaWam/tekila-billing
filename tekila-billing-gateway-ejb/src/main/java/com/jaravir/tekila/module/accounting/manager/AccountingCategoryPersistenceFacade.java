/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jaravir.tekila.module.accounting.manager;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.accounting.AccountingCategoryType;
import com.jaravir.tekila.module.accounting.entity.AccountingCategory;
import com.jaravir.tekila.module.accounting.entity.TaxationCategory;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.log4j.Logger;

import java.util.List;

/**
 *
 * @author sajabrayilov
 *
 */

@Stateless
public class AccountingCategoryPersistenceFacade extends AbstractPersistenceFacade<AccountingCategory>{
    @PersistenceContext
    private EntityManager em;
    @Resource
    private EJBContext ctx;
    @EJB private TaxCategoryPeristenceFacade taxCatFacade;
    private static final Logger log = Logger.getLogger(AccountingCategoryPersistenceFacade.class);
    
    public AccountingCategoryPersistenceFacade() {
        super(AccountingCategory.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }    
 
    public void createFromFrom (Long taxCatID, AccountingCategory category) throws Exception {
        try {
            TaxationCategory taxCat = taxCatFacade.find(taxCatID);
            category.setTaxCategory(taxCat);
            save(category);
        }
        catch (Exception ex) {
            ctx.setRollbackOnly();
            String msg = String.format("Cannot create AccountingCategory: category=%s, taxCatID=%d", category, taxCatID);
            log.error(msg, ex);
            throw new Exception(msg, ex.getCause());
        }
    }
    
    public void edit(AccountingCategory category, Long taxCatID) throws Exception {
        if (category == null)
            throw new IllegalArgumentException("All parameters must be non-null");

        try { 
            if (taxCatID != null) {
                TaxationCategory taxCat = taxCatFacade.find(taxCatID);
                category.setTaxCategory(taxCat);  
            }
            log.debug("AccountingCategory to be updated: " + category);
            update(category);
        }
        catch (Exception ex) {
            ctx.getRollbackOnly();
            String message = String.format("Cannot update AccountingCategory: %s, taxationCategoryID=%d", category, taxCatID);
            log.error(message, ex);            
            throw new Exception(message, ex.getCause());
        }
    }        
    
    public AccountingCategory findByType (AccountingCategoryType type) {
        return 
            em.createQuery("select c from AccountingCategory c where c.type = :type", AccountingCategory.class)
           .setParameter("type", type).getSingleResult();        
    }

    public List<AccountingCategory> findAllAdj () {
        return
                em.createQuery("select c from AccountingCategory c where c.isAdjustment = :adj", AccountingCategory.class)
                        .setParameter("adj", Boolean.TRUE).getResultList();
    }
}
