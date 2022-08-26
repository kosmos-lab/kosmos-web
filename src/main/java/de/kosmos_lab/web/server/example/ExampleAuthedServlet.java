package de.kosmos_lab.web.server.example;

import de.kosmos_lab.web.data.User;
import de.kosmos_lab.web.exceptions.LoginFailedException;
import de.kosmos_lab.web.persistence.ISesssionPersistence;
import de.kosmos_lab.web.persistence.IUserPersistence;
import de.kosmos_lab.web.persistence.exceptions.NoPersistenceException;
import de.kosmos_lab.web.persistence.exceptions.NotFoundInPersistenceException;
import de.kosmos_lab.web.server.WebServer;
import de.kosmos_lab.web.server.servlets.BaseServlet;
import de.kosmos_lab.web.server.servlets.MyHttpServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;

public abstract class ExampleAuthedServlet extends BaseServlet {
    protected final int level;


    public ExampleAuthedServlet(ExampleWebServer webServer) {
        this(webServer, 1);
    }

    public ExampleAuthedServlet(ExampleWebServer webServer, int level) {
        super(webServer);

        this.level = level;
        logger.info("created servlet {}", this.getClass());
    }


    protected boolean isAllowed(HttpServletRequest request, HttpServletResponse response) {
        String auth = request.getHeader("Authorization");
        ExampleWebServer server = (ExampleWebServer) this.server;
        if (auth != null) {
            auth = auth.trim();
            if (auth.startsWith("Bearer")) {
                auth = auth.substring(6).trim();
            }
            JSONObject s = null;
            try {
                s = server.getPersistence(ISesssionPersistence.class).verifyJWT(auth);
                if (s != null) {

                    try {
                        User u = server.getPersistence(IUserPersistence.class).getUser(s.getString("name"));
                        request.setAttribute("user", u);
                        if (u.canAccess(this.level)) {
                            return true;
                        }
                        response.setStatus(WebServer.STATUS_FORBIDDEN);
                        return false;
                    } catch (NotFoundInPersistenceException e) {
                        e.printStackTrace();
                    }
                    response.setHeader("WWW-Authenticate", "Bearer realm=\"example\",\n" +
                            "                   error=\"invalid_token\",\n" +
                            "                   error_description=\"The access token expired\"");
                    response.setStatus(WebServer.STATUS_NO_AUTH);
                    return false;
                }
            } catch (NoPersistenceException e) {

            }

            response.setHeader("WWW-Authenticate", "Bearer realm=\"example\",\n" +
                    "                   error=\"invalid_token\",\n" +
                    "                   error_description=\"The access token expired\"");
            response.setStatus(WebServer.STATUS_NO_AUTH);
            return false;
        }
        String username = request.getHeader("username");
        String password = request.getHeader("password");

        if (username == null || password == null) {
            username = request.getParameter("username");
            password = request.getParameter("password");
        }
        if (username != null && password != null) {

            try {

                try {
                    User u = server.getPersistence(IUserPersistence.class).login(username, password);
                    if (u != null) {
                        request.setAttribute("user", u);
                        if (u.canAccess(this.level)) {
                            return true;
                        }
                        response.setStatus(WebServer.STATUS_FORBIDDEN);
                        return false;
                    }
                } catch (NoPersistenceException e) {
                    e.printStackTrace();
                }

            } catch (LoginFailedException e) {
                e.printStackTrace();
            }

        }
        response.setStatus(WebServer.STATUS_NO_AUTH);
        return false;


    }

    protected boolean isMeOrAmAdmin(MyHttpServletRequest request, User u) {
        User me = request.getUser();
        //logger.warn(me.toJWT() + " vs " + u.toJWT());

        return me.isAdmin() && me.getLevel() >= u.getLevel();
    }
}