package com.github.teocci.libstream.utils.gl;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.IOException;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public class ImageStreamObject extends StreamObjectBase
{
    private static final String TAG = "ImageStreamObject";

    private int numFrames;
    private Bitmap imageBitmap;

    public ImageStreamObject() {}

    @Override
    public int getWidth()
    {
        return imageBitmap.getWidth();
    }

    @Override
    public int getHeight()
    {
        return imageBitmap.getHeight();
    }

    @Override
    public void resize(int width, int height)
    {
        imageBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
    }

    @Override
    public void recycle()
    {
        imageBitmap.recycle();
    }

    @Override
    public int getNumFrames()
    {
        return numFrames;
    }

    public Bitmap getImageBitmap()
    {
        return imageBitmap;
    }

    @Override
    public int updateFrame()
    {
        return 0;
    }

    public void load(Bitmap imageBitmap)
    {
        this.imageBitmap = imageBitmap;
        numFrames = 1;
        Log.i(TAG, "finish load image");
    }
}
