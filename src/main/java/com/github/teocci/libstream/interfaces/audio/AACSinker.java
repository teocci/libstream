package com.github.teocci.libstream.interfaces.audio;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public interface AACSinker
{
    void onAACData(ByteBuffer aacBuffer, MediaCodec.BufferInfo info);

    void onAudioFormat(MediaFormat mediaFormat);
}
