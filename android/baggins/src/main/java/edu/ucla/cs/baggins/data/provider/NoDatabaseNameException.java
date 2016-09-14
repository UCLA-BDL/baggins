package edu.ucla.cs.baggins.data.provider;

/**
 * Created by Ethan L. Schreiber on 5/3/16.
 */
public class NoDatabaseNameException extends RuntimeException {
    public NoDatabaseNameException(String detailMessage) {
        super(detailMessage);
    }
}
