package com.github.teocci.libstream.input.video;

import android.hardware.Camera;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public class FPSController
{
    private int ignoredFps = 0;
    private int cont = 0;

    public FPSController(int fps, Camera camera)
    {
        int[] fpsCamera = new int[2];
        camera.getParameters().getPreviewFpsRange(fpsCamera);
        ignoredFps = (fpsCamera[0] / 1000) / fps;
    }

    public boolean isValid()
    {
        if (cont++ < ignoredFps) {
            return false;
        }
        cont = 0;
        return true;
    }
}
