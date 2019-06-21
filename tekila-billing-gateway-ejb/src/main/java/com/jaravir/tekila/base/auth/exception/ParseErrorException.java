package com.jaravir.tekila.base.auth.exception;

/**
 * Created by khsadigov
 */
public class ParseErrorException extends Exception {

    public ParseErrorException() {
    }

    public ParseErrorException(String message) {
        super(message);
    }

    public ParseErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParseErrorException(Throwable cause) {
        super(cause);
    }

    public ParseErrorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
