package edu.ucla.cs.baggins.data.provider.util;

import android.content.ContentResolver;
import android.content.UriMatcher;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * This class uses UriMatcher and maps a ContentProvider URI to a few values:
 * - A unique integer code generated by this class.
 *   This uri/code combination is stored in UriMatcher
 * -
 * Created by Ethan L. Schreiber on 4/6/16.
 */
public class ContentProviderMatcher {

    /**
     * The Uri Matcher
     */
    UriMatcher mMatcher;

    private int mMatchCode;

    private final String AUTHORITY;

    private Map<Integer, UriData> mPathDataMap;

    private class UriData {
        public String tableName;
        public String primaryKey;
        public String type;

        public UriData(String tableName, String primaryKey, String type) {
            this.tableName = tableName;
            this.primaryKey = primaryKey;
            this.type = type;
        }
    }

    public ContentProviderMatcher(String authority) {
        mMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mMatchCode = 0;
        AUTHORITY = authority;
        mPathDataMap = new HashMap<>();

    }

    private int nextMatchCode() {
        return mMatchCode++;
    }

    /**
     * Helper method for each class.
     *
     * @param tableName Given the table name
     * @return the dir mime type.
     */
    public String getContentDirType(String tableName) {
        return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + AUTHORITY + "." + tableName;
    }

    /**
     * Helper method for each class.
     *
     * @param tableName Given the table name,
     * @return the item mime type.
     */
    public String getContentItemType(String tableName) {
        return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + AUTHORITY + "." + tableName;
    }


    /**
     * Map a uri to a code, contentName and tableName. For example, consider the uri /sync
     *
     * This adds two paths:
     * - /sync   - The directory mime type
     * - /sync/# - The item mime type
     *
     * Both of these paths are linked to the same contentName and tableName, but have different codes
     * asssigned to them. These codes can be reacted to differently later.
     *
     * @param uri The uri. This MUST NOT end in '/' or '#' and must be a valid ContentProvider uri
     * @param contentName
     * @param tableName
     */
    public void addURI(@NonNull String uri, String contentName, String tableName, String primaryKey) {

        if (uri.endsWith("#") || uri.endsWith(File.pathSeparator)) {
            throw new RuntimeException("Invalid uri: " + uri);
        }

        // Add the dir type uri
        int code = nextMatchCode();
        mMatcher.addURI(AUTHORITY, uri, code);
        mPathDataMap.put(code, new UriData(tableName, primaryKey, getContentDirType(contentName)));

        // Add the item type uri
        code = nextMatchCode();
        mMatcher.addURI(AUTHORITY, uri + "/#", code);
        mPathDataMap.put(code, new UriData(tableName, primaryKey, getContentItemType(contentName)));
    }

    protected UriData getUriData(@NonNull Uri uri) {
        int code = mMatcher.match(uri);
        return mPathDataMap.get(code);
    }

    /**
     * Given a uri, tries to match it. If it was registered with addURI, it will match. It returns
     * the type associated with the uri.
     * @param uri The uri to match.
     * @return The type.
     */
    public String matchType(@NonNull Uri uri) {
        UriData data = getUriData(uri);
        if (data == null) {
            return null;
        } else {
            return data.type;
        }
    }


    /**
     * @param uri
     * @return true if this is an item type, i.e. ends with a #, false otherwise.
     */
    public boolean isItemType(@NonNull Uri uri) {
        String type = matchType(uri);
        return (type == null) ? false :
               type.startsWith(ContentResolver.CURSOR_ITEM_BASE_TYPE);
    }


    /**
     * Given a uri, tries to match it. If it was registered with addURI, it will match. It returns
     * the table name associated with the uri.
     * @param uri The uri to match.
     * @return The table name.
     */
    public String matchTable(@NonNull Uri uri) {
        UriData data = getUriData(uri);
        if (data == null) {
            return null;
        } else {
            return data.tableName;
        }
    }

    /**
     * Given a uri, tries to match it. If it was registered with addURI, it will match. It returns
     * the primary key name associated with the uri.
     * @param uri The uri to match.
     * @return The table name.
     */
    public String matchPrimaryKey(@NonNull Uri uri) {
        UriData data = getUriData(uri);
        if (data == null) {
            return null;
        } else {
            return data.primaryKey;
        }
    }
}
