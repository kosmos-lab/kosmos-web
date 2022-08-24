package de.kosmos_lab.web.server;

import org.eclipse.jetty.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.websocket.server.JettyServerUpgradeResponse;
import org.eclipse.jetty.websocket.server.JettyWebSocketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketCreator implements JettyWebSocketCreator {
    private static final Logger logger = LoggerFactory.getLogger("KosmosWebSocketCreator");

    private WebSocketService binaryService;
    private WebSocketService textService;

    public WebSocketCreator(WebSocketService textService, WebSocketService binaryService) {
        // Create the reusable sockets
        this.textService = textService;
        this.binaryService = binaryService;
    }

    @Override
    public Object createWebSocket(JettyServerUpgradeRequest req, JettyServerUpgradeResponse resp) {
        //logger.info("creating websocket for {} and {}",textService,binaryService);
        for (String subprotocol : req.getSubProtocols()) {
            logger.info("found subprotocol {}",subprotocol);
            if ("binary".equals(subprotocol)) {
                resp.setAcceptedSubProtocol(subprotocol);
                return binaryService;
            }
            if ("text".equals(subprotocol))
            {
                resp.setAcceptedSubProtocol(subprotocol);
                return textService;
            }

        }

        // No valid subprotocol in request - happens a lot in testing - just select the first available one
        if (textService != null ) {
            return textService;
        }
        if (binaryService != null) {
            return binaryService;

        }
        return null;
    }
}