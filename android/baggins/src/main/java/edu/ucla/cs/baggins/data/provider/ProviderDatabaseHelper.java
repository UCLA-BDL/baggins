package edu.ucla.cs.baggins.data.provider;

/**
 * Created by Ethan L. Schreiber on 10/13/15.
 */

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.ucla.cs.baggins.data.provider.model.annotations.AnnotationMissingException;
import edu.ucla.cs.baggins.data.provider.model.annotations.ClientColumn;
import edu.ucla.cs.baggins.data.provider.model.annotations.ClientTable;
import edu.ucla.cs.baggins.data.provider.model.base.BagginsDomainModel;
import edu.ucla.cs.baggins.util.StaticUtil;

/**
 * The database helper is for creating and updating the database schema.
 */
final public class ProviderDatabaseHelper extends SQLiteOpenHelper {

    /**
     * The version of the DB. If this changes, onUpgrade or onDowngrade is run
     */
    public static final  int    DATABASE_VERSION = 1;
    private final static String TAG              = "db_helper";
    /**
     * Stupid SQLiteOpenHelper doesn't give me access to context.
     */
    private Context mContext;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------
    public ProviderDatabaseHelper(Context context, String databaseName) {
        super(context, databaseName, null, DATABASE_VERSION);
        mContext = context;
        Log.i(TAG, "Instantiate Database Helper: " + databaseName);
        // setWriteAheadLoggingEnabled(true);
    }


    // ------------------------------------------------------------------------
    // Override methods for creating and upgrading
    // ------------------------------------------------------------------------

