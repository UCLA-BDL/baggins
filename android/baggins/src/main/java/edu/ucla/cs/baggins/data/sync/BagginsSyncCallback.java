package edu.ucla.cs.baggins.data.sync;

/**
 * Created by ethan on 8/18/16.
 */
public interface BagginsSyncCallback {

    /**
     *
     */
    public void onSyncComplete(String syncType, boolean isSuccesful);
}
