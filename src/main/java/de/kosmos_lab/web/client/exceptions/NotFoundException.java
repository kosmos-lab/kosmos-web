package de.kosmos_lab.web.client.exceptions;

public class NotFoundException extends RequestWrongStatusExeption {
    
    
    public NotFoundException(int status) {
        super("KosmoS returned a Not Found", status);
    }
}
