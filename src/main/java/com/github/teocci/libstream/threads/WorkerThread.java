package com.github.teocci.libstream.threads;

import android.util.Base64;

import com.github.teocci.libstream.protocols.rtp.packets.AacPacket;
import com.github.teocci.libstream.protocols.rtp.packets.H264Packet;
import com.github.teocci.libstream.protocols.rtsp.RtspServerBase;
import com.github.teocci.libstream.protocols.rtsp.Session;
import com.github.teocci.libstream.utils.LogHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.teocci.libstream.enums.Protocol.UDP;
import static com.github.teocci.libstream.enums.RtspMethod.OPTIONS;
import static com.github.teocci.libstream.threads.Response.STATUS_BAD_REQUEST;
import static com.github.teocci.libstream.threads.Response.STATUS_INTERNAL_SERVER_ERROR;
import static com.github.teocci.libstream.utils.Config.SERVER_NAME;

/**
 * One thread per client
 * <p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Dec-14
 */

public class WorkerThread extends Thread implements Runnable
{
    private static String TAG = LogHelper.makeLogTag(WorkerThread.class);

    /**
     * Port already in use.
     */
    public final static int ERROR_BIND_FAILED = 0x00;

    /**
     * A stream could not be started.
     */
    public final static int ERROR_START_FAILED = 0x01;

    /**
     * Streaming started.
     */
    public final static int MESSAGE_STREAMING_STARTED = 0X00;

    /**
     * Streaming stopped.
     */
    public final static int MESSAGE_STREAMING_STOPPED = 0X01;

    private final RtspServerBase rtspServer;
    private final Socket client;

    private final OutputStream outputStream;
    private final BufferedReader inputStream;

    // Each client has an associated session
    private Session session;

    /**
     * Credentials for Basic Auth
     */
    private String username;
    private String password;

    public WorkerThread(RtspServerBase rtspServer, final Socket client) throws IOException
    {
        this.rtspServer = rtspServer;
        this.client = client;

        this.outputStream = client.getOutputStream();
        this.inputStream = new BufferedReader(new InputStreamReader(client.getInputStream()));

        this.session = new Session();
        this.session.setProtocol(rtspServer.getProtocol());
        this.session.setConnectCheckerRtsp(rtspServer.getConnectCheckerRtsp());
        this.session.setOutputStream(client.getOutputStream());
        this.session.setSampleRate(rtspServer.getSampleRate());
        this.session.setChannel(rtspServer.getChannel());

        if (session.sps != null && session.pps != null) {
            this.session.setPSPair(rtspServer.sps, rtspServer.pps);
            this.session.setVideoPorts(5000 + (int) (Math.random() * 1000));
        }

        this.session.setAudioPorts(6000 + (int) (Math.random() * 1000));

        this.rtspServer.setCurrentSession(session);
    }

    public void run()
    {
        Request request;
        Response response;

        boolean isTCP = session.isTCP();

        if (session.sps != null && session.pps != null) {
            session.h264Packet = new H264Packet(session, isTCP);
            session.h264Packet.setPSPair(session.sps, session.pps);
        }

        session.aacPacket = new AacPacket(session, isTCP);
        session.aacPacket.setSampleRate(session.getSampleRate());

        LogHelper.i(TAG, "Connection from " + client.getInetAddress().getHostAddress());

        while (!Thread.interrupted()) {
            request = null;
            response = null;

            // Parse the request
            try {
                request = Request.parseRequest(inputStream);
            } catch (SocketException e) {
                // Client has left
                break;
            } catch (Exception e) {
                // We don't understand the request :/
                response = new Response();
                response.status = STATUS_BAD_REQUEST;
            }

            // Do something accordingly like starting the streams, sending a session description
            if (request != null) {
                try {
                    response = processRequest(request);
                } catch (Exception e) {
                    // This alerts the main thread that something has gone wrong in this thread
//                    rtspServer.postError(e, ERROR_START_FAILED);
                    LogHelper.e(TAG, e.getMessage() != null ? e.getMessage() : "An error occurred");
                    e.printStackTrace();
                    response = new Response(request);
                }
            }

            // We always send a response
            // The client will receive an "INTERNAL SERVER ERROR" if an exception has been thrown at some point
            try {
                if (response != null) {
                    response.send(outputStream);
                }
            } catch (IOException e) {
                LogHelper.e(TAG, "Response was not sent properly");
                break;
            }
        }

        // Streaming stops when client disconnects
        boolean streaming = rtspServer.isStreaming();
        session.syncStop();
        if (streaming && !rtspServer.isStreaming()) {
            rtspServer.postMessage(MESSAGE_STREAMING_STOPPED);
        }
        session.release();

        try {
            client.close();
        } catch (IOException ignore) {}

        rtspServer.streaming = false;
        LogHelper.e(TAG, "Client disconnected");
    }

