package com.github.teocci.libstream.base;

import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Pair;
import android.view.SurfaceView;

import com.github.teocci.libstream.coder.encoder.audio.AudioEncoder;
import com.github.teocci.libstream.coder.encoder.video.VideoEncoder;
import com.github.teocci.libstream.input.audio.AudioQuality;
import com.github.teocci.libstream.input.audio.MicManager;
import com.github.teocci.libstream.input.video.Frame;
import com.github.teocci.libstream.input.video.VideoQuality;
import com.github.teocci.libstream.interfaces.audio.AACSinker;
import com.github.teocci.libstream.interfaces.audio.MicSinker;
import com.github.teocci.libstream.interfaces.video.CameraSinker;
import com.github.teocci.libstream.interfaces.video.EncoderSinker;
import com.github.teocci.libstream.utils.LogHelper;

import java.io.IOException;
import java.nio.ByteBuffer;

import static android.content.Context.MEDIA_PROJECTION_SERVICE;
import static com.github.teocci.libstream.enums.FormatVideoEncoder.SURFACE;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public abstract class DisplayBase implements MicSinker, AACSinker, CameraSinker, EncoderSinker
{
    private static String TAG = LogHelper.makeLogTag(DisplayBase.class);

    protected Context context;
    private MediaProjection mediaProjection;
    private MediaProjectionManager mediaProjectionManager;

    protected VideoEncoder videoEncoder;
    protected AudioEncoder audioEncoder;

    protected MicManager micManager;

    private boolean streaming;
    protected SurfaceView surfaceView;
    private boolean videoEnabled = true;

    //record
    private MediaMuxer mediaMuxer;

    private int videoTrack = -1;
    private int audioTrack = -1;

    private boolean recording = false;
    private boolean canRecord = false;

    private MediaFormat videoFormat;
    private MediaFormat audioFormat;

    private int dpi = 320;

    public DisplayBase(Context context)
    {
        this.context = context;
        mediaProjectionManager =
                ((MediaProjectionManager) context.getSystemService(MEDIA_PROJECTION_SERVICE));
        this.surfaceView = null;
        videoEncoder = new VideoEncoder(this);
        micManager = new MicManager(this);
        audioEncoder = new AudioEncoder(this);
        streaming = false;
    }

    @Override
    public void onAACData(ByteBuffer aacBuffer, MediaCodec.BufferInfo info)
    {
        if (recording && canRecord) {
            mediaMuxer.writeSampleData(audioTrack, aacBuffer, info);
        }
        getAacDataRtp(aacBuffer, info);
    }

    @Override
    public void onPSReady(Pair<ByteBuffer, ByteBuffer> psPair)
    {
        sendAVCInfo(psPair.first, psPair.second, null);
    }

    @Override
    public void onSpsPpsVpsReady(ByteBuffer sps, ByteBuffer pps, ByteBuffer vps)
    {
        sendAVCInfo(sps, pps, vps);
    }

    @Override
    public void onEncodedData(ByteBuffer videoBuffer, MediaCodec.BufferInfo info)
    {
        if (recording) {
            if (info.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) canRecord = true;
            if (canRecord) {
                mediaMuxer.writeSampleData(videoTrack, videoBuffer, info);
            }
        }
        getH264DataRtp(videoBuffer, info);
    }

    @Override
    public void onPCMData(byte[] buffer, int size)
    {
        audioEncoder.onPCMData(buffer, size);
    }

    @Override
    public void onYUVData(byte[] buffer)
    {
        videoEncoder.onYUVData(buffer);
    }

    @Override
    public void onYUVData(Frame frame)
    {
        videoEncoder.onYUVData(frame);
    }

    @Override
    public void onVideoFormat(MediaFormat mediaFormat)
    {
        videoFormat = mediaFormat;
    }

    @Override
    public void onAudioFormat(MediaFormat mediaFormat)
    {
        audioFormat = mediaFormat;
    }

    public abstract void setAuthorization(String user, String password);

    public boolean prepareVideo(
            int width,
            int height,
            int fps,
            int bitrate,
            boolean hardwareRotation,
            int rotation,
            int dpi
    )
    {
        this.dpi = dpi;
        int imageFormat = ImageFormat.NV21; //supported nv21 and yv12
        videoEncoder.setImageFormat(imageFormat);

        VideoQuality quality = new VideoQuality(width, height, fps, bitrate);

        boolean result = videoEncoder.prepare(quality, hardwareRotation, rotation, SURFACE);
        return result;
    }

    public boolean prepareAudio(AudioQuality quality, boolean echoCanceler, boolean noiseSuppressor)
    {
        micManager.config(quality.sampleRate, quality.channel, echoCanceler, noiseSuppressor);
        prepareAudioRtp(quality);
        return audioEncoder.prepare(quality);
    }

    public boolean prepareVideo()
    {
        return videoEncoder.prepare(VideoQuality.DEFAULT, true, 0, SURFACE);
    }

    public boolean prepareAudio()
    {
        micManager.config();
        return audioEncoder.prepare();
    }

    /*Need be called while stream*/
    public void startRecord(String path) throws IOException
    {
        if (streaming) {
            mediaMuxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            videoTrack = mediaMuxer.addTrack(videoFormat);
            audioTrack = mediaMuxer.addTrack(audioFormat);
            mediaMuxer.start();
            recording = true;
        } else {
            throw new IOException("Need be called while stream");
        }
    }

    public void stopRecord()
    {
        recording = false;
        canRecord = false;
        if (mediaMuxer != null) {
            mediaMuxer.stop();
            mediaMuxer.release();
            mediaMuxer = null;
        }
        videoTrack = -1;
        audioTrack = -1;
    }

    public Intent sendIntent()
    {
        return mediaProjectionManager.createScreenCaptureIntent();
    }

    public void startStream(String url, int resultCode, Intent data)
    {
        videoEncoder.start();
        audioEncoder.start();
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        mediaProjection.createVirtualDisplay("Stream Display", videoEncoder.getWidth(),
                videoEncoder.getHeight(), dpi, 0, videoEncoder.getInputSurface(), null, null);
        micManager.start();
        streaming = true;
        startStreamRtp(url);
    }

    public void stopStream()
    {
        micManager.stop();
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
        stopStreamRtp();
        videoEncoder.stop();
        audioEncoder.stop();
        streaming = false;
    }

    public void disableAudio()
    {
        micManager.mute();
    }

    public void enableAudio()
    {
        micManager.unMute();
    }

    public void disableVideo()
    {
        videoEncoder.startSendBlackImage();
        videoEnabled = false;
    }

    public void enableVideo()
    {
        videoEncoder.stopSendBlackImage();
        videoEnabled = true;
    }

    /**
     * need min API 19
     */
    public void setVideoBitrateOnFly(int bitrate)
    {
        if (Build.VERSION.SDK_INT >= 19) {
            videoEncoder.setVideoBitrateOnFly(bitrate);
        }
    }

    protected abstract void stopStreamRtp();

    protected abstract void startStreamRtp(String url);

    protected abstract void prepareAudioRtp(AudioQuality audioQuality);

    protected abstract void getAacDataRtp(ByteBuffer aacBuffer, MediaCodec.BufferInfo info);

    protected abstract void sendAVCInfo(ByteBuffer sps, ByteBuffer pps, ByteBuffer vps);

    protected abstract void getH264DataRtp(ByteBuffer h264Buffer, MediaCodec.BufferInfo info);


    public boolean isStreaming()
    {
        return streaming;
    }

    public boolean isAudioMuted()
    {
        return micManager.isMuted();
    }

    public boolean isVideoEnabled()
    {
        return videoEnabled;
    }
}

