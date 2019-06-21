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
public class NoSuchSubscriptionException extends Exception {
    public NoSuchSubscriptionException () { super(); }
    public NoSuchSubscriptionException (String message) { super(message); }
    public NoSuchSubscriptionException (String message, Throwable cause) { super(message, cause); }
    public NoSuchSubscriptionException (Throwable cause) { super(cause); }
}
