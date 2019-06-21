package spring.exceptions;

/**
 * @author MushfigM on 5/13/2019
 */
public class VasException extends CustomerOperationException {

    public VasException() {
        super();
    }

    public VasException(String message) {
        super(message);
    }

    public VasException(String message, Throwable cause) {
        super(message, cause);
    }
}
