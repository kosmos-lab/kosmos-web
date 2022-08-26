package de.kosmos_lab.web.server;

import ch.qos.logback.classic.Level;
import de.kosmos_lab.web.annotations.Operation;
import de.kosmos_lab.web.annotations.Parameter;
import de.kosmos_lab.web.annotations.info.AsyncInfo;
import de.kosmos_lab.web.annotations.media.ArraySchema;
import de.kosmos_lab.web.annotations.media.ObjectSchema;
import de.kosmos_lab.web.annotations.media.Schema;
import de.kosmos_lab.web.annotations.tags.Tag;
import de.kosmos_lab.web.doc.openapi.ApiEndpoint;
import de.kosmos_lab.web.doc.openapi.ApiResponseDescription;
import de.kosmos_lab.web.doc.openapi.Message;
import de.kosmos_lab.web.doc.openapi.WebSocketEndpoint;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.reflections.Reflections;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ResourceBundle;

public class AsyncApiParser extends OpenApiParser {


    private JSONObject json = null;
    private HashSet<Schema> schemas = new HashSet<>();
    private HashSet<ObjectSchema> oschemas = new HashSet<>();
    private HashSet<ArraySchema> aschemas = new HashSet<>();
    private HashSet<Parameter> parameters = new HashSet<>();
    private HashSet<Tag> tags = new HashSet<>();
    private ResourceBundle labels = null;
    private HashMap<String, JSONObject> mResponses = new HashMap<>();
    private JSONObject components;
    private LinkedList<Example> examples = new LinkedList<>();
    private JSONObject responses = new JSONObject();

    public AsyncApiParser(WebServer server) {
        super(server);
    }

