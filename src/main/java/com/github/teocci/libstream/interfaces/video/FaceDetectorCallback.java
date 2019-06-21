package com.github.teocci.libstream.interfaces.video;

import android.hardware.Camera;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2019-May-29
 */
public interface FaceDetectorCallback
{
    void onGetFaces(Camera.Face[] faces);
}
