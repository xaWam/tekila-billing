/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jaravir.tekila.module.accounting.manager;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.accounting.entity.Bank;
import com.jaravir.tekila.module.accounting.entity.Currency;
import java.util.Arrays;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.log4j.Logger;

/**
 *
 * @author sajabrayilov
 */
@Stateless
public class BankPersistenceFacade extends AbstractPersistenceFacade<Bank>{
    @PersistenceContext
    private EntityManager em;
    @EJB private CurrencyPersistenceFacade currencyFacade;
    private static final Logger log = Logger.getLogger(BankPersistenceFacade.class);
    
    public BankPersistenceFacade() {
        super(Bank.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }    
    
    public List<Bank> findAllParents() {
        return em.createQuery("select b from Bank b where b.parent is null", Bank.class)
          .getResultList();
    }
        
    public List<Bank> findAllParentsNotIn(Long... idList) {
        if (idList == null || idList.length == 0)
            return null;
        
        return em.createQuery("select b from Bank b where b.id NOT IN :idList", Bank.class)           
           .setParameter("idList", Arrays.asList(idList))
           .getResultList();
    }
    
    public List<Bank> findAllParentsNotBank(Long... bankID) {
        if (bankID == null)
            return null;
        
        return em.createQuery("select p from Bank p where p.id != :b_id", Bank.class)
            .setParameter("b_id", bankID)
            .getResultList();
    }
            
    public void createFromForm(Bank bank, String currencyCode, Long parentID) throws Exception {
        try {
            Currency currency = currencyFacade.findByCode(currencyCode);
            bank.setCurrency(currency);
            if (parentID != null)
                bank.setParent(find(parentID));
            
            save(bank);
        }
        catch (Exception ex) {
            String message = String.format("Cannot create bank from bank=%s, currencyCode=%s, parentID=%d", 
                bank, currencyCode, parentID);
            log.error(message, ex);
            throw new Exception(message, ex.getCause());
        }
    }
    
    public void updateFromForm(Bank bank, String currencyCode, Long parentID) throws Exception {
        try {
            Currency currency = currencyFacade.findByCode(currencyCode);
            bank.setCurrency(currency);
            if (parentID != null)
                bank.setParent(find(parentID));
            
            update(bank);
        }
        catch (Exception ex) {
            String message = String.format("Cannot edit bank from bank=%s, currencyCode=%s, parentID=%d", 
                bank, currencyCode, parentID);
            log.error(message, ex);
            throw new Exception(message, ex.getCause());
        }
    }    
}
