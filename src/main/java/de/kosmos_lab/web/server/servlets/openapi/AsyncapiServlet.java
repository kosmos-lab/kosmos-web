package de.kosmos_lab.web.server.servlets.openapi;

import de.kosmos_lab.web.doc.openapi.ApiEndpoint;
import de.kosmos_lab.web.server.WebServer;
import de.kosmos_lab.web.server.servlets.BaseServlet;
import de.kosmos_lab.web.server.servlets.BaseServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@ApiEndpoint(path = "/doc/asyncapi.html", userLevel = -1, hidden = true)
public class AsyncapiServlet extends AsyncServlet {
    public AsyncapiServlet(WebServer webServer) {
        super(webServer);
    }
}