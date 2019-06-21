package com.jaravir.tekila.module.service.persistence.manager;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.service.entity.ServiceProperty;
import com.jaravir.tekila.module.service.entity.Zone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class ZonePersistenceFacade extends AbstractPersistenceFacade<Zone> {

    private final Logger log = LoggerFactory.getLogger(ZonePersistenceFacade.class);

    @PersistenceContext
    private EntityManager em;

    public ZonePersistenceFacade() {
        super(Zone.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }


    public List<Zone> findAllByService(Long serviceId){

        String sql = "select distinct serviceProperty.zone from ServiceProperty serviceProperty " +
                "left join fetch serviceProperty.zone " +
                "where serviceProperty.service.id = :serviceId";
        try{
            return em.createQuery(sql, Zone.class).setParameter("serviceId", serviceId).getResultList();
        }catch (Exception ex){
            log.error(ex.toString());
            return null;
        }

    }

    public List<Zone> findAllByService2(Long serviceId){

        String sql = "select distinct zone from Zone zone " +
                "join ServiceProperty serviceProperty on zone.id = serviceProperty.zone.id " +
                "join Service service on service.id = serviceProperty.service.id " +
                "where service.id = :serviceId";
        try{
            return em.createQuery(sql, Zone.class).setParameter("serviceId", serviceId).getResultList();
        }catch (Exception ex){
            log.error(ex.toString());
            return null;
        }

    }

    public List<Zone> findAllByService3(Long serviceId){

        String sql = "select distinct zone from Zone zone " +
                "join ServiceProperty serviceProperty on zone.id = serviceProperty.zone.id " +
                "where serviceProperty.service.id = :serviceId";
        try{
            return em.createQuery(sql, Zone.class).setParameter("serviceId", serviceId).getResultList();
        }catch (Exception ex){
            log.error(ex.toString());
            return null;
        }

    }


    public List<Zone> findAllByService4(Long serviceId){

        String sql = "select distinct serviceProperty from ServiceProperty serviceProperty " +
                "left join fetch serviceProperty.zone " +
                "left join serviceProperty.service service " +
                "where service.id = :serviceId";
        try{
            return em.createQuery(sql, ServiceProperty.class)
                    .setParameter("serviceId", serviceId)
                    .getResultList()
                    .stream()
                    .map(ServiceProperty::getZone)
                    .collect(Collectors.toList());
        }catch (Exception ex){
            log.error(ex.toString());
            return null;
        }

    }

}
