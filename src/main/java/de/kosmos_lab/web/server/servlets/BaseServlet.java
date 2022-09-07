package de.kosmos_lab.web.server.servlets;


import de.kosmos_lab.web.annotations.responses.ApiResponse;
import de.kosmos_lab.web.exceptions.ServletException;
import de.kosmos_lab.web.exceptions.UnauthorizedException;
import de.kosmos_lab.web.server.WebServer;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.everit.json.schema.ValidationException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static de.kosmos_lab.web.server.WebServer.STATUS_FAILED;


public class BaseServlet extends HttpServlet {
    protected static final org.slf4j.Logger logger = LoggerFactory.getLogger("KosmoSServlet");
    protected final WebServer server;
    final ALLOW_AUTH allow_auth;
    ;
    protected ConcurrentHashMap<String, Object> values = new ConcurrentHashMap();

    /*public <T> T get(String key,Class<T> clazz) {
        return (T) values.get(key);
    }
    public void set(String key, Object o) {
        values.put(key,o);
    }*/
    public BaseServlet(WebServer server) {
        this(server, ALLOW_AUTH.PARAMETER_AND_HEADER);
    }

    public BaseServlet(WebServer server, ALLOW_AUTH allow_auth) {
        this.server = server;

        this.allow_auth = allow_auth;
        logger.info("created servlet {}", this.getClass());
    }

    public static void sendJSON(BaseServletRequest req, HttpServletResponse response, JSONObject obj) throws IOException {
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

    public static void sendJSON(BaseServletRequest req, HttpServletResponse response, JSONArray obj) throws IOException {
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

    public static void sendJSON(BaseServletRequest req, HttpServletResponse response, String text) throws IOException {
        response.setHeader("Content-Type", "application/json");
        response.getWriter().print(text);


    }

    public static void sendJWT(BaseServletRequest req, HttpServletResponse response, String text) throws IOException {
        response.setHeader("Content-Type", "application/jwt");
        response.getWriter().print(text);


    }

    public static void sendText(BaseServletRequest req, HttpServletResponse response, String text) throws IOException {
        response.setHeader("Content-Type", "text/plain");
        response.getWriter().print(text);
    }

    public static void sendHTML(BaseServletRequest req, HttpServletResponse response, String text) throws IOException {
        response.setHeader("Content-Type", "text/html");
        response.getWriter().print(text);
    }

    public static void sendTextAs(BaseServletRequest req, HttpServletResponse response, String text, String type) throws IOException {
        response.setHeader("Content-Type", type);
        response.getWriter().print(text);
    }

    public static void sendXML(BaseServletRequest req, HttpServletResponse response, String text) throws IOException {
        response.setHeader("Content-Type", "application/xml");
        response.getWriter().print(text);
    }

    protected void addCORSHeader(HttpServletRequest req, HttpServletResponse response) {
        String origin = req.getHeader("Origin");
        if (origin == null || origin.length() == 0) {
            origin = "*";
        } else {
            //origin = URLEncoder.encode(origin, StandardCharsets.UTF_8);

        }

        response.setHeader("Access-Control-Allow-Origin", origin);
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Cache-Control");
        response.setHeader("Access-Control-Allow-Credentials", "true");
    }

    public void handleException(HttpServletRequest request, HttpServletResponse response, Exception e) {
        //logger.warn("got exception {}",e.getMessage(),e);
        if (e instanceof de.kosmos_lab.web.exceptions.ServletException) {
            ApiResponse r = e.getClass().getAnnotation(ApiResponse.class);
            if (r != null && r.responseCode() != null) {
                response.setStatus(r.responseCode().statusCode());
            } else {
                response.setStatus(STATUS_FAILED);
            }
            try {
                response.getWriter().print(e.getMessage());
                return;
            } catch (IOException ex) {
                //throw new RuntimeException(ex);
                logger.error("error while writing error message", ex);
            }
        } else if (e instanceof JSONException) {
            response.setStatus(de.kosmos_lab.web.server.WebServer.STATUS_UNPROCESSABLE);
            try {
                response.getWriter().print(e.getMessage());
                return;
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } else if (e instanceof ValidationException) {
            response.setStatus(WebServer.STATUS_VALIDATION_FAILED);
            try {
                response.getWriter().print(e.getMessage());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            ApiResponse r = e.getClass().getAnnotation(ApiResponse.class);
            if (r != null && r.responseCode() != null) {
                response.setStatus(r.responseCode().statusCode());
            } else {
                response.setStatus(de.kosmos_lab.web.server.WebServer.STATUS_ERROR);

            }
            try {
                response.getWriter().print(e.getMessage());
                return;
            } catch (IOException ex) {
                //throw new RuntimeException(ex);
                logger.error("error while writing error message", ex);
            }

        }
        logger.warn("got unexpected exception {}",e.getMessage(),e);
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

    public void delete(BaseServletRequest request, HttpServletResponse response) throws ServletException {

        response.setStatus(de.kosmos_lab.web.server.WebServer.STATUS_METHOD_NOT_ALLOWED);
    }

    public void doDelete(HttpServletRequest request, HttpServletResponse response)

            throws IOException {

        try {
            if (this.isAllowed(request, response)) {

                addCORSHeader(request, response);

                delete(new BaseServletRequest(request), response);


            }
        } catch (Exception e) {
            handleException(request, response, e);
        }

    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)

            throws IOException {
        try {
            if (this.isAllowed(request, response)) {

                addCORSHeader(request, response);
                get(new BaseServletRequest(request), response);

            }
        } catch (Exception e) {
            handleException(request, response, e);
        }

    }

    public void doOptions(HttpServletRequest request, HttpServletResponse response)

            throws IOException {
        addCORSHeader(request, response);

    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)

            throws IOException {

        //logger.info("HITTING doPOST");
        try {
            if (this.isAllowed(request, response)) {

                addCORSHeader(request, response);

                post(new BaseServletRequest(request), response);


            }
        } catch (Exception e) {
            handleException(request, response, e);
        }

    }

    public void doPut(HttpServletRequest request, HttpServletResponse response)

            throws IOException {
        //super.doPut(request, response);
        try {
            if (this.isAllowed(request, response)) {

                addCORSHeader(request, response);

                put(new BaseServletRequest(request), response);

            }
        } catch (Exception e) {
            handleException(request, response, e);
        }

    }

    public void get(BaseServletRequest request, HttpServletResponse response) throws Exception {
        //logger.info("HITTING GET");

        response.setStatus(de.kosmos_lab.web.server.WebServer.STATUS_METHOD_NOT_ALLOWED);
    }

    protected boolean isAllowed(HttpServletRequest request, HttpServletResponse response) throws UnauthorizedException {
        return true;
    }

    public void options(BaseServletRequest request, HttpServletResponse response) {

        response.setStatus(de.kosmos_lab.web.server.WebServer.STATUS_OK);
        addCORSHeader(request.getRequest(), response);

    }

    public void post(BaseServletRequest request, HttpServletResponse response) throws Exception {
        //logger.info("HITTING POST");
        response.setStatus(de.kosmos_lab.web.server.WebServer.STATUS_METHOD_NOT_ALLOWED);
    }

    public void put(BaseServletRequest request, HttpServletResponse response) throws Exception {
        //logger.info("HITTING PUT");
        response.setStatus(de.kosmos_lab.web.server.WebServer.STATUS_METHOD_NOT_ALLOWED);
    }

    public enum ALLOW_AUTH {HEADER_ONLY, PARAMETER_AND_HEADER}
}
