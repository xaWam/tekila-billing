package spring.exceptions;

/**
 * @author ElmarMa on 3/16/2018
        */
public class CustomerOperationException extends RuntimeException {

    public CustomerOperationException() {
        super();
    }

    public CustomerOperationException(String message) {
        super(message);
    }

    public CustomerOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
