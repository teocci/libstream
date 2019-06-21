package com.github.teocci.libstream.base;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.hardware.camera2.CameraCharacteristics;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Pair;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;

import com.github.teocci.libstream.coder.encoder.audio.AudioEncoder;
import com.github.teocci.libstream.coder.encoder.video.VideoEncoder;
import com.github.teocci.libstream.enums.TranslateTo;
import com.github.teocci.libstream.exceptions.CameraInUseException;
import com.github.teocci.libstream.input.audio.AudioQuality;
import com.github.teocci.libstream.input.audio.MicManager;
import com.github.teocci.libstream.input.video.Camera2Manager;
import com.github.teocci.libstream.input.video.Frame;
import com.github.teocci.libstream.input.video.VideoQuality;
import com.github.teocci.libstream.interfaces.audio.AACSinker;
import com.github.teocci.libstream.enums.Camera2Facing;
import com.github.teocci.libstream.interfaces.video.CameraSinker;
import com.github.teocci.libstream.interfaces.video.EncoderSinker;
import com.github.teocci.libstream.interfaces.audio.MicSinker;
import com.github.teocci.libstream.utils.LogHelper;
import com.github.teocci.libstream.utils.gl.GifStreamObject;
import com.github.teocci.libstream.utils.gl.ImageStreamObject;
import com.github.teocci.libstream.utils.gl.Position;
import com.github.teocci.libstream.utils.gl.TextStreamObject;
import com.github.teocci.libstream.view.OpenGlView;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.github.teocci.libstream.enums.FormatVideoEncoder.SURFACE;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public abstract class Camera2Base implements MicSinker, AACSinker, CameraSinker, EncoderSinker
{
    private static String TAG = LogHelper.makeLogTag(Camera2Base.class);

    protected Context context;
    protected Camera2Manager cameraManager;
    protected VideoEncoder videoEncoder;
    protected MicManager micManager;
    protected AudioEncoder audioEncoder;
    private boolean streaming;
    private SurfaceView surfaceView;
    private TextureView textureView;
    private OpenGlView openGlView;
    private boolean videoEnabled = false;
    //record
    private MediaMuxer mediaMuxer;
    private int videoTrack = -1;
    private int audioTrack = -1;
    private boolean recording = false;
    private boolean canRecord = false;
    private boolean onPreview = false;
    private MediaFormat videoFormat;
    private MediaFormat audioFormat;

    public Camera2Base(SurfaceView surfaceView, Context context)
    {
        this.surfaceView = surfaceView;
        this.context = context;
        cameraManager = new Camera2Manager(context);
        videoEncoder = new VideoEncoder(this);
        micManager = new MicManager(this);
        audioEncoder = new AudioEncoder(this);
        streaming = false;
    }

    public Camera2Base(TextureView textureView, Context context)
    {
        this.textureView = textureView;
        this.context = context;
        cameraManager = new Camera2Manager(context);
        videoEncoder = new VideoEncoder(this);
        micManager = new MicManager(this);
        audioEncoder = new AudioEncoder(this);
        streaming = false;
    }

    public Camera2Base(OpenGlView openGlView, Context context)
    {
        this.openGlView = openGlView;
        this.context = context;
        cameraManager = new Camera2Manager(context);
        videoEncoder = new VideoEncoder(this);
        micManager = new MicManager(this);
        audioEncoder = new AudioEncoder(this);
        streaming = false;
    }

    public Camera2Base(Context context)
    {
        this.context = context;
        this.textureView = null;
        cameraManager = new Camera2Manager(context);
        videoEncoder = new VideoEncoder(this);
        micManager = new MicManager(this);
        audioEncoder = new AudioEncoder(this);
        streaming = false;
    }

    @Override
    public void onAACData(ByteBuffer aacBuffer, MediaCodec.BufferInfo info)
    {
        if (recording && audioTrack != -1 && canRecord) {
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
        if (recording && videoTrack != -1) {
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

    public boolean prepareVideo(VideoQuality quality, boolean hardwareRotation, int rotation)
    {
        if (onPreview) {
            stopPreview();
            onPreview = true;
        }
        int imageFormat = ImageFormat.NV21; //supported nv21 and yv12
        videoEncoder.setImageFormat(imageFormat);
        boolean result = videoEncoder.prepare(quality, hardwareRotation, rotation, SURFACE);
        prepareCameraManager();
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
        if (onPreview) {
            stopPreview();
            onPreview = true;
        }

        boolean result = videoEncoder.prepare(VideoQuality.DEFAULT, true, 0, SURFACE);
        prepareCameraManager();
        return result;
    }

    public boolean prepareAudio()
    {
        micManager.config();
        return audioEncoder.prepare();
    }

    /**
     * Need be called while stream
     */
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

    /**
     * Select a camera for preview passing
     * {@link android.hardware.camera2.CameraMetadata#LENS_FACING_BACK} or
     * {@link android.hardware.camera2.CameraMetadata#LENS_FACING_FRONT}
     *
     * @param cameraFacing -
     */
    public void startPreview(@Camera2Facing int cameraFacing)
    {
        if (!isStreaming() && !onPreview) {
            if (surfaceView != null) {
                cameraManager.prepareCamera(surfaceView.getHolder().getSurface(), false);
            } else if (textureView != null) {
                cameraManager.prepareCamera(new Surface(textureView.getSurfaceTexture()), false);
            } else if (openGlView != null) {
                openGlView.startGLThread();
                cameraManager.prepareCamera(openGlView.getSurface(), true);
            }
            cameraManager.openCameraFacing(cameraFacing);
            onPreview = true;
        }
    }

    /**
     * Default cam is back
     */
    public void startPreview()
    {
        startPreview(CameraCharacteristics.LENS_FACING_BACK);
    }

    public void stopPreview()
    {
        if (!isStreaming() && onPreview) {
            if (openGlView != null) {
                openGlView.stopGlThread();
            }
            cameraManager.closeCamera(false);
            onPreview = false;
        }
    }

    public void startStream(String url)
    {
        if (openGlView != null && videoEnabled) {
            openGlView.setEncoderSize(videoEncoder.getWidth(), videoEncoder.getHeight());
            openGlView.startGLThread();
            openGlView.addMediaCodecSurface(videoEncoder.getInputSurface());
            cameraManager.prepareCamera(openGlView.getSurface(), true);
        }
        videoEncoder.start();
        audioEncoder.start();
        if (onPreview) {
            cameraManager.openLastCamera();
        } else {
            cameraManager.openCameraBack();
        }
        micManager.start();
        streaming = true;
        startStreamRtp(url);
    }

    public void stopStream()
    {
        cameraManager.closeCamera(true);
        micManager.stop();
        stopStreamRtp();
        videoEncoder.stop();
        audioEncoder.stop();
        if (openGlView != null) {
            openGlView.stopGlThread();
            openGlView.removeMediaCodecSurface();
        }
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

    public void switchCamera() throws CameraInUseException
    {
        if (isStreaming() || onPreview) {
            cameraManager.switchCamera();
        }
    }

    public void setGifStreamObject(GifStreamObject gifStreamObject) throws RuntimeException
    {
        if (openGlView != null) {
            openGlView.setGif(gifStreamObject);
        } else {
            throw new RuntimeException("You must use OpenGlView in the constructor to set a gif");
        }
    }

    public void setImageStreamObject(ImageStreamObject imageStreamObject) throws RuntimeException
    {
        if (openGlView != null) {
            openGlView.setImage(imageStreamObject);
        } else {
            throw new RuntimeException("You must use OpenGlView in the constructor to set a image");
        }
    }

    public void setTextStreamObject(TextStreamObject textStreamObject) throws RuntimeException
    {
        if (openGlView != null) {
            openGlView.setText(textStreamObject);
        } else {
            throw new RuntimeException("You must use OpenGlView in the constructor to set a text");
        }
    }

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
    public void setPositionStreamObject(@TranslateTo int position) throws RuntimeException
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
    public void setVideoBitrateOnFly(int bitrate)
    {
        if (Build.VERSION.SDK_INT >= 19) {
            videoEncoder.setVideoBitrateOnFly(bitrate);
        }
    }

    private void prepareCameraManager()
    {
        if (textureView != null) {
            cameraManager.prepareCamera(textureView, videoEncoder.getInputSurface());
        } else if (surfaceView != null) {
            cameraManager.prepareCamera(surfaceView, videoEncoder.getInputSurface());
        } else if (openGlView != null) {
            // TODO
        } else {
            cameraManager.prepareCamera(videoEncoder.getInputSurface(), false);
        }
        videoEnabled = true;
    }

    public abstract void setAuthorization(String user, String password);

    protected abstract void startStreamRtp(String url);

    protected abstract void stopStreamRtp();

    protected abstract void prepareAudioRtp(AudioQuality audioQuality);

    protected abstract void getAacDataRtp(ByteBuffer aacBuffer, MediaCodec.BufferInfo info);

    protected abstract void getH264DataRtp(ByteBuffer h264Buffer, MediaCodec.BufferInfo info);

    protected abstract void sendAVCInfo(ByteBuffer sps, ByteBuffer pps, ByteBuffer vps);




    public boolean isStreaming()
    {
        return streaming;
    }

    public boolean isRecording()
    {
        return recording;
    }

    public boolean isOnPreview()
    {
        return onPreview;
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
