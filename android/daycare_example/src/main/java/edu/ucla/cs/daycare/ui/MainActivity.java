package edu.ucla.cs.daycare.ui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.ucla.cs.baggins.data.provider.BagginsContract;
import edu.ucla.cs.baggins.data.sync.BagginsSync;
import edu.ucla.cs.daycare.R;
import edu.ucla.cs.daycare.auth.BagginsAuthenticator;
import edu.ucla.cs.daycare.model.User;
import edu.ucla.cs.daycare.test.PopulateDB;


public class MainActivity extends AppCompatActivity {

    public static String TAG = "main_activity";


    /**
     * Only call loginCompleted once.
     */
    private boolean mWasLoginCompleted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG,"Create");
        setContentView(R.layout.main_activity);

        BagginsContract.setAuthority(this, R.string.authority);

        findViewById(R.id.main_logout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"LOGOUT");
                BagginsAuthenticator.logout(MainActivity.this, new AccountManagerCallback<Bundle>() {
                    @Override
                    public void run(AccountManagerFuture<Bundle> future) {
                        Log.i(TAG,"LOGGED OUT");
                        getAuthToken();

                    }
                });
            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG,"Resume");

//        Log.i(TAG,"Temp logout, REMOVE ME");
//
//        BagginsAuthenticator.logout(getApplicationContext(), getAccountType(getApplicationContext()), new AccountManagerCallback<Bundle>() {
//            @Override
//            public void run(AccountManagerFuture<Bundle> future) {
//
//                // When the logout is done, call getAuthToken. Since we have
//                // just logged out, this will bring up the login screen.
//
//                getAuthToken();
//            }
//        });

        // ------------------------------------------------------------------------
        // Try to login, then setup sync adapter if we are logged in
        // ------------------------------------------------------------------------
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG,"About to call Baggins Auth!");
                Account account = BagginsAuthenticator.getAccount(MainActivity.this);

                Log.i(TAG, "Account: " + account);
                if (account == null) {      //  If no account
                    addNewAccount();        //  Add new one. This
                } else {
                    Log.i(TAG, "MainActivity.onResume().getAuthToken()");
                    getAuthToken();
                }
                Log.i(TAG,"Async Done");
            }

        });
    }

    // ------------------------------------------------------------------------
    // Authentication methods
    // ------------------------------------------------------------------------

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

    /**
     * Add new account to the account manager. When this is done, it calls getAuthToken.
     */
    public void addNewAccount() {
        final AccountManager accountManager = BagginsAuthenticator.getAccountManager(this);
        final String         accountType    = getAccountType(this);

        Log.i(TAG,"About to Add New Account through accountManager: " + accountType);
        final AccountManagerFuture<Bundle> future = accountManager.addAccount(accountType, BagginsAuthenticator.AUTH_TOKEN_TYPE, null, null,
                                                                                 this, new AccountManagerCallback<Bundle>() {
                    @Override
                    public void run(AccountManagerFuture<Bundle> future) {
                        try {
                            Log.i(TAG,"Getting Result");
                            future.getResult();  // This blocks until the future is ready.
                            Log.i(TAG,"Got Result");
                            getAuthToken();

                        } catch (OperationCanceledException e) {
                            Log.i(TAG, "Add new Account canceled, exiting.");
                            finish();
                        } catch (IOException e) {
                            Log.e(TAG, "IOException: " + e);
                        } catch (AuthenticatorException e) {
                            Log.e(TAG, "Authenticator Exception: " + e);
                        }
                    }
                }, null);

    }


    /**
     * Get the auth token for an existing account on the AccountManager. If there is no valid
     * auth token, this prompts for a login.
     * If the operation is canceled, exit app.
     */
    protected void getAuthToken() {

        final AccountManager accountManager = BagginsAuthenticator.getAccountManager(MainActivity.this);
        final Account        account        = BagginsAuthenticator.getAccount(MainActivity.this);

        // This is the callback for accountManager.getAuthToken called below.
        // THis sets up the syncadapter and sets up the view
        AccountManagerCallback<Bundle> callback = new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> future) {

                try {
                    future.getResult(); // This blocks until the future is ready.
                } catch (OperationCanceledException e) {
                    Log.i(TAG, "Get Existing Account canceled, exiting.");
                    mWasLoginCompleted = true; // Don't keep going, exit app
                    finish();
                } catch (Exception e) {
                    Log.e(TAG, "Exception!!!: " + e);
                }
 
                if (!mWasLoginCompleted) {
                    mWasLoginCompleted = true;

                    // ------------------------------------------------------------------------
                    // Setup Sync
                    // ------------------------------------------------------------------------
                    if (accountManager.addAccountExplicitly(account, null, null)) {
                        ContentResolver.setIsSyncable(account, BagginsContract.getAuthority(), 1);
                        ContentResolver.setMasterSyncAutomatically(true);
                        ContentResolver.setSyncAutomatically(account, BagginsContract.getAuthority(), true);
                    }

                    PopulateDB.execute(MainActivity.this);

                    // --------------------------------------------------------------------
                    // Create RecyclerView
                    // --------------------------------------------------------------------

                    List<User> users;
                    try {
                        users = User.with(MainActivity.this).query(null, null, null);
                    } catch (RemoteException e) {
                        users = new ArrayList<>(0);
                        Log.e(TAG, "Error: " + e);
                    }
                    Log.i(TAG, "User List: " + users.size());
                    RecyclerView    userList        = (RecyclerView) findViewById(R.id.main_user_list);
                    UserListAdapter userListAdapter = new UserListAdapter(users);
                    userList.setAdapter(userListAdapter);
                    userList.setLayoutManager(new LinearLayoutManager(MainActivity.this));

                    Account account = BagginsAuthenticator.getAccount(MainActivity.this);
                    BagginsSync.upSync(MainActivity.this,account);
                    BagginsSync.downSync(MainActivity.this,account,"EthanSync");
                }
            }
        };

        final AccountManagerFuture<Bundle> future =
                accountManager.getAuthToken(account, BagginsAuthenticator.AUTH_TOKEN_TYPE, null, this, callback, null);

    }

}
