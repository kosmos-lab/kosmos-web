package de.kosmos_lab.web.client.websocket;

import de.kosmos_lab.web.client.websocket.handlers.IOnConnected;
import de.kosmos_lab.web.client.websocket.handlers.IOnDisconnected;
import de.kosmos_lab.web.client.websocket.handlers.IOnMessage;
import jakarta.websocket.*;
import org.eclipse.jetty.util.IO;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ClientEndpoint
public class SimpleWebSocketEndpoint  {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("SimpleWebSocketEndpoint");

    Session userSession = null;
    private MessageHandler messageHandler;

    public SimpleWebSocketEndpoint(URI endpointURI) {
        try {
            logger.info("creating new SimpleWebSocketEndpoint for {}",endpointURI.toString());
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
            Endpoint endpoint = new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig endpointConfig) {
                    userSession = session;
                    triggerOnOpen();
                }
            };


            Session session = container.connectToServer(endpoint, cec, endpointURI);
            session.addMessageHandler(String.class, text -> onIncomingMessage(text));


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
    private void triggerOnMessage(CloseReason closeReason) {
        for ( IOnDisconnected handler : this.handlerOnDisconnect) {
            handler.onDisconnected(closeReason);
        }
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
    public void addHandler(IOnDisconnected handler) {
        this.handlerOnDisconnect.add(handler);
    }
    ConcurrentHashMap<String, Queue<MessageHandler>> mapMessageHandler = new ConcurrentHashMap<>();
    ConcurrentHashMap<Pattern, Queue<RegexMessageHandler>> getMapMessageHandlerRegex = new ConcurrentHashMap<>();
    public void addMessageHandler(String message, MessageHandler messageHandler) {
        Queue<MessageHandler> list = mapMessageHandler.get(message);
        if (list == null) {
            list = new ConcurrentLinkedQueue();
            this.mapMessageHandler.put(message, list);
        }

        list.add(messageHandler);

    }

    public void addMessageHandler(Pattern message, RegexMessageHandler messageHandler) {
        Queue<RegexMessageHandler> list = getMapMessageHandlerRegex.get(message);
        if (list == null) {
            list = new ConcurrentLinkedQueue();
            this.getMapMessageHandlerRegex.put(message, list);
        }

        list.add(messageHandler);

    }


    @OnOpen
    public void onOpen(Session session) {
        logger.info("opening websocket");
        this.userSession = userSession;
        triggerOnOpen();
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param userSession the userSession which is getting closed.
     * @param reason      the reason for connection close
     */

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        logger.info("closing websocket");
        this.userSession = null;
        triggerOnMessage(reason);

    }

    /**
     * Callback hook for Message Events. This method will be invoked when a client send a message.
     *
     * @param message The text message
     */
    @OnMessage
    public void onIncomingMessage(String message) {
        logger.info("got message {}",message);
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
        for (Entry<Pattern, Queue<RegexMessageHandler>> queueEntry : getMapMessageHandlerRegex.entrySet()) {
            try {
                Matcher m = queueEntry.getKey().matcher(message);
                if (m.matches()) {
                    for (RegexMessageHandler messageHandler : queueEntry.getValue()) {
                        messageHandler.handleMessage(message,m);
                    }
                    continue;
                }


            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }
        if (this.messageHandler != null) {
            this.messageHandler.handleMessage(message);
        }
        triggerOnMessage(message);

    }

    @OnMessage
    public void onMessage(ByteBuffer bytes) {
        logger.info("Handle byte buffer");
    }


    /**
     * register message handler
     *
     * @param msgHandler
     */
    public void addMessageHandler(MessageHandler msgHandler) {

        this.messageHandler = msgHandler;
    }

    /**
     * Send a message.
     *
     * @param message
     */
    public void sendMessage(String message) {

        this.userSession.getAsyncRemote().sendText(message);
    }

    public static interface MessageHandler {

        public void handleMessage(String message);
    }
    public static interface RegexMessageHandler {

        public void handleMessage(String message, Matcher matcher);
    }
    public void close() throws IOException {
        this.userSession.close();
    }
}