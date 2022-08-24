package de.kosmos_lab.web.server.servlets.session;

import de.kosmos_lab.web.persistence.ISesssionPersistence;
import de.kosmos_lab.web.persistence.exceptions.NoPersistenceException;
import de.kosmos_lab.web.server.ExampleWebServer;
import de.kosmos_lab.web.server.WebServer;
import de.kosmos_lab.web.server.servlets.AuthedServlet;
import de.kosmos_lab.web.server.servlets.MyHttpServletRequest;
import de.kosmos_lab.web.exceptions.ParameterNotFoundException;
import org.json.JSONObject;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = {"/session/kill"}, loadOnStartup = 1)

public class KillServlet extends AuthedServlet {
    public KillServlet(ExampleWebServer webServer) {
        super(webServer, 1);
    }
    
    public void post(MyHttpServletRequest request, HttpServletResponse response) throws IOException, ParameterNotFoundException, NoPersistenceException {
        String jwtid = request.getParameter("id", true);
        JSONObject jwt = server.getPersistence(ISesssionPersistence.class).getJWT(jwtid);
        if (request.getUser().getName().equals(jwt.getString("name"))) {
            server.getPersistence(ISesssionPersistence.class).killJWT(jwtid);
            response.setStatus(MyServlet.STATUS_NO_RESPONSE);
            return;
        }
        if (request.getUser().isAdmin() && request.getUser().getLevel() > jwt.getInt("level")) {
            server.getPersistence(ISesssionPersistence.class).killJWT(jwtid);
            response.setStatus(MyServlet.STATUS_NO_RESPONSE);
            return;
        }
        response.setStatus(MyServlet.STATUS_FORBIDDEN);
        return;
    }
    
}
