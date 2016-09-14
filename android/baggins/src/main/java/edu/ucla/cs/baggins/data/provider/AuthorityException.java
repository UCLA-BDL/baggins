package edu.ucla.cs.baggins.data.provider;

/**
 * Created by Ethan L. Schreiber on 4/21/16.
 *
 * Thrown when there is a problem with the ContentProvider Authority
 */
public class AuthorityException extends RuntimeException {
    public AuthorityException(String detailMessage) {
        super(detailMessage);
    }
}
