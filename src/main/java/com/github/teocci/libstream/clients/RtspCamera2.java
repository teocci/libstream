package com.github.teocci.libstream.clients;

import android.content.Context;
import android.media.MediaCodec;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.SurfaceView;
import android.view.TextureView;

import com.github.teocci.libstream.base.Camera2Base;
import com.github.teocci.libstream.enums.Protocol;
import com.github.teocci.libstream.input.audio.AudioQuality;
import com.github.teocci.libstream.interfaces.ConnectCheckerRtsp;
import com.github.teocci.libstream.protocols.rtsp.RtspClient;
import com.github.teocci.libstream.view.OpenGlView;

import java.nio.ByteBuffer;

/**
 * This builder is under test, rotation only work with hardware because use encoding surface mode.
 * <p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Dec-14
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class RtspCamera2 extends Camera2Base
{
    private RtspClient rtspClient;

    public RtspCamera2(SurfaceView surfaceView, ConnectCheckerRtsp connectCheckerRtsp)
    {
        super(surfaceView, surfaceView.getContext());
        rtspClient = new RtspClient(connectCheckerRtsp);
    }

    public RtspCamera2(TextureView textureView, ConnectCheckerRtsp connectCheckerRtsp)
    {
        super(textureView, textureView.getContext());
        rtspClient = new RtspClient(connectCheckerRtsp);
    }

    public RtspCamera2(OpenGlView openGlView, ConnectCheckerRtsp connectCheckerRtsp)
    {
        super(openGlView, openGlView.getContext());
        rtspClient = new RtspClient(connectCheckerRtsp);
    }

    public RtspCamera2(Context context, ConnectCheckerRtsp connectCheckerRtsp)
    {
        super(context);
        rtspClient = new RtspClient(connectCheckerRtsp);
    }

    @Override
    public void setAuthorization(String user, String password)
    {
        rtspClient.setAuthorization(user, password);
    }

    @Override
    protected void prepareAudioRtp(AudioQuality audioQuality)
    {
        rtspClient.setChannel(audioQuality.channel);
        rtspClient.setSampleRate(audioQuality.sampling);
    }

    @Override
    protected void startStreamRtp(String url)
    {
        rtspClient.setUrl(url);
        if (!cameraManager.isPrepared()) {
            rtspClient.connect();
        }
    }

    @Override
    protected void stopStreamRtp()
    {
        rtspClient.disconnect();
    }

    @Override
    protected void getAacDataRtp(ByteBuffer aacBuffer, MediaCodec.BufferInfo info)
    {
        rtspClient.sendAudio(aacBuffer, info);
    }

    @Override
    protected void onSPSandPPSRtp(ByteBuffer sps, ByteBuffer pps)
    {
        ByteBuffer newSps = sps.duplicate();
        ByteBuffer newPps = pps.duplicate();
        rtspClient.setSPSandPPS(newSps, newPps);
        rtspClient.connect();
    }

    @Override
    protected void getH264DataRtp(ByteBuffer h264Buffer, MediaCodec.BufferInfo info)
    {
        rtspClient.sendVideo(h264Buffer, info);
    }

    public void setProtocol(Protocol protocol)
    {
        rtspClient.setProtocol(protocol);
    }
}

