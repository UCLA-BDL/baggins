package edu.ucla.cs.baggins.data.sync;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.PeriodicSync;
import android.os.Bundle;
import android.util.Log;

import java.util.List;

import edu.ucla.cs.baggins.data.provider.BagginsContract;


/**
 * Static helper methods for working with the sync framework.
 *
 * This should be moved to BagginsSync.class
 */
public class BagginsSyncUtils {
    private final static String TAG = "baggins_sync_utils";

    /**
     * Make an account syncable
     *
     * @param context Context
     * @param account The account to make syncable
     */
    public static void setAccountSyncable(Context context, Account account) {

        // Inform the system that this account supports sync
        ContentResolver.setIsSyncable(account, BagginsContract.getAuthority(), 1);

        // Inform the system that this account is eligible for auto sync when the network is up
        ContentResolver.setSyncAutomatically(account, BagginsContract.getAuthority(), true);
    }

//    /**
//     * @param clientToServer
//     * @param serverToClient
//     * @return
//     */
//    protected static Bundle getBundle(boolean clientToServer, boolean serverToClient) {
//        Bundle b = new Bundle();
//        b.putBoolean(BagginsSyncAdapter.ARG_UP_SYNC, clientToServer);
//        b.putBoolean(BagginsSyncAdapter.ARG_SYNC_SERVER_TO_CLIENT, serverToClient);
//        return b;
//    }

//    /**
//     * Add a periodic sync for client 2 server and server 2 client.
//     *
//     * @param account      The account to sync on.
//     * @param syncInterval The amount of seconds between sync
//     */
//    public static void addPeriodicSync(Account account, long syncInterval) {
//
//        if (account != null) {
//            // SyncModel client 2 server and server 2 client
//            Bundle b = getBundle(true, true);
//
//            // Turn on periodic syncing
//            ContentResolver.addPeriodicSync(
//                    account, BagginsContract.getAuthority(), b, syncInterval);
//        }
//    }

    public static void logPeriodicSyncs(Account account, String actionHeader) {
        List<PeriodicSync> syncs = ContentResolver.getPeriodicSyncs(account, BagginsContract.getAuthority());

        Log.i(TAG, "*** " + actionHeader + " ***");
        Log.i(TAG, "Num Periodic Syncs: " + syncs.size());
        for (PeriodicSync sync : syncs) {
            Log.i(TAG, "   SyncModel Details: " + sync.toString());

            if (sync.extras != null) {

                Log.i(TAG, "   SyncModel Bundle:");
                for (String key : sync.extras.keySet()) {
                    Log.i(TAG, "      [" + key + "] = " + sync.extras.get(key));
                }
            }
        }


    }





}