    public synchronized JSONObject getJSON() {
        if (json != null) {
            return json;
        }
        json = new JSONObject();

        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.reflections")).setLevel(Level.OFF);
        Reflections r = new Reflections("");

        add("asyncapi", "2.4.0", json);

        JSONObject info = new JSONObject();


        AsyncInfo infoAnnotation = server.getClass().getAnnotation(AsyncInfo.class);
        if (infoAnnotation != null) {
            info = toJSON(infoAnnotation);
        }
        /*
         * needs to be in code to get the version from pom
         */
        try {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new FileReader("pom.xml"));

            info.put("version", model.getVersion());

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        add("info", info, json);

        components = new JSONObject();
        JSONObject securitySchemes = new JSONObject();
        for (Schema schema : server.getClass().getAnnotationsByType(Schema.class)) {
            String name = schema.name();

            if (name.length() > 0) {
                schemas.add(schema);
            }
        }
        for (ObjectSchema schema : server.getClass().getAnnotationsByType(ObjectSchema.class)) {
            String name = schema.componentName();
            if (name.length() > 0) {
                oschemas.add(schema);
            }
        }
        /*for (SecuritySchema schema : server.getClass().getAnnotationsByType(SecuritySchema.class)) {
            String name = schema.componentName();
            if (name.length() == 0) {
                name = schema.name();
            }
            if (name.length() > 0) {
                JSONObject schemaJSON = new JSONObject();
                add("name", schema.name(), schemaJSON);
                add("description", schema.description(), schemaJSON);
                add("scheme", schema.scheme(), schemaJSON);
                add("openIdConnectUrl", schema.openIdConnectUrl(), schemaJSON);
                add("bearerFormat", schema.bearerFormat(), schemaJSON);
                add("type", schema.type().toString(), schemaJSON);
                add("in", schema.in().toString(), schemaJSON);
                add("flows", schema.flows(), schemaJSON);
                add(name, schemaJSON, securitySchemes);
            }
        }*/
        add("securitySchemes", securitySchemes, components);

        for (Class<?> c : r.getTypesAnnotatedWith(ApiResponseDescription.class)) {
            for (ApiResponseDescription ann : c.getAnnotationsByType(ApiResponseDescription.class)) {
                if (ann.name().length() > 0) {
                    responses.put(ann.name(), new JSONObject().put("description", ann.description()));
                }
            }
        }
        JSONObject servers = new JSONObject();


        JSONObject paths = new JSONObject();
        for (Class<? extends WebSocketService> c : server.getWebSocketServices()) {
            ApiEndpoint a = c.getAnnotation(ApiEndpoint.class);
            if (a != null) {
                if (!a.hidden()) {
                    logger.info("found {}", a);
                    for (Method m : c.getDeclaredMethods()) {
                        for (String method : new String[]{"get", "post", "delete"}) {
                            try {

                                //Method m = c.getMethod(method, MyHttpServletRequest.class, HttpServletResponse.class);
                                if (m.getName().equals(method)) {
                                    Operation am = m.getAnnotation(Operation.class);

                                    if (am != null) {
                                        add(a, am, m, paths);
                                    }
                                }

                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }
            add("paths", paths, json);
            for (ObjectSchema schema : c.getAnnotationsByType(ObjectSchema.class)) {
                oschemas.add(schema);
            }
            for (ArraySchema schema : c.getAnnotationsByType(ArraySchema.class)) {
                aschemas.add(schema);
            }
            for (Schema schema : c.getAnnotationsByType(Schema.class)) {
                schemas.add(schema);
            }
            for (Parameter p : c.getAnnotationsByType(Parameter.class)) {
                parameters.add(p);
            }
            for (Tag p : c.getAnnotationsByType(Tag.class)) {
                tags.add(p);
            }
        }
        /*for (Class<?> c : r.getTypesAnnotatedWith(Server.class)) {
            for (Server s : c.getAnnotationsByType(Server.class)) {
                logger.info("found server {}", s);
                JSONObject j = new JSONObject();
                add("description", s.description(), j);
                add("url", s.url(), j);

                servers.put(j);
            }
        }*/
        if (servers.length() == 0) {
            servers.put("default", new JSONObject().put("protocol", "ws").put("url", "http://none").put("description", "current host"));
        }
        add("servers", servers, json);
        add("responses", responses, components);
        JSONObject schemajson = new JSONObject();
        JSONObject parametersjson = new JSONObject();
        JSONArray tagarray = new JSONArray();


        JSONObject messages = new JSONObject();
        JSONObject channels = new JSONObject();
        for (Message message : server.getClass().getAnnotationsByType(Message.class)) {
            logger.info("found {}", message);

            messages.put(message.name(), toJSON(message));

        }
        for (Class<? extends WebSocketService> c : server.getWebSocketServices()) {
            WebSocketEndpoint a = c.getAnnotation(WebSocketEndpoint.class);
            if (a != null) {
                if (!a.hidden()) {
                    logger.info("found {}", a);
                    try {
                        add(a, channels);
                        for (Message message : c.getAnnotationsByType(Message.class)) {
                            messages.put(message.name(), toJSON(message));
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                }
            }
        }
        add("channels", channels, json);
        for (ArraySchema s : aschemas) {
            add(s.name(), toJSON(s), schemajson);
        }
        for (ObjectSchema s : oschemas) {
            add(s.componentName(), toJSON(s), schemajson);
        }
        for (Schema s : schemas) {
            add(s.name(), s, schemajson);
        }
        for (Parameter p : parameters) {
            String name = p.componentName();
            if (name.length() == 0) {
                name = p.name();
            }
            if (name.length() > 0) {
                add(name, toJSON(p), parametersjson);
            }
        }
        for (Tag t : tags) {
            add(t, tagarray);
        }
        add("schemas", schemajson, components);
        add("tags", tagarray, json);
        add("parameters", parametersjson, components);
        add("messages", messages, components);
        add("components", components, json);
        checkExamples();
        return json;
    }

    private JSONObject toJSON(Message message) {
        JSONObject json = new JSONObject();
        add("summary", message.summary(), json);
        add("description", message.description(), json);
        add("name", message.name(), json);
        add("title", message.title(), json);
        //JSONArray payload = new JSONArray();
        if (message.payloadRef().length()>0){
            //payload.put("$ref",message.payloadRef());
            json.put("payload", new JSONObject().put("$ref",message.payloadRef()));
        }



        return json;

    }


    private void add(WebSocketEndpoint endpoint, JSONObject channels) {
        JSONObject json = new JSONObject();
        JSONArray payloads = new JSONArray();
        for (String r : endpoint.subscribeRefs()) {
            payloads.put(new JSONObject().put("$ref", r));
        }
        for (Message message : endpoint.publishMessages()) {
            payloads.put(toJSON(message));
        }
        if (payloads.length() > 0) {
            json.put("publish", new JSONObject().put("message", new JSONObject().put("oneOf", payloads)));
        }
        channels.put(endpoint.path(), json);


    }


}