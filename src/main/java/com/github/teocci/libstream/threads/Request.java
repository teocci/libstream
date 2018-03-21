package com.github.teocci.libstream.threads;

import com.github.teocci.libstream.enums.RtspMethod;
import com.github.teocci.libstream.utils.LogHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Dec-14
 */

public class Request
{
    private static String TAG = LogHelper.makeLogTag(Request.class);

    // Parse method & uri
    public static final Pattern regexMethod = Pattern.compile("(\\w+) (\\S+) RTSP", Pattern.CASE_INSENSITIVE);
    // Parse a request header
    public static final Pattern regexHeader = Pattern.compile("(\\S+):(.+)", Pattern.CASE_INSENSITIVE);

    public RtspMethod method;
    public String uri;
    public HashMap<String, String> headers = new HashMap<String, String>();

    /**
     * Parse the method, uri & headers of a RTSP request
     */
    public static Request parseRequest(BufferedReader input) throws IOException, IllegalStateException, SocketException
    {
        Request request = new Request();
        String line;
        Matcher matcher;

        // Parsing request method & uri
        if ((line = input.readLine()) == null) throw new SocketException("Client disconnected");

        matcher = regexMethod.matcher(line);
        if (matcher.find()) {
            request.method = RtspMethod.valueOf(matcher.group(1));
            request.uri = matcher.group(2);
            LogHelper.e(TAG, "Line: " + line);
            LogHelper.e(TAG, "Parsing request method & uri: " + request.method + " --> " + request.uri);

            // Parsing headers of the request
            while ((line = input.readLine()) != null && line.length() > 3) {
                matcher = regexHeader.matcher(line);
                if (matcher.find()) {
                    request.headers.put(matcher.group(1).toLowerCase(Locale.US), matcher.group(2));
                    LogHelper.e(TAG, "Parsing headers of the request: " + line + " | " +
                            matcher.group(1).toLowerCase(Locale.US) + " --> " + matcher.group(2));
                }
            }
            if (line == null) throw new SocketException("Client disconnected");

            // It's not an error, it's just easier to follow what's happening in logcat with the request in red
            LogHelper.e(TAG, request.method + " " + request.uri);
        }

        return request;
    }
}
