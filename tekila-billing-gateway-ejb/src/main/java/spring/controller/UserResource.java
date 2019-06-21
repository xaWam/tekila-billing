package spring.controller;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.dto.UserDTO;
import spring.dto.UserDTOExtended;
import spring.exceptions.UserOperationException;
import spring.service.UserHolder;
import spring.service.UserService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author ElmarMa on 4/20/2018
 */
@RestController
public class UserResource {

    private static final Logger log = Logger.getLogger(UserResource.class);


    @Autowired
    private UserService userService;

    @RequestMapping("/user/clone")
    @PostMapping
    public ResponseEntity<Void> cloneUserFromSecurityModule(@RequestBody UserDTO incomingUser) {
        userService.cloneUserIntoBilling(incomingUser);
        return ResponseEntity.status(201).build();
    }


    @RequestMapping("/user/remember")
    @PostMapping
    public ResponseEntity<Void> saveInMemory(@RequestBody UserDTO incomingUser) {
        try {
            UserHolder.credentials.put(incomingUser.getUsername(), incomingUser.getPassword());
        } catch (Exception e) {
            throw new UserOperationException("can not remember user credentials : " + e.getMessage(), e);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @Autowired
    HttpServletRequest request;


    @RequestMapping("/user/clone-to-security")
    @PostMapping
    public ResponseEntity<UserDTOExtended> checkUserIfExistInTekila(@RequestBody UserDTO incomingUser) {
        try {
            log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + request);
            log.info(incomingUser);
            request.login(incomingUser.getUsername(), incomingUser.getPassword());
            UserDTOExtended extended = userService.getUserDataForCloningToSecurity(incomingUser.getUsername());
            log.info(extended);
            return ResponseEntity.ok(extended);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            log.info(e.getCause());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }


}
