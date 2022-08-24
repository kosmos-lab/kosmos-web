package de.kosmos_lab.web.client.exceptions;

public class RequestFailedException extends Exception {
    
    public RequestFailedException(Exception ex) {
        super(ex);
    }
    
    
    public RequestFailedException(String text) {
        super(text);
        
    }
    
    
}