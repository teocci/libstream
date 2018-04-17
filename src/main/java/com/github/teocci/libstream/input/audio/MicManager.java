package com.github.teocci.libstream.input.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.github.teocci.libstream.coder.encoder.audio.AudioSink;
import com.github.teocci.libstream.interfaces.audio.MicSinker;
import com.github.teocci.libstream.utils.LogHelper;

import static android.media.AudioFormat.CHANNEL_IN_MONO;
import static android.media.AudioFormat.CHANNEL_IN_STEREO;
import static com.github.teocci.libstream.input.audio.AudioQuality.STEREO;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */

public class MicManager
{
    private static String TAG = LogHelper.makeLogTag(MicManager.class);

    public static final int BUFFER_SIZE = 4096;

    private AudioRecord audioRecord;
    private MicSinker micSinker;

    private byte[] pcmBuffer = new byte[BUFFER_SIZE];
    private byte[] pcmBufferMuted = new byte[11];

    private boolean running = false;
    private boolean created = false;

    // Default parameters for microphone
//    private int sampling = 44100; //hz
//    private int channel = AudioFormat.CHANNEL_IN_STEREO;
    private AudioQuality quality = AudioQuality.DEFAULT;

    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    private boolean muted = false;

    private AudioPostProcessEffect audioPostProcessEffect;

    public MicManager(MicSinker micSinker)
    {
        this.micSinker = micSinker;
    }

    /**
     * Create audio record
     */
    public void config()
    {
        config(quality.sampling, quality.channel, false, false);
        String chl = quality.channel == STEREO ? "Stereo" : "Mono";
        LogHelper.i(TAG, "Microphone created, " + quality.sampling + "hz, " + chl);
    }

    /**
     * Create audio record with params
     */
    public void config(int sampleRate, int channel, boolean echoCanceler, boolean noiseSuppressor)
    {
        quality.sampling = sampleRate;
        quality.channel = channel;
        int channelConfig = channel == STEREO ? CHANNEL_IN_STEREO : CHANNEL_IN_MONO;

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.DEFAULT,
                sampleRate,
                channelConfig,
                audioFormat,
                getPCMBufferSize() * 4
        );

        audioPostProcessEffect = new AudioPostProcessEffect(audioRecord.getAudioSessionId());
        if (echoCanceler) audioPostProcessEffect.enableEchoCanceler();
        if (noiseSuppressor) audioPostProcessEffect.enableNoiseSuppressor();

        String chl = channel == STEREO ? "Stereo" : "Mono";
        LogHelper.i(TAG, "Microphone created, " + sampleRate + "hz, " + chl);
        created = true;
    }

    /**
     * Start record and get data
     */
    public void start()
    {
        if (isCreated()) {
            init();
            new Thread(() -> {
                while (running && !Thread.interrupted()) {
                    AudioSink audioSink = read();
                    if (audioSink != null) {
                        micSinker.onPCMData(audioSink.getPCMBuffer(), audioSink.getSize());
                    } else {
                        running = false;
                    }
                }
            }).start();
        } else {
            LogHelper.e(TAG, "Microphone no created, MicManager not enabled");
        }
    }

    private void init()
    {
        if (audioRecord != null) {
            audioRecord.startRecording();
            running = true;
            LogHelper.i(TAG, "Microphone started");
        } else {
            LogHelper.e(TAG, "Error starting, microphone was stopped or not created, "
                    + "use config() before start()");
        }
    }

    public void mute()
    {
        muted = true;
    }

    public void unMute()
    {
        muted = false;
    }

    public boolean isMuted()
    {
        return muted;
    }

    /**
     * @return Object with size and PCM buffer data
     */
    private AudioSink read()
    {
        int size;
        if (muted) {
            size = audioRecord.read(pcmBufferMuted, 0, pcmBufferMuted.length);
        } else {
            size = audioRecord.read(pcmBuffer, 0, pcmBuffer.length);
        }
        if (size <= 0) {
            return null;
        }
        return new AudioSink(pcmBuffer, size);
    }

    /**
     * Stop and release microphone
     */
    public void stop()
    {
        running = false;
        created = false;
        if (audioRecord != null) {
            audioRecord.setRecordPositionUpdateListener(null);
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (audioPostProcessEffect != null) {
            audioPostProcessEffect.releaseEchoCanceler();
            audioPostProcessEffect.releaseNoiseSuppressor();
        }

        LogHelper.i(TAG, "Microphone stopped");
    }

    /**
     * Get PCM buffer size
     */
    private int getPCMBufferSize()
    {
        int pcmBufSize = AudioRecord.getMinBufferSize(
                quality.sampling,
                quality.channel,
                AudioFormat.ENCODING_PCM_16BIT
        ) + 8191;
        return pcmBufSize - (pcmBufSize % 8192);
    }

    public int getSampleRate()
    {
        return quality.sampling;
    }

    public void setSampleRate(int sampleRate)
    {
        quality.sampling = sampleRate;
    }

    public int getAudioFormat()
    {
        return audioFormat;
    }

    public int getChannel()
    {
        return quality.channel;
    }

    public boolean isRunning()
    {
        return running;
    }

    public boolean isCreated()
    {
        return created;
    }
}