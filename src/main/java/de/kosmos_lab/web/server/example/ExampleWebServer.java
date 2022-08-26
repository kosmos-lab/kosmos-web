package de.kosmos_lab.web.server.example;

import de.kosmos_lab.utils.HashFunctions;
import de.kosmos_lab.utils.StringFunctions;
import de.kosmos_lab.web.annotations.enums.SecurityIn;
import de.kosmos_lab.web.annotations.enums.SecurityType;
import de.kosmos_lab.web.annotations.info.AsyncInfo;
import de.kosmos_lab.web.annotations.info.Contact;
import de.kosmos_lab.web.annotations.info.Info;
import de.kosmos_lab.web.annotations.info.License;
import de.kosmos_lab.web.annotations.security.SecuritySchema;
import de.kosmos_lab.web.doc.openapi.ApiEndpoint;
import de.kosmos_lab.web.doc.openapi.WebSocketEndpoint;
import de.kosmos_lab.web.persistence.ControllerWithPersistence;
import de.kosmos_lab.web.persistence.IPersistence;
import de.kosmos_lab.web.persistence.ISesssionPersistence;
import de.kosmos_lab.web.persistence.JSONPersistence;
import de.kosmos_lab.web.persistence.exceptions.NoPersistenceException;
import de.kosmos_lab.web.server.JWT;
import de.kosmos_lab.web.server.WebServer;
import de.kosmos_lab.web.server.WebSocketService;
import de.kosmos_lab.web.server.example.servlets.session.KillServlet;
import de.kosmos_lab.web.server.example.servlets.session.MyServlet;
import de.kosmos_lab.web.server.example.servlets.user.AdminViewServlet;
import de.kosmos_lab.web.server.example.servlets.user.LoginServlet;
import jakarta.servlet.http.HttpServlet;
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

@SecuritySchema(
        componentName = "bearerAuth",

        description = "contains a JSON Web Tokens (JWT) obtainable from #post-/user/login",
        type = SecurityType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
@SecuritySchema(
        componentName = "basicAuth",
        description = "basic auth is also allowed for all requests",
        type = SecurityType.HTTP,
        bearerFormat = "JWT",
        scheme = "basic"
)
@SecuritySchema(
        componentName = "secret",
        name = "token",
        description = "Contains a secret known to both parties",
        type = SecurityType.APIKEY,
        in = SecurityIn.QUERY
)
@Info(
        description = "# WebExample Synchron HTTP API \n" +
                "### [Asyncron WS/MQTT Documentation](async.html) \n" +
                "This is an example openApi specification \n" +
                "Please make sure you are logged in if you want to try to execute any request to the server.\n" +
                "You can simply login with the form injected to the top of the page.\n" +
                "(Almost) all POST requests with simple a datatype for parameters can be used either with parameters in query or a JSONObject in the request body. Exceptions are more complex datatypes like JSONObjects themselves (for example for /schema/add).",
        title = "Example OpenAPI",
        version = "filled-by-code",
        license = @License(name = "Apache 2.0", url = "http://www.apache.org/licenses/LICENSE-2.0.html"),
        contact = @Contact(name = "Jan Janssen", email = "Jan.Janssen@dfki.de")
)
@AsyncInfo(
        description = "# WebExample ASynchron HTTP API",
        title = "Example OpenAPI",
        version = "filled-by-code",
        license = @License(name = "Apache 2.0", url = "http://www.apache.org/licenses/LICENSE-2.0.html"),
        contact = @Contact(name = "Jan Janssen", email = "Jan.Janssen@dfki.de")
)

public class ExampleWebServer extends WebServer implements ControllerWithPersistence {

    public static final String DEFAULT_STORAGE = "storage.json";
    private static final long JWTLIFETIME = 3600000;
    private final ConcurrentHashMap<Class<?>, IPersistence> persistences;
    private final ConcurrentHashMap<UUID, Object> usedUUID = new ConcurrentHashMap<>();
    private String pepper;
    private JWT jwt;


    public ExampleWebServer(File configFile, boolean testing) throws Exception {
        super();
        this.setConfigFile(configFile);
        this.persistences = new ConcurrentHashMap<>();
        prepare();
        start();


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
            String relativepath = "config";
            storage_file = relativepath + "/" + getDefaultStorage();
            persistence.put("file", storage_file);
        }

        String clazz = persistence.optString("class");
        Class c = null;
        try {
            if (clazz != null && clazz.length() > 0) {
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

    public void init(JSONObject config) {

    }

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
        super.prepare();
        JSONObject config = readConfig();
        createPersistence(config);
        init(config);
    }

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

    public HttpServlet create(Class<? extends HttpServlet> servlet, ApiEndpoint api) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if (api.userLevel() >= 0) {
            try {
                return servlet.getConstructor(ExampleWebServer.class, int.class).newInstance(this, api.userLevel());

            } catch (NoSuchMethodException e) {

                return servlet.getConstructor(WebServer.class, int.class).newInstance(this, api.userLevel());


            }
        } else {
            try {
                return servlet.getConstructor(ExampleWebServer.class).newInstance(this);

            } catch (NoSuchMethodException e) {

                return servlet.getConstructor(WebServer.class).newInstance(this);


            }
        }
    }

    public WebSocketService create(Class<? extends WebSocketService> service, WebSocketEndpoint api) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        try {
            return service.getConstructor(ExampleWebServer.class).newInstance(this);

        } catch (NoSuchMethodException e) {

            return service.getConstructor(WebServer.class).newInstance(this);


        }
        
    }


    public void findServlets(String[] namespaces, Class<? extends HttpServlet> baseServletClass, Class<? extends WebSocketService> baseSocketClass) {
        super.findServlets(namespaces, baseServletClass, baseSocketClass);
        //manually add those back to the wanted servlets
        this.servlets.add(LoginServlet.class);
        this.servlets.add(AdminViewServlet.class);
        this.servlets.add(KillServlet.class);
        this.servlets.add(MyServlet.class);
        this.wsservices.add(MyWebSocketService.class);
    }


}
