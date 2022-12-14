package de.kosmos_lab.web.server;


import de.kosmos_lab.utils.FileUtils;
import de.kosmos_lab.utils.StringFunctions;
import de.kosmos_lab.web.annotations.responses.ApiResponse;
import de.kosmos_lab.web.doc.openapi.ApiEndpoint;
import de.kosmos_lab.web.doc.openapi.ApiResponseDescription;
import de.kosmos_lab.web.doc.openapi.WebSocketEndpoint;
import de.kosmos_lab.web.server.servlets.BaseServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.websocket.server.ServerEndpoint;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    protected Set<Class<? extends WebSocketService>> wsservices = new HashSet<>();
    protected HashSet<String> wssclasses = new HashSet<>();
    protected HashSet<String> servclasses = new HashSet<>();
    protected HashSet<String> paths = new HashSet<>();
    protected HashSet<String> wsPaths = new HashSet<>();

    protected ContextHandlerCollection handlers;
    protected ServletContextHandler context;
    protected HashSet<Class<? extends HttpServlet>> loadedServlets = new HashSet<>();
    protected boolean stopped = false;
    HashSet<Class<? extends Exception>> thrown = new HashSet<>();
    private File configFile;
    private OpenApiParser openApiParser;

    public OpenApiParser getOpenApiParser() {
        return openApiParser;
    }



    public AsyncApiParser getAsyncApiParser() {
        return asyncApiParser;
    }



    private AsyncApiParser asyncApiParser;

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


    public abstract HttpServlet create(Class<? extends HttpServlet> servlet, ApiEndpoint api) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException;

    public abstract WebSocketService create(Class<? extends WebSocketService> servlet, WebSocketEndpoint api) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException;

    public void createServlet(Class<? extends HttpServlet> servlet) {
        ApiEndpoint api = servlet.getAnnotation(ApiEndpoint.class);
        if (api != null) {
            try {
                if (paths.contains(api.path())) {
                    logger.warn("did find path {} again?", api.path());
                    return;
                }
                //if (api.load()) { //we moved this check to findServlets, so we could add classes manually for testing etc
                HttpServlet s = create(servlet, api);


                context.addServlet(new ServletHolder(s), api.path());
                loadedServlets.add(servlet);
                paths.add(api.path());
                logger.info("registered web servlet {} ", s.getClass());
                return;
                //}

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
        //OpenApiParser.create(this.loadedServlets);
        loadResources();
        openApiParser = new OpenApiParser(this);
        asyncApiParser = new AsyncApiParser(this);

        server.start();
        //server.dump(System.err);


    }

    public void createWebSocketService(Class<? extends WebSocketService> c) {
        WebSocketEndpoint endpoint = c.getAnnotation(WebSocketEndpoint.class);
        if (endpoint != null) {
            if (!wsPaths.contains(endpoint.path())) {
                logger.info("found: WebSocketService: {} endpoint {}", c.getName(), endpoint.path());
                try {
                    WebSocketService service = create(c, endpoint);
                    JettyWebSocketServlet websocketServlet = new JettyWebSocketServlet() {
                        @Override
                        protected void configure(JettyWebSocketServletFactory factory) {
                            factory.setIdleTimeout(Duration.ofSeconds(60));
                            factory.setCreator(new WebSocketCreator(service, null));
                        }
                    };

                    context.addServlet(new ServletHolder(websocketServlet), endpoint.path());
                    wsPaths.add(endpoint.path());

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
        ServerEndpoint serverEndpoint = c.getAnnotation(ServerEndpoint.class);
        if (serverEndpoint != null) {
            if (!wsPaths.contains(serverEndpoint.value())) {

                logger.info("found: WebSocketService: {} endpoint {}", c.getName(), serverEndpoint.value());
                try {
                    WebSocketService service = c.getConstructor(WebServer.class).newInstance(this);
                    JettyWebSocketServlet websocketServlet = new JettyWebSocketServlet() {
                        @Override
                        protected void configure(JettyWebSocketServletFactory factory) {
                            factory.setIdleTimeout(Duration.ofSeconds(60));
                            factory.setCreator(new WebSocketCreator(service, null));
                        }
                    };

                    context.addServlet(new ServletHolder(websocketServlet), serverEndpoint.value());
                    wsPaths.add(serverEndpoint.value());

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
                                ApiEndpoint endpoint = c.getAnnotation(ApiEndpoint.class);
                                if (endpoint != null) {


                                    servclasses.add(c.getName());
                                    if (endpoint.load()) {
                                        logger.info("Reflections found HttpServlet: {}", c.getName());

                                        servlets.add(c);
                                    } else {
                                        logger.info("Reflections found HttpServlet, but it is marked with load=false {}", c.getName());
                                    }
                                } else {
                                    logger.warn("Reflections found HttpServlet, but it had no @ApiEndpoint annotation!: {}", c.getName());
                                }
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
                                        if (annotation == ApiEndpoint.class) {
                                            ApiEndpoint endpoint = (ApiEndpoint) c.getAnnotation(ApiEndpoint.class);
                                            if (endpoint != null) { //can technically not happen, but its safer code
                                                if (endpoint.load()) {
                                                    logger.info("Reflections found HttpServlet: {}", c.getName());
                                                    servlets.add(c);
                                                } else {
                                                    logger.info("Reflections found HttpServlet, but it is marked with load=false {}", c.getName());
                                                }
                                            }

                                        } else {
                                            servclasses.add(c.getName());
                                            logger.info("Annotations ({}) found HttpServlet: {}", annotation.getName(), c.getName());
                                            servlets.add(c);
                                        }
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
                try {

                    for (Class c : r.getTypesAnnotatedWith(WebSocketEndpoint.class)) {
                        if (baseSocketClass.isAssignableFrom(c)) {
                            if (!wsservices.contains(c)) {
                                if (!wssclasses.contains(c.getName())) {
                                    WebSocketEndpoint endpoint = (WebSocketEndpoint) c.getAnnotation(WebSocketEndpoint.class);
                                    if (endpoint != null && endpoint.load()) {
                                        wssclasses.add(c.getName());
                                        logger.info("Annotations ({}) found WebSocketService: {}", WebSocketEndpoint.class.getName(), c.getName());
                                        wsservices.add(c);
                                    }
                                }
                            }
                        }
                    }
                } catch (org.reflections.ReflectionsException ex) {

                }

            }


        }


    }

    public JSONObject readConfig() {
        JSONObject config = null;
        if (configFile == null) {
            logger.error("CONFIG FILE IS NULL!");

            throw new RuntimeException("CONFIG FILE IS NULL!");
        }
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
        //OpenApiParser.serverClass = this.getClass();
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
        connector.setHost(webserverConfig.optString("host","0.0.0.0"));
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


    protected void setConfigFile(File configFile) {
        this.configFile = configFile;
    }

    protected Set<Class<? extends WebSocketService>> getWebSocketServices() {
        return this.wsservices;
    }


    public void loadResources() {
        HashSet<ClassLoader> classLoaders = new HashSet<>();

        for (Class<? extends HttpServlet> servlet : servlets) {
            ClassLoader loader = servlet.getClassLoader();
            if (!classLoaders.contains(loader)) {
                classLoaders.add(loader);
                logger.info("found classloader {}",loader.getName());
            }


        }
        for (ClassLoader loader : classLoaders) {
            try {
                for (String f : getResourceFiles(loader, "web")) {
                    logger.info("found resource file {}", f);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private List<String> getResourceFiles(ClassLoader loader, String path) throws IOException {
        List<String> filenames = new ArrayList<>();

        try (
                InputStream in = getResourceAsStream(loader, path);
                BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String resource;

            while ((resource = br.readLine()) != null) {
                filenames.add(resource);
            }
        }

        return filenames;
    }

    private List<String> getResourceFiles(String path) throws IOException {
        List<String> filenames = new ArrayList<>();

        try (
                InputStream in = getResourceAsStream(path);
                BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String resource;

            while ((resource = br.readLine()) != null) {
                filenames.add(resource);
            }
        }

        return filenames;
    }

    private InputStream getResourceAsStream(ClassLoader loader, String resource) {
        final InputStream in
                = loader.getResourceAsStream(resource);

        return in == null ? getClass().getResourceAsStream(resource) : in;
    }

    private InputStream getResourceAsStream(String resource) {
        final InputStream in
                = getContextClassLoader().getResourceAsStream(resource);

        return in == null ? getClass().getResourceAsStream(resource) : in;
    }

    private ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    public String replaceHostName(String cached,String hostname) {
        return cached.replace("${host}",hostname);


    }
}