    public Response processRequest(Request request) throws IllegalStateException, IOException
    {
        Response response = new Response(request);
        String requestAttributes;

//        if (rtspServer.sps == null && rtspServer.pps == null)  {
//            LogHelper.e(TAG, "SPS and PPS not setup.");
//            response.status = STATUS_INTERNAL_SERVER_ERROR;
//        }

        // Ask for authorization unless this is an OPTIONS request
        if (!isAuthorized(request) && request.method != OPTIONS) {
            response.attributes = "WWW-Authenticate: Basic realm=\"" + SERVER_NAME + "\"\r\n";
            response.status = Response.STATUS_UNAUTHORIZED;
        } else {
            switch (request.method) {
                case DESCRIBE:
                    // Configure the session
                    session.setOrigin(client.getLocalAddress().getHostAddress());
                    if (session.getDestination() == null) {
                        session.setDestination(client.getInetAddress().getHostAddress());
                    }

//                    session = rtspServer.handleRequest(request.uri, client);
                    rtspServer.sessions.put(session, null);
//                    session.syncConfigure();
                    requestAttributes = "Content-Base: " + getHost() + "/\r\n" +
                            "Content-Type: application/sdp\r\n";
//                    String requestContent = session.getSessionDescription();
                    String requestContent = session.createDescription();

                    response.attributes = requestAttributes;
                    response.content = requestContent;

                    // If no exception has been thrown, we reply with OK
                    response.status = Response.STATUS_OK;

                    break;
                case OPTIONS:
                    response.status = Response.STATUS_OK;
                    response.attributes = "Public: DESCRIBE,SETUP,TEARDOWN,PLAY,PAUSE\r\n";
                    response.status = Response.STATUS_OK;

                    break;
                case SETUP:
                    Pattern p;
                    Matcher m;

                    p = Pattern.compile("trackID=(\\w+)", Pattern.CASE_INSENSITIVE);
                    m = p.matcher(request.uri);

                    if (!m.find()) {
                        response.status = STATUS_BAD_REQUEST;
                        return response;
                    }

                    int p2, p1, ssrc, trackId;
                    int[] ports, src;

                    String params;
                    String destination;

                    trackId = Integer.parseInt(m.group(1));

                    if (!session.trackExists(trackId)) {
                        response.status = Response.STATUS_NOT_FOUND;
                        return response;
                    }

                    if (session.protocol == UDP) {
                        p = Pattern.compile("client_port=(\\d+)-(\\d+)", Pattern.CASE_INSENSITIVE);
                        m = p.matcher(request.headers.get("transport"));

                        if (m.find()) {
                            p1 = Integer.parseInt(m.group(1));
                            p2 = Integer.parseInt(m.group(2));
                            session.setDestinationPorts(trackId, p1, p2);
                        }

                        ports = session.getDestinationPorts(trackId);

                        src = session.getLocalPorts(trackId);
                        destination = session.getDestination();

                        params = "Transport: RTP/AVP/UDP;" + (isMulticast(destination) ? "multicast" : "unicast") + ";" +
                                "destination=" + session.getDestination() + ";" +
                                "client_port=" + ports[0] + "-" + ports[1] + ";" +
                                "server_port=" + src[0] + "-" + src[1] + ";";
                    } else {
                        params = "Transport: RTP/AVP/TCP;client_ip=" + session.getDestination() + ";" +
                                "interleaved=" + 2 * trackId + "-" + (2 * trackId + 1);
                    }

                    ssrc = session.getSSRC(trackId);

                    session.updateDestination();

                    rtspServer.streaming = true;

//                    boolean streaming = rtspServer.isStreaming();
//                    session.syncStart(trackId);
//                    if (!streaming && rtspServer.isStreaming()) {
//                      rtspServer.postMessage(MESSAGE_STREAMING_STARTED);
//                    }

                    response.attributes = params + "ssrc=" + Integer.toHexString(ssrc) + ";" +
                            "mode=play\r\n" +
                            "Session: " + "1185d20035702ca" + ";timeout=10000" + "\r\n" +
                            "Cache-Control: no-cache\r\n";

                    // If no exception has been thrown, we reply with OK
                    response.status = Response.STATUS_OK;

                    LogHelper.d(TAG, response.attributes.replace("\r", ""));

                    break;
                case PLAY:
                    requestAttributes = "RTP-Info: ";
                    if (session.trackExists(0)) {
                        requestAttributes += "url=rtsp://" + getHost() + "/trackID=" + 0 + ";seq=0,";
                    }
                    if (session.trackExists(1)) {
                        requestAttributes += "url=rtsp://" + getHost() + "/trackID=" + 1 + ";seq=0,";
                    }
                    requestAttributes = requestAttributes.substring(0, requestAttributes.length() - 1) + "\r\n" +
                            "Session: 1185d20035702ca\r\n";

                    response.attributes = requestAttributes;

                    // If no exception has been thrown, we reply with OK
                    response.status = Response.STATUS_OK;

                    break;
                case PAUSE:
                    response.status = Response.STATUS_OK;

                    break;
                case TEARDOWN:
                    response.status = Response.STATUS_OK;

                    rtspServer.streaming = false;

                    break;
                default:
                    LogHelper.e(TAG, "Command unknown: " + request);
                    response.status = STATUS_BAD_REQUEST;

                    rtspServer.streaming = false;

                    break;
            }
        }

        return response;
    }

    private String getHost()
    {
        if (client == null) return "";
        return client.getLocalAddress().getHostAddress() + ":" + client.getLocalPort();
    }

    /**
     * Set Basic authorization to access RTSP Stream
     *
     * @param username username
     * @param password password
     */
    public void setAuthorization(String username, String password)
    {
        this.username = username;
        this.password = password;
    }

    /**
     * Check if the request is authorized
     *
     * @param request RTSP Request
     * @return true or false
     */
    private boolean isAuthorized(Request request)
    {
        String auth = request.headers.get("authorization");
        if (username == null || password == null || username.isEmpty())
            return true;

        if (auth != null && !auth.isEmpty()) {
            String received = auth.substring(auth.lastIndexOf(" ") + 1);
            String local = username + ":" + password;
            String localEncoded = Base64.encodeToString(local.getBytes(), Base64.NO_WRAP);
            if (localEncoded.equals(received))
                return true;
        }

        return false;
    }

    private boolean isMulticast(String destination)
    {
        try {
            return InetAddress.getByName(destination).isMulticastAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        }
    }
}
