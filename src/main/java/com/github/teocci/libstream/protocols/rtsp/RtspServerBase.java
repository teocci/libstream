package com.github.teocci.libstream.protocols.rtsp;

import android.app.Service;
import android.content.Intent;
import android.media.MediaCodec;

import com.github.teocci.libstream.enums.Protocol;
import com.github.teocci.libstream.input.audio.AudioQuality;
import com.github.teocci.libstream.interfaces.ConnectCheckerRtsp;
import com.github.teocci.libstream.interfaces.RtspCallback;
import com.github.teocci.libstream.threads.RequestWorker;
import com.github.teocci.libstream.utils.LogHelper;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Map;
import java.util.WeakHashMap;

import static com.github.teocci.libstream.enums.Protocol.TCP;

/**
 * Implementation of a subset of the RTSP protocol (RFC 2326).
 * <p>
 * It allows remote control of an android device cameras & microphone.
 * For each connected client, a Session is instantiated.
 * The Session will start or stop streams according to what the client wants.
 */
public abstract class RtspServerBase extends Service
{
    private static String TAG = LogHelper.makeLogTag(RtspServerBase.class);

    /**
     * Port used by default.
     */
    public static final int DEFAULT_RTSP_PORT = 8086;

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

//    /** Key used in the SharedPreferences to store whether the RTSP server is enabled or not. */
//    public final static String KEY_ENABLED = "rtsp_enabled";

//    /** Key used in the SharedPreferences for the port used by the RTSP server. */
//    public final static String KEY_PORT = "rtsp_port";

    protected int rtspPort = DEFAULT_RTSP_PORT;

    private ConnectCheckerRtsp connectCheckerRtsp;

    public Map<Session, Object> sessions = new WeakHashMap<>(2);
    private final LinkedList<RtspCallback> listeners = new LinkedList<>();

    private RequestWorker listenerThread;
//    private final IBinder binder;

    public Session currentSession;

    public Protocol protocol = TCP;

    public ByteBuffer sps, pps;

    private AudioQuality audioQuality = AudioQuality.DEFAULT;

    protected boolean enabled = true;
    private boolean restart = false;

    private volatile boolean loaded = false;
    public volatile boolean streaming = false;
    public volatile boolean running = false;

    public RtspServerBase() {}

//    @Override
//    public IBinder onBind(Intent intent)
//    {
//        return null;
//    }

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

    public void postMessage(int id)
    {
        synchronized (listeners) {
            if (!listeners.isEmpty()) {
                for (RtspCallback cl : listeners) {
                    cl.onMessage(this, id);
                }
            }
        }
    }

    /**
     * See {@link RtspCallback} to check out what events will be fired once you set up a listener.
     *
     * @param listener The listener
     */
    public void addCallbackListener(RtspCallback listener)
    {
        synchronized (listeners) {
            if (!listeners.isEmpty()) {
                for (RtspCallback cl : listeners) {
                    if (cl == listener) return;
                }
            }
            listeners.add(listener);
        }
    }

    /**
     * Removes the listener.
     *
     * @param listener The listener
     */
    public void removeCallbackListener(RtspCallback listener)
    {
        synchronized (listeners) {
            this.listeners.remove(listener);
        }
    }

    /**
     * Returns the port used by the RTSP server.
     */
    public int getPort()
    {
        return rtspPort;
    }

    /**
     * Sets the port for the RTSP server to use.
     *
     * @param port The port
     */
    public void setPort(int port)
    {
        rtspPort = port;
//        Editor editor = sharedPreferences.edit();
//        editor.putString(KEY_RTSP_PORT, String.valueOf(port));
//        editor.apply();
    }

