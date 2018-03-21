package com.github.teocci.libstream.utils.gl.gif;

import android.content.Context;
import android.content.res.Resources;

import java.io.InputStream;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public class ReadGifFromRawFolder
{
    public static InputStream getStringFromRaw(Context context, int id)
    {
        Resources r = context.getResources();
        return r.openRawResource(id);
    }
}