    /**
     * Each time the database is opened, we compare the domain objects to the DB to make sure they
     * are the same. Since the domain objects (Classes extending BagginsDomainObject which are
     * annotated @ClientTable) define the sqlite databases, they have to match
     *
     * @param db
     */
    @Override
    public void onOpen(SQLiteDatabase db) {
        // db.execSQL("pragma foreign_keys = true");

        Log.i(TAG, "Open DB");
        Set<String> dbTableNames = getAllTableNames(db);

        try {
            // Iterate through domain objects
            Log.i(TAG, "Iterate through domain Objects");

            for (Class<BagginsDomainModel<?>> modelClass : StaticUtil.getAllModelClasses(mContext)) {
                String domainTableName = getTableName(modelClass);
                Log.i(TAG, "-----------------------------------------");
                Log.i(TAG, "   Domain Table Name: " + domainTableName);
                if (dbTableNames.contains(domainTableName)) {                               // If the db contains a table for this domain model

                    Log.i(TAG, "   " + domainTableName + " exists in SQLite");
                    // Compare the domain table to the existing table
                    boolean                       recreateTable = false;  // Do we need to drop and recreate the table?
                    Map<String, SQLiteColumnInfo> dbColumns     = getColumnInfo(db, domainTableName);   // Get the db column info
                    Map<String, SQLiteColumnInfo> modelColumns  = getColumnInfo(modelClass);            // Get the model column info

                    Log.i(TAG, "Iterate through fields in domain model.");
                    for (SQLiteColumnInfo domainColumn : modelColumns.values()) {                       // Iterate through domain column info
                        if (domainColumn.type == SQLiteType.MANY_TO_MANY) {                             // Many to Many is treated differently



                        } else if (dbColumns.containsKey(domainColumn.name)) {                    // If it exists in the db (same column name)
                            SQLiteColumnInfo dbColumn = dbColumns.remove(domainColumn.name);      // Get the DB version of the column
                            Log.i(TAG, "      Column: " + domainColumn + " exists.");

                            if (domainColumn.type != dbColumn.type) {       // If the type changed,
                                Log.i(TAG, "      " + dbColumn + " type changed, recreate table");
                                recreateTable = true;                       // we need to recreate
                                break;                                      // Can break this for loop
                            }
                        } else {
                            Log.i(TAG, "      Column: " + domainColumn + " does not exist, creating.");

                            addColumn(db, domainTableName, domainColumn);   // Add the new column
                        }
                    }

                    if (recreateTable ||             // If a field type changed
                        !dbColumns.isEmpty()) {      // Or we removed a field from the domain model
                        Log.i(TAG, "Recreating table");
                        Log.i(TAG, "Dropping existing table " + domainTableName);
                        db.execSQL("DROP TABLE IF EXISTS " + domainTableName);  // Drop table

                        Log.i(TAG, "Create table " + domainTableName + " again.");
                        createTable(db, modelClass);                            // Create table
                    }

                } else {
                    Log.i(TAG, "   Does not exist, creating");
                    createTable(db, modelClass);     // Create table
                }

            }
        } catch (IOException e) {
            Log.e(TAG, "Error: " + e);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        Log.d(TAG, "Creating SQLite Tables!!");

        String sourceDir = mContext.getApplicationInfo().sourceDir;   // Search for all classes
        try {
            for (Class<BagginsDomainModel<?>> modelClass : StaticUtil.getAllModelClasses(mContext)) {
                createTable(db, modelClass);                                // Create the table in SQLite
            }
        } catch (IOException e) {
            Log.e(TAG, "Error: " + e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        Log.i(TAG, "DB Upgrade from " + oldVersion + " to " + newVersion + ".");

        try {
            for (String idxName : getIndexNamesToDelete(db)) {
                db.execSQL("DROP INDEX IF EXISTS " + idxName);
            }
            for (String tableName : getAllTableNames(db)) {
                db.execSQL("DROP TABLE IF EXISTS " + tableName);
            }

        } catch (SQLiteException e) {
            Log.e(TAG, "Upgrade Error: " + e);
        }
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "DB Upgrade from " + oldVersion + " to " + newVersion + ".");
        onUpgrade(db, oldVersion, newVersion);
    }

    // ------------------------------------------------------------------------
    // Methods for checking/modifying the current state of SQLite
    // ------------------------------------------------------------------------

    /**
     * Add the column to tableName
     *
     * @param db        The SQLite database connection.
     * @param tableName The name of the table to add a column to.
     * @param column    The colum to add.
     */
    protected void addColumn(SQLiteDatabase db, String tableName, SQLiteColumnInfo column) {
//        db.execSQL("ALTER TABLE ? ADD COLUMN ? ?",
//                   new Object[]{tableName, column.name, column.type.toString()});

        String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + column.name + " " + column.type.toString();
        Log.i(TAG, "SQL: " + sql);
        db.execSQL(sql);
    }

    /**
     * Given a class which is annotated as ClientTable.class, creates a
     * table based on the class.
     *
     * @param modelClass
     */
    protected void createTable(SQLiteDatabase db, Class<BagginsDomainModel<?>> modelClass) {

        StringBuffer sb = new StringBuffer("CREATE TABLE ");        // Create the SQL statement
        sb.append(getTableName(modelClass)).append("(\n");          // Get the name of the table

        // Iterate through the fields
        boolean isFirst = true;                                         // Don't prepend "," before first

        for (SQLiteColumnInfo column : getColumnInfo(modelClass).values()) { // Iterate through columns

            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(",\n");
            }

            sb.append("    " + column.name + " " + column.type.toString());
            if (column.isPrimaryKey) {
                sb.append(" PRIMARY KEY AUTOINCREMENT");
            }
        }

        // If no fields were annotated
        if (isFirst) {
            throw new AnnotationMissingException("Class " + modelClass.getName() +      // throw exception
                                                 "requires at least one @ClientColumn annotation on a field.");
        }

        sb.append(");");    // close statement

        Log.i(TAG, "SQL: " + sb.toString());
        db.execSQL(sb.toString());
    }


    /**
     * Check to see if the table name passed in exists.
     *
     * @param db        The sqlite db
     * @param tableName The table name to check for existence.
     * @return True if the table exists, false otherwise
     * @throws AnnotationMissingException
     */
    protected boolean doesTableExist(SQLiteDatabase db, String tableName) throws AnnotationMissingException {
        return DatabaseUtils.queryNumEntries(db, "sqlite_master", "type=? AND name=?", new String[]{"table", tableName}) != 0;
    }


    /**
     * Get sqlite table names, ignoring system tables starting with android_ or sqlite_
     */
    protected Set<String> getAllTableNames(SQLiteDatabase db) {

        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

        Set<String> list = new HashSet<>(c.getCount());
        if (c.moveToFirst()) {
            while (!c.isAfterLast()) {
                String tableName = c.getString(0);
                if (!tableName.toLowerCase().startsWith("sqlite_") &&
                    !tableName.toLowerCase().startsWith("sqlite_") &&
                    !tableName.toLowerCase().equals("android_metadata")) {
                    list.add(tableName);
                }
                c.moveToNext();
            }
        }
        return list;
    }

    /**
     * Get sqlite index names.
     */
    protected List<String> getIndexNamesToDelete(SQLiteDatabase db) {

        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='index'", null);

        List<String> list = new ArrayList<>(c.getCount());
        if (c.moveToFirst()) {
            while (!c.isAfterLast()) {
                String tableName = c.getString(0);
                if (!tableName.toLowerCase().startsWith("sqlite_") &&
                    !tableName.toLowerCase().startsWith("sqlite_")) {
                    list.add(tableName);
                    c.moveToNext();
                } else {
                    Log.i(TAG, "Index Name: " + c.getString(0) + " ignored.");
                }
            }
        }
        return list;
    }

    // ------------------------------------------------------------------------
    // Fields for parsing BagginsDomainObjects
    // ------------------------------------------------------------------------

    /**
     * Given a BaggingDomainModel, returns the table name. This expectsd the class to be
     * annotated with @ClientTable("[table_name]")
     *
     * @param modelClass The class to read the annotation from
     * @return The table name
     * @throws AnnotationMissingException If the class is not annotated with @ClientTable("[table_name]")
     */
    protected String getTableName(Class<BagginsDomainModel<?>> modelClass) throws AnnotationMissingException {
        // Get the table name from the ClientTable annotation
        if (modelClass.isAnnotationPresent(ClientTable.class)) {                    // Double check this class is annotated as @ClientTable
            return modelClass.getAnnotation(ClientTable.class).value();    // Get the name of the table from the annotation

        } else {
            throw new AnnotationMissingException("Class " + modelClass.getName() +      // Throw exception if not @ClientTable
                                                 " requires @ClientTable annotation on class declaration.");
        }
    }

    /**
     * Get all fields in class travelling up the class hierarchy to parents. (i.e.,look at the
     * classes that startClass extends). Stops when we reach exclusiveParent and does not include fields
     * in that class. Alterantively, stops when there are no more parents.
     *
     * @param startClass      The first class to get fields from.
     * @param exclusiveParent get fields up to this class exclusive.
     * @return®
     */
    protected static Iterable<Field> getFieldsUpTo(
            @NonNull Class<?> startClass,
            @Nullable Class<?> exclusiveParent) {

        List<Field> currentClassFields = new ArrayList<Field>(Arrays.asList(startClass.getDeclaredFields()));
        Class<?>    parentClass        = startClass.getSuperclass();

        if (parentClass != null &&
            (exclusiveParent == null || !(parentClass.equals(exclusiveParent)))) {
            List<Field> parentClassFields =
                    (List<Field>) getFieldsUpTo(parentClass, exclusiveParent);
            currentClassFields.addAll(parentClassFields);
        }

        return currentClassFields;
    }


    // ------------------------------------------------------------------------
    // Data Structures for this class
    // ------------------------------------------------------------------------

    /**
     * This enum represent the possible types for a SQLite Column
     */
    protected enum SQLiteType {
        NULL("NULL"), INTEGER("INTEGER"), REAL("REAL"), TEXT("TEXT"), BLOB("BLOB"), MANY_TO_MANY("MANY_TO_MANY");

        /**
         * Store the string version of the SQLiteType enum for passing to queries.
         */
        private String mValue;

        SQLiteType(String value) {
            mValue = value;
        }

        /**
         * Convert from the string version to the enum version
         *
         * @param columnTypeString The string
         * @return the SQLiteType
         * @throws RuntimeException if the columnTypeString is not a valid PostType string.
         */
        public static SQLiteType toSQLiteType(@NonNull String columnTypeString) {

            Collection<String> columnTypeStrings = new ArrayList<>();

            for (SQLiteType columnType : SQLiteType.values()) {
                if (columnType.toString().equals(columnTypeString)) {
                    return columnType;
                }
                columnTypeStrings.add(columnType.toString());
            }
            throw new RuntimeException("[" + columnTypeString + "] is not a valid SQLiteType string. It must be one of {"
                                       + TextUtils.join(",", columnTypeStrings) + "}");
        }

        /**
         * Convert from the java type version to the enum version
         *
         * @param field The field from the java domain object. Reads the type and converts to SQLite type.
         * @return the SQLiteType
         * @throws RuntimeException if the columnTypeString is not a valid PostType string.
         */
        public static SQLiteType toSQLiteType(@NonNull Field field) {

            Class<?> type = field.getType();
            if (String.class.isAssignableFrom(type)) {
                return TEXT;
            } else if (Long.class.isAssignableFrom(type) ||
                       long.class.isAssignableFrom(type) ||
                       Integer.class.isAssignableFrom(type) ||
                       int.class.isAssignableFrom(type) ||
                       Date.class.isAssignableFrom(type)) {
                return INTEGER;
            } else if (Double.class.isAssignableFrom(type)) {
                return REAL;
            } else if (Collection.class.isAssignableFrom(type)) {   // Collections are stored as a comma delimited text field
                return TEXT;
            } else {
                throw new RuntimeException("Model field type " + type + " is not recognized as a SQLite Column Type.");
            }
        }

        /**
         * @return The string version of the SQLiteType.
         */
        public String toString() {
            return mValue;
        }
    }

    /**
     * This contains information describing the column of a SQLite table. Use this  to compare
     * the existing tables in SQLite with the tables implied by the BagginsDomainObjects. We can
     * then create and update tables using this structure.
     */
    protected class SQLiteColumnInfo {
        /**
         * The column name. For many to many, "my column" is stored in this variable
         */
        public String name;

        /**
         * The type of the column
         */
        public SQLiteType type;

        /**
         * For many to many, the name of the join table.
         */
        public String joinTable;

        /**
         * For many to many, the name of the other class.
         */
        public String otherClass;


        public boolean isNotNull;
        public boolean isPrimaryKey;

        /**
         * Use this for a normal column
         *
         * @param name
         * @param type
         * @param isNotNull
         * @param isPrimaryKey
         */
        public SQLiteColumnInfo(String name, SQLiteType type, boolean isNotNull, boolean isPrimaryKey) {
            this.name = name;
            this.type = type;
            this.isNotNull = isNotNull;
            this.isPrimaryKey = isPrimaryKey;
        }

        /**
         * Use this for create a Many to Many
         *
         * @param joinTable  The name of the join table.
         * @param myColumn   The name of the field within the join table pointing to this table
         * @param otherClass
         */
        public SQLiteColumnInfo(String joinTable, String myColumn, String otherClass) {
            this(myColumn, SQLiteType.MANY_TO_MANY, false, false);
            this.joinTable = joinTable;
            this.otherClass = otherClass;
        }

        // Implement equals and hashcode based on name field
        @Override
        public boolean equals(Object o) {
            if (o != null && o instanceof SQLiteColumnInfo) {
                return ((SQLiteColumnInfo) o).name.equals(name);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return ((name == null) ? 0 : name.hashCode());

        }

        @Override
        public String toString() {
            return "Columns {name=\"" + name + "\" type=\"" + type + "\"}";
        }
    }


    // ------------------------------------------------------------------------
    // Get Column Info
    // ------------------------------------------------------------------------


    /**
     * Given a table name, get all of the sqlite columns
     *
     * @param db        The sqlite db
     * @param tableName The name of the table
     * @return A Map of SQLiteColumnInfo with Key being the name of the column and value being
     * the columns of the table.
     */
    protected Map<String, SQLiteColumnInfo> getColumnInfo(SQLiteDatabase db, String tableName) {
        Map<String, SQLiteColumnInfo> columns = new HashMap<>();

        Cursor c = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);

        if (c.moveToFirst()) {
            int nameIdx      = c.getColumnIndex("name");    // The column name idx
            int typeIdx      = c.getColumnIndex("type");    // The column type idx
            int isNotNullIdx = c.getColumnIndex("notnull"); // The column notnull property idx
            int isPKIdx      = c.getColumnIndex("pk");      // The column notnull property idx

            while (!c.isAfterLast()) {

                String     columnName = c.getString(nameIdx);
                SQLiteType columnType = SQLiteType.toSQLiteType(c.getString(typeIdx));
                boolean    isNotNull  = c.getInt(isNotNullIdx) != 0;
                boolean    isPK       = c.getInt(isPKIdx) != 0;
                columns.put(columnName, new SQLiteColumnInfo(columnName, columnType, isNotNull, isPK));
                c.moveToNext();
            }
        }
        c.close();
        return columns;
    }

    /**
     * Given a model class, get all of the sqlite columns.
     *
     * @param modelClass The domain class to read the columns from
     * @return A Map with key being the column name and value being the column info.
     */
    protected Map<String, SQLiteColumnInfo> getColumnInfo(Class<BagginsDomainModel<?>> modelClass) {
        Map<String, SQLiteColumnInfo> columns = new HashMap<>();

        try {
            // Iterate through the fields
            boolean isFirst = true;                                         // Don't prepend "," before first
            for (Field field : getFieldsUpTo(modelClass, Object.class)) {   // Iterate through all fields in class
                if (field.isAnnotationPresent(ClientColumn.class)) {        // If field is annotated as ClientColumn.class

                    String     columnName = field.getAnnotation(ClientColumn.class).value();
                    SQLiteType columnType = SQLiteType.toSQLiteType(field);
                    boolean    isNotNull  = false; // In the future, we could use annotations to determine
                    boolean    isPK       = columnName.equals(BagginsDomainModel._ID);

                    columns.put(columnName, new SQLiteColumnInfo(columnName, columnType, isNotNull, isPK));
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error getting column info from BagginsDomainObject: " + t.toString());
            throw t;
        }
        return columns;
    }

}