package de.kosmos_lab.web.client;

import de.kosmos_lab.web.client.exceptions.LoginFailedException;
import de.kosmos_lab.web.client.exceptions.NotFoundException;
import de.kosmos_lab.web.client.exceptions.RequestConflictException;
import de.kosmos_lab.web.client.exceptions.RequestFailedException;
import de.kosmos_lab.web.client.exceptions.RequestNoAccessException;
import de.kosmos_lab.web.client.exceptions.RequestWrongStatusExeption;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public abstract class AuthedHTTPClient extends HttpClient {
    
    
    private static final Logger logger = LoggerFactory.getLogger("AuthedHTTPClient");
    private final String base;
    private int maxRetries = 3;
    
    /**
     * @param baseurl the baseurl to connect to
     * @throws Exception the exception is thrown from jetty http client when starting the client
     */
    public AuthedHTTPClient(String baseurl) throws Exception {
        if (baseurl.endsWith("/")) {
            baseurl = baseurl.substring(0, baseurl.length() - 1);
        }
        this.base = baseurl;
        
        
        //start the underlying client
        this.start();
        
    }
    
    /**
     * implement this method to add your login information to the request
     *
     * @param request
     * @throws LoginFailedException
     */
    public abstract void authorizeRequest(@Nonnull Request request) throws LoginFailedException;
    
    /**
     * create a request to the given url with the given method, if the url is not a complete url the baseurl will be prepended
     *
     * @param url    the url to use
     * @param method the method to use
     * @return a Request object
     */
    @Nonnull
    public Request createRequest(@Nonnull String url, @Nonnull HttpMethod method) {
        if (!url.startsWith("http")) {
            url = base + url;
        }
        
        Request request = newRequest(url);
        request.method(method);
        request.agent("KosmoS Client");
        return request;
    }
    
    /**
     * get the base url
     *
     * @return the base url
     */
    @Nonnull
    public String getBase() {
        return this.base;
    }
    
    /**
     * get the max amount of retries
     *
     * @return the amount of retries
     */
    public int getMaxRetries() {
        return this.maxRetries;
    }
    
    protected ContentResponse getResponse(@Nonnull String url, @Nonnull HttpMethod method, int maxRetries, @CheckForNull Integer expectedStatus) throws RequestFailedException {
        return getResponse(url, method, null, maxRetries, expectedStatus);
    }
    
    protected ContentResponse getResponse(@Nonnull String url, @Nonnull HttpMethod method, @CheckForNull Object body, @CheckForNull Integer expectedStatus) throws RequestFailedException {
        return getResponse(url, method, body, getMaxRetries(), expectedStatus);
    }
    
    public abstract String getToken();
    
    /**
     * set the max amount of retries (defaults to 3)
     *
     * @param maxRetries the new amount of retries
     */
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    /**
     * get the response for a given result, if 401 is returned the jwt will be renewed, and it will be tried again
     *
     * @param url
     * @param method
     * @param body           the body we want to send (can be null)
     * @param maxRetries     the amount of tests to try again
     * @param expectedStatus
     * @return
     * @throws RequestFailedException
     */
    @Nonnull
    protected ContentResponse getResponse(@Nonnull String url, @Nonnull HttpMethod method, @CheckForNull Object body, int maxRetries, @CheckForNull Integer expectedStatus) throws RequestFailedException {
        
        Request request = createRequest(url, method);
        authorizeRequest(request);
        if (body != null) {
            if (body instanceof JSONObject) {
                request.content(new StringContentProvider(body.toString()), "application/json");
                
                //request.body(new StringRequestContent("application/json", body.toString()));
            } else if (body instanceof JSONArray) {
                //request.body(new StringRequestContent("application/json", body.toString()));
                request.content(new StringContentProvider(body.toString()), "application/json");
            } else {
                //request.body(new StringRequestContent("application/text", body.toString()));
                request.content(new StringContentProvider(body.toString()), "application/text");
            }
        }
        
        try {
            ContentResponse response = request.send();
            int s = response.getStatus();
            //logger.info("got status: {}",s);
            
            
            //if we gave an expected status
            if (expectedStatus != null) {
                //check if the code is correct
                if (s != expectedStatus) {
                    if (s == 401) {
                        //401 means our token expired, lets get a new one
                        if (maxRetries > 0) {
                            this.clearToken();
                            refreshToken();
                            return getResponse(url, method, body, maxRetries - 1, expectedStatus);
                        }
                    }
                    if (s == 404) {
                        //no reason to retry, it will never work
                        throw new NotFoundException(s);
                    }
                    if (s == 409) {
                        //no reason to retry, it will never work
                        throw new RequestConflictException(s);
                    }
                    
                    if (s == 403) {
                        //no reason to retry, it will never work
                        throw new RequestNoAccessException(s);
                    }
                    if (maxRetries > 0) {
                        logger.info("got wrong status?! {} {}", response.getStatus(), expectedStatus);
                        return getResponse(url, method, body, maxRetries - 1, expectedStatus);
                    }
                    throw new RequestWrongStatusExeption(response.getStatus());
                }
                
            }
            
            
            return response;
            
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            e.printStackTrace();
            //as long as there are retries left try it again
            if (maxRetries > 0) {
                return getResponse(url, method, body, maxRetries - 1, expectedStatus);
            }
            throw new RequestFailedException(e);
            
        }
        
        
    }
    
    
    abstract public void clearToken();
    
    /**
     * create a new request with empty body and default retries
     *
     * @param url
     * @param method
     * @param expectedStatus
     * @return
     * @throws RequestFailedException
     */
    protected ContentResponse getResponse(@Nonnull String url, @Nonnull HttpMethod method, @Nullable Integer expectedStatus) throws RequestFailedException {
        return this.getResponse(url, method, null, getMaxRetries(), expectedStatus);
        
    }
    
    /**
     * implement this method to get a new token from the system
     *
     * @throws LoginFailedException
     */
    public abstract boolean refreshToken() throws LoginFailedException;
}
