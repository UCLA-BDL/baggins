package edu.ucla.cs.baggins.data.sync;

/**
 * Created by ethan on 8/9/16.
 *
 * At least some of this stuff should probably be defined in build.gradle. Specifically, look
 * into productFlavors. For example:
 *
 productFlavors {
    dev {
        applicationIdSuffix ".dev"

        resValue "string", "content_provider_authority", defaultConfig.applicationId + '.dev.provider'
        resValue "string", "authenticator_account_type", defaultConfig.applicationId + ".dev.account"

        buildConfigField "String", "CONTENT_PROVIDER_AUTHORITY", "\"" + defaultConfig.applicationId + ".dev.provider\""
        buildConfigField "String", "ACCOUNT_TYPE", "\"" + defaultConfig.applicationId + ".dev.account\""
        buildConfigField "String", "SERVER_URL", "\"http://localhost:8080\""
        ...
    }
 }
 *
 */

public class RestConstants {

    /**
     * The URL of the REST server.
     */
    protected static String SERVER_URL = "http://10.0.1.42:3005";

    /**
     * The key for passing username to register/login on the server
     */
    public final static String AUTH_USERNAME = "username";

    /**
     * The key for passing password to register/login on the server
     */
    public final static String AUTH_PASSWORD = "password";

    /**
     * The key for returning the token
     */
    public final static String AUTH_TOKEN= "token";

    /**
     * Was login successful key
     */
    public final static String AUTH_LOGIN_SUCCESS = "success";


    /**
     * Login was successful
     */
    public final static String AUTH_LOGIN_SUCCESS_TRUE = "true";

    /**
     * Login was successful
     */
    public final static String AUTH_LOGIN_USER_ID = "user_id";


    /**
     * @return The url for registering a user.
     */
    public static String register() {
        return SERVER_URL + "/user";
    }

    /**
     * @return The url for logging in a user.
     */
    public static String login() {
        return SERVER_URL + "/login";
    }
}
