package com.skycstech.graphclient.core;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.cloud.context.named.NamedContextFactory;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class GraphFeignClientSpecification implements NamedContextFactory.Specification {

    private String name;
    private Class<?>[] configuration;

    GraphFeignClientSpecification() {
    }

    public GraphFeignClientSpecification(String name, Class<?>[] configuration) {
        this.name = name;
        this.configuration = configuration;
    }
}
