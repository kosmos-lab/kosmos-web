package de.kosmos_lab.web.client.websocket;

import org.json.JSONObject;

public interface WebSocketEventConsumer {

    void parse(WebSocketClientEndpoint client, JSONObject object);
}
