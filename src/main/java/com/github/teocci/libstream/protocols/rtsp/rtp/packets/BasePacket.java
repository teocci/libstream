package com.github.teocci.libstream.protocols.rtsp.rtp.packets;

import android.media.MediaCodec;

import com.github.teocci.libstream.protocols.rtsp.rtp.sockets.BaseRtpSocket;
import com.github.teocci.libstream.protocols.rtsp.rtp.sockets.RtpSocketTcp;
import com.github.teocci.libstream.protocols.rtsp.rtp.sockets.RtpSocketUdp;
import com.github.teocci.libstream.protocols.rtsp.rtsp.Session;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

/**
 * All packets inherits from this one and therefore uses UDP.
 * <p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public abstract class BasePacket
{
    protected BaseRtpSocket socket;

    protected byte[] buffer;
    protected long ts;

    protected Session session;

    protected volatile boolean streaming;

    public BasePacket(Session session)
    {
        this.session = session;
        ts = new Random().nextInt();
        socket = session.isTCP() ?
                new RtpSocketTcp(session.getConnectCheckerRtsp()) :
                new RtpSocketUdp(session.getConnectCheckerRtsp());

        socket.setSSRC(new Random().nextInt());

        if (socket instanceof RtpSocketUdp) {
            try {
                ((RtpSocketUdp) socket).setTimeToLive(64);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        streaming = true;
    }

    public void close()
    {
        streaming = false;
        if (socket instanceof RtpSocketUdp) {
            ((RtpSocketUdp) socket).close();
        }
        if (socket instanceof RtpSocketTcp) {
            try {
                ((RtpSocketTcp) socket).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public int getSSRC()
    {
        return socket.getSSRC();
    }

    public int[] getLocalPorts()
    {
        if (socket instanceof RtpSocketUdp) {
            return ((RtpSocketUdp) socket).getLocalPorts();
        }

        return null;
    }


    /**
     * Indicates if the {@link BasePacket} is streaming.
     *
     * @return A boolean indicating if the {@link BasePacket} is streaming
     */
    public boolean isStreaming()
    {
        return streaming;
    }


    public abstract void updateDestination();

    public abstract void createAndSendPacket(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo);
}
