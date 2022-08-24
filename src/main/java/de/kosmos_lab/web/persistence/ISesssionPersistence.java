package de.kosmos_lab.web.persistence;

import de.kosmos_lab.web.data.User;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

public interface ISesssionPersistence extends IPersistence {
    
    void addJWT(@Nonnull String token);
    
    
    /**
     * get the JWT with a specific jwtid back, if it is in the system and still valid
     *
     * @param jwtid the id to find
     * @return
     */
    JSONObject getJWT(@Nonnull String jwtid);
    
    /**
     * get the jwt for a user
     *
     * @param user
     * @return
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    String getJWT(@Nonnull User user) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException;
    
    Collection<JSONObject> getMySessions(String username);
    
    
    void killJWT(String jwtid);
    
    
    /**
     * verify the given jwt
     *
     * @param token
     * @return
     */
    JSONObject verifyJWT(@Nonnull String token);
    
    
    int initSesssions();
    
    
}
