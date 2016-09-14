package edu.ucla.cs.baggins.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by ethan on 8/9/16.
 */
public class BagginsSharedPreferences {
    public final static String TAG = "baggins_sp";


    /**
     * An instance of the Android SharedPreferences
     */
    private SharedPreferences mSharedPreferences;

    /**
     * TODO: Make this configurable, should probably be in build.gradle
     */
    private final static String NAMESPACE = "edu.ucla.baggins.persist";

    private final static String AUTH_USER_ID                  = NAMESPACE + ".auth.user_id";
    private final static String AUTH_USERNAME                 = NAMESPACE + ".auth.username";

    // ------------------------------------------------------------------------
    // Get an instance of this class
    // ------------------------------------------------------------------------

    protected BagginsSharedPreferences(Context context) {
        mSharedPreferences = context.getSharedPreferences(NAMESPACE, Context.MODE_PRIVATE);
    }

    public static BagginsSharedPreferences instance(Context context) {
        return new BagginsSharedPreferences(context);
    }
    // ------------------------------------------------------------------------
    // For the user information of the currently logged in user
    // ------------------------------------------------------------------------

    /**
     * @return The currently logged in user's id.
     */
    public long getMyUserId() {
        return mSharedPreferences.getLong(AUTH_USER_ID, Long.MIN_VALUE);
    }

    /**
     * Set the currently logged in user's id. Called from Auth login method.
     *
     * @param userId The currently logged in user's id.
     */
    public void setMyUserId(long userId) {
        mSharedPreferences.edit().putLong(AUTH_USER_ID, userId).apply();
    }


    /**
     * @return The currently logged in user's username.
     */
    public String getMyUsername() {
        return mSharedPreferences.getString(AUTH_USERNAME, "");
    }

    /**
     * Set the currently logged in user's username. Called from Auth login method.
     *
     * @param username The currently logged in user's username.
     */
    public void setMyUsername(String username) {
        mSharedPreferences.edit().putString(AUTH_USERNAME, username).apply();
    }

}
