package edu.ucla.cs.baggins.data.provider.model.base;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.annotations.SerializedName;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.ucla.cs.baggins.data.provider.BagginsContract;
import edu.ucla.cs.baggins.data.provider.model.annotations.AnnotationMissingException;
import edu.ucla.cs.baggins.data.provider.model.annotations.ClientColumn;
import edu.ucla.cs.baggins.data.provider.model.annotations.ClientTable;
import edu.ucla.cs.baggins.data.provider.model.base.exceptions.ObjectNotFoundException;

/**
 * Created by Ethan L. Schreiber on 4/12/16.
 * <p/>
 * parent class for all baggins models. Use this to pass data along the following routes:
 * <p/>
 * Server -> Model using gson mapping. @SerializedName annotations define this mapping.
 * Model -> ContentProvider for storing data in the ContentProvider
 * Model -> Post Params to persist the model the server
 * ContentProvider -> Model to read from the local sqlite database
 * <p/>
 * All Baggins domain models extend this class. There are two ways to use a BagginsDomainModel:
 * <p/>
 * 1. As a domain object
 * Model extends BagginsDomainModel<Model>
 * Model.with(context)
 * .
 */
public abstract class BagginsDomainModel<T extends BagginsDomainModel> {

    public final static String TAG = "baggins_domain_model";

    public final static String _ID    = "_id";
    public final static String STATUS = "_status";

    /**
     * A value for status meaning the domain model was inserted at touched_time.
     */
    public char STATUS_INSERT = 'I';

    /**
     * A value for status meaning the domain model was updated at touched_time.
     */
    public char STATUS_UPDATE = 'U';

    /**
     * A value for status meaning the domain model was deleted at touched_time.
     */
    public char STATUS_DELETE = 'D';

    /**
     * A value for status meaning the domain model is synced with the server.
     */
    public char STATUS_SYNCED = 'S';

    /**
     * The list of ContentProvider Fields cached so it is only constructed once. This is done
     * so that the order remains consistent. In practice, this should happen anyway, but sisnce
     * Class.getDeclaredFields() does not guarantee an order, this guarantees it.
     * <p/>
     * NEVER READ FROM THIS DIRECTLY, use getClientFields()
     */
    private List<Field> mClientFields = null;


    /**
     * Every Baggins model object has a long id
     */
    @ClientColumn(_ID)
    @SerializedName("id")
    public long id;

    /**
     * The status field, which marks whether the field is updated, deleted, inserted. This should
     * contain one of the values:
     * <p/>
     * STATUS_DELETE, STATUS_INSERT, STATUS_SYNCED or STATUS_UPDATE.
     */
    @ClientColumn(STATUS)
    @SerializedName("status")
    public String status;


    /**
     * The context.
     */
    private Context mContext;

    /**
     * The content provider client, used to interact with the ContentProvider tables.
     * This variable should ONLY be accessed through acquireClient, it should never be accessed
     * directly by any other method.
     */
    private ContentProviderClient mClient;

    /**
     * Do we need to acquire a ContentProviderClient? If one is not passed in, then this is true.
     * Use this to know if we have to release the ContentproviderClient when the domain model
     * is released.
     */
    private boolean mAcquireContentProviderClient;


    // ------------------------------------------------------------------------
    // These constructors are used when you want to treat this
    // BagginsDomainModel as a domain object. This is in contrast to treating
    // this object as a DAO to query to local database.
    // ------------------------------------------------------------------------

    /**
     * Use this to create a new domain object for inserting into the local DB.
     * This will generate the client side id.
     */
    public BagginsDomainModel() {
        this.status = Character.toString(STATUS_INSERT);
    }

    /**
     * @return
     */
    public T createModel() {
        // See http://stackoverflow.com/questions/299998/instantiating-object-of-type-parameter
        try {
            Constructor<T> ctor = ((Class<T>) ((T) this).getClass()).getConstructor();
            return ctor.newInstance();
        } catch (Throwable t) {
            Log.e(TAG, "Error instantiating createModel(Class<T>): " + t);
            throw new RuntimeException(t);
        }
    }


