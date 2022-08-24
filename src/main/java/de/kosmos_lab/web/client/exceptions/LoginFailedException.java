package de.kosmos_lab.web.client.exceptions;

public class LoginFailedException extends RequestFailedException {
    
    public LoginFailedException() {
        super("Login to server failed");
    }
}
