package com.jaravir.tekila.equip;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.accounting.AccountingStatus;
import com.jaravir.tekila.module.accounting.InvoiceState;
import com.jaravir.tekila.module.accounting.entity.SalesPartnerCharge;
import com.jaravir.tekila.module.accounting.entity.SalesPartnerInvoice;
import com.jaravir.tekila.module.accounting.manager.SalesPartnerChargePersitenceFacade;
import com.jaravir.tekila.module.sales.persistence.entity.SalesPartner;
import com.jaravir.tekila.module.store.equip.Equipment;
import com.jaravir.tekila.module.store.equip.EquipmentBrand;
import com.jaravir.tekila.module.store.equip.EquipmentModel;
import com.jaravir.tekila.module.system.SystemEvent;
import com.jaravir.tekila.module.system.log.SystemLogger;
import org.apache.log4j.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Created by sajabrayilov on 12/18/2014.
 */
@Stateless
public class EquipmentBrandPersistenceFacade extends AbstractPersistenceFacade<EquipmentBrand>{
    @PersistenceContext
    private EntityManager em;

    public EquipmentBrandPersistenceFacade() {
        super(EquipmentBrand.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public EquipmentBrand isModelExists (String modelName) {
        return em.createQuery("select distinct m from EquipmentBrand m where m.name = :name", EquipmentBrand.class)
                .setParameter("name", modelName).getSingleResult();
    }

    public EquipmentBrand findByName (String modelName) {
        try {
            return em.createQuery("select m from EquipmentBrand m where m.name = :name", EquipmentBrand.class)
                    .setParameter("name", modelName).getSingleResult();
        }
        catch (Exception ex) {
            return null;
        }
    }



}
