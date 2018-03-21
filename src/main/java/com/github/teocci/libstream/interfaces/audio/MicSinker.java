package com.github.teocci.libstream.interfaces.audio;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */

public interface MicSinker
{
    void onPCMData(byte[] buffer, int size);
}
