package com.github.teocci.libstream.protocols.rtp.sockets;

import com.github.teocci.libstream.protocols.rtcp.SenderReportTcp;
import com.github.teocci.libstream.utils.LogHelper;
import com.github.teocci.libstream.interfaces.ConnectCheckerRtsp;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public class RtpSocketTcp extends BaseRtpSocket implements Runnable
{
    private static String TAG = LogHelper.makeLogTag(RtpSocketTcp.class);

    private SenderReportTcp senderReportTcp;
    private byte tcpHeader[];
    private int[] lengths;
    private OutputStream outputStream = null;
    private ConnectCheckerRtsp connectCheckerRtsp;

    private final Object lock = new Object();

    public RtpSocketTcp(ConnectCheckerRtsp connectCheckerRtsp)
    {
        super();
        this.connectCheckerRtsp = connectCheckerRtsp;
        lengths = new int[bufferCount];
        senderReportTcp = new SenderReportTcp(connectCheckerRtsp);
        senderReportTcp.reset();
        tcpHeader = new byte[]{'$', 0, 0, 0};
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
            LogHelper.e(TAG, "TCP send error: ", e);
            connectCheckerRtsp.onConnectionFailedRtsp("Error send packet, " + e.getMessage());
        }

        thread = null;
        resetFifo();
        senderReportTcp.reset();
    }

    @Override
    public void setSSRC(int ssrc)
    {
        this.ssrc = ssrc;
        updateSSRC(ssrc);
        senderReportTcp.setSSRC(ssrc);
    }

    @Override
    protected void send() throws IOException
    {
        if (outputStream == null) return;
        senderReportTcp.update(lengths[bufferOut], timestamps[bufferOut]);
        synchronized (outputStream) {
            int len = lengths[bufferOut];
            tcpHeader[2] = (byte) (len >> 8);
            tcpHeader[3] = (byte) (len & 0xFF);
            outputStream.write(tcpHeader);
            outputStream.write(buffers[bufferOut], 0, len);
            outputStream.flush();
            LogHelper.i(TAG, "send packet, " + len + " Size");
        }
    }

    /**
     * Sends the RTP packet over the network.
     */
    @Override
    public void commitLength(int length)
    {
        lengths[bufferIn] = length;
    }

    public void setOutputStream(OutputStream outputStream, byte channelIdentifier)
    {
        if (outputStream != null) {
            this.outputStream = outputStream;
            tcpHeader[1] = channelIdentifier;
            senderReportTcp.setOutputStream(outputStream, (byte) (channelIdentifier + 1));
        }
    }

    public void close() throws IOException
    {
        if (outputStream != null) {
            outputStream.close();
        }
    }
}