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
    public static byte[] mixPCM(byte[] pcmA, byte[] pcmB)
    {
        int lenA = pcmA.length;
        int lenB = pcmB.length;
        byte[] pcmL;
        byte[] pcmS;
        int lenL; // Length of the longest
        int lenS; // Length of the shortest
        if (lenB > lenA) {
            lenL = lenA;
            pcmL = pcmA;
            lenS = lenB;
            pcmS = pcmB;
        } else {
            lenL = lenB;
            pcmL = pcmB;
            lenS = lenA;
            pcmS = pcmA;
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
