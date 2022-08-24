package de.kosmos_lab.web.exceptions;

import de.kosmos_lab.web.annotations.responses.ApiResponse;
import de.kosmos_lab.web.doc.openapi.ResponseCode;
import de.kosmos_lab.web.server.WebServer;

@ApiResponse(responseCode = @ResponseCode(statusCode = WebServer.STATUS_FORBIDDEN), description = "You have no access to this")
public class UnauthorizedException extends ServletException {
    public UnauthorizedException() {
        super("You are not allowed to access this");
    }
    public UnauthorizedException(String message) {
        super(message);
    }
}
