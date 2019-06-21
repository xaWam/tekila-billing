package spring.exceptions;

/**
 * @author MushfigM on 6/1/2019
 */
public class SubscriptionNotFoundException extends RuntimeException{

    public SubscriptionNotFoundException() {
        super();
    }

    public SubscriptionNotFoundException(String message) {
        super(message);
    }

    public SubscriptionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
