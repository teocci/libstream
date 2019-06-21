package com.github.teocci.libstream.protocols.rtsp.rtcp;

import com.github.teocci.libstream.utils.LogHelper;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public abstract class BaseSenderReport
{
    private final static String TAG = LogHelper.makeLogTag(BaseSenderReport.class);

    protected static final int MTU = 1500;
    protected static final int PACKET_LENGTH = 28;

    protected byte[] buffer = new byte[MTU];

    protected int ssrc;
    protected int octetCount = 0, packetCount = 0;
    protected long interval, delta, now, old;

    public BaseSenderReport()
    {
        //   0               1               2               3
        //   0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
        //  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        //  |V=2|P|    RC   |   PT=SR=200   |             length            | header
        //  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

        // RTCP-version field (V) must be 2
        // Padding (P) and reception report count (RC) fields are set to zero
        buffer[0] = (byte) Integer.parseInt("10000000", 2);

        // Payload Type
        buffer[1] = (byte) 200;

        // Byte 2,3          ->  Length
        setLong(PACKET_LENGTH / 4 - 1, 2, 4);

        // Byte 4,5,6,7      ->  SSRC
        // Byte 8,9,10,11    ->  NTP timestamp hb
        // Byte 12,13,14,15  ->  NTP timestamp lb
        // Byte 16,17,18,19  ->  RTP timestamp
        // Byte 20,21,22,23  ->  Packet count
        // Byte 24,25,26,27  ->  Octet count

        // By default we sent one report every 3 second
        interval = 3000;
    }

    public void setSSRC(int ssrc)
    {
        // Byte 4,5,6,7      ->  SSRC
        this.ssrc = ssrc;
        setLong(ssrc, 4, 8);

        resetCounters();
    }

    /**
     * Updates the number of packets sent, and the total amount of data sent.
     *
     * @param length The length of the packet
     */
    protected boolean updateSend(int length)
    {
        increaseCounters(length);

        now = System.currentTimeMillis();
        delta += old != 0 ? now - old : 0;
        old = now;
        if (interval > 0) {
            if (delta >= interval) {
                // We send a Sender Report
                delta = 0;
                return true;
            }
        }
        return false;
    }

    private void increaseCounters(int length)
    {
        // Byte 20,21,22,23  ->  packet count
        // Byte 24,25,26,27  ->  octet count
        packetCount += 1;
        octetCount += length;
        setLong(packetCount, 20, 24);
        setLong(octetCount, 24, 28);
    }

    /**
     * Resets the reports (total number of bytes sent, number of packets sent, etc.)
     */
    public void reset()
    {
        resetCounters();
        delta = now = old = 0;
    }

    private void resetCounters()
    {
        // Byte 20,21,22,23  ->  packet count
        // Byte 24,25,26,27  ->  octet count
        packetCount = 0;
        octetCount = 0;
        setLong(packetCount, 20, 24);
        setLong(octetCount, 24, 28);
    }

    protected void setLong(long n, int begin, int end)
    {
        for (end--; end >= begin; end--) {
            buffer[end] = (byte) (n % 256);
            n >>= 8;
        }
    }

    protected void setData(long ntpts, long rtpts)
    {
        // Byte 8,9,10,11    ->  NTP timestamp hb
        // Byte 12,13,14,15  ->  NTP timestamp lb
        long hb = ntpts / 1000000000;
        long lb = ((ntpts - hb * 1000000000) * 4294967296L) / 1000000000;
        setLong(hb, 8, 12);
        setLong(lb, 12, 16);

        // Byte 16,17,18,19  ->  RTP timestamp
        setLong(rtpts, 16, 20);
    }
}
