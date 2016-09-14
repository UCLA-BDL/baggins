package edu.ucla.cs.baggins.data.provider.model.base.exceptions;

/**
 * Created by Ethan L. Schreiber on 3/2/16.
 */
public class ObjectNotFoundException extends Exception {
    public ObjectNotFoundException(long objectId) {
        super("There is no object with id " + objectId);
    }
}
