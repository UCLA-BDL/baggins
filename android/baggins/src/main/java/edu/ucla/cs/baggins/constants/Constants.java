package edu.ucla.cs.baggins.constants;

import android.os.Environment;

/**
 * Created by Ethan L. Schreiber on 4/12/16.
 */
public class Constants {

    public final static String BAGGINS_DIR     = Environment.getExternalStorageDirectory() + "/baggins";
    public final static String IMAGE_DIRECTORY = BAGGINS_DIR + "/image";
    public final static String CACHE_DIR       = BAGGINS_DIR + "/cache";
    public final static int    UNSET_INT       = Integer.MIN_VALUE;
    public final static long   UNSET_LONG      = Long.MIN_VALUE;

    public final static String REST_HOST = "http://www.resthost.com";

    /**
     * Prefix is between REST_HOST and the rest of the rest call. It can be used for things
     * like debug or versions
     */
    public final static String REST_VERSION = "";

    /**
     * The base uri for all REST calls.
     */
    public final static String REST_URI_PREFIX = REST_HOST + REST_VERSION;

    /**
     * Network connection timeout, in milliseconds.
     */
    public final static int NET_CONNECT_TIMEOUT_MILLIS = 15000;  // 15 seconds

    /**
     * Network read timeout, in milliseconds.
     */
    public final static int NET_READ_TIMEOUT_MILLIS = 10000;  // 10 seconds

    /**
     * Sync interval for the global sync - Once a minute (This is the minimum value)
     */
    public final static long GLOBAL_SYNC_INTERVAL = 60L;

    /**
     * Sync interval for the global sync when not active - Once a day.
     */
    public final static long GLOBAL_SYNC_INTERVAL_OFFLINE = 60L * 60 * 24;


    // 10 seconds
    public final static long COMMENT_SYNC_INTERVAL = 60L;

    /**
     * This is the auth token key to put in the http header along with the access token.
     */
    public final static String AUTH_TOKEN_KEY = "x-access-token";

    private final static String SYNC_ROUTE = "/sync";


    /**
     * Get the sync after timestamp
     *
     * @return The URI
     */
    public static String sync(long timestamp) {
        return REST_URI_PREFIX + SYNC_ROUTE + "/" + timestamp;
    }

}
