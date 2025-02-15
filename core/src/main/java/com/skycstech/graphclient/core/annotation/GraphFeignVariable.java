package com.skycstech.graphclient.core.annotation;

import java.lang.annotation.*;

/**
 * Binds passed in value as variable name within GQL request
 *
 * @author Akash Patel
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GraphFeignVariable {

    /**
     * The name of the variable.
     */
    String value() default "";

}
