package de.kosmos_lab.web.exceptions;

import de.kosmos_lab.web.annotations.responses.ApiResponse;
import de.kosmos_lab.web.doc.openapi.ResponseCode;
import de.kosmos_lab.web.server.WebServer;

@ApiResponse(responseCode = @ResponseCode(statusCode = WebServer.STATUS_FORBIDDEN), description = "Login was not successful")

public class LoginFailedException extends UnauthorizedException {
    public LoginFailedException() {
        super(LoginFailedException.class.getAnnotation(ApiResponse.class).description());
    }
}
