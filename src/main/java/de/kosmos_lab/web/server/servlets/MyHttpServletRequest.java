package de.kosmos_lab.web.server.servlets;

import de.kosmos_lab.web.data.User;
import de.kosmos_lab.web.exceptions.ParameterNotFoundException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;


/**
 * "small" wrapper for our Requests
 * primarily used to cache the body / jsonobject body objects and allow the direct getting of ints etc
 */
public class MyHttpServletRequest {
    protected static final org.slf4j.Logger logger = LoggerFactory.getLogger("MyHttpServletRequest");

    private final HttpServletRequest request;
    private JSONObject bodyJSONObject = null;
    private JSONArray bodyJSONArray = null;
    private String body = null;

    public MyHttpServletRequest(HttpServletRequest request) {
        this.request = request;
    }

    public void checkParameter(String[] params) throws ParameterNotFoundException {
        for (String param : params) {
            checkParameter(param);
        }

    }

    private void checkParameter(String param) throws ParameterNotFoundException {
        if (this.getParameter(param) == null) {
            throw new ParameterNotFoundException(param);
        }


    }


    public @Nonnull String getBody() throws IOException {
        if (body != null) {
            return this.body;
        }
        StringBuilder lines = new StringBuilder();
        String line;
        while ((line = request.getReader().readLine()) != null) {
            lines.append(line);
            lines.append('\n');
        }
        body = lines.toString();

        return body;
    }

    public JSONArray getBodyAsJSONArray() {
        if (this.bodyJSONArray != null) {
            return this.bodyJSONArray;
        }

        try {
            String body = this.getBody();
            if (body.startsWith("[")) {
                JSONArray a = new JSONArray(body);
                this.bodyJSONArray = a;
                return a;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;


    }

    public JSONObject getBodyAsJSONObject() {
        //check if we already know this
        if (bodyJSONObject != null) {
            return bodyJSONObject;
        }

        try {

            String body = this.getBody();

            if (body.startsWith("{")) {
                JSONObject o = new JSONObject(body);
                this.bodyJSONObject = o;
                return o;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //if something happened, return back
        return new JSONObject();


    }

    public boolean getBoolean(String key) throws ParameterNotFoundException {
        String v = this.getParameter(key);
        if (v != null) {
            return Boolean.parseBoolean(v);
        }
        try {
            return this.getBodyAsJSONObject().getBoolean(key);
        } catch (JSONException ex) {

        }
        throw new ParameterNotFoundException(key);


    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String v = this.getParameter(key);
        //logger.info("found {} as {}",key,v);
        if (v != null) {
            return Boolean.parseBoolean(v);
        }
        try {
            return this.getBodyAsJSONObject().optBoolean(key, defaultValue);
        } catch (JSONException ex) {

        }
        return defaultValue;


    }

    public double getDouble(String key) throws ParameterNotFoundException {


        String v = this.getParameter(key);
        if (v != null) {
            return Double.parseDouble(v);
        }
        try {
            return this.getBodyAsJSONObject().getDouble(key);
        } catch (JSONException ex) {

        }
        throw new ParameterNotFoundException(key);
    }

    public double getDouble(String key, double defaultValue) {
        String v = this.getParameter(key);
        if (v != null) {
            return Double.parseDouble(v);
        }
        try {
            return this.getBodyAsJSONObject().optDouble(key, defaultValue);
        } catch (JSONException ex) {

        }
        return defaultValue;


    }

    public int getInt(String key) throws ParameterNotFoundException {
        String v = this.getParameter(key);
        if (v != null) {
            return Integer.parseInt(v);
        }
        try {
            return this.getBodyAsJSONObject().getInt(key);
        } catch (JSONException ex) {

        }
        throw new ParameterNotFoundException(key);

    }

    public int getInt(String key, int defaultValue) {
        try {
            String v = this.getParameter(key);
            if (v != null) {
                return Integer.parseInt(v);
            }

        } catch (SecurityException | NullPointerException | NumberFormatException ex) {
        }
        try {
            return this.getBodyAsJSONObject().optInt(key, defaultValue);
        } catch (JSONException ex) {

        }
        return defaultValue;

    }

    public String getParameter(String key) {
        //logger.info("trying to read parameter {}",key);
        String p = request.getParameter(key);
        if (p != null) {
            //logger.info("found in parameters {}={}",key,p);
            return p;
        }
        try {
            JSONObject o = this.getBodyAsJSONObject();
            if (o != null) {
                return o.get(key).toString();

            }
        } catch (Exception e) {
            //e.printStackTrace();
        }


        return null;
    }

    @Nonnull
    public String getParameter(String key, boolean throwException) throws ParameterNotFoundException {
        String r = getParameter(key);
        if (throwException && r == null) {
            throw new ParameterNotFoundException(key);
        }
        return r;
    }

    public HttpServletRequest getRequest() {
        return this.request;
    }

    @Nonnull
    public String getString(String key) throws ParameterNotFoundException {
        String v = getParameter(key);
        if (v != null) {
            return v;
        }
        throw new ParameterNotFoundException(key);

    }

    public String getString(String key, String defaultValue) {
        try {
            String value = getParameter(key);
            if (value != null) {
                return getParameter(key);
            }
        } catch (SecurityException | NullPointerException ex) {

        }
        return defaultValue;
    }

    public User getUser() {
        return (User) request.getAttribute("user");
    }

    public Object getAttribute(String key) {
        return this.request.getAttribute(key);
    }
}
