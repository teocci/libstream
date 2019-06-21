package com.github.teocci.libstream.clients;

import android.media.MediaCodec;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.github.teocci.libstream.base.FromFileBase;
import com.github.teocci.libstream.enums.Protocol;
import com.github.teocci.libstream.interfaces.ConnectCheckerRtsp;
import com.github.teocci.libstream.interfaces.video.VideoDecoderListener;
import com.github.teocci.libstream.protocols.rtsp.rtsp.RtspClient;

import java.nio.ByteBuffer;

/**
 * This builder is under test, rotation only work with hardware because use encoding surface mode.
 * Only video is working, audio will be added when it work
 * <p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Dec-14
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class RtspFromFile extends FromFileBase
{
    private RtspClient rtspClient;

    public RtspFromFile(ConnectCheckerRtsp connectCheckerRtsp,
                        VideoDecoderListener videoListener)
    {
        super(videoListener);
        rtspClient = new RtspClient(connectCheckerRtsp);
    }

    @Override
    public void setAuthorization(String user, String password)
    {
        rtspClient.setAuthorization(user, password);
    }

    @Override
    protected void startStreamRtp(String url)
    {
        rtspClient.setUrl(url);
    }

    @Override
    protected void stopStreamRtp()
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
    protected void getH264DataRtp(ByteBuffer h264Buffer, MediaCodec.BufferInfo info)
    {
        rtspClient.sendVideo(h264Buffer, info);
    }

    public void setProtocol(Protocol protocol)
    {
        rtspClient.setProtocol(protocol);
    }
}

