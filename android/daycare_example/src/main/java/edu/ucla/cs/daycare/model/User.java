package edu.ucla.cs.daycare.model;

import android.content.ContentProviderClient;
import android.content.Context;

import com.google.gson.annotations.SerializedName;

import java.util.Collection;

import edu.ucla.cs.baggins.data.provider.model.annotations.ClientColumn;
import edu.ucla.cs.baggins.data.provider.model.annotations.ClientTable;
import edu.ucla.cs.baggins.data.provider.model.annotations.ReferenceProperty;
import edu.ucla.cs.baggins.data.provider.model.base.BagginsDomainModel;

/**
 * User domain model object. This is used to transfer data to and from the local SQLite database
 * as well as through the rest API to the server.
 */
@ClientTable("user")
public class User extends BagginsDomainModel<User> {

    @ClientColumn("first_name")
    @SerializedName("firstName")
    private String mFirstName;

    @ClientColumn("last_name")
    @SerializedName("lastName")
    private String mLastName;

    @ClientColumn("mAddress1")
    @SerializedName("address1")
    private String mAddress1;

    @ClientColumn("mAddress2")
    @SerializedName("address2")
    private String mAddress2;

    @ClientColumn("city")
    @SerializedName("city")
    private String mCity;

    @ClientColumn("state")
    @SerializedName("state")
    private String mState;

    @ClientColumn("zipcode")
    @SerializedName("zipcode")
    private String mZipcode;

    @ClientColumn("phone_number")
    @SerializedName("phoneNumber")
    private String mPhoneNumber;

    // ------------------------------------------------------------------------
    // Use these methods to set the data fields.
    // ------------------------------------------------------------------------

    /**
     * If you use this method to load an instance of the class, then a ContentProviderClient
     * will be acquired and released as necessary by the parent BagginsDomainModel class.
     *
     * @param context The Android application context.
     * @return An instance of this class to be used to interact with the ContentProvider.
     */
    public static User with(Context context) {
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
    public static User with(Context context, ContentProviderClient client) {
        return new User().connect(context, client);
    }

    public User firstName(String firstName) {
        mFirstName = firstName;
        return this;
    }

    public User lastName(String lastName) {
        mLastName = lastName;
        return this;
    }

    public User address1(String address1) {
        mAddress1 = address1;
        return this;
    }

    public User address2(String address2) {
        mAddress2 = address2;
        return this;
    }

    public User city(String city) {
        mCity = city;
        return this;
    }

    public User state(String state) {
        mState = state;
        return this;
    }

    // ------------------------------------------------------------------------
    // Use these methods to get the data fields.
    // ------------------------------------------------------------------------

    public User zipcode(String zipcode) {
        mZipcode = zipcode;
        return this;
    }

    public User phoneNumber(String phoneNumber) {
        mPhoneNumber = phoneNumber;
        return this;
    }

    public String firstName() {
        return mFirstName;
    }

    public String lastName() {
        return mLastName;
    }

    public String address1() {
        return mAddress1;
    }

    public String address2() {
        return mAddress2;
    }

    public String city() {
        return mCity;
    }

    public String state() {
        return mState;
    }

    // ------------------------------------------------------------------------
    // ***     T h e r e   i s   n o   r e a s o n   t o    c h a n g e     ***
    // ***           a n y t h i n g   b e l o w   t h i s   l i n e.       ***
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Use the "with" methods to treat User as as data access object (DAO).
    // ------------------------------------------------------------------------

    public String zipcode() {
        return mZipcode;
    }

    public String phoneNumber() {
        return mPhoneNumber;
    }
}
