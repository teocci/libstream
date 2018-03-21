package com.github.teocci.libstream.enums;

import android.support.annotation.IntDef;

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
import static android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
@IntDef({CAMERA_FACING_BACK, CAMERA_FACING_FRONT})
public @interface CameraFacing {}