package de.kosmos_lab.web.server.example.servlets.user;

import de.kosmos_lab.web.annotations.Operation;
import de.kosmos_lab.web.annotations.Parameter;
import de.kosmos_lab.web.annotations.enums.ParameterIn;
import de.kosmos_lab.web.annotations.enums.SchemaType;
import de.kosmos_lab.web.annotations.media.Content;
import de.kosmos_lab.web.annotations.media.ExampleObject;
import de.kosmos_lab.web.annotations.media.Schema;
import de.kosmos_lab.web.annotations.responses.ApiResponse;
import de.kosmos_lab.web.data.User;
import de.kosmos_lab.web.doc.openapi.ApiEndpoint;
import de.kosmos_lab.web.doc.openapi.ResponseCode;
import de.kosmos_lab.web.exceptions.LoginFailedException;
import de.kosmos_lab.web.exceptions.ParameterNotFoundException;
import de.kosmos_lab.web.persistence.ISesssionPersistence;
import de.kosmos_lab.web.persistence.IUserPersistence;
import de.kosmos_lab.web.persistence.exceptions.NoPersistenceException;
import de.kosmos_lab.web.server.example.ExampleWebServer;
import de.kosmos_lab.web.server.servlets.BaseServlet;
import de.kosmos_lab.web.server.servlets.BaseServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@ApiEndpoint(path = "/user/login", load = false)
public class LoginServlet extends BaseServlet {
    private static final String FIELD_USER = "username";
    private static final String FIELD_PASS = "password";
    public LoginServlet(ExampleWebServer server) {

        super(server);
    }
    @Operation(

            tags = {"user"},
            summary = "login",
            description = "Used to get a JWT token from the system.\n" +
                    "This token should be included as a header (Authorization) for all other requests.",
            parameters = {
                    @Parameter(
                            name = FIELD_USER,
                            description = "the username of the user",
                            schema = @Schema(
                                    type = SchemaType.STRING
                            ),
                            in = ParameterIn.QUERY,
                            required = true,
                            example = "karl"
                    ),
                    @Parameter(
                            name = FIELD_PASS,
                            description = "the password of the user",
                            schema = @Schema(
                                    type = SchemaType.STRING
                            ),
                            in = ParameterIn.QUERY,
                            required = true,
                            examples = {@ExampleObject(value = "test")}
                    )
            },
            responses = {
                    @ApiResponse(responseCode = @ResponseCode(statusCode = de.kosmos_lab.web.server.WebServer.STATUS_OK), description = "Login successful", content = @Content(mediaType = "application/jwt", schema = @Schema(type = SchemaType.STRING, example = "eyJ0eXBlIjoiSldUIiwiYWxnIjoiSFMyNTYifQ.eyJsZXZlbCI6MSwibmFtZSI6ImphbiIsImV4cCI6MTYwMzQ1NDE4NDY1NSwiaGFzaCI6Ii0ifQ.gAQh1snnG_VlzJ-lv4X7_-A0GV7iQA_l83b1285mPSo"))),
                    @ApiResponse(responseCode = @ResponseCode(statusCode = de.kosmos_lab.web.server.WebServer.STATUS_FORBIDDEN), description = "The credentials did not match"),
                    @ApiResponse(responseCode = @ResponseCode(statusCode = de.kosmos_lab.web.server.WebServer.STATUS_MISSING_VALUE), ref = "#/components/responses/MissingValuesError"),


            }
    )

    public void post(BaseServletRequest request, HttpServletResponse response)
            throws LoginFailedException, ParameterNotFoundException, IOException, NoPersistenceException, NoSuchAlgorithmException, InvalidKeyException {
        ExampleWebServer server = (ExampleWebServer) this.server;
        User user = server.getPersistence(IUserPersistence.class).login(request.getParameter(FIELD_USER, true), request.getParameter(FIELD_PASS, true));


        String jwt = server.getPersistence(ISesssionPersistence.class).getJWT(user);
        sendText(request, response, jwt);


    }
}
