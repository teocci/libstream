package com.github.teocci.libstream.input.media;

import com.github.teocci.libstream.enums.Protocol;
import com.github.teocci.libstream.input.audio.AudioQuality;
import com.github.teocci.libstream.input.audio.AudioStream;
import com.github.teocci.libstream.input.video.VideoQuality;
import com.github.teocci.libstream.input.video.VideoStream;
import com.github.teocci.libstream.interfaces.Stream;
import com.github.teocci.libstream.protocols.rtp.packets.BasePacket;
import com.github.teocci.libstream.protocols.rtp.sockets.BaseRtpSocket;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;

import static com.github.teocci.libstream.enums.Protocol.TCP;
import static com.github.teocci.libstream.enums.Protocol.UDP;

/**
 * A MediaRecorder that streams what it records using a packetizer from the RTP package.
 * You can't use this class directly !
 */
public abstract class MediaStream implements Stream
{
    protected static final String TAG = "MediaStream";

    /**
     * The packetizer that will read the output of the camera and send RTP packets over the networked.
     */
    protected boolean streaming = false, configured = false;
    public Protocol protocol = TCP;

    protected InetAddress destination;

    protected int rtpPort = 0, rtcpPort = 0;
    protected int[] ports = new int[]{5000, 5001};

    protected byte channelIdentifier = 0;
    protected OutputStream outputStream = null;

    protected BasePacket packetizer;

    private int timeToLive = 64;

    public MediaStream() {}

    /**
     * Sets the destination IP address of the stream.
     *
     * @param dest The destination address of the stream
     */
    public void setDestinationAddress(InetAddress dest)
    {
        destination = dest;
    }

    /**
     * Sets the destination ports of the stream.
     * If an odd number is supplied for the destination port then the next
     * lower even number will be used for RTP and it will be used for RTCP.
     * If an even number is supplied, it will be used for RTP and the next odd
     * number will be used for RTCP.
     *
     * @param dport The destination port
     */
    public void setDestinationPorts(int dport)
    {
        if (protocol != UDP) return;
        if (dport % 2 == 1) {
            rtpPort = dport - 1;
            rtcpPort = dport;
        } else {
            rtpPort = dport;
            rtcpPort = dport + 1;
        }

        ports = new int[]{rtpPort, rtcpPort};
        this.outputStream = null;
    }

    /**
     * Sets the destination ports of the stream.
     *
     * @param rtpPort  Destination port that will be used for RTP
     * @param rtcpPort Destination port that will be used for RTCP
     */
    public void setDestinationPorts(int rtpPort, int rtcpPort)
    {
        if (protocol != UDP) return;
        this.rtpPort = rtpPort;
        this.rtcpPort = rtcpPort;
        this.ports = new int[]{rtpPort, rtcpPort};
        this.outputStream = null;
    }

    /**
     * If a TCP is used as the transport protocol for the RTP session,
     * the output stream to which RTP packets will be written to must
     * be specified with this method.
     */
    public void setOutputStream(OutputStream stream, byte channelID)
    {
        if (protocol != TCP) return;
        outputStream = stream;
        channelIdentifier = channelID;
    }


    /**
     * Sets the Time To Live of packets sent over the network.
     *
     * @param ttl The time to live
     * @throws IOException
     */
    public void setTimeToLive(int ttl) throws IOException
    {
        timeToLive = ttl;
    }

    /**
     * Returns a pair of destination ports, the first one is the
     * one used for RTP and the second one is used for RTCP.
     **/
    public int[] getDestinationPorts()
    {
        return ports;
    }

    /**
     * Returns a pair of source ports, the first one is the
     * one used for RTP and the second one is used for RTCP.
     **/
    public int[] getLocalPorts()
    {
        return packetizer.getLocalPorts();
    }

    /**
     * Returns the packetizer associated with the {@link MediaStream}.
     *
     * @return The packetizer
     */
    public BasePacket getPacketizer()
    {
        return packetizer;
    }

    /**
     * Returns an approximation of the bit rate consumed by the stream in bit per seconde.
     */
    public long getBitrate()
    {
        return !streaming ? 0 : packetizer.getBitrate();
    }

    /**
     * Indicates if the {@link MediaStream} is streaming.
     *
     * @return A boolean indicating if the {@link MediaStream} is streaming
     */
    public boolean isStreaming()
    {
        return streaming;
    }

    /**
     * Configures the stream with the settings supplied with
     * {@link VideoStream#setQuality(VideoQuality)} for a {@link VideoStream} and
     * {@link AudioStream#setQuality(AudioQuality)} for a {@link AudioStream}.
     */
    public synchronized void configure() throws IllegalStateException, IOException
    {
        if (streaming) throw new IllegalStateException("Can't be called while streaming.");
//        if (packetizer != null) {
//            packetizer.setDestination(destination, rtpPort, rtcpPort);
//            packetizer.getRtpSocket().setOutputStream(outputStream, channelIdentifier);
//        }

        configured = true;
    }

    /**
     * Starts the stream.
     */
    public synchronized void start() throws IllegalStateException, IOException
    {
        if (protocol == UDP) {
            if (destination == null)
                throw new IllegalStateException("No destination ip address set for the stream !");

            if (rtpPort <= 0 || rtcpPort <= 0)
                throw new IllegalStateException("No destination ports set for the stream !");

//            if (packetizer instanceof AacPacket) {
//                packetizer.setTimeToLive(timeToLive);
//            }
        }

//        encodeWithMediaCodec();
    }


    /**
     * Stops the stream.
     */
    public synchronized void stop()
    {
        if (streaming) {
            try {
//                packetizer.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
            streaming = false;
        }
    }

//    /**
//     * Returns a description of the stream using SDP.
//     * This method can only be called after {@link Stream#configure()}.
//     *
//     * @throws IllegalStateException Thrown when {@link Stream#configure()} was not called.
//     */
//    public abstract String getSessionDescription();
//

    /**
     * Returns the SSRC of the underlying {@link BaseRtpSocket}.
     *
     * @return the SSRC of the stream
     */
    public int getSSRC()
    {
        return getPacketizer().getSSRC();
    }

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
}
