package de.kosmos_lab.web.server;

import ch.qos.logback.classic.Level;
import de.kosmos_lab.web.annotations.Parameter;
import de.kosmos_lab.web.annotations.enums.SchemaType;
import de.kosmos_lab.web.annotations.info.AsyncInfo;
import de.kosmos_lab.web.annotations.media.ArraySchema;
import de.kosmos_lab.web.annotations.media.ObjectSchema;
import de.kosmos_lab.web.annotations.media.Schema;
import de.kosmos_lab.web.annotations.servers.AsyncServer;
import de.kosmos_lab.web.annotations.tags.Tag;
import de.kosmos_lab.web.doc.openapi.Channel;
import de.kosmos_lab.web.doc.openapi.Message;
import de.kosmos_lab.web.doc.openapi.WebSocketEndpoint;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.reflections.Reflections;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
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
        /*for (Schema schema : server.getClass().getAnnotationsByType(Schema.class)) {
            String name = schema.name();

            if (name.length() > 0) {
                schemas.add(schema);
            }
        }*/
        /*for (ObjectSchema schema : server.getClass().getAnnotationsByType(ObjectSchema.class)) {
            String name = schema.componentName();
            if (name.length() > 0) {
                oschemas.add(schema);
            }
        }*/
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


        JSONObject servers = new JSONObject();

        HashSet<AsyncServer> serverSet = new HashSet<>();
        JSONObject paths = new JSONObject();
        for (Class<? extends WebSocketService> c : server.getWebSocketServices()) {

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
            for (AsyncServer s : c.getAnnotationsByType(AsyncServer.class)) {
                logger.info("found server {}", s);
                serverSet.add(s);

            }
        }


        for (AsyncServer s : server.getClass().getAnnotationsByType(AsyncServer.class)) {
            logger.info("found server {}", s);
            serverSet.add(s);

        }
        for (Class<?> c : r.getTypesAnnotatedWith(AsyncServer.class)) {
            for (AsyncServer s : c.getAnnotationsByType(AsyncServer.class)) {

                serverSet.add(s);
            }
        }
        for (AsyncServer s : serverSet) {
            JSONObject j = new JSONObject();
            add("description", s.description(), j);
            add("url", s.url(), j);
            add("protocol", s.protocol(), j);
            servers.put(s.name(), j);
        }
        if (servers.length() == 0) {
            servers.put("default", new JSONObject().put("protocol", "wss").put("url", "wss://${host}").put("description", "current host"));
        }
        add("servers", servers, json);
        JSONObject schemajson = new JSONObject();
        JSONObject parametersjson = new JSONObject();


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
        JSONArray tagarray = new JSONArray();
        for (Tag t : tags) {
            add(t, tagarray);
        }
        add("tags", tagarray, json);
        add("schemas", schemajson, components);

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
        add("tags", message.tags(), json);
        JSONArray payloads = new JSONArray();
        JSONArray responses = new JSONArray();
        JSONArray examplesArray = getExamplesArray(message.examples());

        //JSONArray payload = new JSONArray();
        if (message.payloadRefs().length == 1) {
            //payload.put("$ref",message.payloadRef());
            payloads.put(new JSONObject().put("$ref", message.payloadRefs()[0]));
        }
        if (message.payloadRefs().length > 1) {

            for (String r : message.payloadRefs()) {
                payloads.put(new JSONObject().put("$ref", r));

                //json.put("payload", new JSONObject().put("$ref",message.payloadRefs()));
            }

        }
        if (message.payload().properties().length > 0) {
            JSONObject j = toJSON(message.payload());
            add("examples", getExamplesArray(message.payload().examples()), j);
            payloads.put(j);

        }

        if (message.xResponse().properties().length > 0) {

            JSONObject j = toJSON(message.xResponse());
            add("examples", getExamplesArray(message.xResponse().examples()), j);
            responses.put(j);
        }
        if (message.xResponseRefs().length == 1) {
            //payload.put("$ref",message.payloadRef());
            responses.put(new JSONObject().put("$ref", message.xResponseRefs()[0]));
        }
        if (message.xResponseRefs().length > 1) {
            for (String r : message.xResponseRefs()) {
                responses.put(new JSONObject().put("$ref", r));
            }
        }
        if (message.payloadSchema().type() != SchemaType.DEFAULT) {
            JSONObject j = toJSON(message.payloadSchema());
            add("examples", getExamplesArray(message.payloadSchema().examples()), j);

            payloads.put(j);
        }
        if (message.xResponseSchema().type() != SchemaType.DEFAULT) {
            JSONObject j = toJSON(message.xResponseSchema());
            add("examples", getExamplesArray(message.xResponseSchema().examples()), j);

            responses.put(j);
        }
        if (payloads.length() == 1) {
            json.put("payload", payloads.get(0));
        }
        if (payloads.length() > 1) {
            json.put("payload", new JSONObject().put("oneOf", payloads));
        }
        if (responses.length() == 1) {
            json.put("x-response", responses.get(0));
        }
        if (responses.length() > 1) {
            json.put("x-response", new JSONObject().put("oneOf", responses));
        }

        return json;

    }

    private void add(Channel channel, WebSocketEndpoint endpoint, JSONObject channels) {
        JSONObject json = new JSONObject();
        JSONArray subscriptions = new JSONArray();
        JSONArray tagarray = new JSONArray();
        for (Tag t : channel.tags()) {
            add(t, tagarray);
            //tagarray.put(t.name());
        }
        for (String r : channel.subscribeRefs()) {
            subscriptions.put(new JSONObject().put("$ref", r));
        }
        for (Message message : channel.subscribeMessages()) {
            subscriptions.put(toJSON(message));
        }
        if (subscriptions.length() > 0) {
            JSONObject j = new JSONObject().put("message", new JSONObject().put("oneOf", subscriptions));
            if (tagarray.length() > 0) {
                j.put("tags", tagarray);
            }
            json.put("subscribe", j);
        }
        JSONArray publishes = new JSONArray();
        for (String r : channel.publishRefs()) {
            publishes.put(new JSONObject().put("$ref", r));
        }
        for (Message message : channel.publishMessages()) {
            publishes.put(toJSON(message));
        }
        if (publishes.length() > 0) {
            JSONObject j = new JSONObject().put("message", new JSONObject().put("oneOf", publishes));
            if (tagarray.length() > 0) {
                j.put("tags", tagarray);
            }
            json.put("publish", j);
        }

        //add("tags", tagarray, json);
        String path = channel.path();
        if (path.length() == 0) {
            path = endpoint.path();
        }

        if (channel.path().length() > 0) {
            JSONObject bindings = new JSONObject();
            if (endpoint.enableWS()) {

                JSONObject j = new JSONObject()
                        .put("path", endpoint.path());
                if (channel.needsMessage()) {
                    j.put("message", String.format("%s:{message}", channel.path()));
                }
                if (channel.userLevel() > -1) {
                    j.put("userLevel", channel.userLevel());
                    j.put("authRequired", true);
                }
                bindings.put("ws", j
                );
            }
            if (endpoint.enableMQTT()) {
                JSONObject j = new JSONObject()
                        .put("topic", endpoint.path());
                if (channel.needsMessage()) {
                    j.put("message", "{message}");
                }
                if (channel.userLevel() > -1) {
                    j.put("userLevel", channel.userLevel());
                    j.put("authRequired", true);
                }
                bindings.put("mqtt",
                        j);
            }
            json.put("bindings", bindings);
        } else {
            if (channel.path().length() > 0) {
                JSONObject bindings = new JSONObject();
                if (endpoint.enableWS()) {
                    JSONObject j = new JSONObject()
                            .put("path", endpoint.path());

                    if (channel.needsMessage()) {
                        j.put("message", "{message}");
                    }
                    bindings.put("ws", j
                    );
                }
                if (endpoint.enableMQTT()) {
                    JSONObject j = new JSONObject()
                            .put("topic", endpoint.path());
                    if (channel.needsMessage()) {
                        j.put("message", "{message}");
                    }
                    bindings.put("mqtt",j);


                }
                json.put("bindings", bindings);
            }
        }
        if (channel.parameters().length > 0) {
            JSONObject parametersjson = new JSONObject();
            //add("parameters",channel.parameters(),json);
            for (Parameter p : channel.parameters()) {
                String name = p.componentName();
                if (name.length() == 0) {
                    name = p.name();
                }
                if (name.length() > 0) {
                    JSONObject j = toJSON(p);
                    //we need to remove some keys that would be invalid here
                    j.remove("in");
                    j.remove("name");
                    j.remove("required");
                    add(name, j, parametersjson);
                }
            }
            json.put("parameters", parametersjson);
        }
        add("description", channel.description(), json);
        channels.put(path, json);
    }

    private void add(WebSocketEndpoint endpoint, JSONObject channels) {
        for (Channel channel : endpoint.channels()) {
            add(channel, endpoint, channels);

        }


    }


}
