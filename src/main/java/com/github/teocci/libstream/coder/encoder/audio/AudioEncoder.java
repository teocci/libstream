package com.github.teocci.libstream.coder.encoder.audio;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.github.teocci.libstream.input.audio.AudioQuality;
import com.github.teocci.libstream.interfaces.audio.AACSinker;
import com.github.teocci.libstream.interfaces.audio.MicSinker;
import com.github.teocci.libstream.utils.LogHelper;

import java.io.IOException;
import java.nio.ByteBuffer;

import static android.media.MediaCodec.CONFIGURE_FLAG_ENCODE;
import static android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED;
import static android.media.MediaCodecInfo.CodecProfileLevel.AACObjectLC;
import static android.media.MediaFormat.KEY_AAC_PROFILE;
import static android.media.MediaFormat.KEY_BIT_RATE;
import static android.media.MediaFormat.KEY_MAX_INPUT_SIZE;
import static com.github.teocci.libstream.utils.rtsp.RtpConstants.PAYLOAD_TYPE;

/**
 * Encode PCM audio data to ACC and return in a callback
 * <p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public class AudioEncoder implements MicSinker
{
    private static String TAG = LogHelper.makeLogTag(AudioEncoder.class);

    /**
     * supported sampleRates.
     **/
    private static final int[] AUDIO_SAMPLING_RATES = {
            96000, // 0
            88200, // 1
            64000, // 2
            48000, // 3
            44100, // 4
            32000, // 5
            24000, // 6
            22050, // 7
            16000, // 8
            12000, // 9
            11025, // 10
            8000,  // 11
            7350,  // 12
            -1,   // 13
            -1,   // 14
            -1,   // 15
    };

    private MediaCodec audioEncoder;
    private MediaCodec.BufferInfo audioInfo = new MediaCodec.BufferInfo();

    private AACSinker aacSinker;

    private long presentTimeUs;
    private boolean running;

    // default parameters for encoder
