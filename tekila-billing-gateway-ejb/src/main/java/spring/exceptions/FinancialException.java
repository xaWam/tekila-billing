package spring.exceptions;

/**
 * @author ElmarMa on 4/9/2018
 */
public class FinancialException extends RuntimeException {

    public FinancialException() {
        super();
    }

    public FinancialException(String message) {
        super(message);
    }

    public FinancialException(String message, Throwable cause) {
        super(message, cause);
    }
}
