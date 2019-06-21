package spring.exceptions;

/**
 * @author ElmarMa on 5/11/2018
 */
public class UserOperationException extends RuntimeException {

    public UserOperationException() {
    }

    public UserOperationException(String message) {
        super(message);
    }

    public UserOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
