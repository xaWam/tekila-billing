/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jaravir.tekila.module.web.service.exception;

/**
 *
 * @author sajabrayilov
 */
public class NoInvoiceFoundException extends Exception {

    /**
     * Creates a new instance of <code>NoInvoiceFoundException</code> without
     * detail message.
     */
    public NoInvoiceFoundException() {
    }

    /**
     * Constructs an instance of <code>NoInvoiceFoundException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public NoInvoiceFoundException(String msg) {
        super(msg);
    }
    
    public NoInvoiceFoundException(Throwable cause) {
        super(cause);
    }
    
    public NoInvoiceFoundException (String msg, Throwable cause) {
        super(msg, cause);
    }
}
