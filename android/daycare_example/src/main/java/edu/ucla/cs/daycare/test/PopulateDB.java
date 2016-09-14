package edu.ucla.cs.daycare.test;

import android.content.ContentProviderClient;
import android.content.Context;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import edu.ucla.cs.baggins.data.provider.BagginsContract;
import edu.ucla.cs.daycare.model.Child;
import edu.ucla.cs.daycare.model.User;

/**
 * Created by ethan on 5/25/16.
 */
public class PopulateDB {
    public final static String TAG = "populate_db";

    public static void execute(Context context) {
        try {
            ContentProviderClient client =
                    context.getContentResolver().acquireContentProviderClient(BagginsContract.getAuthorityURI());

            // ----------------------------------------------------------------
            // U S E R S
            // ----------------------------------------------------------------

            // Delete all users
            User.with(context).delete(null, null);

            // Insert test users
            User.with(context)
                .address1("1629 Selby Ave")
                .address2("Apt C")
                .firstName("Ethan")
                .lastName("Schreiber")
                .phoneNumber("917-838-9657")
                .city("Los Angeles")
                .zipcode("90024")
                .state("CA")
                .save();

            List<User> users = User.with(context, client).query(null, null, null);
            for (User u : users) {
                Log.i(TAG, "User: " + u.address1() + "  Address1: " + u.address1());
            }


            // ----------------------------------------------------------------
            // C H I L D R E N
            // ----------------------------------------------------------------

            Uri uri = Child.with(context)
                           .firstName("Isaac")
                           .lastName("Schreiber")
                           .birthday(new GregorianCalendar(2015, Calendar.SEPTEMBER, 11).getTime())
                           .save();

            Log.i(TAG, "URI 1: " + uri);
            uri = Child.with(context)
                       .firstName("Isaac")
                       .lastName("Schreiber")
                       .birthday(new GregorianCalendar(2015, Calendar.SEPTEMBER, 11).getTime())
                       .save();
            Log.i(TAG, "URI 2: " + uri);

            client.release();

        } catch (RemoteException e) {
            Log.e(TAG, "Error: " + e);
        }
    }
}
