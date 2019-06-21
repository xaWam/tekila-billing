/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jaravir.tekila.module.accounting.manager;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.accounting.entity.Currency;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author sajabrayilov
 */
@Stateless
public class CurrencyPersistenceFacade extends AbstractPersistenceFacade<Currency>{
    @PersistenceContext
    private EntityManager em;
    
    public CurrencyPersistenceFacade() {
        super(Currency.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
    
    @Override
    public List<Currency> findAll() {
        return em.createQuery("select c from Currency c order by c.id", Currency.class)
           .getResultList();
    }
    
    public Currency findByCode(String code) {
        return em.createQuery("select c from Currency c where c.code = :c_code", Currency.class)
            .setParameter("c_code", code)
            .getSingleResult();
    }
}
