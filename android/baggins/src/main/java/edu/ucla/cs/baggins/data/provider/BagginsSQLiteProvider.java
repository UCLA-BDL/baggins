package edu.ucla.cs.baggins.data.provider;

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import edu.ucla.cs.baggins.data.provider.model.base.BagginsDomainModel;
import edu.ucla.cs.baggins.data.provider.util.ContentProviderMatcher;
import edu.ucla.cs.baggins.util.StaticUtil;

public class BagginsSQLiteProvider extends AbstractSQLiteProvider {

    private final static String TAG = "sqlite_content_provider";

    /**
     * A parameter for use when querying any table that allows specifying a limit on the number
     * of rows returned.
     */
    public final static String LIMIT = "limit";

    /**
     * Group by a column in a query
     */
    public final static String GROUP_BY = "groupBy";


    private static ContentProviderMatcher URI_MATCHER;

    static {

//        URI_MATCHER.addURI("/sync", BaggingContract.Sync.CONTENT_NAME, SYNC_TABLE, BagginsContract.Sync._ID);

    }

    protected ContentProviderMatcher matcher() {
        if (URI_MATCHER == null) {
            URI_MATCHER = new ContentProviderMatcher(BagginsContract.getAuthority());


            Log.i(TAG,"Listing URI Matcher classes");
            try {
                Log.i(TAG,"Call getAllModelClasses from BaggisnSQLIiteProvider");
                for (Class<BagginsDomainModel<?>> modelClass : StaticUtil.getAllModelClasses(getContext())) {

                    BagginsDomainModel<?> model = modelClass.newInstance();
                    URI_MATCHER.addURI("/" + model.getContentProviderTableName(),   // URI
                                       model.getContentProviderTableName(),         // Content Provider Name
                                       model.getContentProviderTableName(),         // Table Name
                                       model._ID);                                  // Primary key

                    Log.i(TAG,"Class: " + modelClass.getName() + "  Content URI: " + model.getContentUri());
                }
            } catch (Exception e) {
                Log.e(TAG,"Error: " + e);
            }
        }

        return URI_MATCHER;
    }

    @Override
    @Nullable
    public String getType(@NonNull Uri uri) {
        return matcher().matchType(uri);
    }

    // ------------------------------------------------------------------------
    // Database Helper - For creating and updating Schema
    // ------------------------------------------------------------------------
    private ProviderDatabaseHelper mOpenHelper;

    @Override
    public SQLiteOpenHelper getDatabaseHelper(Context context) {
        synchronized (this) {
            if (mOpenHelper == null) {

                try {
                    Log.i(TAG, "Package: " + context.getPackageName());
                    Log.i(TAG, "Class  : " + this.getClass().getCanonicalName());
                    ComponentName componentName = new ComponentName(context.getPackageName(), this.getClass().getCanonicalName());
                    ProviderInfo  providerInfo  = context.getPackageManager().getProviderInfo(componentName, PackageManager.GET_META_DATA);
                    Log.i(TAG, "AI: " + providerInfo);
                    Log.i(TAG, "Metadata: " + providerInfo.metaData);
                    String databaseName = providerInfo.metaData.getString("SQLiteDatabaseName");
                    if (databaseName == null) {
                        throw new NoDatabaseNameException("You must create meta-data within the <provider/> for BagginsSQLiteProvider in AndroidManifest.xml. e.g.: " + "\n" +
                                                          "<meta-data android:name=\"SQLiteDatabaseName\" android:value=\"<sqlite_db_name\" />");
                    }
                    Log.i(TAG, "DB Name: " + databaseName);
                    mOpenHelper = new ProviderDatabaseHelper(context, databaseName);

                } catch (PackageManager.NameNotFoundException e) {
                    throw new NoDatabaseNameException("You must create meta-data within the <provider/> for BagginsSQLiteProvider in AndroidManifest.xml. e.g.: " + "\n" +
                                                      "<meta-data android:name=\"SQLiteDatabaseName\" android:value=\"<sqlite_db_name\" />");
                }
            }
            return mOpenHelper;
        }
    }

    // ------------------------------------------------------------------------
    // INSERT, UPDATE, DELETE and QUERY. These are all called from the parent
    // class SQLLiteContentProvider. That class opens a transaction and then
    // calls one of these methods.
    // ------------------------------------------------------------------------

    @Override
    /**
     * Note that this version does a SQLite replace instead of insert. This is update or insert
     */
    public Uri insertInTransaction(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();


        String table = matcher().matchTable(uri);
        long   id    = db.replaceOrThrow(table, null, values);
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int updateInTransaction(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db         = mOpenHelper.getWritableDatabase();
        String               table      = matcher().matchTable(uri);
        String               primaryKey = matcher().matchPrimaryKey(uri);

        if (matcher().isItemType(uri)) {
            selection = DatabaseUtils.concatenateWhere(selection, table + "." + primaryKey + " = ?");
            selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{Long.toString(ContentUris.parseId(uri))});
        }
        return db.update(table, values, selection, selectionArgs);
    }

    @Override
    public int deleteInTransaction(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db         = mOpenHelper.getWritableDatabase();
        String               table      = matcher().matchTable(uri);
        String               primaryKey = matcher().matchPrimaryKey(uri);

        if (matcher().isItemType(uri)) { // If this is an item, add the selection.
            selection = DatabaseUtils.concatenateWhere(selection, table + "." + primaryKey + " = ?");
            selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{Long.toString(ContentUris.parseId(uri))});
        }
        return db.delete(table, selection, selectionArgs);
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final SQLiteDatabase db         = mOpenHelper.getReadableDatabase();
        final String         limit      = uri.getQueryParameter(BagginsContract.QueryParameters.LIMIT);
        final String         groupBy    = uri.getQueryParameter(BagginsContract.QueryParameters.GROUP_BY);
        String               table      = matcher().matchTable(uri);
        String               primaryKey = matcher().matchPrimaryKey(uri);


        if (matcher().isItemType(uri)) {  // If this is an item, add the selection.
            selection = DatabaseUtils.concatenateWhere(selection, table + "." + primaryKey + " = ?");
            selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs, new String[]{Long.toString(ContentUris.parseId(uri))});
        }

        Log.i(TAG, "URI: " + uri);
        Log.i(TAG, "TABLE: " + table);
        if (projection != null) {
            Log.i(TAG, "Projection: " + TextUtils.join(",", projection));
        }
        Log.i(TAG, "Selection: " + selection);
        if (selectionArgs != null) {
            Log.i(TAG, "Selection Args: " + TextUtils.join(",", selectionArgs));
        }

        final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(table);
        Cursor cursor = qb.query(db, projection, selection, selectionArgs, groupBy,
                                 null, sortOrder, limit);

//        if (getContext() != null) {
//            cursor.setNotificationUri(getContext().getContentResolver(), BagginsContract.getAuthorityURI());
//        }
        return cursor;
    }

    @Override
    public boolean isCallerSyncAdapter(Uri uri) {
        return uri.getBooleanQueryParameter(edu.ucla.cs.baggins.data.provider.BagginsContract.QueryParameters.CALLER_IS_SYNCADAPTER, false);
    }
}
