package de.kosmos_lab.web.client.websocket;

import de.kosmos_lab.web.client.websocket.SimpleWebSocketEndpoint.MessageHandler;
import de.kosmos_lab.web.client.websocket.SimpleWebSocketEndpoint.RegexMessageHandler;
import de.kosmos_lab.web.client.websocket.handlers.IOnConnected;
import de.kosmos_lab.web.client.websocket.handlers.IOnDisconnected;
import de.kosmos_lab.web.client.websocket.handlers.IOnMessage;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ClientEndpointConfig.Builder;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Session;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.MultiPartContentProvider;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.glassfish.tyrus.client.ClientManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebSocketClient extends HttpClient {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("WebSocketClient");

    private final String user;
    private final String pass;
    private final String base;
    private final String wspath;
    boolean stopped = false;
    private String token;

    public WebSocketClientEndpoint getEndpoint() {
        return endpoint;
    }

    private WebSocketClientEndpoint endpoint;


    public WebSocketClient(String base, String user, String pass,String wspath, WebSocketClientEndpoint endpoint) {
        this.base = base;
        this.user = user;
        this.pass = pass;
        this.wspath = wspath;
        this.setConnectTimeout(5000l);
        this.endpoint = endpoint;


        /*this.setMaxConnectionsPerDestination(10);
        this.setMaxRequestsQueuedPerDestination(100);*/
        
        try {
            this.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    public void setEndpoint(WebSocketClientEndpoint endpoint) {
        this.endpoint = endpoint;
    }
    public void connect() {
        WebSocketClient c = this;
        (new Thread() {
            public void run() {
                while (!stopped) {
                    try {
                        //this.devices.clear();
                        final ClientEndpointConfig cec = Builder.create().build();
                        
                        ClientManager client = ClientManager.createClient();
                        if ( endpoint == null ) {
                            endpoint = new WebSocketClientEndpoint(c) {
                                @Override
                                public void wsOnOpen(Session session){
                                    triggerOnOpen();
                                }
                                @Override
                                public void wsOnClose(Session session,CloseReason reason){
                                    triggerOnClose(reason);
                                }
                                @Override
                                public void wsOnMessage(Session session, String message) {
                                    logger.info("WS Client got message {}", message);
                                    for (Entry<String, Queue<MessageHandler>> queueEntry : mapMessageHandler.entrySet()) {
                                        try {

                                            if (queueEntry.getKey().equals(message)) {
                                                for (MessageHandler messageHandler : queueEntry.getValue()) {
                                                    messageHandler.handleMessage(message);
                                                }
                                                continue;
                                            }


                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                        }

                                    }
                                    for (Entry<Pattern, Queue<RegexMessageHandler>> queueEntry : mapRegexMessageHandler.entrySet()) {
                                        try {
                                            Matcher m = queueEntry.getKey().matcher(message);
                                            if (m.matches()) {
                                                for (RegexMessageHandler messageHandler : queueEntry.getValue()) {
                                                    messageHandler.handleMessage(message, m);
                                                }
                                                continue;
                                            }


                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                        }

                                    }
                                    triggerOnMessage(message);
                                }
                            };
                        }
                        URI uri = new URI(getBase().replace("https:", "wss:").replace("http:", "ws:") + wspath);
                        logger.info("WS trying to connect to {}",uri);
                        //client.connectToServer(endpoint, cec, uri);
                        client.connectToServer(endpoint,  uri);
                        
                        while (endpoint.stopped != true) {
                            Thread.sleep(100);
                        }
                        
                    } catch (Exception e) {
                        logger.error("Exception!", e);
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        logger.error("Exception!", e);
                    }
                }
            }
        }).start();
        
        
    }
    Set<IOnConnected> handlerOnConnect = ConcurrentHashMap.newKeySet();
    Set<IOnMessage> handlerOnMessage = ConcurrentHashMap.newKeySet();
    Set<IOnDisconnected> handlerOnDisconnect = ConcurrentHashMap.newKeySet();
    public void addHandler(IOnConnected handler) {
        this.handlerOnConnect.add(handler);
    }
    public void addHandler(IOnMessage handler) {
        this.handlerOnMessage.add(handler);
    }
    public void send(String message) throws IOException {
        endpoint.send(message);
    }
    public void addHandler(IOnDisconnected handler) {
        this.handlerOnDisconnect.add(handler);
    }
    private void triggerOnOpen() {
        for ( IOnConnected handler : this.handlerOnConnect) {
            handler.onConnect();
        }
    }
    private void triggerOnMessage(String message) {
        for ( IOnMessage handler : this.handlerOnMessage) {

            handler.onMessage(message);
        }
    }
    private void triggerOnClose(CloseReason closeReason) {
        for ( IOnDisconnected handler : this.handlerOnDisconnect) {
            handler.onDisconnected(closeReason);
        }
    }
    private Request createRequest(String url, HttpMethod method) {
        
        if (!url.startsWith("http")) {
            url = base + url;
        }
        logger.trace("creating request for {}", url);
        Request request = newRequest(url);
        request.method(method);
        request.agent("KosmoS Client");
        return request;
    }
    
    public void disconnect() {
        this.stopped = true;
        this.endpoint.stop();
    }
    public JSONObject getVars() {
        return this.endpoint.getVars();
    }
    public String getBase() {
        return this.base;
    }
    
    public String getPass() {
        return this.pass;
    }
    
    /**
     * get the response for a given result, if 401 is returned the jwt will be renewed and it will be tried again
     *
     * @param request the request to parse
     * @return
     */
    private ContentResponse getResponse(Request request) {
        ContentResponse response = null;
        try {
            
            response = request.send();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
            
        }
        return response;
        
    }
    
    public String getToken() {
        return this.token;
    }
    
    public Object getVar(String key) {
        return this.endpoint.getVar(key);
    }
    
    public void setToken(String access_token) {
        this.token = access_token;
    }
    
    public String getUser() {
        return this.user;
    }
    
    public WebSocketClientEndpoint getWebSocket() {
        return this.endpoint;
    }
    
    public String post(String url, HashMap<String, Object> parameters) {
        Request request = createRequest(url, HttpMethod.POST);
        MultiPartContentProvider multiPart = new MultiPartContentProvider();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            multiPart.addFieldPart(entry.getKey(), new StringContentProvider(entry.getValue().toString()), null);
        }
        request.content(multiPart);
        return (getResponse(request).getContentAsString());
    }
    
    public JSONObject postJSON(String url, JSONObject json) {
        Request request = createRequest(url, HttpMethod.POST);
        if (json != null) {
            request.content(new StringContentProvider(json.toString()), "application/json");
        }
        if (token != null) {
            request.header("authorization", "Bearer " + token);
        }
        
        ContentResponse r = getResponse(request);
        if (r != null) {
            try {
                return new JSONObject(r.getContentAsString());
            } catch (JSONException ex) {
                logger.error("HTTP Status {}:{}",r.getStatus(),r.getContentAsString());
                logger.error("Exception",ex);
            }
            
        }
        return null;
    }
    
    public void sendCommand(JSONObject command, WebSocketEventConsumer consumer) {
        if (this.endpoint != null) {
            this.endpoint.sendCommand(command, consumer);
        } else {
            logger.error("NO ENDPOINT!!");
        }
    }
    
    public void unstop() {
        this.stopped = false;
    }
    
    public boolean waitForValue(String key, Object expected, long waittime) {
        
        return endpoint.waitForValue(key, expected, waittime);
    }

    public void waitForInit() throws InterruptedException {
        this.endpoint.initLatch.await();
    }
    ConcurrentHashMap<String, Queue<MessageHandler>> mapMessageHandler = new ConcurrentHashMap<>();
    ConcurrentHashMap<Pattern, Queue<RegexMessageHandler>> mapRegexMessageHandler = new ConcurrentHashMap<>();
    public void addMessageHandler(String message, MessageHandler messageHandler) {
        Queue<MessageHandler> list = mapMessageHandler.get(message);
        if (list == null) {
            list = new ConcurrentLinkedQueue();
            this.mapMessageHandler.put(message, list);
        }

        list.add(messageHandler);

    }

    public void addMessageHandler(Pattern message, RegexMessageHandler messageHandler) {
        Queue<RegexMessageHandler> list = mapRegexMessageHandler.get(message);
        if (list == null) {
            list = new ConcurrentLinkedQueue();
            this.mapRegexMessageHandler.put(message, list);
        }

        list.add(messageHandler);

    }

}
