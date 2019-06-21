package com.jaravir.tekila.module.auth;

import com.jaravir.tekila.base.auth.Privilege;
import com.jaravir.tekila.base.auth.persistence.Group;
import com.jaravir.tekila.base.auth.persistence.Module;
import com.jaravir.tekila.base.auth.persistence.Permission;
import com.jaravir.tekila.base.auth.persistence.Role;
import com.jaravir.tekila.base.filter.Filterable;
import com.jaravir.tekila.base.filter.MatchingOperation;
import com.jaravir.tekila.base.persistence.facade.AbstractPersistenceFacade;
import com.jaravir.tekila.module.service.entity.ServiceProvider;
import com.jaravir.tekila.base.auth.PermissionRow;
import org.apache.log4j.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sajabrayilov on 12/8/2014.
 */
@Stateless
public class RolePersistenceFacade extends AbstractPersistenceFacade<Role> {
    @PersistenceContext
    private EntityManager em;
    @EJB private PermissionPersistenceFacade permissionPersistenceFacade;
    @EJB private GroupPersistenceFacade groupPersistenceFacade;

    private final static Logger log = Logger.getLogger(RolePersistenceFacade.class);

    public RolePersistenceFacade() {
        super(Role.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public enum Filter implements Filterable {
        ID("id"),
        NAME("name"),
        PROVIDERNAME("provider.name"),
        MODULENAME("module.name"),
        DSC("dsc");

        private final String field;
        private MatchingOperation operation;

        Filter(String field) {
            this.field = field;
            this.operation = MatchingOperation.LIKE;
        }

        public String getField() {
            return field;
        }

        @Override
        public MatchingOperation getOperation() {
            return operation;
        }

        public void setOperation(MatchingOperation operation) {
            this.operation = operation;
        }
    }

    public void create (Role role, Module module, ServiceProvider provider, List<PermissionRow> rowList) {
        createRole(role, module, provider, rowList);
        save(role);

        log.debug("Role: " + role);
    }

    public void edit (Role role, Module module, ServiceProvider provider, List<PermissionRow> rowList) {
        createRole(role, module, provider, rowList);
        update(role);
    }

    private void createRole (Role role, Module module, ServiceProvider provider, List<PermissionRow> rowList) {
        if (rowList == null || rowList.isEmpty())
            throw new IllegalArgumentException("Permission rows are required");

        List<Permission> permissionList = new ArrayList<>();
        Permission permission = null;

        log.debug("Starting Permission row iteration");
        List<Permission> permList = null;

        for (PermissionRow row : rowList) {
            if (row.getSelectedPrivilegeList() != null && !row.getSelectedPrivilegeList().isEmpty()) {
               /* permList = permissionPersistenceFacade.findByPrivileges(row);

                log.debug("Permission row: " + row);
                log.debug("Permissions list form DB: " + permList);

                if (permList != null) {
                    for (Permission perm : permissionList) {
                        if (perm.getPrivilege().size() == row.getSelectedPrivilegeList().size()) {
                            permission = perm;
                            break;
                        }
                    }
                }

                log.debug("Permission: " + permission);
                log.debug("Should skip iteration: " + (permission != null || row.getSelectedPrivilegeList() == null
                        || row.getSelectedPrivilegeList().isEmpty()));
                */
                if (permission == null) {
                    permission = new Permission();
                    permission.setSubModule(row.getSubModule());
                    permission.setPrivilege(row.getSelectedPrivilegeListAsPrivs());
                    permission.setSubModule(row.getSubModule());
                }

                if (permission != null)
                    permissionList.add(permission);

                log.debug("Permission list after iter: " + permissionList);
                permission = null;
            }

            permList = null;
        }

        role.setModule(module);
        role.setProvider(provider);
        role.setPermissions(permissionList);

        log.debug("Role: " + role);
    }
}
