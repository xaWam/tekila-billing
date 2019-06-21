/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jaravir.tekila.module.store;

import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.store.scratchcard.persistence.entity.Serial;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.log4j.Logger;

/**
 * @created by shnovruzov on 28.04.2016
 */
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class SerialPersistenceFacade extends AbstractPersistenceFacade<Serial> {

    @Resource
    private SessionContext ctx;
    @PersistenceContext
    private EntityManager em;
    private final static Logger log = Logger.getLogger(SerialPersistenceFacade.class);

    public SerialPersistenceFacade() {
        super(Serial.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public Long findLast() {
        try {
            return (Long) em.createQuery("select MAX(sr.id) from Serial sr").getSingleResult();
        } catch (Exception ex) {
            return null;
        }
    }

}