    // ------------------------------------------------------------------------
    // These methods are for creating an instance of BagginsDomainModel
    // for querying the local DB.
    // ------------------------------------------------------------------------

    /**
     * Connect this instance for querying the local DB.
     *
     * @param context The context.
     * @param client  A content provider client. If provided, then one does not need to be acquired.
     *                If this is null, then a client will be acquired and released by the class.
     */
    protected T connect(@NonNull Context context, @Nullable ContentProviderClient client) {
        mContext = context;
        mClient = client;
        mAcquireContentProviderClient = (client == null);
        return ((T) this);
    }

    /**
     * Use this method to acquire a ContentProviderClient. If none was passed to the constructor,
     * then it is acquired here. If we acquire it here, then release will release the client.
     *
     * @return A ContentProviderClient to communicate with the ContentProvider
     */
    protected ContentProviderClient acquireClient() {
        // If no ContentProviderClient, then acquire it
        if (mClient == null) {
            Log.i(TAG, "Getting client with authority: " + BagginsContract.getAuthorityURI());
            mClient = mContext.getContentResolver()
                              .acquireContentProviderClient(BagginsContract.getAuthorityURI());
            Log.i(TAG, "Got Client: " + mClient);
        }
        return mClient;
    }

    /**
     * This releases a ContentProviderClient if it was acquired within this class. If it was
     * acquired outside of this class, this method does nothing.
     */
    protected void releaseClient() {
        if (mAcquireContentProviderClient) {
            ContentProviderClient client = acquireClient();
            if (client != null) {
                client.release();
                mClient = null;
            }
        }
    }

    // ------------------------------------------------------------------------
    // Helper methods
    // ------------------------------------------------------------------------

