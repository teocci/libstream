package com.github.teocci.libstream.utils;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public class PCMUtil
{
    /**
     * See https://stackoverflow.com/a/33255658/5372008
     */
    public static byte[] mixPCM(byte[] pcm1, byte[] pcm2)
    {
        int len1 = pcm1.length;
        int len2 = pcm2.length;
        byte[] pcmL;
        byte[] pcmS;
        int lenL; // Length of the longest
        int lenS; // Length of the shortest
        if (len2 > len1) {
            lenL = len1;
            pcmL = pcm1;
            lenS = len2;
            pcmS = pcm2;
        } else {
            lenL = len2;
            pcmL = pcm2;
            lenS = len1;
            pcmS = pcm1;
        }
        for (int idx = 0; idx < lenL; idx++) {
            int sample;
            if (idx >= lenS) {
                sample = pcmL[idx];
            } else {
                sample = pcmL[idx] + pcmS[idx];
            }
            sample = (int) (sample * .71);
            if (sample > 127) sample = 127;
            if (sample < -128) sample = -128;
            pcmL[idx] = (byte) sample;
        }
        return pcmL;
    }
}
