package com.github.teocci.libstream.input.media;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import com.github.teocci.libstream.interfaces.Stream;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.Random;

/**
 * A MediaRecorder that streams what it records using a packetizer from the RTP package.
 * You can't use this class directly !
 */
public abstract class MediaStream implements Stream
{
    protected static final String TAG = "MediaStream";

    /**
     * Raw audio/video will be encoded using the MediaRecorder API.
     */
    public static final byte MODE_MEDIARECORDER_API = 0x01;

    /**
     * Raw audio/video will be encoded using the MediaCodec API with buffers.
     */
    public static final byte MODE_MEDIACODEC_API = 0x02;

    /**
     * Raw audio/video will be encoded using the MediaCode API with a surface.
     */
    public static final byte MODE_MEDIACODEC_API_2 = 0x05;

    /**
     * A LocalSocket will be used to feed the MediaRecorder object
     */
    public static final byte PIPE_API_LS = 0x01;

    /**
     * A ParcelFileDescriptor will be used to feed the MediaRecorder object
     */
    public static final byte PIPE_API_PFD = 0x02;

    /**
     * Prefix that will be used for all shared preferences saved by libstreaming
     */
    protected static final String PREF_PREFIX = "libstreaming-";

    /**
     * The packetizer that will read the output of the camera and send RTP packets over the networked.
     */
//    protected AbstractPacketizer packetizer = null;
//
//    protected static byte suggestedMode = MODE_MEDIARECORDER_API;
//    protected byte currentMode, requestedMode;
//
//    /**
//     * Starting lollipop the LocalSocket API cannot be used to feed a MediaRecorder object.
//     * You can force what API to use to create the pipe that feeds it with this constant
//     * by using  {@link #PIPE_API_LS} and {@link #PIPE_API_PFD}.
//     */
//    protected final static byte pipeAPI;
//
//    protected boolean streaming = false, configured = false;
//    protected int rtpPort = 0, rtcpPort = 0;
//    protected byte channelIdentifier = 0;
//    protected OutputStream outputStream = null;
//    protected InetAddress destination;
//
//    protected ParcelFileDescriptor[] parcelFileDescriptors;
//    protected ParcelFileDescriptor parcelRead;
//    protected ParcelFileDescriptor parcelWrite;
//
//    protected LocalSocket receiver, sender = null;
//    private LocalServerSocket localSocket = null;
//    private int socketId;
//
//    private int timeToLive = 64;
//
//    protected MediaRecorder mediaRecorder;
//    protected MediaCodec mediaCodec;
//
//    static {
//        // We determine whether or not the MediaCodec API should be used
//        try {
//            Class.forName("android.media.MediaCodec");
//            // Will be set to MODE_MEDIACODEC_API at some point...
//            suggestedMode = MODE_MEDIACODEC_API;
//            Log.i(TAG, "Phone supports the MediaCoded API");
//        } catch (ClassNotFoundException e) {
//            suggestedMode = MODE_MEDIARECORDER_API;
//            Log.i(TAG, "Phone does not support the MediaCodec API");
//        }
//
//        // Starting lollipop, the LocalSocket API cannot be used anymore to feed
//        // a MediaRecorder object for security reasons
//        pipeAPI = Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH ?
//                PIPE_API_PFD : PIPE_API_LS;
//    }
//
//    public MediaStream()
//    {
//        this.requestedMode = suggestedMode;
//        this.currentMode = suggestedMode;
//    }
//
//    /**
//     * Sets the destination IP address of the stream.
//     *
//     * @param dest The destination address of the stream
//     */
//    public void setDestinationAddress(InetAddress dest)
//    {
//        destination = dest;
//    }
//
//    /**
//     * Sets the destination ports of the stream.
//     * If an odd number is supplied for the destination port then the next
//     * lower even number will be used for RTP and it will be used for RTCP.
//     * If an even number is supplied, it will be used for RTP and the next odd
//     * number will be used for RTCP.
//     *
//     * @param dport The destination port
//     */
//    public void setDestinationPorts(int dport)
//    {
//        if (dport % 2 == 1) {
//            rtpPort = dport - 1;
//            rtcpPort = dport;
//        } else {
//            rtpPort = dport;
//            rtcpPort = dport + 1;
//        }
//    }
//
//    /**
//     * Sets the destination ports of the stream.
//     *
//     * @param rtpPort  Destination port that will be used for RTP
//     * @param rtcpPort Destination port that will be used for RTCP
//     */
//    public void setDestinationPorts(int rtpPort, int rtcpPort)
//    {
//        this.rtpPort = rtpPort;
//        this.rtcpPort = rtcpPort;
//        this.outputStream = null;
//    }
//
//    /**
//     * If a TCP is used as the transport protocol for the RTP session,
//     * the output stream to which RTP packets will be written to must
//     * be specified with this method.
//     */
//    public void setOutputStream(OutputStream stream, byte channelID)
//    {
//        outputStream = stream;
//        channelIdentifier = channelID;
//    }
//
//
//    /**
//     * Sets the Time To Live of packets sent over the network.
//     *
//     * @param ttl The time to live
//     * @throws IOException
//     */
//    public void setTimeToLive(int ttl) throws IOException
//    {
//        timeToLive = ttl;
//    }
//
//    /**
//     * Returns a pair of destination ports, the first one is the
//     * one used for RTP and the second one is used for RTCP.
//     **/
//    public int[] getDestinationPorts()
//    {
//        return new int[]{
//                rtpPort,
//                rtcpPort
//        };
//    }
//
//    /**
//     * Returns a pair of source ports, the first one is the
//     * one used for RTP and the second one is used for RTCP.
//     **/
//    public int[] getLocalPorts()
//    {
//        return packetizer.getRtpSocket().getLocalPorts();
//    }
//
//    /**
//     * Sets the streaming method that will be used.
//     * <p>
//     * If the mode is set to {@link #MODE_MEDIARECORDER_API}, raw audio/video will be encoded
//     * using the MediaRecorder API. <br />
//     * <p>
//     * If the mode is set to {@link #MODE_MEDIACODEC_API} or to {@link #MODE_MEDIACODEC_API_2},
//     * audio/video will be encoded with using the MediaCodec. <br />
//     * <p>
//     * The {@link #MODE_MEDIACODEC_API_2} mode only concerns {@link VideoStream}, it makes
//     * use of the createInputSurface() method of the MediaCodec API (Android 4.3 is needed there). <br />
//     *
//     * @param mode Can be {@link #MODE_MEDIARECORDER_API}, {@link #MODE_MEDIACODEC_API} or {@link #MODE_MEDIACODEC_API_2}
//     */
//    public void setStreamingMethod(byte mode)
//    {
//        requestedMode = mode;
//    }
//
//    /**
//     * Returns the streaming method in use, call this after
//     * {@link #configure()} to get an accurate response.
//     */
//    public byte getStreamingMethod()
//    {
//        return currentMode;
//    }
//
//    /**
//     * Returns the packetizer associated with the {@link MediaStream}.
//     *
//     * @return The packetizer
//     */
//    public AbstractPacketizer getPacketizer()
//    {
//        return packetizer;
//    }
//
//    /**
//     * Returns an approximation of the bit rate consumed by the stream in bit per seconde.
//     */
//    public long getBitrate()
//    {
//        return !streaming ? 0 : packetizer.getRtpSocket().getBitrate();
//    }
//
//    /**
//     * Indicates if the {@link MediaStream} is streaming.
//     *
//     * @return A boolean indicating if the {@link MediaStream} is streaming
//     */
//    public boolean isStreaming()
//    {
//        return streaming;
//    }
//
//    /**
//     * Configures the stream with the settings supplied with
//     * {@link VideoStream#setVideoQuality(net.kseek.streaming.video.VideoQuality)}
//     * for a {@link VideoStream} and {@link AudioStream#setAudioQuality(net.kseek.streaming.audio.AudioQuality)}
//     * for a {@link AudioStream}.
//     */
//    public synchronized void configure() throws IllegalStateException, IOException
//    {
//        if (streaming) throw new IllegalStateException("Can't be called while streaming.");
//        if (packetizer != null) {
//            packetizer.setDestination(destination, rtpPort, rtcpPort);
//            packetizer.getRtpSocket().setOutputStream(outputStream, channelIdentifier);
//        }
//        currentMode = requestedMode;
//        configured = true;
//    }
//
//    /**
//     * Starts the stream.
//     */
//    public synchronized void start() throws IllegalStateException, IOException
//    {
//
//        if (destination == null)
//            throw new IllegalStateException("No destination ip address set for the stream !");
//
//        if (rtpPort <= 0 || rtcpPort <= 0)
//            throw new IllegalStateException("No destination ports set for the stream !");
//
//        packetizer.setTimeToLive(timeToLive);
//
//        if (Build.VERSION.SDK_INT >= 23 || currentMode != MODE_MEDIARECORDER_API) {
//            currentMode = MODE_MEDIACODEC_API;
//            Log.e(TAG, "Forcing encodeWithMediaCodec");
//            encodeWithMediaCodec();
//        } else {
//            encodeWithMediaRecorder();
//        }
//    }
//
//    /**
//     * Stops the stream.
//     */
//    @SuppressLint("NewApi")
//    public synchronized void stop()
//    {
//        if (streaming) {
//            try {
//                if (currentMode == MODE_MEDIARECORDER_API) {
//                    mediaRecorder.stop();
//                    mediaRecorder.release();
//                    mediaRecorder = null;
//                    closeSockets();
//                    packetizer.stop();
//                } else {
//                    packetizer.stop();
//                    mediaCodec.stop();
//                    mediaCodec.release();
//                    mediaCodec = null;
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            streaming = false;
//        }
//    }
//
//    protected abstract void encodeWithMediaRecorder() throws IOException;
//
//    protected abstract void encodeWithMediaCodec() throws IOException;
//
//    /**
//     * Returns a description of the stream using SDP.
//     * This method can only be called after {@link Stream#configure()}.
//     *
//     * @throws IllegalStateException Thrown when {@link Stream#configure()} was not called.
//     */
//    public abstract String getSessionDescription();
//
//    /**
//     * Returns the SSRC of the underlying {@link net.kseek.streaming.rtp.RtpSocket}.
//     *
//     * @return the SSRC of the stream
//     */
//    public int getSSRC()
//    {
//        return getPacketizer().getSSRC();
//    }
//
//    protected void createSockets() throws IOException
//    {
//        if (pipeAPI == PIPE_API_LS) {
//            final String LOCAL_ADDR = "net.kseek.streaming-";
//
//            for (int i = 0; i < 10; i++) {
//                try {
//                    socketId = new Random().nextInt();
//                    localSocket = new LocalServerSocket(LOCAL_ADDR + socketId);
//                    break;
//                } catch (IOException e1) {}
//            }
//
//            receiver = new LocalSocket();
//            receiver.connect(new LocalSocketAddress(LOCAL_ADDR + socketId));
//            receiver.setReceiveBufferSize(500000);
//            receiver.setSoTimeout(3000);
//            sender = localSocket.accept();
//            sender.setSendBufferSize(500000);
//        } else {
//            Log.e(TAG, "parcelFileDescriptors createPipe version = Lollipop");
//            parcelFileDescriptors = ParcelFileDescriptor.createPipe();
//            parcelRead = new ParcelFileDescriptor(parcelFileDescriptors[0]);
//            parcelWrite = new ParcelFileDescriptor(parcelFileDescriptors[1]);
//        }
//    }
//
//    protected void closeSockets()
//    {
//        if (pipeAPI == PIPE_API_LS) {
//            try {
//                receiver.close();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            try {
//                sender.close();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            try {
//                localSocket.close();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            localSocket = null;
//            sender = null;
//            receiver = null;
//        } else {
//            try {
//                if (parcelRead != null)
//                    parcelRead.close();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            try {
//                if (parcelWrite != null)
//                    parcelWrite.close();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
}
