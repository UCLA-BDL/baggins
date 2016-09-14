package edu.ucla.cs.daycare.auth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;


//        FOLLOW THIS: http://www.fussylogic.co.uk/blog/?p=1031

/**
 * Created by ethan on 10/6/15.
 * See: http://www.fussylogic.co.uk/blog/?p=1031
 */
public class BagginsAuthenticatorService extends Service {

    private final static String TAG = "baggins_auth_service";

    // ------------------------------------------------------------------------
    // The methods to override Service
    // ------------------------------------------------------------------------

    @Override
    public IBinder onBind(Intent intent) {

        BagginsAuthenticator authenticator = new BagginsAuthenticator(this);
        return authenticator.getIBinder();
    }

}
