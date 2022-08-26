package de.kosmos_lab.web.server.servlets.openapi;

import de.kosmos_lab.web.annotations.Operation;
import de.kosmos_lab.web.annotations.responses.ApiResponse;
import de.kosmos_lab.web.doc.openapi.ApiEndpoint;
import de.kosmos_lab.web.doc.openapi.ResponseCode;
import de.kosmos_lab.web.server.AsyncApiParser;
import de.kosmos_lab.web.server.OpenApiParser;
import de.kosmos_lab.web.server.WebServer;
import de.kosmos_lab.web.server.servlets.BaseServlet;
import de.kosmos_lab.web.server.servlets.BaseServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;


@ApiEndpoint(path = "/doc/asyncapi.yaml", userLevel = -1)
public class AsyncApiServlet extends BaseServlet {
    static AsyncApiParser parser = null;
    public String cached = null;

    public AsyncApiServlet(WebServer webServer) {
        super(webServer);
        if (parser == null) {
            parser = new AsyncApiParser(webServer);
        }

    }

    @Operation(
            tags = {"asyncApi"},
            summary = "asyncapi.yaml",
            description = "The generated asyncApi specification for this service in YAML format",
            responses = {
                    @ApiResponse(responseCode = @ResponseCode(statusCode = WebServer.STATUS_OK), description = "The generated asyncApi specification for this service"),
            }
    )
    @Override
    public void get(BaseServletRequest request, HttpServletResponse response)
            throws IOException {
        if (cached == null) {


            cached = parser.getYAML();

        }
        sendText(request, response, cached);


    }

}

