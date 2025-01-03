package com.skycstech.graphclient.core;

import com.skycstech.graphclient.core.logger.GraphFeignLogger;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "graph-feign.client")
public class GraphFeignClientProperties {

    private boolean disableSslValidation = Boolean.FALSE;
    private GraphFeignLogger.Level loggerLevel = GraphFeignLogger.Level.NONE;
    private List<String> sensitiveHeaders = new ArrayList<>();

}
