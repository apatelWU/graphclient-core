package com.skycstech.graphclient.core;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

/**
 * Factory bean for creating Grapheign clients.
 *
 * @author Akash Patel
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class GraphFeignFactoryBean
        implements FactoryBean<Object>, InitializingBean, ApplicationContextAware, BeanFactoryAware {

    private Class<?> type;
    private String name;
    private String url;
    Class<? extends GraphFeignClientConfiguration> clientConfiguration;

    private ApplicationContext applicationContext;
    private BeanFactory beanFactory;

    @Override
    public void afterPropertiesSet() {
        Assert.hasText(name, "Name must be set");
        Assert.hasText(url, "URL must be set");
    }

    protected GraphFeign.Builder graphFeign(GraphFeignContext context) {
        GraphFeign.Builder builder = get(context, GraphFeign.Builder.class)
                .name(name)
                .type(type)
                .url(url)
                .graphFeignClientConfiguration(getOrInstantiate(clientConfiguration));

        configureGraphFeign(context, builder);
        return builder;
    }

    protected void configureGraphFeign(GraphFeignContext context, GraphFeign.Builder builder) {
        GraphFeignClientProperties properties = beanFactory != null ? beanFactory.getBean(GraphFeignClientProperties.class) : applicationContext.getBean(GraphFeignClientProperties.class);

        builder.disableSslValidation(properties.isDisableSslValidation());

        GraphFeignCustomizer customizer = getOrInstantiate(GraphFeignCustomizer.class);
        customizer.getGraphFeignLogger().setObjectMapper(customizer.getObjectMapper());
        customizer.getGraphFeignLogger().setLevel(properties.getLoggerLevel());
        customizer.getGraphFeignLogger().setSensitiveHeaders(properties.getSensitiveHeaders());
        builder.customizer(customizer);
    }

    <T> T getTarget() {
        GraphFeignContext context = beanFactory != null ? beanFactory.getBean(GraphFeignContext.class) : applicationContext.getBean(GraphFeignContext.class);
        GraphFeign.Builder builder = graphFeign(context);
        return builder.target();
    }

    protected <T> T get(GraphFeignContext context, Class<T> type) {
        T instance = context.getInstance(name, type);
        if (instance == null) {
            throw new IllegalStateException("No bean found of type " + type + " for " + name);
        }
        return instance;
    }

    private <T> T getOrInstantiate(Class<T> tClass) {
        if (tClass == null) {
            return null;
        }

        try {
            return beanFactory != null ? beanFactory.getBean(tClass) : applicationContext.getBean(tClass);
        } catch (NoSuchBeanDefinitionException e) {
            return BeanUtils.instantiateClass(tClass);
        }
    }


    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public Object getObject() {
        return getTarget();
    }

    @Override
    public Class<?> getObjectType() {
        return type;
    }


    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
        beanFactory = context;
    }
}
