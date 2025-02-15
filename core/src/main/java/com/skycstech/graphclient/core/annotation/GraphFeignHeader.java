package com.skycstech.graphclient.core.annotation;

import java.lang.annotation.*;

/**
 * Binds passed in value as header name within GQL request
 *
 * @author Akash Patel
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GraphFeignHeader {

    /**
     * The name of the variable.
     */
    String value() default "";

    boolean required() default true;

}
