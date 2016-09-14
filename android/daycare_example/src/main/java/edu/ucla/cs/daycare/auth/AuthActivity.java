package edu.ucla.cs.daycare.auth;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.ucla.cs.daycare.R;


//http://www.fussylogic.co.uk/blog/?p=1031

/**
 * Created by ethan on 10/5/15.
 * This Activity asks the user to login
 * see: http://blog.udinic.com/2013/04/24/write-your-own-android-authenticator/
 */
public class AuthActivity extends AccountAuthenticatorActivity {

    // -----------------------------------------------------------
    // Keys to pass arguments to this activity through the intent.
    // -----------------------------------------------------------
    public final static String ARG_ACCOUNT_TYPE          = "ACCOUNT_TYPE";
    public final static String ARG_AUTH_TYPE             = "AUTH_TYPE";
    public final static String ARG_ACCOUNT_NAME          = "ACCOUNT_NAME";
    public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";
    public final static String ARG_USER_PASS             = "USER_PASS";
    public static final String ARG_ERROR_MESSAGE         = "ERR_MSG";


    private final int REQ_SIGNUP = 1;

    private final String TAG = this.getClass().getSimpleName();

    /**
     * The type of token from the server.
     * For example, could be:
     * AccountStaticConstants.AUTHTOKENr_TYPE_FULL_ACCESS or
     * AccountStaticConstants.AUTHTOKEN_TYPE_READ_ONLY
     */
    private String mAuthType;


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_activity);

        final EditText usernameBox = (EditText) findViewById(R.id.auth_login_username);
        final EditText passwordBox = (EditText) findViewById(R.id.auth_login_password);

        mAuthType = getIntent().getStringExtra(ARG_AUTH_TYPE);

        if (mAuthType == null) {  // Set mAuthType Default value
            mAuthType = BagginsAuthenticator.AUTH_TOKEN_TYPE;
        }

        // If the username was passed, set the username in the textbox
        if (getIntent().hasExtra(ARG_ACCOUNT_NAME)) {
            String username = getIntent().getStringExtra(ARG_ACCOUNT_NAME);
            usernameBox.setText(username);
        }

        // Setup the login button
        findViewById(R.id.auth_login_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateSubmit(usernameBox, passwordBox)) {
                    login();
                }
            }
        });

        // --------------------------------------------------------------------
        //  Setup the register button
        // --------------------------------------------------------------------
