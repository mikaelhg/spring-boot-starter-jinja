package org.jinja;

import java.nio.charset.Charset;

import javax.annotation.PostConstruct;
import javax.servlet.Servlet;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.bind.RelaxedPropertyResolver;
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

import com.hubspot.jinjava.Jinjava;

/**
 * @author Marco Andreini
 */
@Configuration
@ConditionalOnClass(JinjaTemplateLoader.class)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class JinjaAutoConfiguration {

	public static final String DEFAULT_PREFIX = "classpath:/templates/";
	public static final String DEFAULT_SUFFIX = ".html";

	@Configuration
	@ConditionalOnMissingBean(name = "defaultSpringTemplateLoader")
	public static class DefaultTemplateResolverConfiguration implements EnvironmentAware {

		private final ResourceLoader resourceLoader;

		private RelaxedPropertyResolver environment;

        public DefaultTemplateResolverConfiguration(final ResourceLoader resourceLoader) {
            if (null == resourceLoader) {
                this.resourceLoader = new DefaultResourceLoader();
            } else {
                this.resourceLoader = resourceLoader;
            }
        }

        @Override
		public void setEnvironment(Environment environment) {
			this.environment = new RelaxedPropertyResolver(environment, "spring.jinja.");
		}

		@PostConstruct
		public void checkTemplateLocationExists() {
			final Boolean checkTemplateLocation = this.environment.getProperty(
					"checkTemplateLocation", Boolean.class, true);
			if (checkTemplateLocation) {
				final Resource resource = this.resourceLoader.getResource(
				        this.environment.getProperty("prefix", DEFAULT_PREFIX));
				Assert.state(resource.exists(), String.format("Cannot find template location: %s (please add some templates or check your jinjava configuration)", resource));
			}
		}

		@Bean
		public JinjaTemplateLoader defaultSpringTemplateLoader() {
			final JinjaTemplateLoader resolver = new JinjaTemplateLoader();
			resolver.setBasePath(this.environment.getProperty("prefix", DEFAULT_PREFIX));
			resolver.setSuffix(this.environment.getProperty("suffix", DEFAULT_SUFFIX));
			return resolver;
		}

		@Bean
		public Jinjava jinja(JinjaTemplateLoader loader) {
			// TODO: Add the jinjavaconfig
			final Jinjava engine = new Jinjava();
			engine.setResourceLocator(loader);
			return engine;
		}

	}


	@Configuration
	@ConditionalOnClass({Servlet.class})
	@ConditionalOnWebApplication
	protected static class JinjavaViewResolverConfiguration implements EnvironmentAware {

		private RelaxedPropertyResolver environment;

		private final Jinjava engine;

		@Autowired
		public JinjavaViewResolverConfiguration(final Jinjava engine) {
			this.engine = engine;
		}

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = new RelaxedPropertyResolver(environment, "spring.jinja.");
		}

		@Bean
		@ConditionalOnMissingBean(name = "jinjaViewResolver")
		public JinjaViewResolver jinjaViewResolver() {
            final Charset encoding = Charset.forName(this.environment.getProperty("encoding", "UTF-8"));
            final JinjaViewResolver resolver = new JinjaViewResolver();
			resolver.setCharset(encoding);
			resolver.setEngine(engine);

			resolver.setContentType(
			        appendCharset(this.environment.getProperty("contentType", "text/html"),
					encoding.name()));

			resolver.setViewNames(this.environment.getProperty("viewNames", String[].class));

			// This resolver acts as a fallback resolver (e.g. like a
			// InternalResourceViewResolver) so it needs to have low precedence
			resolver.setOrder(this.environment.getProperty("resolver.order",
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
					final JinjaHelper annotation = AnnotationUtils.findAnnotation(bean.getClass(), JinjaHelper.class);
					if (annotation != null) {
						engine.getGlobalContext().put(beanName, bean);
					}
					return bean;
				}
			};
		}


		private String appendCharset(String type, String charset) {
			if (type.contains("charset=")) {
				return type;
			}
			return type + ";charset=" + charset;
		}
	}
}