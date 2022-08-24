package de.kosmos_lab.web.server.servlets.user;

import de.kosmos_lab.web.persistence.ISesssionPersistence;
import de.kosmos_lab.web.persistence.IUserPersistence;
import de.kosmos_lab.web.persistence.exceptions.NoPersistenceException;
import de.kosmos_lab.web.server.ExampleWebServer;
import de.kosmos_lab.web.server.servlets.MyHttpServletRequest;
import de.kosmos_lab.web.server.servlets.ExampleServlet;
import de.kosmos_lab.web.server.WebServer;
import de.kosmos_lab.web.data.User;
import de.kosmos_lab.web.exceptions.LoginFailedException;
import de.kosmos_lab.web.exceptions.ParameterNotFoundException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@WebServlet(urlPatterns = {"/user/login"}, loadOnStartup = 1)

public class LoginServlet extends ExampleServlet {
    public LoginServlet(ExampleWebServer server) {
        super(server);
    }
    
    
    public void post(MyHttpServletRequest request, HttpServletResponse response)
            throws IOException, LoginFailedException, NoSuchAlgorithmException, InvalidKeyException, ParameterNotFoundException, NoPersistenceException {
        
        User user = server.getPersistence(IUserPersistence.class).login(request.getParameter("username", true), request.getParameter("password", true));
        
        
        String jwt = server.getPersistence(ISesssionPersistence.class).getJWT(user);
        sendText(request, response, jwt);
        
        
    }
}
