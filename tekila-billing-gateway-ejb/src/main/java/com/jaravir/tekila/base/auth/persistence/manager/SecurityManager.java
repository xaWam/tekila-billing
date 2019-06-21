package com.jaravir.tekila.base.auth.persistence.manager;

import com.jaravir.tekila.base.auth.PathPrivilegeItem;
import com.jaravir.tekila.base.auth.Privilege;
import com.jaravir.tekila.base.auth.persistence.Group;
import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.exception.NoPrivilegeFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spring.security.PathPrivilegeStore;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;
import java.util.List;

import static spring.security.Constants.USER_GROUP;


/**
 * Created by sajabrayilov on 12/15/2014.
 */
@Stateless
public class SecurityManager {
    private final static Logger log = LoggerFactory.getLogger(SecurityManager.class);

    @Resource
    private SessionContext ctx;

    @EJB
    private UserPersistenceFacade userPersistenceFacade;


    public boolean checkPermissions(String subModuleName, Privilege privilege) throws NoPrivilegeFoundException {
        try {
            HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
            Group group = (Group) session.getAttribute(USER_GROUP);
            return group.hasPrivilige(subModuleName, privilege);
        } catch (Exception ex) {
            throw new NoPrivilegeFoundException();
        }
    }

    public boolean checkPermissionsForPath(String path) {
        try {
            if (path == null)
                throw new IllegalArgumentException("Path is required");
            if (path.startsWith("/pages/")) {
                path = path.replace("/pages/", "");
            } else if (path.startsWith("/export")) {
                path = path.replace("/export", "export");
            }
            List<PathPrivilegeItem> itemList = PathPrivilegeStore.check(path);
            if (itemList != null) {
                for (PathPrivilegeItem item : itemList) {
//                    log.info("checkPermissionsForPath **************************>>>>>>>>>>>>>>> result from "+checkUIPermissions(item.getSubModuleName(), item.getPrivilege()));
                    if (checkUIPermissions(item.getSubModuleName(), item.getPrivilege())) {
//                        log.info("checkPermissionsForPath ************************** result is true must not run again>>>>>>>>>>>>>>> ");
                        return true;
                    }


                }
            }
            return false;
        } catch (Exception ex) {
            log.error("checkPermissionsForPath failed", ex);
            return false;
        }
    }

    public boolean checkUIPermissions(String subModuleName, Privilege privilege) {
        try {
//            HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
//            Group group = (Group) session.getAttribute(USER_GROUP);
            User user = userPersistenceFacade.findByUserName(ctx.getCallerPrincipal().getName());
//            Group group = (Group) ctx.getContextData().get(USER_GROUP);

//            log.info("checkUIPermissions ************************** group name >>>>>>>>>>>>>>>"+group.getGroupName());
//            log.info("checkUIPermissions ************************** result >>>>>>>>>>>>>>> "+group.hasPrivilige(subModuleName, privilege));
            boolean res = user.getGroup().hasPrivilige(subModuleName, privilege);
            if(res){
                return true;
            }
        } catch (Exception ex) {
//            ex.printStackTrace();
//            log.error("checkUIPermissions ************************** error >>>>>>>>>>>>>>> "+ex);
            return false;
        }
        return false;
    }

    public boolean checkUIPermissionsForModule(String moduleName, Privilege privilege) {
        HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
        Group group = (Group) session.getAttribute(USER_GROUP);
        String username = (String) session.getAttribute("username");
        try {
            return group.hasPriviligeForMobule(moduleName, privilege);
        } catch (Exception ex) {
            log.error("Check permissions failed for user : {}", username);
            return false;
        }
    }

    public boolean isUserInRole(String groupName) {
        return ctx.isCallerInRole(groupName);
    }
}
