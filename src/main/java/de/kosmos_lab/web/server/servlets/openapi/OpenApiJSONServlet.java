package de.kosmos_lab.web.server.servlets.openapi;

import de.kosmos_lab.web.annotations.Operation;
import de.kosmos_lab.web.annotations.responses.ApiResponse;
import de.kosmos_lab.web.doc.openapi.ApiEndpoint;
import de.kosmos_lab.web.doc.openapi.ResponseCode;
import de.kosmos_lab.web.server.WebServer;
import de.kosmos_lab.web.server.servlets.BaseServlet;
import de.kosmos_lab.web.server.servlets.BaseServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import static de.kosmos_lab.web.server.servlets.openapi.OpenApiYamlServlet.parser;


@ApiEndpoint(path = "/doc/openapi.json", userLevel = -1)
public class OpenApiJSONServlet extends BaseServlet {
    public String cached = null;

    public OpenApiJSONServlet(WebServer webServer) {
        super(webServer);

    }

    @Operation(
            tags = {"OpenApi"},
            summary = "openapi.json",
            description = "The generated openApi specification for this service in JSONObject format",
            responses = {
                    @ApiResponse(responseCode = @ResponseCode(statusCode = de.kosmos_lab.web.server.WebServer.STATUS_OK), description = "The generated openApi specification for this service"),
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

