package com.github.teocci.libstream.base;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Pair;
import android.view.SurfaceView;
import android.view.TextureView;

import com.github.teocci.libstream.coder.encoder.audio.AudioEncoder;
import com.github.teocci.libstream.coder.encoder.video.VideoEncoder;
import com.github.teocci.libstream.enums.CameraFacing;
import com.github.teocci.libstream.enums.ColorEffect;
import com.github.teocci.libstream.exceptions.CameraInUseException;
import com.github.teocci.libstream.input.audio.AudioQuality;
import com.github.teocci.libstream.input.audio.MicManager;
import com.github.teocci.libstream.input.video.CamManager;
import com.github.teocci.libstream.input.video.VideoQuality;
import com.github.teocci.libstream.interfaces.audio.AACSinker;
import com.github.teocci.libstream.interfaces.audio.MicSinker;
import com.github.teocci.libstream.interfaces.video.CameraSinker;
import com.github.teocci.libstream.interfaces.video.H264Sinker;
import com.github.teocci.libstream.utils.LogHelper;
import com.github.teocci.libstream.utils.gl.GifStreamObject;
import com.github.teocci.libstream.utils.gl.ImageStreamObject;
import com.github.teocci.libstream.utils.gl.Position;
import com.github.teocci.libstream.utils.gl.TextStreamObject;
import com.github.teocci.libstream.view.OpenGlView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
import static com.github.teocci.libstream.enums.VideoEncodingFormat.SURFACE;
import static com.github.teocci.libstream.enums.VideoEncodingFormat.YUV420DYNAMICAL;
import static com.github.teocci.libstream.utils.Utils.minAPI18;
import static com.github.teocci.libstream.utils.Utils.minAPI19;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public abstract class EncoderBase implements MicSinker, AACSinker, CameraSinker, H264Sinker
{
    private static String TAG = LogHelper.makeLogTag(EncoderBase.class);

    private Context context;
    private OpenGlView openGlView;

    // Record
    private MediaMuxer mediaMuxer;
    private MediaFormat videoFormat;
    private MediaFormat audioFormat;

    protected CamManager camManager;
    protected MicManager micManager;

    protected VideoEncoder videoEncoder;
    protected AudioEncoder audioEncoder;

    private int videoTrack = -1;
    private int audioTrack = -1;

    private boolean streaming;

    private boolean videoEnabled = true;
    private boolean recording = false;
    private boolean canRecord = false;
    private boolean onPreview = false;

    public EncoderBase(SurfaceView surfaceView)
    {
        camManager = new CamManager(surfaceView, this);
        videoEncoder = new VideoEncoder(this);
        micManager = new MicManager(this);
        audioEncoder = new AudioEncoder(this);
        streaming = false;
    }

    public EncoderBase(TextureView textureView)
    {
        camManager = new CamManager(textureView, this);
        videoEncoder = new VideoEncoder(this);
        audioEncoder = new AudioEncoder(this);

        micManager = new MicManager(this);
        streaming = false;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public EncoderBase(OpenGlView openGlView)
    {
        this.openGlView = openGlView;
        this.context = openGlView.getContext();

        this.videoEncoder = new VideoEncoder(this);
        this.audioEncoder = new AudioEncoder(this);

        this.micManager = new MicManager(this);

        this.streaming = false;
    }

    @Override
    public void onPSReady(Pair<ByteBuffer, ByteBuffer> psPair)
    {
        LogHelper.e(TAG, "onPSReady()");
        setPSPair(psPair.first, psPair.second);
    }

    @Override
    public void onAACData(ByteBuffer aacBuffer, MediaCodec.BufferInfo info)
    {
        if (minAPI19() && recording && audioTrack != -1 && canRecord) {
            mediaMuxer.writeSampleData(audioTrack, aacBuffer, info);
        }
        sendAACData(aacBuffer, info);
    }

    @Override
    public void onH264Data(ByteBuffer h264Buffer, MediaCodec.BufferInfo info)
    {
        if (minAPI19() && recording && videoTrack != -1) {
            if (info.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) canRecord = true;
            if (canRecord) {
                mediaMuxer.writeSampleData(videoTrack, h264Buffer, info);
            }
        }
        sendH264Data(h264Buffer, info);
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
    public void onVideoFormat(MediaFormat mediaFormat)
    {
        videoFormat = mediaFormat;
    }

    @Override
    public void onAudioFormat(MediaFormat mediaFormat)
    {
        audioFormat = mediaFormat;
    }


    public boolean prepareVideo(VideoQuality quality)
    {
        return prepareVideo(quality, false, 0);
    }

    public boolean prepareVideo(VideoQuality quality, boolean hardwareRotation, int rotation)
    {
        LogHelper.e(TAG, "prepareVideo called");
        if (onPreview) {
            stopPreview();
            onPreview = true;
        }
        // Supported nv21 and yv12
        int imageFormat = ImageFormat.NV21;
        if (openGlView == null) {
            camManager.prepare(quality, imageFormat);
            videoEncoder.setImageFormat(imageFormat);
            return videoEncoder.prepare(quality, rotation, hardwareRotation, YUV420DYNAMICAL);
        } else {
            return videoEncoder.prepare(quality, rotation, hardwareRotation, SURFACE);
        }
    }

    public boolean prepareVideo()
    {
        if (onPreview) {
            stopPreview();
            onPreview = true;
        }
        if (openGlView == null) {
            camManager.prepare();
            return videoEncoder.prepare();
        } else {
            int orientation = 0;
            if (context.getResources().getConfiguration().orientation == 1) {
                orientation = 90;
            }

            return videoEncoder.prepare(VideoQuality.DEFAULT, orientation, false, SURFACE);
        }
    }

    public boolean prepareAudio(AudioQuality quality)
    {
        return prepareAudio(quality, true, true);
    }

    public boolean prepareAudio(AudioQuality quality, boolean echoCanceler, boolean noiseSuppressor)
    {
        LogHelper.e(TAG, quality);
        micManager.config(quality.sampleRate, quality.channel, echoCanceler, noiseSuppressor);
        prepareAudioRtp(quality);
        return audioEncoder.prepare(quality);
    }

    public boolean prepareAudio()
    {
        micManager.config();
        return audioEncoder.prepare();
    }

    /**
     * Need be called while stream
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void startRecord(String path) throws IOException
    {
        if (streaming) {
            mediaMuxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            if (videoFormat != null) {
                videoTrack = mediaMuxer.addTrack(videoFormat);
            }
            if (audioFormat != null) {
                audioTrack = mediaMuxer.addTrack(audioFormat);
            }
            mediaMuxer.start();
            recording = true;
        } else {
            throw new IOException("Need be called while stream");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
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

    public void startPreview(@CameraFacing int cameraFacing, int width, int height)
    {
        if (!isStreaming() && !onPreview) {
            if (openGlView != null && minAPI18()) {
                openGlView.startGLThread();
                camManager = new CamManager(openGlView.getSurfaceTexture(), openGlView.getContext());
            }
            camManager.prepare();
            if (width == 0 || height == 0) {
                camManager.start(cameraFacing);
            } else {
                camManager.start(cameraFacing, width, height);
            }
            onPreview = true;
        } else {
            LogHelper.e(TAG, "Streaming or preview started, ignored");
        }
    }

    public void startPreview(@CameraFacing int cameraFacing)
    {
        startPreview(cameraFacing, 0, 0);
    }

    public void startPreview(int width, int height)
    {
        startPreview(CAMERA_FACING_BACK, width, height);
    }

    public void startPreview()
    {
        startPreview(CAMERA_FACING_BACK);
    }

    public void stopPreview()
    {
        if (!isStreaming() && onPreview) {
            if (openGlView != null && minAPI18()) {
                openGlView.stopGlThread();
            }
            camManager.stop();
            onPreview = false;
        } else {
            LogHelper.e(TAG, "Streaming or preview stopped, ignored");
        }
    }

    public void setPreviewOrientation(int orientation)
    {
        camManager.setPreviewOrientation(orientation);
    }

    public void startStream()
    {
        prepareCamera();
        startProcessing();
        startRtspStream();
    }

    public void startStream(String url)
    {
        prepareCamera();
        startProcessing();
        startRtspStream(url);
    }


    private void prepareCamera()
    {
        if (openGlView != null && minAPI18()) {
            if (videoEncoder.getRotation() == 90 || videoEncoder.getRotation() == 270) {
                openGlView.setEncoderSize(videoEncoder.getHeight(), videoEncoder.getWidth());
            } else {
                openGlView.setEncoderSize(videoEncoder.getWidth(), videoEncoder.getHeight());
            }

            openGlView.startGLThread();
            openGlView.addMediaCodecSurface(videoEncoder.getInputSurface());
            camManager = new CamManager(openGlView.getSurfaceTexture(), openGlView.getContext());
            camManager.prepare(
                    videoEncoder.getWidth(),
                    videoEncoder.getHeight(),
                    videoEncoder.getFps(),
                    ImageFormat.NV21
            );
        }
    }

    private void startProcessing()
    {
        videoEncoder.start();
        audioEncoder.start();

        camManager.start();
        micManager.start();

        streaming = true;
        onPreview = true;
    }

    public void stopStream()
    {
        stopProcessing();
        stopRtspStream();
    }

    private void stopProcessing()
    {
        micManager.stop();

        videoEncoder.stop();
        audioEncoder.stop();

        if (openGlView != null && minAPI18()) {
            openGlView.stopGlThread();
            openGlView.removeMediaCodecSurface();
        }

        streaming = false;
    }

    public List<String> getBackResolutionsString()
    {
        List<Camera.Size> list = camManager.getPreviewSizeBack();
        return getResolutions(list);
    }

    public List<Camera.Size> getBackResolutions()
    {
        return camManager != null ? camManager.getPreviewSizeBack() : null;
    }

    public List<String> getFrontResolutions()
    {
        List<Camera.Size> list = camManager.getPreviewSizeFront();
        return getResolutions(list);
    }

    public List<String> getResolutions(List<Camera.Size> list)
    {
        List<String> resolutions = new ArrayList<>();
        for (Camera.Size size : list) {
            resolutions.add(size.width + "X" + size.height);
        }

        return resolutions;
    }

    public int getBackCamResolutionIndex(int width, int height)
    {
        int index = 0;

        List<Camera.Size> sizes = camManager.getPreviewSizeBack();
        if (sizes == null) return -1;

        LogHelper.e(TAG, "Arr: " + Arrays.toString(sizes.toArray()));

        for (Camera.Size size : sizes) {
            if (size.width == width && size.height == height) {
                return index;
            }
            index++;
        }

        return -1;
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

    public void switchCamera() throws CameraInUseException
    {
        if (isStreaming() || onPreview) {
            camManager.switchCamera();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void setGifStreamObject(GifStreamObject gifStreamObject) throws RuntimeException
    {
        if (openGlView != null) {
            openGlView.setGif(gifStreamObject);
        } else {
            throw new RuntimeException("You must use OpenGlView in the constructor to set a gif");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void setImageStreamObject(ImageStreamObject imageStreamObject) throws RuntimeException
    {
        if (openGlView != null) {
            openGlView.setImage(imageStreamObject);
        } else {
            throw new RuntimeException("You must use OpenGlView in the constructor to set an image");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void setTextStreamObject(TextStreamObject textStreamObject) throws RuntimeException
    {
        if (openGlView != null) {
            openGlView.setText(textStreamObject);
        } else {
            throw new RuntimeException("You must use OpenGlView in the constructor to set a text");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void clearStreamObject() throws RuntimeException
    {
        if (openGlView != null) {
            openGlView.clear();
        } else {
            throw new RuntimeException("You must use OpenGlView in the constructor to set a text");
        }
    }

    /**
     * @param alpha of the stream object on fly, 1.0f totally opaque and 0.0f totally transparent
     * @throws RuntimeException if is not an OpenGLView
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void setAlphaStreamObject(float alpha) throws RuntimeException
    {
        if (openGlView != null) {
            openGlView.setStreamObjectAlpha(alpha);
        } else {
            throw new RuntimeException("You must use OpenGlView in the constructor to set an alpha");
        }
    }

    /**
     * @param sizeX of the stream object in percent: 100 full screen to 1
     * @param sizeY of the stream object in percent: 100 full screen to 1
     * @throws RuntimeException if is not an OpenGLView
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void setSizeStreamObject(float sizeX, float sizeY) throws RuntimeException
    {
        if (openGlView != null) {
            openGlView.setStreamObjectSize(sizeX, sizeY);
        } else {
            throw new RuntimeException("You must use OpenGlView in the constructor to set a size");
        }
    }

    /**
     * @param x of the stream object in percent: 100 full screen left to 0 full right
     * @param y of the stream object in percent: 100 full screen top to 0 full bottom
     * @throws RuntimeException if is not an OpenGLView
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void setPositionStreamObject(float x, float y) throws RuntimeException
    {
        if (openGlView != null) {
            openGlView.setStreamObjectPosition(x, y);
        } else {
            throw new RuntimeException("You must use OpenGlView in the constructor to set a position");
        }
    }

    /**
     * @param position pre determinate positions
     * @throws RuntimeException if is not an OpenGLView
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void setPositionStreamObject(Position position) throws RuntimeException
    {
        if (openGlView != null) {
            openGlView.setStreamObjectPosition(position);
        } else {
            throw new RuntimeException("You must use OpenGlView in the constructor to set a position");
        }
    }

    /**
     * @return scale in percent, 0 is stream not started
     * @throws RuntimeException if is not an OpenGLView
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public PointF getSizeStreamObject() throws RuntimeException
    {
        if (openGlView != null) {
            return openGlView.getScale();
        } else {
            throw new RuntimeException("You must use OpenGlView in the constructor to get position");
        }
    }

    /**
     * @return position in percent, 0 is stream not started
     * @throws RuntimeException if is not an OpenGLView
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public PointF getPositionStreamObject() throws RuntimeException
    {
        if (openGlView != null) {
            return openGlView.getPosition();
        } else {
            throw new RuntimeException("You must use OpenGlView in the constructor to get scale");
        }
    }

    /**
     * need min API 19
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void setVideoBitrateOnFly(int bitrate)
    {
        videoEncoder.setVideoBitrateOnFly(bitrate);
    }

    public void setEffect(ColorEffect effect)
    {
        if (isStreaming()) {
            camManager.setEffect(effect);
        }
    }

    protected abstract void startRtspStream();

    protected abstract void startRtspStream(String url);

    protected abstract void stopRtspStream();

    protected abstract void prepareAudioRtp(AudioQuality audioQuality);

    protected abstract void sendAACData(ByteBuffer aacBuffer, MediaCodec.BufferInfo info);

    protected abstract void sendH264Data(ByteBuffer h264Buffer, MediaCodec.BufferInfo info);

    protected abstract void setPSPair(ByteBuffer sps, ByteBuffer pps);

    public boolean isStreaming()
    {
        return streaming;
    }

    public boolean isOnPreview()
    {
        return onPreview;
    }

    public boolean isRecording()
    {
        return recording;
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