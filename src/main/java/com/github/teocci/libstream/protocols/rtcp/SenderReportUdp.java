package com.github.teocci.libstream.protocols.rtcp;

import com.github.teocci.libstream.utils.LogHelper;
import com.github.teocci.libstream.interfaces.ConnectCheckerRtsp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public class SenderReportUdp extends BaseSenderReport
{
    private static String TAG = LogHelper.makeLogTag(SenderReportUdp.class);

    private MulticastSocket socket;
    private DatagramPacket datagramPacket;

    private int port = -1;

    private ConnectCheckerRtsp connectCheckerRtsp;

    public SenderReportUdp(ConnectCheckerRtsp connectCheckerRtsp)
    {
        super();
        this.connectCheckerRtsp = connectCheckerRtsp;

        try {
            socket = new MulticastSocket();
        } catch (IOException e) {
            // Very unlikely to happen. Means that all UDP ports are already being used
            throw new RuntimeException(e.getMessage());
        }
        datagramPacket = new DatagramPacket(buffer, 1);
    }

    public void close()
    {
        socket.close();
    }

    /**
     * Updates the number of packets sent, and the total amount of data sent.
     *
     * @param length The length of the packet
     * @param rtpts  The RTP timestamp.
     * @param port   to send packet
     **/
    public void update(int length, long rtpts, int port)
    {
        if (updateSend(length)) send(System.nanoTime(), rtpts, port);
    }

    public void setDestination(InetAddress dest, int dport)
    {
        port = dport;
        datagramPacket.setPort(dport);
        datagramPacket.setAddress(dest);
    }

    /**
     * Sends the RTCP packet over the network.
     *
     * @param ntpts the NTP timestamp.
     * @param rtpts the RTP timestamp.
     */
    private void send(final long ntpts, final long rtpts, final int port)
    {
        new Thread(() -> {
            setData(ntpts, rtpts);
            datagramPacket.setLength(PACKET_LENGTH);
            datagramPacket.setPort(port);
            try {
                socket.send(datagramPacket);
                LogHelper.i(TAG, "send report, " + datagramPacket.getPort() + " Port");
            } catch (IOException e) {
                LogHelper.e(TAG, "send UDP report error", e);
                connectCheckerRtsp.onConnectionFailedRtsp("Error send report, " + e.getMessage());
            }
        }).start();
    }

    public int getPort() {
        return port;
    }

    public int getLocalPort() {
        return socket.getLocalPort();
    }
}
