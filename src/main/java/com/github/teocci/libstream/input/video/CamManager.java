package com.github.teocci.libstream.input.video;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.SurfaceView;
import android.view.TextureView;

import com.github.teocci.libstream.enums.CameraFacing;
import com.github.teocci.libstream.enums.ColorEffect;
import com.github.teocci.libstream.exceptions.CameraInUseException;
import com.github.teocci.libstream.exceptions.ConfNotSupportedException;
import com.github.teocci.libstream.interfaces.video.CameraSinker;
import com.github.teocci.libstream.interfaces.video.FaceDetectorCallback;
import com.github.teocci.libstream.utils.LogHelper;

import java.util.Iterator;
import java.util.List;

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
import static android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;
import static android.hardware.Camera.Parameters.FOCUS_MODE_AUTO;
import static android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;

/**
 * This class need use same resolution, fps and imageFormat that VideoEncoder
 * Tested with YV12 and NV21.
 * <p>
 * Advantage = you can control fps of the stream.
 * Disadvantages = you cant use all resolutions, only resolution that your camera support.
 * <p>
 * If you want use all resolutions. You can use libYuv for resize images in OnPreviewFrame:
 * https://chromium.googlesource.com/libyuv/libyuv/
 * <p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-1-23
 */

public class CamManager implements Camera.PreviewCallback, Camera.FaceDetectionListener
{
    private static String TAG = LogHelper.makeLogTag(CamManager.class);

    private Camera camera = null;

    private CameraSinker cameraSinker;

    private SurfaceView surfaceView;
    private TextureView textureView;
    private SurfaceTexture surfaceTexture;

    private boolean lanternEnable = false;
    private boolean isFrontCamera = false;
    private boolean isPortrait = false;

    private int cameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
    private Context context;

    private int cameraSelect;

    // Default parameters for camera
//    private int width = 640;
//    private int height = 480;
//    private int fps = 30;
    private VideoQuality quality = VideoQuality.DEFAULT;

    private int currentZoom;

    private int orientation = 0;
    private int imageFormat = ImageFormat.NV21;

    private HandlerThread handlerThread;
    private Handler handler;

    private byte[] yuvBuffer;

    private List<Camera.Size> previewSizeBack;
    private List<Camera.Size> previewSizeFront;

    private FaceDetectorCallback faceDetectorCallback;

    private volatile boolean prepared = false;
    private volatile boolean running = false;

    public CamManager(SurfaceView surfaceView, CameraSinker cameraSinker)
    {
        this.surfaceView = surfaceView;
        this.cameraSinker = cameraSinker;
        this.context = surfaceView.getContext();

        init();
    }

    public CamManager(TextureView textureView, CameraSinker cameraSinker)
    {
        this.textureView = textureView;
        this.cameraSinker = cameraSinker;
        this.context = textureView.getContext();

        init();
    }

    public CamManager(SurfaceTexture surfaceTexture, Context context)
    {
        this.surfaceTexture = surfaceTexture;
        this.context = context;

        init();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera)
    {
//        if (isFrontCamera) data = YUVUtil.rotateNV21(data, quality.width, quality.height, 180);
        cameraSinker.onYUVData(data);
//        cameraSinker.onYUVData(new Frame(data, orientation, isFrontCamera && isPortrait, imageFormat));
        camera.addCallbackBuffer(yuvBuffer);
    }

    @Override
    public void onFaceDetection(Camera.Face[] faces, Camera camera)
    {
        if (faceDetectorCallback != null) faceDetectorCallback.onGetFaces(faces);
    }


    private void init()
    {
        cameraSelect = selectCameraFront();
        previewSizeFront = getPreviewSize();
        cameraSelect = selectCameraBack();
        previewSizeBack = getPreviewSize();
    }

    public void prepare()
    {
        prepare(VideoQuality.DEFAULT, imageFormat);
    }

    public void prepare(VideoQuality quality, int imageFormat)
    {
        this.quality = quality;
        this.imageFormat = imageFormat;
        prepared = true;
        LogHelper.e(TAG, "prepare with VideoQuality called");
    }

    /**
     * @param width       The horizontal resolution in px.
     * @param height      The vertical resolution in px.
     * @param fps         The fps in frames per second of the stream.
     * @param imageFormat
     */
    public void prepare(int width, int height, int fps, int imageFormat)
    {
        this.quality.width = width;
        this.quality.height = height;
        this.quality.fps = fps;
        this.imageFormat = imageFormat;

        prepared = true;
        LogHelper.e(TAG, "prepare called");
    }

