package de.kosmos_lab.web.server.servlets.openapi;

import de.kosmos_lab.web.annotations.Operation;
import de.kosmos_lab.web.annotations.responses.ApiResponse;
import de.kosmos_lab.web.doc.openapi.ApiEndpoint;
import de.kosmos_lab.web.doc.openapi.ResponseCode;
import de.kosmos_lab.web.server.WebServer;
import de.kosmos_lab.web.server.servlets.BaseServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;


@ApiEndpoint(path = "/doc/asyncapi.json", userLevel = -1)
public class AsyncApiJSONServlet extends AsyncApiYamlServlet {
    public String cached = null;

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
        if (cached == null) {


            cached = parser.getJSON().toString(2);

        }

        String host = null;
        try {
            host = request.getRequest().getHeader("host");
        } catch (Exception ex ) {

        }
        if ( host != null ) {
            sendJSON(request, response, server.replaceHostName(cached,host));

        }
        else {
            sendJSON(request, response, cached);
        }


    }

}

