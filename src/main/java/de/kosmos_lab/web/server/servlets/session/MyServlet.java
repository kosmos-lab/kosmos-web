package de.kosmos_lab.web.server.servlets.session;

import de.kosmos_lab.web.persistence.ISesssionPersistence;
import de.kosmos_lab.web.persistence.exceptions.NoPersistenceException;
import de.kosmos_lab.web.server.ExampleWebServer;
import de.kosmos_lab.web.server.servlets.AuthedServlet;
import de.kosmos_lab.web.server.servlets.MyHttpServletRequest;
import de.kosmos_lab.web.server.WebServer;
import org.json.JSONArray;
import org.json.JSONObject;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

@WebServlet(urlPatterns = {"/session/my"}, loadOnStartup = 1)

public class MyServlet extends AuthedServlet {
    public MyServlet(ExampleWebServer webServer) {
        super(webServer, 1);
    }
    
    public void get(MyHttpServletRequest request, HttpServletResponse response) throws IOException, NoPersistenceException {
        Collection<JSONObject> list = ((ExampleWebServer)server).getPersistence(ISesssionPersistence.class).getMySessions(request.getUser().getName());
        JSONArray arr = new JSONArray();
        for (JSONObject o : list) {
            arr.put(o.get("jwtid"));
        }
        sendJSON(request, response, arr);
        
    }
    
}
