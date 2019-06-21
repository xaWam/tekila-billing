/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jaravir.tekila.base.auth.persistence.manager;

import com.jaravir.tekila.base.auth.persistence.ExternalUser;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author sajabrayilov
 */
@Stateless
public class ExternalUserPersistenceFacade extends AbstractPersistenceFacade<ExternalUser> {

    @PersistenceContext
    private EntityManager em;

    public ExternalUserPersistenceFacade() {
        super(ExternalUser.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public ExternalUser find(String userName) {
        return getEntityManager().createQuery("select u from ExternalUser u where u.username = :name", ExternalUser.class)
                .setParameter("name", userName).getSingleResult();
    }

    public boolean authenticate(String username, String password) {
        try {
            return find(username).getPasswd().equals(password);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAllowed(String username, String ip) {
        try {
            ExternalUser user = find(username);

            if (user.getAllowedAddresses() == null) {
                return true;
            }

            String[] allowedList = user.getAllowedAddresses().split(",");

            for (String s : allowedList) {
                if (s.equals(ip)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

}
