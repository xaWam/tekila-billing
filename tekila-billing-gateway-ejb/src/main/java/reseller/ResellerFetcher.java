package reseller;

import com.jaravir.tekila.module.subscription.persistence.entity.Reseller;
import com.jaravir.tekila.module.subscription.persistence.management.ResellerPersistenceFacade;
import io.jsonwebtoken.Jwts;
import org.apache.log4j.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;

@Stateless
public class ResellerFetcher {
    @EJB
    private ResellerPersistenceFacade resellerFacade;
    private final static Logger log = Logger.getLogger(ResellerFetcher.class);

    public Reseller getReseller(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Extract the token from the HTTP Authorization header
        String token = authorizationHeader.substring("Bearer".length()).trim();
        log.debug(String.format("createSubscription.token = %s", token));

        String resellerId =
                Jwts.parser().setSigningKey("secretkey1").parseClaimsJws(token).getBody().getSubject();
        log.debug("#### valid token : " + token);
        log.debug(String.format("resellerId = %s", resellerId));
        Reseller reseller = resellerFacade.find(Long.parseLong(resellerId));
        log.debug(String.format("reseller name = %s, user name = %s", reseller.getName(), reseller.getUser().getUserName()));
        return reseller;
    }
}
