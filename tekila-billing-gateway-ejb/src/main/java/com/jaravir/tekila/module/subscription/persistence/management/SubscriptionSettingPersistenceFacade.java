/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jaravir.tekila.module.subscription.persistence.management;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionSetting;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author sajabrayilov
 */
@Stateless
@LocalBean
public class SubscriptionSettingPersistenceFacade extends AbstractPersistenceFacade<SubscriptionSetting>{
    @PersistenceContext
    private EntityManager em;
    
    public SubscriptionSettingPersistenceFacade() {
        super(SubscriptionSetting.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }       
}
