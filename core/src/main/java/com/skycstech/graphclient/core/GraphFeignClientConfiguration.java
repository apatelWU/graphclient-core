package com.skycstech.graphclient.core;

import org.springframework.graphql.client.GraphQlClientInterceptor;
import org.springframework.graphql.support.DocumentSource;
import org.springframework.http.HttpHeaders;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;

public interface GraphFeignClientConfiguration {

    /**
     * Headers consumer for the given GraphFeignClient
     */
    Consumer<HttpHeaders> headersConsumer(Method method);

    /**
     * Interceptors consumer for the given GraphFeignClient
     */
    default Consumer<List<GraphQlClientInterceptor>> interceptorsConsumer(Method method) {
        return interceptors -> {
        };
    }

    /**
     * DocumentSource for the given GraphFeignClient
     * <p> Overrides default DocumentSource
     */
    default DocumentSource documentSource(Method method) {
        return null;
    }

}
