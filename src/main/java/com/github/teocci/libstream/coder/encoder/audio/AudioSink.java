package com.github.teocci.libstream.coder.encoder.audio;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public class AudioSink
{
    private byte[] pcmBuffer;
    private int size;

    public AudioSink(byte[] pcmBuffer, int size)
    {
        this.pcmBuffer = pcmBuffer;
        this.size = size;
    }

    public byte[] getPCMBuffer()
    {
        return pcmBuffer;
    }

    public void setPCMBuffer(byte[] pcmBuffer)
    {
        this.pcmBuffer = pcmBuffer;
    }

    public int getSize()
    {
        return size;
    }

    public void setSize(int size)
    {
        this.size = size;
    }
}
