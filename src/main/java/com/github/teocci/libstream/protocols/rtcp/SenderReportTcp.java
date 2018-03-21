package com.github.teocci.libstream.protocols.rtcp;

import com.github.teocci.libstream.utils.LogHelper;
import com.github.teocci.libstream.interfaces.ConnectCheckerRtsp;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public class SenderReportTcp extends BaseSenderReport
{
    private static String TAG = LogHelper.makeLogTag(SenderReportTcp.class);
    
    private final byte[] tcpHeader;
    private OutputStream outputStream = null;

    private ConnectCheckerRtsp connectCheckerRtsp;

    public SenderReportTcp(ConnectCheckerRtsp connectCheckerRtsp)
    {
        super();
        this.connectCheckerRtsp = connectCheckerRtsp;
        tcpHeader = new byte[]{'$', 0, 0, PACKET_LENGTH};
    }

    /**
     * Updates the number of packets sent, and the total amount of data sent.
     *
     * @param length The length of the packet
     * @param rtpts  The RTP timestamp.
     **/
    public void update(int length, long rtpts)
    {
        if (updateSend(length)) send(System.nanoTime(), rtpts);
    }

    /**
     * Sends the RTCP packet over the network.
     *
     * @param ntpts the NTP timestamp.
     * @param rtpts the RTP timestamp.
     */
    private void send(final long ntpts, final long rtpts)
    {
        new Thread(() -> {
            setData(ntpts, rtpts);
            synchronized (outputStream) {
                try {
                    outputStream.write(tcpHeader);
                    outputStream.write(buffer, 0, PACKET_LENGTH);
                    LogHelper.i(TAG, "send report");
                } catch (IOException e) {
                    LogHelper.e(TAG, "send TCP report error", e);
                    connectCheckerRtsp.onConnectionFailedRtsp("Error send report, " + e.getMessage());
                }
            }
        }).start();
    }

    public void setOutputStream(OutputStream os, byte channelIdentifier)
    {
        outputStream = os;
        tcpHeader[1] = channelIdentifier;
    }
}
