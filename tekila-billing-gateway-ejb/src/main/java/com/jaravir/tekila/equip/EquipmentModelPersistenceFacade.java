package com.jaravir.tekila.equip;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.store.equip.Equipment;
import com.jaravir.tekila.module.store.equip.EquipmentModel;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by sajabrayilov on 12/18/2014.
 */
@Stateless
public class EquipmentModelPersistenceFacade extends AbstractPersistenceFacade<EquipmentModel>{
    @PersistenceContext
    private EntityManager em;

    public EquipmentModelPersistenceFacade() {
        super(EquipmentModel.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public EquipmentModel isModelExists (String modelName) {
        return em.createQuery("select distinct m from EquipmentModel m where m.name = :name", EquipmentModel.class)
                .setParameter("name", modelName).getSingleResult();
    }

    public EquipmentModel findByName (String modelName) {
        try {
            return em.createQuery("select m from EquipmentModel m where m.name = :name", EquipmentModel.class)
                    .setParameter("name", modelName).getSingleResult();
        }
        catch (Exception ex) {
            return null;
        }
    }
}
