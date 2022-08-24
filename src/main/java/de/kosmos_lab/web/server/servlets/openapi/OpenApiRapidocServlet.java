package de.kosmos_lab.web.server.servlets.openapi;

import de.kosmos_lab.web.doc.openapi.ApiEndpoint;
import de.kosmos_lab.web.server.WebServer;
import de.kosmos_lab.web.server.servlets.BaseServlet;
import de.kosmos_lab.web.server.servlets.BaseServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@ApiEndpoint(path = "/doc/rapidoc-min.js", userLevel = -1, hidden = true)
public class OpenApiRapidocServlet extends BaseServlet {
    public String cached = null;

    public OpenApiRapidocServlet(WebServer webServer) {
        super(webServer);

    }


    @Override
    public void get(BaseServletRequest request, HttpServletResponse response)
            throws IOException {
        if (cached == null) {

            cached = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("/web/doc/rapidoc-min.js"), StandardCharsets.UTF_8);


        }
        sendHTML(request, response, cached);


    }

}