//        findViewById(R.id.auth_register_button).setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                if (validateSubmit(usernameBox, passwordBox)) {
//                    // Get the layout inflater
//                    LayoutInflater inflater = AuthActivity.this.getLayoutInflater();
//                    registerUser(usernameBox.getText().toString(), passwordBox.getText().toString());
//                }
//            }
//        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        // The sign up activity returned that the user has successfully created an account
        if (requestCode == REQ_SIGNUP && resultCode == RESULT_OK) {
            finishLogin(intent);
        } else
            super.onActivityResult(requestCode, resultCode, intent);
    }

    /**
     * Create a snackbar message to display at the bottom of the coordinator layout.
     *
     * @param message The message to display
     */
    private void createToast(String message) {
        Toast.makeText(this,message,Toast.LENGTH_LONG);
    }


    // ------------------------------------------------------------------------
    // For login
    // ------------------------------------------------------------------------

    /**
     * Called when the login form is submitted
     */
    public void login() {

        final String username = ((TextView) findViewById(R.id.auth_login_username)).getText().toString();
        final String password = ((TextView) findViewById(R.id.auth_login_password)).getText().toString();


        new AsyncTask<String, Void, Intent>() {

            @Override
            protected Intent doInBackground(String... params) {

                Bundle intentExtras = new Bundle();

                try {

                    String accountType = BagginsAuthenticator.getAccountType(getBaseContext());
                    String authToken   = BagginsAuthenticator.userLogIn(getBaseContext(), username, password);

                    intentExtras.putString(AccountManager.KEY_ACCOUNT_NAME, username);
                    intentExtras.putString(AccountManager.KEY_AUTHTOKEN, authToken);
                    intentExtras.putString(AccountManager.KEY_PASSWORD, password);
                    intentExtras.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);

                } catch (IOException e) {
                    intentExtras.putString(ARG_ERROR_MESSAGE, e.getMessage());  // This read in onPostExecute
                } catch (LoginFailedException lfe) {
                    if (lfe.areCredentialsInvalid()) {
                        intentExtras.putString(ARG_ERROR_MESSAGE, "Invalid username or password");  // This read in onPostExecute
                    } else {
                        intentExtras.putString(ARG_ERROR_MESSAGE, "Error while attempting to log in."); // This read in onPostExecute
                    }
                }


                final Intent response = new Intent();
                response.putExtras(intentExtras);
                return response;
            }

            @Override
            protected void onPostExecute(Intent intent) {
                if (intent.hasExtra(ARG_ERROR_MESSAGE)) {
                    createToast(intent.getStringExtra(ARG_ERROR_MESSAGE));
                } else {
                    finishLogin(intent);
                }
            }
        }.execute();
    }


    /**
     * Called  from login after a user is successfully authenticated.
     *
     * @param intent
     */
    private void finishLogin(Intent intent) {

        // Read the username, password  and account typefrom the intent set in login() method above.
        String        username    = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        String        password    = intent.getStringExtra(AccountManager.KEY_PASSWORD);
        String        accountType = intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);
        final Account account     = new Account(username, accountType);

        // Get the account manager
        AccountManager accountManager = AccountManager.get(getBaseContext());

        // Add the account
        accountManager.addAccountExplicitly(account, password, null); // create new account

        // Get the auth token set in the login() method
        String authtoken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);

        accountManager.setAuthToken(account, mAuthType, authtoken);    // Set the auth token
        accountManager.setPassword(account, password);                 // Set the password

        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);

        // Finish this activity
        finish();
    }

    // ------------------------------------------------------------------------
    // For register
    // ------------------------------------------------------------------------

    /**
     * @param username
     * @param password
     */
    public void registerUser(final String username, final String password) {
        new AsyncTask<String, Void, Intent>() {

            @Override
            protected Intent doInBackground(String... params) {

                Bundle data = new Bundle();
                try {
                    String authtoken   = BagginsAuthenticator.userRegister(getBaseContext(),username, password);
                    String accountType = BagginsAuthenticator.getAccountType(getBaseContext());

                    data.putString(AccountManager.KEY_ACCOUNT_NAME, username);
                    data.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);
                    data.putString(AccountManager.KEY_AUTHTOKEN, authtoken);
                    data.putString(AuthActivity.ARG_USER_PASS, password);
                } catch (Exception e) {
                    data.putString(AuthActivity.ARG_ERROR_MESSAGE, e.getMessage());
                }

                final Intent res = new Intent();
                res.putExtras(data);
                return res;
            }

            @Override
            protected void onPostExecute(Intent intent) {
                if (intent.hasExtra(AuthActivity.ARG_ERROR_MESSAGE)) {
                    createToast(intent.getStringExtra(AuthActivity.ARG_ERROR_MESSAGE));
                } else {
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        }.execute();

    }

    /**
     * This makes sure the input is valid to submit the login/password. If not, prints a snackbar and
     * returns false, returns true if valid.
     *
     * @param username
     * @param password
     * @return true if the data is valid, false otherwise.
     */
    protected boolean validateSubmit(EditText username, EditText password) {
        List<String> errors = new ArrayList<>();

        if (username.getText().toString().trim().isEmpty()) {
            errors.add("The username cannot be empty.");
        }

        if (password.getText().toString().trim().isEmpty()) {
            errors.add("The password cannot be empty.");
        }


        if (!errors.isEmpty()) {
            createToast(TextUtils.join("\n", errors));
        }

        return errors.isEmpty();
    }

}