    // Start camera methods

    public void start()
    {
        start(cameraFacing, quality.width, quality.height, quality.fps);
    }

    public void start(@CameraFacing int cameraFacing)
    {
        start(cameraFacing, quality.width, quality.height);
    }

    public void start(VideoQuality quality)
    {
        start(cameraFacing, quality.width, quality.height, quality.fps);
    }

    public void start(@CameraFacing int cameraFacing, VideoQuality quality)
    {
        start(cameraFacing, quality.width, quality.height, quality.fps);
    }

    public void start(@CameraFacing int cameraFacing, int width, int height)
    {
        start(cameraFacing, width, height, quality.fps);
    }

    /**
     * @param cameraFacing
     * @param width
     * @param height
     * @param fps
     */
    public void start(@CameraFacing int cameraFacing, int width, int height, int fps)
    {
        LogHelper.e(TAG, "start with cam facing");
        quality.width = width;
        quality.height = height;
        quality.fps = fps;
        cameraSelect = (cameraFacing == CAMERA_FACING_BACK ? selectCameraBack() : selectCameraFront());

        if (!configSupported()) {
            throw new ConfNotSupportedException("This camera resolution cant be opened");
        }
        LogHelper.e(TAG, "started");

        handlerThread = new HandlerThread("camera-thread");
        handlerThread.start();

        handler = new Handler(handlerThread.getLooper());
        handler.post(() -> {
            LogHelper.e(TAG, "start handler");
            yuvBuffer = new byte[quality.width * quality.height * 3 / 2];
//            YUVUtil.preAllocateRotateBuffers(yuvBuffer.length);
//            if (imageFormat == ImageFormat.NV21) {
//                YUVUtil.preAllocateNv21Buffers(yuvBuffer.length);
//            } else {
//                YUVUtil.preAllocateYv12Buffers(yuvBuffer.length);
//            }
//
//            if (camera == null && prepared) {
            try {
                camera = Camera.open(cameraSelect);
                if (camera == null) throw new NullPointerException("Camera is null.");

                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(cameraSelect, info);
                isFrontCamera = info.facing == CAMERA_FACING_FRONT;
                isPortrait = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

                Camera.Parameters parameters = camera.getParameters();
                parameters.setPreviewSize(quality.width, quality.height);
                parameters.setPreviewFormat(imageFormat);
                int[] range = adaptFpsRange(quality.fps, parameters.getSupportedPreviewFpsRange());
                parameters.setPreviewFpsRange(range[0], range[1]);

//                List<String> supportedFocusModes = parameters.getSupportedFocusModes();
//                if (supportedFocusModes != null && !supportedFocusModes.isEmpty()) {
//                    if (supportedFocusModes.contains(FOCUS_MODE_CONTINUOUS_PICTURE)) {
//                        parameters.setFocusMode(FOCUS_MODE_CONTINUOUS_PICTURE);
//                    } else if (supportedFocusModes.contains(FOCUS_MODE_AUTO)) {
//                        parameters.setFocusMode(FOCUS_MODE_AUTO);
//                    } else {
//                        parameters.setFocusMode(supportedFocusModes.get(0));
//                    }
//                }

                camera.setParameters(parameters);
                camera.setDisplayOrientation(orientation);
                LogHelper.e(TAG, "Current Orientation: " + orientation);

                if (surfaceView != null) {
                    camera.setPreviewDisplay(surfaceView.getHolder());
                    camera.addCallbackBuffer(yuvBuffer);
                    camera.setPreviewCallbackWithBuffer(this);
                } else if (textureView != null) {
                    camera.setPreviewTexture(textureView.getSurfaceTexture());
                    camera.addCallbackBuffer(yuvBuffer);
                    camera.setPreviewCallbackWithBuffer(this);
                } else {
                    camera.setPreviewTexture(surfaceTexture);
                }

                camera.startPreview();
                running = true;
                LogHelper.e(TAG, quality.width + "X" + quality.height);
            } catch (Exception e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
//            } else {
//                LogHelper.e(TAG, "CamManager: " + camera + " prepared: " + prepared);
//                LogHelper.e(TAG, "CamManager need be prepared, CamManager not enabled");
//                if (camera != null && prepared) {
//                    stop();
//                    prepared = true;
//                    start();
//                }
//            }
//            LogHelper.e(TAG, "end handler");
        });
    }

    private int selectCameraBack()
    {
        return selectCamera(CAMERA_FACING_BACK);
    }

    private int selectCameraFront()
    {
        return selectCamera(CAMERA_FACING_FRONT);
    }

    private int selectCamera(int facing)
    {
        int number = Camera.getNumberOfCameras();
        for (int i = 0; i < number; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == facing) return i;
        }

        return 0;
    }

