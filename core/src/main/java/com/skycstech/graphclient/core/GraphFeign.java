package com.skycstech.graphclient.core;

import com.skycstech.graphclient.core.annotation.GraphFeignDocument;
import com.skycstech.graphclient.core.annotation.GraphFeignHeader;
import com.skycstech.graphclient.core.annotation.GraphFeignRequest;
import com.skycstech.graphclient.core.annotation.GraphFeignVariable;
import com.skycstech.graphclient.core.exception.GraphFeignException;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"unused"})
public class GraphFeign {

    private final String name;
    private final Class<?> type;
    private final HttpGraphQlClient.Builder<?> gqlClientBuilder;
    private final GraphFeignClientConfiguration gqlClientConfiguration;

    GraphFeign(String name, Class<?> type,
               HttpGraphQlClient.Builder<?> gqlClientBuilder,
               GraphFeignClientConfiguration gqlClientConfiguration) {
        this.name = name;
        this.type = type;
        this.gqlClientBuilder = gqlClientBuilder;
        this.gqlClientConfiguration = gqlClientConfiguration;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        // From annotation
        private String name;
        private Class<?> type;
        private String url;
        private GraphFeignClientConfiguration graphFeignClientConfiguration;

        // From context
        private GraphFeignCustomizer customizer;

        // From properties
        private boolean disableSslValidation = Boolean.FALSE;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(Class<?> type) {
            this.type = type;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder graphFeignClientConfiguration(GraphFeignClientConfiguration graphFeignClientConfiguration) {
            this.graphFeignClientConfiguration = graphFeignClientConfiguration;
            return this;
        }

        public Builder customizer(GraphFeignCustomizer customizer) {
            this.customizer = customizer;
            return this;
        }

        public Builder disableSslValidation(Boolean disableSslValidation) {
            this.disableSslValidation = disableSslValidation;
            return this;
        }

        public <T> T target() {
            return build().newInstance();
        }

        public GraphFeign build() {
            WebClient webClient = WebClient.builder()
                    .baseUrl(this.url)
                    .filter(this.customizer.getGraphFeignLogger().logRequest())
                    .filter(this.customizer.getGraphFeignLogger().logResponse())
                    .build();

            if (this.customizer.getWebClient() != null) {
                webClient = this.customizer.getWebClient().mutate()
                        .baseUrl(this.url)
                        .filter(this.customizer.getGraphFeignLogger().logRequest())
                        .filter(this.customizer.getGraphFeignLogger().logResponse())
                        .build();
            } else if (this.disableSslValidation) {
                try {
                    // Creates a HttpClient to allow self-signed certificates
                    SslContext sslContext = SslContextBuilder
                            .forClient()
                            .trustManager(InsecureTrustManagerFactory.INSTANCE)
                            .build();

                    HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));
                    webClient = WebClient.builder()
                            .clientConnector(new ReactorClientHttpConnector(httpClient))
                            .baseUrl(this.url)
                            .filter(this.customizer.getGraphFeignLogger().logRequest())
                            .filter(this.customizer.getGraphFeignLogger().logResponse())
                            .build();
                } catch (Exception e) {
                    throw new GraphFeignException("Unable to construct web client with disable ssl validation.", e);
                }
            }

            HttpGraphQlClient.Builder<?> gqlClientBuilder = HttpGraphQlClient.builder(webClient);
            if (this.customizer.getCodecConfigurer() != null) {
                gqlClientBuilder.codecConfigurer(this.customizer.getCodecConfigurer());
            }

            if (this.customizer.getContentLoader() != null) {
                gqlClientBuilder.documentSource(this.customizer.getContentLoader());
            }

            return new GraphFeign(this.name, this.type, gqlClientBuilder, this.graphFeignClientConfiguration);
        }

    }

    @SuppressWarnings("unchecked")
    public <T> T newInstance() {
        return (T) Proxy.newProxyInstance(this.type.getClassLoader(), new Class[]{this.type},
                new GraphFeignInvocationHandler(this.gqlClientBuilder, this.gqlClientConfiguration));
    }


    static class GraphFeignInvocationHandler implements InvocationHandler {

        private static final Logger log = LoggerFactory.getLogger(GraphFeignInvocationHandler.class);

        private final HttpGraphQlClient.Builder<?> builder;
        private final GraphFeignClientConfiguration configuration;

        GraphFeignInvocationHandler(HttpGraphQlClient.Builder<?> builder, GraphFeignClientConfiguration configuration) {
            this.builder = builder;
            this.configuration = configuration;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return switch (method.getName()) {
                case "equals" -> {
                    try {
                        Object otherHandler =
                                args.length > 0 && args[0] != null ? Proxy.getInvocationHandler(args[0]) : null;
                        yield equals(otherHandler);
                    } catch (IllegalArgumentException e) {
                        yield false;
                    }
                }
                case "hashCode" -> hashCode();
                case "toString" -> toString();
                default -> buildAndExecuteRequest(method, args);
            };
        }

        enum ParamType {
            VARIABLE,
            DOCUMENT,
            HEADER
        }

        record ParamInfo(String name, Object value, Class<?> type, ParamType paramType) {
        }

        record ResponseBinder(Class<?> type, boolean isList, boolean isMono, boolean isFlux, String methodKey) {
            public Object response(GraphQlClient.RetrieveSubscriptionSpec spec) {
                Flux<?> response;
                if (this.isList) {
                    response = spec.toEntityList(this.type);
                } else {
                    response = spec.toEntity(this.type);
                }

                if (isFlux) {
                    return response
                            .onErrorMap(e -> new GraphFeignException("Error while calling Graph API [method: {" + this.methodKey + "}]: " + e.getMessage()));
                } else {
                    return response
                            .onErrorMap(e -> new GraphFeignException("Error while calling Graph API [method: {" + this.methodKey + "}]: " + e.getMessage()))
                            .collectList().block();
                }
            }

            public Object response(GraphQlClient.RetrieveSpec spec) {
                Mono<?> response;
                if (this.isList) {
                    response = spec.toEntityList(this.type);
                } else {
                    response = spec.toEntity(this.type);
                }

                if (isMono) {
                    return response
                            .onErrorMap(e -> new GraphFeignException("Error while calling Graph API [method: {" + this.methodKey + "}]: " + e.getMessage()));
                } else {
                    return response
                            .onErrorMap(e -> new GraphFeignException("Error while calling Graph API [method: {" + this.methodKey + "}]: " + e.getMessage()))
                            .block();
                }
            }

            public Object response(Flux<ClientGraphQlResponse> responseFlux) {
                Flux<?> finalResponse = responseFlux.map(response -> {
                    if (!response.getErrors().isEmpty()) {
                        response.getErrors().forEach(error -> log.error("Error while calling Graph API [method: {}]: Errors [path: {}, message: {}]", this.methodKey, error.getPath(), error.getMessage()));
                        return Flux.error(new GraphFeignException("Error while calling Graph API [method: {" + this.methodKey + "}]: " + response.getErrors().get(0).getMessage()));
                    } else {
                        return Flux.just(response);
                    }
                });
                if (isFlux) {
                    return finalResponse;
                } else {
                    return finalResponse.collectList().block();
                }
            }

            public Object response(Mono<ClientGraphQlResponse> responseMono) {
                Mono<?> finalResponse = responseMono.map(response -> {
                    if (!response.getErrors().isEmpty()) {
                        response.getErrors().forEach(error -> log.error("Error while calling Graph API [method: {}]: Errors [path: {}, message: {}]", this.methodKey, error.getPath(), error.getMessage()));
                        return Mono.error(new GraphFeignException("Error while calling Graph API [method: {" + this.methodKey + "}]: " + response.getErrors().get(0).getMessage()));
                    } else {
                        return Mono.just(response);
                    }
                });

                if (isMono) {
                    return finalResponse;
                } else {
                    return finalResponse.block();
                }
            }
        }

        private Object buildAndExecuteRequest(Method method, Object[] args) {
            Map<ParamType, List<ParamInfo>> params = getParams(method, args);
            HttpGraphQlClient httpGraphQlClient = getHttpGraphQlClient(method, params);
            GraphQlClient.RequestSpec requestSpec = buildRequestSpec(httpGraphQlClient, method, params);
            return executeRequest(requestSpec, method);
        }

        private synchronized HttpGraphQlClient getHttpGraphQlClient(Method method, Map<ParamType, List<ParamInfo>> params) {
            applyConfiguration(method, params);
            return this.builder.build();
        }

        private GraphQlClient.RequestSpec buildRequestSpec(HttpGraphQlClient httpGraphQlClient, Method method, Map<ParamType, List<ParamInfo>> params) {
            GraphQlClient.RequestSpec requestSpec = applyDocument(httpGraphQlClient, method, params);
            applyOperationName(requestSpec, method);
            applyVariables(requestSpec, params);
            return requestSpec;
        }

        private Object executeRequest(GraphQlClient.RequestSpec requestSpec, Method method) {
            ResponseBinder responseBinder = getResponseBinder(method);
            GraphFeignRequest annotation = method.getAnnotation(GraphFeignRequest.class);
            boolean isSubscription = annotation.isSubscription();
            String retrievePath = annotation.retrievePath();

            if (StringUtils.hasText(retrievePath)) {
                log.debug("Using retrievePath as specified in GraphFeignRequest annotation: [{}]", retrievePath);
                if (isSubscription) {
                    log.debug("Using subscription retrievePath as specified in GraphFeignRequest annotation.");
                    GraphQlClient.RetrieveSubscriptionSpec retrieveSpec = requestSpec.retrieveSubscription(retrievePath);
                    return responseBinder.response(retrieveSpec);
                } else {
                    GraphQlClient.RetrieveSpec retrieveSpec = requestSpec.retrieve(retrievePath);
                    return responseBinder.response(retrieveSpec);
                }
            } else {
                if (isSubscription) {
                    log.debug("No retrievePath specified in GraphFeignRequest annotation. Using subscription execute");
                    return responseBinder.response(requestSpec.executeSubscription());
                } else {
                    log.debug("No retrievePath specified in GraphFeignRequest annotation. Using execute");
                    return responseBinder.response(requestSpec.execute());
                }
            }
        }

        private Map<ParamType, List<ParamInfo>> getParams(Method method, Object[] args) {
            List<ParamInfo> params = new ArrayList<>();
            // Iterate over the method parameters and arguments
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].isAnnotationPresent(GraphFeignDocument.class)) {
                    String paramName = parameters[i].getName();
                    GraphFeignDocument annotation = parameters[i].getAnnotation(GraphFeignDocument.class);
                    if (annotation.isDocumentName()) {
                        paramName = "documentName";
                    }
                    Object paramValue = args[i];
                    Class<?> paramClassType = parameters[i].getType();
                    params.add(new ParamInfo(paramName, paramValue, paramClassType, ParamType.DOCUMENT));
                } else if (parameters[i].isAnnotationPresent(GraphFeignHeader.class)) {
                    String paramName = parameters[i].getName();
                    GraphFeignHeader annotation = parameters[i].getAnnotation(GraphFeignHeader.class);
                    if (StringUtils.hasText(annotation.value())) {
                        paramName = annotation.value();
                    }
                    Object paramValue = args[i];
                    Class<?> paramClassType = parameters[i].getType();
                    params.add(new ParamInfo(paramName, paramValue, paramClassType, ParamType.HEADER));
                } else {
                    String paramName = parameters[i].getName();
                    GraphFeignVariable annotation = parameters[i].getAnnotation(GraphFeignVariable.class);
                    if (StringUtils.hasText(annotation.value())) {
                        paramName = annotation.value();
                    }
                    Object paramValue = args[i];
                    Class<?> paramClassType = parameters[i].getType();
                    params.add(new ParamInfo(paramName, paramValue, paramClassType, ParamType.VARIABLE));
                }
            }

            return params.stream().collect(Collectors.groupingBy(ParamInfo::paramType));
        }

        private void applyConfiguration(Method method, Map<ParamType, List<ParamInfo>> params) {
            log.debug("Applying configuration for method: [{}]", method.getName());

            // applying headers
            log.debug("Applying headers configuration for method: [{}]", method.getName());
            Optional.ofNullable(configuration)
                    .map(config -> config.headersConsumer(method))
                    .ifPresent(this.builder::headers);

            params.getOrDefault(ParamType.HEADER, Collections.emptyList())
                    .forEach(paramInfo -> {
                        if (paramInfo.type().isAssignableFrom(String.class)) {
                            this.builder.headers(headers -> headers.set(paramInfo.name(), (String) paramInfo.value()));
                        }
                    });

            // applying interceptors
            log.debug("Applying interceptors configuration for method: [{}]", method.getName());
            Optional.ofNullable(configuration)
                    .map(config -> config.interceptorsConsumer(method))
                    .ifPresent(this.builder::interceptors);

            // applying document source
            log.debug("Applying document source configuration for method: [{}]", method.getName());
            Optional.ofNullable(configuration)
                    .map(config -> config.documentSource(method))
                    .ifPresent(this.builder::documentSource);
        }

        private GraphQlClient.RequestSpec applyDocument(HttpGraphQlClient httpGraphQlClient, Method method, Map<ParamType, List<ParamInfo>> params) {
            return Optional.ofNullable(method.getAnnotation(GraphFeignRequest.class))
                    .map(GraphFeignRequest::documentName)
                    .filter(StringUtils::hasText)
                    .map(documentName -> {
                        log.debug("Using documentName specified in GraphFeignRequest annotation: [{}]", documentName);
                        return httpGraphQlClient.documentName(documentName);
                    })
                    .orElseGet(() -> {
                        log.debug("No documentName specified in GraphFeignRequest annotation. Searching GraphFeignDocument parameter");
                        var documentParams = params.getOrDefault(ParamType.DOCUMENT, Collections.emptyList());
                        if (documentParams.size() != 1) {
                            throw new IllegalArgumentException("Either documentName in GraphFeignRequest or GraphFeignDocument parameter is required");
                        }

                        ParamInfo paramInfo = documentParams.get(0);
                        if (paramInfo.type().isAssignableFrom(String.class)) {
                            if (paramInfo.name().equalsIgnoreCase("documentName")) {
                                log.debug("Using documentName specified in GraphFeignDocument parameter");
                                return httpGraphQlClient.documentName((String) paramInfo.value());
                            } else {
                                log.debug("Using document specified in GraphFeignDocument parameter");
                                return httpGraphQlClient.document((String) paramInfo.value());
                            }
                        } else {
                            throw new IllegalArgumentException("GraphFeignDocument parameter must be a String");
                        }
                    });
        }

        private void applyOperationName(GraphQlClient.RequestSpec requestSpec, Method method) {
            Optional.ofNullable(method.getAnnotation(GraphFeignRequest.class))
                    .map(GraphFeignRequest::operationName)
                    .filter(StringUtils::hasText)
                    .ifPresent(operationName -> {
                        log.debug("Using operationName specified in GraphFeignRequest annotation: [{}]", operationName);
                        requestSpec.operationName(operationName);
                    });
        }

        private void applyVariables(GraphQlClient.RequestSpec requestSpec, Map<ParamType, List<ParamInfo>> params) {
            var variablesParams = params.getOrDefault(ParamType.VARIABLE, Collections.emptyList());
            if (variablesParams.isEmpty()) {
                log.debug("Not variables found in method parameters");
                return;
            }

            Map<String, Object> variables = new HashMap<>();
            for (ParamInfo param : variablesParams) {
                if (param.type().isAssignableFrom(Map.class) && param.value() instanceof Map<?, ?> paramMap) {
                    paramMap.forEach((key, value) -> {
                        if (key instanceof String keyStr) {
                            variables.put(keyStr, value);
                        } else {
                            throw new IllegalArgumentException("Map key must be a String");
                        }
                    });
                } else {
                    variables.put(param.name(), param.value());
                }
            }

            log.debug("applying variables: [{}]", variables);
            requestSpec.variables(variables);
        }

        private ResponseBinder getResponseBinder(Method method) {
            Class<?> returnType = method.getReturnType();
            String methodKey = method.getDeclaringClass().getName() + "#" + method.getName();
            boolean isList = false;
            boolean isMono = false;
            boolean isFlux = false;

            if (Mono.class.isAssignableFrom(returnType) || Flux.class.isAssignableFrom(returnType)) {
                if (Flux.class.isAssignableFrom(returnType)) {
                    isFlux = true;
                } else {
                    isMono = true;
                }

                ParameterizedType parameterizedType = (ParameterizedType) method.getGenericReturnType();
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

                if (actualTypeArguments[0] instanceof ParameterizedType nestedParameterizedType) {
                    if (nestedParameterizedType.getRawType().getTypeName().equalsIgnoreCase("java.util.List")) {
                        isList = true;
                        Type[] nestedActualTypeArguments = nestedParameterizedType.getActualTypeArguments();
                        returnType = (Class<?>) nestedActualTypeArguments[0];
                    } else {
                        throw new IllegalArgumentException("Unsupported nested return type");
                    }
                } else {
                    returnType = (Class<?>) actualTypeArguments[0];
                }
            }

            if (List.class.isAssignableFrom(returnType)) {
                isList = true;
                ParameterizedType parameterizedType = (ParameterizedType) method.getGenericReturnType();
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                returnType = (Class<?>) actualTypeArguments[0];
            }

            return new ResponseBinder(returnType, isList, isMono, isFlux, methodKey);
        }
    }
}
