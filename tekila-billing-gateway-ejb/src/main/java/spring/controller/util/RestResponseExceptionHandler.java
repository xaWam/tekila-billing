package spring.controller.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import spring.controller.util.ErrorResponse;
import spring.exceptions.*;

import static spring.util.Constants.BASE_ERROR_MESSAGE;
import static spring.controller.util.ErrorResponse.errorCodes;

/**
 * @author ElmarMa on 3/15/2018
 */

@ControllerAdvice
public class RestResponseExceptionHandler extends ResponseEntityExceptionHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @ExceptionHandler(value = {CustomerOperationException.class, AdjustmentException.class, FinancialException.class, UserOperationException.class, VasException.class, SubscriptionNotFoundException.class})
    protected ResponseEntity<Object> handleCustomExceptions(RuntimeException ex, WebRequest request) {

        log.debug("RestResponseExceptionHandler custom exception message: {}, WebRequest: {}", ex.toString(), request.toString());

        ErrorResponse errorResponse = new ErrorResponse();
        String message = ex.getMessage() == null ? "" : ex.getMessage().contains("java") ? "" : ", " + ex.getMessage();

        errorResponse.setMessage(BASE_ERROR_MESSAGE + message);

        errorResponse.setCode(errorCodes.get(ex.getClass()));

        errorResponse.setDescription(message);
        //errorResponse.setDescription(Arrays.toString(ex.getStackTrace()));

        return handleExceptionInternal(ex, errorResponse, new HttpHeaders(), HttpStatus.OK, request);
    }

    @ExceptionHandler(value = {BadRequestAlertException.class})
    protected ResponseEntity<Object> handleBadRequestExceptions(RuntimeException ex, WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse();

        errorResponse.setMessage(BASE_ERROR_MESSAGE + ", " + ex.getMessage());

        errorResponse.setCode(errorCodes.get(ex.getClass()));

        errorResponse.setDescription(ex.getMessage());
        //errorResponse.setDescription(Arrays.toString(ex.getStackTrace()));

        return handleExceptionInternal(ex, errorResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }


}
