package de.kosmos_lab.web.server.servlets.openapi;

import de.kosmos_lab.web.annotations.Operation;
import de.kosmos_lab.web.annotations.responses.ApiResponse;
import de.kosmos_lab.web.doc.openapi.ApiEndpoint;
import de.kosmos_lab.web.doc.openapi.ResponseCode;
import de.kosmos_lab.web.exceptions.ParameterNotFoundException;
import de.kosmos_lab.web.server.AsyncApiParser;
import de.kosmos_lab.web.server.WebServer;
import de.kosmos_lab.web.server.servlets.BaseServlet;
import de.kosmos_lab.web.server.servlets.BaseServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;


@ApiEndpoint(path = "/doc/asyncapi.yaml", userLevel = -1)
public class AsyncApiYamlServlet extends BaseServlet {

    public AsyncApiYamlServlet(WebServer webServer) {
        super(webServer);


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


        String host = null;
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
            sendTextAs(request, response, server.replaceHostName(server.getAsyncApiParser().getCachedYAML(),host),"text/x-yaml");

        }
        else {
            sendTextAs(request, response, server.getAsyncApiParser().getCachedYAML(),"text/x-yaml");

        }


    }

}

