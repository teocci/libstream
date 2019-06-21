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
import com.github.teocci.libstream.protocols.rtsp.rtsp.RtspServerBase;
import com.github.teocci.libstream.utils.LogHelper;
import com.github.teocci.libstream.view.OpenGlView;

import java.nio.ByteBuffer;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Dec-14
 */
public class ServerAVEncoder extends AVEncoderBase
{
    private static String TAG = LogHelper.makeLogTag(ServerAVEncoder.class);

    private RtspServerBase rtspServer;

    public ServerAVEncoder(RtspServerBase rtspServer, SurfaceView surfaceView)
    {
        super(surfaceView);

        this.rtspServer = rtspServer;
    }

    public ServerAVEncoder(RtspServerBase rtspServer, SurfaceView surfaceView, ConnectCheckerRtsp connectCheckerRtsp)
    {
        super(surfaceView);

        this.rtspServer = rtspServer;
        this.rtspServer.setConnectCheckerRtsp(connectCheckerRtsp);
    }

    public ServerAVEncoder(RtspServerBase rtspServer, TextureView textureView, ConnectCheckerRtsp connectCheckerRtsp)
    {
        super(textureView);
        this.rtspServer = rtspServer;
        this.rtspServer.setConnectCheckerRtsp(connectCheckerRtsp);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public ServerAVEncoder(RtspServerBase rtspServer, OpenGlView openGlView, ConnectCheckerRtsp connectCheckerRtsp)
    {
        super(openGlView);
        this.rtspServer = rtspServer;
        this.rtspServer.setConnectCheckerRtsp(connectCheckerRtsp);
    }

    @Override
    public void setAuthorization(String user, String password)
    {
        rtspServer.setAuthorization(user, password);
    }

    @Override
    protected void prepareAudioRtp(AudioQuality audioQuality)
    {
        rtspServer.setChannel(audioQuality.channel);
        rtspServer.setSampleRate(audioQuality.sampleRate);
    }

    @Override
    protected void startRtpStream()
    {
//        if (!camManager.isPrepared()) {
//            rtspServer.start();
//        }
    }

    @Override
    protected void startRtpStream(String url)
    {
        if (!camManager.isPrepared()) {
            rtspServer.start();
        }
    }

    @Override
    protected void stopRtpStream()
    {
        LogHelper.e(TAG, "stopRtpStream()");
//        rtspServer.stop();
    }

    @Override
    protected void sendAVCInfo(ByteBuffer sps, ByteBuffer pps, ByteBuffer vps)
    {
        LogHelper.e(TAG, "sendAVCInfo()");
        ByteBuffer newSps = sps.duplicate();
        ByteBuffer newPps = pps.duplicate();
        ByteBuffer newVps = vps != null ? vps.duplicate() : null;
        rtspServer.setAVCInfo(newSps, newPps, newVps);
        rtspServer.start();
    }

    @Override
    protected void sendAACData(ByteBuffer aacBuffer, MediaCodec.BufferInfo info)
    {
        rtspServer.sendAudio(aacBuffer, info);
    }

    @Override
    protected void sendH264Data(ByteBuffer h264Buffer, MediaCodec.BufferInfo info)
    {
        rtspServer.sendVideo(h264Buffer, info);
    }

    public void setProtocol(Protocol protocol)
    {
        rtspServer.setProtocol(protocol);
    }
}