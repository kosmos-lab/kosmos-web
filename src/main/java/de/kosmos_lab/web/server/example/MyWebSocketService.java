package de.kosmos_lab.web.server.example;


import de.kosmos_lab.web.annotations.enums.SchemaType;
import de.kosmos_lab.web.annotations.media.ExampleObject;
import de.kosmos_lab.web.annotations.media.ObjectSchema;
import de.kosmos_lab.web.annotations.media.Schema;
import de.kosmos_lab.web.annotations.media.SchemaProperty;
import de.kosmos_lab.web.data.IUser;
import de.kosmos_lab.web.doc.openapi.Channel;
import de.kosmos_lab.web.doc.openapi.Message;
import de.kosmos_lab.web.doc.openapi.WebSocketEndpoint;
import de.kosmos_lab.web.exceptions.LoginFailedException;
import de.kosmos_lab.web.persistence.IUserPersistence;
import de.kosmos_lab.web.persistence.exceptions.NoPersistenceException;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;


/**
 * This is the Service used to control the Websocket on /ws
 */
@Message(
        name = "ping",
        payloadRefs = {
                "#/components/schemas/ping"}
)
@Message(
        name = "auth",
        payloadRefs = {
                "#/components/schemas/auth"}
)
@ObjectSchema(
        componentName = "auth",
        properties = {
                @SchemaProperty(name = "type", schema = @Schema(type = SchemaType.STRING, allowableValues = {"auth"}, required = true)),
                @SchemaProperty(name = "username", schema = @Schema(type = SchemaType.STRING, required = true)),
                @SchemaProperty(name = "password", schema = @Schema(type = SchemaType.STRING, required = true))
        }
        ,
        examples = {
                @ExampleObject(
                        name = "login example",
                        value = "{\"type\":\"ping\",\"username\":\"user\",\"password\":\"secret\"}"
                )
        }
)
@ObjectSchema(
        componentName = "ping",
        properties = {
                @SchemaProperty(name = "type",
                        schema = @Schema(
                                type = SchemaType.STRING,
                                allowableValues = {"ping"}
                        )
                )
        }

)


@WebSocketEndpoint(
        path = "/ws",
        load = false,
        channels = @Channel(subscribeRefs = {
                "#/components/messages/ping",
                "#/components/messages/auth"
        })
)
@WebSocket
public class MyWebSocketService extends de.kosmos_lab.web.server.WebSocketService {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MyWebSocketService.class);
    private final Pinger pinger;
    private final ExampleWebServer server;


    ConcurrentHashMap<Session, IUser> mapSessionAuth = new ConcurrentHashMap<>();


    public MyWebSocketService(ExampleWebServer server) {
        super(server);
        this.server = server;


        this.pinger = new Pinger(this);
        this.pinger.start();

    }

    @Override
    @OnWebSocketConnect
    public void addWebSocketClient(Session session) {
        logger.info("got new client {}", session);
        sessions.add(session);


    }

    @Override
    @OnWebSocketClose
    public void delWebSocketClient(Session session) {
        super.delWebSocketClient(session);
        logger.info("lost client {}", session);
        mapSessionAuth.remove(session);
        sessions.remove(session);

    }


    @Override
    @OnWebSocketMessage
    public void onWebSocketMessage(Session session, String message) {
        ExampleWebServer server = (ExampleWebServer) this.server;
        logger.info("got WS message {}", message);
        try {
            JSONObject json = new JSONObject(message);
            if (json.has("type")) {
                String type = json.getString("type");
                if (type.equals("auth")) {
                    IUser u = null;
                    try {
                        try {
                            u = server.getPersistence(IUserPersistence.class).login(json.getString("username"), json.getString("password"));
                        } catch (LoginFailedException e) {

                        }
                        if (u != null) {

                            this.setAuth(session, u);
                            logger.info("user authed to ws");
                            try {
                                session.getRemote().sendString(new JSONObject().put("type", "auth-success").put("value", new JSONObject().put("username", u.getName()).put("level", u.getLevel())).toString());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                session.getRemote().sendString(new JSONObject().put("type", "auth-failed").toString());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (NoPersistenceException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } catch (JSONException ex) {
            logger.error("could not parse JSON: {}", message);
        }
    }

    private void setAuth(Session session, IUser u) {

        mapSessionAuth.put(session, u);

    }


}
