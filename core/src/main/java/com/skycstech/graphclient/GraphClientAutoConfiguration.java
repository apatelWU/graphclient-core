package com.skycstech.graphclient;

import com.skycstech.graphclient.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Akash Patel
 */
@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackages = {"com.skycstech.graphclient"})
@EnableConfigurationProperties({GraphFeignClientProperties.class})
public class GraphClientAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(GraphClientAutoConfiguration.class);

    @Autowired(required = false)
    private List<GraphFeignClientSpecification> configurations = new ArrayList<>();

    @Bean
    public HasFeatures graphFeignFeature() {
        return HasFeatures.namedFeature("GraphFeign", GraphFeign.class);
    }

    @Bean
    public GraphFeignContext graphFeignContext() {
        GraphFeignContext context = new GraphFeignContext();
        context.setConfigurations(this.configurations);
        return context;
    }

    @Bean
    @ConditionalOnMissingBean(GraphFeignCustomizer.class)
    public GraphFeignCustomizer graphFeignCustomizer() {
        log.debug("No Bean found GraphFeignCustomizer, Creating default bean");
        return new GraphFeignCustomizer();
    }

    @Bean
    @ConditionalOnMissingBean
    @Scope("prototype")
    GraphFeign.Builder graphFeignBuilder(BeanFactory beanFactory) {
        return GraphFeign.builder();
    }

}
