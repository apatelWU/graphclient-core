package com.skycstech.graphclient.sample.graphfeign;

import com.skycstech.graphclient.core.GraphFeignClientConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public class BookGQLClientConfiguration implements GraphFeignClientConfiguration {

    @Override
    public Consumer<HttpHeaders> headersConsumer(Method method) {
        return headers -> headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }
}
