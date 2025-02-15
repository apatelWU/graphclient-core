package com.skycstech.graphclient.core.logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class GraphFeignLogger {

    private static final Logger log = LoggerFactory.getLogger(GraphFeignLogger.class);

    private Level level = Level.NONE;
    private List<String> sensitiveHeaders = new ArrayList<>();
    private ObjectMapper objectMapper = new ObjectMapper();

    public ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (level != Level.NONE) {
                logRequest(clientRequest);
            }
            return Mono.just(clientRequest);
        });
    }

    public ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (level != Level.NONE) {
                logResponse(clientResponse);
            }
            return Mono.just(clientResponse);
        });
    }


    public enum Level {
        /**
         * No logging.
         */
        NONE,

        /**
         * Log only the request method and URL and the response status code and execution time.
         */
        BASIC,

        /**
         * Log the basic information along with request and response headers.
         */
        HEADERS,

        /**
         * Log the headers, body, and metadata for both requests and responses.
         */
        FULL
    }


    private void logRequest(ClientRequest request) {
        switch (level) {
            case BASIC -> log.info("Request: {} {}", request.method(), request.url());
            case HEADERS ->
                    log.info("Request: {} {}\nHeaders: {}", request.method(), request.url(), maskSensitiveHeaders(request.headers()));
            case FULL ->
                    log.info("Request: {} {}\nHeaders: {}\nBody: {}", request.method(), request.url(), maskSensitiveHeaders(request.headers()), extractRequestBody(request));
            default -> log.debug("Unexpected value: {}", level);
        }
    }

    private void logResponse(ClientResponse response) {
        switch (level) {
            case BASIC -> log.info("Response: {}", response.statusCode());
            case HEADERS ->
                    log.info("Response: {}\nHeaders: {}", response.statusCode(), response.headers().asHttpHeaders());
            case FULL ->
                // TODO: need to resolve the issue with bodyToMono
                    log.info("Response: {}\nHeaders: {}\nBody: {}", response.statusCode(), response.headers().asHttpHeaders(), response.bodyToMono(String.class));
            default -> log.debug("Unexpected value: {}", level);
        }
    }

    private Map<String, List<String>> maskSensitiveHeaders(HttpHeaders headers) {
        Map<String, List<String>> headersMap = new HashMap<>();
        headers.forEach((key, value) -> {
            if (sensitiveHeaders.contains(key)) {
                var maskedValue = value.stream()
                        .filter(StringUtils::hasText)
                        .map(v -> {
                            if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(key)) {
                                if (v.contains("Bearer") && v.contains(".")) {
                                    String[] headerValueSplit = v.split("\\.");
                                    v = headerValueSplit[0] + headerValueSplit[1] + ".TRUNCATED";
                                } else if (v.contains("Basic")) {
                                    v = "[Basic xxxxxxxxxxxxxxxxxxxxxxxxx]";
                                } else {
                                    v = maskChars(v.toCharArray(), 1.0);
                                }
                            } else {
                                v = maskChars(v.toCharArray(), 0.7);
                            }
                            return v;
                        })
                        .toList();
                headersMap.put(key, maskedValue);
            } else {
                headersMap.put(key, value);
            }
        });
        return headersMap;
    }

    private String maskChars(char[] chars, double maskPercent) {
        for (int i = 0; i < (chars.length * maskPercent); i++) {
            chars[i] = 'x';
        }
        return String.copyValueOf(chars);
    }

    private String extractRequestBody(ClientRequest request) {
        Field inserterField = null;
        String bodyStr = null;
        try {
            BodyInserter<?, ?> inserter = request.body();
            inserterField = inserter.getClass().getDeclaredField("arg$1");
            inserterField.setAccessible(true);
            Object body = inserterField.get(inserter);
            inserterField.setAccessible(false);
            bodyStr = toJSONString(body);
        } catch (Exception e) {
            log.warn("Failed to extract request body", e);
        } finally {
            if (inserterField != null) {
                inserterField.setAccessible(false);
            }
        }
        return bodyStr;
    }

    private String toJSONString(Object object) {
        try {
            return objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(object);
        } catch (Exception e) {
            log.warn("Error while marshalling {}", object.getClass().getSimpleName(), e);
        }
        return null;
    }

}
