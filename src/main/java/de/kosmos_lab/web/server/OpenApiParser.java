package de.kosmos_lab.web.server;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import de.kosmos_lab.web.annotations.ExternalDocumentation;
import de.kosmos_lab.web.annotations.Operation;
import de.kosmos_lab.web.annotations.Parameter;
import de.kosmos_lab.web.annotations.enums.Explode;
import de.kosmos_lab.web.annotations.enums.SchemaType;
import de.kosmos_lab.web.annotations.headers.Header;
import de.kosmos_lab.web.annotations.info.Contact;
import de.kosmos_lab.web.annotations.info.Info;
import de.kosmos_lab.web.annotations.info.License;
import de.kosmos_lab.web.annotations.media.ArraySchema;
import de.kosmos_lab.web.annotations.media.Content;
import de.kosmos_lab.web.annotations.media.DiscriminatorMapping;
import de.kosmos_lab.web.annotations.media.ExampleObject;
import de.kosmos_lab.web.annotations.media.ObjectSchema;
import de.kosmos_lab.web.annotations.media.Schema;
import de.kosmos_lab.web.annotations.media.SchemaProperty;
import de.kosmos_lab.web.annotations.responses.ApiResponse;
import de.kosmos_lab.web.annotations.security.OAuthFlow;
import de.kosmos_lab.web.annotations.security.OAuthFlows;
import de.kosmos_lab.web.annotations.security.Scope;
import de.kosmos_lab.web.annotations.security.SecurityRequirement;
import de.kosmos_lab.web.annotations.security.SecuritySchema;
import de.kosmos_lab.web.annotations.servers.Server;
import de.kosmos_lab.web.annotations.tags.Tag;
import de.kosmos_lab.web.data.IUser;
import de.kosmos_lab.web.data.Tuple;
import de.kosmos_lab.web.doc.openapi.ApiEndpoint;
import de.kosmos_lab.web.doc.openapi.ApiResponseDescription;
import de.kosmos_lab.web.doc.openapi.ResponseCode;
import jakarta.servlet.http.HttpServlet;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.reflections.Reflections;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class OpenApiParser {
    public static final HashSet<String> missingFromResource = new HashSet<>();
    protected static final org.slf4j.Logger logger = LoggerFactory.getLogger("OpenApiParser");
    private static final String formatRegex = "%\\{(?<key>.*?)\\}";
    private static final Pattern formatPattern = Pattern.compile(".*?(" + formatRegex + ").*+");
    public static Class<? extends WebServer> serverClass = WebServer.class;
    static HashSet<Class<? extends HttpServlet>> servlets = new HashSet<>();
    private static JSONObject json = null;
    private static HashSet<Schema> schemas = new HashSet<>();
    private static HashSet<ObjectSchema> oschemas = new HashSet<>();
    private static HashSet<ArraySchema> aschemas = new HashSet<>();
    private static HashSet<Parameter> parameters = new HashSet<>();
    private static HashSet<Tag> tags = new HashSet<>();
    private static ResourceBundle labels = null;
    private static HashMap<String, JSONObject> mResponses = new HashMap<>();
    private static JSONObject components;
    private static LinkedList<Example> examples = new LinkedList<>();
    private static JSONObject responses = new JSONObject();

    public static String asYaml(String jsonString) throws JsonProcessingException, IOException {
        // parse JSON
        JsonNode jsonNodeTree = new ObjectMapper().readTree(jsonString);
        // save it as YAML
        String jsonAsYaml = new YAMLMapper().writeValueAsString(jsonNodeTree);
        return jsonAsYaml;
    }


    public static String getYAML() throws IOException {
        return asYaml(getJSON().toString());
    }

    public static void create(HashSet<Class<? extends HttpServlet>> pservlets) {
        if (servlets == null) {
            servlets = pservlets;
        } else {
            for (Class<? extends HttpServlet> servlet : pservlets) {
                servlets.add(servlet);
            }
        }

    }

    public static JSONObject toJSON(Info info) {
        JSONObject jinfo = new JSONObject();
        add("description", info.description(), jinfo);
        add("contact", info.contact(), jinfo);
        add("termsOfService", info.termsOfService(), jinfo);
        add("title", info.title(), jinfo);
        add("version", info.version(), jinfo);
        add("license", info.license(), jinfo);

        return jinfo;


    }

    public static void add(String tag, License license, JSONObject json) {
        JSONObject licenseJSON = new JSONObject();
        add("url", license.url(), licenseJSON);
        add("name", license.name(), licenseJSON);
        add(tag, licenseJSON, json);
    }

    public static void add(String tag, Contact contact, JSONObject json) {
        JSONObject contactJSON = new JSONObject();
        add("email", contact.email(), contactJSON);
        add("url", contact.url(), contactJSON);
        add("name", contact.name(), contactJSON);
        add(tag, contactJSON, json);
    }

    public static JSONObject getJSON() {
        if (json != null) {
            return json;
        }
        json = new JSONObject();

        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.reflections")).setLevel(Level.OFF);
        Reflections r = new Reflections("");

        add("openapi", "3.0.0", json);

        JSONObject info = new JSONObject();


        Info infoAnnotation = serverClass.getAnnotation(Info.class);
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
        for (Schema schema : serverClass.getAnnotationsByType(Schema.class)) {
            String name = schema.name();

            if (name.length() > 0) {
                schemas.add(schema);
            }
        }
        for (ObjectSchema schema : serverClass.getAnnotationsByType(ObjectSchema.class)) {
            String name = schema.componentName();
            if (name.length() > 0) {
                oschemas.add(schema);
            }
        }
        for (SecuritySchema schema : serverClass.getAnnotationsByType(SecuritySchema.class)) {
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
        }
        add("securitySchemes", securitySchemes, components);
        responses = new JSONObject();
//        responses.put("DeviceNotFoundError", new JSONObject().put("description", "Could not find the given Device"));
        /*for (Field f : KosmoSServlet.class.getDeclaredFields()) {
            ApiResponseDescription ann = f.getAnnotation(ApiResponseDescription.class);
            if (ann != null) {
                responses.put(ann.name(), new JSONObject().put("description", ann.description()));
            }
        }*/
        for (Class<?> c : r.getTypesAnnotatedWith(ApiResponse.class)) {
            for (ApiResponse ann : c.getAnnotationsByType(ApiResponse.class)) {
                /*if (c.isAssignableFrom(Exception.class)) {
                    responses.put(c.getSimpleName().replace("Exception", "Error"), toJSON(ann));
                }*/
                if (Exception.class.isAssignableFrom(c)) {
                    responses.put(c.getSimpleName().replace("Exception", "Error"), toJSON(ann));
                }
                if (ann.componentName().length() > 0) {
                    responses.put(ann.componentName(), toJSON(ann));
                }

            }
        }
        for (Class<?> c : r.getTypesAnnotatedWith(ApiResponseDescription.class)) {
            for (ApiResponseDescription ann : c.getAnnotationsByType(ApiResponseDescription.class)) {
                if (ann.name().length() > 0) {
                    responses.put(ann.name(), new JSONObject().put("description", ann.description()));
                }
            }
        }
        JSONArray servers = new JSONArray();
        for (Server s : serverClass.getAnnotationsByType(Server.class)) {
            logger.info("found server {}", s);
            JSONObject j = new JSONObject();
            add("description", s.description(), j);
            add("url", s.url(), j);
            servers.put(j);
        }

        for (Class<?> c : r.getTypesAnnotatedWith(Server.class)) {
            for (Server s : c.getAnnotationsByType(Server.class)) {
                logger.info("found server {}", s);
                JSONObject j = new JSONObject();
                add("description", s.description(), j);
                add("url", s.url(), j);
                servers.put(j);
            }
        }
        add("servers", servers, json);
        add("responses", responses, components);
        JSONObject schemajson = new JSONObject();
        JSONObject parametersjson = new JSONObject();
        JSONArray tagarray = new JSONArray();


        JSONObject paths = new JSONObject();
        for (Class<? extends HttpServlet> c : servlets) {
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
                                        OpenApiParser.add(a, am, m, paths);
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
        for (ArraySchema s : aschemas) {
            add(s.name(), toJSON(s), schemajson);
        }
        for (ObjectSchema s : oschemas) {
            add(s.componentName(), toJSON(s.properties()), schemajson);
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
        add("components", components, json);
        checkExamples();
        return json;
    }

    /**
     * used to check if the examples are all correct
     *
     * @return
     */
    public static Tuple checkExamples() {
        int failed = 0;
        int checked = 0;
        for (Example ex : examples) {
            if (!checkExample(ex.value, createJSONSchemaFromSchema(ex.schema), ex.type)) {
                logger.error("Example failed! {} {}", ex.schema, ex.value);
                failed++;
            } else {
                logger.info("Example did pass! {} {}", ex.schema, ex.value);
            }
            checked++;
        }
        if (failed > 0) {
            logger.error("Had {} Examples that failed", failed);

        }
        return new Tuple(checked, failed);

    }

    public static void add(String tag, OAuthFlows flows, JSONObject json) {
        JSONObject flowsJSON = new JSONObject();
        add("implicit", flows.implicit(), flowsJSON);
        add("password", flows.password(), flowsJSON);
        add("clientCredentials", flows.clientCredentials(), flowsJSON);
        add("authorizationCode", flows.authorizationCode(), flowsJSON);
    }

    public static void add(String tag, OAuthFlow flow, JSONObject json) {
        JSONObject flowJSON = new JSONObject();
        add("authorizationUrl", flow.authorizationUrl(), flowJSON);
        add("tokenUrl", flow.tokenUrl(), flowJSON);
        add("refreshUrl", flow.refreshUrl(), flowJSON);
        JSONObject scopesJSON = new JSONObject();
        for (Scope scope : flow.scopes()) {
            scopesJSON.put(scope.name(), scope.value());
        }
        add("scopes", scopesJSON, flowJSON);
        add(tag, flowJSON, json);

    }

    public static void add(Tag tag, JSONArray array) {
        JSONObject j = new JSONObject();
        add("name", tag.name(), j);
        add("description", tag.description(), j);
        add("externalDocs", tag.externalDocs(), j);
        if (j.length() > 0) {
            array.put(j);
        }

    }

    public static void add(ApiEndpoint endpoint, Operation operation, Method method, JSONObject paths) {

        JSONObject epp = paths.optJSONObject(endpoint.path());
        if (epp == null) {
            epp = new JSONObject();
            paths.put(endpoint.path(), epp);
        }
        JSONObject j = new JSONObject();
        epp.put(method.getName(), j);
        add("tags", operation.tags(), j);

        if (operation.description().length() > 0) {
            add("description", operation.description(), j);
        } else {
            add("description", operation.summary(), j);

        }
        add("summary", operation.summary(), j);
        String operationId = operation.operationId();
        if (operationId.length() == 0) {
            operationId = method.getName() + "-" + endpoint.path().replace("/", "");
        }
        add("operationId", operationId, j);
        JSONArray security = new JSONArray();
        if (operation.security().length == 0) {

            if (endpoint.userLevel() >= IUser.LEVEL_ADMIN) {

                security.put(new JSONObject().put("bearerAuth", new JSONArray().put("admin")));
                security.put(new JSONObject().put("basicAuth", new JSONArray().put("admin")));

            } else if (endpoint.userLevel() >= 0) {
                security.put(new JSONObject().put("bearerAuth", new JSONArray().put("user")));
                security.put(new JSONObject().put("basicAuth", new JSONArray().put("user")));

            }

        } else {
            for (SecurityRequirement s : operation.security()) {
                JSONArray scopes = new JSONArray();
                for (String scope : s.scopes()) {
                    scopes.put(scope);
                }

                security.put(new JSONObject().put(s.name(), scopes));

            }
        }
        add("security", security, j);
        JSONObject resp = new JSONObject();
        if (operation.responses().length > 0) {

            for (ApiResponse response : operation.responses()) {
                JSONObject rjson = toJSON(response);
                ResponseCode sc = response.responseCode();
                if (sc.statusCode() != -1) {
                    resp.put(String.valueOf(sc.statusCode()), rjson);
                } else {
                    resp.put(String.valueOf(sc.status()), rjson);
                }
            }


        }
        try {
            for (Class<?> ex : method.getExceptionTypes()) {
                String n = ex.getSimpleName().replace("Exception", "Error");
                ApiResponse apiresp = ex.getAnnotation(ApiResponse.class);
                if (apiresp != null) {
                    //only add if we have a valid description for the Exception
                    if (responses.has(n)) {
                        //JSONObject rjson = toJSON(apiresp);
                        ResponseCode sc = apiresp.responseCode();
                        //add the response, but only if there is not already a description for that given exception
                        if (sc.statusCode() != -1) {
                            String ssc = String.valueOf(sc.statusCode());
                            if (!resp.has(ssc)) {
                                resp.put(ssc, new JSONObject().put("$ref", "#/components/responses/" + n));
                            }
                        } else {
                            String ssc = String.valueOf(sc.status());
                            if (!resp.has(ssc)) {
                                resp.put(ssc, new JSONObject().put("$ref", "#/components/responses/" + n));
                            }
                        }
                    }
                }


            }
        } catch (Exception e) {

        }
        if (endpoint.userLevel() != -1) {
            if (!resp.has(String.valueOf(WebServer.STATUS_NO_AUTH))) {
                resp.put(String.valueOf(WebServer.STATUS_NO_AUTH), new JSONObject().put("$ref", "#/components/responses/NoAuthError"));
            }
            /*if (!resp.has(String.valueOf(KosmoSServlet.STATUS_FORBIDDEN))) {
                resp.put(String.valueOf(KosmoSServlet.STATUS_FORBIDDEN), new JSONObject().put("$ref", "#/components/responses/NoAuthError"));
            }*/
        }
        j.put("responses", resp);
        JSONArray params = new JSONArray();


        for (Parameter p : operation.parameters()) {
            add(p, params);
        }


        if (params.length() > 0) {
            j.put("parameters", params);
        }
        JSONObject requestBody = new JSONObject();
        add("content", operation.requestBody().content(), requestBody);
        if (requestBody.length() > 0) {
            j.put("requestBody", requestBody);
        }


    }

    private static JSONObject toJSON(ApiResponse response) {
        JSONObject rjson = new JSONObject();
        add("description", response.description(), rjson);
        if (response.ref().length() > 0) {
            //add("$ref", response.ref(), rjson);
            JSONObject o = mResponses.get(response.ref());
            if (o != null) {
                return o;
            }
            add("$ref", response.ref(), rjson);
        }


        add("content", response.content(), rjson);
        add("headers", response.headers(), rjson);
        return rjson;
    }

    public static JSONObject toJSON(ArraySchema arraySchema) {
        //logger.info("called toJSON(ArraySchema) with {}", arraySchema);
        //if (arraySchema.schema().type().length() > 0 || arraySchema.arraySchema().type().length() > 0 || arraySchema.schema().ref().length() > 0 || arraySchema.arraySchema().ref().length() > 0) {
        JSONObject schema = new JSONObject();


        //schema.put("items",p.array().schema());
        add("items", arraySchema.arraySchema(), schema);
        add("items", arraySchema.schema(), schema);

        add("minItems", arraySchema.minItems(), schema, Integer.MAX_VALUE);
        add("maxItems", arraySchema.maxItems(), schema, Integer.MIN_VALUE);
        add("uniqueItems", arraySchema.uniqueItems(), schema, false);
        if (schema.length() > 0) {
            add("type", "array", schema);
            if (schema.has("items")) {
                schema.getJSONObject("items").remove("description");
            }
            add("description", arraySchema.schema().description(), schema);
            add("description", arraySchema.arraySchema().description(), schema);
            //  logger.info("returned toJSON ArraySchema {}", schema);
        }

        return schema;

        //}
        //return null;
    }

    public static JSONObject createJSONSchemaFromSchema(JSONObject schema) {
        return new JSONObject().
                put("components", new JSONObject().put("schemas", components.optJSONObject("schemas"))).
                put("$schema", "http://json-schema.org/draft-07/schema#").

                put("type", "object").
                put("properties",
                        new JSONObject().
                                put("value", schema)
                ).put("additionalProperties", false).put("required", new JSONArray().put("value"));

    }

    public static boolean checkExample(String value, JSONObject schema, SchemaType type) {
        JSONObject v = null;
        try {
            org.everit.json.schema.Schema s = SchemaLoader.load(schema);
            if (type == SchemaType.DEFAULT) {
                if (value.startsWith("{")) {
                    type = SchemaType.OBJECT;
                } else if (value.startsWith("[")) {
                    type = SchemaType.ARRAY;
                }
            }

            switch (type) {
                case STRING:
                    v = new JSONObject().put("value", value);
                    s.validate(v);
                    return true;
                case INTEGER:
                    v = new JSONObject().put("value", Integer.parseInt(value));
                    s.validate(v);
                    return true;
                case NUMBER:
                    v = new JSONObject().put("value", Double.parseDouble(value));
                    s.validate(v);
                    return true;
                case ARRAY:
                    v = new JSONObject().put("value", new JSONArray(value));
                    s.validate(v);
                    return true;
                case BOOLEAN:
                    v = new JSONObject().put("value", Boolean.parseBoolean(value));
                    s.validate(v);
                    return true;
                case OBJECT:
                    v = new JSONObject().put("value", new JSONObject(value));
                    s.validate(v);
                    return true;
                default:
                    logger.warn("DID NOT TEST {}", value);
            }
        } catch (Exception ex) {
            logger.error("FAILED TO CHECK \nRaw:{}\nValue: {}\n Schema: {}\nReason:{}", value, v, schema, ex.getMessage(), ex);
        }
        return false;

    }

    public static boolean checkExample(ExampleObject e, JSONObject schema, SchemaType type) {
        String value = e.value();
        if (e.value().length() == 0) {
            value = e.name();
        }
        return checkExample(value, schema, type);

    }

    public static JSONObject toJSON(Parameter p) {
        JSONObject pjson = new JSONObject();
        if (p.ref().length() > 0) {
            add("$ref", p.ref(), pjson);
        } else {
            add("description", p.description(), pjson);

            add("name", p.name(), pjson);
            JSONObject schema = toJSON(p.schema());
            add("examples", p.examples(), pjson);
            if (schema.length() > 0) {
                add("schema", schema, pjson);

                JSONObject j = null;
                try {
                    //j = createJSONSchemaFromSchema(schema);

                    if (p.example().length() > 0) {
                        examples.add(new Example(p.example(), schema, p.schema().type()));


                    }
                    if (p.examples().length > 0) {

                        for (ExampleObject e : p.examples()) {

                            //checkExample(e, j, p.schema().type());
                            String v = e.value();
                            if (v.length() == 0) {
                                v = e.name();
                            }
                            if (v.length() > 0) {
                                examples.add(new Example(v, schema, p.schema().type()));
                            }
                        }
                    }
                } catch (Exception e) {
                    //throw new RuntimeException(e);
                    logger.error("COULD NOT PARSE SCHEMA! {}", j, e);
                }


            }

            if (p.explode() == Explode.TRUE) {
                add("explode", true, pjson, null);
            } else if (p.explode() == Explode.FALSE) {
                add("explode", false, pjson, null);
            }

            add("example", p.example(), pjson);
            add("required", p.required(), pjson, null);
            add("deprecated", p.deprecated(), pjson, false);
            add("allowEmptyValue", p.allowEmptyValue(), pjson, false);
            add("in", p.in().toString(), pjson);
            add("style", p.style().toString(), pjson);
            add("allowReserved", p.allowReserved(), pjson, false);
            add("content", p.content(), pjson);
            //add("array", p.array(), pjson);
            add("schema", toJSON(p.array()), pjson);
/*
        JSONObject j = new JSONObject();
        JSONObject schema = new JSONObject();
        schema.put("type","array");
        schema.put("items",array.arraySchema());
        //add("schema", array.schema(), j);
        //add("arraySchema", array.arraySchema(), j);

        add("maxItems", array.maxItems(), j, Integer.MIN_VALUE);
        add("minItems", array.minItems(), j, Integer.MAX_VALUE);
        add("uniqueItems", array.uniqueItems(), j, false);

        if (j.length() > 0) {

            json.put(tag, j);
        }

 */
            add("hidden", p.hidden(), pjson, false);
        }
        return pjson;
    }

    public static void add(Parameter p, JSONArray params) {

        params.put(toJSON(p));
    }

    public static JSONObject toJSON(SchemaProperty[] properties) {
        JSONObject schemajson = new JSONObject();
        schemajson.put("type", "object");
        JSONObject propjson = new JSONObject();
        JSONArray reqarray = new JSONArray();
        for (SchemaProperty sp : properties) {
            //logger.info("sp {}", sp);
            //propjson.put(sp.name()
            add(sp.name(), sp.schema(), propjson);
            //add(sp.name(), toJSON(sp.array()), propjson);

            JSONObject sj = toJSON(sp.array());
            if (sj != null) {
                //add("description",sp)
                add(sp.name(), sj, propjson);
            }
            if (sp.schema().required()) {
                reqarray.put(sp.name());
            } else if (sp.array().schema().required()) {
                reqarray.put(sp.name());
            } else if (sp.array().arraySchema().required()) {
                reqarray.put(sp.name());
            }

        }
        if (propjson.length() > 0) {

            schemajson.put("properties", propjson);
        }
        if (reqarray.length() > 0) {
            schemajson.put("required", reqarray);
        }
        return schemajson;

    }

    public static void add(String tag, SchemaProperty[] properties, JSONObject json) {

        json.put(tag, toJSON(properties));
    }

    public static void add(String tag, Content[] value, JSONObject json) {
        if (value.length > 0) {
            JSONObject contentjson = new JSONObject();
            for (Content c : value) {

                if (c.mediaType().length() > 0) {
                    JSONObject cjson = new JSONObject();
                    add("schema", toJSON(c.array()), cjson);
                    if (!cjson.has("schema")) {
                        if (c.schemaProperties().length > 0) {
                            add("schema", c.schemaProperties(), cjson);
                        }
                        if (!cjson.has("schema")) {
                            add("schema", toJSON(c.schema()), cjson);
                        }

                    }

                    if (c.examples().length > 0) {
                        add("examples", c.examples(), cjson);

                    }
                    if (cjson.length() > 0) {
                        contentjson.put(c.mediaType(), cjson);
                    }
                    JSONObject j = null;
                    try {
                        if (c.examples().length > 0) {
                            if (cjson.has("schema")) {
                                for (ExampleObject e : c.examples()) {
                                    String v = e.value();
                                    if (v.length() == 0) {
                                        v = e.name();
                                    }
                                    if (v.length() > 0) {
                                        examples.add(new Example(v, cjson.getJSONObject("schema"), c.schema().type()));
                                    }
                                }
                            } else {
                                logger.warn("no schema for {}", c);
                            }
                        }
                    } catch (Exception e) {
                        //throw new RuntimeException(e);
                        logger.error("COULD NOT PARSE SCHEMA! {}", j, e);
                    }
                }
            }
            if (contentjson.length() > 0) {
                json.put(tag, contentjson);
            }
        }

    }

    public static void add(String tag, Header[] value, JSONObject json) {
        if (value.length > 0) {
            JSONObject contentjson = new JSONObject();
            for (Header c : value) {

                if (c.description().length() > 0) {
                    JSONObject cjson = new JSONObject();
                    add("description", c.description(), cjson);
                    //add("name", c.name(), cjson);
                    add("schema", toJSON(c.schema()), cjson);

                    //add("required", String.valueOf(c.required()), cjson);

                    if (c.deprecated()) {
                        add("deprecated", String.valueOf(c.deprecated()), cjson);
                    }
                    add("$ref", c.ref(), cjson);

                    if (cjson.length() > 0) {
                        contentjson.put(c.name(), cjson);
                    }

                }
            }
            if (contentjson.length() > 0) {
                json.put(tag, contentjson);
            }
        }

    }

    public static JSONObject toJSON(ExampleObject example) {
        JSONObject json = new JSONObject();
        if (example.ref().length() > 0) {
            add("$ref", example.ref(), json);
        } else {
            add("summary", example.summary(), json);
            add("description", example.description(), json);
            add("value", example.value(), json, true);

            if (!json.has("value")) {
                add("value", example.value(), json);
            }
            if (!json.has("value")) {
                add("value", example.name(), json);
            }

            add("externalValue", example.externalValue(), json);
        }


        return json;
    }

    public static void add(String tag, ExampleObject[] examples, JSONObject json) {
        JSONObject ex = new JSONObject();
        for (ExampleObject e : examples) {
            String name = e.name();

            if (name.length() == 0) {
                name = e.value();
            }
            ex.put(name, toJSON(e));
        }

        if (ex.length() > 0) {
            json.put(tag, ex);
        }
    }

    public static JSONObject toJSON(Schema schema) {
        JSONObject sjson = new JSONObject();
        if (schema.ref().length() > 0) {
            add("$ref", schema.ref(), sjson);
        } else {
            add("type", schema.type().toString(), sjson);
            add("hidden", schema.hidden(), sjson, false);
            add("not", schema.not(), sjson);
            if (schema.notRef().length > 0) {
                JSONArray jsonArray = new JSONArray();
                for (String ref : schema.notRef()) {
                    jsonArray.put(new JSONObject().put("$ref", ref));
                }
                add("not", jsonArray, sjson);
            } else {
                add("not", schema.not(), sjson);
            }
            if (schema.oneOfRef().length > 0) {
                JSONArray jsonArray = new JSONArray();
                for (String ref : schema.oneOfRef()) {
                    jsonArray.put(new JSONObject().put("$ref", ref));
                }
                add("oneOf", jsonArray, sjson);
            } else {
                add("oneOf", schema.oneOf(), sjson);
            }
            if (schema.anyOfRef().length > 0) {
                JSONArray jsonArray = new JSONArray();
                for (String ref : schema.anyOfRef()) {
                    jsonArray.put(new JSONObject().put("$ref", ref));
                }
                add("anyOf", jsonArray, sjson);
            } else {
                add("anyOf", schema.anyOf(), sjson);
            }
            if (schema.allOfRef().length > 0) {
                JSONArray jsonArray = new JSONArray();
                for (String ref : schema.allOfRef()) {
                    jsonArray.put(new JSONObject().put("$ref", ref));
                }
                add("allOf", jsonArray, sjson);
            } else {
                add("allOf", schema.oneOf(), sjson);
            }

            //add("name", schema.name(), sjson);
            add("title", schema.title(), sjson);
            add("multipleOf", schema.multipleOf(), sjson, 0.0);
            add("exclusiveMaximum", schema.exclusiveMaximum(), sjson, false);
            add("exclusiveMaximum", schema.exclusiveMaximum(), sjson, false);
            add("maxLength", schema.maxLength(), sjson, Integer.MAX_VALUE);
            add("minLength", schema.minLength(), sjson, 0);
            try {
                if (schema.minimum().length() > 0) {
                    add("minimum", Integer.parseInt(schema.minimum()), sjson, 0);
                }
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
            }
            try {
                if (schema.maximum().length() > 0) {
                    add("maximum", Integer.parseInt(schema.maximum()), sjson, 0);
                }
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
            }
            add("pattern", schema.pattern(), sjson);
            add("maxProperties", schema.maxProperties(), sjson, 0);
            add("minProperties", schema.minProperties(), sjson, 0);
            add("requiredProperties", schema.requiredProperties(), sjson);

            add("description", schema.description(), sjson);
            add("format", schema.format(), sjson);

            add("nullable", schema.nullable(), sjson, false);

            add("deprecated", schema.deprecated(), sjson, false);
            add("default", schema.defaultValue(), sjson);
            add("enum", schema.allowableValues(), sjson);
            add("externalDocs", schema.externalDocs(), sjson);
            add("discriminatorProperty", schema.discriminatorProperty(), sjson);
            add("DiscriminatorMapping", schema.discriminatorMapping(), sjson);
            add("additionalProperties", schema.additionalProperties(), sjson, true);
            add("subTypes", schema.subTypes(), sjson);
            if (schema.example().length() > 0) {
                add("example", schema.example(), sjson);

                examples.add(new Example(schema.example(), sjson, schema.type()));


            }
        }
        return sjson;
    }

    public static void add(String tag, Schema schema, JSONObject json) {
        JSONObject sjson = toJSON(schema);
        if (sjson.length() > 0) {
            //only add the not optional things here so we always know if we need it
            //add("required", schema.required(), sjson, false);
            json.put(tag, sjson);

        }

    }

    // TODO: 7/20/22 expand to correct values
    public static void add(String tag, DiscriminatorMapping[] value, JSONObject json) {
        if (value.length > 0) {

        }
    }


    public static void add(String tag, ExternalDocumentation value, JSONObject json) {
        JSONObject j = new JSONObject();
        if (value.url().length() > 0) {
            j.put("url", value.url());
            json.put(tag, j);
        }
        if (value.description().length() > 0) {
            j.put("description", value.description());
            json.put(tag, j);
        }
    }

    public static void add(String tag, double value, JSONObject json, Double ignoreIfThis) {
        if (ignoreIfThis == null || ignoreIfThis != value) {
            json.put(tag, value);
        }
    }

    public static void add(String tag, Class<?>[] clzlist, JSONObject json) {
        JSONArray arr = new JSONArray();
        for (Class<?> clz : clzlist) {
            if (clz != Void.class) {
                arr.put(clz.getName());
            }
        }
        if (arr.length() > 0) {
            json.put(tag, arr);
        }
    }

    public static void add(String tag, Class<?> clz, JSONObject json) {
        if (clz != Void.class) {
            json.put(tag, clz.getName());
        }
    }

    public static void add(String tag, boolean value, JSONObject json, Boolean ignoreIfThis) {
        if (ignoreIfThis == null || ignoreIfThis != value) {
            json.put(tag, value);
        }
    }

    public static void add(String tag, int value, JSONObject json, Integer ignoreIfThis) {
        if (ignoreIfThis == null || ignoreIfThis != value) {
            json.put(tag, value);
        }
    }

    public static void add(String tag, JSONObject values, JSONObject json) {
        if (values != null && values.length() > 0) {

            json.put(tag, values);
        }
    }

    public static void add(String tag, JSONArray values, JSONObject json) {
        if (values != null && values.length() > 0) {

            json.put(tag, values);
        }
    }

    public static String getStringFromResource(String name) {
        if (labels == null) {
            Locale locale = new Locale("en", "US");

            labels = ResourceBundle.getBundle("translation", locale);
        }
        String v = labels.getString(name);
        if (v != null) {
            //logger.info("FOUND {} as  {}",name,v);
            return v;
        }
        //logger.error("COULD NOT FIND KEY {} IN TRANSLATION FILE",name);
        missingFromResource.add(name);
        return name;

    }

    public static String prepareString(String input) {
        @Deprecated
        Matcher m = formatPattern.matcher(input);
        while (m.matches()) {
            //logger.info("matched {}",input);
            if (m.groupCount() >= 2) {
                //logger.info("replacing {}",m.group(1));
                input = input.replaceFirst(formatRegex, getStringFromResource(m.group("key")));
            }
            //logger.info("changed to {}",input);
            m = formatPattern.matcher(input);
        }
        return input.replace("\n", "  \n");
    }

    public static void add(String tag, String value, JSONObject json) {
        if (value != null && value.length() > 0) {
            json.put(tag, prepareString(value));
        }

    }

    public static void add(String tag, String value, JSONObject json, boolean parseValue) {
        if (value != null && value.length() > 0) {
            if (parseValue) {
                if (value.equalsIgnoreCase("true")) {
                    json.put(tag, true);
                    return;
                }
                if (value.equalsIgnoreCase("false")) {
                    json.put(tag, false);
                    return;
                }
                if (value.startsWith("{")) {

                    try {
                        json.put(tag, new JSONObject(value));
                        return;
                    } catch (Exception ex) {

                    }
                }
                if (value.startsWith("[")) {

                    try {
                        json.put(tag, new JSONArray(value));
                        return;
                    } catch (Exception ex) {

                    }
                }
            }
            json.put(tag, prepareString(value));
        }

    }

    public static void add(String tag, String[] values, JSONObject json) {
        if (values != null && values.length > 0) {
            JSONArray arr = new JSONArray();
            for (String v : values) {
                arr.put(v);
            }
            json.put(tag, arr);
        }

    }


}

class Example {
    final String value;
    final SchemaType type;
    final JSONObject schema;

    public Example(String value, JSONObject schema, SchemaType type) {
        this.value = value;
        this.type = type;
        this.schema = schema;
    }


}