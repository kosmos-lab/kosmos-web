package de.kosmos_lab.web.server.example.servlets.session;

import de.kosmos_lab.web.doc.openapi.ApiEndpoint;
import de.kosmos_lab.web.exceptions.ParameterNotFoundException;
import de.kosmos_lab.web.persistence.ISesssionPersistence;
import de.kosmos_lab.web.persistence.exceptions.NoPersistenceException;
import de.kosmos_lab.web.server.WebServer;
import de.kosmos_lab.web.server.example.ExampleAuthedServlet;
import de.kosmos_lab.web.server.example.ExampleWebServer;
import de.kosmos_lab.web.server.servlets.BaseServletRequest;
import de.kosmos_lab.web.server.servlets.MyHttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;

import java.io.IOException;

@ApiEndpoint(path = "/session/kill",hidden = true,load = false, userLevel = 1)

public class KillServlet extends ExampleAuthedServlet {
    public KillServlet(ExampleWebServer webServer, int level) {
        super(webServer, level);
    }
    
    public void post(BaseServletRequest request, HttpServletResponse response) throws IOException, ParameterNotFoundException, NoPersistenceException {
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
