package edu.ucla.cs.baggins.data.provider.model.annotations;

/**
 * Created by Ethan L. Schreiber on 4/28/16.
 */
public class AnnotationMissingException extends RuntimeException {
    public AnnotationMissingException(String detailMessage) {
        super(detailMessage);
    }
}
