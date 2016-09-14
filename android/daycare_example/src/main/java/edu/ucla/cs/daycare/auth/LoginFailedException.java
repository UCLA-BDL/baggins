package edu.ucla.cs.daycare.auth;

/**
 * Created by ethan on 3/21/16.
 */
public class LoginFailedException extends Exception {

    public enum Reason {INVALID_CREDENTIALS, OTHER};
    private final Reason mReason;
    public LoginFailedException(Reason reason) {
        mReason = reason;
    }

    public boolean areCredentialsInvalid() {
        return mReason == Reason.INVALID_CREDENTIALS;
    }
}
