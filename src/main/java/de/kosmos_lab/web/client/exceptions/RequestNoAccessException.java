package de.kosmos_lab.web.client.exceptions;

public class RequestNoAccessException extends RequestWrongStatusExeption {
    
    
    public RequestNoAccessException(int status) {
        super("KosmoS returned an access denied", status);
    }
}
