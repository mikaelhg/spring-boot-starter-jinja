package org.jinja;

import java.io.IOException;
import java.nio.charset.Charset;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;

import org.springframework.core.io.ResourceLoader;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.support.ServletContextResourceLoader;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.loader.ResourceLocator;

/**
 * @author Marco Andreini
 */
public class JinjaTemplateLoader implements ResourceLocator, ServletContextAware {

    private ResourceLoader resourceLoader;

    private String basePath = "";

    private String suffix = ".html";

    private ServletContext servletContext;

    @PostConstruct
    public void init() {
        if (this.resourceLoader == null) {
            this.resourceLoader = new ServletContextResourceLoader(getServletContext());
        }
    }

    @Override
    public String getString(final String fullName, final Charset encoding,
                            final JinjavaInterpreter interpreter) throws IOException {
        Preconditions.checkNotNull(resourceLoader, "post construct not called");
        Preconditions.checkNotNull(fullName);
        Preconditions.checkNotNull(encoding);
        final String name = fullName.contains(".") ? (getBasePath() + fullName) : (getBasePath() + fullName + getSuffix());
        return Files.toString(resourceLoader.getResource(name).getFile(), encoding);
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
}
