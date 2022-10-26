package de.kosmos_lab.web.client.websocket.handlers;

import jakarta.websocket.CloseReason;

public interface IOnDisconnected {

    void onDisconnected(CloseReason closeReason);
}
