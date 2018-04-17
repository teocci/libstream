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
    public final String mime = "audio/mp4a-latm";

    public int sampling = 0;
    public int bitrate = 0;
    public int channel = 0;

    /**
     * Represents a currentQuality for a video stream.
     */
    public AudioQuality() {}

    /**
     * Represents a currentQuality for an audio stream.
     *
     * @param quality The audio quality to be copy
     */
    public AudioQuality(AudioQuality quality)
    {
        this.sampling = quality.sampling;
        this.bitrate = quality.bitrate;
        this.channel = quality.channel;
    }

    /**
     * Represents a currentQuality for an audio stream.
     *
     * @param sampling The sampling rate
     * @param bitrate    The bitrate in bit per seconds
     */
    public AudioQuality(int sampling, int bitrate)
    {
        this.sampling = sampling;
        this.bitrate = bitrate;
    }

    /**
     * Represents a currentQuality for an audio stream.
     *
     * @param sampling The sampling rate
     * @param bitrate  The bitrate in bit per seconds
     * @param channel  The audio channel
     */
    public AudioQuality(int sampling, int bitrate, int channel)
    {
        this.sampling = sampling;
        this.bitrate = bitrate;
        this.channel = channel;
    }

    @Override
    public String toString()
    {
        return "AudioQuality{" +
                "sampling=" + sampling +
                ", bitrate=" + bitrate +
                ", channel=" + channel +
                '}';
    }

    public boolean equals(AudioQuality quality)
    {
        return quality != null &&
                quality.sampling == this.sampling &&
                quality.bitrate == this.bitrate &&
                quality.channel == this.channel;
    }

    public static AudioQuality parseQuality(String str)
    {
        AudioQuality quality = new AudioQuality(DEFAULT);
        if (str != null) {
            String[] config = str.split("-");
            try {
                quality.sampling = Integer.parseInt(config[1]);
                quality.bitrate = Integer.parseInt(config[0]) * 1000; // conversion to bit/s
                quality.channel = Integer.parseInt(config[2]);
            } catch (IndexOutOfBoundsException ignore) {}
        }
        return quality;
    }
}
