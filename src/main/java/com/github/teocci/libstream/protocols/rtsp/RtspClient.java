package com.github.teocci.libstream.protocols.rtsp;

import android.media.MediaCodec;

import com.github.teocci.libstream.enums.Protocol;
import com.github.teocci.libstream.input.audio.AudioQuality;
import com.github.teocci.libstream.interfaces.ConnectCheckerRtsp;
import com.github.teocci.libstream.protocols.rtp.packets.AacPacket;
import com.github.teocci.libstream.protocols.rtp.packets.H264Packet;
import com.github.teocci.libstream.utils.LogHelper;
import com.github.teocci.libstream.utils.rtsp.AuthUtil;
import com.github.teocci.libstream.utils.rtsp.CreateSSLSocket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.teocci.libstream.enums.Protocol.UDP;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public class RtspClient
{
    private static String TAG = LogHelper.makeLogTag(RtspClient.class);

    private static final Pattern RTSP_PATTERN =
            Pattern.compile("^rtsp:\\/\\/([^\\/:]+)(:(\\d+))*\\/?([^\\/]+)?(\\/(.*))*$");
    private static final Pattern RTSPS_PATTERN =
            Pattern.compile("^rtsps:\\/\\/([^\\/:]+)(:(\\d+))*\\/?([^\\/]+)?(\\/(.*))*$");

    private String host = "";
    private int port;
    private String path;

    private int cseq = 0;
    private String authorization = null;
    private String user;
    private String password;
    private String sessionId;

    private ConnectCheckerRtsp connectCheckerRtsp;
    private Session currentSession;

    // Sockets objects
    private Socket connectionSocket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private Thread thread;

    private AudioQuality audioQuality = AudioQuality.DEFAULT;

    private volatile boolean streaming = false;

    // For secure transport
    private boolean tlsEnabled = false;

    public RtspClient(ConnectCheckerRtsp connectCheckerRtsp)
    {
        this.connectCheckerRtsp = connectCheckerRtsp;
        this.currentSession = new Session();
    }

    public void setProtocol(Protocol protocol)
    {
        currentSession.setProtocol(protocol);
    }

    public void setAuthorization(String user, String password)
    {
        this.user = user;
        this.password = password;
    }

    public boolean isStreaming()
    {
        return streaming;
    }

    public void setUrl(String url)
    {
        Matcher rtspMatcher = RTSP_PATTERN.matcher(url);
        Matcher rtspsMatcher = RTSPS_PATTERN.matcher(url);
        Matcher matcher;
        if (rtspMatcher.matches()) {
            matcher = rtspMatcher;
            tlsEnabled = false;
        } else if (rtspsMatcher.matches()) {
            matcher = rtspsMatcher;
            tlsEnabled = true;
        } else {
            streaming = false;
            connectCheckerRtsp.onConnectionFailedRtsp("Endpoint malformed, should be: rtsp://ip:port");
            return;
        }
        host = matcher.group(1);
        port = Integer.parseInt((matcher.group(3) != null) ? matcher.group(3) : "1935");
        path = "/" + matcher.group(4) + "/" + matcher.group(6);
    }

    public void setSampleRate(int sampleRate)
    {
        audioQuality.sampling = sampleRate;
    }

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    public String getPath()
    {
        return path;
    }

    public ConnectCheckerRtsp getConnectCheckerRtsp()
    {
        return connectCheckerRtsp;
    }

    public void setSPSandPPS(ByteBuffer sps, ByteBuffer pps)
    {
        byte[] mSPS = new byte[sps.capacity() - 4];
        sps.position(4);
        sps.get(mSPS, 0, mSPS.length);
        byte[] mPPS = new byte[pps.capacity() - 4];
        pps.position(4);
        pps.get(mPPS, 0, mPPS.length);
        currentSession.sps = mSPS;
        currentSession.pps = mPPS;
    }

    public void setChannel(int channel)
    {
        audioQuality.channel = channel;
    }

    public void connect()
    {
        if (!streaming) {
            boolean isTCP = currentSession.isTCP();

            currentSession.h264Packet = new H264Packet(currentSession, isTCP);
            if (currentSession.sps != null && currentSession.pps != null) {
                currentSession.h264Packet.setPSPair(currentSession.sps, currentSession.pps);
            }

            currentSession.aacPacket = new AacPacket(currentSession, isTCP);
            currentSession.aacPacket.setSampleRate(audioQuality.sampling);
            thread = new Thread(() -> {
                try {
                    if (!tlsEnabled) {
                        connectionSocket = new Socket();
                        SocketAddress socketAddress = new InetSocketAddress(host, port);
                        connectionSocket.connect(socketAddress, 3000);
                    } else {
                        connectionSocket = CreateSSLSocket.createSSlSocket(host, port);
                    }

                    reader = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));

                    currentSession.setOutputStream(connectionSocket.getOutputStream());
                    writer = new BufferedWriter(new OutputStreamWriter(currentSession.getOutputStream()));
                    writer.write(sendAnnounce());
                    writer.flush();

                    // Check if you need credential for stream, if you need try connect with credential
                    String response = getResponse(false, false);
                    int status = getResponseStatus(response);
                    switch (status) {
                        case 403:
                            connectCheckerRtsp.onConnectionFailedRtsp("Error configure stream, access denied");
                            LogHelper.e(TAG, "Response 403, access denied");
                            return;
                        case 401:
                            if (user == null || password == null) {
                                connectCheckerRtsp.onAuthErrorRtsp();
                                return;
                            } else {
                                writer.write(sendAnnounceWithAuth(response));
                                writer.flush();
                                int statusAuth = getResponseStatus(getResponse(false, false));
                                switch (statusAuth) {
                                    case 401:
                                        connectCheckerRtsp.onAuthErrorRtsp();
                                        return;
                                    case 200:
                                        connectCheckerRtsp.onAuthSuccessRtsp();
                                        break;
                                    default:
                                        connectCheckerRtsp.onConnectionFailedRtsp("Error configure stream, announce with auth failed");
                                        break;
                                }
                            }
                            break;
                        case 200:
                            connectCheckerRtsp.onConnectionFailedRtsp("Error configure stream, announce failed");
                            break;
                    }

                    writer.write(sendSetup(currentSession.trackAudio, currentSession.protocol));
                    writer.flush();
                    getResponse(true, true);

                    writer.write(sendSetup(currentSession.trackVideo, currentSession.protocol));
                    writer.flush();
                    getResponse(false, true);

                    writer.write(sendRecord());
                    writer.flush();
                    getResponse(false, true);

                    currentSession.updateDestination();
//                    connectCheckerRtsp.onConnectionSuccessRtsp();

                    streaming = true;
                } catch (IOException | NullPointerException e) {
                    LogHelper.e(TAG, "connection error", e);
                    connectCheckerRtsp.onConnectionFailedRtsp("Error configure stream, " + e.getMessage());
                    streaming = false;
                }
            });
            thread.start();
        }
    }

    public void disconnect()
    {
        if (streaming) {
            thread = new Thread(() -> {
                try {
                    writer.write(sendTearDown());
                    connectionSocket.close();
                } catch (IOException e) {
                    LogHelper.e(TAG, "disconnect error", e);
                }
                connectCheckerRtsp.onDisconnectRtsp();
                streaming = false;
            });
            thread.start();
            if (currentSession.h264Packet != null && currentSession.aacPacket != null) {
                currentSession.h264Packet.close();
                currentSession.aacPacket.close();
            }
            cseq = 0;
            currentSession.sps = null;
            currentSession.pps = null;
        }
    }

    private String sendAnnounce()
    {
        String body = currentSession.createDescription();
        String announce = "ANNOUNCE rtsp://" + host + ":" + port + path + " RTSP/1.0\r\n" +
                "CSeq: " + (++cseq) + "\r\n" +
                "Content-Length: " + body.length() + "\r\n";
        if (authorization != null) {
            announce += "Authorization: " + authorization + "\r\n";
        }
        announce += "Content-Type: application/sdp\r\n\r\n" + body;

        LogHelper.i(TAG, announce);
        return announce;
    }

    private String sendSetup(int track, Protocol protocol)
    {
        int[] ports = currentSession.getDestinationPorts(track);
        String params = (protocol == UDP) ?
                ("UDP;unicast;client_port=" + ports[0] + "-" + ports[1] + ";mode=record") :
                ("TCP;interleaved=" + 2 * track + "-" + (2 * track + 1) + ";mode=record");

        String setup = "SETUP rtsp://" + host + ":" + port + path + "/trackID=" + track + " RTSP/1.0\r\n" +
                "Transport: RTP/AVP/" + params + "\r\n" +
                addHeaders(authorization);
        LogHelper.i(TAG, setup);

        return setup;
    }

    private String sendOptions()
    {
        String options = "OPTIONS rtsp://" + host + ":" + port + path + " RTSP/1.0\r\n" +
                addHeaders(authorization);
        LogHelper.i(TAG, options);

        return options;
    }

    private String sendRecord()
    {
        String record = "RECORD rtsp://" + host + ":" + port + path + " RTSP/1.0\r\n" +
                "Range: npt=0.000-\r\n" +
                addHeaders(authorization);
        LogHelper.i(TAG, record);

        return record;
    }

    private String sendTearDown()
    {
        String teardown = "TEARDOWN rtsp://" + host + ":" + port + path + " RTSP/1.0\r\n" +
                addHeaders(authorization);
        LogHelper.i(TAG, teardown);

        return teardown;
    }

    private String addHeaders(String authorization)
    {
        // For some reason you may have to remove last "\r\n" in the next line to make the
        // RTSP client work with your wowza server :/
        return "CSeq: " + (++cseq) + "\r\n" +
                "Content-Length: 0\r\n" +
                "Session: " + sessionId + "\r\n" +
                (authorization != null ? "Authorization: " + authorization + "\r\n" : "") + "\r\n";
    }

    private String getResponse(boolean isAudio, boolean checkStatus)
    {
        try {
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                if (line.contains("Session")) {
                    Pattern rtspPattern = Pattern.compile("Session: (\\w+)");
                    Matcher matcher = rtspPattern.matcher(line);
                    if (matcher.find()) {
                        sessionId = matcher.group(1);
                    }
                    sessionId = line.split(";")[0].split(":")[1].trim();
                }
                if (line.contains("server_port")) {
                    Pattern rtspPattern = Pattern.compile("server_port=([0-9]+)-([0-9]+)");
                    Matcher matcher = rtspPattern.matcher(line);
                    if (matcher.find()) {
                        if (isAudio) {
                            currentSession.setAudioPorts(new int[]{
                                    Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))
                            });
                        } else {
                            currentSession.setVideoPorts(new int[]{
                                    Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))
                            });
                        }
                    }
                }
                sb.append(line);
                sb.append("\n");
                // End of response
                if (line.length() < 3) break;
            }

            String response = sb.toString();
            if (checkStatus && getResponseStatus(response) != 200) {
                connectCheckerRtsp.onConnectionFailedRtsp("Error configure stream, " + response);
            }
            LogHelper.i(TAG, response);

            return response;
        } catch (IOException e) {
            LogHelper.e(TAG, "read error", e);
            return null;
        }
    }

    private String sendAnnounceWithAuth(String authResponse)
    {
        authorization = createAuth(authResponse);
        LogHelper.i(TAG, authorization);
        String body = currentSession.createDescription();
        String announce = "ANNOUNCE rtsp://" + host + ":" + port + path + " RTSP/1.0" + "\r\n" +
                "CSeq: " + (++cseq) + "\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "Authorization: " + authorization + "\r\n" +
                "Content-Type: application/sdp\r\n\r\n" +
                body;
        LogHelper.i(TAG, announce);

        return announce;
    }

    private String createAuth(String authResponse)
    {
        Pattern authPattern = Pattern.compile("realm=\"(.+)\",\\s+nonce=\"(\\w+)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher = authPattern.matcher(authResponse);
        if (matcher.find()) {
            String realm = matcher.group(1);
            String nonce = matcher.group(2);
            String hash1 = AuthUtil.getMd5Hash(user + ":" + realm + ":" + password);
            String hash2 = AuthUtil.getMd5Hash("ANNOUNCE:rtsp://" + host + ":" + port + path);
            String hash3 = AuthUtil.getMd5Hash(hash1 + ":" + nonce + ":" + hash2);

            return "Digest username=\"" + user + "\"," +
                    "realm=\"" + realm + "\"," +
                    "nonce=\"" + nonce + "\"," +
                    "uri=\"rtsp://" + host + ":" + port + path + "\"," +
                    "response=\"" + hash3 + "\"";
        }

        return "";
    }

    private int getResponseStatus(String response)
    {
        String rtspResponse = "RTSP/\\d.\\d (\\d+) (\\w+)";
        Matcher matcher = Pattern.compile(rtspResponse, Pattern.CASE_INSENSITIVE).matcher(response);

        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }

    public int[] getAudioPorts()
    {
        return currentSession.getAudioPorts();
    }

    public int[] getVideoPorts()
    {
        return currentSession.getVideoPorts();
    }

    public void sendVideo(ByteBuffer h264Buffer, MediaCodec.BufferInfo info)
    {
        if (isStreaming()) {
            currentSession.h264Packet.createAndSendPacket(h264Buffer, info);
        }
    }

    public void sendAudio(ByteBuffer aacBuffer, MediaCodec.BufferInfo info)
    {
        if (isStreaming()) {
            currentSession.aacPacket.createAndSendPacket(aacBuffer, info);
        }
    }
}