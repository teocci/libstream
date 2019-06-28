package com.github.teocci.libstream.utils.rtsp;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public class RtpConstants
{
    public static final long CLOCK_VIDEO_FREQUENCY = 90000L;

    public static final int MTU = 1300;
    public static final int PAYLOAD_TYPE = 96;

    // Used on all packets
    public final static int MAX_PACKET_SIZE = MTU - 28;

    public static final int RTP_HEADER_LENGTH = 12;
    public static final int MIN_RTP_PACKET_LENGTH = MAX_PACKET_SIZE - (RTP_HEADER_LENGTH + 4);


    public final static int IPTOS_LOWCOST = 0x02;
    public final static int IPTOS_RELIABILITY = 0x04;
    public final static int IPTOS_THROUGHPUT = 0x08;
    public final static int IPTOS_LOWDELAY = 0x10;
}
