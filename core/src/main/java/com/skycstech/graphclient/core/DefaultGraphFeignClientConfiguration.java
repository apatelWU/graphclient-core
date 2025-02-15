package com.skycstech.graphclient.core;

import org.springframework.http.HttpHeaders;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public class DefaultGraphFeignClientConfiguration implements GraphFeignClientConfiguration {

    @Override
    public Consumer<HttpHeaders> headersConsumer(Method method) {
        return httpHeaders -> {
        };
    }

}
