package de.kosmos_lab.web.server.servlets;

import de.kosmos_lab.web.exceptions.ParameterNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;


/**
 * "small" wrapper for our Requests primarily used to cache the body / jsonobject body objects and allow the direct
 * getting of ints etc
 */
public class BaseServletRequest extends MyHttpServletRequest {
    protected static final org.slf4j.Logger logger = LoggerFactory.getLogger("KosmoSHttpServletRequest");


    public BaseServletRequest(HttpServletRequest request) {
        super(request);
    }


    public JSONObject getJSONObject(String key) throws ParameterNotFoundException {
        String v = this.getParameter(key);
        if (v != null) {
            return new JSONObject(v);
        }
        try {
            return this.getBodyAsJSONObject().getJSONObject(key);
        } catch (JSONException ex) {

        } catch (NullPointerException ex) {

        }
        throw new ParameterNotFoundException(key);


    }

    public JSONArray getJSONArray(String key) throws ParameterNotFoundException {
        String v = this.getParameter(key);
        if (v != null) {
            return new JSONArray(v);
        }
        try {
            return this.getBodyAsJSONObject().getJSONArray(key);
        } catch (JSONException ex) {

        } catch (NullPointerException ex) {

        }
        throw new ParameterNotFoundException(key);


    }


}
