package com.skycstech.graphclient.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skycstech.graphclient.core.logger.GraphFeignLogger;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.graphql.support.DocumentSource;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.function.Consumer;

@Getter
@Setter
@Accessors(chain = true)
public class GraphFeignCustomizer {

    private WebClient webClient;
    private ObjectMapper objectMapper = new ObjectMapper();
    private Consumer<CodecConfigurer> codecConfigurer = configurer -> {
        configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
        configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
    };
    private DocumentSource contentLoader;
    private GraphFeignLogger graphFeignLogger = new GraphFeignLogger();

}
