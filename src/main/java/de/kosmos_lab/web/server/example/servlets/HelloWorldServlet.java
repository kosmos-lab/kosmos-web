package de.kosmos_lab.web.server.example.servlets;

import de.kosmos_lab.web.doc.openapi.ApiEndpoint;
import de.kosmos_lab.web.server.WebServer;
import de.kosmos_lab.web.server.servlets.BaseServlet;
import de.kosmos_lab.web.server.servlets.BaseServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@ApiEndpoint(path = "/hello_world",
        hidden = true, // this is just an example, we don't want to propagate it to real documents
        load = false) // this is just an example, we don't want to have it actually load automatically
public class HelloWorldServlet extends BaseServlet {
    public HelloWorldServlet(WebServer server) {
        super(server);
    }

    public void get(BaseServletRequest request, HttpServletResponse response)
            throws IOException {
        sendText(request, response, "Hello you");
    }
}
