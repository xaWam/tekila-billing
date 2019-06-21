package spring.service;

import com.jaravir.tekila.base.auth.UserStatus;
import com.jaravir.tekila.base.auth.persistence.User;
import com.jaravir.tekila.base.auth.persistence.manager.UserPersistenceFacade;
import com.jaravir.tekila.module.auth.security.PasswordGenerator;
import org.springframework.stereotype.Service;
import spring.dto.UserDTO;
import spring.dto.UserDTOExtended;
import spring.exceptions.UserOperationException;

import static spring.util.Constants.INJECTION_POINT;

import javax.ejb.EJB;

/**
 * @author ElmarMa on 4/20/2018
 */
@Service
public class UserService {


    @EJB(mappedName = INJECTION_POINT + "UserPersistenceFacade")
    private UserPersistenceFacade userPersistenceFacade;


    @EJB(mappedName = INJECTION_POINT + "PasswordGenerator")
    private PasswordGenerator passwordGenerator;

    public void cloneUserIntoBilling(UserDTO userDTO) {

        char[] rawPass = userDTO.getPassword().toCharArray();
        User user = new User();
        user.setUserName(userDTO.getUsername());
        user.setPassword(passwordGenerator.encodePassword(rawPass));
        user.setEmail(userDTO.getEmail());
        user.setSurname(userDTO.getFirstName());
        user.setFirstName(userDTO.getFirstName());
        user.setStatus(UserStatus.ACTIVE);
        try {
            userPersistenceFacade.save(user, String.valueOf(rawPass), false);
        } catch (Exception ex) {
            throw new UserOperationException("can not create user ," + ex.getMessage());
        }
    }

    public UserDTOExtended getUserDataForCloningToSecurity(String username) {
        User user = null;
        try {
            user = userPersistenceFacade.findByUserName(username);
        } catch (Exception ex) {

        }
        if (user == null) {
            throw new UserOperationException("user not found with this username -> " + username);
        }
        UserDTOExtended extended = new UserDTOExtended(user);
        return extended;
    }

}
