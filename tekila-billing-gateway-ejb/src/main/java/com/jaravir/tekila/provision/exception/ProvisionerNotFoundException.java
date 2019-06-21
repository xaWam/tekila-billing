package com.jaravir.tekila.provision.exception;

/**
 * Created by sajabrayilov on 11/18/2014.
 */
public class ProvisionerNotFoundException extends Exception {
    private static String exceptionMessage = "Cannot find provisioner";

    public ProvisionerNotFoundException() {
        super(exceptionMessage);
    }

    public ProvisionerNotFoundException(String message) {
        super(exceptionMessage + " " + message);
    }

    public ProvisionerNotFoundException(String message, Throwable cause) {
        super(exceptionMessage + " " + message, cause);
    }

    public ProvisionerNotFoundException(Throwable cause) {
        super(exceptionMessage, cause);
    }

    public ProvisionerNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(exceptionMessage + " " + message, cause, enableSuppression, writableStackTrace);
    }
}
