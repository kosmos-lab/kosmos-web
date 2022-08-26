package common;


import de.kosmos_lab.utils.JSONChecker;
import de.kosmos_lab.utils.KosmosFileUtils;
import de.kosmos_lab.utils.StringFunctions;
import de.kosmos_lab.utils.exceptions.CompareException;
import de.kosmos_lab.web.client.AuthedHTTPClient;
import de.kosmos_lab.web.client.MyTestClient;
import de.kosmos_lab.web.exceptions.LoginFailedException;
import de.kosmos_lab.web.helper.EnvHelper;
import de.kosmos_lab.web.persistence.IUserPersistence;
import de.kosmos_lab.web.persistence.exceptions.NoPersistenceException;
import de.kosmos_lab.web.server.example.ExampleWebServer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

@SuppressFBWarnings("MS_CANNOT_BE_FINAL")
public class CommonBase {
    final public static ConcurrentHashMap<String, JSONObject> jsonCache = new ConcurrentHashMap<>();
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("Test");
    public static String pass;

    public static ExampleWebServer server;
    public static MyTestClient clientAdmin;
    public static MyTestClient clientUser;
    public static MyTestClient clientUser2;
    public static MyTestClient clientUser3;

    public static MyTestClient clientFakeUser;
    public static String baseUrl = "";


    @SuppressFBWarnings({"UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD"})
    @BeforeSuite
    public static void prepare() {
        String TEST_HOST = EnvHelper.getEnv("TEST_HOST");
        logger.info("Test host:{}", TEST_HOST);
        if (TEST_HOST == null || TEST_HOST.length() == 0) {


            File testdb = new File(String.format("config/%s", ExampleWebServer.DEFAULT_STORAGE));
            if (testdb.exists()) {
                //Assert.assertTrue(testdb.delete(),"could not delete old test db!!");
                logger.info("deleting old DB {}", testdb);
                if (!testdb.delete()) {
                    // we don't actually care about the return value, the next loop will get it deleted
                    // this might happen if we start the test again while the previous one is still shutting down
                }

            }
            if (testdb.exists()) {
                for (int i = 0; i < 12; i++) {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {

                        logger.error("could not sleep!", e);
                    }
                    if (testdb.delete()) {
                        break;
                    }

                }
            }
            if (testdb.exists()) {
                Assert.fail("could not delete old test db!!");
            }


            setup();

            try {

                clientAdmin = new MyTestClient(baseUrl, "admin", StringFunctions.generateRandomKey());
                clientUser = new MyTestClient(baseUrl, "user", StringFunctions.generateRandomKey());
                clientUser2 = new MyTestClient(baseUrl, "user2", StringFunctions.generateRandomKey());
                clientUser3 = new MyTestClient(baseUrl, "user3", StringFunctions.generateRandomKey());

                clientFakeUser = new MyTestClient(baseUrl, "fakeuser2", "test");
                //FileUtils.writeToFile(KosmoSController.getFile("users.json", RunMode.TEST), new JSONObject().put("admin", clientAdmin.getPassword()).put("user", clientUser.getPassword()).put("user2", clientUser2.getPassword()).put("user3", clientUser3.getPassword()).toString());

            } catch (Exception e) {
                logger.error("could not create users and write the information to a seperate file!", e);
            }
            addAndTestUser(clientAdmin, 100);
            addAndTestUser(clientUser, 1);
            addAndTestUser(clientUser2, 1);
            addAndTestUser(clientUser3, 1);

            String pass = clientAdmin.getPassword();
            Assert.assertNotNull(pass);


        } else {
            logger.info("using services for kosmos");
            baseUrl = "http://" + TEST_HOST;
            try {
                clientFakeUser = new MyTestClient(baseUrl, "fakeuser2", "test");
            } catch (Exception e) {
                logger.error("could not start fake user!", e);
            }


        }


    }


    public static void setup()  {
        File confDir = new File("config/");
        if (!confDir.exists()) {
            confDir.mkdir();
        }
        File testConf = new File("config/config.json");
        JSONObject config = new JSONObject();
        int port = 18082;
        config.put("port", port);

        baseUrl = "http://localhost:" + port;

        KosmosFileUtils.writeToFile(testConf, config.toString(2));


        try {
            server = new ExampleWebServer(testConf, true);


        } catch (Exception e) {
            logger.error("exception while creating Server",e);
            //throw new RuntimeException(e);
        }







    }

    public static void addAndTestUser(AuthedHTTPClient client, int level) {

        try {
            IUserPersistence userPersistence = server.getPersistence(IUserPersistence.class);
            Assert.assertNotNull(userPersistence);
            userPersistence.addUser(
                    client.getUserName(),
                    client.getPassword(),
                    level);
            userPersistence.login(client.getUserName(), client.getPassword());
        } catch (NoPersistenceException e) {
            Assert.fail("could not find IUserPersistence");

        } catch (LoginFailedException e) {
            Assert.fail(String.format("could not login as user %s", client.getUserName()));
        }


    }

    public static void startIfNeeded() {
        if (server == null) {
            prepare();
        }

    }

    /**
     * this method is used to give the system some time to react to an order via a slow..ish medium. It checks the value
     * every 500ms and returns true as soon as it changed, or false if it never changed and the timeout was reached
     *
     * @param device   the JSONObject to "monitor"
     * @param key      the key to watch
     * @param expected the expected value
     * @param waittime waittime in ms
     *
     * @return
     */
    public static boolean waitForValue(JSONObject device, String key, Object expected, long waittime) {
        long started = System.currentTimeMillis();
        CompareException laste = null;
        while (true) {
            try {
                if (JSONChecker.checkValue(device, key, expected)) {
                    return true;
                }
            } catch (CompareException e) {
                //dont spam the log for now
                laste = e;
            } catch (Exception e) {
                //e.printStackTrace();
                logger.warn("Exception while comparing {} to {}: {}", key, expected, e.getMessage());
            }
            logger.info(device.toString());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                logger.error("could not sleep!", e);
            }
            long delta = System.currentTimeMillis() - started;
            if (delta > waittime) {
                if (laste != null) {
                    logger.warn("could not match {} to {}: {}", key, expected, laste.getMessage());
                }
                return false;
            }
        }
    }

    public static boolean waitForValueJSONHttp(AuthedHTTPClient client, String url, String key, Object expected, long waittime) {
        long started = System.currentTimeMillis();
        while (true) {
            try {
                ContentResponse response = client.getResponse(url, HttpMethod.GET);
                if (response != null) {
                    if (response.getStatus() <= 400) {
                        JSONObject device = new JSONObject(response.getContentAsString());
                        logger.info("waitForValueJSONHttp {}", device.toString(2));
                        if (JSONChecker.checkValue(device, key, expected)) {
                            return true;
                        }
                        if (device.has("attributes")) {
                            if (JSONChecker.checkValue(device.getJSONObject("attributes"), key, expected)) {
                                return true;
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                logger.error("could not wait for JSON Value!", ex);
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                logger.error("could not sleep!", e);
            }
            long delta = System.currentTimeMillis() - started;
            if (delta > waittime) {
                return false;
            }
        }
    }

    @AfterSuite
    public void cleanup(ITestContext context) {
        //just for testing
        /*try {
            Thread.sleep(6000000000l);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }*/


        /*
        checks if the changes survive a reboot of the system.
         */
        String TEST_HOST = EnvHelper.getEnv("TEST_HOST");
        if (TEST_HOST == null) {

            if (context.getFailedTests().size() == 0) {

            }
            if (server != null) {
                server.stop();
            }
        }

    }


}
