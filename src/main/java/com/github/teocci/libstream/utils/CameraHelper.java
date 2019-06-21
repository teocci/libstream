package com.github.teocci.libstream.utils;

import android.content.Context;
import android.opengl.GLES20;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.WindowManager;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422Flexible;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422PackedPlanar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422PackedSemiPlanar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422Planar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422SemiPlanar;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV444Flexible;
import static android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV444Interleaved;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2019-May-29
 */
public class CameraHelper
{
    public static final int YUV420FLEXIBLE = COLOR_FormatYUV420Flexible;
    public static final int YUV420PLANAR = COLOR_FormatYUV420Planar;
    public static final int YUV420SEMIPLANAR = COLOR_FormatYUV420SemiPlanar;
    public static final int YUV420PACKEDPLANAR = COLOR_FormatYUV420PackedPlanar;
    public static final int YUV420PACKEDSEMIPLANAR = COLOR_FormatYUV420PackedSemiPlanar;
    public static final int YUV422FLEXIBLE = COLOR_FormatYUV422Flexible;
    public static final int YUV422PLANAR = COLOR_FormatYUV422Planar;
    public static final int YUV422SEMIPLANAR = COLOR_FormatYUV422SemiPlanar;
    public static final int YUV422PACKEDPLANAR = COLOR_FormatYUV422PackedPlanar;
    public static final int YUV422PACKEDSEMIPLANAR = COLOR_FormatYUV422PackedSemiPlanar;
    public static final int YUV444FLEXIBLE = COLOR_FormatYUV444Flexible;
    public static final int YUV444INTERLEAVED = COLOR_FormatYUV444Interleaved;
    public static final int SURFACE = COLOR_FormatSurface;
    // Take first valid color for encoder (YUV420PLANAR, YUV420SEMIPLANAR or YUV420PACKEDPLANAR)
    public static final int YUV420DYNAMICAL = -1;

    // X, Y, Z, U, V
    private static final float[] verticesData = {
            -1f, -1f, 0f, 0f, 0f,
            1f, -1f, 0f, 1f, 0f,
            -1f, 1f, 0f, 0f, 1f,
            1f, 1f, 0f, 1f, 1f,
    };

    public static float[] getVerticesData()
    {
        return verticesData;
    }

    public static int getCameraOrientation(Context context)
    {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            int orientation = windowManager.getDefaultDisplay().getRotation();
            switch (orientation) {
                case Surface.ROTATION_0: // Portrait
                    return 90;
                case Surface.ROTATION_90: // Landscape
                    return 0;
                case Surface.ROTATION_180: // Reverse portrait
                    return 270;
                case Surface.ROTATION_270: // Reverse landscape
                    return 180;
                default:
                    return 0;
            }
        } else {
            return 0;
        }
    }

    public static float getFingerSpacing(MotionEvent event)
    {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
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
}