//    private String mime = "audio/mp4a-latm";
//    private int bitRate = 128 * 1024;  //in kbps
//    private int sampleRate = 44100; //in hz
//    private boolean isStereo = true;
    private AudioQuality quality = AudioQuality.DEFAULT;

    public AudioEncoder(AACSinker aacSinker)
    {
        this.aacSinker = aacSinker;
    }

    /**
     * Set custom PCM data.
     * Use it after prepare(int sampleRate, int channel).
     * Used too with microphone.
     *
     * @param buffer PCM buffer
     * @param size   Min PCM buffer size
     */
    @Override
    public void onPCMData(final byte[] buffer, final int size)
    {
        if (Build.VERSION.SDK_INT >= 21) {
            encodeDataAPI21(buffer, size);
        } else {
            encodeData(buffer, size);
        }
    }

    public void setSampleRate(int sampleRate)
    {
        quality.sampleRate = sampleRate;
    }

    /**
     * Prepare encoder with custom parameters
     */
    public boolean prepare(AudioQuality quality)
    {
        this.quality.sampleRate = quality.sampleRate;
        this.quality.bitRate = quality.bitRate;
        this.quality.channel = quality.channel;

        return prepare();
    }

    /**
     * Prepare encoder with default parameters
     */
    public boolean prepare()
    {
        try {
            audioEncoder = MediaCodec.createEncoderByType(quality.mime);
            MediaFormat audioFormat = MediaFormat.createAudioFormat(
                    quality.mime,
                    quality.sampleRate,
                    quality.channel
            );
            audioFormat.setInteger(KEY_BIT_RATE, quality.bitRate);
            audioFormat.setInteger(KEY_MAX_INPUT_SIZE, 0);
            audioFormat.setInteger(KEY_AAC_PROFILE, AACObjectLC);

            audioEncoder.configure(audioFormat, null, null, CONFIGURE_FLAG_ENCODE);
            running = false;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void start()
    {
        if (audioEncoder != null) {
            presentTimeUs = System.nanoTime() / 1000;
            audioEncoder.start();
            running = true;
            LogHelper.i(TAG, "AudioEncoder started");
        } else {
            LogHelper.e(TAG, "AudioEncoder need be prepared, AudioEncoder not enabled");
        }
    }

    public void stop()
    {
        running = false;
        if (audioEncoder != null) {
            audioEncoder.stop();
            audioEncoder.release();
            audioEncoder = null;
        }
        LogHelper.i(TAG, "AudioEncoder stopped");
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void encodeDataAPI21(byte[] data, int size)
    {
        try {
            int inBufferIndex = audioEncoder.dequeueInputBuffer(-1);
            if (inBufferIndex >= 0) {
                ByteBuffer bb = audioEncoder.getInputBuffer(inBufferIndex);
                if (bb != null) {
                    bb.put(data, 0, size);
                }
                long pts = System.nanoTime() / 1000 - presentTimeUs;
                audioEncoder.queueInputBuffer(inBufferIndex, 0, size, pts, 0);
            }

            for (; ; ) {
                int outBufferIndex = audioEncoder.dequeueOutputBuffer(audioInfo, 0);
                if (outBufferIndex == INFO_OUTPUT_FORMAT_CHANGED) {
                    aacSinker.onAudioFormat(audioEncoder.getOutputFormat());
                } else if (outBufferIndex >= 0) {
                    // This ByteBuffer is AAC
                    ByteBuffer bb = audioEncoder.getOutputBuffer(outBufferIndex);
                    aacSinker.onAACData(bb, audioInfo);
                    audioEncoder.releaseOutputBuffer(outBufferIndex, false);
                } else {
                    break;
                }
            }
        } catch (IllegalStateException ie) {
            ie.printStackTrace();
        }
    }

    private void encodeData(byte[] data, int size)
    {
        ByteBuffer[] inputBuffers = audioEncoder.getInputBuffers();
        ByteBuffer[] outputBuffers = audioEncoder.getOutputBuffers();

        int inBufferIndex = audioEncoder.dequeueInputBuffer(-1);
        if (inBufferIndex >= 0) {
            ByteBuffer bb = inputBuffers[inBufferIndex];
            bb.clear();
            bb.put(data, 0, size);
            long pts = System.nanoTime() / 1000 - presentTimeUs;
            audioEncoder.queueInputBuffer(inBufferIndex, 0, size, pts, 0);
        }

        for (; ; ) {
            int outBufferIndex = audioEncoder.dequeueOutputBuffer(audioInfo, 0);
            if (outBufferIndex == INFO_OUTPUT_FORMAT_CHANGED) {
                aacSinker.onAudioFormat(audioEncoder.getOutputFormat());
            } else if (outBufferIndex >= 0) {
                // This ByteBuffer is AAC
                ByteBuffer bb = outputBuffers[outBufferIndex];
                aacSinker.onAACData(bb, audioInfo);
                audioEncoder.releaseOutputBuffer(outBufferIndex, false);
            } else {
                break;
            }
        }
    }

    public static String createBody(int trackAudio, int port, AudioQuality quality)
    {
        int samplingIndex = -1;
        int index = 0;
        for (int sampleRate : AUDIO_SAMPLING_RATES) {
            if (sampleRate == quality.sampleRate) {
                samplingIndex = index;
                break;
            }
            index++;
        }

        // 00000000 00000010 = 0x02
        // 00000000 00011111 = 0x1F
        // 00010000 00000000 = (2 & 0x1F) << 11
        // 00001111 = 0x0F

        // 00010010 00010000 = 1210
        int config = (2 & 0x1F) << 11 | (samplingIndex & 0x0F) << 7 | (quality.channel & 0x0F) << 3;
        return "m=audio " + port + " RTP/AVP " + PAYLOAD_TYPE + "\r\n" +
                "a=rtpmap:" + PAYLOAD_TYPE + " mpeg4-generic/" + quality.sampleRate + "/" + quality.channel + "\r\n" +
                "a=fmtp:" + PAYLOAD_TYPE + " streamtype=5; profile-level-id=15; mode=AAC-hbr; " +
                "config=" + Integer.toHexString(config) + "; SizeLength=13; IndexLength=3; IndexDeltaLength=3;\r\n" +
                "a=control:trackID=" + trackAudio + "\r\n";
    }

    public boolean isRunning()
    {
        return running;
    }
}
