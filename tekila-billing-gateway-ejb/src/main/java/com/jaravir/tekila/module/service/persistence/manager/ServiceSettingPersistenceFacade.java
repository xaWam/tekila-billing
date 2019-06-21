/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jaravir.tekila.module.service.persistence.manager;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.service.ServiceSettingType;
import com.jaravir.tekila.module.service.ServiceType;
import com.jaravir.tekila.module.service.entity.ServiceSetting;

import java.util.List;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * @author sajabrayilov
 */
@Stateless
@LocalBean
public class ServiceSettingPersistenceFacade extends AbstractPersistenceFacade<ServiceSetting> {

    @PersistenceContext
    private EntityManager em;

    public ServiceSettingPersistenceFacade() {
        super(ServiceSetting.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public List<ServiceSetting> findAllByServiceTypeAndProviderID(long providerID, ServiceType serviceType) {
        return em.createQuery("select s from ServiceSetting s join s.provider p where p.id = :provider_id and s.serviceType = :service_type", ServiceSetting.class)
                .setParameter("provider_id", providerID)
                .setParameter("service_type", serviceType)
                .getResultList();
    }

    public ServiceSetting find(long providerID, ServiceType serviceType, ServiceSettingType serviceSettingType) {
        List<ServiceSetting> settingList = em.createQuery("select s from ServiceSetting s join s.provider p where p.id = :provider_id and s.serviceType = :service_type and s.type=:type", ServiceSetting.class)
                .setParameter("provider_id", providerID)
                .setParameter("service_type", serviceType)
                .setParameter("type", serviceSettingType)
                .getResultList();
        if (settingList == null || settingList.isEmpty()) {
            return null;
        } else {
            return settingList.get(0);
        }
    }
}
