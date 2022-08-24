package de.kosmos_lab.web.data;

import org.json.JSONObject;

import java.util.UUID;

public interface IUser {

    int LEVEL_ADMIN = 100;

    boolean canAccess(int level);
    
    boolean checkPassword(String input);
    
    int getLevel();
    
    String getName();
    
    UUID getUUID();
    
    boolean isAdmin();
    
    
    JSONObject toJWT();
    
}
