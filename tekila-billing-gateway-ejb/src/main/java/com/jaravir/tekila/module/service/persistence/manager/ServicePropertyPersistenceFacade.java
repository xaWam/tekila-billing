package com.jaravir.tekila.module.service.persistence.manager;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.service.entity.Service;
import com.jaravir.tekila.module.service.entity.ServiceProperty;
import com.jaravir.tekila.module.service.entity.SubscriptionServiceType;
import com.jaravir.tekila.module.subscription.persistence.entity.SubscriptionSetting;
import org.apache.log4j.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class ServicePropertyPersistenceFacade extends AbstractPersistenceFacade<ServiceProperty> {
    public static final Logger log = Logger.getLogger(ServicePropertyPersistenceFacade.class);

    @PersistenceContext
    private EntityManager em;

    public ServicePropertyPersistenceFacade() {
        super(ServiceProperty.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }


    public ServiceProperty find(
            String serviceId,
            String zoneId) {
        try {
            return em.createQuery("select prop from ServiceProperty prop " +
                    "where prop.service.id = :service_id and prop.zone.id = :zone_id", ServiceProperty.class)
                    .setParameter("service_id", Long.parseLong(serviceId))
                    .setParameter("zone_id", Long.parseLong(zoneId))
                    .getSingleResult();
        } catch (Exception ex) {
            log.debug(String.format("Could not retrieve service property, s_id=%s, z_id=%s",
                    serviceId,
                    zoneId), ex);
            return null;
        }
    }


    public ServiceProperty find(
            Service service,
            SubscriptionSetting zoneSetting) {
        try {
            return em.createQuery("select prop from ServiceProperty prop " +
                    "where prop.service.id = :service_id and prop.zone.id = :zone_id", ServiceProperty.class)
                    .setParameter("service_id", service.getId())
                    .setParameter("zone_id", Long.parseLong(zoneSetting.getValue()))
                    .getSingleResult();
        } catch (Exception ex) {
            log.debug(String.format("Could not retrieve service property, s_id=%d, z_id=%s",
                    service.getId(),
                    zoneSetting.getValue()), ex);
            return null;
        }
    }
}
