package org.jinja;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.view.AbstractTemplateView;

import com.google.common.base.Charsets;
import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.interpret.FatalTemplateErrorsException;

/**
 * @author Marco Andreini
 */
public class JinjaView extends AbstractTemplateView {

    private Charset encoding = Charsets.UTF_8;

    private Jinjava engine;

    private boolean renderExceptions = false;

    private String contentType;

    @Override
    protected void renderMergedTemplateModel(
            final Map<String, Object> model,
            final HttpServletRequest request,
            final HttpServletResponse response) throws Exception
    {
        doRender(model, response);
    }

    private void doRender(Map<String, Object> model,
                          HttpServletResponse response) throws IOException {

        logger.trace(String.format("Rendering Jinja template [%s] in JinjaView '%s'", getUrl(), getBeanName()));

        if (contentType != null) {
            response.setContentType(contentType);
        }

        PrintWriter responseWriter = response.getWriter();

        if (renderExceptions) {
            try {
                responseWriter.write(engine.render(getTemplate(), model));
            } catch (FatalTemplateErrorsException e) {
                // TODO: render exception
                responseWriter.write(e.getLocalizedMessage());
                logger.error(String.format("failed to render template [%s]", getUrl()), e);
            } catch (IOException e) {
                responseWriter.write(String.format("<pre>could not find template: %s\n", getUrl()));
                e.printStackTrace(responseWriter);
                responseWriter.write("</pre>");
                logger.error("could not find template", e);
            }
        } else {
            try {
                responseWriter.write(engine.render(getTemplate(), model));
            } catch (Throwable e) {
                logger.error(String.format("failed to render template [%s]\n", getUrl()), e);
            }
        }
    }

    protected String getTemplate() throws IOException {
        // XXX: interpreter could be null...
        return engine.getResourceLocator().getString(getUrl(), encoding, null);
    }

    @Override
    public boolean checkResource(Locale locale) throws Exception {
        try {
            // XXX: interpreter could be null...
            engine.getResourceLocator().getString(getUrl(), encoding, null);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public Charset getEncoding() {
        return encoding;
    }

    public void setEncoding(Charset encoding) {
        this.encoding = encoding;
    }

    public Jinjava getEngine() {
        return engine;
    }

    public void setEngine(Jinjava engine) {
        this.engine = engine;
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
