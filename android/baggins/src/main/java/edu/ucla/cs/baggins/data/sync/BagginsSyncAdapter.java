package edu.ucla.cs.baggins.data.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.net.ParseException;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;


/*
 * Define a sync adapter for the app.
 *
 * <p>This class is instantiated in {@link BagginsSyncService}, which also binds SyncConstants to the system.
 * SyncConstants should only be initialized in SyncService, never anywhere else.
 *
 * <p>The system calls onPerformSync() via an RPC call through the IBinder object supplied by
 * SyncService.
 */
public class BagginsSyncAdapter extends AbstractThreadedSyncAdapter {
    public static final String TAG = "baggins_sync_adapter";

    /**
     * For passing a value into the sync extras bundle to determine if we should sync client to server.
     */
    public static final String ARG_UP_SYNC = "arg_up_sync";

    /**
     * For passing a value into the sync extras bundle to initiate a downsync with the name given.
     */
    public static final String ARG_DOWN_SYNC_FEED_NAME = "arg_down_sync_feed_name";

    /**
     * For logging in and getting auth token.
     */
//    private final AccountManager mAccountManager;

    /**
     * Constructor. Obtains handle to content resolver for later use.
     */
    public BagginsSyncAdapter(Context context, boolean autoInitialize) {
        this(context, autoInitialize, false);
    }

    /**
     * Constructor. Obtains handle to content resolver for later use.
     */
    public BagginsSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);

//        CookieManager cookieManager = new CookieManager(new PersistentCookieStore(context), CookiePolicy.ACCEPT_ORIGINAL_SERVER);
//        CookieHandler.setDefault(cookieManager);
//        mAccountManager = AccountManager.get(context);
    }

    /**
     * Called by the Android system in response to a request to run the sync adapter. The work
     * required to read data from the network, parse it, and store it in the content provider is
     * done here. Extending AbstractThreadedSyncAdapter ensures that all methods within SyncConstants
     * run on a background thread. For this reason, blocking I/O and other long-running tasks can be
     * run <em>in situ</em>, and you don't have to set up a separate thread for them.
     * .
     * <p>
     * <p>This is where we actually perform any work required to perform a sync.
     * {@link AbstractThreadedSyncAdapter} guarantees that this will be called on a non-UI thread,
     * so it is safe to peform blocking I/O here.
     * <p>
     * <p>The syncResult argument allows you to pass information back to the method that triggered
     * the sync.
     */
    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient cpClient, SyncResult syncResult) {

        // Get the auth token for the current account and
        // The false means prompt for login if no auth
        String  authToken    = null;
        boolean errorSyncing = false;
        try {
//            authToken = mAccountManager.blockingGetAuthToken(account,Constants.AUTHTOKEN_TYPE_FULL_ACCESS, false);

            // Call the client to server sync
            if (extras.getBoolean(ARG_UP_SYNC, false)) {
                Log.i(TAG,"UP SYNC!");
                onPerformSyncClientToServer(authToken, cpClient);
            }
            // Call the server to client sync. This can only be called a max of once every
            // 60 seconds. If it has been called within the last MIN_PERIOD seconds, this request will
            // be ignored.
            if (extras.containsKey(ARG_DOWN_SYNC_FEED_NAME)) {
                String feedName = extras.getString(ARG_DOWN_SYNC_FEED_NAME);
                Log.i(TAG,"DOWN SYNC with name " + feedName);
                onPerformSyncServerToClient(authToken, cpClient);
            }

        } catch (ParseException e) {
            errorSyncing = true;
            Log.e(TAG, "Error parsing feed: " + e.toString());
            syncResult.stats.numParseExceptions++;
        } catch (RemoteException e) {
            errorSyncing = true;
            Log.e(TAG, "Remote Exception: " + e);
            syncResult.stats.numIoExceptions++;
        } catch (IOException e) {
            errorSyncing = true;
            syncResult.stats.numIoExceptions++;
            Log.e(TAG, "IO Exception: " + e);
        }
//        catch (AuthenticatorException e) {
//            errorSyncing = true;
//            syncResult.stats.numAuthExceptions++;
//            Log.e(TAG, "Auth Exception: " + e);
//        } catch (OperationCanceledException e) {
//            errorSyncing = true;
//            Log.e(TAG, "Operation Canceled Exception: " + e);
//        }

        if (errorSyncing) {
            Log.e(TAG, "****************************************");
            Log.e(TAG, "*** ERROR SYNCING, DID NOT COMPLETE! ***");
            Log.e(TAG, "****************************************");
        }
    }

    // ------------------------------------------------------------------------
    // C L I E N T    T O    S E R V E R
    // ------------------------------------------------------------------------

    /**
     * Perform the sync from client to server.
     *
     * @param authToken The authentication token for communicating with  the server.
     * @param cpClient  The ContentProviderClient which is used for reading/writing to ContentProvider tables.
     */
    protected void onPerformSyncClientToServer(String authToken, ContentProviderClient cpClient) throws RemoteException, IOException {

//            // Notify that the Post table has changed
//            if (!mediaList.isEmpty()) {
//                Uri CONTENT_URI = contentprovider.Baggins.getContentUri(Baggins.Post.CONTENT_NAME);
//                getContext().getContentResolver().notifyChange(CONTENT_URI, null, false);
//            }
    }

    // ------------------------------------------------------------------------
    // S E R V E R    T O    C L I E N T
    // ------------------------------------------------------------------------

    /**
     * Perform the sync from server to client. This method can be called a maximum of once every
     * MAX_SYNC_PERIOD seconds.
     *
     * @param authToken The authentication token for communicating with  the server.
     * @param cpClient  The ContentProviderClient which is used for reading/writing to ContentProvider tables.
     */
    protected void onPerformSyncServerToClient(String authToken, ContentProviderClient cpClient) throws RemoteException, IOException {

//        long lastTimestamp = api.syncGetTouchedTime();

        // Get the json in string form.
//        String data = HttpUtils.getJSON(RestConstants.sync(lastTimestamp), authToken);


        // For references, see this: http://stackoverflow.com/questions/5554217/google-gson-deserialize-listclass-object-generic-type

        // Use GSON to parse the json
//        SyncModel syncModel = GsonFactory.get().fromJson(data, SyncModel.class);

        // Set the time difference between the server clock and the client clock.
//        BagginsSharedPreferences.instance(getContext()).setClientServerTimeDifference(syncModel.timestamp.getTime());



    }


}