    /**
     * Make sure the cursor returned is not null
     *
     * @param cursor
     * @throws RemoteException
     */
    protected void assertCursorNotNull(Cursor cursor) throws RemoteException {
        if (cursor == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                throw new RemoteException("Cursor is null in ContentProviderApi");
            } else {
                throw new RuntimeException("Cursor is null in ContentProviderApi");
            }
        }
    }

    /**
     * @return The URI to the ContentPropublicle associated with this Model.
     */
    public Uri getContentUri() {
        return Uri.withAppendedPath(BagginsContract.getAuthorityURI(), getContentProviderTableName());
    }

    /**
     * @return The name of the content provider table associated with this model.
     */
    public String getContentProviderTableName() {
        ClientTable clientTable = this.getClass().getAnnotation(ClientTable.class);
        if (clientTable == null) {
            throw new AnnotationMissingException("All domain objects extending BagginsDomainModel" +
                                                 " must contain the class annotation @ClientTable(\"table name\")");
        }
        return clientTable.value();
    }


    // ------------------------------------------------------------------------
    // Helper methods using reflection to read annotated fields.
    // ------------------------------------------------------------------------

    /**
     * Given a Field annotated with @ClientColumn, returns the ClientColumn annotation.
     * This is a helper method for getClientName and getClientType.
     *
     * @param f The input Field
     * @return The name of the column.
     * @throws AnnotationMissingException If the field is not annotated with ClientColumn.
     */
    protected ClientColumn getClientColumn(Field f) throws AnnotationMissingException {
        ClientColumn clientColumn = f.getAnnotation(ClientColumn.class);
        if (clientColumn == null) {
            throw new AnnotationMissingException("The field " + f.getName() + " is not annotated with @ClientColumn.");
        }
        return clientColumn;
    }
    /**
     * Given a Field annotated with @ClientColumn, returns the column name.
     *
     * @param f The input Field
     * @return The name of the column.
     * @throws AnnotationMissingException If the field is not annotated with ClientColumn.
     */
    protected String getClientName(Field f) throws AnnotationMissingException {
        return getClientColumn(f).value();
    }

    /**
     * Given a Field annotated with @ClientColumn, returns the column type.
     *
     * @param f The input Field
     * @return The name of the column.
     * @throws AnnotationMissingException If the field is not annotated with ClientColumn.
     */
    protected ClientColumn.Type getClientType(Field f) throws AnnotationMissingException {
        return ClientColumn.Type.toType(getClientColumn(f).type());
    }


    /**
     * Given a Field annotated with @SerializedName, returns the column name.
     *
     * @param f The input Field
     * @return The name of the column.
     * @throws AnnotationMissingException If the field is not annotated with ClientColumn.
     */
    protected String getSerializedName(Field f) throws AnnotationMissingException {
        SerializedName serializedName = f.getAnnotation(SerializedName.class);
        if (serializedName == null) {
            throw new AnnotationMissingException("The field " + f.getName() + " is not annotated with @SerializedName.");
        }
        return serializedName.value();
    }

    /**
     * This takes a Field, attempts to set it as accessible, and returns its value
     *
     * @param f A field in this domain object.
     * @return The value of the field.
     * @throws RuntimeException If the field is not accessible.
     */
    protected Object getValue(Field f) {
        f.setAccessible(true);               // Set accessible since it is likely private
        try {
            return f.get((T) this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Field " + f.getName() + " is not accessible.");
        }
    }

    /**
     * This method is for ContentProvider -> Model
     *
     * @return The Content Provider columns associated with this object.
     * The idea is if you make a call to the ContentProvider with these columns,
     * then the returned cursor can be used to load this object.
     */
    protected String[] getClientNames() {
        ArrayList<String> columns = new ArrayList<>();

        // Iterate through all fields annotated with @ClientColumn
        for (Field f : getClientFields()) {    // For each field
            columns.add(getClientName(f));              // Add the column name
        }

        return (String[]) columns.toArray(new String[columns.size()]);
    }

    /**
     * @return A list of all Fields of this BagginsDomainModel which are annotated with @ClientColumn     .
     */
    protected List<Field> getClientFields() {
        return getFields(ClientColumn.class);
    }

    /**
     * @return A list of all Fields of this BagginsDomainModel which are annotated with @SerializaedName.
     */
    protected List<Field> getSerializableFields() {
        return getFields(SerializedName.class);
    }

    /**
     * @return A list of all Fields of this BagginsDomainModel which are annotated with @ClientColumn     .
     */
    protected List<Field> getFields(Class<? extends Annotation> annotation) {
        if (mClientFields == null) {
            mClientFields = new ArrayList<>();

            // First iterate through BagginsDomainModel.class
            for (Field f : BagginsDomainModel.class.getDeclaredFields()) {         // For each field
                if (f.isAnnotationPresent(annotation)) {     // If its annotated as ClientColumn
                    mClientFields.add(f);
                }
            }

            // Now iterate through the subclass.
            for (Field f : getClass().getDeclaredFields()) { // For each field
                if (f.isAnnotationPresent(annotation)) {     // If its annotated as ClientColumn
                    mClientFields.add(f);
                }
            }
        }

        return mClientFields;
    }


    // ------------------------------------------------------------------------
    // ContentProvider -> Model
    // ------------------------------------------------------------------------

    /**
     * This method is for ContentProvider -> Model
     * <p/>
     * Load this model from a ContentProvider cursor.
     *
     * @param cursor The cursor to populate this model with.
     */
    public T load(Cursor cursor) {

        for (Field f : getClientFields()) {
            int columnIdx = cursor.getColumnIndex(getClientName(f));
            if (columnIdx == -1) {
                throw new RuntimeException("The column " + getClientName(f) + " for field " +
                                           f.getName() + " does not exist.");
            }

            setModelField((T) this, f, cursor, columnIdx);
        }

        return (T) this;                // Return instance of the loaded BagginsDomainModel
    }

    public T load(long id) throws ObjectNotFoundException, RemoteException {
        String   selection     = _ID + " = ?";                     // Select by _ID
        String[] selectionArgs = new String[]{Long.toString(id)};  // Set the id

        // Query the ContentProvider
        Cursor cursor = acquireClient().query(getContentUri(), getClientNames(), selection, selectionArgs, null);

        T obj;
        if (cursor == null || cursor.getCount() < 1) {
            throw new ObjectNotFoundException(id);
        } else {
            cursor.moveToFirst();       // Get the first result
            load(cursor);
            cursor.close();
        }
        releaseClient();                // Release client if it was acquired
        return (T) this;                // Return instance of the loaded BagginsDomainModel
    }


    /**
     * Helper method for populate(Cursor). This sets the field in model to the value from cursor
     * corresponding to the columnIdx passed in. It interprets the value in cursor according to the
     * type of the field.
     * <p/>
     * In the end, it might be best to put this in a separate class and allow end users to add
     * custom handlers for other types. This is for ContentProvider -> Model and is a complement
     * class to the function setContentValue which helps to go from Model -> ContentProvider.
     *
     * @param model     The model to set the field on.
     * @param field     The field to set.
     * @param cursor    A result row from ContentProvider to get the value from.
     * @param columnIdx The idx of the cursor column to get the value from.
     */
    protected void setModelField(T model, Field field, Cursor cursor, int columnIdx) {
        field.setAccessible(true);
        Class<?> fieldClass = field.getType();
        try {
            if (fieldClass.isAssignableFrom(String.class)) {
                field.set(model, cursor.getString(columnIdx));
            } else if (fieldClass.isAssignableFrom(Byte.class) ||
                       fieldClass.isAssignableFrom(byte.class)) {
                field.set(model, cursor.getShort(columnIdx));
            } else if (fieldClass.isAssignableFrom(Byte[].class) ||
                       fieldClass.isAssignableFrom(byte[].class)) {
                field.set(model, cursor.getBlob(columnIdx));
            } else if (fieldClass.isAssignableFrom(Float.class) ||
                       fieldClass.isAssignableFrom(float.class)) {
                field.setFloat(model, cursor.getFloat(columnIdx));
            } else if (fieldClass.isAssignableFrom(Double.class) ||
                       fieldClass.isAssignableFrom(double.class)) {
                field.setDouble(model, cursor.getDouble(columnIdx));
            } else if (fieldClass.isAssignableFrom(Short.class) ||
                       fieldClass.isAssignableFrom(short.class)) {
                field.setShort(model, cursor.getShort(columnIdx));
            } else if (fieldClass.isAssignableFrom(Integer.class) ||
                       fieldClass.isAssignableFrom(int.class)) {
                field.setInt(model, cursor.getInt(columnIdx));
            } else if (fieldClass.isAssignableFrom(Long.class) ||
                       fieldClass.isAssignableFrom(long.class)) {
                field.setLong(model, cursor.getLong(columnIdx));
            } else if (fieldClass.isAssignableFrom(Boolean.class) ||
                       fieldClass.isAssignableFrom(boolean.class)) {
                field.setBoolean(model, cursor.getInt(columnIdx) != 0);
            } else if (fieldClass.isAssignableFrom(Character.class)||
                       fieldClass.isAssignableFrom(char.class)) {
                String s = cursor.getString(columnIdx);
                if (s != null && !s.isEmpty()) {
                    field.setChar(model, s.charAt(0));
                } else {
                    field.setChar(model, '\0');
                }
            } else {
                throw new IllegalArgumentException("Error in BagginsDomainModel.setModelField(...), unknown type " +
                                                   fieldClass + " for field " + field.getDeclaringClass() + "#" + field.getType());
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("setModelField Field " + field.getName() + " is not accessible.");
        }
    }


    // ------------------------------------------------------------------------
    // Model -> ContentProvider
    // ------------------------------------------------------------------------

    /**
     * This method is for Model -> ContentProvider
     *
     * @return The ContentValues from this object to be inserted into a ContentProvider
     */
    protected ContentValues createContentValues() {

        ContentValues values = new ContentValues();

        // Iterate through all fields annotated with @ClientColumn and add to builder.
        for (Field f : getClientFields()) {
            setContentValue(values,             // The values to update
                            getClientName(f),  // Get column name from field
                            getValue(f));      // Get value from field
        }
        return values;
    }

    /**
     * Helper method for createContentValues. Given a ContentValues object values, key and value,
     * this populates values with the new key/value. Depending on the Type of Object, this
     * interprets the value differenyl.
     * <p/>
     * In the end, it might be best to put this in a separate class and allow end users to add
     * custom handlers for other types. This is for Model -> ContentProvider and is a complement
     * class to the function setModelField which helps to go from ContentProvider -> Model.
     *
     * @param values
     * @param key
     * @param value
     */
    protected void setContentValue(ContentValues values, String key, Object value) {
        if (value == null) {
            values.putNull(key);
        } else if (value instanceof String) {
            values.put(key, (String) value);
        } else if (value instanceof Byte) {
            values.put(key, (Byte) value);
        } else if (value instanceof Byte[]) {
            values.put(key, (byte[]) value);

        } else if (value instanceof Float) {
            values.put(key, (Float) value);
        } else if (value instanceof Double) {
            values.put(key, (Double) value);

        } else if (value instanceof Short) {
            values.put(key, (Short) value);
        } else if (value instanceof Integer) {
            values.put(key, (Integer) value);
        } else if (value instanceof Long) {
            values.put(key, (Long) value);

        } else if (value instanceof Boolean) {
            values.put(key, (Boolean) value);
        } else if (value instanceof Character) {
            values.put(key, Character.toString((Character) value));
        } else if (value instanceof Date) {
            Date d = (Date) value;
            values.put(key, d.getTime());

        } else if (value instanceof List<?>) {  // If this is a list of objects, then it must be a list of BagginsDomainModel objects
            StringBuffer keyString = new StringBuffer("");
            boolean isFirst = true;
            for (BagginsDomainModel<T> obj : (List<BagginsDomainModel<T>>) value) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    keyString.append(",");
                }

                keyString.append(obj.id);
            }
            values.put(key,keyString.toString());
        } else {
            throw new IllegalArgumentException("bad value type: " + value.getClass().getName());
        }
    }

    // ------------------------------------------------------------------------
    // Model -> Post Params to server
    // ------------------------------------------------------------------------

    /**
     * This method is for Model -> Post Params in order to generate the parameters to
     * pass to the http post to pass the the REST api.
     *
     * @return the Map of key/value pairs to pass to the http post methods of the REST api.
     */
    public Map<String, String> getPostParams() {

        Map<String, String> postParams = new HashMap<>();

        for (Field f : getClientFields()) {

            setPostDataParam(postParams,        // The values to update
                             getClientName(f),  // Get column name from field
                             getValue(f));      // Get value from field
        }
        return postParams;

    }

    /**
     * Helper method for createContentValues. Given a ContentValues object values, key and value,
     * this populates values with the new key/value. Depending on the Type of Object, this
     * interprets the value differenyl.
     * <p/>
     * In the end, it might be best to put this in a separate class and allow end users to add
     * custom handlers for other types. This is for Model -> ContentProvider and is a complement
     * class to the function setModelField which helps to go from ContentProvider -> Model.
     *
     * @param postParams The postParams to fill out
     * @param key        The key to put into postParams
     * @param value      The value to put into postParms
     */
    protected void setPostDataParam(Map<String, String> postParams, String key, Object value) {
        if (value == null) {
            postParams.remove(key); // Remove key to send null
        } else if (value instanceof String) {
            postParams.put(key, (String) value);
        } else if (value instanceof Byte) {
            postParams.put(key, Byte.toString((Byte) value));

        } else if (value instanceof Float) {
            postParams.put(key, Float.toString((float) value));
        } else if (value instanceof Double) {
            postParams.put(key, Double.toString((Double) value));

        } else if (value instanceof Short) {
            postParams.put(key, Short.toString((Short) value));
        } else if (value instanceof Integer) {
            postParams.put(key, Integer.toString((Integer) value));
        } else if (value instanceof Long) {
            postParams.put(key, Long.toString((Long) value));

        } else if (value instanceof Boolean) {
            postParams.put(key, ((Boolean) value == true) ? "1" : "0");
        } else if (value instanceof Character) {
            postParams.put(key, Character.toString((Character) value));
        } else if (value instanceof Date) {
            Date d = (Date) value;
            postParams.put(key, Long.toString(d.getTime()));
        } else {
            throw new IllegalArgumentException("bad value type: " + value.getClass().getName());
        }
    }
    // ------------------------------------------------------------------------
    // These methods are used to query,update,delete and insert to and from the
    // ContentProvider. They are batch operations which ignore the underlying
    // Data Model
    // ------------------------------------------------------------------------

    /**
     * Inserts this object into the ContentProvider. If it exists, it updates the existing
     * row.
     *
     * @return The Uri of the row in the ContentProvider
     * @throws RemoteException
     */
    public Uri save() throws RemoteException {

        // Set id as a new temp id, which is 1 smaller than any other temp id
        String[] projection = new String[]{"MIN(" + _ID + ")"};
        id = Math.min(-1, queryLong(projection, null, null, null, 0) - 1);

        ContentValues values = createContentValues();
        Uri           uri    = acquireClient().insert(getContentUri(), values);
        releaseClient();
        return uri;
    }

    /**
     * Get a single long from a query.
     *
     * @return The single long, or defaultValue if the cursor getCount is not 1.
     * @throws RemoteException
     */
    protected long queryLong(@Nullable String[] projection, @Nullable String selection,
                             @Nullable String[] selectionArgs, @Nullable String sortOrder,
                             long defaultValue) throws RemoteException {

        Cursor cursor = acquireClient().query(getContentUri(), projection, selection, selectionArgs, sortOrder);
        assertCursorNotNull(cursor);

        long val;
        if (cursor.getCount() != 1) {
            val = defaultValue;

        } else {
            cursor.moveToFirst();
            val = cursor.getLong(0);
        }
        cursor.close();
        releaseClient();
        return val;
    }

    /**
     * Query from the SQLite DB. This returns rows whose status is NOT 'D'
     *
     * @param selection     A selection criteria to apply when filtering rows.
     *                      If {@code null} then all rows are included.
     * @param selectionArgs You may include ?s in selection, which will be replaced by
     *                      the values from selectionArgs, in order that they appear in the selection.
     *                      The values will be bound as Strings.
     * @param sortOrder     How the rows in the cursor should be sorted.
     *                      If {@code null} then the provider is free to define the sort order.
     */
    public List<T> query(String selection, String[] selectionArgs, String sortOrder) throws RemoteException {

        // --------------------------------------------------------------------
        // Make sure we only query non-deleted fields
        // --------------------------------------------------------------------
        String   decoratedSelection;
        String[] decoratedSelectionArgs;

        // First decorate the selection
        if (selection == null) {
            decoratedSelection = STATUS + " !=?";
        } else {
            decoratedSelection = "(" + selection + ") AND (" + STATUS + "!=?)";
        }

        // Now decorate the selectionArgs
        if (selectionArgs == null) {
            decoratedSelectionArgs = new String[1];
        } else {
            // Instantiate new array
            decoratedSelectionArgs = new String[selectionArgs.length + 1];

            // Copy old array to new array
            System.arraycopy(selectionArgs, 0, decoratedSelectionArgs, 0, selectionArgs.length);
        }
        decoratedSelectionArgs[decoratedSelectionArgs.length - 1] = Character.toString(STATUS_DELETE);

        // --------------------------------------------------------------------
        // Query the ContentProvider
        // --------------------------------------------------------------------
        Cursor cursor = acquireClient().query(getContentUri(), getClientNames(), decoratedSelection, decoratedSelectionArgs, sortOrder);

        assertCursorNotNull(cursor);

        List<T> models = new ArrayList<>(cursor.getCount());
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            T model = createModel();
            model.load(cursor);
            models.add(model);
            cursor.moveToNext();            // Go to the next row.
        }
        cursor.close();

        releaseClient();
        return models;
    }

    public int update(ContentValues contentValues, String selection, String[] selectionArgs) throws RemoteException {
        // Update the ContentProvider
        int count = acquireClient().update(getContentUri(), contentValues, selection, selectionArgs);
        releaseClient();
        return count;
    }

    /**
     * Mark the selected rows for deletion.
     *
     * @param selection     The
     * @param selectionArgs
     * @return
     * @throws RemoteException
     */
    public int delete(String selection, String[] selectionArgs) throws RemoteException {

        ContentValues values = new ContentValues();
        setContentValue(values, STATUS, STATUS_DELETE);

        return update(values, selection, selectionArgs);
    }


}
