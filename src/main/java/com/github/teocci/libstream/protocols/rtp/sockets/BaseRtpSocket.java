package com.github.teocci.libstream.protocols.rtp.sockets;

import com.github.teocci.libstream.utils.AverageBitrate;

import java.io.IOException;
import java.util.concurrent.Semaphore;

import static com.github.teocci.libstream.utils.rtsp.RtpConstants.MTU;
import static com.github.teocci.libstream.utils.rtsp.RtpConstants.PAYLOAD_TYPE;

/**
 * A basic implementation of an RTP socket.
 * It implements a buffering mechanism, relying on a FIFO of buffers and a Thread.
 * That way, if a packetizer tries to send many packets too quickly, the FIFO will
 * grow and packets will be sent one by one smoothly.
 * <p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public abstract class BaseRtpSocket implements Runnable
{
    protected byte[][] buffers;
    protected long[] timestamps;

    protected Semaphore bufferRequested, bufferCommitted;
    protected Thread thread;

    protected int ssrc;

    protected int bufferOut;
    protected long clock = 0;
    protected int seq = 0;
    protected int bufferCount, bufferIn;

    protected AverageBitrate averageBitrate;

    /**
     * This RTP socket implements a buffering mechanism relying on a FIFO of buffers and a Thread.
     */
    public BaseRtpSocket()
    {
        bufferCount = 300;
        buffers = new byte[bufferCount][];
        averageBitrate = new AverageBitrate();

        resetFifo();

        //   0               1               2               3
        //   0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
        //  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        //  |V=2|P|X|  CC   |M|     PT      |       sequence number         |
        //  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        //  |                           timestamp                           |
        //  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        //  |           synchronization source (SSRC) identifier            |
        //  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

        for (int i = 0; i < bufferCount; i++) {
            buffers[i] = new byte[MTU];

            // RTP-version field (V) must be 2
            // Padding (P), extension (X), number of contributing sources (CC),
            // and marker (M) fields. These are all set to zero
            // Byte 0          ->  V, P, X, CC.
            buffers[i][0] = (byte) Integer.parseInt("10000000", 2);

            // Byte 1          ->  M, Payload Type (PT)
            buffers[i][1] = (byte) PAYLOAD_TYPE;

            // Byte 2,3        ->  Sequence Number
            // Byte 4,5,6,7    ->  Timestamp
            // Byte 8,9,10,11  ->  Sync Source Identifier (SSRC)
        }
    }

    protected void resetFifo()
    {
        bufferIn = 0;
        bufferOut = 0;
        timestamps = new long[bufferCount];
        bufferRequested = new Semaphore(bufferCount);
        bufferCommitted = new Semaphore(0);
        averageBitrate.reset();
    }

    /**
     * Returns the SSRC of the stream.
     */
    public int getSSRC()
    {
        return ssrc;
    }

    /**
     * Sets the clock frequency of the stream in Hz.
     */
    public void setClockFrequency(long clock)
    {
        this.clock = clock;
    }

    /**
     * Returns an available buffer from the FIFO, it can then be modified.
     *
     * @throws InterruptedException
     **/
    public byte[] requestBuffer() throws InterruptedException
    {
        if (Thread.interrupted())  // Clears interrupted status!
            throw new InterruptedException();
        bufferRequested.acquire();
        try {
            buffers[bufferIn][1] &= 0x7F;
            return buffers[bufferIn];
        } finally {
            bufferRequested.release();
        }
    }

    /**
     * Increments the sequence number.
     */
    public void increaseSeq()
    {
        // Byte 2,3 -> Sequence Number
        setLong(buffers[bufferIn], ++seq, 2, 4);
    }

    /**
     * Overwrites the timestamp in the packet.
     *
     * @param timestamp The new timestamp in ns.
     **/
    public void updateTimestamp(long timestamp)
    {
        long ts = timestamp * clock / 1000000000L;
        timestamps[bufferIn] = ts;

        // Byte 4,5,6,7 -> Timestamp
        setLong(buffers[bufferIn], ts, 4, 8);
    }

    /**
     * Overwrites the sync source identifier (SSRC) in each packet.
     *
     * @param ssrc The new sync source identifier.
     **/
    protected void updateSSRC(int ssrc)
    {
        // Byte 8,9,10,11 -> Sync Source Identifier (SSRC)
        for (int i = 0; i < bufferCount; i++) {
            setLong(buffers[i], ssrc, 8, 12);
        }
    }

    public void commitBuffer() throws IOException
    {
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
        if (++bufferIn >= bufferCount) bufferIn = 0;
        bufferCommitted.release();
    }

    /**
     * Sends the RTP packet over the network.
     */
    public void commitBuffer(int length) throws IOException
    {
        increaseSeq();
        commitLength(length);
        averageBitrate.push(length);
        if (++bufferIn >= bufferCount) bufferIn = 0;
        bufferCommitted.release();
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }

    /**
     * Sets the marker in the RTP packet.
     */
    public void markNextPacket()
    {
        // Byte 1
        //  0 1 2 3 4 5 6 7
        // +-+-+-+-+-+-+-+-+
        // |M|     PT      |
        // +-+-+-+-+-+-+-+-+
        //  1 0 0 0 0 0 0 0 = 0x80
        buffers[bufferIn][1] |= 0x80;
    }

    /**
     * Sets a long into a buffer.
     *
     * @param buffer The buffer into which the long value is to be written.
     * @param n      The value to be inserted.
     * @param begin  The begin byte.
     * @param end    The end byte.
     **/
    protected void setLong(byte[] buffer, long n, int begin, int end)
    {
        for (end--; end >= begin; end--) {
            buffer[end] = (byte) (n % 256);
            n >>= 8;
        }
    }


    /**
     * Returns an approximation of the bitrate of the RTP stream in bits per second.
     */
    public long getBitrate()
    {
        return averageBitrate.average();
    }

    /**
     * Sets the SSRC of the stream.
     */
    public abstract void setSSRC(int ssrc);

    protected abstract void send() throws IOException;

    protected abstract void commitLength(int length);

}