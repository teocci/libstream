package com.github.teocci.libstream.base;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Pair;
import android.view.SurfaceView;
import android.view.TextureView;

import com.github.teocci.libstream.coder.encoder.audio.AudioEncoder;
import com.github.teocci.libstream.coder.encoder.video.VideoEncoder;
import com.github.teocci.libstream.controllers.RecordController;
import com.github.teocci.libstream.enums.CameraFacing;
import com.github.teocci.libstream.enums.ColorEffect;
import com.github.teocci.libstream.enums.FormatVideoEncoder;
import com.github.teocci.libstream.enums.RecordStatus;
import com.github.teocci.libstream.exceptions.CameraInUseException;
import com.github.teocci.libstream.input.audio.AudioQuality;
import com.github.teocci.libstream.input.audio.MicManager;
import com.github.teocci.libstream.input.video.CamManager;
import com.github.teocci.libstream.input.video.Frame;
import com.github.teocci.libstream.input.video.VideoQuality;
import com.github.teocci.libstream.interfaces.RecordStatusListener;
import com.github.teocci.libstream.interfaces.audio.AACSinker;
import com.github.teocci.libstream.interfaces.audio.MicSinker;
import com.github.teocci.libstream.interfaces.video.CameraSinker;
import com.github.teocci.libstream.interfaces.video.EncoderSinker;
import com.github.teocci.libstream.utils.LogHelper;
import com.github.teocci.libstream.view.OpenGlView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
import static com.github.teocci.libstream.enums.FormatVideoEncoder.SURFACE;
import static com.github.teocci.libstream.enums.FormatVideoEncoder.YUV420DYNAMICAL;
import static com.github.teocci.libstream.utils.CameraHelper.getCameraOrientation;
import static com.github.teocci.libstream.utils.CodecUtil.IFRAME_INTERVAL;
import static com.github.teocci.libstream.utils.BuildUtil.minAPI18;
import static com.github.teocci.libstream.utils.BuildUtil.minAPI19;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public abstract class EncoderBase implements MicSinker, AACSinker, CameraSinker, EncoderSinker
{
    private static String TAG = LogHelper.makeLogTag(EncoderBase.class);

    private Context context;
    private OpenGlView openGlView;

    // Record
    protected RecordController recordController;

//    private MediaMuxer mediaMuxer;
//    private MediaFormat videoFormat;
//    private MediaFormat audioFormat;

    protected CamManager camManager;
    protected MicManager micManager;

    protected VideoEncoder videoEncoder;
    protected AudioEncoder audioEncoder;

    private int previewWidth, previewHeight;

    private boolean streaming = false;

    private boolean videoEnabled = true;
    private boolean onPreview = false;

    public EncoderBase(SurfaceView surfaceView)
    {
        context = surfaceView.getContext();
        camManager = new CamManager(surfaceView, this);

        init();
    }

    public EncoderBase(TextureView textureView)
    {
        context = textureView.getContext();
        camManager = new CamManager(textureView, this);

        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public EncoderBase(OpenGlView openGlView)
    {
        this.openGlView = openGlView;
        this.context = openGlView.getContext();

        init();
    }


    // Implementations

    /**
     * TODO: Merge with onSpsPpsVpsReady()
     *
     * @param psPair
     */
    @Override
    public void onPSReady(Pair<ByteBuffer, ByteBuffer> psPair)
    {
        LogHelper.e(TAG, "onPSReady()");
        sendAVCInfo(psPair.first, psPair.second, null);
    }


    /**
     * TODO: Merge with onPSReady()
     *
     * @param sps
     * @param pps
     * @param vps
     */
    @Override
    public void onSpsPpsVpsReady(ByteBuffer sps, ByteBuffer pps, ByteBuffer vps)
    {
        sendAVCInfo(sps, pps, vps);
    }

    @Override
    public void onAACData(ByteBuffer aacBuffer, MediaCodec.BufferInfo info)
    {
        if (minAPI19()) {
            recordController.recordAudio(aacBuffer, info);
        }
        if (streaming) sendAACData(aacBuffer, info);
    }

    @Override
    public void onEncodedData(ByteBuffer videoBuffer, MediaCodec.BufferInfo info)
    {
        if (minAPI19()) {
            recordController.recordVideo(videoBuffer, info);
        }
        if (streaming) sendH264Data(videoBuffer, info);
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
//        videoFormat = mediaFormat;
        recordController.setVideoFormat(mediaFormat);
    }

    @Override
    public void onAudioFormat(MediaFormat mediaFormat)
    {
//        audioFormat = mediaFormat;
        recordController.setAudioFormat(mediaFormat);
    }


    // Class Methods

    private void init()
    {
        videoEncoder = new VideoEncoder(this);
        micManager = new MicManager(this);
        audioEncoder = new AudioEncoder(this);
        recordController = new RecordController();
    }


    /**
     * Same to call: rotation = 0; if (Portrait) rotation = 90; prepareVideo(640, 480, 30, 1200 *
     * 1024, false, rotation);
     *
     * @return true if success, false if you get a error (Normally because the encoder selected
     * doesn't support any configuration seated or your device hasn't a H264 encoder).
     */
    public boolean prepareVideo()
    {
        int orientation = getCameraOrientation(context);

        return prepareVideo(VideoQuality.DEFAULT, false, orientation);
    }


    /**
     * TODO: active implementation
     *
     * @param quality
     * @return
     */
    public boolean prepareVideo(VideoQuality quality)
    {
        return prepareVideo(quality, false, 0);
    }

    public boolean prepareVideo(VideoQuality quality, boolean hardwareRotation, int orientation)
    {
        return prepareVideo(quality, hardwareRotation, orientation, IFRAME_INTERVAL);
    }

    /**
     * Call this method before use @startStream. If not you will do a stream without video. NOTE:
     * Rotation with encoder is silence ignored in some devices.
     *
     * @param quality          represents the quality of the video stream.
     * @param hardwareRotation true if you want rotate using encoder, false if you want rotate with
     *                         software if you are using a SurfaceView or TextureView or with OpenGl if you are using
     *                         OpenGlView.
     * @param orientation      could be 90, 180, 270 or 0. You should use CameraHelper.getCameraOrientation
     *                         with SurfaceView or TextureView and 0 with OpenGlView or LightOpenGlView. NOTE: Rotation with
     *                         encoder is silence ignored in some devices.
     * @param iFrameInterval   seconds between I-frames
     * @return true if success, false if you get a error (Normally because the encoder selected
     * doesn't support any configuration seated or your device hasn't a H264 encoder).
     */
    public boolean prepareVideo(VideoQuality quality, boolean hardwareRotation, int orientation, int iFrameInterval)
    {
        LogHelper.e(TAG, "prepareVideo called");
        if (onPreview) {
            stopPreview();
            onPreview = true;
        }

        // Supported NV21 and YV12
        FormatVideoEncoder format = openGlView == null ? YUV420DYNAMICAL : SURFACE;

        return videoEncoder.prepare(quality, hardwareRotation, orientation, iFrameInterval, format);


//        if (openGlView == null) {
//            camManager.prepare(quality, imageFormat);
//            videoEncoder.setImageFormat(imageFormat);
//            return videoEncoder.prepare(quality, rotation, hardwareRotation, YUV420DYNAMICAL);
//        } else {
//            return videoEncoder.prepare(quality, rotation, hardwareRotation, SURFACE);
//        }
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

    /**
     * Start record a MP4 video. Need be called while stream.
     *
     * @param path where file will be saved.
     * @throws IOException If you init it before encode stream.
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void startRecord(final String path) throws IOException
    {
        startRecord(path, null);
    }

    /**
     * Start record a MP4 video. Need be called while stream.
     *
     * @param path     where file will be saved.
     * @param listener is the callback for the record status
     * @throws IOException If you init it before start stream.
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void startRecord(final String path, RecordStatusListener listener) throws IOException
    {
        if (!streaming) {
            startEncoders();
        } else if (videoEncoder.isRunning()) {
            resetVideoEncoder();
        }

        recordController.startRecord(path, listener);
    }


    public void startPreview()
    {
        startPreview(CAMERA_FACING_BACK);
    }

    public void startPreview(@CameraFacing int cameraFacing)
    {
        startPreview(cameraFacing, VideoQuality.DEFAULT.width, VideoQuality.DEFAULT.height);
    }

    public void startPreview(int width, int height)
    {
        startPreview(CAMERA_FACING_BACK, width, height);
    }

    public void startPreview(@CameraFacing int cameraFacing, int width, int height)
    {
        startPreview(cameraFacing, width, height, getCameraOrientation(context));
    }

    /**
     * Start camera preview. Ignored, if stream or preview is started.
     *
     * @param cameraFacing front or back camera identifier
     * @param width        of preview in px.
     * @param height       of preview in px.
     * @param orientation  camera rotation (0, 90, 180, 270).
     */
    public void startPreview(@CameraFacing int cameraFacing, int width, int height, int orientation)
    {
        if (!isStreaming() && !onPreview) {
            previewWidth = width;
            previewHeight = height;

            if (openGlView != null && minAPI18()) {
                openGlView.startGLThread();
                camManager = new CamManager(openGlView.getSurfaceTexture(), openGlView.getContext());
            }
            camManager.prepare();
            camManager.setOrientation(orientation);
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

    public void startStream()
    {
//        prepareCamera();
        startProcessing();
        startRtpStream();
    }

    /**
     * Need be called after @prepareVideo or/and @prepareAudio. This method override resolution of
     *
     * @param url of the stream like: protocol://ip:port/application/streamName
     *            <p>
     *            RTSP: rtsp://192.168.1.1:1935/live/pedroSG94 RTSPS: rtsps://192.168.1.1:1935/live/pedroSG94
     *            RTMP: rtmp://192.168.1.1:1935/live/pedroSG94 RTMPS: rtmps://192.168.1.1:1935/live/pedroSG94
     * @startPreview to resolution seated in @prepareVideo. If you never startPreview this method
     * startPreview for you to resolution seated in @prepareVideo.
     */
    public void startStream(String url)
    {
        prepareCamera();
        startProcessing();
        startRtpStream(url);
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

    private void startEncoders()
    {
        videoEncoder.start();
        audioEncoder.start();
//        prepareGlView();
        prepareCamera();
        camManager.setOrientation(videoEncoder.getRotation());
        if (!camManager.isRunning() && videoEncoder.getWidth() != previewWidth || videoEncoder.getHeight() != previewHeight) {
            camManager.start(videoEncoder.getQuality());
        }
        micManager.start();
    }

    private void resetVideoEncoder()
    {
        if (openGlView != null && minAPI18()) {
            openGlView.removeMediaCodecSurface();
        }
        videoEncoder.reset();
        if (openGlView != null && minAPI18()) {
            openGlView.addMediaCodecSurface(videoEncoder.getInputSurface());
        }
    }

    /**
     * TODO
     */
    private void prepareGlView()
    {
        if (openGlView != null && minAPI18()) {
            openGlView.startGLThread();
            if (videoEncoder.getRotation() == 90 || videoEncoder.getRotation() == 270) {
                openGlView.setEncoderSize(videoEncoder.getHeight(), videoEncoder.getWidth());
            } else {
                openGlView.setEncoderSize(videoEncoder.getWidth(), videoEncoder.getHeight());
            }
            // TODO: rotation
            openGlView.setRotation(0);
            if (!camManager.isRunning() && videoEncoder.getWidth() != previewWidth || videoEncoder.getHeight() != previewHeight) {
                openGlView.startGLThread();
            }
            if (videoEncoder.getInputSurface() != null) {
                openGlView.addMediaCodecSurface(videoEncoder.getInputSurface());
            }
            camManager.setSurfaceTexture(openGlView.getSurfaceTexture());
        }
    }


    /**
     * OLD Implementations
     */
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


    /**
     * Stops recording the MP4 video started by the startRecord() method. If you don't call it file will be unreadable.
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void stopRecord()
    {
        recordController.stopRecord();
        if (!streaming) stopStream();
//        recording = false;
//        canRecord = false;
//        if (mediaMuxer != null) {
//            mediaMuxer.stop();
//            mediaMuxer.release();
//            mediaMuxer = null;
//        }
//        videoTrack = -1;
//        audioTrack = -1;
    }


    /**
     * Stop camera preview. Ignored if streaming or already stopped. You need call it after calling the stopStream()
     * method to release camera properly if you will close activity.
     */
    public void stopPreview()
    {
        if (!isStreaming() && onPreview) {
            if (openGlView != null && minAPI18()) {
                openGlView.stopGlThread();
            }
            camManager.stop();
            onPreview = false;
            previewWidth = 0;
            previewHeight = 0;
        } else {
            LogHelper.e(TAG, "Streaming or preview stopped, ignored");
        }
    }

    /**
     * Stop stream started by the startStream() method.
     */
    public void stopStream()
    {
        stopProcessing();
        stopRtpStream();
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

    /**
     * Switch camera used. Can be called on preview or while stream, ignored with preview off.
     *
     * @throws CameraInUseException If the other camera doesn't support same resolution.
     */
    public void switchCamera() throws CameraInUseException
    {
        if (isStreaming() || onPreview) {
            camManager.switchCamera();
        }
    }

    public void handleZoom(int newZoom)
    {
        camManager.handleZoom(newZoom);
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

    /**
     * Disable send camera frames and send a black image with low bitrate(to reduce bandwith used)
     * instance it.
     */
    public void disableVideo()
    {
        videoEncoder.startSendBlackImage();
        videoEnabled = false;
    }

    /**
     * Enable send camera frames.
     */
    public void enableVideo()
    {
        videoEncoder.stopSendBlackImage();
        videoEnabled = true;
    }

    public void pauseRecord()
    {
        recordController.pauseRecord();
    }

    public void resumeRecord()
    {
        recordController.resumeRecord();
    }


    // Setters

    /**
     * Change preview orientation can be called while stream.
     *
     * @param orientation of the camera preview. Could be 90, 180, 270 or 0.
     */
    public void setPreviewOrientation(int orientation)
    {
        camManager.setPreviewOrientation(orientation);
    }

    /**
     * Set zoomIn or zoomOut to camera.
     *
     * @param newZoom motion value
     */
    public void setZoom(int newZoom)
    {
        camManager.setZoom(newZoom);
    }


    /**
     * Set video bitrate of H264 in kb while stream.
     *
     * @param bitrate H264 in kb.
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void setVideoBitrateOnFly(int bitrate)
    {
        videoEncoder.setVideoBitrateOnFly(bitrate);
    }

    /**
     * Set limit FPS while stream. This will be override when you call to prepareVideo method. This
     * could produce a change in iFrameInterval.
     *
     * @param fps frames per second
     */
    public void setLimitFPSOnFly(int fps)
    {
        videoEncoder.setFps(fps);
    }

    public void setEffect(ColorEffect effect)
    {
        if (isStreaming()) {
            camManager.setEffect(effect);
        }
    }


    // Getters

    public RecordStatus getRecordStatus()
    {
        return recordController.getStatus();
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


    // Boolean Methods

    /**
     * Gets stream state.
     *
     * @return true if streaming, false if not streaming.
     */
    public boolean isStreaming()
    {
        return streaming;
    }

    /**
     * Gets preview state.
     *
     * @return true if enabled, false if disabled.
     */
    public boolean isOnPreview()
    {
        return onPreview;
    }

    /**
     * Gets record state.
     *
     * @return true if recording, false if not recoding.
     */
    public boolean isRecording()
    {
        return recordController.isRunning();
    }

    /**
     * Gets mute state of microphone.
     *
     * @return true if muted, false if enabled
     */
    public boolean isAudioMuted()
    {
        return micManager.isMuted();
    }

    /**
     * Gets video camera state
     *
     * @return true if disabled, false if enabled
     */
    public boolean isVideoEnabled()
    {
        return videoEnabled;
    }


    // Abstract methods

    /**
     * Basic auth developed to work with Wowza. No tested with other server
     *
     * @param user     auth.
     * @param password auth.
     */
    public abstract void setAuthorization(String user, String password);

    protected abstract void prepareAudioRtp(AudioQuality audioQuality);

    protected abstract void startRtpStream();

    protected abstract void startRtpStream(String url);

    protected abstract void stopRtpStream();


    protected abstract void sendAACData(ByteBuffer aacBuffer, MediaCodec.BufferInfo info);

    protected abstract void sendH264Data(ByteBuffer h264Buffer, MediaCodec.BufferInfo info);

    protected abstract void sendAVCInfo(ByteBuffer sps, ByteBuffer pps, ByteBuffer vps);
}