package com.github.teocci.libstream.interfaces.video;


import com.github.teocci.libstream.input.video.Frame;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */

public interface CameraSinker
{
    void onYUVData(byte[] buffer);

    void onYUVData(Frame frame);
}
