package com.github.teocci.libstream.enums;

import android.hardware.Camera;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public enum ColorEffect
{
    CLEAR,
    GREYSCALE,
    SEPIA,
    NEGATIVE,
    AQUA,
    POSTERIZE;

    public String getEffect()
    {
        switch (this) {
            case CLEAR:
                return Camera.Parameters.EFFECT_NONE;
            case GREYSCALE:
                return Camera.Parameters.EFFECT_MONO;
            case SEPIA:
                return Camera.Parameters.EFFECT_SEPIA;
            case NEGATIVE:
                return Camera.Parameters.EFFECT_NEGATIVE;
            case AQUA:
                return Camera.Parameters.EFFECT_AQUA;
            case POSTERIZE:
                return Camera.Parameters.EFFECT_POSTERIZE;
            default:
                return null;
        }
    }
}