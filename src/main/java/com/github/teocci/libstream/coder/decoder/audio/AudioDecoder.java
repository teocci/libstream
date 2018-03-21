package com.github.teocci.libstream.coder.decoder.audio;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import com.github.teocci.libstream.interfaces.audio.AudioDecoderListener;
import com.github.teocci.libstream.interfaces.audio.MicSinker;
import com.github.teocci.libstream.utils.LogHelper;

import java.io.IOException;
import java.nio.ByteBuffer;

import static android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM;
import static android.media.MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED;
import static android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED;
import static android.media.MediaCodec.INFO_TRY_AGAIN_LATER;
import static android.media.MediaExtractor.SEEK_TO_CLOSEST_SYNC;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public class AudioDecoder
{
    private static String TAG = LogHelper.makeLogTag(AudioDecoder.class);

    private final AudioDecoderListener audioDecoderListener;
    private MediaExtractor audioExtractor;
    private MediaCodec audioDecoder;
    private MediaCodec.BufferInfo audioInfo = new MediaCodec.BufferInfo();
    private boolean decoding;
    private Thread thread;
    private MicSinker micSinker;
    private MediaFormat audioFormat;
    private String mime = "";
    private int sampleRate;

    private byte[] pcmBuffer = new byte[4096];
    private byte[] pcmBufferMuted = new byte[11];

    private boolean stereo;
    private boolean loopMode = false;
    private boolean muted = false;

    public AudioDecoder(MicSinker micSinker,
                        AudioDecoderListener audioDecoderListener)
    {
        this.micSinker = micSinker;
        this.audioDecoderListener = audioDecoderListener;
    }

    public boolean initExtractor(String filePath) throws IOException
    {
        decoding = false;
        audioExtractor = new MediaExtractor();
        audioExtractor.setDataSource(filePath);
        for (int i = 0; i < audioExtractor.getTrackCount() && !mime.startsWith("audio/"); i++) {
            audioFormat = audioExtractor.getTrackFormat(i);
            mime = audioFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                audioExtractor.selectTrack(i);
            } else {
                audioFormat = null;
            }
        }
        if (audioFormat != null && mime.equals("audio/mp4a-latm")) {
            stereo = (audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) == 2);
            sampleRate = audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            return true;
        } else { // Audio decoder not supported
            mime = "";
            audioFormat = null;
            return false;
        }
    }

    public boolean prepareAudio()
    {
        try {
            audioDecoder = MediaCodec.createDecoderByType(mime);
            audioDecoder.configure(audioFormat, null, null, 0);
            return true;
        } catch (IOException e) {
            LogHelper.e(TAG, "Prepare decoder error:", e);
            return false;
        }
    }

    public void start()
    {
        decoding = true;
        audioDecoder.start();
        thread = new Thread(() -> {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
            decodeAudio();
        });
        thread.start();
    }

    public void stop()
    {
        decoding = false;
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException e) {
                thread.interrupt();
            }
            thread = null;
        }
        if (audioDecoder != null) {
            audioDecoder.stop();
            audioDecoder.release();
            audioDecoder = null;
        }
        if (audioExtractor != null) {
            audioExtractor.release();
            audioExtractor = null;
        }
    }

    private void decodeAudio()
    {
        ByteBuffer[] inputBuffers = audioDecoder.getInputBuffers();
        ByteBuffer[] outputBuffers = audioDecoder.getOutputBuffers();

        while (decoding) {
            int inIndex = audioDecoder.dequeueInputBuffer(-1);
            if (inIndex >= 0) {
                ByteBuffer buffer = inputBuffers[inIndex];
                int sampleSize = audioExtractor.readSampleData(buffer, 0);
                if (sampleSize < 0) {
                    audioDecoder.queueInputBuffer(inIndex, 0, 0, 0, BUFFER_FLAG_END_OF_STREAM);
                } else {
                    audioDecoder.queueInputBuffer(inIndex, 0, sampleSize, audioExtractor.getSampleTime(), 0);
                    audioExtractor.advance();
                }

                int outIndex = audioDecoder.dequeueOutputBuffer(audioInfo, 0);
                switch (outIndex) {
                    case INFO_OUTPUT_BUFFERS_CHANGED:
                        outputBuffers = audioDecoder.getOutputBuffers();
                        break;
                    case INFO_OUTPUT_FORMAT_CHANGED:
                        break;
                    case INFO_TRY_AGAIN_LATER:
                        break;
                    default:
                        ByteBuffer outBuffer = outputBuffers[outIndex];
                        // This buffer is PCM data
                        if (muted) {
                            outBuffer.get(pcmBufferMuted, 0, pcmBufferMuted.length);
                            micSinker.onPCMData(pcmBufferMuted, pcmBufferMuted.length);
                        } else {
                            outBuffer.get(pcmBuffer, 0, pcmBuffer.length);
                            micSinker.onPCMData(pcmBuffer, pcmBuffer.length);
                        }
                        audioDecoder.releaseOutputBuffer(outIndex, false);
                        break;
                }

                // All decoded frames have been rendered, we can stop playing now
                if ((audioInfo.flags & BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (loopMode) {
                        audioExtractor.seekTo(0, SEEK_TO_CLOSEST_SYNC);
                        audioDecoder.flush();
                    } else {
                        audioDecoderListener.onAudioDecoderFinished();
                    }
                }
            }
        }
    }

    public void setLoopMode(boolean loopMode)
    {
        this.loopMode = loopMode;
    }

    public void mute()
    {
        muted = true;
    }

    public void unMute()
    {
        muted = false;
    }

    public int getSampleRate()
    {
        return sampleRate;
    }

    public boolean isMuted()
    {
        return muted;
    }

    public boolean isStereo()
    {
        return stereo;
    }
}
