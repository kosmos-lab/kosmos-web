package de.kosmos_lab.web.persistence.exceptions;

public abstract class AlreadyExistsException extends Exception {
    
    
    public AlreadyExistsException(String message) {
        super(message);
    }
}
