package com.skycstech.graphclient.core.annotation;

import com.skycstech.graphclient.core.DefaultGraphFeignClientConfiguration;
import com.skycstech.graphclient.core.GraphFeignClientConfiguration;

import java.lang.annotation.*;

/**
 * Annotation to specify a client for Grapheign.
 * All methods in the interface must be annotated with {@link GraphFeignRequest}.
 *
 * <p>For example:
 *
 * <pre class="code">
 *  &#064;GraphFeignClient(name = "someGQLClient", url = "${some.router.url}")
 *  public interface SomeGQLClient {
 *  }
 *  </pre>
 * <p>
 * OR
 *
 * <pre class="code">
 *  &#064;GraphFeignClient(name = "someGQLClient", url = "${some.router.url}", configuration = SomeConfiguration.class)
 *  public interface SomeGQLClient {
 *  }
 *  </pre>
 *
 * @author Akash Patel
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Inherited
public @interface GraphFeignClient {

    /**
     * Required. The name of the client.
     */
    String name() default "";

    /**
     * Required. The absolute URL of the GraphQL server.
     */
    String url() default "";


    Class<? extends GraphFeignClientConfiguration> configuration() default DefaultGraphFeignClientConfiguration.class;

}
