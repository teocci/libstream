package com.github.teocci.libstream.input.audio;

import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.github.teocci.libstream.coder.encoder.audio.AudioSink;
import com.github.teocci.libstream.interfaces.audio.MicSinker;
import com.github.teocci.libstream.utils.LogHelper;

import static android.media.AudioFormat.CHANNEL_IN_MONO;
import static android.media.AudioFormat.CHANNEL_IN_STEREO;
import static android.media.AudioFormat.ENCODING_PCM_16BIT;
import static android.media.AudioRecord.getMinBufferSize;
import static com.github.teocci.libstream.input.audio.AudioQuality.MONO;
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
    public static final int DOUBLE_BUFFER_SIZE = BUFFER_SIZE * 2;

    private AudioRecord audioRecord;
    private MicSinker micSinker;

    private byte[] pcmBuffer = new byte[BUFFER_SIZE];
    private byte[] pcmBufferMuted = new byte[BUFFER_SIZE];

    private boolean running = false;
    private boolean created = false;
    private boolean muted = false;

    // Default parameters for microphone
//    private int sampleRate = 44100; //hz
//    private int channel = AudioFormat.CHANNEL_IN_STEREO;
    private AudioQuality quality = AudioQuality.DEFAULT;

    private int audioFormat = ENCODING_PCM_16BIT;

    private AudioPostProcessEffect audioPostProcessEffect;
    private Thread thread;

    public MicManager(MicSinker micSinker)
    {
        this.micSinker = micSinker;
    }

    /**
     * Create audio record
     */
    public void config()
    {
        config(quality.sampleRate, quality.channel, false, false);
    }

    /**
     * Create audio record
     */
    public void config(AudioQuality quality, boolean echoCanceler, boolean noiseSuppressor)
    {
        config(quality.sampleRate, quality.channel, echoCanceler, noiseSuppressor);
    }

    /**
     * Create audio record with params
     */
    public void config(int sampleRate, int channel, boolean echoCanceler, boolean noiseSuppressor)
    {
        quality.sampleRate = sampleRate;
        quality.channel = channel;
        int channelConfig = channel == MONO ? CHANNEL_IN_MONO : CHANNEL_IN_STEREO;

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

        created = true;

        String chl = channel == STEREO ? "Stereo" : "Mono";
        LogHelper.i(TAG, "Microphone created, " + sampleRate + "hz, " + chl);
    }

    /**
     * Start record and get data
     */
    public void start()
    {
        if (!isCreated()) {
            LogHelper.e(TAG, "Microphone no created, MicManager not enabled");
            return;
        }

        init();
        thread = new Thread(() -> {
            while (running && !Thread.interrupted()) {
                AudioSink audioSink = read();
                if (audioSink != null) {
                    micSinker.onPCMData(audioSink.getPCMBuffer(), audioSink.getSize());
                } else {
                    running = false;
                }
            }
        });
        thread.start();
    }

    private void init()
    {
        if (audioRecord != null) {
            audioRecord.startRecording();
            running = true;
            LogHelper.i(TAG, "Microphone started");
        } else {
            LogHelper.e(TAG, "Error starting, microphone was stopped or not created, " + "use config() before start()");
        }
    }

    /**
     * @return Object with size and PCM buffer data
     */
    private AudioSink read()
    {
        int size = audioRecord.read(pcmBuffer, 0, pcmBuffer.length);
        if (size <= 0) return null;

        return new AudioSink(muted ? pcmBufferMuted : pcmBuffer, size);
    }

    /**
     * Stop and release microphone
     */
    public void stop()
    {
        running = false;
        created = false;

        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(100);
            } catch (InterruptedException e) {
                thread.interrupt();
            }
            thread = null;
        }

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

    public void mute()
    {
        muted = true;
    }

    public void unMute()
    {
        muted = false;
    }


    // Setters

    public void setSampleRate(int sampleRate)
    {
        quality.sampleRate = sampleRate;
    }


    // Getters

    /**
     * Get PCM buffer size
     */
    private int getPCMBufferSize()
    {
        int pcmBufSize = getMinBufferSize(quality.sampleRate, quality.channel, ENCODING_PCM_16BIT) + DOUBLE_BUFFER_SIZE - 1;

        return pcmBufSize - (pcmBufSize % DOUBLE_BUFFER_SIZE);
    }

    public int getSampleRate()
    {
        return quality.sampleRate;
    }

    public int getAudioFormat()
    {
        return audioFormat;
    }

    public int getChannel()
    {
        return quality.channel;
    }


    // Boolean Methods

    public boolean isMuted()
    {
        return muted;
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