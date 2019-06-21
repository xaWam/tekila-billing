package reseller;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.module.auth.security.PasswordGenerator;
import com.jaravir.tekila.module.subscription.persistence.entity.Reseller;
import com.jaravir.tekila.module.subscription.persistence.management.ResellerPersistenceFacade;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

@Stateless
@Path("password")
@Transactional
public class UserEndpoint {
    private final static Logger log = Logger.getLogger(UserEndpoint.class);
    @EJB
    private ResellerPersistenceFacade resellerPersistenceFacade;
    @EJB
    private ResellerFetcher resellerFetcher;
    @EJB
    private PasswordGenerator passwordGenerator;
    @EJB
    private UserPersistenceFacade userPersistenceFacade;

    @GET
    @Path("get")
    @JWTTokenNeeded
    @Produces(TEXT_PLAIN)
    public String getPassword(@Context HttpServletRequest request) {
        Reseller reseller = resellerFetcher.getReseller(request);
        return reseller.getPassword();
    }

    public static class PasswordParams {
        public String password;
    }

    @POST
    @Path("set")
    @JWTTokenNeeded
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response resetPassword(
            PasswordParams passwordParams,
            @Context HttpServletRequest request) {
        Reseller reseller = resellerFetcher.getReseller(request);
        String password = passwordParams.password;
        log.info(String.format("UserEndpoint.resetPassword()...(%s, %s)", reseller.getUsername(), password));

        try {
            String encodedPassword = passwordGenerator.encodePassword(password.toCharArray());

            User user = reseller.getUser();
            user.setPassword(encodedPassword);
            user.setUsedPasswords(user.getUsedPasswords()+","+encodedPassword);
            user.setLastPasswordChanged(DateTime.now());
            userPersistenceFacade.update(user, password);

            reseller.setPassword(password);
            resellerPersistenceFacade.update(reseller);

            return Response.ok().entity("Password has been resetted").build();
        } catch (Exception e) {
            return Response.status(UNAUTHORIZED).build();
        }
    }
}
