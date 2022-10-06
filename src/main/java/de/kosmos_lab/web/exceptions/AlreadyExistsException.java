package de.kosmos_lab.web.exceptions;

import de.kosmos_lab.web.annotations.responses.ApiResponse;
import de.kosmos_lab.web.doc.openapi.ResponseCode;
import de.kosmos_lab.web.server.WebServer;

@ApiResponse(responseCode = @ResponseCode(statusCode = WebServer.STATUS_CONFLICT), description = "The resource already exists")
public class AlreadyExistsException extends ServletException {
    public AlreadyExistsException(String message) {
        super(message);
    }
}
