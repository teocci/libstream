package com.github.teocci.libstream.utils;

import android.os.Build;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Jan-16
 */

public class Utils
{
    public static boolean minAPI19()
    {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    public static boolean minAPI21()
    {
        return Build.VERSION.SDK_INT >= 21;
    }

    public static boolean minAPI18()
    {
        return Build.VERSION.SDK_INT >= 18;
    }
}
