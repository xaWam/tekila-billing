/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jaravir.tekila.base.persistence.manager;

import com.jaravir.tekila.base.entity.BillingSettings;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import javax.ejb.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.joda.time.DateTime;

/**
 *
 * @author sajabrayilov
 */
@Singleton
public class BillingSettingsManager extends AbstractPersistenceFacade<BillingSettings>{
    @PersistenceContext private EntityManager em;
    private BillingSettings settings;
    
    public BillingSettingsManager () {
        super(BillingSettings.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
    
    public BillingSettings getSettings() {
        if (settings == null) {
            settings = new BillingSettings();
        }
        
        return settings;
    }
    
    public DateTime getPrepaidNextLifeCycleDateTime (DateTime billedUpToDate) {
        return (getSettings().getPrepaidlifeCycleLength() == 30) ?
            billedUpToDate.plusMonths(1) 
            : billedUpToDate.plusDays(getSettings().getPrepaidlifeCycleLength());
    }
    
    public DateTime getPrepaidPreviousLifeCycleDateTime(DateTime billedUpToDate) {
        return (getSettings().getPrepaidlifeCycleLength() == 30) ?
            billedUpToDate.minusMonths(1) 
            : billedUpToDate.minusDays(getSettings().getPrepaidlifeCycleLength());
    }
    
    public DateTime getPostpaidNextLifeCycleDate (DateTime billedUpToDate) {
        return (getSettings().getPospaidLifeCycleLength() == 30) ?
            billedUpToDate.plusMonths(1)
            : billedUpToDate.plusDays(getSettings().getPospaidLifeCycleLength());                   
    }
    
    public DateTime getPostpaidPreviousLifeCycleDate (DateTime billedUpToDate) {
        return (getSettings().getPospaidLifeCycleLength() == 30) ?
            billedUpToDate.minusMonths(1)
            : billedUpToDate.minusDays(getSettings().getPospaidLifeCycleLength());                   
    }
    
}
