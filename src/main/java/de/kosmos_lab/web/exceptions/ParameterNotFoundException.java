package de.kosmos_lab.web.exceptions;

import de.kosmos_lab.web.server.WebServer;
import de.kosmos_lab.web.annotations.responses.ApiResponse;
import de.kosmos_lab.web.doc.openapi.ResponseCode;

@ApiResponse(responseCode = @ResponseCode(statusCode = WebServer.STATUS_MISSING_VALUE), description = "The request could not be processed, are all required properties/parameters filled?\nSee errormessage for details")
public class ParameterNotFoundException extends ServletException {
    public ParameterNotFoundException(String key) {
        super("could not find parameter " + key);
    }
}
