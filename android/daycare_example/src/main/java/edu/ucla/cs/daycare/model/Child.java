package edu.ucla.cs.daycare.model;

import android.content.ContentProviderClient;
import android.content.Context;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;

import edu.ucla.cs.baggins.data.provider.model.annotations.ClientColumn;
import edu.ucla.cs.baggins.data.provider.model.annotations.ClientTable;
import edu.ucla.cs.baggins.data.provider.model.base.BagginsDomainModel;

/**
 * Java domain model object. This is used to transfer data to and from the local SQLite database
 * as well as through the rest API to the server.
 */
@ClientTable("child")
public class Child extends BagginsDomainModel<Child> {

    @ClientColumn("first_name")
    @SerializedName("mFirstName")
    private String mFirstName;

    @ClientColumn("last_name")
    @SerializedName("mLastName")
    private String mLastName;

    @ClientColumn("birthday")
    @SerializedName("birthday")
    private Date mBirthday;

    @ClientColumn(value = "parent_ids", type = ClientColumn.LIST_OF_KEYS_STRING)
    @SerializedName("parentIds")
    private List<Long> mParentsIds;

    // ------------------------------------------------------------------------
    // Use these methods to set the data fields.
    // ------------------------------------------------------------------------

    public Child firstName(String firstName) {
        mFirstName = firstName;
        return this;
    }

    public Child lastName(String lastName) {
        mLastName = lastName;
        return this;
    }

    public Child birthday(Date birthday) {
        mBirthday = birthday;
        return this;
    }

    public Child parentIds(List<Long> parentIds) {
        mParentsIds = parentIds;
        return this;
    }


    // ------------------------------------------------------------------------
    // Use these methods to get the data fields.
    // ------------------------------------------------------------------------

    public String firstName() {
        return mFirstName;
    }

    public String lastName() {
        return mLastName;
    }

    public Date birthday() {
        return mBirthday;
    }

    public List<Long> parentIds() { return mParentsIds; }

    // ------------------------------------------------------------------------
    // ***     T h e r e   i s   n o   r e a s o n   t o    c h a n g e     ***
    // ***           a n y t h i n g   b e l o w   t h i s   l i n e.       ***
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Use the "with" methods to treat User as as data access object (DAO).
    // ------------------------------------------------------------------------

    /**
     * If you use this method to load an instance of the class, then a ContentProviderClient
     * will be acquired and released as necessary by the parent BagginsDomainModel class.
     *
     * @param context The Android application context.
     * @return An instance of this class to be used to interact with the ContentProvider.
     */
    public static Child with(Context context) {
        return with(context, null);
    }

    /**
     * If you use this method to load an instance of the class, then it will not be necessary for
     * the parent BagginsDomainModel to acquire or release a ContentProviderClient.
     *
     * @param context The Android application context.
     * @param client  The ContentProviderClient to use to connect to the ContentProvider.
     * @return An instance of this class to be used to interact with the ContentProvider;
     */
    public static Child with(Context context, ContentProviderClient client) {
        return new Child().connect(context, client);
    }

}
