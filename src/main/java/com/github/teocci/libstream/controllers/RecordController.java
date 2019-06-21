package com.github.teocci.libstream.controllers;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.github.teocci.libstream.enums.RecordStatus;
import com.github.teocci.libstream.interfaces.RecordStatusListener;
import com.github.teocci.libstream.utils.LogHelper;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.github.teocci.libstream.enums.RecordStatus.PAUSED;
import static com.github.teocci.libstream.enums.RecordStatus.RECORDING;
import static com.github.teocci.libstream.enums.RecordStatus.RESUMED;
import static com.github.teocci.libstream.enums.RecordStatus.STARTED;
import static com.github.teocci.libstream.enums.RecordStatus.STOPPED;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2019-May-29
 */
public class RecordController
{
    private final static String TAG = LogHelper.makeLogTag(RecordController.class);

    private RecordStatus status = STOPPED;

    private MediaMuxer mediaMuxer;
    private MediaFormat videoFormat, audioFormat;

    private int videoTrack = -1;
    private int audioTrack = -1;

    private RecordStatusListener listener;

    private long pauseMoment = 0;
    private long pauseTime = 0;

    private MediaCodec.BufferInfo videoInfo = new MediaCodec.BufferInfo();
    private MediaCodec.BufferInfo audioInfo = new MediaCodec.BufferInfo();

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void startRecord(String path, RecordStatusListener listener) throws IOException
    {
        this.listener = listener;

        mediaMuxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        status = STARTED;
        updateStatus();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void recordVideo(ByteBuffer videoBuffer, MediaCodec.BufferInfo videoInfo)
    {
        if (hasStarted(videoInfo) && videoFormat != null) {
            videoTrack = mediaMuxer.addTrack(videoFormat);
            mediaMuxer.start();
            status = RECORDING;

            if (listener != null) listener.onStatusChange(status);
        } else if (status == RESUMED && videoInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
            status = RECORDING;
            updateStatus();
        }
        if (isRecording()) {
            updateFormat(this.videoInfo, videoInfo);
            mediaMuxer.writeSampleData(videoTrack, videoBuffer, this.videoInfo);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void recordAudio(ByteBuffer audioBuffer, MediaCodec.BufferInfo audioInfo)
    {
        if (hasStarted(audioInfo) && audioFormat != null) {
            audioTrack = mediaMuxer.addTrack(audioFormat);
            mediaMuxer.start();
            status = RECORDING;

            if (listener != null) listener.onStatusChange(status);
        } else if (status == RESUMED && audioInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
            status = RECORDING;
            updateStatus();
        }
        if (isRecording()) {
            updateFormat(this.audioInfo, audioInfo);
            mediaMuxer.writeSampleData(audioTrack, audioBuffer, this.audioInfo);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void stopRecord()
    {
        if (mediaMuxer != null) {
            try {
                mediaMuxer.stop();
                mediaMuxer.release();
            } catch (Exception ignored) {}
        }

        mediaMuxer = null;
        videoTrack = -1;
        audioTrack = -1;
        pauseMoment = 0;
        pauseTime = 0;

        status = STOPPED;
        updateStatus();
    }

    public void resetFormats()
    {
        videoFormat = null;
        audioFormat = null;
    }

    public void pauseRecord()
    {
        if (status == RECORDING) {
            pauseMoment = System.nanoTime() / 1000;
            status = PAUSED;
            updateStatus();
        }
    }

    public void resumeRecord()
    {
        if (status == PAUSED) {
            pauseTime += System.nanoTime() / 1000 - pauseMoment;
            status = RESUMED;
            updateStatus();
        }
    }

    private void updateStatus()
    {
        if (listener != null) listener.onStatusChange(status);
    }

    /**
     * We can't reuse info because could produce stream issues
     *
     * @param newInfo new buffer information
     * @param oldInfo old buffer information
     */
    private void updateFormat(MediaCodec.BufferInfo newInfo, MediaCodec.BufferInfo oldInfo)
    {
        newInfo.flags = oldInfo.flags;
        newInfo.offset = oldInfo.offset;
        newInfo.size = oldInfo.size;
        newInfo.presentationTimeUs = oldInfo.presentationTimeUs - pauseTime;
    }

    public void setVideoFormat(MediaFormat videoFormat)
    {
        this.videoFormat = videoFormat;
    }

    public void setAudioFormat(MediaFormat audioFormat)
    {
        this.audioFormat = audioFormat;
    }

    public RecordStatus getStatus()
    {
        return status;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean hasStarted(MediaCodec.BufferInfo videoInfo)
    {
        return status == STARTED && videoInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME;
    }


    // Boolean methods

    public boolean isRunning()
    {
        return status == STARTED || status == RECORDING || status == RESUMED || status == PAUSED;
    }

    public boolean isRecording()
    {
        return status == RECORDING;
    }
}
