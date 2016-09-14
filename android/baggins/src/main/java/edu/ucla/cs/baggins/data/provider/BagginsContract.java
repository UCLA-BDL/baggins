package edu.ucla.cs.baggins.data.provider;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.StringRes;

/**
 * The contract for the content provider
 */
public class BagginsContract {
    private BagginsContract() {
    }

    /**
     * Authority for the content provider
     */
    private static String AUTHORITY;

    /**
     * The uri corresponding to the content provider AUTHORITY.
     * <p/>
     * This is in practice: "content://" + AUTHORITY;
     */
    private static Uri AUTHORITY_URI;

    /**
     * You must call this once when you load your app. This should be set as the package of your app.
     *
     * @param context   An Activity context.
     * @param stringId The int id from the String file. This should be something like R.string.authority.
     *                 The authority is set in the strings file and can be accessed everywhere.
     */
    public static void setAuthority(Context context, @StringRes int stringId) {
        String authority = context.getResources().getString(stringId);

        if (AUTHORITY == null) {
            AUTHORITY = authority;

            AUTHORITY_URI = new Uri.Builder()
                    .scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(getAuthority())
                    .build();
        } else if (!authority.equals(AUTHORITY)) {
            throw new AuthorityException("You cannot change the BagginsContract.AUTHORITY\n" +
                                         "Was: " + AUTHORITY + "\n New Authority Request: " + authority);
        }
    }

    private static void checkAuthority() throws AuthorityException {
        if (AUTHORITY == null) {
            throw new AuthorityException("Error: The authority needs to be set. call BagginsContract.setAuthority(String)");
        }
    }

    /**
     * @return The AUTHORITY for the ContentProvider associated with this contract
     */
    public static String getAuthority() throws AuthorityException {
        checkAuthority();
        return AUTHORITY;
    }

    /**
     * @return The Uri to the ContentProvider Authority.
     */
    public static Uri getAuthorityURI() {
        checkAuthority();
        return AUTHORITY_URI;
    }


    public final static class QueryParameters {
        /**
         * An optional insert, update or delete URI parameter that allows the caller
         * to specify that it is a sync adapter. The default value is false. If true
         * the dirty flag is not automatically set and the "syncToNetwork" parameter
         * is set to false when calling
         * {@link ContentResolver#notifyChange(android.net.Uri, android.database.ContentObserver, boolean)}.
         */
        public final static String CALLER_IS_SYNCADAPTER = "caller_is_syncadapter";

        /**
         * A parameter for use when querying any table that allows specifying a limit on the number
         * of rows returned.
         */
        public final static String LIMIT = "limit";


        public final static String GROUP_BY = "groupBy";
    }

    // /\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\
    // ------------------------------------------------------------------------
    // Definition of Tables
    // ------------------------------------------------------------------------
    // /\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\

    /**
     * Helper method for each class.
     *
     * @param tableName Given the table name
     * @return The content:// style URI
     */
    public static Uri getContentUri(String tableName) {
        return Uri.withAppendedPath(AUTHORITY_URI, tableName);
    }

    /**
     * Helper method for each class.
     *
     * @param tableName Given the table name
     * @return The content:// style URI
     */
    public static Uri getContentUri(String tableName, long id) {
        return Uri.withAppendedPath(getContentUri(tableName), Long.toString(id));
    }


    /**
     * These are columns that every table has.
     */
    public interface BaseColumns {

        /**
         * The unique ID for a row.
         * <P>Type: INTEGER (long)</P>
         */
        String _ID = "_id";

        /**
         * The status of this row:
         * <p/>
         * status values:
         * <P>'I' - Row was inserted at touched_time</P>
         * <P>'U' - Row was updated at touched_time</P>
         * <P>'D' - Row was deleted at touched_time</P>
         * <P>Type: STRING </P>
         */

        String STATUS = "status";

        /**
         * Timestamp (milliseconds since epoch) of when this startRecording was last updated.
         * <P>Type: INTEGER </P>
         */
        String TOUCHED_TIME = "touched_time";
    }

// ------------------------------------------------------------------------
// SyncModel Table
// ------------------------------------------------------------------------


    public final static class Sync implements BaseColumns {
        private Sync() {
        }

        public final static String CONTENT_NAME = "sync";
    }
}
