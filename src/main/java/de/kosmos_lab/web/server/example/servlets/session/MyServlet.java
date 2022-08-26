package de.kosmos_lab.web.server.example.servlets.session;

import de.kosmos_lab.web.doc.openapi.ApiEndpoint;
import de.kosmos_lab.web.persistence.ISesssionPersistence;
import de.kosmos_lab.web.persistence.exceptions.NoPersistenceException;
import de.kosmos_lab.web.server.WebServer;
import de.kosmos_lab.web.server.example.ExampleWebServer;
import de.kosmos_lab.web.server.example.ExampleAuthedServlet;
import de.kosmos_lab.web.server.servlets.BaseServletRequest;
import de.kosmos_lab.web.server.servlets.MyHttpServletRequest;
import org.json.JSONArray;
import org.json.JSONObject;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

@ApiEndpoint(path = "/session/my",hidden = true,load = false)
public class MyServlet extends ExampleAuthedServlet {
    public MyServlet(ExampleWebServer webServer) {
        super(webServer, 1);
    }
    
    public void get(BaseServletRequest request, HttpServletResponse response) throws IOException, NoPersistenceException {
        Collection<JSONObject> list = ((ExampleWebServer)server).getPersistence(ISesssionPersistence.class).getMySessions(request.getUser().getName());
        JSONArray arr = new JSONArray();
        for (JSONObject o : list) {
            arr.put(o.get("jwtid"));
        }
        sendJSON(request, response, arr);
        
    }
    
}
