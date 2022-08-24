package de.kosmos_lab.web.client;


import de.kosmos_lab.web.client.exceptions.LoginFailedException;
import de.kosmos_lab.web.client.exceptions.RequestFailedException;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * test client, primarly used for testing
 */
public class MyTestClient extends AuthedHTTPClient {
    
    private static final Logger logger = LoggerFactory.getLogger("MyClient");
    
    
    private final String user;
    private final String pass;
    
    boolean connected = false;
    private String token = null;
    
    
    /**
     * create a new Client
     *
     * @param baseurl the base url
     * @param user    the password
     * @param pass    the password
     * @throws Exception some catastrophic failure
     */
    public MyTestClient(@Nonnull String baseurl, @Nonnull String user, @Nonnull String pass) throws Exception {
        super(baseurl);
        this.user = user;
        this.pass = pass;
        
    }
    
    
    public void authorizeRequest(@Nonnull Request request) throws LoginFailedException {
        if (token == null) {
            if (!refreshToken()) {
                throw new LoginFailedException();
            }
        }
        if (token != null) {
            //request.headers(httpFields -> httpFields.add("Authorization", "Bearer " + token));
            request.header("Authorization", "Bearer " + this.token);
        }
        
    }
    
    @Override
    public String getToken() {
        return this.token;
        
    }
    
    
    public boolean isConnected() {
        return this.connected;
    }
    
    @CheckForNull
    public String login() {
        Request request = this.createRequest("/user/login", HttpMethod.POST);
        try {
            request.param("username", user);
            request.param("password", pass);
            ContentResponse response = request.send();
            
            if (response.getStatus() == 200) {
                
                return response.getContentAsString();
                
            }
            logger.warn("login status: {}", response.getStatus());
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    
    /**
     * get a new JWT token (try to login)
     *
     * @return true if login was successful
     */
    public boolean refreshToken() {
        if (user != null && pass != null) {
            String t = login();
            if (t != null) {
                this.token = t;
                return true;
            }
        } else {
            this.token = "";
            return true;
        }
        return false;
        
    }
    
    public boolean refreshTokenIfNeeded() {
        if (token == null) {
            return refreshToken();
        }
        return true;
    }
    
    public ContentResponse getResponse(@Nonnull String url, @Nonnull HttpMethod method, @CheckForNull Object body, int maxRetries, @CheckForNull Integer expectedStatus) throws RequestFailedException {
        return super.getResponse(url, method, body, maxRetries, expectedStatus);
        
    }
    
    @Override
    public void clearToken() {
        this.token = null;
        
    }
}