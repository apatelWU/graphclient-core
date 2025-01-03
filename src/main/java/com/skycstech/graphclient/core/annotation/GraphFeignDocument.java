package com.skycstech.graphclient.core.annotation;

import java.lang.annotation.*;

/**
 * Binds parameter as document to start defining the GQL request.
 * <p>Ignored if the document name is specified in the {@link GraphFeignRequest}.
 *
 * @author Akash Patel
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GraphFeignDocument {

    /**
     * If the document is a file, set this to true.
     * <p> If set to true, the document will be resolved from configured.
     * <p> Default is false - takes textual representation of an operation (or operations) to execute.
     */
    boolean isDocumentName() default false;

}
