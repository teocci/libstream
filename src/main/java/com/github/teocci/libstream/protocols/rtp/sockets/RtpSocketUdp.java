package com.github.teocci.libstream.protocols.rtp.sockets;

import com.github.teocci.libstream.protocols.rtcp.SenderReportUdp;
import com.github.teocci.libstream.utils.LogHelper;
import com.github.teocci.libstream.interfaces.ConnectCheckerRtsp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public class RtpSocketUdp extends BaseRtpSocket implements Runnable
{
    private static String TAG = LogHelper.makeLogTag(RtpSocketTcp.class);

    private SenderReportUdp senderReportUdp;
    private MulticastSocket socket;
    private DatagramPacket[] packets;

    private ConnectCheckerRtsp connectCheckerRtsp;

    private int port = -1;

    /**
     * This RTP socket implements a buffering mechanism relying on a FIFO of buffers and a Thread.
     */
    public RtpSocketUdp(ConnectCheckerRtsp connectCheckerRtsp)
    {
        super();
        this.connectCheckerRtsp = connectCheckerRtsp;
        senderReportUdp = new SenderReportUdp(connectCheckerRtsp);
        senderReportUdp.reset();
        packets = new DatagramPacket[bufferCount];

        for (int i = 0; i < bufferCount; i++) {
            packets[i] = new DatagramPacket(buffers[i], 1);
        }

        try {
            socket = new MulticastSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The Thread sends the packets in the FIFO one by one at a constant rate.
     */
    @Override
    public void run()
    {
        try {
            while (bufferCommitted.tryAcquire(4, TimeUnit.SECONDS)) {
                send();
                if (++bufferOut >= bufferCount) bufferOut = 0;
                bufferRequested.release();
            }
        } catch (IOException | InterruptedException e) {
            LogHelper.e(TAG, "UDP send error: ", e);
            connectCheckerRtsp.onConnectionFailedRtsp("Error send packet, " + e.getMessage());
        }
        thread = null;
        resetFifo();
        senderReportUdp.reset();
    }

    @Override
    public void setSSRC(int ssrc)
    {
        this.ssrc = ssrc;
        updateSSRC(ssrc);
        senderReportUdp.setSSRC(ssrc);
    }

    @Override
    protected void send() throws IOException
    {
        if (packets[bufferOut] == null) return;
        try {
            senderReportUdp.update(packets[bufferOut].getLength(), timestamps[bufferOut], port);
            socket.send(packets[bufferOut]);
            LogHelper.i(TAG, "send packet, "
                    + packets[bufferOut].getLength() + " Size, "
                    + packets[bufferOut].getPort() + " Port"
            );
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void commitLength(int length)
    {
        packets[bufferIn].setLength(length);
    }

    /**
     * Closes the underlying socket.
     */
    public void close()
    {
        socket.close();
        senderReportUdp.close();
    }

    /**
     * Sets the Time To Live of the UDP packets.
     */
    public void setTimeToLive(int ttl) throws IOException
    {
        socket.setTimeToLive(ttl);
    }

    /**
     * Sets the destination address and to which the packets will be sent.
     */
    public void setDestination(String host, int dport, int rtcpPort)
    {
        try {
            InetAddress dest = InetAddress.getByName(host);
            if (dport != 0 && rtcpPort != 0) {
                port = dport;
                for (int i = 0; i < bufferCount; i++) {
                    packets[i].setPort(dport);
                    packets[i].setAddress(dest);
                }
                senderReportUdp.setDestination(dest, rtcpPort);
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public int getPort()
    {
        return port;
    }

    public int[] getLocalPorts()
    {
        return new int[]{
                socket.getLocalPort(),
                senderReportUdp.getLocalPort()
        };
    }
}