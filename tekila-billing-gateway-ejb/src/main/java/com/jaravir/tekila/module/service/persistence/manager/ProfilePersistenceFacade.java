package com.jaravir.tekila.module.service.persistence.manager;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.service.entity.Profile;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class ProfilePersistenceFacade extends AbstractPersistenceFacade<Profile> {
    @PersistenceContext
    private EntityManager em;

    public ProfilePersistenceFacade() {
        super(Profile.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
}
