package com.jaravir.tekila.equip;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.store.equip.EquipmentBrand;
import com.jaravir.tekila.module.store.equip.EquipmentModel;
import com.jaravir.tekila.module.store.equip.EquipmentType;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by sajabrayilov on 12/18/2014.
 */
@Stateless
public class EquipmentTypePersistenceFacade extends AbstractPersistenceFacade<EquipmentType>{
    @PersistenceContext
    private EntityManager em;

    public EquipmentTypePersistenceFacade() {
        super(EquipmentType.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public EquipmentType isModelExists (String modelName) {
        return em.createQuery("select distinct m from EquipmentType m where m.name = :name", EquipmentType.class)
                .setParameter("name", modelName).getSingleResult();
    }

    public EquipmentType findByName (String modelName) {
        try {
            return em.createQuery("select m from EquipmentType m where m.name = :name", EquipmentType.class)
                    .setParameter("name", modelName).getSingleResult();
        }
        catch (Exception ex) {
            return null;
        }
    }
}
