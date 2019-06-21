package spring.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author MusaAl
 * @date 4/4/2018 : 6:39 PM
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestAlertException extends RuntimeException{
    public BadRequestAlertException(String msg){
        super(msg);
    }
}
