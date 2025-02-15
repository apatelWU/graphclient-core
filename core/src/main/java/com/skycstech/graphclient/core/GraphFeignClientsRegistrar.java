package com.skycstech.graphclient.core;

import com.skycstech.graphclient.core.annotation.GraphFeignClient;
import com.skycstech.graphclient.core.annotation.GraphFeignRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Registrar for GrapheignClients.
 *
 * @author Akash Patel
 */
class GraphFeignClientsRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

    private static Logger log = LoggerFactory.getLogger(GraphFeignClientsRegistrar.class);

    private ResourceLoader resourceLoader;
    private Environment environment;

    GraphFeignClientsRegistrar() {
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        registerGraphFeignClients(metadata, registry);
    }

    private void registerGraphFeignClients(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        LinkedHashSet<BeanDefinition> candidateComponents = new LinkedHashSet<>();

        ClassPathScanningCandidateComponentProvider scanner = getScanner();
        scanner.setResourceLoader(this.resourceLoader);
        scanner.addIncludeFilter(new AnnotationTypeFilter(GraphFeignClient.class));
        Set<String> basePackages = getBasePackages(metadata);
        for (String basePackage : basePackages) {
            candidateComponents.addAll(scanner.findCandidateComponents(basePackage));
        }

        for (BeanDefinition candidateComponent : candidateComponents) {
            if (candidateComponent instanceof AnnotatedBeanDefinition beanDefinition) {
                // verify annotated class is an interface
                AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
                Assert.isTrue(annotationMetadata.isInterface(), "@GrapheignClient can only be specified on an interface");

                Map<String, Object> attributes = annotationMetadata
                        .getAnnotationAttributes(GraphFeignClient.class.getCanonicalName());

                // Register GrapheignClient
                assert attributes != null;
                registerClientConfiguration(registry, attributes.get("name"), attributes.get("configuration"));
                registerGraphFeignClient(registry, annotationMetadata, attributes);
            }
        }
    }

    protected ClassPathScanningCandidateComponentProvider getScanner() {
        return new ClassPathScanningCandidateComponentProvider(false, this.environment) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                boolean isCandidate = false;
                if (beanDefinition.getMetadata().isIndependent()) {
                    if (!beanDefinition.getMetadata().isAnnotation()) {
                        isCandidate = true;
                    }
                }
                return isCandidate;
            }
        };
    }

    protected Set<String> getBasePackages(AnnotationMetadata importingClassMetadata) {
        Map<String, Object> attributes = importingClassMetadata
                .getAnnotationAttributes(EnableGraphFeignClients.class.getCanonicalName());

        Set<String> basePackages = new HashSet<>();
        if (attributes != null) {
            for (String pkg : (String[]) attributes.get("value")) {
                if (StringUtils.hasText(pkg)) {
                    basePackages.add(pkg);
                }
            }

            for (String pkg : (String[]) attributes.get("basePackages")) {
                if (StringUtils.hasText(pkg)) {
                    basePackages.add(pkg);
                }
            }

            for (Class<?> clazz : (Class<?>[]) attributes.get("basePackageClasses")) {
                basePackages.add(ClassUtils.getPackageName(clazz));
            }
        }

        if (basePackages.isEmpty()) {
            basePackages.add(ClassUtils.getPackageName(importingClassMetadata.getClassName()));
        }
        return basePackages;
    }

    private void registerClientConfiguration(BeanDefinitionRegistry registry, Object name, Object configuration) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(GraphFeignClientSpecification.class);
        builder.addConstructorArgValue(name);
        builder.addConstructorArgValue(configuration);
        registry.registerBeanDefinition(name + "." + GraphFeignClientSpecification.class.getSimpleName(),
                builder.getBeanDefinition());
    }

    @SuppressWarnings("unchecked")
    private void registerGraphFeignClient(BeanDefinitionRegistry registry, AnnotationMetadata annotationMetadata,
                                         Map<String, Object> attributes) {
        validateGraphFeignClientAttributes(attributes);

        ConfigurableBeanFactory beanFactory = registry instanceof ConfigurableBeanFactory
                ? (ConfigurableBeanFactory) registry : null;
        assert beanFactory != null;

        String className = annotationMetadata.getClassName();
        Class<?> clazz = ClassUtils.resolveClassName(className, null);
        validateMethods(clazz);

        String name = (String) attributes.get("name");
        String url = getUrl(beanFactory, attributes);
        Class<? extends GraphFeignClientConfiguration> requestInterceptor = attributes.get("configuration") != null
                ? (Class<? extends GraphFeignClientConfiguration>) attributes.get("configuration") : DefaultGraphFeignClientConfiguration.class;

        GraphFeignFactoryBean factoryBean = new GraphFeignFactoryBean();
        factoryBean.setBeanFactory(beanFactory);
        factoryBean.setType(clazz);
        factoryBean.setName(name);

        BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(clazz, () -> {
            factoryBean.setUrl(url);
            factoryBean.setClientConfiguration(requestInterceptor);
            return factoryBean.getTarget();
        });
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        definition.setLazyInit(true);

        AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();
        beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, className);
        beanDefinition.setAttribute("graphFeignClientsRegistrarFactoryBean", factoryBean);
        beanDefinition.setPrimary(true);

        String[] qualifiers = new String[]{name + "GraphFeignClient"};

        BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, className, qualifiers);
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);

        log.info("Registered GrapheignClient [{}]", name);
        log.debug("Registered GrapheignClient [{}] with URL [{}] and RequestInterceptor [{}]", name, url, requestInterceptor.getSimpleName());
    }

    private void validateGraphFeignClientAttributes(Map<String, Object> attributes) {
        if (attributes == null) {
            throw new IllegalStateException("Name and URL must be provided in @" + GraphFeignClient.class.getSimpleName());
        }

        String name = (String) attributes.get("name");
        if (!StringUtils.hasText(name)) {
            throw new IllegalStateException("Name must be provided in @" + GraphFeignClient.class.getSimpleName());
        }

        String url = (String) attributes.get("url");
        if (!StringUtils.hasText(url)) {
            throw new IllegalStateException("URL must be provided in @" + GraphFeignClient.class.getSimpleName());
        }
    }

    private void validateMethods(Class<?> clazz) {
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (!("equals".equals(method.getName()) || "hashCode".equals(method.getName()) || "toString".equals(method.getName()))) {
                if (!method.isAnnotationPresent(GraphFeignRequest.class)) {
                    throw new IllegalArgumentException("Method [" + method.getName() + "] in class [" + clazz.getName() + "] must be annotated with @GrapheignRequest");
                }
            }
        }
    }

    private String getUrl(ConfigurableBeanFactory beanFactory, Map<String, Object> attributes) {
        String url = resolve(beanFactory, (String) attributes.get("url"));
        return getUrl(url);
    }

    private String resolve(ConfigurableBeanFactory beanFactory, String value) {
        if (StringUtils.hasText(value)) {
            if (beanFactory == null || value.startsWith("${")) {
                return this.environment.resolvePlaceholders(value);
            }
            BeanExpressionResolver resolver = beanFactory.getBeanExpressionResolver();
            String resolved = beanFactory.resolveEmbeddedValue(value);
            if (resolver == null) {
                return resolved;
            }
            Object evaluateValue = resolver.evaluate(resolved, new BeanExpressionContext(beanFactory, null));
            if (evaluateValue != null) {
                return String.valueOf(evaluateValue);
            }
            return null;
        }
        return value;
    }

    static String getUrl(String url) {
        if (StringUtils.hasText(url) && !(url.startsWith("#{") && url.contains("}"))) {
            try {
                new URL(url);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(url + " is malformed", e);
            }
        }
        return url;
    }
}
