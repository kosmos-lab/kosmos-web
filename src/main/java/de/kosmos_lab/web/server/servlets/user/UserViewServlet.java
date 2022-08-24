package de.kosmos_lab.web.server.servlets.user;

import de.kosmos_lab.web.server.ExampleWebServer;
import de.kosmos_lab.web.server.servlets.AuthedServlet;
import de.kosmos_lab.web.server.servlets.MyHttpServletRequest;
import de.kosmos_lab.web.server.WebServer;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = {"/user/view"}, loadOnStartup = 1)

public class UserViewServlet extends AuthedServlet {
    public UserViewServlet(ExampleWebServer webServer) {
        super(webServer, 10);
    }
    
    public void get(MyHttpServletRequest request, HttpServletResponse response) throws IOException {
        sendText(request, response, "Hello - you are worthy of seeing this!");
        
    }
    
}
