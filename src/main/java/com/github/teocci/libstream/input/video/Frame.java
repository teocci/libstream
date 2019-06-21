package com.github.teocci.libstream.input.video;

import android.graphics.ImageFormat;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-May-17
 */
public class Frame
{
    private byte[] buffer;
    private int orientation;
    private boolean flip;
    private int format;

    /**
     * @param buffer      the contents of the preview frame in the format defined
     *                    by {@link android.graphics.ImageFormat}, the default will be the NV21 format.
     * @param orientation the angle that the picture will be rotated clockwise. Valid values are 0, 90, 180, and 270.
     * @param flip        if the frame will be flipped or not
     * @param format      either NV21 or YV12 are supported
     */
    public Frame(byte[] buffer, int orientation, boolean flip, int format)
    {
        this.buffer = buffer;
        this.orientation = orientation;
        this.flip = flip;
        this.format = format;
    }

    public void setBuffer(byte[] buffer)
    {
        this.buffer = buffer;
    }

    public void setOrientation(int orientation)
    {
        this.orientation = orientation;
    }

    public void setFlip(boolean flip)
    {
        this.flip = flip;
    }

    public void setFormat(int format)
    {
        this.format = format;
    }


    public byte[] getBuffer()
    {
        return buffer;
    }

    public int getFormat()
    {
        return format;
    }

    public int getOrientation()
    {
        return orientation;
    }


    public boolean isFlip()
    {
        return flip;
    }

    public boolean isYV12()
    {
        return format == ImageFormat.YV12;
    }
}
