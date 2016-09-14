package edu.ucla.cs.baggins.data.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Service to handle sync requests.
 * <p>
 * <p>This service is invoked in response to Intents with android.content.SyncConstants, and
 * returns a Binder connection to SyncConstants.
 * <p>
 * <p>For performance, only one sync adapter will be initialized within this application's context.
 * <p>
 * <p>Note: The SyncService itself is not notified when a new sync occurs. It's role is to
 * manage the lifecycle of our {@link BagginsSyncAdapter} and provide a handle to said SyncConstants to the
 * OS on request.
 */
public class BagginsSyncService extends Service {
    private static final String TAG = "baggins_sync_service";

    private static final Object             sSyncAdapterLock = new Object();
    private static       BagginsSyncAdapter sSyncAdapter     = null;

    /**
     * Thread-safe constructor, creates static {@link BagginsSyncAdapter} with.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new BagginsSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    /**
     * Return Binder handle for IPC communication with {@link BagginsSyncAdapter}.
     * <p>
     * <p>New sync requests will be sent directly to the SyncConstants using this channel.
     *
     * @param intent Calling intent
     * @return Binder handle for {@link BagginsSyncAdapter}
     */
    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.getSyncAdapterBinder();
    }
}
