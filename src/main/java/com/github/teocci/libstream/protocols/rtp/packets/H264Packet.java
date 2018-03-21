package com.github.teocci.libstream.protocols.rtp.packets;

import android.media.MediaCodec.BufferInfo;

import com.github.teocci.libstream.protocols.rtp.sockets.RtpSocketTcp;
import com.github.teocci.libstream.protocols.rtp.sockets.RtpSocketUdp;
import com.github.teocci.libstream.protocols.rtsp.Session;
import com.github.teocci.libstream.utils.LogHelper;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.github.teocci.libstream.utils.rtsp.RtpConstants.CLOCK_VIDEO_FREQUENCY;
import static com.github.teocci.libstream.utils.rtsp.RtpConstants.MAX_PACKET_SIZE;
import static com.github.teocci.libstream.utils.rtsp.RtpConstants.RTP_HEADER_LENGTH;

/**
 * RFC 3984.
 * H264 streaming over RTP.
 * <p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public class H264Packet extends BasePacket
{
    private static String TAG = LogHelper.makeLogTag(H264Packet.class);

    // Contain header from ByteBuffer (first 5 bytes)
    private byte[] header = new byte[5];
    private byte[] stapA;

    public H264Packet(Session session, boolean tcpProtocol)
    {
        super(session, tcpProtocol);
        socket.setClockFrequency(CLOCK_VIDEO_FREQUENCY);
    }

    @Override
    public void updateDestination()
    {
        if (socket instanceof RtpSocketUdp) {
            ((RtpSocketUdp) socket).setDestination(
                    session.getDestination(),
                    session.getVideoPorts()[0],
                    session.getVideoPorts()[1]
            );
        } else {
            ((RtpSocketTcp) socket).setOutputStream(session.getOutputStream(), (byte) 2);
        }
    }

    @Override
    public void createAndSendPacket(ByteBuffer byteBuffer, BufferInfo bufferInfo)
    {
        boolean interrupted = false;
        // We read a NAL units from ByteBuffer and we send them
        try {
            // NAL units are preceded with 0x01
            byteBuffer.get(header, 0, 5);
            ts = bufferInfo.presentationTimeUs * 1000L;
            int naluLength = bufferInfo.size - byteBuffer.position() + 1;
            int type = header[4] & 0x1F; // 00011111 = 0x1F

            if (type == 5) {
                buffer = socket.requestBuffer();
                socket.markNextPacket();
                socket.updateTimestamp(ts);
                System.arraycopy(stapA, 0, buffer, RTP_HEADER_LENGTH, stapA.length);
                socket.commitBuffer(stapA.length + RTP_HEADER_LENGTH);
            }

            // Small NAL unit -> Single NAL unit
            if (naluLength <= MAX_PACKET_SIZE - RTP_HEADER_LENGTH - 2) {
                buffer = socket.requestBuffer();
                buffer[RTP_HEADER_LENGTH] = header[4];

                int cont = naluLength - 1;
                int bufferSize = bufferInfo.size - byteBuffer.position();
                int length = cont < bufferSize ? cont : bufferSize;
                byteBuffer.get(buffer, RTP_HEADER_LENGTH + 1, length);

                socket.updateTimestamp(ts);
                socket.markNextPacket();
                socket.commitBuffer(naluLength + RTP_HEADER_LENGTH);
            } else { // Large NAL unit -> Split NAL unit
                // Set FU-A header
                // +---------------+
                // |0|1|2|3|4|5|6|7|
                // +-+-+-+-+-+-+-+-+
                // |S|E|R|  Type   |
                // +---------------+

                // FU header type
                // 00011111 = 0x1F
                header[1] = (byte) (header[4] & 0x1F);
                // Start bit
                // 10000000 = 0x80
                header[1] += 0x80;

                // Set FU-A indicator
                // +---------------+
                // |0|1|2|3|4|5|6|7|
                // +-+-+-+-+-+-+-+-+
                // |F|NRI|  Type   |
                // +---------------+

                // FU indicator NRI
                // 01100000 = 0x60
                // 11111111 = 0xFF
                header[0] = (byte) ((header[4] & 0x60) & 0xFF);
                // Type = 28 FU-A Fragmentation unit (RFC6184 5.8)
                header[0] += 28;

                int sum = 1;

                while (sum < naluLength) {
                    buffer = socket.requestBuffer();
                    buffer[RTP_HEADER_LENGTH] = header[0];
                    buffer[RTP_HEADER_LENGTH + 1] = header[1];
                    socket.updateTimestamp(ts);

                    int cont = naluLength - sum > MAX_PACKET_SIZE - RTP_HEADER_LENGTH - 2 ?
                            MAX_PACKET_SIZE - RTP_HEADER_LENGTH - 2 : naluLength - sum;
                    int bufferSize = bufferInfo.size - byteBuffer.position();
                    int length = cont < bufferSize ? cont : bufferSize;

                    byteBuffer.get(buffer, RTP_HEADER_LENGTH + 2, length);
                    if (length < 0) {
                        return;
                    }

                    sum += length;
                    // Last packet before next NAL
                    if (sum >= naluLength) {
                        // End bit on
                        // 01000000 = 0x40
                        buffer[RTP_HEADER_LENGTH + 1] += 0x40;
                        socket.markNextPacket();
                    }
                    socket.commitBuffer(length + RTP_HEADER_LENGTH + 2);
                    // Switch start bit
                    // 01111111 = 0x7F
                    header[1] = (byte) (header[1] & 0x7F);
                }
            }
        } catch (IOException | IndexOutOfBoundsException e) {
            e.printStackTrace();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
            interrupted = true;
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
                close();
            }
        }
    }

    public void setPSPair(byte[] sps, byte[] pps)
    {
        // Single-Time Aggregation Packet type A (STAP-A)

        //  0               1               2               3
        //  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
        // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        // |                          RTP Header                           |
        // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        // |STAP-A NAL HDR |         NALU 1 Size           |               |
        // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+               +
        // |                         NALU 1 Data                           |
        // :                                                               :
        // +               +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        // |               |         NALU 2 Size           |               |
        // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+               +
        // |                         NALU 2 Data                           |
        // :                                                               :
        // |                       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        // |                       :         OPTIONAL RTP padding          |
        // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        stapA = new byte[sps.length + pps.length + 5];

        // STAP-A NAL HDR: The NAL Type is 24, NRI is 00 and F is 0
        // +---------------+
        // |0|1|2|3|4|5|6|7|
        // +-+-+-+-+-+-+-+-+
        // |F|NRI|  Type   |
        // +---------------+
        stapA[0] = 24;

        // Write NALU 1 size into the array (NALU 1 is the SPS).
        // 11111111 = 0xFF
        stapA[1] = (byte) (sps.length >> 8);
        stapA[2] = (byte) (sps.length & 0xFF);

        // Write NALU 2 size into the array (NALU 2 is the PPS).
        // 11111111 = 0xFF
        stapA[sps.length + 3] = (byte) (pps.length >> 8);
        stapA[sps.length + 4] = (byte) (pps.length & 0xFF);

        // Write NALU 1 into the array, then write NALU 2 into the array.
        System.arraycopy(sps, 0, stapA, 3, sps.length);
        System.arraycopy(pps, 0, stapA, 5 + sps.length, pps.length);
    }
}