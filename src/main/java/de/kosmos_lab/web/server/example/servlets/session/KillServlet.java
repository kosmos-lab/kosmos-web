package de.kosmos_lab.web.server.example.servlets.session;

import de.kosmos_lab.web.doc.openapi.ApiEndpoint;
import de.kosmos_lab.web.persistence.ISesssionPersistence;
import de.kosmos_lab.web.persistence.exceptions.NoPersistenceException;
import de.kosmos_lab.web.server.WebServer;
import de.kosmos_lab.web.server.example.ExampleWebServer;
import de.kosmos_lab.web.server.example.ExampleAuthedServlet;
import de.kosmos_lab.web.server.servlets.MyHttpServletRequest;
import de.kosmos_lab.web.exceptions.ParameterNotFoundException;
import org.json.JSONObject;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@ApiEndpoint(path = "/session/kill",hidden = true,load = false)

public class KillServlet extends ExampleAuthedServlet {
    public KillServlet(ExampleWebServer webServer) {
        super(webServer, 1);
    }
    
    public void post(MyHttpServletRequest request, HttpServletResponse response) throws IOException, ParameterNotFoundException, NoPersistenceException {
        String jwtid = request.getParameter("id", true);
        JSONObject jwt = ((ExampleWebServer)server).getPersistence(ISesssionPersistence.class).getJWT(jwtid);
        if (request.getUser().getName().equals(jwt.getString("name"))) {
            ((ExampleWebServer)server).getPersistence(ISesssionPersistence.class).killJWT(jwtid);
            response.setStatus(WebServer.STATUS_NO_RESPONSE);
            return;
        }
        if (request.getUser().isAdmin() && request.getUser().getLevel() > jwt.getInt("level")) {
            ((ExampleWebServer)server).getPersistence(ISesssionPersistence.class).killJWT(jwtid);
            response.setStatus(WebServer.STATUS_NO_RESPONSE);
            return;
        }
        response.setStatus(WebServer.STATUS_FORBIDDEN);
        return;
    }
    
}
