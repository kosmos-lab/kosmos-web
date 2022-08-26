package de.kosmos_lab.web.server.example.servlets;

import de.kosmos_lab.web.server.WebServer;
import de.kosmos_lab.web.server.servlets.BaseServlet;
import de.kosmos_lab.web.server.servlets.BaseServletRequest;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = {"/online"}, loadOnStartup = 1)
public class OnlineServlet extends BaseServlet {
    public OnlineServlet(WebServer server) {
        super(server);
    }
    
    public void get(BaseServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setStatus(WebServer.STATUS_NO_RESPONSE);
    }
}
