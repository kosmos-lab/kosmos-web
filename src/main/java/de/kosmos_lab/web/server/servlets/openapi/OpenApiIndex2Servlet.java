package de.kosmos_lab.web.server.servlets.openapi;

import de.kosmos_lab.web.doc.openapi.ApiEndpoint;
import de.kosmos_lab.web.server.WebServer;

@ApiEndpoint(path = "/doc/", userLevel = -1, hidden = true)
public class OpenApiIndex2Servlet extends OpenApiIndexServlet {

    public OpenApiIndex2Servlet(WebServer webServer) {
        super(webServer);
    }
}