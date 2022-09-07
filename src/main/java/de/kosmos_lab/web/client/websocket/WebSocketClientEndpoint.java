package de.kosmos_lab.web.client.websocket;

import de.kosmos_lab.utils.JSONChecker;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class WebSocketClientEndpoint extends Endpoint {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("WebSocketClientEndpoint");
    protected final WebSocketClient client;
    public CountDownLatch initLatch;
    protected boolean stopped = false;
    int lastId = 1;
    protected Session session;
    private boolean authed = false;
    private final JSONObject vars = new JSONObject();
    protected final HashMap<Integer, WebSocketEventConsumer> consumers = new HashMap<>();

    public WebSocketClientEndpoint(WebSocketClient webSocketClient) {
        this.client = webSocketClient;
        this.initLatch = new CountDownLatch(1);

    }
    public boolean isStopped() {
        return this.stopped;
    }
    public void addConsumer(int id, WebSocketEventConsumer consumer) {
        this.consumers.put(id, consumer);
        
        
    }
    
    public Object getVar(String name) {
        return this.vars.get(name);
    }
    public JSONObject getVars() {
        return this.vars;
    }
    
    public boolean isAuthed() {
        return this.authed;
    }
    
    @Override
    public void onClose(Session session, CloseReason closeReason) {
        super.onClose(session, closeReason);
        logger.info("WS onClose");
        //this.stopped = false;
    }
    
    public void onMessageReceived(String message) {
        logger.info("wsreceived: {}",message);

    }

    
    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {

        try {
            logger.info("WSClient onOpen");
            this.session = session;
            
            session.addMessageHandler(new MessageHandler.Whole<String>() {
                
                @Override
                public void onMessage(String message) {
                    onMessageReceived(message);
                    
                }
            });
        } catch (Exception e) {
            logger.error("Exception!", e);
        }
    }
    
    
    /**
     * sends the given text to the Endpoint
     *
     * @param text
     * @throws IOException
     */
    public void send(String text) throws IOException {
        
        session.getBasicRemote().sendText(text);
    }
    
    public void sendCommand(JSONObject command, WebSocketEventConsumer consumer) {
        if (command.has("id")) {
            try {
                int id = command.getInt("id");
                if (id > lastId) {
                    lastId = id;
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!command.has("id")) {
            command.put("id", ++lastId);
        }
        this.addConsumer(command.getInt("id"), consumer);
        try {
            logger.info("wssent: {}", command);
    
            this.send(command.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
    public boolean waitForValue( String key, Object expected, long waittime) {
        long started = System.currentTimeMillis();
        while (true) {
            try {
                
                if (JSONChecker.equals(vars.get(key), expected)) {
                    return true;
                }
            } catch (Exception e) {
                //e.printStackTrace();
            }
    
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long delta = System.currentTimeMillis() - started;
            if (delta > waittime) {
                return false;
            }
        }
    }
    
    
    public void setVar(String name, Object value) {
        logger.info("setting {} to {}",name,value);
        this.vars.put(name, value);
    }
    
    public void stop() {
        this.stopped = true;
        try {
            this.session.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
