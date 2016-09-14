package edu.ucla.cs.baggins.data.provider.model.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Ethan L. Schreiber on 4/28/16.
 *
 * This says that the field is a reference to another BagginsDomainModel. value is the name
 * of the reference which can be used by the referenced class to get a list of the classes which
 * reference it.
 */
@Retention(RetentionPolicy.RUNTIME) // Can be used reflectively
@Target(ElementType.FIELD)          // can use for field only
public @interface ReferenceProperty {
    String value();
}
