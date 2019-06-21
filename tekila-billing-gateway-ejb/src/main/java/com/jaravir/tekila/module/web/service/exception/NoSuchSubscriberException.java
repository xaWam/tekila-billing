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
public class NoSuchSubscriberException extends Exception {
    public NoSuchSubscriberException () { super(); }
    public NoSuchSubscriberException (String message) { super(message); }
    public NoSuchSubscriberException (String message, Throwable cause) { super(message, cause); }
    public NoSuchSubscriberException (Throwable cause) { super(cause); }    
}
