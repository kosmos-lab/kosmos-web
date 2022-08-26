package de.kosmos_lab.web.server.example.servlets.user;

import de.kosmos_lab.web.annotations.Operation;
import de.kosmos_lab.web.annotations.responses.ApiResponse;
import de.kosmos_lab.web.doc.openapi.ApiEndpoint;
import de.kosmos_lab.web.doc.openapi.ResponseCode;
import de.kosmos_lab.web.server.example.ExampleAuthedServlet;
import de.kosmos_lab.web.server.example.ExampleWebServer;
import de.kosmos_lab.web.server.servlets.BaseServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@ApiEndpoint(path = "/user/isAdmin",load = false, userLevel = 100)

public class AdminViewServlet extends ExampleAuthedServlet {
    public AdminViewServlet(ExampleWebServer webServer, int level) {
        super(webServer, level);
    }

    @Operation(
            tags = {"user"},
            summary = "isAdmin",

            description = "Test servlet to check if access levels are parsed correctly",
            responses = {
                    @ApiResponse(responseCode = @ResponseCode(statusCode = de.kosmos_lab.web.server.WebServer.STATUS_OK), description = "User has Access")
            }

    )
    public void get(BaseServletRequest request, HttpServletResponse response) throws IOException {
        sendText(request, response, "Hello - you are worthy of seeing this!");
        
    }
    
}
