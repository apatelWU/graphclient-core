package com.skycstech.graphclient.core;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.cloud.context.named.NamedContextFactory;
import org.springframework.lang.Nullable;

import java.util.Map;

public class GraphFeignContext extends NamedContextFactory<GraphFeignClientSpecification> {

    public GraphFeignContext() {
        super(GraphFeignClientSpecification.class, "graph-feign", "graph-feign.client.name");
    }

    @Nullable
    public <T> T getInstanceWithoutAncestors(String name, Class<T> type) {
        try {
            return BeanFactoryUtils.beanOfType(getContext(name), type);
        } catch (BeansException ex) {
            return null;
        }
    }

    @Nullable
    public <T> Map<String, T> getInstancesWithoutAncestors(String name, Class<T> type) {
        return getContext(name).getBeansOfType(type);
    }

    public <T> T getInstance(String contextName, String beanName, Class<T> type) {
        return getContext(contextName).getBean(beanName, type);
    }
}
