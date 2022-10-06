package de.kosmos_lab.web.server.servlets.openapi;

import de.kosmos_lab.web.annotations.Operation;
import de.kosmos_lab.web.annotations.responses.ApiResponse;
import de.kosmos_lab.web.doc.openapi.ApiEndpoint;
import de.kosmos_lab.web.doc.openapi.ResponseCode;
import de.kosmos_lab.web.exceptions.ParameterNotFoundException;
import de.kosmos_lab.web.server.OpenApiParser;
import de.kosmos_lab.web.server.WebServer;
import de.kosmos_lab.web.server.servlets.BaseServlet;
import de.kosmos_lab.web.server.servlets.BaseServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;


@ApiEndpoint(path = "/doc/openapi.yaml", userLevel = -1)
public class OpenApiYamlServlet extends BaseServlet {


    public OpenApiYamlServlet(WebServer webServer) {
        super(webServer);


    }

    @Operation(
            tags = {"OpenApi"},
            summary = "openapi.yaml",
            description = "The generated openApi specification for this service in YAML format",
            responses = {
                    @ApiResponse(responseCode = @ResponseCode(statusCode = de.kosmos_lab.web.server.WebServer.STATUS_OK), description = "The generated openApi specification for this service"),
            }
    )
    @Override
    public void get(BaseServletRequest request, HttpServletResponse response)
            throws IOException {

        String host = request.getParameter("host");;

        if ( host == null ) {
            try {
                host = request.getRequest().getHeader("host");
            } catch (Exception ex) {

            }
        }
        if ( host != null ) {
            sendTextAs(request, response, server.replaceHostName(server.getOpenApiParser().getCachedYAML(),host),"text/x-yaml");

        }
        else {
            sendTextAs(request, response, server.getOpenApiParser().getCachedYAML(),"text/x-yaml");
        }

    }

}

