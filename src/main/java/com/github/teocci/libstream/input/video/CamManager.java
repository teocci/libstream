package com.github.teocci.libstream.input.video;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.SurfaceView;
import android.view.TextureView;

import com.github.teocci.libstream.enums.CameraFacing;
import com.github.teocci.libstream.enums.ColorEffect;
import com.github.teocci.libstream.exceptions.CameraInUseException;
import com.github.teocci.libstream.exceptions.ConfNotSupportedException;
import com.github.teocci.libstream.interfaces.video.CameraSinker;
import com.github.teocci.libstream.utils.LogHelper;
import com.github.teocci.libstream.utils.YUVUtil;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
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

public class CamManager implements Camera.PreviewCallback
{
    private static String TAG = LogHelper.makeLogTag(CamManager.class);

    private Camera camera = null;

    private CameraSinker cameraSinker;

    private SurfaceView surfaceView;
    private TextureView textureView;
    private SurfaceTexture surfaceTexture;

    private volatile boolean prepared = false;

    private boolean running = false;

    private boolean lanternEnable = false;
    private boolean isFrontCamera = false;

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

    public CamManager(SurfaceView surfaceView, CameraSinker cameraSinker)
    {
        this.surfaceView = surfaceView;
        this.cameraSinker = cameraSinker;
        init(surfaceView.getContext());
    }

    public CamManager(TextureView textureView, CameraSinker cameraSinker)
    {
        this.textureView = textureView;
        this.cameraSinker = cameraSinker;
        init(textureView.getContext());
    }

    public CamManager(SurfaceTexture surfaceTexture, Context context)
    {
        this.surfaceTexture = surfaceTexture;
        init(context);
    }

    private void init(Context context)
    {
        if (context.getResources().getConfiguration().orientation == ORIENTATION_PORTRAIT) {
            orientation = 90;
        }

        cameraSelect = selectCameraFront();
        previewSizeFront = getPreviewSize();
        cameraSelect = selectCameraBack();
        previewSizeBack = getPreviewSize();
    }

    public void prepare(int width, int height, int fps, int imageFormat)
    {
        this.quality.width = width;
        this.quality.height = height;
        this.quality.fps = fps;
        this.imageFormat = imageFormat;
        prepared = true;
        LogHelper.e(TAG, "prepare called");
    }

    public void prepare(VideoQuality quality, int imageFormat)
    {
        this.quality.width = quality.width;
        this.quality.height = quality.height;
        this.quality.fps = quality.fps;
        this.imageFormat = imageFormat;
        prepared = true;
        LogHelper.e(TAG, "prepare with VideoQuality called");
    }

    public void prepare()
    {
        prepare(VideoQuality.DEFAULT, imageFormat);
    }

    public void start(@CameraFacing int cameraFacing)
    {
        start(cameraFacing, quality.width, quality.height);
    }

    public void start(@CameraFacing int cameraFacing, int width, int height)
    {
        LogHelper.e(TAG, "start with cam facing");
        quality.width = width;
        quality.height = height;
        cameraSelect = cameraFacing == CAMERA_FACING_BACK ? selectCameraBack() : selectCameraFront();
        start();
    }

