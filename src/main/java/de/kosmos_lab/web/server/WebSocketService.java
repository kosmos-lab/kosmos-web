package de.kosmos_lab.web.server;

import de.kosmos_lab.web.server.example.MyWebSocketService;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interface for all WebSocket Services
 */
public abstract  class WebSocketService {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(WebSocketService.class);

    protected  Set<Session> sessions = ConcurrentHashMap.newKeySet();
    protected final WebServer server;

    public WebSocketService(WebServer server) {
        this.server = server;
    }

    /**
     * a new client connected
     *
     * @param session
     */
    @OnWebSocketConnect
    public void addWebSocketClient(Session session) {
        logger.info("got new client {}",session);
        sessions.add(session);


    }

    /**
     * the given client left
     *
     * @param session
     */
    @OnWebSocketClose
    public void delWebSocketClient(Session session) {
        logger.info("lost client {}",session);

        sessions.remove(session);
    }

    /**
     * will be triggered if a new message arrives from a client
     *
     * @param sess
     * @param message
     */
    public abstract void onWebSocketMessage(Session sess, String message);

    public boolean serverIsStopped() {
        return this.server.isStopped();
    }
    public void broadCast(String message) {
        for (Session session : this.sessions) {
            try {
                session.getRemote().sendString(message);
            } catch (org.eclipse.jetty.io.EofException ex) {
                //Nothing here
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    public static class Pinger extends Thread {
        private final WebSocketService socket;

        public Pinger(WebSocketService socket) {
            this.socket = socket;
        }

        @Override
        public void run() {

            while (!socket.serverIsStopped()) {
                socket.ping();;
                /*for (Session s : socket.sessions) {
                    try {
                        s.getRemote().sendString();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }


                 */
                try {
                    Thread.sleep(30000l);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void ping() {
        broadCast(new JSONObject().put("type", "ping").toString());
    }
}
