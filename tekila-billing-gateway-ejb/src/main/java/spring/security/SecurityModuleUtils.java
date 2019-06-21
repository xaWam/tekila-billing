package spring.security;

import spring.security.wrapper.Holder;
import spring.security.wrapper.Subject;
import spring.security.wrapper.SubjectApplicationDTO;
import spring.security.wrapper.SubjectProviderDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author MusaAl
 * @date 4/26/2018 : 3:50 PM
 */
public final class SecurityModuleUtils {


    public static final int PASSWORD_MIN_LENGTH = 4;

    public static final int PASSWORD_MAX_LENGTH = 100;

    private SecurityModuleUtils() {
    }

    /**
     * Get the login of the current user.
     *
     * @return the login of the current user
     */
    public static String getCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        String userName = null;
        if (authentication != null) {
            if (authentication.getPrincipal() instanceof Holder) {
                Holder principal = (Holder) authentication.getPrincipal();
                UserDetails springSecurityUser = (UserDetails) principal.getSpringUser();
                userName = springSecurityUser.getUsername();
            } else if (authentication.getPrincipal() instanceof Holder) {
                userName = ((Holder) authentication.getPrincipal()).getSpringUser().getUsername();
            }
        }
        return userName;
    }

    /**
     * Get the JWT of the current user.
     *
     * @return the JWT of the current user
     */
    public static String getCurrentUserJWT() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        if (authentication != null && authentication.getCredentials() instanceof String) {
            return (String) authentication.getCredentials();
        }
        return null;
    }

    /**
     * Check if a user is authenticated.
     *
     * @return true if the user is authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                    .noneMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(AuthoritiesConstants.PERMISSION1));
        }
        return false;
    }

    /**
     * If the current user has a specific authority (security permission).
     * <p>
     * The name of this method comes from the isPermitted() method in the Servlet API
     *
     * @param authority the authority to check
     * @return true if the current user has the authority, false otherwise
     */
    public static boolean isPermitted(String authority) {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
        }
        return false;
    }

    /**
     * Get the Authorities of the current user.
     *
     * @return the Set<Authority> </> of the current user
     */
    public static Set<String> getCurrentSubjectAuthorities() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();

        Set<String> authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());

        return authorities;
    }

    /**
     * Get the Current user full information.
     *
     * @return the Holder of the current user
     */
    public static Holder getCurrentSecurityHolder(){
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();

        if(authentication != null && authentication instanceof Holder){
            Holder securityHolder = (Holder) authentication.getPrincipal();
            return securityHolder;
        }
        return null;
    }

    /**
     * Get the Current user.
     *
     * @return Subject of the current user
     */
    public static Subject getCurrentSubject(){
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();

        if(authentication != null && authentication.getPrincipal() instanceof Holder){
            Holder securityHolder = (Holder) authentication.getPrincipal();
            return securityHolder.getUser();
        }
        return null;
    }

    /**
     * Get the Current user granted Applications.
     *
     * @return Authorized Applications of the current user
     */
    public static List<SubjectApplicationDTO> getCurrentSubjectGrantedApplications(){
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();

        if(authentication != null && authentication.getPrincipal() instanceof Holder){
            Holder securityHolder = (Holder) authentication.getPrincipal();
            return securityHolder.getUser().getApplications();
        }
        return null;
    }

    /**
     * Get the Current user granted Providers.
     *
     * @return Authorized Providers of the current user
     */
    public static List<SubjectProviderDTO> getCurrentSubjectGrantedProviders(){
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();

        if(authentication != null && authentication.getPrincipal() instanceof Holder){
            Holder securityHolder = (Holder) authentication.getPrincipal();
            return securityHolder.getUser().getProviders();
        }
        return null;
    }

    public static boolean checkPasswordLength(String password) {
        return !StringUtils.isEmpty(password) &&
                password.length() >= PASSWORD_MIN_LENGTH &&
                password.length() <= PASSWORD_MAX_LENGTH;
    }

}
