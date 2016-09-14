package edu.ucla.cs.daycare.auth;

import android.accounts.Account;

/**
 * Created by ethan on 3/25/16.
 */
public class TooManyAccountException extends RuntimeException {


    public TooManyAccountException(Account[] accounts) {
        super(getAccountNamesString(accounts));
    }

    protected static String getAccountNamesString(Account[] accounts) {

        StringBuilder sb = new StringBuilder();
        for (Account account : accounts) {
            sb.append("   ").append(account.toString()).append("\n");
        }
        return sb.toString();
    }
}
