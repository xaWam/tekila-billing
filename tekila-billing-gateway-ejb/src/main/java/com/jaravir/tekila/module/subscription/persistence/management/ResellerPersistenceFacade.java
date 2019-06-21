package com.jaravir.tekila.module.subscription.persistence.management;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.subscription.persistence.entity.Reseller;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class ResellerPersistenceFacade extends AbstractPersistenceFacade<Reseller> {
    @PersistenceContext
    private EntityManager em;

    public ResellerPersistenceFacade() {
        super(Reseller.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return this.em;
    }

    public Reseller findByUsernameAndPassword(String username, String password) {
        return em.createQuery("select r from Reseller r where r.username = :username and r.password = :password", Reseller.class)
                .setParameter("username", username)
                .setParameter("password", password)
                .getSingleResult();
    }

    public Reseller findByUsernameAndPassword2(String username, String password, long provider) {
        return em.createQuery("select r from Reseller r " +
                "where r.username = :username " +
                "and r.password = :password " +
                "and r.provider.id = :prov ", Reseller.class)
                .setParameter("username", username)
                .setParameter("password", password)
                .setParameter("prov", provider)
                .getSingleResult();
    }

    public Reseller findByUsername(String username) {
        return em.createQuery("select r from Reseller r where r.username = :username", Reseller.class)
                .setParameter("username", username)
                .getSingleResult();
    }

    public List<Reseller> findByProviderId(Long providerId) {
        return em.createQuery("select r from Reseller r where r.provider.id = :providerId", Reseller.class)
                .setParameter("providerId", providerId)
                .getResultList();
    }
}
