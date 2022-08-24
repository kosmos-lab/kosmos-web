package de.kosmos_lab.web.persistence.exceptions;

public class NotFoundInPersistenceException extends Exception {
    public NotFoundInPersistenceException(String message) {
        //super("query: "+query+" "+ SQLPersistence.print(params));
        //super("query: "+SQLPersistence.fullPrint(query,params));
        super(message);
    }
}
