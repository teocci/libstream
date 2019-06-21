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
import static android.media.MediaCodecInfo.CodecProfileLevel.AACObjectLD;
import static android.media.MediaFormat.KEY_AAC_PROFILE;
import static android.media.MediaFormat.KEY_BIT_RATE;
import static android.media.MediaFormat.KEY_MAX_INPUT_SIZE;
import static com.github.teocci.libstream.utils.CodecUtil.MAX_INPUT_SIZE;
import static com.github.teocci.libstream.utils.Utils.minAPI21;
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

    private static final int AUDIO_PROFILE = AACObjectLC;

    private MediaCodec audioEncoder;
    private MediaCodec.BufferInfo audioInfo = new MediaCodec.BufferInfo();

    private AACSinker aacSinker;

    //    private long presentTimeUs;
//    private long frameIndex = 0;

    private long startPTS = 0;
    private long totalSamplesNum = 0;

    private boolean running;

    // default parameters for encoder
//    private String mime = "audio/mp4a-latm";
//    private int bitRate = 128 * 1024;  //in kbps
//    private int sampleRate = 44100; //in hz
//    private boolean isStereo = true;
    private AudioQuality quality = AudioQuality.DEFAULT;
    private final Object sync = new Object();

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
        synchronized (sync) {
            if (minAPI21()) {
                encodeDataAPI21(buffer, size);
            } else {
                encodeData(buffer, size);
            }
        }
    }


    /**
     * Prepare encoder with default parameters
     */
    public boolean prepare()
    {
        return prepare(quality);
    }

    /**
     * Prepare encoder with custom parameters
     */
    public boolean prepare(AudioQuality quality)
    {
        this.quality.sampleRate = quality.sampleRate;
        this.quality.bitRate = quality.bitRate;
        this.quality.channel = quality.channel;

        try {
            audioEncoder = MediaCodec.createEncoderByType(quality.mime);
            MediaFormat audioFormat = MediaFormat.createAudioFormat(
                    quality.mime,
                    quality.sampleRate,
                    quality.channel
            );
            audioFormat.setInteger(KEY_BIT_RATE, quality.bitRate);
            audioFormat.setInteger(KEY_MAX_INPUT_SIZE, MAX_INPUT_SIZE);
            // Low Complexity AAC
            audioFormat.setInteger(KEY_AAC_PROFILE, AUDIO_PROFILE);

            audioEncoder.configure(audioFormat, null, null, CONFIGURE_FLAG_ENCODE);
            running = false;
            return true;
        } catch (IOException | IllegalStateException e) {
            LogHelper.e(TAG, "AudioEncoder creation has failed.", e);
            e.printStackTrace();
            return false;
        }
    }

    public void start()
    {
        synchronized (sync) {
            if (audioEncoder == null) {
                LogHelper.e(TAG, "AudioEncoder need be prepared, AudioEncoder not enabled");
                return;
            }

//            presentTimeUs = System.nanoTime() / 1000;
            audioEncoder.start();
            running = true;
            LogHelper.i(TAG, "AudioEncoder started");
        }
    }

    public void stop()
    {
        synchronized (sync) {
            running = false;
            if (audioEncoder != null) {
                audioEncoder.flush();
                audioEncoder.stop();
                audioEncoder.release();
                audioEncoder = null;
            }
            LogHelper.i(TAG, "AudioEncoder stopped");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void encodeDataAPI21(byte[] data, int size)
    {
        try {
            int inBufferIndex = audioEncoder.dequeueInputBuffer(-1);
            if (inBufferIndex >= 0) {
                ByteBuffer inputBuffer = audioEncoder.getInputBuffer(inBufferIndex);
                if (inputBuffer != null) {
//                    long pts = System.nanoTime() / 1000 - presentTimeUs;
//                    long pts = System.nanoTime() / 1000;
//                    long pts = computePresentationTime(frameIndex);
                    long pts = System.nanoTime() / 1000L;
                    pts = getJitterFreePTS(pts, size / 2);
                    inputBuffer.put(data, 0, size);
                    audioEncoder.queueInputBuffer(inBufferIndex, 0, size, pts, 0);
                }
            }

            drainEncoderAPI21();
        } catch (IllegalStateException ie) {
            ie.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void drainEncoderAPI21()
    {
        for (; running; ) {
            int outBufferIndex = audioEncoder.dequeueOutputBuffer(audioInfo, 0);
            if (outBufferIndex == INFO_OUTPUT_FORMAT_CHANGED) {
                aacSinker.onAudioFormat(audioEncoder.getOutputFormat());
            } else if (outBufferIndex >= 0) {
                // This ByteBuffer is AAC
                ByteBuffer outputBuffer = audioEncoder.getOutputBuffer(outBufferIndex);
                if (outputBuffer != null) {
//                    presentTimeUs = audioInfo.presentationTimeUs;
//                    audioInfo.presentationTimeUs = computePresentationTime();
//                    audioInfo.presentationTimeUs = computePresentationTime(frameIndex);
                    aacSinker.onAACData(outputBuffer, audioInfo);
                    audioEncoder.releaseOutputBuffer(outBufferIndex, false);
//                    frameIndex++;
                }
            } else {
                break;
            }
        }
    }

    private void encodeData(byte[] data, int size)
    {
        ByteBuffer[] inputBuffers = audioEncoder.getInputBuffers();

        int inBufferIndex = audioEncoder.dequeueInputBuffer(-1);
        if (inBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inBufferIndex];
            if (inputBuffer != null) {
//                long pts = System.nanoTime() / 1000 - presentTimeUs;
//                long pts = System.nanoTime() / 1000;
//                long pts = computePresentationTime(frameIndex);
                long pts = System.nanoTime() / 1000L;
                pts = getJitterFreePTS(pts, size / 2);
                inputBuffer.clear();
                inputBuffer.put(data, 0, size);
                audioEncoder.queueInputBuffer(inBufferIndex, 0, size, pts, 0);
            }
        }

        drainEncoder();
    }

    private void drainEncoder()
    {
        ByteBuffer[] outputBuffers = audioEncoder.getOutputBuffers();

        for (; running; ) {
            int outBufferIndex = audioEncoder.dequeueOutputBuffer(audioInfo, 0);
            if (outBufferIndex == INFO_OUTPUT_FORMAT_CHANGED) {
                aacSinker.onAudioFormat(audioEncoder.getOutputFormat());
            } else if (outBufferIndex >= 0) {
                // This ByteBuffer is AAC
                ByteBuffer outputBuffer = outputBuffers[outBufferIndex];
                if (outputBuffer != null) {
//                    presentTimeUs = audioInfo.presentationTimeUs;
//                    audioInfo.presentationTimeUs = computePresentationTime();
//                    audioInfo.presentationTimeUs = computePresentationTime(frameIndex);
                    aacSinker.onAACData(outputBuffer, audioInfo);
                    audioEncoder.releaseOutputBuffer(outBufferIndex, false);
//                    frameIndex++;
                }
            } else {
                break;
            }
        }
    }

//    private long computePresentationTime()
//    {
//        return System.nanoTime() / 1000 - presentTimeUs;
//    }

    private long computePresentationTime(long frameIndex)
    {
        return 132 + frameIndex * 1_000_000L / quality.sampleRate;
    }

    /**
     * Ensures that each audio pts differs by a constant amount from the previous one.
     *
     * @param pts        presentation timestamp in us
     * @param bufferSize the number of samples of the buffer's frame
     * @return corrected presentation timestamp in us
     */
    private long getJitterFreePTS(long pts, long bufferSize)
    {
        long correctedPts;
        long bufferDuration = 1_000_000L * bufferSize / quality.sampleRate;
        // accounts for the delay of acquiring the audio buffer
        pts -= bufferDuration;
        if (totalSamplesNum == 0) {
            startPTS = pts;
        }
        correctedPts = startPTS + (1_000_000L * totalSamplesNum / quality.sampleRate);
        if (pts - correctedPts >= 2 * bufferDuration) {
            // reset
            startPTS = pts;
            totalSamplesNum = 0;
            correctedPts = startPTS;
        }
        totalSamplesNum += bufferSize;

        return correctedPts;
    }

    public static String createBody(int trackAudio, int port, AudioQuality quality)
    {
        // 00000000 00000010 = 0x02
        // 00000000 00011111 = 0x1F
        // 00010000 00000000 = (2 & 0x1F) << 11 | Audio profile

        // 00000000 00000100 = 0x04
        // 00001111 = 0x0F
        // 00000010 00000000 = (4 & 0x1F) << 7 | Sample rate index

        // 00000000 00000010 = 0x02
        // 00001111 = 0x0F
        // 00000010 00010000 = (4 & 0x1F) << 3 | Channel Stereo


        // 00010010 00010000 = 1210
        int config = (AUDIO_PROFILE & 0x1F) << 11 | (quality.getSampleRateIndex() & 0x0F) << 7 | (quality.channel & 0x0F) << 3;
        return "m=audio " + port + " RTP/AVP " + PAYLOAD_TYPE + "\r\n" +
                "a=rtpmap:" + PAYLOAD_TYPE + " mpeg4-generic/" + quality.sampleRate + "/" + quality.channel + "\r\n" +
                "a=fmtp:" + PAYLOAD_TYPE + " streamtype=5; profile-level-id=15; mode=AAC-hbr; " +
                "config=" + Integer.toHexString(config) + "; SizeLength=13; IndexLength=3; IndexDeltaLength=3;\r\n" +
                "a=control:trackID=" + trackAudio + "\r\n";
    }


    public void setSampleRate(int sampleRate)
    {
        quality.sampleRate = sampleRate;
    }

    public boolean isRunning()
    {
        return running;
    }
}
