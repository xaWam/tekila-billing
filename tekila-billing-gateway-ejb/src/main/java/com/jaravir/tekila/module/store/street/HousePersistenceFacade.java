package com.jaravir.tekila.module.store.street;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.House;
import com.jaravir.tekila.module.subscription.persistence.entity.Streets;
import org.apache.log4j.Logger;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Created by shnovruzov on 9/25/2016.
 */
@Stateless
public class HousePersistenceFacade extends AbstractPersistenceFacade<House> {
    private final static Logger log = Logger.getLogger(HousePersistenceFacade.class);

    @PersistenceContext
    private EntityManager em;
    @Resource
    private EJBContext ctx;

    public HousePersistenceFacade() {
        super(House.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public House findByHouseNo(String no) {
        try {
            List<House> houseList = findAll();
            for (House house : houseList)
                if (no.equals(house.getHouseNo())) {
                    return house;
                }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }
}
