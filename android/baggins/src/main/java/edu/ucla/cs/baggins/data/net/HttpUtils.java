package edu.ucla.cs.baggins.data.net;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import edu.ucla.cs.baggins.constants.Constants;

/**
 * Created by Ethan L. Schreiber on 11/2/15.
 */
public class HttpUtils {
    private final static String TAG = "http_utils";

    // ------------------------------------------------------------------------
    // Constants for creating request strings
    // ------------------------------------------------------------------------

    private final static String POST           = "POST";
    private final static String GET            = "GET";
    private final static String CT_JSON        = "application/json";
    private final static String CT_URL_ENCODED = "application/x-www-form-urlencoded";

    /**
     * Get an HttpURLConnection.
     *
     * @param url           The url to connect to.
     * @param requestMethod The request method (i.e. POST or GET).
     * @param contentType   The content-type, (e.g. application/json).
     * @param authToken     The auth token for connecting. Will be set only if not null.
     * @return An HttpURLConnection object
     * @throws IOException
     */
    protected static HttpURLConnection getConnection(String url, String requestMethod, String contentType, String authToken) throws IOException {

        Log.i(TAG, "Get u: " + url);
        URL u = new URL(url);

        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setReadTimeout(Constants.NET_READ_TIMEOUT_MILLIS /* milliseconds */);
        conn.setConnectTimeout(Constants.NET_CONNECT_TIMEOUT_MILLIS /* milliseconds */);
        conn.setRequestMethod(requestMethod);
        conn.setRequestProperty("Content-Type", contentType);


        if (requestMethod.equals(POST)) {       // If a POST
            conn.setDoOutput(true);             // Then further output will be appended
        }

        if (authToken != null) {
            conn.setRequestProperty(Constants.AUTH_TOKEN_KEY, authToken);
        }

        return conn;
    }

    /**
     * Get JSON from server.
     *
     * @param url
     * @return
     */
    public static String getJSON(String url, String authToken) throws IOException {

        HttpURLConnection conn = getConnection(url, GET, CT_JSON, authToken);
        conn.setUseCaches(false);
        conn.setAllowUserInteraction(false);
        return connectionToResponseString(conn);
    }

    /**
     * Perform a post to the server.
     *
     * @param requestURL     The url for the post request
     * @param postDataParams The post params
     * @return
     * @throws IOException
     */
    public static String performPostCall(String requestURL, Map<String, String> postDataParams, String authToken)
    throws IOException {

        HttpURLConnection conn   = getConnection(requestURL, POST, CT_URL_ENCODED, authToken);
        OutputStream      os     = conn.getOutputStream();
        BufferedWriter    writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(formatPostParams(postDataParams));
        writer.flush();
        writer.close();
        os.close();

        return connectionToResponseString(conn);
    }


    // Add binary in later. Will need to implement FileUtils.pipe(is,os)
//    /**
//     * Perform a binary post to the server.
//     *
//     * @param requestURL The url for the post request
//     * @param inputFile  The inputfile containing the binary data to write
//     * @param mimeType   The content type of the request
//     * @param authToken  The authentication token
//     * @return
//     * @throws IOException
//     */
//    public static String performBinaryPostCall(String requestURL, File inputFile, String mimeType, String authToken)
//    throws IOException {
//
//        URL url = new URL(requestURL);
//
//        HttpURLConnection conn = getConnection(requestURL, POST, mimeType, authToken);
//
//        OutputStream os = conn.getOutputStream();
//        InputStream  is = new FileInputStream(inputFile);
//        FileUtils.pipe(is, os);
//        os.flush();
//        os.close();
//        is.close();
//
//        return connectionToResponseString(conn);
//    }

    /**
     * Convert the post params to UTF-8 encoded strings of the form
     * key1=val1&key2=val2&key3=val3...
     *
     * @param params The map of params
     * @return A UTF-8 encoded string of the form key1=val1&key2=val2&key3=val3...
     * @throws UnsupportedEncodingException
     */
    private static String formatPostParams(Map<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean       first  = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                  .append("=")
                  .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }


    /**
     * Get the response from the HttpURLConnection and convert it to a string
     *
     * @param conn
     * @return
     * @throws IOException
     */
    private static String connectionToResponseString(final HttpURLConnection conn) throws IOException {
        try {
            conn.connect();
            int status = conn.getResponseCode();
            Log.i(TAG, "Http Status: " + status);
            switch (status) {
                case 200:
                case 201:
                    StringBuilder sb = new StringBuilder();
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    br.close();
                    return sb.toString();

                default:
                    Log.i(TAG, "Throw Error");
                    throw new IOException("Response code: " + status +
                                          ".\nResponse Message " + conn.getResponseMessage());
            }
        } finally {
            Log.i(TAG, "Disconnecting");
            conn.disconnect();
        }
    }
}
