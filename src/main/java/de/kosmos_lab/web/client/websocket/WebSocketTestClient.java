package de.kosmos_lab.web.client.websocket;


import de.kosmos_lab.web.client.websocket.SimpleWebSocketEndpoint;
import jakarta.websocket.ClientEndpoint;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ClientEndpoint
public class WebSocketTestClient extends SimpleWebSocketEndpoint {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("WebSocketTestClient");

    private Queue<String> listMessages = new ConcurrentLinkedQueue();

    JSONObject json = new JSONObject();

    public void set(String key, Object value) {
        json.put(key, value);
    }

   public JSONObject getObjects()  {
        return json;
   }

    public WebSocketTestClient(URI endpointURI) {
        super(endpointURI);

        addMessageHandler(new SimpleWebSocketEndpoint.MessageHandler() {
            public void handleMessage(String message) {
                logger.info("got message: {}", message);
                listMessages.add(message);


            }
        });

    }



    public boolean hasMessage(String message) {
        return this.listMessages.contains(message);
    }

}
