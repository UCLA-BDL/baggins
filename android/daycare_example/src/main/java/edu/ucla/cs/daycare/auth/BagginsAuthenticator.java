package edu.ucla.cs.daycare.auth;

/**
 * Created by ethan on 3/22/16.
 */

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.NetworkErrorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import edu.ucla.cs.daycare.R;
import edu.ucla.cs.baggins.data.BagginsSharedPreferences;
import edu.ucla.cs.baggins.data.net.HttpUtils;
import edu.ucla.cs.baggins.data.provider.model.gson.GsonFactory;
import edu.ucla.cs.baggins.data.sync.BagginsSyncUtils;
import edu.ucla.cs.baggins.data.sync.RestConstants;

/**
 * The Authenticator class
 */
public class BagginsAuthenticator extends AbstractAccountAuthenticator {

    private final static String TAG = "baggins_auth";

    /**
     * The AUTH TYPE.
     */
    public final static String AUTH_TOKEN_TYPE = "Full access";

    /**
     * Since AbstractAccountAuthenticator sets mContext as private, we need to store it here.
     */
    private final Context mContext;

    public BagginsAuthenticator(Context context) {
        super(context);
        mContext = context;
    }


    // For the most part, the pattern for implementing these abstract methods is the same:
    //  - If we know the answer already, return a Bundle with it
    //  - If we don’t know the answer, and need to ask the user something in order to find the
    //    answer, we create an Intent that will start an Activity to interact with the user.
    //    We return that Intent via the Bundle, under the key, KEY_INTENT. It’s the caller’s
    //    responsibility to make use of that intent; if the caller is Android, it will do so.
    //  - Alternatively, if no synchronous response is possible, but an asynchronous response
    //    is possible then it may return null and use the given AccountAuthenticatorResponse
    //    to return it later.
    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
                             String authTokenType, String[] requiredFeatures, Bundle options)
    throws NetworkErrorException {

        Log.i(TAG,"Add Account");
        // We're going to use a LoginActivity to talk to the user (mContext
        // we'll have noted on construction).
        final Intent intent = new Intent(mContext, AuthActivity.class);

        intent.putExtra(AuthActivity.ARG_ACCOUNT_TYPE, accountType);
        intent.putExtra(AuthActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        // We can configure that activity however we wish via the Intent.
        intent.putExtra(AuthActivity.ARG_AUTH_TYPE, authTokenType);

        // It will also need to know how to send its response to the
        // account manager; LoginActivity must derive from
        // AccountAuthenticatorActivity, which will want this key set
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        // Wrap up this intent, and return it, which will cause the
        // intent to be run, triggering the login activity
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        Log.i(TAG,"Intent: " + intent);
        Log.i(TAG,"Bundle Intent: " + intent.getExtras());
        return bundle;
    }

    // See: http://blog.udinic.com/assets/media/images/2013-04-24-write-your-own-android-authenticator/oauth_dance1.png
    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
                               String authTokenType, Bundle options) throws NetworkErrorException {
        Log.i(TAG,"Get Auth Token");
        // Extract the username and password from the Account Manager, and ask
        // the server for an appropriate AuthToken.
        final AccountManager am = AccountManager.get(mContext);

        // Get the auth token from the account manager cache
        String authToken = am.peekAuthToken(account, authTokenType);

        // If we have no token, use the account credentials to fetch a new one, effectively another logon
        if (TextUtils.isEmpty(authToken)) {  // if null or 0-length

            final String password = am.getPassword(account);    // get password from accountmanager given the account passed in

            if (password != null) {
                try {

                    authToken = BagginsAuthenticator.userLogIn(mContext, account.name, password);

                    // With new auth token, register the device again.
//                    BagginsSharedPreferences.instance(mContext).setDeviceRegistered(false);

                } catch (IOException e) {
                    Log.e(TAG, "IO Error: " + e);
                } catch (LoginFailedException lfe) {
                    Log.e(TAG, "Failed login: " + lfe); // TODO: This should redirect to login
                }
            }
        }

        final Bundle bundle = new Bundle();      // Create the bundle to return

        Log.i(TAG, "Auth Token: " + authToken);
        if (!TextUtils.isEmpty(authToken)) {  // If we got an authToken - we return it

            bundle.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            bundle.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);

        } else {        // Prompt for login
            // If we get here, then we couldn't access the user's password - so we
            // need to re-prompt them for their credentials. We do that by creating
            // an intent to display our AuthenticatorActivity.
            final Intent intent = new Intent(mContext, AuthActivity.class);
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            intent.putExtra(AuthActivity.ARG_ACCOUNT_NAME, account.name);
            intent.putExtra(AuthActivity.ARG_AUTH_TYPE, authTokenType);
            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        }

        return bundle;
    }


    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        Log.i(TAG,"Confirm Credentials");
        return null;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {

        Log.i(TAG,"Get Auth Token Label");
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle getAccountRemovalAllowed(AccountAuthenticatorResponse response, Account account) throws NetworkErrorException {
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
        return result;
    }


    // ------------------------------------------------------------------------
    // Static methods for registering, signing in and invalidating by auth
    // ------------------------------------------------------------------------

    /**
     * @param username The username
     * @param password The password
     * @return
     * @throws Exception
     */
    public static String userRegister(Context context, String username, String password) throws IOException {

        Log.i(TAG,"User Register: " + username + " " + password);
        Map<String, String> params = getAuthParams(context,username,password);

        String response = HttpUtils.performPostCall(RestConstants.register(), params, null);
        String token    = null;
        if (response != null) {
            token = GsonFactory.getValue(response, RestConstants.AUTH_TOKEN);
        }

        return token;

    }

    /**
     * Tries to login with username and password. If successful:
     * - Set my user_id returned from server in shared preferences.
     * - Return the auth token from server.
     * - Set the device id on the server
     *
     * @param username
     * @param password
     * @return a pair with the first being the authtoken and the second being the user id.
     * @throws Exception
     */
    public static String userLogIn(Context context, String username, String password) throws IOException, LoginFailedException {


        Map<String, String> params = getAuthParams(context, username, password);

//        // Set device id for GCM
//        try {
//            params.put(RestConstants.LOGIN_DEVICE_ID,
//                       RegisterDeviceService.getDeviceToken(context));
//            params.put(RestConstants.LOGIN_DEVICE_TYPE, RestConstants.LOGIN_DEVICE_TYPE_VALUE);
//        } catch (IOException ioe) {
//            Log.e(TAG, "Error getting device id, notifications won't work: " + ioe);
//        }


        String authToken = null;  // No authToken sent for login so set to null at first
        Log.i(TAG,"Call " + RestConstants.login() + " with params " + params);
        String response  = HttpUtils.performPostCall(RestConstants.login(), params, authToken);
        Log.i(TAG, "Response: " + response);
        if (response != null) {

            Map<String, String> responseMap = GsonFactory.getMap(response);

            // If login has a SUCCESS value. This means the username was recognized.
            if (responseMap.containsKey(RestConstants.AUTH_LOGIN_SUCCESS)) {

                boolean success = responseMap.get(RestConstants.AUTH_LOGIN_SUCCESS).equals(RestConstants.AUTH_LOGIN_SUCCESS_TRUE);

                if (success) {  // If login was successful, meaning password matched
                    BagginsSharedPreferences bagginsSP = BagginsSharedPreferences.instance(context);
                    bagginsSP.setMyUserId(Long.parseLong(responseMap.get(RestConstants.AUTH_LOGIN_USER_ID)));
                    bagginsSP.setMyUsername(username);
                    authToken = responseMap.get(RestConstants.AUTH_TOKEN);
                } else {    // login or password invalid
                    throw new LoginFailedException(LoginFailedException.Reason.INVALID_CREDENTIALS);
                }
            } else {    // RestConstants.LOGIN_SUCCESS not included in response
                throw new LoginFailedException(LoginFailedException.Reason.OTHER);
            }
        }
        return authToken;


    }


    /**
     * Helper function for login and register.
     *
     * @return A partially constructed Map of parameters shared by login and register
     */
    protected static Map<String, String> getAuthParams(Context context, String username, String password) {
        // Construct the parameters to send to server
        Map<String, String> params = new HashMap<>();
        params.put(RestConstants.AUTH_USERNAME, username); // Set username
        params.put(RestConstants.AUTH_PASSWORD, password); // Set password


//        // Set device id for GCM
//        try {
//            String deviceToken = RegisterDeviceService.getDeviceToken(context);     // Get a new device token
//            params.put(RestConstants.AUTH_DEVICE_ID, deviceToken);                                 // Add device token
//            params.put(RestConstants.AUTH_DEVICE_TYPE, RestConstants.AUTH_DEVICE_TYPE_VALUE);     // Add that this is an Android device
//            Log.i(TAG, "Device Id: " + deviceToken);
//        } catch (IOException ioe) {
//            Log.e(TAG, "Error getting device id, notifications won't work: " + ioe);
//        }

        return params;
    }
    /**
     * Logout from account manager
     *
     * @param context
     */
    public static void logout(final Context context, final AccountManagerCallback<Bundle> callback) {

        final AccountManager accountManager = AccountManager.get(context);

        final Account account = getAccount(context);
        Log.i(TAG, "Account: " + account);
        if (account != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {

//                    // First logout from server
//                    try {
//                        final AccountManager accountManager = AccountManager.get(context);
//                        String               authToken      = accountManager.blockingGetAuthToken(account, AUTH_TOKEN_TYPE, false);  // Get auth token
////                        String               deviceToken    = deviceToken = RegisterDeviceService.getDeviceToken(context);   // Get device token (GCM)
//
//                        Map<String, String> params = new HashMap<>();               // Set device token in params
////                        params.put(RestConstants.LOGOUT_DEVICE_ID, deviceToken);
//
//                        // Call server
//                        String response = HttpUtils.performPostCall(RestConstants.logout(), params, authToken);
//
//                    } catch (Exception e) {
//                        Log.e(TAG, "Error Logging Out From Server: " + e);
//                    }
//

                    // Now logout from local account manager
                    try {
                        String authToken = accountManager.blockingGetAuthToken(account, AUTH_TOKEN_TYPE, true);
                        Log.i(TAG,"Logout Auth Token: " + authToken);
                        accountManager.invalidateAuthToken(getAccountType(context), authToken);
                        Log.i(TAG,"Logout Invalidate: " + authToken);
                        accountManager.setPassword(account, null);
                    } catch (Exception e) {
                        Log.e(TAG, "Error From Client Logging Out: " + e);
                    }

                    callback.run(null);

                }
            }).start();
        }

    }
    /**
     * @param context The context to read the resources file from. Any Activity in this project
     *                will work.
     * @return The account type for the baggins app for the Google Authentication Service.
     * This is defined in the string.xml file under authenticator_account_type.
     */
    public static String getAccountType(Context context) {
        return context.getResources().getString(R.string.authenticator_account_type);
//        throw new RuntimeException("not implemented");
    }

    public static AccountManager getAccountManager(Context context) {
        return AccountManager.get(context);
    }

    /**
     * Try to get the current account given the context
     *
     * @param context
     * @return The account if it exists, or null otherwise.
     * @throws TooManyAccountException If there is more than one account matching the
     *                                 accountType.
     */
    public static Account getAccount(Context context) throws TooManyAccountException {
        String         accountType    = getAccountType(context);
        AccountManager accountManager = getAccountManager(context);
        Account[]      accounts       = accountManager.getAccountsByType(accountType);

        Log.i(TAG,"Get Account Type: " + accountType);
        Log.i(TAG,"Get Account accounts: " + accounts.length);
        Account account = null;
        if (accounts.length > 1) {
            throw new TooManyAccountException(accounts);
        } else if (accounts.length == 1) {
            account = accounts[0];
        }
        return account;
    }


    /**
     * Get the auth token for an existing account on the AccountManager. After the token
     * is set in the auth manager, sets up the sync adapter
     * <p/>
     * If the operation is canceled, finish the passed in activity
     */
    public static void getExistingAccountAuthTokenAndSetupSync(final Activity activity) {

        final AccountManager accountManager = getAccountManager(activity);
        final Account        account        = getAccount(activity);
        // Get the auth token for an account asynchronously. This returns an AccountManagerFuture.
        // It is not to be used on the main thread since it blocks. This is why it is called from a Runnable.
        final AccountManagerFuture<Bundle> future = accountManager.getAuthToken(account, AUTH_TOKEN_TYPE, null, activity, null, null);

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    Bundle bundle = future.getResult();

                    // This should be moved to authlogin
                    BagginsSyncUtils.setAccountSyncable(activity, account);

                    // Add a periodic sync for online
//                    BagginsSyncUtils.addPeriodicSync(account, Constants.GLOBAL_SYNC_INTERVAL);

//                    BagginsSyncUtils.logPeriodicSyncs(account, " On App Start");

//                    // Manual sync both client to server and server to client
//                    BagginsSync.newSync(activity)
//                               .upSync()
//                               .downSync()
//                               .sync();


                } catch (OperationCanceledException e) {
                    Log.i(TAG, "Get Existing Account canceled, exiting.");
                    activity.finish();
                } catch (IOException e) {
                    Log.e(TAG, "IOException: " + e);
                } catch (AuthenticatorException e) {
                    Log.e(TAG, "Authenticator Exception: " + e);
                }
            }
        }).start();
    }

    /**
     * Login to the server given the credentials in the AccountManager. This also registers
     * the device id with the node server for gcm
     *
     * @param context
     * @throws IOException
     * @throws LoginFailedException
     */
    public static void loginServerFromAccountManager(Context context) throws IOException, LoginFailedException {
        AccountManager accountManager = getAccountManager(context);
        Account        account        = getAccount(context);
        String         password       = accountManager.getPassword(account);

        BagginsAuthenticator.userLogIn(context, account.name, password);

    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

}
