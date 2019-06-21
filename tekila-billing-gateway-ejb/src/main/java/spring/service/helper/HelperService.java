package spring.service.helper;

import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import spring.model.helper.MailDetails;
import spring.security.SecurityModuleUtils;
import javax.ejb.EJB;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static spring.util.Constants.INJECTION_POINT;

/**
 * @author MushfigM on 03.04.2019
 */
@Service
public class HelperService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @EJB(mappedName = INJECTION_POINT + "UserPersistenceFacade")
    private UserPersistenceFacade userPersistenceFacade;

    public void sendMailToUsers(MailDetails mailDetails){
        try {
            String currentWorkingUser = SecurityModuleUtils.getCurrentUserLogin();
            log.debug("Received /send mail to users/ request, current user {}", currentWorkingUser);

            User currentUser = userPersistenceFacade.findByUserName(currentWorkingUser);
            if(currentUser == null)
                throw new IllegalArgumentException("Nonauthorized user!");

            log.info("Current user email [{}] and other details: {}", currentUser.getEmail(), mailDetails.toString());
            List<User> listUsers;
            String users = mailDetails.getUsers();
            if (users.equalsIgnoreCase("all"))
                listUsers = userPersistenceFacade.findAllActive();
            else {
                listUsers = Stream.of(users.split(","))
                        .map(userName -> userPersistenceFacade.findByUserName(userName))
                        .collect(Collectors.toList());
            }

            userPersistenceFacade.sendBulkEmailNotification(listUsers, mailDetails.getBody(), mailDetails.getSubject());
            log.info("Sending mail method finished successfully, but sending process will continue asynchronously");
        } catch (Exception ex){
            log.error("Error occurs when sending email to users", ex);
            throw new RuntimeException(ex.getMessage());
        }
    }
}
