package edu.ucla.cs.baggins.data.provider.model.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;

import edu.ucla.cs.baggins.data.provider.model.gson.serializer.BooleanSerializer;
import edu.ucla.cs.baggins.data.provider.model.gson.serializer.UTCSerializer;

/**
 * Configures the Gson java object to JSON parser.
 * It registers adapters to convert java.util.Date and Android.latLng
 * to their JSON representations.
 * <p/>
 * This is a singleton class, call get() to get the single instance.
 */
// TODO: Singleton can cause problems. Consider storing this Gson object with in the global Application context.
public class GsonFactory {

    public static final String TAG = "gson_factory";

    /**
     * The singleton with
     */
    private static Gson sGson = null;

    private static GsonBuilder getBaseGsonBuilder() {

        final GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.registerTypeAdapter(boolean.class, new BooleanSerializer());
        gsonBuilder.registerTypeAdapter(Boolean.class, new BooleanSerializer());
        gsonBuilder.registerTypeAdapter(Date.class, new UTCSerializer());

        return gsonBuilder;
    }

    /**
     * @return a singleton with of the Gson class.
     */
    public static Gson get() {
        if (sGson == null) {
            sGson = getBaseGsonBuilder().create();
        }
        return sGson;
    }

    /**
     * Given a json string, return a key/value map. Not sure if this will
     * work for nested JSON.
     * See:
     * http://stackoverflow.com/questions/2779251/how-can-i-convert-json-to-a-hashmap-using-gson
     *
     * @param json The json string
     * @return The key/value map
     */
    public static Map<String, String> getMap(String json) {
        Type type = new TypeToken<Map<String, String>>() {
        }.getType();
        return get().fromJson(json, type);

    }

    /**
     * Given some json and a key, returns it's value.
     *
     * @param json The json string
     * @param key  The key to get the value of
     * @return THe value, or null if it does not exist
     */
    public static String getValue(String json, String key) {
        return getMap(json).get(key);
    }

    /**
     * Given some json and a key, returns as a long.
     *
     * @param json The json string
     * @param key  The key to get the value of
     * @return The value, or defaultValue if it does not exist
     */
    public static long getValueLong(String json, String key, long defaultValue) {
        String value = getMap(json).get(key);

        if (value == null) {
            value = Long.toString(defaultValue);
        }

        return Long.parseLong(value);
    }

}