    public void stop()
    {
        if (camera != null) {
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.setPreviewCallbackWithBuffer(null);
            camera.release();
            camera = null;

//            if (surfaceView != null) {
//                clearSurface(surfaceView.getHolder());
//            } else if (textureView != null) {
//                clearSurface(textureView.getSurfaceTexture());
//            } else {
//                clearSurface(surfaceTexture);
//            }
        }

        if (handlerThread != null) {
            handlerThread.quit();
            handlerThread = null;
        }

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }

        running = false;
        prepared = false;
    }

    private int[] adaptFpsRange(int expectedFps, List<int[]> fpsRanges)
    {
        expectedFps *= 1000;
        int[] closestRange = fpsRanges.get(0);
        int measure = Math.abs(closestRange[0] - expectedFps) + Math.abs(closestRange[1] - expectedFps);
        for (int[] range : fpsRanges) {
            if (range[0] <= expectedFps && range[1] >= expectedFps) {
                int curMeasure = Math.abs(range[0] - expectedFps) + Math.abs(range[1] - expectedFps);
                if (curMeasure < measure) {
                    closestRange = range;
                    measure = curMeasure;
                }
            }
        }

        return closestRange;
    }

    public void switchCamera() throws CameraInUseException
    {
        if (camera == null) return;

        int oldCamera = cameraSelect;
        int number = Camera.getNumberOfCameras();

        for (int i = 0; i < number; i++) {
            if (cameraSelect != i) {
                cameraSelect = i;
                if (!configSupported()) {
                    cameraSelect = oldCamera;
                    throw new CameraInUseException("This camera resolution cant be opened");
                }
                stop();
//                    prepared = true;

                cameraFacing = cameraFacing == CAMERA_FACING_BACK ? CAMERA_FACING_FRONT : cameraFacing;
                start();

                return;
            }
        }
    }

    private boolean configSupported()
    {
        List<Camera.Size> previews = cameraSelect == selectCameraBack() ? previewSizeBack : previewSizeFront;

        if (previews != null) {
            for (Camera.Size size : previews) {
                if (size.width == quality.width && size.height == quality.height) {
                    return true;
                }
            }
        }

        return false;
    }

    public void handleZoom(int newZoom)
    {
        try {
            if (camera == null) return;
            if (!isRunning()) return;
            if (camera.getParameters() == null) return;
            if (!camera.getParameters().isZoomSupported()) return;

            Camera.Parameters parameters = camera.getParameters();
            int maxZoom = parameters.getMaxZoom();
            int zoom = parameters.getZoom();

            if (newZoom > 0) {
                // zoom in
                if (zoom < maxZoom)
                    zoom++;
            } else if (newZoom < 0) {
                // zoom out
                if (zoom > 0)
                    zoom--;
            }

            parameters.setZoom(zoom);
            camera.setParameters(parameters);
        } catch (Exception e) {
            LogHelper.e(TAG, "handleZoom");
            e.printStackTrace();
        }
    }

    /**
     * Add <uses-permission android:name="android.permission.FLASHLIGHT"/>
     */
    public void enableLantern() throws Exception
    {
        if (camera == null) return;
        Camera.Parameters parameters = camera.getParameters();
        List<String> supportedFlashModes = parameters.getSupportedFlashModes();
        if (supportedFlashModes != null && !supportedFlashModes.isEmpty()) {
            if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                camera.setParameters(parameters);
                lanternEnable = true;
            } else {
                LogHelper.e(TAG, "Lantern unsupported");
                throw new Exception("Lantern unsupported");
            }
        }
    }

    /**
     * Add <uses-permission android:name="android.permission.FLASHLIGHT"/>
     */
    public void disableLantern()
    {
        if (camera == null) return;
        Camera.Parameters parameters = camera.getParameters();
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        camera.setParameters(parameters);
        lanternEnable = false;
    }

    public void enableRecordingHint()
    {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setRecordingHint(true);
            camera.setParameters(parameters);
        }
    }

    public void disableRecordingHint()
    {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setRecordingHint(false);
            camera.setParameters(parameters);
        }
    }

    public void enableFaceDetection(FaceDetectorCallback faceDetectorCallback)
    {
        if (camera != null) {
            this.faceDetectorCallback = faceDetectorCallback;
            camera.setFaceDetectionListener(this);
            camera.startFaceDetection();
        }
    }

    public void disableFaceDetection()
    {
        if (camera != null) {
            faceDetectorCallback = null;
            camera.stopFaceDetection();
            camera.setFaceDetectionListener(null);
        }
    }


    // Setters

    public void setSurfaceTexture(SurfaceTexture surfaceTexture)
    {
        this.surfaceTexture = surfaceTexture;
    }

    public void setOrientation(int orientation)
    {
        this.orientation = orientation;
    }

    public void setPreviewOrientation(final int orientation)
    {
        this.orientation = orientation;
        if (camera != null && running) {
            camera.stopPreview();
            camera.setDisplayOrientation(orientation);
            camera.startPreview();
        }
    }

    public void setZoom(int newZoom)
    {
        int zoom = 0;
        if (newZoom > currentZoom) {
            // zoom in
            zoom = 1;
        } else if (newZoom < currentZoom) {
            // zoom out
            zoom = -1;
        }
        currentZoom = newZoom;
        handleZoom(zoom);
    }

    public void setEffect(ColorEffect effect)
    {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setColorEffect(effect.getEffect());
            try {
                camera.setParameters(parameters);
            } catch (RuntimeException e) {
                LogHelper.e(TAG, "Unsupported effect: ", e);
            }
        }
    }

    // Getters

    /**
     * See: https://developer.android.com/reference/android/graphics/ImageFormat.html
     * to know name of constant values
     * Example: 842094169 -> YV12, 17 -> NV21
     */
    public List<Integer> getCameraPreviewImageFormatSupported()
    {
        try {
            List<Integer> formats;
            if (camera != null) {
                formats = camera.getParameters().getSupportedPreviewFormats();
                for (Integer i : formats) {
                    LogHelper.i(TAG, "camera format supported: " + i);
                }
            } else {
                camera = Camera.open(cameraSelect);
                formats = camera.getParameters().getSupportedPreviewFormats();
                camera.release();
                camera = null;
            }

            return formats;
        } catch (RuntimeException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @return max size that device can record.
     */
    private Camera.Size getMaxEncoderSizeSupported()
    {
        if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_2160P)) {
            return camera.new Size(3840, 2160);
        } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_1080P)) {
            return camera.new Size(1920, 1080);
        } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_720P)) {
            return camera.new Size(1280, 720);
        } else {
            return camera.new Size(720, 480);
        }
    }

    private List<Camera.Size> getPreviewSize()
    {
        try {
            List<Camera.Size> previewSizes;
            Camera.Size maxSize;
            if (camera != null) {
                maxSize = getMaxEncoderSizeSupported();
                previewSizes = camera.getParameters().getSupportedPreviewSizes();
            } else {
                camera = Camera.open(cameraSelect);
                maxSize = getMaxEncoderSizeSupported();
                previewSizes = camera.getParameters().getSupportedPreviewSizes();
                camera.release();
                camera = null;
            }
            // Discard previews with higher dimension than the supported by the encoder
            Iterator<Camera.Size> iterator = previewSizes.iterator();
            while (iterator.hasNext()) {
                Camera.Size size = iterator.next();
                if (size.width > maxSize.width || size.height > maxSize.height) {
                    // LogHelper.i(TAG, size.width + "X" + size.height + ", not supported for encoder");
                    iterator.remove();
                }
            }
            return previewSizes;
        } catch (RuntimeException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Camera.Size> getPreviewSizeBack()
    {
        return previewSizeBack;
    }

    public List<Camera.Size> getPreviewSizeFront()
    {
        return previewSizeFront;
    }

    public VideoQuality getVideoQuality()
    {
        return quality;
    }

    public int getOrientation()
    {
        return orientation;
    }


    // Booleans

    public boolean isRunning()
    {
        return running;
    }

    public boolean isFrontCamera()
    {
        return isFrontCamera;
    }

    public boolean isPrepared()
    {
        return prepared;
    }

    public boolean isLanternEnabled()
    {
        return lanternEnable;
    }

    public boolean isFaceDetectionEnabled()
    {
        return faceDetectorCallback != null;
    }
}