    public void start()
    {
        if (!configSupported()) {
            throw new ConfNotSupportedException("This camera resolution cant be opened");
        }
        LogHelper.e(TAG, "start");

        handlerThread = new HandlerThread("cameraThread");
        handlerThread.start();

        handler = new Handler(handlerThread.getLooper());
        handler.post(() -> {
            LogHelper.e(TAG, "start handler");
            yuvBuffer = new byte[quality.width * quality.height * 3 / 2];
            YUVUtil.preAllocateRotateBuffers(yuvBuffer.length);
            if (imageFormat == ImageFormat.NV21) {
                YUVUtil.preAllocateNv21Buffers(yuvBuffer.length);
            } else {
                YUVUtil.preAllocateYv12Buffers(yuvBuffer.length);
            }

            if (camera == null && prepared) {
                try {
                    camera = Camera.open(cameraSelect);
                    Camera.CameraInfo info = new Camera.CameraInfo();
                    Camera.getCameraInfo(cameraSelect, info);
                    isFrontCamera = info.facing == CAMERA_FACING_FRONT;

                    Camera.Parameters parameters = camera.getParameters();
                    parameters.setPreviewSize(quality.width, quality.height);
                    parameters.setPreviewFormat(imageFormat);
                    int[] range = adaptFpsRange(quality.fps, parameters.getSupportedPreviewFpsRange());
                    parameters.setPreviewFpsRange(range[0], range[1]);

                    List<String> supportedFocusModes = parameters.getSupportedFocusModes();
                    if (supportedFocusModes != null && !supportedFocusModes.isEmpty()) {
                        if (supportedFocusModes.contains(FOCUS_MODE_CONTINUOUS_PICTURE)) {
                            parameters.setFocusMode(FOCUS_MODE_CONTINUOUS_PICTURE);
                        } else if (supportedFocusModes.contains(FOCUS_MODE_AUTO)) {
                            parameters.setFocusMode(FOCUS_MODE_AUTO);
                        } else {
                            parameters.setFocusMode(supportedFocusModes.get(0));
                        }
                    }

                    camera.setParameters(parameters);
                    camera.setDisplayOrientation(orientation);
                    if (surfaceView != null) {
                        camera.setPreviewDisplay(surfaceView.getHolder());
                        camera.addCallbackBuffer(yuvBuffer);
                        camera.setPreviewCallbackWithBuffer(CamManager.this);
                    } else if (textureView != null) {
                        camera.setPreviewTexture(textureView.getSurfaceTexture());
                        camera.addCallbackBuffer(yuvBuffer);
                        camera.setPreviewCallbackWithBuffer(CamManager.this);
                    } else {
                        camera.setPreviewTexture(surfaceTexture);
                    }
                    camera.startPreview();
                    running = true;
                    LogHelper.e(TAG, quality.width + "X" + quality.height);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                LogHelper.e(TAG, "CamManager: " + camera + " prepared: " + prepared);
                LogHelper.e(TAG, "CamManager need be prepared, CamManager not enabled");
                if (camera != null && prepared) {
                    stop();
                    prepared = true;
                    start();
                }
            }
            LogHelper.e(TAG, "end handler");
        });
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

    public void handleZoom(int newZoom)
    {
        if (camera != null && running) {
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
        }
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

    private int selectCameraBack()
    {
        int number = Camera.getNumberOfCameras();
        for (int i = 0; i < number; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == CAMERA_FACING_BACK) {
                return i;
            } else {
                cameraSelect = i;
            }
        }
        return cameraSelect;
    }

    private int selectCameraFront()
    {
        int number = Camera.getNumberOfCameras();
        for (int i = 0; i < number; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == CAMERA_FACING_FRONT) {
                return i;
            } else {
                cameraSelect = i;
            }
        }
        return cameraSelect;
    }

    public void stop()
    {
        if (camera != null) {
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.setPreviewCallbackWithBuffer(null);
            camera.release();
            camera = null;
            if (surfaceView != null) {
                clearSurface(surfaceView.getHolder());
            } else if (textureView != null) {
                clearSurface(textureView.getSurfaceTexture());
            } else {
                clearSurface(surfaceTexture);
            }
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

    /**
     * Clear data from surface using opengl
     */
    private void clearSurface(Object texture)
    {
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        egl.eglInitialize(display, null);

        int[] attributeList = {
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_RENDERABLE_TYPE,
                EGL10.EGL_WINDOW_BIT,
                EGL10.EGL_NONE, 0,
                // Placeholder for recordable [@-3]
                EGL10.EGL_NONE
        };

        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        egl.eglChooseConfig(display, attributeList, configs, configs.length, numConfigs);
        EGLConfig config = configs[0];
        EGLContext context = egl.eglCreateContext(
                display,
                config,
                EGL10.EGL_NO_CONTEXT,
                new int[]{12440, 2, EGL10.EGL_NONE}
        );

        EGLSurface eglSurface = egl.eglCreateWindowSurface(
                display,
                config,
                texture,
                new int[]{EGL10.EGL_NONE}
        );

        egl.eglMakeCurrent(display, eglSurface, eglSurface, context);
        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        egl.eglSwapBuffers(display, eglSurface);
        egl.eglDestroySurface(display, eglSurface);
        egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
        egl.eglDestroyContext(display, context);
        egl.eglTerminate(display);
    }

    public boolean isRunning()
    {
        return running;
    }

    public boolean isPrepared()
    {
        return prepared;
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

    @Override
    public void onPreviewFrame(byte[] data, Camera camera)
    {
        if (isFrontCamera) data = YUVUtil.rotateNV21(data, quality.width, quality.height, 180);
        cameraSinker.onYUVData(data);
        camera.addCallbackBuffer(yuvBuffer);
    }

    /**
     * See: https://developer.android.com/reference/android/graphics/ImageFormat.html
     * to know name of constant values
     * Example: 842094169 -> YV12, 17 -> NV21
     */
    public List<Integer> getCameraPreviewImageFormatSupported()
    {
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
                    LogHelper.i(TAG, size.width + "X" + size.height + ", not supported for encoder");
                    iterator.remove();
                }
            }
            return previewSizes;
        } catch (RuntimeException re) {
            re.printStackTrace();
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

    public void switchCamera() throws CameraInUseException
    {
        if (camera != null) {
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
                    prepared = true;
                    start();
                    return;
                }
            }
        }
    }

    private boolean configSupported()
    {
        List<Camera.Size> previews;
        if (cameraSelect == selectCameraBack()) {
            previews = previewSizeBack;
        } else {
            previews = previewSizeFront;
        }
        if (previews != null) {
            for (Camera.Size size : previews) {
                if (size.width == quality.width && size.height == quality.height) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isLanternEnable()
    {
        return lanternEnable;
    }

    /**
     * @required: <uses-permission android:name="android.permission.FLASHLIGHT"/>
     */
    public void enableLantern()
    {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            List<String> supportedFlashModes = parameters.getSupportedFlashModes();
            if (supportedFlashModes != null && !supportedFlashModes.isEmpty()) {
                if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    camera.setParameters(parameters);
                    lanternEnable = true;
                } else {
                    LogHelper.e(TAG, "Lantern unsupported");
                }
            }
        }
    }

    /**
     * @required: <uses-permission android:name="android.permission.FLASHLIGHT"/>
     */
    public void disableLantern()
    {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(parameters);
            lanternEnable = false;
        }
    }
}