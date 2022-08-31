package de.kosmos_lab.web.exceptions;

import de.kosmos_lab.web.annotations.responses.ApiResponse;
import de.kosmos_lab.web.doc.openapi.ResponseCode;
import de.kosmos_lab.web.server.WebServer;

@ApiResponse(responseCode = @ResponseCode(statusCode = WebServer.STATUS_NO_AUTH), description = "You're not authorized! You need to provide authentication")
public class UnauthorizedException extends ServletException {
    public UnauthorizedException() {
        super(UnauthorizedException.class.getAnnotation(ApiResponse.class).description());
    }

    public UnauthorizedException(String message) {
        super(message);
    }
}
