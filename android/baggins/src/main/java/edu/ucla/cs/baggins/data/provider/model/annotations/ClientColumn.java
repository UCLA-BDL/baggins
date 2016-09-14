package edu.ucla.cs.baggins.data.provider.model.annotations;

import android.text.TextUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by Ethan L. Schreiber on 4/28/16.
 */

@Retention(RetentionPolicy.RUNTIME) // Can be used reflectively
@Target(ElementType.FIELD) // can use for field only
public @interface ClientColumn {


    String NOT_SET_STRING      = "NOT SET";
    String LIST_OF_KEYS_STRING = "LIST_OF_KEYS";

    public enum Type {
        NOT_SET(NOT_SET_STRING), LIST_OF_KEYS(LIST_OF_KEYS_STRING);

        /**
         * Store the string version of the Type enum.
         */
        private String mValue;

        Type(String value) {
            mValue = value;
        }

        /**
         * Convert from the string version to the enum version
         *
         * @param typeString The string
         * @return the PostType
         * @throws RuntimeException if the postTypeString is not a valid PostType string.
         */
        public static Type toType(String typeString) {
            Collection<String> typeStrings = new ArrayList<>();

            for (Type type : Type.values()) {
                if (type.toString().equals(typeString)) {
                    return type;
                }
                typeStrings.add(type.toString());
            }
            throw new RuntimeException("[" + typeString + "] is not a valid Type string. It must be one of {"
                                       + TextUtils.join(",", typeStrings) + "}");
        }

        /**
         * @return The string version of the PostType.
         */
        public String toString() {
            return mValue;
        }
    }

    String value();

    String type() default NOT_SET_STRING;
}
