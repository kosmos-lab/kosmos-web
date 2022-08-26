package de.kosmos_lab.web.client;


import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.jetbrains.annotations.Nullable;
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
        super(baseurl,user,pass);

        
    }

    @Override
    public boolean addAuthToRequest(@Nullable Request request) {
        request.header("Authorization", "Bearer " + this.token);
        return true;
    }


    @CheckForNull public String login() {
        Request request = this.createRequest("/user/login", HttpMethod.POST);
        if (request != null ) {
            try {
                request.param("username", getUserName());
                request.param("password", getPassword());
                ContentResponse response = request.send();
                logger.info("login status: {}", response.getStatus());
                if (response.getStatus() == 200) {
                    return response.getContentAsString();

                }
                else {
                    logger.info("login message: {}", response.getContentAsString());
                }
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                logger.error("could not get Response for Request", e);
            }
        }
        return null;
    }
}