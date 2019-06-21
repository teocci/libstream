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
public abstract class AVEncoderBase extends EncoderBase
{
    private static String TAG = LogHelper.makeLogTag(AVEncoderBase.class);

    public AVEncoderBase(SurfaceView surfaceView)
    {
        super(surfaceView);
    }

    public AVEncoderBase(TextureView textureView)
    {
        super(textureView);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public AVEncoderBase(OpenGlView openGlView)
    {
        super(openGlView);
    }

    public abstract void setAuthorization(String user, String password);

    protected abstract void startRtpStream();

    protected abstract void startRtpStream(String url);

    protected abstract void stopRtpStream();

    protected abstract void prepareAudioRtp(AudioQuality audioQuality);

    protected abstract void sendAACData(ByteBuffer aacBuffer, MediaCodec.BufferInfo info);

    protected abstract void sendH264Data(ByteBuffer h264Buffer, MediaCodec.BufferInfo info);

    protected abstract void sendAVCInfo(ByteBuffer sps, ByteBuffer pps, ByteBuffer vps);
}