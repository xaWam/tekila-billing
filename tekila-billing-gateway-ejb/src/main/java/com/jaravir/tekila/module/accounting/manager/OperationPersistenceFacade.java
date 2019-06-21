/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jaravir.tekila.module.accounting.manager;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.accounting.entity.Operation;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author sajabrayilov
 */
@Stateless
public class OperationPersistenceFacade extends AbstractPersistenceFacade<Operation>{
    @PersistenceContext
    private EntityManager em;

    public OperationPersistenceFacade () {
        super(Operation.class);
    }
    
    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
    
    
}
