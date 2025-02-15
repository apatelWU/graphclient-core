package com.skycstech.graphclient.core.annotation;

import org.springframework.graphql.client.FieldAccessException;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.support.DocumentSource;

import java.lang.annotation.*;

/**
 * <p> Annotation for a GQL Request methods to used with @GraphFeignClient,
 * allowing to construct request dynamically.
 * <p>Arguments can be passed to the method as a Map<String, Object> or as individual argument(s).
 * Framework will automatically convert the arguments to variables in the GQL request.
 * GraphFeignException is thrown if the request fails.
 *
 *
 * <p>For example:
 *
 * <pre class="code">
 *  &#064;GraphFeignRequest(documentName = "SomeDocumentName", retrievePath = "someResponsePath")
 *  {@code T someMethod(Map<String, Object> variables);}
 * </pre>
 *
 * <p>
 * OR
 *
 * <pre class="code">
 *  &#064;GraphFeignRequest(documentName = "SomeDocumentName", retrievePath = "someResponsePath")
 *  {@code T someMethod(@GraphFeignVariable("variableName") String variableName, Map<String, Object> variables);}
 * </pre>
 *
 * <p>
 * OR
 *
 * <pre class="code">
 *  &#064;GraphFeignRequest(documentName = "SomeDocumentName", retrievePath = "someResponsePath")
 *  {@code T someMethod(@GraphFeignVariable("variableName") String variableName);}
 * </pre>
 *
 * <p>
 * OR
 *
 * <pre class="code">
 *  &#064;GraphFeignRequest(retrievePath = "someResponsePath")
 *  {@code T someMethod(@GraphFeignDocument String document, Map<String, Object> variables);}
 *  </pre>
 *
 * @author Akash Patel
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface GraphFeignRequest {

    /**
     * The name of GraphQL Request document to load from the configured
     * {@link GraphQlClient.Builder#documentSource(DocumentSource) DocumentSource}.
     * <p> Takes priority over {@link GraphFeignDocument}.
     *
     * @throws IllegalArgumentException if the content could not be loaded
     */
    String documentName() default "";

    /**
     * Shortcut for {@link GraphQlClient.RequestSpec#execute()} with a field path to decode from.
     * <p>If you want to decode the full data instead, leave it blank or null and use the response as is.
     * <pre>
     * ((ClientGraphQlResponse) response).map(response -> response.toEntity(..))
     * </pre>
     *
     * @throws FieldAccessException if the target field has any errors,
     *                              including nested errors.
     */
    String retrievePath() default "";


    /**
     * Optional:
     * <p>Select the name of the operation to execute, if the document contains multiple operations.
     */
    String operationName() default "";

    /**
     * Optional:
     * <p>Set the request to be a subscription request.
     */
    boolean isSubscription() default false;

}
