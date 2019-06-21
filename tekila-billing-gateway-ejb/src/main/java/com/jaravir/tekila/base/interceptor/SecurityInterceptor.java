package com.jaravir.tekila.base.interceptor;

import com.jaravir.tekila.base.auth.Privilege;
import com.jaravir.tekila.base.auth.exception.NotPermittedException;
import com.jaravir.tekila.base.auth.persistence.exception.NoPrivilegeFoundException;
import com.jaravir.tekila.base.auth.persistence.manager.SecurityManager;
import com.jaravir.tekila.base.interceptor.annot.PermissionRequired;
import org.apache.log4j.Logger;

import javax.ejb.EJB;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
/**
 * Created by sajabrayilov on 12/15/2014.
 */

public class SecurityInterceptor {
    private final static Logger log = Logger.getLogger(SecurityInterceptor.class);
    @EJB private SecurityManager securityManager;

    @AroundInvoke
    public Object authorize(InvocationContext ctx) throws NotPermittedException{
        log.debug("Interceptor SecurityInterceptor called on " + ctx.getTarget().getClass().getName());
        Object result = null;
        try {
            PermissionRequired pe = ctx.getMethod().getAnnotation(PermissionRequired.class);
            String module = pe.module();
            String subModule = pe.subModule();
            Privilege privilege = pe.privilege();
            boolean res = securityManager.checkPermissions(subModule, privilege);
            if (res)
                return ctx.proceed();
        }
        catch (NoPrivilegeFoundException ex) {
            log.error("Privilege not found. Check permissions failed", ex);
            throw new NotPermittedException();
        }
        catch (Exception ex) {
            log.error("Cannot authorize: ", ex);
        }
        return result;
    }
}
