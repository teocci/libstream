package com.github.teocci.libstream.utils.rtsp;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public class RtpConstants
{
    public static final long CLOCK_VIDEO_FREQUENCY = 90000L;

    public static final int RTP_HEADER_LENGTH = 12;

    public static final int MTU = 1300;
    public static final int PAYLOAD_TYPE = 96;

    // Used on all packets
    public final static int MAX_PACKET_SIZE = MTU - 28;
}
