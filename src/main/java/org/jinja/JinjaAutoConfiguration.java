package org.jinja;

import com.hubspot.jinjava.Jinjava;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.nio.charset.Charset;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET;
import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE;

/**
 * @author Marco Andreini
 */
@Configuration
@ConditionalOnClass(JinjaTemplateLoader.class)
@AutoConfigureAfter({WebMvcAutoConfiguration.class, WebFluxAutoConfiguration.class})
public class JinjaAutoConfiguration {

    public static final String DEFAULT_PREFIX = "classpath:/templates/";

    public static final String DEFAULT_SUFFIX = ".html";

    @Configuration
    @ConditionalOnMissingBean(name = "defaultSpringTemplateLoader")
    public static class DefaultTemplateResolverConfiguration implements EnvironmentAware {

        private final ResourceLoader resourceLoader;

        private Environment environment;

        public DefaultTemplateResolverConfiguration(final ResourceLoader resourceLoader) {
            if (null == resourceLoader) {
                this.resourceLoader = new DefaultResourceLoader();
            } else {
                this.resourceLoader = resourceLoader;
            }
        }

        @Override
        public void setEnvironment(final Environment environment) {
            this.environment = environment;
        }

        @PostConstruct
        public void checkTemplateLocationExists() {
            final var checkTemplateLocation = this.environment.getProperty(
                    "checkTemplateLocation", Boolean.class, true);
            if (checkTemplateLocation) {
                final var resource = this.resourceLoader.getResource(
                        this.environment.getProperty("spring.jinja.prefix", DEFAULT_PREFIX));
                Assert.state(resource.exists(), String.format("Cannot find template location: %s (please add some templates or check your jinjava configuration)", resource));
            }
        }

        @Bean
        public JinjaTemplateLoader defaultSpringTemplateLoader() {
            var resolver = new JinjaTemplateLoader();
            resolver.setBasePath(this.environment.getProperty("spring.jinja.prefix", DEFAULT_PREFIX));
            resolver.setSuffix(this.environment.getProperty("spring.jinja.suffix", DEFAULT_SUFFIX));
            return resolver;
        }

        @Bean
        public Jinjava jinja(final JinjaTemplateLoader loader) {
            final var engine = new Jinjava();
            engine.setResourceLocator(loader);
            return engine;
        }

    }

    @Configuration
    @ConditionalOnWebApplication(type = SERVLET)
    protected static class JinjavaViewResolverConfiguration implements EnvironmentAware {

        private Environment environment;

        private final Jinjava engine;

        @Autowired
        public JinjavaViewResolverConfiguration(final Jinjava engine) {
            this.engine = engine;
        }

        @Override
        public void setEnvironment(final Environment environment) {
            this.environment = environment;
        }

        @Bean
        @ConditionalOnMissingBean(name = "jinjaViewResolver")
        public JinjaViewResolver jinjaViewResolver() {
            final var encoding = Charset.forName(
                    this.environment.getProperty("spring.jinja.encoding", "UTF-8"));
            final var resolver = new JinjaViewResolver();
            resolver.setCharset(encoding);
            resolver.setEngine(engine);

            resolver.setContentType(
                    appendCharset(this.environment.getProperty("spring.jinja.contentType", "text/html"),
                            encoding.name()));

            resolver.setViewNames(this.environment.getProperty("spring.jinja.viewNames", String[].class));

            // This resolver acts as a fallback resolver (e.g. like a
            // InternalResourceViewResolver) so it needs to have low precedence
            resolver.setOrder(this.environment.getProperty("spring.jinja.resolver.order",
                    Integer.class, Ordered.LOWEST_PRECEDENCE - 50));

            return resolver;
        }

        @Bean
        public BeanPostProcessor jinjaBeanPostProcessor() {
            return new BeanPostProcessor() {

                @Override
                public Object postProcessBeforeInitialization(final Object bean, final String beanName) {
                    return bean;
                }

                @Override
                public Object postProcessAfterInitialization(final Object bean, final String beanName)
                        throws BeansException
                {
                    final var annotation = AnnotationUtils.findAnnotation(bean.getClass(), JinjaHelper.class);
                    if (annotation != null) {
                        engine.getGlobalContext().put(beanName, bean);
                    }
                    return bean;
                }
            };
        }


        private String appendCharset(final String type, final String charset) {
            if (type.contains("charset=")) {
                return type;
            }
            return type + ";charset=" + charset;
        }
    }
}