package org.jinja;

import com.google.common.base.Charsets;
import com.hubspot.jinjava.Jinjava;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.view.AbstractTemplateViewResolver;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

import java.nio.charset.Charset;

/**
 * @author Marco Andreini
 */
public class JinjaViewResolver extends AbstractTemplateViewResolver {

    private Jinjava engine;

    private Charset charset = Charsets.UTF_8;

    private boolean renderExceptions = false;

    private String contentType = "text/html;charset=UTF-8";

    public JinjaViewResolver() {
        setViewClass(requiredViewClass());
    }

    @Override
    protected Class<?> requiredViewClass() {
        return JinjaView.class;
    }

    @Override
    protected AbstractUrlBasedView buildView(String viewName) throws Exception {
        final var view = (JinjaView) super.buildView(viewName);
        view.setEngine(this.engine);
        view.setContentType(contentType);
        view.setRenderExceptions(renderExceptions);
        view.setEncoding(charset);
        return view;
    }

    public Jinjava getEngine() {
        return engine;
    }

    public void setEngine(Jinjava engine) {
        this.engine = engine;
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public boolean isRenderExceptions() {
        return renderExceptions;
    }

    public void setRenderExceptions(boolean renderExceptions) {
        this.renderExceptions = renderExceptions;
    }

    @Nullable
    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

}
