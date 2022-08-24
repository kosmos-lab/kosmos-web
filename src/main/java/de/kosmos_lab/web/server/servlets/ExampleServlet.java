package de.kosmos_lab.web.server.servlets;


import de.kosmos_lab.web.persistence.exceptions.NoPersistenceException;
import de.kosmos_lab.web.exceptions.LoginFailedException;
import de.kosmos_lab.web.exceptions.NotFoundException;
import de.kosmos_lab.web.exceptions.ParameterNotFoundException;
import de.kosmos_lab.web.exceptions.UnauthorizedException;
import de.kosmos_lab.web.server.ExampleWebServer;
import de.kosmos_lab.web.server.WebServer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExampleServlet extends HttpServlet {
    protected static final org.slf4j.Logger logger = LoggerFactory.getLogger("MyServlet");
    protected static final int STATUS_OK = 200;
    protected static final int STATUS_NO_RESPONSE = 204;
    protected static final int STATUS_FORBIDDEN = 403;
    
    protected static final int STATUS_FAILED = 400;
    protected static final int STATUS_NO_AUTH = 401;
    protected static final int STATUS_NOT_FOUND = 404;
    protected static final int STATUS_CONFLICT = 409;
    protected static final int STATUS_UNPROCESSABLE = 422;
    protected static final int STATUS_ERROR = 500;
    protected static final int STATUS_METHOD_NOT_ALLOWED = 405;
    protected static final Pattern pattern_host = Pattern.compile("^(?<name>http|https|ws|wss)://(?<host>(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])*)(:(?<port>[0-9]*))?$");
    protected final ExampleWebServer server;
    
    public ExampleServlet(ExampleWebServer server) {
        this.server = server;
        
    }
    
    
    protected void addCORSHeader(HttpServletRequest req, HttpServletResponse resp) {
        String origin = req.getHeader("Origin");
        if (origin == null || origin.length() == 0) {
            origin = "*";
        } else {
            Matcher m = pattern_host.matcher(origin);
            if (!m.matches()) {
                origin = "*";
            }
            else {
                origin =URLEncoder.encode(origin, StandardCharsets.UTF_8);
            }



        }
        resp.setHeader("Access-Control-Allow-Origin", origin);
        resp.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Cache-Control");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
    }
    
    protected boolean checkParameter(HttpServletRequest req, HttpServletResponse response, String[] keys) throws IOException {
        Enumeration<String> it = req.getParameterNames();
        HashMap<String, Boolean> missing = new HashMap<>();
        for (String k : keys) {
            missing.put(k, true);
        }
        while (it.hasMoreElements()) {
            String e = it.nextElement();
            
            missing.remove(e);
        }
        if (!missing.isEmpty()) {
            response.setStatus(STATUS_FAILED);
            PrintWriter w = response.getWriter();
            for (String k : missing.keySet()) {
                w.println("missing parameter '" + k + "'");
            }
            return false;
        }
        return true;
        
        
    }
    
    public void delete(MyHttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, ParameterNotFoundException, NotFoundException {
        
        response.setStatus(STATUS_METHOD_NOT_ALLOWED);
    }
    
    public void doDelete(HttpServletRequest request, HttpServletResponse response)
            
            throws ServletException, IOException {
        
        //super.doDelete(request, response);
        if (this.isAllowed(request, response)) {
            try {
                addCORSHeader(request, response);
                
                delete(new MyHttpServletRequest(request), response);
                
            } catch (ParameterNotFoundException e) {
                response.setStatus(STATUS_UNPROCESSABLE);
                response.getWriter().print(e.getMessage());
            } catch (NotFoundException e) {
                response.setStatus(STATUS_NOT_FOUND);
                response.getWriter().print(e.getMessage());
                
            } catch (JSONException e) {
                response.setStatus(STATUS_UNPROCESSABLE);
                response.getWriter().print(e.getMessage());
            } catch (Exception e) {
                response.setStatus(STATUS_ERROR);
                e.printStackTrace();
                
            }
        }
        
    }
    
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            
            throws ServletException, IOException {
        
        if (this.isAllowed(request, response)) {
            try {
                addCORSHeader(request, response);
                get(new MyHttpServletRequest(request), response);
                
            } catch (ParameterNotFoundException e) {
                response.setStatus(STATUS_UNPROCESSABLE);
                response.getWriter().print(e.getMessage());
            } catch (NotFoundException e) {
                response.setStatus(STATUS_NOT_FOUND);
                response.getWriter().print(e.getMessage());
                
            } catch (JSONException e) {
                response.setStatus(STATUS_UNPROCESSABLE);
                response.getWriter().print(e.getMessage());
            } catch (de.kosmos_lab.web.exceptions.LoginFailedException e) {
                response.setStatus(STATUS_FORBIDDEN);
                response.getWriter().print(e.getMessage());
            } catch (de.kosmos_lab.web.exceptions.UnauthorizedException e) {
                response.setStatus(STATUS_NO_AUTH);
                response.getWriter().print(e.getMessage());
            } catch (Exception e) {
                response.setStatus(STATUS_ERROR);
                e.printStackTrace();
                
            }
        }
        
    }
    
    public void doOptions(HttpServletRequest request, HttpServletResponse response)
            
            throws ServletException, IOException {
        addCORSHeader(request, response);
        
    }
    
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            
            throws ServletException, IOException {
        
        //logger.info("HITTING doPOST");
        
        if (this.isAllowed(request, response)) {
            try {
                addCORSHeader(request, response);
                
                post(new MyHttpServletRequest(request), response);
                
            } catch (JSONException e) {
                response.setStatus(STATUS_UNPROCESSABLE);
                response.getWriter().print(e.getMessage());
            } catch (ParameterNotFoundException e) {
                response.setStatus(STATUS_UNPROCESSABLE);
                response.getWriter().print(e.getMessage());
            } catch (NotFoundException e) {
                response.setStatus(STATUS_NOT_FOUND);
                response.getWriter().print(e.getMessage());
                
            } catch (de.kosmos_lab.web.exceptions.LoginFailedException e) {
                response.setStatus(STATUS_FORBIDDEN);
                response.getWriter().print(e.getMessage());
            } catch (de.kosmos_lab.web.exceptions.UnauthorizedException e) {
                response.setStatus(STATUS_NO_AUTH);
            } catch (Exception e) {
                response.setStatus(STATUS_ERROR);
                e.printStackTrace();
                
            }
        }
        
    }
    
    public void doPut(HttpServletRequest request, HttpServletResponse response)
            
            throws ServletException, IOException {
        super.doPut(request, response);
        if (this.isAllowed(request, response)) {
            try {
                addCORSHeader(request, response);
                
                put(new MyHttpServletRequest(request), response);
                
            } catch (ParameterNotFoundException e) {
                response.setStatus(STATUS_UNPROCESSABLE);
                response.getWriter().print(e.getMessage());
            } catch (NotFoundException e) {
                response.setStatus(STATUS_NOT_FOUND);
                response.getWriter().print(e.getMessage());
                
            } catch (JSONException e) {
                response.setStatus(STATUS_UNPROCESSABLE);
                response.getWriter().print(e.getMessage());
            } catch (de.kosmos_lab.web.exceptions.LoginFailedException e) {
                response.setStatus(STATUS_FORBIDDEN);
                response.getWriter().print(e.getMessage());
            } catch (de.kosmos_lab.web.exceptions.UnauthorizedException e) {
                response.setStatus(STATUS_NO_AUTH);
            } catch (Exception e) {
                response.setStatus(STATUS_ERROR);
                e.printStackTrace();
                
            }
        }
        
    }
    
    public void get(MyHttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, ParameterNotFoundException, NotFoundException, de.kosmos_lab.web.exceptions.LoginFailedException, de.kosmos_lab.web.exceptions.UnauthorizedException, NoPersistenceException {
        //logger.info("HITTING GET");
        
        response.setStatus(STATUS_METHOD_NOT_ALLOWED);
    }
    
    
    protected boolean isAllowed(HttpServletRequest request, HttpServletResponse response) {
        return true;
    }
    
    public void options(MyHttpServletRequest request, HttpServletResponse response) {
        
        response.setStatus(200);
        addCORSHeader(request.getRequest(), response);
        
    }
    
    public void post(MyHttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, ParameterNotFoundException, NotFoundException, de.kosmos_lab.web.exceptions.UnauthorizedException, NoSuchAlgorithmException, InvalidKeyException, NoPersistenceException, de.kosmos_lab.web.exceptions.LoginFailedException {
        //logger.info("HITTING POST");
        response.setStatus(STATUS_METHOD_NOT_ALLOWED);
    }
    
    public void put(MyHttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, ParameterNotFoundException, NotFoundException, LoginFailedException, UnauthorizedException {
        //logger.info("HITTING PUT");
        response.setStatus(STATUS_METHOD_NOT_ALLOWED);
    }
    
    protected void sendJSON(MyHttpServletRequest req, HttpServletResponse response, JSONObject obj) throws IOException {
        response.setHeader("Content-Type", "application/json");
        try {
            String p = req.getParameter("pretty");
            if ("1".equals(p)) {
                response.getWriter().print(obj.toString(4));
            } else {
                response.getWriter().print(obj.toString());
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            response.getWriter().print(obj.toString());
        }
        
    }
    
    protected void sendJSON(MyHttpServletRequest req, HttpServletResponse response, JSONArray obj) throws IOException {
        response.setHeader("Content-Type", "application/json");
        try {
            String p = req.getParameter("pretty");
            if ("1".equals(p)) {
                response.getWriter().print(obj.toString(4));
            } else {
                response.getWriter().print(obj.toString());
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            response.getWriter().print(obj.toString());
        }
        
        
    }
    
    protected void sendJWT(MyHttpServletRequest req, HttpServletResponse response, String text) throws IOException {
        response.setHeader("Content-Type", "application/jwt");
        response.getWriter().print(text);
        
        
    }
    
    protected void sendText(MyHttpServletRequest req, HttpServletResponse response, String text) throws IOException {
        response.setHeader("Content-Type", "text/plain");
        response.getWriter().print(text);
    }
    
    protected void sendXML(MyHttpServletRequest req, HttpServletResponse response, String text) throws IOException {
        response.setHeader("Content-Type", "application/xml");
        response.getWriter().print(text);
    }
    
    
}
