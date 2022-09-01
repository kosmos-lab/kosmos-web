package de.kosmos_lab.web.server.servlets.openapi;

import de.kosmos_lab.web.doc.openapi.ApiEndpoint;
import de.kosmos_lab.web.server.WebServer;

@ApiEndpoint(path = "/doc/openapi.html", userLevel = -1, hidden = true)
public class OpenApiIndex3Servlet extends OpenApiIndexServlet {

    public OpenApiIndex3Servlet(WebServer webServer) {
        super(webServer);
    }
}