package com.jaravir.tekila.module.payment;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.accounting.entity.PaymentOption;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Created by kmaharov on 01.07.2016.
 */
@Stateless
public class PaymentOptionsPersistenceFacade extends AbstractPersistenceFacade<PaymentOption> {
    @PersistenceContext
    private EntityManager em;

    public PaymentOptionsPersistenceFacade() {
        super(PaymentOption.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public int getCodeByName(String name) {
        PaymentOption option = (PaymentOption) em.createQuery("SELECT c FROM " + getEntityClass().getName() + " c WHERE c.name LIKE :optionName")
                .setParameter("optionName", name).getSingleResult();
        return (int) option.getId();
    }

    public PaymentOption getOptionByName(String name) {
        PaymentOption option = (PaymentOption) em.createQuery("SELECT c FROM " + getEntityClass().getName() + " c WHERE c.name LIKE :optionName")
                .setParameter("optionName", name).getSingleResult();
        return option;
    }
}