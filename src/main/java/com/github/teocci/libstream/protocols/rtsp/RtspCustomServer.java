package com.github.teocci.libstream.protocols.rtsp;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.github.teocci.libstream.utils.LogHelper;

/**
 * Implementation of a subset of the RTSP protocol (RFC 2326).
 * <p>
 * It allows remote control of an android device cameras & microphone.
 * For each connected client, a Session is instantiated.
 * The Session will start or stop streams according to what the client wants.
 */

public class RtspCustomServer extends RtspServerBase
{
    private static String TAG = LogHelper.makeLogTag(RtspCustomServer.class);

//    /**
//     * Port used by default.
//     */
//    public static final int DEFAULT_RTSP_PORT = 8086;
//
//    /**
//     * Port already in use.
//     */
//    public final static int ERROR_BIND_FAILED = 0x00;
//
//    /**
//     * A stream could not be started.
//     */
//    public final static int ERROR_START_FAILED = 0x01;
//
//    /**
//     * Streaming started.
//     */
//    public final static int MESSAGE_STREAMING_STARTED = 0X00;
//
//    /**
//     * Streaming stopped.
//     */
//    public final static int MESSAGE_STREAMING_STOPPED = 0X01;

//    /** Key used in the SharedPreferences to store whether the RTSP server is enabled or not. */
//    public final static String KEY_ENABLED = "rtsp_enabled";

//    /** Key used in the SharedPreferences for the port used by the RTSP server. */
//    public final static String KEY_PORT = "rtsp_port";

//    protected int rtspPort = DEFAULT_RTSP_PORT;
//
//    private ConnectCheckerRtsp connectCheckerRtsp;

//    public Map<Session, Object> sessions = new WeakHashMap<>(2);
//    private final LinkedList<RtspCallback> listeners = new LinkedList<>();
//
//    private RequestWorker listenerThread;
    
    private final IBinder binder = new LocalBinder();

//    private Session currentSession;
//
//    protected boolean enabled = true;
//    private boolean restart = false;
//
//    public volatile boolean streaming = false;
//
//    private Protocol protocol = TCP;
//
//    public ByteBuffer sps, pps;
//
//    private AudioQuality audioQuality = AudioQuality.DEFAULT;

    public RtspCustomServer() {}

    @Override
    public IBinder onBind(Intent intent)
    {
        return binder;
    }

    @Override
    public void onCreate()
    {
        // Let's restore the state of the service
//        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//        rtspPort = Integer.parseInt(sharedPreferences.getString(KEY_RTSP_PORT, String.valueOf(rtspPort)));
//        enabled = sharedPreferences.getBoolean(KEY_RTSP_ENABLED, enabled);

        start();
    }

