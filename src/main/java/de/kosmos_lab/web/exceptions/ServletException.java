package de.kosmos_lab.web.exceptions;

import de.kosmos_lab.web.annotations.responses.ApiResponse;
import de.kosmos_lab.web.doc.openapi.ResponseCode;
import de.kosmos_lab.web.server.WebServer;

@ApiResponse(responseCode = @ResponseCode(statusCode = WebServer.STATUS_ERROR), description = "ran into an error while executing the result")
public class ServletException extends  Exception{

    public ServletException(String message) {
        super(message);
    }
}
