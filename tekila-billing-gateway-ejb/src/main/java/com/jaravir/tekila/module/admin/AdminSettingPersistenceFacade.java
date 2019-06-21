package com.jaravir.tekila.module.admin;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.admin.setting.AdminSetting;

import javax.annotation.Resource;
import com.jaravir.tekila.module.store.scratchcard.Settings.ScratchCardSettingType;
import com.jaravir.tekila.module.admin.setting.AdminSetting;
import com.jaravir.tekila.module.store.scratchcard.persistence.entity.ScratchCardBlockingSetting;
import org.apache.log4j.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.log4j.Logger;

/**
 * Created by shnovruzov on 5/24/2016.
 */

import javax.persistence.Query;

/**
 * Created by shnovruzov on 5/6/2016.
 */
@Stateless
public class AdminSettingPersistenceFacade extends AbstractPersistenceFacade<AdminSetting> {

    @PersistenceContext
    private EntityManager em;
    @Resource
    private EJBContext ctx;

    private static final Logger log = Logger.getLogger(AdminSettingPersistenceFacade.class);


    public AdminSettingPersistenceFacade() {
        super(AdminSetting.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public ScratchCardBlockingSetting getBlockingSetting() {
        Query query = em.createQuery("select bs from ScratchCardBlockingSetting bs");
        try {
            Object res = query.getSingleResult();
            return (ScratchCardBlockingSetting) res;
        } catch (Exception ex) {
            return null;
        }
    }

}
