package com.jaravir.tekila.equip.price;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.store.equip.price.EquipmentPrice;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by sajabrayilov on 12/19/2014.
 */
@Stateless
public class EquipmentPricePersistenceFactory extends AbstractPersistenceFacade<EquipmentPrice> {
    @PersistenceContext
    private EntityManager em;

    public EquipmentPricePersistenceFactory () {
        super(EquipmentPrice.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
}
