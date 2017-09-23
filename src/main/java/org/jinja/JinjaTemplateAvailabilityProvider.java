package org.jinja;

import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

import static org.jinja.JinjaAutoConfiguration.DEFAULT_PREFIX;
import static org.jinja.JinjaAutoConfiguration.DEFAULT_SUFFIX;

/**
 * @author Marco Andreini
 */
public class JinjaTemplateAvailabilityProvider implements TemplateAvailabilityProvider {

    public static final String CLASS_NAME = "org.jinja.JinjaTemplateLoader";

    @Override
	public boolean isTemplateAvailable(String view, Environment environment,
			ClassLoader classLoader, ResourceLoader resourceLoader)
	{
		if (ClassUtils.isPresent(CLASS_NAME, classLoader)) {
			final String prefix = environment.getProperty("spring.jinja.prefix", DEFAULT_PREFIX);
			final String suffix = environment.getProperty("spring.jinja.suffix", DEFAULT_SUFFIX);
			return resourceLoader.getResource(prefix + view + suffix).exists();
		}
		return false;
	}

}
