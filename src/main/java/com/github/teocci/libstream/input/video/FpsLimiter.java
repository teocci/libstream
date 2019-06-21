package com.github.teocci.libstream.input.video;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-May-19
 */
public class FpsLimiter
{
    private long lastFrameTimestamp = 0L;

    public boolean limitFPS(int fps)
    {
        if (System.currentTimeMillis() - lastFrameTimestamp > 1000 / fps) {
            lastFrameTimestamp = System.currentTimeMillis();

            return false;
        }

        return true;
    }

    public void reset()
    {
        lastFrameTimestamp = 0;
    }
}
