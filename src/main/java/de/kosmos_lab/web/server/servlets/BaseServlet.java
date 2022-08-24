package de.kosmos_lab.web.server.servlets;


import de.kosmos_lab.web.annotations.responses.ApiResponse;
import de.kosmos_lab.web.exceptions.ServletException;
import de.kosmos_lab.web.persistence.exceptions.AlreadyExistsException;
import de.kosmos_lab.web.persistence.exceptions.NotFoundInPersistenceException;
import de.kosmos_lab.web.exceptions.ParameterNotFoundException;

import de.kosmos_lab.web.server.WebServer;
import org.everit.json.schema.ValidationException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;


import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;

import static de.kosmos_lab.web.server.WebServer.STATUS_FAILED;


public class BaseServlet extends HttpServlet {
    final ALLOW_AUTH allow_auth;

    public enum ALLOW_AUTH {HEADER_ONLY, PARAMETER_AND_HEADER};

    protected static final org.slf4j.Logger logger = LoggerFactory.getLogger("KosmoSServlet");


    protected final WebServer server;

    public BaseServlet(WebServer server) {
        this(server, ALLOW_AUTH.PARAMETER_AND_HEADER);
    }

    public BaseServlet(WebServer server, ALLOW_AUTH allow_auth) {
        this.server = server;

        this.allow_auth = allow_auth;
        logger.info("created servlet {}", this.getClass());
    }


    protected void addCORSHeader(HttpServletRequest req, HttpServletResponse response) {
        String origin = req.getHeader("Origin");
        if (origin == null || origin.length() == 0) {
            origin = "*";
        } else {
            origin = URLEncoder.encode(origin, StandardCharsets.UTF_8);
        }

        response.setHeader("Access-Control-Allow-Origin", origin);
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Cache-Control");
        response.setHeader("Access-Control-Allow-Credentials", "true");
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

            throws  IOException {

        //super.doDelete(request, response);
        if (this.isAllowed(request, response)) {
            try {
                addCORSHeader(request, response);

                delete(new BaseServletRequest(request), response);
            } catch (de.kosmos_lab.web.exceptions.ServletException e) {
                ApiResponse r = e.getClass().getAnnotation(ApiResponse.class);
                if ( r != null && r.responseCode() != null ) {
                    response.setStatus(r.responseCode().statusCode());
                }
                else {
                    response.setStatus(STATUS_FAILED);
                }
                response.getWriter().print(e.getMessage());
            } catch (JSONException e) {
                response.setStatus(de.kosmos_lab.web.server.WebServer.STATUS_UNPROCESSABLE);
                response.getWriter().print(e.getMessage());
            } catch (Exception e) {
                response.setStatus(de.kosmos_lab.web.server.WebServer.STATUS_ERROR);
                e.printStackTrace();

            }
        }

    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)

            throws IOException {

        if (this.isAllowed(request, response)) {
            try {
                addCORSHeader(request, response);
                get(new BaseServletRequest(request), response);
            } catch (de.kosmos_lab.web.exceptions.ServletException e) {
                ApiResponse r = e.getClass().getAnnotation(ApiResponse.class);
                if ( r != null && r.responseCode() != null ) {
                    response.setStatus(r.responseCode().statusCode());
                }
                else {
                    response.setStatus(STATUS_FAILED);
                }
                response.getWriter().print(e.getMessage());
            } catch (JSONException e) {
                response.setStatus(de.kosmos_lab.web.server.WebServer.STATUS_UNPROCESSABLE);
                response.getWriter().print(e.getMessage());
            } catch (Exception e) {
                response.setStatus(de.kosmos_lab.web.server.WebServer.STATUS_ERROR);
                e.printStackTrace();

            }
        }

    }

    public void doOptions(HttpServletRequest request, HttpServletResponse response)

            throws IOException {
        addCORSHeader(request, response);

    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)

            throws IOException {

        //logger.info("HITTING doPOST");

        if (this.isAllowed(request, response)) {
            try {
                addCORSHeader(request, response);

                post(new BaseServletRequest(request), response);
            } catch (de.kosmos_lab.web.exceptions.ServletException e) {
                ApiResponse r = e.getClass().getAnnotation(ApiResponse.class);
                if ( r != null && r.responseCode() != null ) {
                    response.setStatus(r.responseCode().statusCode());
                }
                else {
                    response.setStatus(STATUS_FAILED);
                }
                response.getWriter().print(e.getMessage());
            } catch (JSONException e) {
                response.setStatus(de.kosmos_lab.web.server.WebServer.STATUS_UNPROCESSABLE);
                response.getWriter().print(e.getMessage());
            } catch (Exception e) {
                response.setStatus(de.kosmos_lab.web.server.WebServer.STATUS_ERROR);
                e.printStackTrace();

            }

        }

    }

    public void doPut(HttpServletRequest request, HttpServletResponse response)

            throws  IOException {
        //super.doPut(request, response);
        if (this.isAllowed(request, response)) {
            try {
                addCORSHeader(request, response);

                put(new BaseServletRequest(request), response);
            } catch (de.kosmos_lab.web.exceptions.ServletException e) {
                ApiResponse r = e.getClass().getAnnotation(ApiResponse.class);
                if ( r != null && r.responseCode() != null ) {
                    response.setStatus(r.responseCode().statusCode());
                }
                else {
                    response.setStatus(STATUS_FAILED);
                }
                response.getWriter().print(e.getMessage());
            } catch (JSONException e) {
                response.setStatus(de.kosmos_lab.web.server.WebServer.STATUS_UNPROCESSABLE);
                response.getWriter().print(e.getMessage());
            } catch (Exception e) {
                response.setStatus(de.kosmos_lab.web.server.WebServer.STATUS_ERROR);
                e.printStackTrace();

            }
        }

    }

    public void get(BaseServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //logger.info("HITTING GET");

        response.setStatus(de.kosmos_lab.web.server.WebServer.STATUS_METHOD_NOT_ALLOWED);
    }


    protected boolean isAllowed(HttpServletRequest request, HttpServletResponse response) {
        return true;
    }

    public void options(BaseServletRequest request, HttpServletResponse response) {

        response.setStatus(200);
        addCORSHeader(request.getRequest(), response);

    }

    public void post(BaseServletRequest request, HttpServletResponse response) throws ServletException {
        //logger.info("HITTING POST");
        response.setStatus(de.kosmos_lab.web.server.WebServer.STATUS_METHOD_NOT_ALLOWED);
    }

    public void put(BaseServletRequest request, HttpServletResponse response) throws ServletException {
        //logger.info("HITTING PUT");
        response.setStatus(de.kosmos_lab.web.server.WebServer.STATUS_METHOD_NOT_ALLOWED);
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
    public static void sendXML(BaseServletRequest req, HttpServletResponse response, String text) throws IOException {
        response.setHeader("Content-Type", "application/xml");
        response.getWriter().print(text);
    }
}
