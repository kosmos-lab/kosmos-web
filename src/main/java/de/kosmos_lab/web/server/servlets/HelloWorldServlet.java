package de.kosmos_lab.web.server.servlets;

import de.kosmos_lab.web.server.ExampleWebServer;
import de.kosmos_lab.web.server.WebServer;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = {"/hello_world"}, loadOnStartup = 1)
public class HelloWorldServlet extends ExampleServlet {
    public HelloWorldServlet(ExampleWebServer server) {
        super(server);
    }
    
    public void get(MyHttpServletRequest request, HttpServletResponse response)
            throws IOException {
        sendText(request, response, "Hello you");
    }
}
