package com.github.teocci.libstream.threads;

import com.github.teocci.libstream.utils.LogHelper;

import java.io.IOException;
import java.io.OutputStream;

import static com.github.teocci.libstream.utils.Config.SERVER_NAME;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Dec-14
 */
public class Response
{
    private static String TAG = LogHelper.makeLogTag(Response.class);

    // Status code definitions
    public static final String STATUS_OK = "200 OK";
    public static final String STATUS_BAD_REQUEST = "400 Bad Request";
    public static final String STATUS_UNAUTHORIZED = "401 Unauthorized";
    public static final String STATUS_NOT_FOUND = "404 Not Found";
    public static final String STATUS_INTERNAL_SERVER_ERROR = "500 Internal Server Error";

    public String status = STATUS_INTERNAL_SERVER_ERROR;
    public String content = "";
    public String attributes = "";

    private final Request request;

    public Response(Request request)
    {
        this.request = request;
    }

    public Response()
    {
        // Be careful if you modify the send() method because request might be null!
        request = null;
    }

    public void send(OutputStream output) throws IOException
    {
        int seqId = -1;

        try {
            seqId = Integer.parseInt(request.headers.get("cseq").replace(" ", ""));
        } catch (Exception e) {
            LogHelper.e(TAG, "Error parsing CSeq: " + (e.getMessage() != null ? e.getMessage() : ""));
        }

        String response = "RTSP/1.0 " + status + "\r\n" +
                "Server: " + SERVER_NAME + "\r\n" +
                (seqId >= 0 ? ("Cseq: " + seqId + "\r\n") : "") +
                "Content-Length: " + content.length() + "\r\n" +
                attributes +
                "\r\n" +
                content;

        LogHelper.e(TAG, response.replace("\r", ""));

        output.write(response.getBytes());
    }
}
