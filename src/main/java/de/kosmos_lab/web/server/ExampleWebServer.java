package de.kosmos_lab.web.server;

import de.kosmos_lab.utils.FileUtils;
import de.kosmos_lab.utils.HashFunctions;
import de.kosmos_lab.utils.StringFunctions;
import de.kosmos_lab.web.persistence.ControllerWithPersistence;
import de.kosmos_lab.web.persistence.IPersistence;
import de.kosmos_lab.web.persistence.ISesssionPersistence;
import de.kosmos_lab.web.persistence.JSONPersistence;
import de.kosmos_lab.web.persistence.exceptions.NoPersistenceException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.json.JSONObject;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ExampleWebServer extends WebServer implements ControllerWithPersistence {

    private static final long JWTLIFETIME = 3600000;
    private static final String DEFAULT_STORAGE = "storage.json";
    private final ConcurrentHashMap<Class<?>, IPersistence> persistences = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Object> usedUUID = new ConcurrentHashMap<>();
    private final File configFile;
    private String pepper;
    private JWT jwt;


    public ExampleWebServer(File configFile, boolean testing) throws Exception {
        prepare();
        if (configFile == null) {
            configFile = new File(getDefaultConfig());
        }
        this.configFile = configFile;
        JSONObject config = readConfig();


        createPersistence(config);


        init(config);
        FileUtils.writeToFile(configFile, config.toString());


        int maxThreads = 10;
        int minThreads = 2;
        int idleTimeout = 120;

        QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads, idleTimeout);

        this.server = new Server(threadPool);
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        ServletHolder staticFiles = new ServletHolder("default", new DefaultServlet());

        staticFiles.setInitParameter("resourceBase", "./web/");

        context.addServlet(staticFiles, "/*");


        //dont judge :p
        //reflections magic to work around the fact that embedded jetty does not want to read the annotations by itself..
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(Arrays.asList(ClasspathHelper.forClass(ExampleBaseServlet.class))));
        for (Class<? extends ExampleBaseServlet> c : reflections.getSubTypesOf(ExampleBaseServlet.class)) {
            jakarta.servlet.annotation.WebServlet f = c.getAnnotation(jakarta.servlet.annotation.WebServlet.class);
            if (f != null) {
                try {
                    ExampleBaseServlet s = c.getConstructor(ExampleWebServer.class).newInstance(this);
                    for (String url : f.urlPatterns()) {
                        context.addServlet(new ServletHolder(s), url);
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
        server.setHandler(context);
        try {
            server.start();
            //server.dump(System.err);
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }

    public ExampleWebServer() throws Exception {
        this(null, false);
    }

    @Override
    public JSONObject getConfig() {
        return readConfig();
    }

    public JSONObject readConfig() {
        JSONObject config = super.readConfig();


        String temp = config.optString("pepper", null); //we try to read it
        this.pepper = temp;
        if (temp == null) {
            temp = StringFunctions.generateRandomKey();
            config.put("pepper", temp);
        }
        temp = config.optString("jwt", null);
        if (temp == null) {
            temp = StringFunctions.generateRandomKey();
            config.put("jwt", temp);
        }
        //update the final variable here, not the most beautiful way - but rather efficient
        this.jwt = new JWT(temp, JWTLIFETIME);

        temp = config.optString("jwt", null); //we try to read it

        if (temp == null) {
            temp = StringFunctions.generateRandomKey();
            config.put("jwt", temp);
        }
        return config;
    }

    @Override
    public void addJWT(String jwtid, JSONObject o) throws NoPersistenceException {
        this.getPersistence(ISesssionPersistence.class).addJWT(jwtid);
    }

    @Override
    public void addPersistence(IPersistence perstistence, Class<?> clazz) {
        this.persistences.put(clazz, perstistence);
    }

    @Override
    public void addPersistence(IPersistence p) {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(Arrays.asList(ClasspathHelper.forClass(IPersistence.class))));

        Set<Class<? extends IPersistence>> subTypes = reflections.getSubTypesOf(IPersistence.class);
        for (Class c : subTypes) {
            if (c.isInterface()) {
                logger.info("Found IPersistence for in {} {}", p.getClass().getCanonicalName(), c.getCanonicalName());
                this.addPersistence(p, c);
            }
        }
    }

    public void addUUID(UUID uuid, Object object) {
        this.usedUUID.put(uuid, object);
    }

    @Override
    public IPersistence createPersistence(JSONObject config) {

        JSONObject persistence = config.optJSONObject("persistence");
        if (persistence == null) {
            persistence = new JSONObject();
            config.put("persistence", persistence);
        }


        String storage_file = persistence.optString("file", null); //we try to read it

        if (storage_file == null) {
            String relativepath = configFile.getParentFile().toString();
            storage_file = relativepath + "/" + getDefaultStorage();
            persistence.put("file", storage_file);
        }

        String clazz = persistence.optString("class");
        Class c = null;
        try {
            if (clazz != null) {
                c = Class.forName(clazz);
            }
        } catch (ClassNotFoundException e) {
            logger.error("COULD NOT FIND PERSISTENCE CLASS: {}", clazz);
            e.printStackTrace();
        }
        if (c == null || !JSONPersistence.class.isAssignableFrom(c)) {
            c = getDefaultPersistenceClass();
            persistence.put("class", c.getCanonicalName());
        }


        try {
            IPersistence p = (IPersistence) c.getConstructor(ControllerWithPersistence.class, File.class).newInstance(this, new File(storage_file));
            this.addPersistence(p);
            return p;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public UUID generateUUID() {
        UUID uuid = UUID.randomUUID();
        while (getByUUID(uuid) != null) {
            uuid = UUID.randomUUID();
        }
        return uuid;

    }

    @Override
    public Object getByUUID(UUID uuid) {
        return usedUUID.get(uuid);

    }


    @Override
    public Class getDefaultPersistenceClass() {
        return JSONPersistence.class;
    }


    @Override
    public JWT getJwt() {
        return this.jwt;
    }


    @Override
    public <T> T getPersistence(Class<T> clazz) throws NoPersistenceException {
        IPersistence p = this.persistences.get(clazz);
        if (p != null) {
            return clazz.cast(p);
        } else {
            throw new NoPersistenceException();
        }
    }


    @Override
    public String hashPepper(String input) {
        return HashFunctions.getSaltedHash(input, this.pepper);
    }

    @Override
    public String hashSaltPepper(String input, String salt) {
        return HashFunctions.getSaltedAndPepperdHash(input, salt, this.pepper);
    }

    public abstract void init(JSONObject config);

    @Override
    public boolean isKnownJWTID(String jwtid) {
        try {
            JSONObject jwt = getPersistence(ISesssionPersistence.class).getJWT(jwtid);
            if (jwt != null) {
                return true;
            }
        } catch (NoPersistenceException ex) {
            ex.printStackTrace();
        }
        return false;

    }

    public void prepare() {

    }

    public abstract void sanitizeConfig(JSONObject jsonConfig);

    public void stop() {
        try {
            this.server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getDefaultStorage() {
        return DEFAULT_STORAGE;
    }

}
