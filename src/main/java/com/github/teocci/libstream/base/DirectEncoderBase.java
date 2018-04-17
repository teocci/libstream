package com.github.teocci.libstream.base;

import android.media.MediaCodec;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.SurfaceView;
import android.view.TextureView;

import com.github.teocci.libstream.input.audio.AudioQuality;
import com.github.teocci.libstream.utils.LogHelper;
import com.github.teocci.libstream.view.OpenGlView;

import java.nio.ByteBuffer;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public abstract class DirectEncoderBase extends EncoderBase
{
    private static String TAG = LogHelper.makeLogTag(DirectEncoderBase.class);

    public DirectEncoderBase()
    {
        super();
    }

    public DirectEncoderBase(SurfaceView surfaceView)
    {
        super(surfaceView);
    }

    public DirectEncoderBase(TextureView textureView)
    {
        super(textureView);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public DirectEncoderBase(OpenGlView openGlView)
    {
        super(openGlView);
    }

    protected abstract void startRtspStream();

    protected abstract void startRtspStream(String url);

    protected abstract void stopRtspStream();

    protected abstract void prepareAudioRtp(AudioQuality audioQuality);

    protected abstract void sendAACData(ByteBuffer aacBuffer, MediaCodec.BufferInfo info);

    protected abstract void sendH264Data(ByteBuffer h264Buffer, MediaCodec.BufferInfo info);

    protected abstract void setPSPair(ByteBuffer sps, ByteBuffer pps);
}