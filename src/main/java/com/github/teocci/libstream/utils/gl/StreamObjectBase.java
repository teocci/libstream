package com.github.teocci.libstream.utils.gl;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */

public abstract class StreamObjectBase
{
    public abstract int getWidth();

    public abstract int getHeight();

    public abstract int updateFrame();

    public abstract void resize(int width, int height);

    public abstract void recycle();

    public abstract int getNumFrames();
}