    /**
     * Starts (or restart if needed, if for example the configuration
     * of the server has been modified) the RTSP server.
     */
    public void start()
    {
        if (!enabled || restart) stop();
        if (enabled && listenerThread == null) {
            try {
                listenerThread = new RequestWorker(this, rtspPort);
                LogHelper.e(TAG, "RequestWorker() called");
            } catch (Exception e) {
                listenerThread = null;
                postMessage(MESSAGE_STREAMING_STOPPED);
            }
        }
        restart = false;
        LogHelper.e(TAG, "start()");
    }

    /**
     * Stops the RTSP server but not the Android Service.
     * To stop the Android Service you need to call {@link android.content.Context#stopService(Intent)};
     */
    public void stop()
    {
        if (listenerThread != null) {
            try {
                listenerThread.kill();
                for (Session session : sessions.keySet()) {
                    if (session != null && session.isStreaming()) {
                        session.stop();
                    }
                }
            } catch (Exception e) {
            } finally {
                listenerThread = null;
                postMessage(MESSAGE_STREAMING_STOPPED);
            }
        }
        LogHelper.e(TAG, "stop()");
    }

    public void postError(Exception exception, int id)
    {
        synchronized (listeners) {
            if (!listeners.isEmpty()) {
                for (RtspCallback cl : listeners) {
                    cl.onError(this, exception, id);
                }
            }
        }
    }

    public void setConnectCheckerRtsp(ConnectCheckerRtsp connectCheckerRtsp)
    {
        this.connectCheckerRtsp = connectCheckerRtsp;
    }

    public void setAuthorization(String user, String password)
    {
        currentSession.user = user;
        currentSession.password = password;
    }

    public void setChannel(int channel)
    {
        this.audioQuality.channel = channel;
    }

    public void setSampleRate(int sampleRate)
    {
        this.audioQuality.sampling = sampleRate;
    }

//    public void setSPSandPPS(ByteBuffer sps, ByteBuffer pps)
//    {
//        setPSPair(sps, pps);
//    }

    public void sendVideo(ByteBuffer h264Buffer, MediaCodec.BufferInfo info)
    {
        if (h264Buffer == null || info == null) return;
        if (currentSession == null) return;
        if (currentSession.h264Packet == null) return;

        if (isStreaming() && isLoaded()) {
            currentSession.h264Packet.createAndSendPacket(h264Buffer, info);
        }
    }

    public void sendAudio(ByteBuffer aacBuffer, MediaCodec.BufferInfo info)
    {
        if (aacBuffer == null || info == null) return;
        if (currentSession == null) return;
        if (currentSession.aacPacket == null) return;

        if (isStreaming() && (isLoaded())) {
            currentSession.aacPacket.createAndSendPacket(aacBuffer, info);
        }
    }

    public void setPSPair(ByteBuffer sps, ByteBuffer pps)
    {
        if (sps == null && pps == null) return;
        this.sps = sps;
        this.pps = pps;

        loaded = true;
    }

    public void setProtocol(Protocol protocol)
    {
        this.protocol = protocol;
    }

    public void setCurrentSession(Session currentSession)
    {
        this.currentSession = currentSession;
    }

    public void setStreaming(boolean streaming)
    {
        this.streaming = streaming;
    }


    public ConnectCheckerRtsp getConnectCheckerRtsp()
    {
        return connectCheckerRtsp;
    }

    public Protocol getProtocol()
    {
        return protocol;
    }

    public int getSampleRate()
    {
        return audioQuality.sampling;
    }

    public int getChannel()
    {
        return audioQuality.channel;
    }

    public RtspServerBase getService()
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

    public boolean isEnabled()
    {
        return enabled;
    }

    public boolean isLoaded()
    {
        return currentSession.h264Packet == null || loaded;
    }

    /**
     * Returns whether or not the RTSP server is streaming to some client(s).
     */
    public boolean isStreaming()
    {
//        for (Session session : sessions.keySet()) {
//            if (session != null && session.isStreaming()) {
//                return true;
//            }
//        }
//
//        return false;
        return streaming;
    }

    public boolean isRunning()
    {
        return running;
    }
}