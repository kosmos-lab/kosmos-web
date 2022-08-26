package de.kosmos_lab.web.server.example.servlets.user;

import de.kosmos_lab.web.data.User;
import de.kosmos_lab.web.doc.openapi.ApiEndpoint;
import de.kosmos_lab.web.exceptions.LoginFailedException;
import de.kosmos_lab.web.exceptions.ParameterNotFoundException;
import de.kosmos_lab.web.persistence.ISesssionPersistence;
import de.kosmos_lab.web.persistence.IUserPersistence;
import de.kosmos_lab.web.persistence.exceptions.NoPersistenceException;
import de.kosmos_lab.web.server.example.ExampleWebServer;
import de.kosmos_lab.web.server.servlets.BaseServlet;
import de.kosmos_lab.web.server.servlets.BaseServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@ApiEndpoint(path = "/user/login", hidden = true, load = false)
public class LoginServlet extends BaseServlet {
    public LoginServlet(ExampleWebServer server) {

        super(server);
    }


    public void post(BaseServletRequest request, HttpServletResponse response)
            throws LoginFailedException, ParameterNotFoundException, IOException, NoPersistenceException, NoSuchAlgorithmException, InvalidKeyException {
        ExampleWebServer server = (ExampleWebServer) this.server;
        User user = server.getPersistence(IUserPersistence.class).login(request.getParameter("username", true), request.getParameter("password", true));


        String jwt = server.getPersistence(ISesssionPersistence.class).getJWT(user);
        sendText(request, response, jwt);


    }
}
