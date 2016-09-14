package edu.ucla.cs.baggins.data.sync;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;

import edu.ucla.cs.baggins.data.provider.BagginsContract;

/**
 * Created by Ethan L. Schreiber on 3/30/16.
 *
 * Use this class to initiate either an upsync from client to server or a downsync from server
 * to client.
 */
public class BagginsSync {
    public final static String TAG = "baggins_sync_builder";

    /**
     * @return The base bundle for all syncs
     */
    protected static Bundle createBaseBundle() {

        // This bundle is how we pass information to the sync about the type of sync to perform.
        Bundle bundle = new Bundle();
        // Disable sync backoff and ignore sync preferences. In other words...perform sync NOW!
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

        return bundle;

    }
    /**
     * Perform the sync.
     */
    protected static void sync(Context context, Account account , Bundle bundle) {
        ContentResolver.setIsSyncable(account, BagginsContract.getAuthority(), 1);
        ContentResolver.requestSync(
                account, // SyncModel account
                BagginsContract.getAuthority(),      // Content authority
                bundle);
    }

    /**
     * Perform an up sync to update from client to server.
     * (This is the public static method which should be called by outside classes).
     * @param context The android context.
     */
    public static void upSync(Context context, Account account) {
        Bundle bundle = createBaseBundle();
        bundle.putBoolean(BagginsSyncAdapter.ARG_UP_SYNC, true);
        BagginsSync.sync(context,account,bundle);
    }

    /**
     * Perform a down sync to update from server to client.
     * (This is the public static method which should be called by outside classes).
     * @param context The android context.
     * @param syncName The name of the down sync as specified on the server.
     */
    public static void downSync(Context context, Account account, String syncName) {
        Bundle bundle = createBaseBundle();
        bundle.putString(BagginsSyncAdapter.ARG_DOWN_SYNC_FEED_NAME, syncName);
        BagginsSync.sync(context,account,bundle);

    }
}


