package de.kosmos_lab.web.client.exceptions;

public class RequestConflictException extends RequestWrongStatusExeption {
    
    
    public RequestConflictException(int status) {
        super("KosmoS returned a Conflict", status);
    }
}
