package com.github.teocci.libstream.input.audio;

import com.github.teocci.libstream.utils.LogHelper;

/**
 * A class that represents the currentQuality of an audio stream.
 */
public class AudioQuality
{
    private final static String TAG = LogHelper.makeLogTag(AudioQuality.class);

    public final static int MONO = 1;
    public final static int STEREO = 2;

    /**
     * Default audio stream currentQuality.
     */
//    public final static AudioQuality DEFAULT = new AudioQuality(8000, 32000, 0);
    public final static AudioQuality DEFAULT = new AudioQuality(44100, 128 * 1024, STEREO);

    // default parameters for encoder
    public String mime = "audio/mp4a-latm";

    public int sampleRate = 0;
    public int bitRate = 0;
    public int channel = 0;

    /**
     * Represents a currentQuality for a video stream.
     */
    public AudioQuality() {}

    /**
     * Represents a currentQuality for an audio stream.
     *
     * @param sampleRate The sampling rate
     * @param bitRate    The bitrate in bit per seconds
     */
    public AudioQuality(int sampleRate, int bitRate)
    {
        this.sampleRate = sampleRate;
        this.bitRate = bitRate;
    }

    /**
     * Represents a currentQuality for an audio stream.
     *
     * @param sampling The sampling rate
     * @param bitRate  The bitrate in bit per seconds
     * @param channel  The audio channel
     */
    public AudioQuality(int sampling, int bitRate, int channel)
    {
        this.sampleRate = sampling;
        this.bitRate = bitRate;
        this.channel = channel;
    }

    public boolean equals(AudioQuality quality)
    {
        return quality != null &&
                (quality.sampleRate == this.sampleRate &&
                        quality.bitRate == this.bitRate &&
                        quality.channel == this.channel);
    }

    public AudioQuality clone()
    {
        return new AudioQuality(sampleRate, bitRate, channel);
    }

    public static AudioQuality parseQuality(String str)
    {
        AudioQuality quality = DEFAULT.clone();
        if (str != null) {
            String[] config = str.split("-");
            try {
                quality.sampleRate = Integer.parseInt(config[1]);
                quality.bitRate = Integer.parseInt(config[0]) * 1000; // conversion to bit/s
                quality.channel = Integer.parseInt(config[2]);
            } catch (IndexOutOfBoundsException ignore) {}
        }
        return quality;
    }

    @Override
    public String toString()
    {
        return "AudioQuality{" +
                "sampleRate=" + sampleRate +
                ", bitRate=" + bitRate +
                ", channel=" + channel +
                '}';
    }
}
