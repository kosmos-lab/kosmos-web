package de.kosmos_lab.web.persistence;


import de.kosmos_lab.web.server.JWT;
import de.kosmos_lab.web.data.User;
import de.kosmos_lab.web.exceptions.LoginFailedException;
import de.kosmos_lab.utils.FileUtils;
import de.kosmos_lab.utils.StringFunctions;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JSONPersistence implements IUserPersistence, ISesssionPersistence {
    private static final Logger logger = LoggerFactory.getLogger("JSONPersistence");
    private final ControllerWithPersistence server;
    private final File file;
    protected JSONObject json;
    protected ConcurrentHashMap<String, User> users;
    protected ConcurrentHashMap<UUID, User> usersByUUID;
    
    protected ConcurrentHashMap<String, String> sessions;
    
    
    public JSONPersistence(ControllerWithPersistence server, File storageFile) {
        prepare();
        this.server = server;
        this.file = storageFile;
        if (!storageFile.exists()) {
            File distFile = new File(storageFile.getAbsolutePath() + ".dist");
            if (distFile.exists()) {
                try {
                    Files.copy(distFile.toPath(), file.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        JSONObject tempJson = new JSONObject();
        try {
            
            tempJson = new JSONObject(FileUtils.readFile(storageFile));
        } catch (FileNotFoundException e) {
            //ignore - its fine
        } catch (IOException e) {
            throw new RuntimeException("COULD NOT READ STORAGE FILE " + storageFile.getName());
        }
        this.json = tempJson;
        
        init();
        this.save();
        
    }
    
    @Override
    public void addJWT(@Nonnull String token) {
        try {
            JSONObject o = server.getJwt().verify(token);
            this.sessions.put(o.getString("jwtid"), token);
            
            this.save();
            
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JWT.JWTVerifyFailed e) {
            e.printStackTrace();
        }
        
        
    }
    
    @Override
    public boolean addUser(@CheckForNull String username, @CheckForNull String password, int level) {
        if (username == null) {
            throw new IllegalArgumentException();
        }
        if (password == null) {
            throw new IllegalArgumentException();
        }
        if (users.containsKey(username.toLowerCase(Locale.ENGLISH))) {
            return false;
            
        }
        String salt = StringFunctions.generateRandomKey();
        User u = new User(server, server.generateUUID(), username, server.hashSaltPepper(password, salt), salt, level);
        cacheUser(u);
        //this.getUser(newu);
        this.save();
        return true;
    }
    
    private void cacheUser(String UsernameLowerCased, User user) {
        this.users.put(UsernameLowerCased.toLowerCase(), user);
        this.usersByUUID.put(user.getUUID(), user);
    }
    
    public void cacheUser(User user) {
        cacheUser(user.getName().toLowerCase(Locale.ENGLISH), user);
        this.server.addUUID(user.getUUID(), user);
    }
    
    public void doSave() {
        //logger.info("SAVING DB! {}", json.toString());
        try {
            FileUtils.writeToFile(this.file, json.toString());
        } catch (IOException e) {
            logger.error("CONFIG COULD NOT BE SAVED!");
        }
        
    }
    
    @Override
    public JSONObject getJWT(@Nonnull String jwtid) {
        try {
            String token = this.sessions.get(jwtid);
            if (token != null) {
                return server.getJwt().verify(token);
            }
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JWT.JWTVerifyFailed e) {
            //e.printStackTrace();
        }
        
        this.sessions.remove(jwtid);
        
        save();
        return null;
    }
    
    @Override
    public String getJWT(@Nonnull User user) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        String jwt = server.getJwt().sign(user.toJWT());
        this.addJWT(jwt);
        return jwt;
    }
    
    @Override
    public Collection<JSONObject> getMySessions(String username) {
        LinkedList<JSONObject> list = new LinkedList<>();
        String ul = username.toLowerCase(Locale.ENGLISH);
        
        
        //Iterator<String> it = this.sessions.keySet().iterator();
        for (String jwtid : sessions.keySet()) {
            JSONObject o = this.getJWT(jwtid);
            if (o != null) {
                if (ul.equals(o.getString("name"))) {
                    list.add(o);
                }
            }
        }
        
        
        return list;
    }
    
    public ControllerWithPersistence getController() {
        return this.server;
    }
    
    
    @Override
    public User getUser(@CheckForNull String username) {
        if (username != null) {
            String ul = username.toLowerCase(Locale.ENGLISH);
            return users.get(ul);
            
        }
        
        return null;
    }
    
    @Override
    public User getUser(@CheckForNull UUID uuid) {
        if (uuid != null) {
            return usersByUUID.get(uuid);
        }
        return null;
    }
    
    protected void init() {
        
        logger.info("Read {} Users", this.initUsers());
        logger.info("Read {} Sessions", this.initSesssions());
    }
    
    @Override
    public int initSesssions() {
        
        int read = 0;
        JSONObject tempJson = this.json.optJSONObject("jwtsessions");
        if (tempJson != null) {
            
            for (String jwtid : tempJson.keySet()) {
                
                this.sessions.put(jwtid, tempJson.getString(jwtid));
                read++;
            }
        }
        return read;
    }
    
    @Override
    public int initUsers() {
        int read = 0;
        JSONObject tempJson = this.json.optJSONObject("users");
        if (tempJson != null) {
            for (String key : tempJson.keySet()) {
                this.users.put(key.toLowerCase(Locale.ENGLISH), new User(this, tempJson.getJSONObject(key)));
                read++;
            }
            
        }
        return read;
        
    }
    
    @Override
    public void killJWT(String jwtid) {
        if (this.sessions.remove(jwtid) != null) {
            this.save();
        }
        
    }
    
    @Override
    public User login(@CheckForNull String username, @CheckForNull String password) throws LoginFailedException {
        if (username != null && password != null) {
            String ul = username.toLowerCase(Locale.ENGLISH);
            User u = this.getUser(ul);
            if (u != null) {
                if (u.checkPassword(password)) {
                    
                    return u;
                } else {
                    logger.warn("mismatched password for user {}", username);
                }
            }
        }
        throw new LoginFailedException();
    }
    
    public void prepare() {
        
        sessions = new ConcurrentHashMap<>();
        users = new ConcurrentHashMap<>();
        usersByUUID = new ConcurrentHashMap<>();
    }
    
    
    protected void save() {
        synchronized (json) {
            
            JSONObject tempJSON = new JSONObject();
            //iterate the sessions
            //logger.info("found {} Sessions to save", sessions.size());
            for (Map.Entry<String, String> entry : sessions.entrySet()) {
                //check the validity of the token
                JSONObject token = null;
                try {
                    token = server.getJwt().verify(entry.getValue());
                    //only add them if the token are still valid
                    tempJSON.put(entry.getKey(), entry.getValue());
                } catch (InvalidKeyException e) {
                    //e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    //e.printStackTrace();
                } catch (JWT.JWTVerifyFailed | UnsupportedEncodingException e) {
                    //e.printStackTrace();
                }
                
                if (token == null) {
                    sessions.remove(entry.getKey());
                }
            }
            json.put("jwtsessions", tempJSON);
            
            tempJSON = new JSONObject();
            for (Map.Entry<String, User> entry : users.entrySet()) {
                tempJSON.put(entry.getKey(), entry.getValue().toJSON());
            }
            json.put("users", tempJSON);
            doSave();
        }
        
    }
    
    @Override
    public JSONObject verifyJWT(@Nonnull String token) {
        try {
            JSONObject o = server.getJwt().verify(token);
            if (o.has("jwtid")) {
                if (this.sessions.containsKey(o.getString("jwtid"))) {
                    return o;
                }
            }
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JWT.JWTVerifyFailed e) {
            e.printStackTrace();
        }
        return null;
    }
}
