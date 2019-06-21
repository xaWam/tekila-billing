package spring.exceptions;

public class AdjustmentException extends CustomerOperationException {

    public AdjustmentException() {
        super();
    }

    public AdjustmentException(String message) {
        super(message);
    }

    public AdjustmentException(String message, Throwable cause) {
        super(message, cause);
    }

}
