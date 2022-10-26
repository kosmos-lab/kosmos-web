package de.kosmos_lab.web.server;

import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Base64.Encoder;

/**
 * JWT is used to verify and sign JWT
 */
public class JWT {
    public enum Algorithm {
        NONE,
        HS256
    }
    public static final long DEFAULT_LIFETIME = 86400000l;
    Algorithm algorithm;
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("JWT");
    
    /**
     * default lifetime of a JWT
     */
    public final long lifetime;
    
    /**
     * The Class JWTVerifyFailed.
     */
    public static final class JWTVerifyFailed extends Exception {
        
        /**
         * The Constant serialVersionUID.
         */
        private static final long serialVersionUID = -7577398934097264131L;
        
    }
    
    /**
     * Trims the data, removes trailing =
     *
     * @param data data to trim
     * @return trimmed version of data
     */
    public static String trim(String data) {
        //might occasionally hit ==, so just loop it
        while (data.endsWith("=")) {
            data = data.substring(0, data.length() - 1);
        }
        return data;
    }
    
    
    /**
     * The key used
     */
    protected byte[] key;
    
    /**
     * Instantiates a new jwt with HS256
     *
     * @param key the key to use
     */
    public JWT(final String key, long lifetime) {
        this(Algorithm.HS256, key, lifetime);
    }
    public JWT(final String key) {
        this(Algorithm.HS256, key,DEFAULT_LIFETIME);
    }
    /**
     * Instantiates a new jwt.
     *
     * @param algorithm the algorithm to use
     * @param key       the key to use
     */
    public JWT(final Algorithm algorithm, final String key, long lifetime) {
        this.lifetime = lifetime;
        this.algorithm = algorithm;
        this.key = key.getBytes(StandardCharsets.UTF_8);
    }
    
    /**
     * Encode the payload
     *
     * @param data the data
     * @return the string
     * @throws InvalidKeyException      the invalid key exception
     * @throws NoSuchAlgorithmException the no such algorithm exception
     * @throws IllegalStateException    the illegal state exception
     */
    public String encode(final String data)
            throws InvalidKeyException, NoSuchAlgorithmException, IllegalStateException {
        if (algorithm == Algorithm.HS256) {
            final Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            final SecretKeySpec secret_key = new SecretKeySpec(this.key, "HmacSHA256");
            sha256_HMAC.init(secret_key);
            
            return Base64.getUrlEncoder().encodeToString(sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } else if (algorithm == Algorithm.NONE) {
            return "";
        }
        throw new NoSuchAlgorithmException(this.algorithm + " is not implemented");
        
    }
    
    /**
     * Sign the payload
     *
     * @param head    the header
     * @param payload the payload
     * @return the signed JWT string
     * @throws UnsupportedEncodingException the unsupported encoding exception
     * @throws InvalidKeyException          the invalid key exception
     * @throws NoSuchAlgorithmException     the no such algorithm exception
     * @throws IllegalStateException        the illegal state exception
     */
    public String sign(final byte[] head, final byte[] payload)
            throws UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException, IllegalStateException {
        final Encoder b = Base64.getUrlEncoder();
        
        String s = JWT.trim(b.encodeToString(head)) + "." + JWT.trim(b.encodeToString(payload));
        s += "." + JWT.trim(this.encode(s));
        
        return s;
    }
    
    /**
     * Sign a json payload
     *
     * @param object the JSON object
     * @return signed JWT string
     * @throws UnsupportedEncodingException the unsupported encoding exception
     * @throws InvalidKeyException          the invalid key exception
     * @throws NoSuchAlgorithmException     the no such algorithm exception
     * @throws IllegalStateException        the illegal state exception
     */
    public String sign(final JSONObject object)
            throws UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException, IllegalStateException {
        final JSONObject h = new JSONObject();
        h.put("type", "JWT");
        h.put("alg", this.algorithm.name());
        object.put("exp", System.currentTimeMillis() + lifetime);
        return this.sign(h.toString().getBytes(StandardCharsets.UTF_8), object.toString().getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Verify.
     *
     * @param data the data to be verified
     * @return an JSONObject with the payload
     * @throws InvalidKeyException          if key is invalid
     * @throws NoSuchAlgorithmException     if algorithm is not supported
     * @throws IllegalStateException        if state was illegal
     * @throws UnsupportedEncodingException if the encoding is not supported
     * @throws JWTVerifyFailed              the JWT verify failed
     */
    public JSONObject verify(String data) throws InvalidKeyException, NoSuchAlgorithmException, IllegalStateException,
            UnsupportedEncodingException, JWTVerifyFailed {
        final String[] token = JWT.trim(data).split("\\.");
        if (token.length != 3) {
            throw new JWTVerifyFailed();
        }
        final byte[] head = Base64.getUrlDecoder().decode(token[0]);
        final byte[] payload = Base64.getUrlDecoder().decode(token[1]);
        final String s = this.sign(head, payload);
        data = data.replace("=", "");
        if (s.equals(data)) {
            final String pl = new String(payload, StandardCharsets.US_ASCII);
            JSONObject obj = new JSONObject(pl);
            if (obj.has("exp")) {
                if (obj.getLong("exp") > System.currentTimeMillis()) {
                    return obj;
                }
            }
            
            
        }
        
        throw new JWTVerifyFailed();
        
    }
    
    /**
     * extract the payload from a given data string
     *
     * @param data
     * @return
     * @throws JWTVerifyFailed will be thrown if it does not appear like a valid JWT
     */
    public static String extractPayload(String data) throws JWTVerifyFailed {
        final String[] token = JWT.trim(data).split("\\.");
        if (token.length != 3) {
            throw new JWTVerifyFailed();
        }
        return new String(Base64.getUrlDecoder().decode(token[1]), StandardCharsets.US_ASCII);
        
    }
    
}