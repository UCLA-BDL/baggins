package edu.ucla.cs.baggins.data.provider.model.base.exceptions;

/**
 * Created by Ethan L. Schreiber on 3/2/16.
 */
public class CardinalityException extends RuntimeException {
    public CardinalityException(int expected, int returned) {
        super("Query expected " + expected + " results but got " + returned +".");
    }
}
