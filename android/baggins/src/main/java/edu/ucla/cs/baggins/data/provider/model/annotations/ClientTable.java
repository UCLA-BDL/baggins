package edu.ucla.cs.baggins.data.provider.model.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Ethan L. Schreiber on 4/28/16.
 */
@Retention(RetentionPolicy.RUNTIME) // Can be used reflectively
@Target(ElementType.TYPE)   // On class level
@Inherited                      // Required
public @interface ClientTable {
    String value();
}
