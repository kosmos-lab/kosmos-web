package de.kosmos_lab.web.server;


import de.kosmos_lab.utils.FileUtils;
import de.kosmos_lab.utils.StringFunctions;
import de.kosmos_lab.web.annotations.responses.ApiResponse;
import de.kosmos_lab.web.doc.openapi.ApiEndpoint;
import de.kosmos_lab.web.doc.openapi.ApiResponseDescription;
import de.kosmos_lab.web.server.servlets.BaseServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.websocket.server.ServerEndpoint;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.json.JSONException;
import org.json.JSONObject;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public abstract class WebServer {
    public static final Logger logger = LoggerFactory.getLogger("WebServer");
    public static final int STATUS_OK = 200;
    public static final int STATUS_NO_RESPONSE = 204;
    @ApiResponseDescription(name = "NoAccessError", description = "The request was aborted because your user does not have the correct privileges to execute the request.")
    public static final int STATUS_FORBIDDEN = 403;
    @ApiResponseDescription(name = "ValidationFailedErr" + "or", description = "The request was aborted because the payload could not be verified against the schema.  \nSee errormessage for details")

    public static final int STATUS_VALIDATION_FAILED = 400;
    @ApiResponseDescription(name = "DuplicateError", description = "The request was aborted because there was already a resource with that identifier.  \nSee errormessage for details")
    public static final int STATUS_DUPLICATE = 409;
    @ApiResponseDescription(name = "FailedError", description = "The request was aborted.  \nSee errormessage for details ")
    public static final int STATUS_FAILED = 400;
    @ApiResponseDescription(name = "NoAuthError", description = "This endpoint only works with authentication")
    public static final int STATUS_NO_AUTH = 401;
    @ApiResponseDescription(name = "NotFoundError", description = "The searched resource was not found  \nSee errormessage for details")
    public static final int STATUS_NOT_FOUND = 404;
    @ApiResponseDescription(name = "ConflictError", description = "The request was aborted because there was already a resource with that identifier.  \nSee errormessage for details")
    public static final int STATUS_CONFLICT = 409;
    @ApiResponseDescription(name = "UnproccessableError", description = "The request could not be processed, are all required properties/parameters filled?  \nSee errormessage for details")
    public static final int STATUS_UNPROCESSABLE = 422;
    @ApiResponseDescription(name = "MissingValuesError", description = "The request could not be processed, are all required properties/parameters filled?  \nSee errormessage for details")
    public static final int STATUS_MISSING_VALUE = 422;
    @ApiResponseDescription(name = "UnknownError", description = "The server ran into an error while processing the request")
    public static final int STATUS_ERROR = 500;
    @ApiResponseDescription(name = "MethodNotAllowedError", description = "The requested HTTP-method is not valid for this endpoint")
    public static final int STATUS_METHOD_NOT_ALLOWED = 405;


    protected final static int DEFAULT_PORT = 8101;
    protected static final String DEFAULT_CONFIG = "config/config.json";
    protected Server server;
    protected int port;
    protected Set<Class<? extends HttpServlet>> servlets = new HashSet<>();
    protected HashSet<Class<? extends WebSocketService>> wsservices = new HashSet<>();
    protected HashSet<String> wssclasses = new HashSet<>();
    protected HashSet<String> servclasses = new HashSet<>();
    protected HashSet<String> paths = new HashSet<>();
    protected ContextHandlerCollection handlers;
    protected ServletContextHandler context;
    protected HashSet<Class<? extends HttpServlet>> loadedServlets = new HashSet<>();
    protected boolean stopped = false;
    HashSet<Class<? extends Exception>> thrown = new HashSet<>();
    private File configFile;

    public WebServer(File configFile, boolean testing) throws Exception {
        if (configFile == null) {
            configFile = new File(getDefaultConfig());
        }
        this.configFile = configFile;


        prepare();


        start();
    }

    /**
     * dummy constructor
     */
    public WebServer() {
        // this(null, false);
    }

    private static ContextHandler createContextHandler(String contextPath, Handler wrappedHandler) {
        ContextHandler ch = new ContextHandler(contextPath);
        ch.setHandler(wrappedHandler);
        ch.clearAliasChecks();
        ch.setAllowNullPathInfo(true);
        return ch;
    }

    public void createServlet(Class<? extends HttpServlet> servlet) {
        ApiEndpoint api = servlet.getAnnotation(ApiEndpoint.class);
        if (api != null) {
            try {
                if (paths.contains(api.path())) {
                    logger.warn("did find path {} again?", api.path());
                    return;
                }
                if (api.load()) {
                    HttpServlet s;
                    if (api.userLevel() >= 0) {
                        s = servlet.getConstructor(WebServer.class, int.class).newInstance(this, api.userLevel());
                    } else {
                        s = servlet.getConstructor(WebServer.class).newInstance(this);
                    }

                    context.addServlet(new ServletHolder(s), api.path());
                    loadedServlets.add(servlet);
                    paths.add(api.path());
                    logger.info("registered web servlet {} ", s.getClass());
                    return;
                }

            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

    }

    public boolean isStopped() {
        return stopped;
    }

    public void addStaticFile(File f, String name) {
        ServletHolder staticFiles = new ServletHolder("default", new DefaultServlet());

        staticFiles.setInitParameter("resourceBase", "./web/");

        context.addServlet(staticFiles, "/*");

    }

    public void start() throws Exception {

        for (Class<? extends HttpServlet> c : servlets) {
            createServlet(c);
        }

        for (Class<? extends WebSocketService> c : wsservices) {
            createWebSocketService(c);


        }
        JettyWebSocketServletContainerInitializer.configure(context, null);


        handlers.addHandler(context);

        server.setHandler(handlers);
        OpenApiParser.create(this.loadedServlets);

        server.start();
        //server.dump(System.err);

    }

    public void createWebSocketService(Class<? extends WebSocketService> c) {
        ServerEndpoint endpoint = c.getAnnotation(ServerEndpoint.class);
        if (endpoint != null) {
            logger.info("found: WebSocketService: {} endpoint {}", c.getName(), endpoint.value());
            try {
                WebSocketService service = c.getConstructor(WebServer.class).newInstance(this);
                JettyWebSocketServlet websocketServlet = new JettyWebSocketServlet() {
                    @Override
                    protected void configure(JettyWebSocketServletFactory factory) {
                        factory.setIdleTimeout(Duration.ofSeconds(60));
                        factory.setCreator(new WebSocketCreator(service, null));
                    }
                };

                context.addServlet(new ServletHolder(websocketServlet), endpoint.value());

            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }

    public abstract JSONObject getConfig();

    public void findServlets(String[] namespaces, Class<? extends HttpServlet> baseServletClass, Class<? extends WebSocketService> baseSocketClass) {
        for (String n : namespaces) {
            Reflections r = new Reflections(n);
            if (baseServletClass != null) {
                try {
                    for (Class<? extends Exception> c : r.getSubTypesOf(Exception.class)) {
                        if (!thrown.contains(c)) {
                            if (c.getAnnotation(ApiResponse.class) != null) {
                                logger.info("Reflections found Exception: {}", c.getName());
                                thrown.add(c);
                            }

                        }
                    }
                } catch (org.reflections.ReflectionsException ex) {

                }
                try {
                    for (Class<? extends HttpServlet> c : r.getSubTypesOf(baseServletClass)) {
                        if (!servlets.contains(c)) {
                            if (!servclasses.contains(c.getName())) {
                                servclasses.add(c.getName());
                                logger.info("Reflections found HttpServlet: {}", c.getName());
                                servlets.add(c);
                            }
                        }
                    }
                } catch (org.reflections.ReflectionsException ex) {

                }

                for (Class<? extends java.lang.annotation.Annotation> annotation : new Class[]{ApiEndpoint.class}) {
                    try {
                        for (Class c : r.getTypesAnnotatedWith(annotation)) {
                            if (baseServletClass.isAssignableFrom(c)) {
                                if (!servlets.contains(c)) {
                                    if (!servclasses.contains(c.getName())) {
                                        servclasses.add(c.getName());
                                        logger.info("Annotations ({}) found HttpServlet: {}", annotation.getName(), c.getName());
                                        servlets.add(c);
                                    }
                                }
                            }
                        }
                    } catch (org.reflections.ReflectionsException ex) {

                    }
                }
            }
            if (baseSocketClass != null) {
                try {
                    for (Class<? extends WebSocketService> c : r.getSubTypesOf(baseSocketClass)) {
                        if (!wsservices.contains(c)) {
                            if (!wssclasses.contains(c.getName())) {
                                wssclasses.add(c.getName());
                                logger.info("Reflections found WebSocketService: {}", c.getName());
                                wsservices.add(c);

                            }
                        }
                    }
                } catch (org.reflections.ReflectionsException ex) {

                }
                for (Class<? extends java.lang.annotation.Annotation> annotation : new Class[]{ServerEndpoint.class}) {

                    try {

                        for (Class c : r.getTypesAnnotatedWith(annotation)) {
                            if (baseSocketClass.isAssignableFrom(c)) {
                                if (!wsservices.contains(c)) {
                                    if (!wssclasses.contains(c.getName())) {
                                        wssclasses.add(c.getName());
                                        logger.info("Annotations ({}) found WebSocketService: {}", annotation.getName(), c.getName());
                                        wsservices.add(c);
                                    }
                                }
                            }
                        }
                    } catch (org.reflections.ReflectionsException ex) {

                    }
                }
            }


        }


    }

    public JSONObject readConfig() {
        JSONObject config = null;

        if (!configFile.exists()) {
            if (!configFile.getParentFile().exists() && !configFile.getParentFile().mkdirs()) {
                logger.error("COULD NOT CREATE PARENT DIRS:{}", configFile.getParentFile());

                throw new RuntimeException("COULD NOT CREATE PARENT DIRS:" + configFile.getParentFile());
            }

            File distFile = new File(configFile.getAbsolutePath() + ".dist");
            if (distFile.exists()) {
                try {
                    Files.copy(distFile.toPath(), configFile.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        }
        if (configFile.exists()) {
            try {
                config = new JSONObject(FileUtils.readFile(configFile));
            } catch (JSONException | IOException ex) {

            }
        }
        if (config == null) {
            config = new JSONObject();
        }

        int myPort = getDefaultPort();
        try {

            myPort = config.getInt("port"); //we try to read it and format it to an int here, if it fails we except here
        } catch (Exception e) {
            //save the default port to the config
            config.put("port", getDefaultPort());

        } finally {
            //update the final variable here, not the most beautiful way - but rather efficient
            port = myPort;
        }
        String temp = config.optString("pepper", null); //we try to read it

        temp = config.optString("jwt", null);
        if (temp == null) {
            temp = StringFunctions.generateRandomKey();
            config.put("jwt", temp);
        }
        //update the final variable here, not the most beautiful way - but rather efficient

        temp = config.optString("jwt", null); //we try to read it

        if (temp == null) {
            temp = StringFunctions.generateRandomKey();
            config.put("jwt", temp);
        }
        return config;
    }

    public String getDefaultConfig() {
        return DEFAULT_CONFIG;
    }


    public int getDefaultPort() {
        return DEFAULT_PORT;
    }


    public int getPort() {
        return this.port;
    }


    public void prepare() {
        //force openApi to always be included
        this.findServlets(new String[]{"de.kosmos_lab.web.server.servlets.openapi"}, BaseServlet.class, null);
        int maxThreads = 10;
        int minThreads = 2;
        int idleTimeout = 120;

        QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads, idleTimeout);
        JSONObject webserverConfig = getConfig();
        this.server = new Server(threadPool);
        ServerConnector connector = new ServerConnector(server);
        this.port = webserverConfig.getInt("port");
        connector.setPort(port);
        server.addConnector(connector);
        this.handlers = new ContextHandlerCollection();

        //this.context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        ServletContextHandler servletContextHandler = new ServletContextHandler();
        this.context = servletContextHandler;
        context.setContextPath("/");
        //context.setContextPath("/");
        ServletHolder staticFiles = new ServletHolder("default", new DefaultServlet());

        staticFiles.setInitParameter("resourceBase", "./web/");
        staticFiles.setInitParameter("dirAllowed", "false");

        context.addServlet(staticFiles, "/*");
    }


    public void stop() {
        try {
            this.stopped = true;
            this.server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
