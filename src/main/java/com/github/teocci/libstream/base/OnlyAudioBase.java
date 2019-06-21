package com.github.teocci.libstream.base;

import android.media.MediaCodec;
import android.media.MediaFormat;

import com.github.teocci.libstream.coder.encoder.audio.AudioEncoder;
import com.github.teocci.libstream.input.audio.AudioQuality;
import com.github.teocci.libstream.input.audio.MicManager;
import com.github.teocci.libstream.interfaces.audio.AACSinker;
import com.github.teocci.libstream.interfaces.audio.MicSinker;
import com.github.teocci.libstream.utils.LogHelper;

import java.nio.ByteBuffer;

/**
 * Wrapper to stream only audio. It is under tests.
 * <p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public abstract class OnlyAudioBase implements AACSinker, MicSinker
{
    private static String TAG = LogHelper.makeLogTag(OnlyAudioBase.class);

    private MicManager micManager;
    private AudioEncoder audioEncoder;

    private boolean streaming = false;

    public OnlyAudioBase()
    {
        micManager = new MicManager(this);
        audioEncoder = new AudioEncoder(this);
    }

    @Override
    public void onAACData(ByteBuffer aacBuffer, MediaCodec.BufferInfo info)
    {
        if (streaming) sendAACData(aacBuffer, info);
    }

    @Override
    public void onPCMData(byte[] buffer, int size)
    {
        audioEncoder.onPCMData(buffer, size);
    }

    @Override
    public void onAudioFormat(MediaFormat mediaFormat)
    {
        //ignored because record is not implemented
    }

    /**
     * Prepares the audio using a default configuration in stereo with 44100 Hz sample rate, and 128 * 1024 bps bitrate
     *
     * @return true if success, false if you get a error (Normally because the encoder selected
     * doesn't support any configuration seated or your device hasn't a AAC encoder).
     */
    public boolean prepareAudio()
    {
        return prepareAudio(AudioQuality.DEFAULT, false, false);
    }

    public boolean prepareAudio(AudioQuality quality)
    {
        return prepareAudio(quality, true, true);
    }

    /**
     * Call this method before use @startStream. If not you will do a stream without audio.
     *
     * @param quality         represents the quality of the audio stream
     * @param echoCanceler    true enable echo canceler, false disable.
     * @param noiseSuppressor true enable noise suppressor, false  disable.
     * @return true if success, false if you get a error (Normally because the encoder selected
     * doesn't support any configuration seated or your device hasn't a AAC encoder).
     */
    public boolean prepareAudio(AudioQuality quality, boolean echoCanceler, boolean noiseSuppressor)
    {
        LogHelper.e(TAG, quality);
        micManager.config(quality, echoCanceler, noiseSuppressor);
        prepareAudioRtp(quality);

        return audioEncoder.prepare(quality);
    }

    protected abstract void startStreamRtp(String url);

    /**
     * Need be called after @prepareVideo or/and @prepareAudio.
     *
     * @param url of the stream like:
     *            protocol://ip:port/application/streamName
     *            <p>
     *            RTSP: rtsp://192.168.1.1:1935/live/pedroSG94
     *            RTSPS: rtsps://192.168.1.1:1935/live/pedroSG94
     *            RTMP: rtmp://192.168.1.1:1935/live/pedroSG94
     *            RTMPS: rtmps://192.168.1.1:1935/live/pedroSG94
     */
    public void startStream(String url)
    {
        streaming = true;
        audioEncoder.start();
        micManager.start();
        startStreamRtp(url);
    }

    protected abstract void stopStreamRtp();

    /**
     * Stop stream started with @startStream.
     */
    public void stopStream()
    {
        streaming = false;
        stopStreamRtp();
        micManager.stop();
        audioEncoder.stop();
    }

    /**
     * Mute microphone, can be called before, while and after stream.
     */
    public void disableAudio()
    {
        micManager.mute();
    }

    /**
     * Enable a muted microphone, can be called before, while and after stream.
     */
    public void enableAudio()
    {
        micManager.unMute();
    }


    // Boolean Methods

    /**
     * Get mute state of microphone.
     *
     * @return true if muted, false if enabled
     */
    public boolean isAudioMuted()
    {
        return micManager.isMuted();
    }

    /**
     * Get stream state.
     *
     * @return true if streaming, false if not streaming.
     */
    public boolean isStreaming()
    {
        return streaming;
    }


    // Abstract Methods

    /**
     * Basic auth developed to work with Wowza. No tested with other server
     *
     * @param user     auth.
     * @param password auth.
     */
    public abstract void setAuthorization(String user, String password);

    protected abstract void prepareAudioRtp(AudioQuality audioQuality);

    protected abstract void sendAACData(ByteBuffer aacBuffer, MediaCodec.BufferInfo info);
}
