package reseller;

import com.jaravir.tekila.module.subscription.persistence.entity.Reseller;
import com.jaravir.tekila.module.subscription.persistence.management.ResellerPersistenceFacade;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import java.io.UnsupportedEncodingException;
import java.security.Key;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

@Stateless
@Path("login")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Transactional
public class LoginEndpoint {
    private final static Logger log = Logger.getLogger(LoginEndpoint.class);
    @EJB
    private KeyProvider keyProvider;
    @EJB
    private ResellerPersistenceFacade resellerPersistenceFacade;

    public static class GroupDTO {
        public long groupId;
        public String groupName;

        GroupDTO() {
        }

        GroupDTO(long groupId, String groupName) {
            this.groupId = groupId;
            this.groupName = groupName;
        }
    }

    @POST
    public Response authenticateUser(LoginCredentials credentials) {
        log.info(String.format("LoginEndpoint.authenticateUser()...(%s, %s, %s)", credentials.username, credentials.password, credentials.provider));
        try {
            Reseller reseller = authenticate(credentials.username, credentials.password, credentials.provider);
            String token = issueToken(String.valueOf(reseller.getId()));
            return Response.ok().header(AUTHORIZATION, "Bearer " + token).entity(
                    new GroupDTO(reseller.getUser().getGroup().getId(),
                            reseller.getUser().getGroup().getGroupName())
            ).build();
        } catch (Exception e) {
            return Response.status(UNAUTHORIZED).build();
        }
    }

    private String issueToken(String resellerId) {
        Key key = keyProvider.getKey();
        String jwtToken = Jwts.builder()
                .setSubject(resellerId)
                //.setIssuer(uriInfo.getAbsolutePath().toString())
                .setIssuedAt(DateTime.now().toDate())
                .setExpiration(DateTime.now().plusHours(10).toDate())
                .signWith(SignatureAlgorithm.HS512, "secretkey1")
                .compact();
        return jwtToken;
    }

    private Reseller authenticate(String username, String password, long provider) {
        Reseller reseller = resellerPersistenceFacade.findByUsernameAndPassword2(username, password, provider);
        if (reseller == null) {
            throw new RuntimeException("Not authorized for reseller api");
        }
        return reseller;
    }
}
