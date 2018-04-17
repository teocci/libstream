package com.github.teocci.libstream.clients;

import android.content.Context;
import android.media.MediaCodec;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.github.teocci.libstream.base.DisplayBase;
import com.github.teocci.libstream.enums.Protocol;
import com.github.teocci.libstream.input.audio.AudioQuality;
import com.github.teocci.libstream.interfaces.ConnectCheckerRtsp;
import com.github.teocci.libstream.protocols.rtsp.RtspClient;

import java.nio.ByteBuffer;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Dec-14
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class RtspDisplay extends DisplayBase
{
    private RtspClient rtspClient;

    public RtspDisplay(Context context, ConnectCheckerRtsp connectCheckerRtsp)
    {
        super(context);
        rtspClient = new RtspClient(connectCheckerRtsp);
    }

    public void setProtocol(Protocol protocol)
    {
        rtspClient.setProtocol(protocol);
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
}
