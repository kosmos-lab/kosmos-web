package de.kosmos_lab.web.data;

import de.kosmos_lab.utils.StringFunctions;
import de.kosmos_lab.web.persistence.ControllerWithPersistence;
import de.kosmos_lab.web.persistence.JSONPersistence;
import de.kosmos_lab.web.persistence.JSONPersistenceObject;
import org.json.JSONObject;

import java.util.UUID;

public class User implements JSONPersistenceObject, IUser {
    private final static String FIELD_UUID = "UUID";
    private final static String FIELD_NAME = "name";
    private final static String FIELD_LEVEL = "level";
    private final static String FIELD_SALT = "salt";
    private final static String FIELD_HASH = "hash";
    
    private final ControllerWithPersistence server;
    private final UUID uuid;
    private final String salt;
    private final String hash;
    private int level = 0;
    private final String name;
    
    public User(ControllerWithPersistence server, String uuid, String username, String hash, String salt, int userLevel) {
        this(server, UUID.fromString(uuid), username, hash, salt, userLevel);
    }
    
    public User(ControllerWithPersistence server, UUID uuid, String username, String hash, String salt, int userLevel) {
        if (uuid == null) {
            uuid = server.generateUUID();
        }
        this.server = server;
        this.salt = salt;
        this.name = username;
        this.level = userLevel;
        this.uuid = uuid;
        this.hash = hash;
    }
    public User(JSONPersistence persistence, JSONObject jsonObject) {
        this(persistence.getController(), jsonObject.optString(FIELD_UUID), jsonObject.getString(FIELD_NAME), jsonObject.getString(FIELD_HASH), jsonObject.getString(FIELD_SALT), jsonObject.getInt(FIELD_LEVEL));
        
    }
    
    
    public boolean canAccess(int level) {
        return this.level >= level;
        
    }
    
    public int getLevel() {
        return this.level;
    }
    
    public String getName() {
        return this.name;
    }
    
    public UUID getUUID() {
        return this.uuid;
    }
    
    public boolean isAdmin() {
        return level >= 100;
    }
    
    public JSONObject toJSON() {
        JSONObject o = new JSONObject();
        o.put(FIELD_NAME, name);
        o.put(FIELD_LEVEL, level);
        o.put(FIELD_UUID, uuid.toString());
        o.put(FIELD_SALT, salt);
        o.put(FIELD_HASH, hash);
        
        return o;
        
        
    }
    
    public JSONObject toJWT() {
        JSONObject o = new JSONObject();
        o.put("name", name);
        o.put("level", level);
        o.put("id", uuid.toString());
        
        String jwtid = StringFunctions.generateRandomKey();
        while (server.isKnownJWTID(jwtid)) {
            jwtid = StringFunctions.generateRandomKey();
        }
        o.put("jwtid", jwtid);
        
        return o;
    }
    
    public boolean checkPassword(String password) {
        
        return server.hashSaltPepper(password, salt).equals(this.hash);
        
    }
}
