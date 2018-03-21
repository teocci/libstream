package com.github.teocci.libstream.interfaces.video;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Pair;

import java.nio.ByteBuffer;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */

public interface H264Sinker
{
    void onPSReady(Pair<ByteBuffer, ByteBuffer> psPair);

    void onH264Data(ByteBuffer h264Buffer, MediaCodec.BufferInfo info);

    void onVideoFormat(MediaFormat mediaFormat);
}
