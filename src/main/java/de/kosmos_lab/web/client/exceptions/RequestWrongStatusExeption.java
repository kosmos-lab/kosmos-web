package de.kosmos_lab.web.client.exceptions;

public class RequestWrongStatusExeption extends RequestFailedException {
    public final int status;
    
    public RequestWrongStatusExeption(int status) {
        super("Request failed - got Status:" + status);
        this.status = status;
    }
    
    public RequestWrongStatusExeption(String text, int status) {
        super("HTTP Error:" + status + " " + text);
        this.status = status;
    }
}
