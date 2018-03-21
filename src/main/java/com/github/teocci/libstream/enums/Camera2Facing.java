package com.github.teocci.libstream.enums;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.IntDef;

import static android.hardware.camera2.CameraMetadata.LENS_FACING_BACK;
import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
@IntDef({LENS_FACING_BACK, LENS_FACING_FRONT /*, LENS_FACING_EXTERNAL*/})
public @interface Camera2Facing {}