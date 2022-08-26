package de.kosmos_lab.web.server.example.servlets.user;

import de.kosmos_lab.web.doc.openapi.ApiEndpoint;
import de.kosmos_lab.web.server.example.ExampleWebServer;
import de.kosmos_lab.web.server.example.ExampleAuthedServlet;
import de.kosmos_lab.web.server.servlets.BaseServletRequest;
import de.kosmos_lab.web.server.servlets.MyHttpServletRequest;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@ApiEndpoint(path = "/user/view",hidden = true,load = false)

public class UserViewServlet extends ExampleAuthedServlet {
    public UserViewServlet(ExampleWebServer webServer) {
        super(webServer, 10);
    }
    
    public void get(BaseServletRequest request, HttpServletResponse response) throws IOException {
        sendText(request, response, "Hello - you are worthy of seeing this!");
        
    }
    
}
