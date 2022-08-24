package de.kosmos_lab.web.exceptions;

import de.kosmos_lab.web.annotations.responses.ApiResponse;
import de.kosmos_lab.web.doc.openapi.ResponseCode;
import de.kosmos_lab.web.server.WebServer;

@ApiResponse(responseCode = @ResponseCode(statusCode = WebServer.STATUS_NOT_FOUND), description = "The resource could not be found")
public class NotFoundException extends ServletException {
    public NotFoundException(String message) {
        super(message);
    }
}
