package spring.controller.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.model.helper.MailDetails;
import spring.service.helper.HelperService;
import javax.validation.Valid;

/**
 * @author MushfigM on 03.04.2019
 */
@RestController
@RequestMapping("/helper")
public class HelperController {

    @Autowired
    private HelperService helperService;

    @RequestMapping(method = RequestMethod.POST, value = "/users/sendmail")
    public ResponseEntity<Void> sendMailToUsers(@Valid @RequestBody MailDetails mailDetails) {
        helperService.sendMailToUsers(mailDetails);
        return ResponseEntity.ok().build();
    }
}
