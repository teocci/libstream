package com.github.teocci.libstream.protocols.rtp.packets;

import android.media.MediaCodec.BufferInfo;

import com.github.teocci.libstream.protocols.rtp.sockets.RtpSocketTcp;
import com.github.teocci.libstream.protocols.rtp.sockets.RtpSocketUdp;
import com.github.teocci.libstream.protocols.rtsp.Session;
import com.github.teocci.libstream.utils.LogHelper;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.github.teocci.libstream.utils.rtsp.RtpConstants.MAX_PACKET_SIZE;
import static com.github.teocci.libstream.utils.rtsp.RtpConstants.RTP_HEADER_LENGTH;

/**
 * RFC 3640.
 * Encapsulates AAC Access Units in RTP packets as specified in the RFC 3640.
 * <p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public class AacPacket extends BasePacket
{
    private static String TAG = LogHelper.makeLogTag(AacPacket.class);

    private long oldTs;

    public AacPacket(Session currentSession, boolean tcpProtocol)
    {
        super(currentSession, tcpProtocol);
    }

    @Override
    public void updateDestination()
    {
        if (socket instanceof RtpSocketUdp) {
            ((RtpSocketUdp) socket).setDestination(
                    session.getDestination(),
                    session.getAudioPorts()[0],
                    session.getAudioPorts()[1]
            );
        } else {
            ((RtpSocketTcp) socket).setOutputStream(session.getOutputStream(), (byte) 0);
        }
    }

    @Override
    public void createAndSendPacket(ByteBuffer byteBuffer, BufferInfo bufferInfo)
    {
        // 3.3.6.  High Bit-rate AAC
        //
        //    0               1               2               3
        //    0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
        //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        // 0 |                           RTP header                          |
        //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        //   |                              ...                              |
        //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        // 3 |       AU-headers-length       |         AU-Size         |AU-In|
        //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        // 4 |                        AAC frame data                         |
        //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        // n |                              ...                              |
        //   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

        boolean interrupted = false;
        try {
            buffer = socket.requestBuffer();
            int length = MAX_PACKET_SIZE - (RTP_HEADER_LENGTH + 4) <= bufferInfo.size - byteBuffer.position() ?
                    MAX_PACKET_SIZE - (RTP_HEADER_LENGTH + 4) :
                    bufferInfo.size - byteBuffer.position();
            if (length > 0) {
                byteBuffer.get(buffer, RTP_HEADER_LENGTH + 4, length);
                oldTs = ts;
                ts = bufferInfo.presentationTimeUs * 1000;
                if (oldTs > ts) {
                    socket.commitBuffer();
                    return;
                }
                socket.markNextPacket();
                socket.updateTimestamp(ts);

                // AU-headers-length field: contains the size in bits of a AU-header
                // 13+3 = 16 bits -> 13bits for AU-size and 3bits for AU-Index / AU-Index-delta
                // 13 bits will be enough because ADTS uses 13 bits for frame length
                buffer[RTP_HEADER_LENGTH] = 0;
                buffer[RTP_HEADER_LENGTH + 1] = 0x10;

                // AU-size
                buffer[RTP_HEADER_LENGTH + 2] = (byte) (length >> 5);
                buffer[RTP_HEADER_LENGTH + 3] = (byte) (length << 3);

                // AU-Index
                buffer[RTP_HEADER_LENGTH + 3] &= 0xF8;
                buffer[RTP_HEADER_LENGTH + 3] |= 0x00;

                socket.commitBuffer(RTP_HEADER_LENGTH + length + 4);
            } else {
                socket.commitBuffer();
            }
        } catch (IOException | ArrayIndexOutOfBoundsException e) {
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

    public void setSampleRate(int sampleRate)
    {
        socket.setClockFrequency(sampleRate);
    }
}