package edu.ucla.cs.baggins.data.provider;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * General purpose {@link ContentProvider} base class that uses SQLiteDatabase for storage.
 */
public abstract class AbstractSQLiteProvider extends ContentProvider {

    private static final String TAG = "AbstractSQLiteProvider";

    protected SQLiteOpenHelper mOpenHelper;
    private   Set<Uri>         mChangedUris;
    protected SQLiteDatabase   mDb;

    private final        ThreadLocal<Boolean> mApplyingBatch          = new ThreadLocal<Boolean>();
    private static final int                  SLEEP_AFTER_YIELD_DELAY = 4000;

    /**
     * Maximum number of operations allowed in a batch between yield points.
     */
    private static final int MAX_OPERATIONS_PER_YIELD_POINT = 500;

    @Override
    public boolean onCreate() {
        Context context = getContext();
        mOpenHelper = getDatabaseHelper(context);
        mChangedUris = new HashSet<Uri>();
        return true;
    }

    /**
     * Returns a {@link SQLiteOpenHelper} that can open the database.
     */
    public abstract SQLiteOpenHelper getDatabaseHelper(Context context);

    /**
     * The equivalent of the {@link #insert} method, but invoked within a transaction.
     */
    public abstract Uri insertInTransaction(Uri uri, ContentValues values);

    /**
     * The equivalent of the {@link #update} method, but invoked within a transaction.
     */
    public abstract int updateInTransaction(Uri uri, ContentValues values, String selection,
                                            String[] selectionArgs);

    /**
     * The equivalent of the {@link #delete} method, but invoked within a transaction.
     */
    public abstract int deleteInTransaction(Uri uri, String selection, String[] selectionArgs);

    /**
     * Call this to add a URI to the list of URIs to be notified when the transaction
     * is committed.
     */
    protected void postNotifyUri(Uri uri) {
        synchronized (mChangedUris) {
            mChangedUris.add(uri);
        }
    }

    public boolean isCallerSyncAdapter(Uri uri) {
        return false;
    }

    public SQLiteOpenHelper getDatabaseHelper() {
        return mOpenHelper;
    }

    private boolean applyingBatch() {
        return mApplyingBatch.get() != null && mApplyingBatch.get();
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        Uri     result              = null;
        boolean callerIsSyncAdapter = isCallerSyncAdapter(uri);
        boolean applyingBatch       = applyingBatch();
        if (!applyingBatch) {
            mDb = mOpenHelper.getWritableDatabase();
            mDb.beginTransaction();
            try {
                result = insertInTransaction(uri, values);
                mDb.setTransactionSuccessful();
            } finally {
                mDb.endTransaction();
            }

            onEndTransaction(callerIsSyncAdapter);
        } else {
            result = insertInTransaction(uri, values);
        }
        return result;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        int     numValues           = values.length;
        boolean callerIsSyncAdapter = isCallerSyncAdapter(uri);
        mDb = mOpenHelper.getWritableDatabase();
        mDb.beginTransaction();
        try {
            for (ContentValues value : values) {
                insertInTransaction(uri, value);
                mDb.yieldIfContendedSafely();
            }
            mDb.setTransactionSuccessful();
        } finally {
            mDb.endTransaction();
        }

        onEndTransaction(callerIsSyncAdapter);
        return numValues;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int     count               = 0;
        boolean callerIsSyncAdapter = isCallerSyncAdapter(uri);
        boolean applyingBatch       = applyingBatch();
        if (!applyingBatch) {
            mDb = mOpenHelper.getWritableDatabase();
            mDb.beginTransaction();
            try {
                count = updateInTransaction(uri, values, selection, selectionArgs
                                           );
                mDb.setTransactionSuccessful();
            } finally {
                mDb.endTransaction();
            }

            onEndTransaction(callerIsSyncAdapter);
        } else {
            count = updateInTransaction(uri, values, selection, selectionArgs);
        }

        return count;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        int     count               = 0;
        boolean callerIsSyncAdapter = isCallerSyncAdapter(uri);
        boolean applyingBatch       = applyingBatch();
        if (!applyingBatch) {
            mDb = mOpenHelper.getWritableDatabase();
            mDb.beginTransaction();
            try {
                count = deleteInTransaction(uri, selection, selectionArgs);
                mDb.setTransactionSuccessful();
            } finally {
                mDb.endTransaction();
            }

            onEndTransaction(callerIsSyncAdapter);
        } else {
            count = deleteInTransaction(uri, selection, selectionArgs);
        }
        return count;
    }

    @NonNull
    @Override
    public ContentProviderResult[] applyBatch(@NonNull ArrayList<ContentProviderOperation> operations)
    throws OperationApplicationException {
        int     ypCount             = 0;
        int     opCount             = 0;
        boolean callerIsSyncAdapter = false;
        mDb = mOpenHelper.getWritableDatabase();
        mDb.beginTransaction();
        try {
            mApplyingBatch.set(true);
            final int                     numOperations = operations.size();
            final ContentProviderResult[] results       = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                if (++opCount >= MAX_OPERATIONS_PER_YIELD_POINT) {
                    throw new OperationApplicationException(
                            "Too many content provider operations between yield points. "
                            + "The maximum number of operations per yield point is "
                            + MAX_OPERATIONS_PER_YIELD_POINT, ypCount);
                }
                final ContentProviderOperation operation = operations.get(i);
                if (!callerIsSyncAdapter && isCallerSyncAdapter(operation.getUri())) {
                    callerIsSyncAdapter = true;
                }
                if (i > 0 && operation.isYieldAllowed()) {
                    opCount = 0;
                    if (mDb.yieldIfContendedSafely(SLEEP_AFTER_YIELD_DELAY)) {
                        ypCount++;
                    }
                }
                results[i] = operation.apply(this, results, i);
            }
            mDb.setTransactionSuccessful();
            return results;
        } finally {
            mApplyingBatch.set(false);
            mDb.endTransaction();
            onEndTransaction(callerIsSyncAdapter);
        }
    }

    protected void onEndTransaction(boolean callerIsSyncAdapter) {
        Set<Uri> changed;
        synchronized (mChangedUris) {
            changed = new HashSet<Uri>(mChangedUris);
            mChangedUris.clear();
        }
        if (getContext() != null) {
            ContentResolver resolver = getContext().getContentResolver();
            for (Uri uri : changed) {
                boolean syncToNetwork = !callerIsSyncAdapter && syncToNetwork(uri);
                resolver.notifyChange(uri, null, syncToNetwork);
            }
        } else {
            Log.w(TAG,"AbstractSQLiteProvider getContext() returns null.");
        }
    }

    protected boolean syncToNetwork(Uri uri) {
        return false;
    }
}
