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

public interface EncoderSinker
{
    void onPSReady(Pair<ByteBuffer, ByteBuffer> psPair);

    void onSpsPpsVpsReady(ByteBuffer sps, ByteBuffer pps, ByteBuffer vps);

    void onEncodedData(ByteBuffer videoBuffer, MediaCodec.BufferInfo info);

    void onVideoFormat(MediaFormat mediaFormat);
}
