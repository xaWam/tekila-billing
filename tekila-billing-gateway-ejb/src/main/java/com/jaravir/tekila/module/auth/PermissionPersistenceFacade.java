package com.jaravir.tekila.module.auth;

import com.jaravir.tekila.base.auth.PermissionRow;
import com.jaravir.tekila.base.auth.Privilege;
import com.jaravir.tekila.base.auth.persistence.Group;
import com.jaravir.tekila.base.auth.persistence.Permission;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Created by sajabrayilov on 12/8/2014.
 */
@Stateless
public class PermissionPersistenceFacade extends AbstractPersistenceFacade<Permission> {
    @PersistenceContext
    private EntityManager em;

    public PermissionPersistenceFacade() {
        super(Permission.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public List<Permission> findByPrivileges (PermissionRow row) {
        List<Permission> permissionList = em.createQuery(
            "select p from Permission p where p.id in (select p.id from Permission p join p.privilegeList priv where p.subModule = :sub and priv in :privs group by p.id having count(p.id) = :privsCount)")
                .setParameter("privs", row.getSelectedPrivilegeListAsPrivs())
                .setParameter("privsCount", row.getSelectedPrivilegeListAsPrivs().size())
                .setParameter("sub", row.getSubModule())
                .getResultList();
        return permissionList.size() > 0 ? permissionList : null;
    }

    public Permission findByPrivileges (String subModule, Privilege privilege) {
        List<Permission> permissionList = em.createQuery(
                "select p from Permission p join p.privilegeList priv where lower(p.subModule.name) = lower(:sub) and priv in :privs")
                .setParameter("privs", privilege)
                .setParameter("sub", subModule)
                .getResultList();
        return permissionList.size() > 0 ? permissionList.get(0) : null;
    }
}
