package com.github.teocci.libstream.clients;

import android.media.MediaCodec;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.SurfaceView;
import android.view.TextureView;

import com.github.teocci.libstream.base.AVEncoderBase;
import com.github.teocci.libstream.enums.Protocol;
import com.github.teocci.libstream.input.audio.AudioQuality;
import com.github.teocci.libstream.interfaces.ConnectCheckerRtsp;
import com.github.teocci.libstream.protocols.rtsp.rtsp.RtspClient;
import com.github.teocci.libstream.utils.LogHelper;
import com.github.teocci.libstream.view.OpenGlView;

import java.nio.ByteBuffer;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Dec-14
 */
public class RtspAVEncoder extends AVEncoderBase
{
    private static String TAG = LogHelper.makeLogTag(RtspAVEncoder.class);

    private RtspClient rtspClient;

    public RtspAVEncoder(SurfaceView surfaceView, ConnectCheckerRtsp connectCheckerRtsp)
    {
        super(surfaceView);
        rtspClient = new RtspClient(connectCheckerRtsp);
    }

    public RtspAVEncoder(TextureView textureView, ConnectCheckerRtsp connectCheckerRtsp)
    {
        super(textureView);
        rtspClient = new RtspClient(connectCheckerRtsp);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public RtspAVEncoder(OpenGlView openGlView, ConnectCheckerRtsp connectCheckerRtsp)
    {
        super(openGlView);
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
        rtspClient.setSampleRate(audioQuality.sampleRate);
    }

    @Override
    protected void startRtpStream()
    {
        if (!camManager.isPrepared()) {
            rtspClient.connect();
        }
    }

    @Override
    protected void startRtpStream(String url)
    {
        rtspClient.setUrl(url);
        if (!camManager.isPrepared()) {
            rtspClient.connect();
        }
    }

    @Override
    protected void stopRtpStream()
    {
        rtspClient.disconnect();
    }

    @Override
    protected void sendAVCInfo(ByteBuffer sps, ByteBuffer pps, ByteBuffer vps)
    {
        ByteBuffer newSps = sps.duplicate();
        ByteBuffer newPps = pps.duplicate();
        ByteBuffer newVps = vps != null ? vps.duplicate() : null;
        rtspClient.setAVCInfo(newSps, newPps, newVps);
        rtspClient.connect();
    }

    @Override
    protected void sendAACData(ByteBuffer aacBuffer, MediaCodec.BufferInfo info)
    {
        rtspClient.sendAudio(aacBuffer, info);
    }

    @Override
    protected void sendH264Data(ByteBuffer h264Buffer, MediaCodec.BufferInfo info)
    {

        rtspClient.sendVideo(h264Buffer, info);
    }

    public void setProtocol(Protocol protocol)
    {
        rtspClient.setProtocol(protocol);
    }
}