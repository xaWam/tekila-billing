package com.jaravir.tekila.base.interceptor.annot;

import com.jaravir.tekila.base.auth.Privilege;

import javax.interceptor.InterceptorBinding;
import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/**
 * Created by sajabrayilov on 12/15/2014.
 */
@Inherited
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({TYPE, METHOD})

public @interface PermissionRequired {
    String module() default "";
    String subModule() default  "";
    Privilege privilege();
}