    @Override
    public void onDestroy()
    {
        stop();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return START_STICKY;
    }

//    public void postMessage(int id)
//    {
//        synchronized (listeners) {
//            if (!listeners.isEmpty()) {
//                for (RtspCallback cl : listeners) {
//                    cl.onMessage(this, id);
//                }
//            }
//        }
//    }
//
//    /**
//     * See {@link RtspCallback} to check out what events will be fired once you set up a listener.
//     *
//     * @param listener The listener
//     */
//    public void addCallbackListener(RtspCallback listener)
//    {
//        synchronized (listeners) {
//            if (!listeners.isEmpty()) {
//                for (RtspCallback cl : listeners) {
//                    if (cl == listener) return;
//                }
//            }
//            listeners.add(listener);
//        }
//    }
//
//    /**
//     * Removes the listener.
//     *
//     * @param listener The listener
//     */
//    public void removeCallbackListener(RtspCallback listener)
//    {
//        synchronized (listeners) {
//            this.listeners.remove(listener);
//        }
//    }
//
//    /**
//     * Returns the port used by the RTSP server.
//     */
//    public int getPort()
//    {
//        return rtspPort;
//    }
//
//    /**
//     * Sets the port for the RTSP server to use.
//     *
//     * @param port The port
//     */
//    public void setPort(int port)
//    {
//        rtspPort = port;
////        Editor editor = sharedPreferences.edit();
////        editor.putString(KEY_RTSP_PORT, String.valueOf(port));
////        editor.apply();
//    }
//
//    /**
//     * Starts (or restart if needed, if for example the configuration
//     * of the server has been modified) the RTSP server.
//     */
//    public void start()
//    {
//        if (!enabled || restart) stop();
//        if (enabled && listenerThread == null) {
//            try {
//                listenerThread = new RequestWorker(this, rtspPort);
//            } catch (Exception e) {
//                listenerThread = null;
//            }
//        }
//        restart = false;
//    }
//
//    /**
//     * Stops the RTSP server but not the Android Service.
//     * To stop the Android Service you need to call {@link android.content.Context#stopService(Intent)};
//     */
//    public void stop()
//    {
//        if (listenerThread != null) {
//            try {
//                listenerThread.kill();
//                for (Session session : sessions.keySet()) {
//                    if (session != null && session.isStreaming()) {
//                        session.stop();
//                    }
//                }
//            } catch (Exception e) {
//            } finally {
//                listenerThread = null;
//                postMessage(MESSAGE_STREAMING_STOPPED);
//            }
//        }
//    }
//
//    /**
//     * Returns whether or not the RTSP server is streaming to some client(s).
//     */
//    public boolean isStreaming()
//    {
////        for (Session session : sessions.keySet()) {
////            if (session != null && session.isStreaming()) {
////                return true;
////            }
////        }
////
////        return false;
//        return streaming;
//    }

//    public void handleZoom(int newZoom)
//    {
//        for (Session session : sessions.keySet()) {
//            if (session != null && session.isStreaming()) {
////                session.getVideoTrack().handleZoom(newZoom);
//            }
//        }
//    }
//
//    public void setZoom(int newZoom)
//    {
//        for (Session session : sessions.keySet()) {
//            if (session != null && session.isStreaming()) {
////                session.getVideoTrack().setZoom(newZoom);
//            }
//        }
//    }
//
//    /**
//     * Returns the bandwidth consumed by the RTSP server in bits per second.
//     */
//    public long getBitrate()
//    {
//        long bitrate = 0;
//        for (Session session : sessions.keySet()) {
//            if (session != null && session.isStreaming()) {
//                bitrate += session.getBitrate();
//            }
//        }
//        return bitrate;
//    }
//
//    protected void updateVideoQuality(String key)
//    {
//        Log.e(TAG, "onSharedPreferenceChanged | key: " + key);
//        for (Session session : sessions.keySet()) {
//            if (session != null && !session.isStreaming()) {
////                int videoWidth = sharedPreferences.getInt(KEY_VIDEO_WIDTH, 0);
////                int videoHeight = sharedPreferences.getInt(KEY_VIDEO_HEIGHT, 0);
////                VideoQuality videoQuality = new VideoQuality(videoWidth, videoHeight);
////                videoQuality.fps = Integer.parseInt(
////                        sharedPreferences.getString(KEY_VIDEO_FRAMERATE, "0")
////                );
////                videoQuality.bitrate = Integer.parseInt(
////                        sharedPreferences.getString(KEY_VIDEO_BITRATE, "0")
////                ) * 1000;
////                session.setVideoQuality(videoQuality);
//            }
//        }
//    }
//
//    public void postError(Exception exception, int id)
//    {
//        synchronized (listeners) {
//            if (!listeners.isEmpty()) {
//                for (RtspCallback cl : listeners) {
//                    cl.onError(this, exception, id);
//                }
//            }
//        }
//    }
//
//    public void setConnectCheckerRtsp(ConnectCheckerRtsp connectCheckerRtsp)
//    {
//        this.connectCheckerRtsp = connectCheckerRtsp;
//    }
//
//    public void setAuthorization(String user, String password)
//    {
//        currentSession.user = user;
//        currentSession.password = password;
//    }
//
//    public void setChannel(int channel)
//    {
//        this.audioQuality.channel = channel;
//    }
//
//    public void setSampleRate(int sampleRate)
//    {
//        this.audioQuality.sampleRate = sampleRate;
//    }
//
//    public void setSPSandPPS(ByteBuffer sps, ByteBuffer pps)
//    {
//        setPSPair(sps, pps);
//    }
//
//    public void sendVideo(ByteBuffer h264Buffer, MediaCodec.BufferInfo info)
//    {
//        if (isStreaming()) {
//            currentSession.h264Packet.createAndSendPacket(h264Buffer, info);
//        }
//    }
//
//    public void sendAudio(ByteBuffer aacBuffer, MediaCodec.BufferInfo info)
//    {
//        if (isStreaming()) {
//            currentSession.aacPacket.createAndSendPacket(aacBuffer, info);
//        }
//    }
//
//    private void setPSPair(ByteBuffer sps, ByteBuffer pps)
//    {
//        this.sps = sps;
//        this.pps = pps;
//    }
//
//    public void setProtocol(Protocol protocol)
//    {
//        this.protocol = protocol;
//    }
//
//    public void setCurrentSession(Session currentSession)
//    {
//        this.currentSession = currentSession;
//    }
//
//    public ConnectCheckerRtsp getConnectCheckerRtsp()
//    {
//        return connectCheckerRtsp;
//    }
//
//    public Protocol getProtocol()
//    {
//        return protocol;
//    }
//
//    public int getSampleRate()
//    {
//        return audioQuality.sampleRate;
//    }
//
//    public int getChannel()
//    {
//        return audioQuality.channel;
//    }

    public RtspCustomServer getService()
    {
        return this;
    }

//    /**
//     * By default the RTSP uses {@link UriParser} to parse the URI requested by the client
//     * but you can change that behavior by override this method.
//     *
//     * @param uri    The uri that the client has requested
//     * @param client The socket associated to the client
//     * @return A proper session
//     */
//    public Session handleRequest(String uri, Socket client) throws IllegalStateException, IOException
//    {
//        Session session = UriParser.parse(uri);
//        session.setOrigin(client.getLocalAddress().getHostAddress());
//        if (session.getDestination() == null) {
//            session.setDestination(client.getInetAddress().getHostAddress());
//        }
//        return session;
//    }

//    public boolean isEnabled()
//    {
//        return enabled;
//    }

    /**
     * The Binder you obtain when a connection with the Service is established.
     */
    public class LocalBinder extends Binder
    {
        public RtspCustomServer getService()
        {
            return RtspCustomServer.this.getService();
        }
    }
}