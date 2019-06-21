package spring.security;

import org.springframework.security.core.AuthenticationException;

/**
 * @author MusaAl
 * @date 4/26/2018 : 4:43 PM
 */
public class SecurityModuleException extends AuthenticationException{

    public SecurityModuleException(String msg, Throwable t) {
        super(msg, t);
    }
    public SecurityModuleException(String msg) {
        super(msg);
    }
}
