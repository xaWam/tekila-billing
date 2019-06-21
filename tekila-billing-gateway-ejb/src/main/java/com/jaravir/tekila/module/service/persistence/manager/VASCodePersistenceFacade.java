/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jaravir.tekila.module.service.persistence.manager;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.service.entity.VASCode;
import com.jaravir.tekila.module.service.entity.VASCodeSequence;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author sajabrayilov
 */
@Stateless
public class VASCodePersistenceFacade extends AbstractPersistenceFacade<VASCode>{
    @PersistenceContext
    private EntityManager em;
    
    public VASCodePersistenceFacade() {
        super(VASCode.class);
    }
    
    @Override
    protected EntityManager getEntityManager() {
        return em;
    }    
    
    @Override
    public void save (VASCode code) {
        code.setGenerator(new VASCodeSequence());
        super.save(code);
    }
}
