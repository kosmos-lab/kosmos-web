package de.kosmos_lab.web.server.servlets.openapi;

import de.kosmos_lab.web.annotations.Operation;
import de.kosmos_lab.web.annotations.responses.ApiResponse;
import de.kosmos_lab.web.doc.openapi.ApiEndpoint;
import de.kosmos_lab.web.doc.openapi.ResponseCode;
import de.kosmos_lab.web.exceptions.ParameterNotFoundException;
import de.kosmos_lab.web.server.WebServer;
import de.kosmos_lab.web.server.servlets.BaseServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;


@ApiEndpoint(path = "/doc/asyncapi.json", userLevel = -1)
public class AsyncApiJSONServlet extends AsyncApiYamlServlet {

    public AsyncApiJSONServlet(WebServer webServer) {
        super(webServer);

    }

    @Operation(
            tags = {"asyncApi"},
            summary = "asyncapi.json",
            description = "The generated asyncApi specification for this service in JSONObject format",
            responses = {
                    @ApiResponse(responseCode = @ResponseCode(statusCode = WebServer.STATUS_OK), description = "The generated asyncApi specification for this service"),
            }
    )

    public void get(BaseServletRequest request, HttpServletResponse response) throws IOException {


        String host = null;
        boolean export = request.getBoolean("export",false);
        try {
            host = request.getParameter("host",false);
        } catch (ParameterNotFoundException e) {
            throw new RuntimeException(e);
        }
        if ( host == null ) {
            try {
                host = request.getRequest().getHeader("host");
            } catch (Exception ex) {

            }
        }
        if ( host != null ) {
            sendJSON(request, response, server.replaceHostName(server.getAsyncApiParser().getCachedJSON(),host));

        }
        else {
            sendJSON(request, response, server.getAsyncApiParser().getCachedJSON());
        }


    }

}

