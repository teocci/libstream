package com.github.teocci.libstream.input.audio;

import com.github.teocci.libstream.utils.LogHelper;

/**
 * A class that represents the currentQuality of an audio stream.
 */
public class AudioQuality
{
    private final static String TAG = LogHelper.makeLogTag(AudioQuality.class);

    /**
     * supported sampleRates.
     **/
    private static final int[] AUDIO_SAMPLING_RATES = {
            96000, // 0
            88200, // 1
            64000, // 2
            48000, // 3
            44100, // 4
            32000, // 5
            24000, // 6
            22050, // 7
            16000, // 8
            12000, // 9
            11025, // 10
            8000,  // 11
            7350,  // 12
            -1,   // 13
            -1,   // 14
            -1,   // 15
    };

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
     * Represents a undefined quality for a video stream.
     */
    public AudioQuality() {}

    /**
     * Represents a quality for an audio stream based on sampling rate and bitrate.
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
     * Represents a quality for an audio stream based on sampling rate, bitrate and audio channels.
     *
     * @param sampleRate The sampling rate
     * @param bitRate    The bitrate in bit per seconds
     * @param channel    The audio channel
     */
    public AudioQuality(int sampleRate, int bitRate, int channel)
    {
        this.sampleRate = sampleRate;
        this.bitRate = bitRate;
        this.channel = channel;
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


    public int getSampleRateIndex()
    {
        int index = 0;
        for (int sampleRate : AUDIO_SAMPLING_RATES) {
            if (this.sampleRate == sampleRate) {
                return index;
            }
            index++;
        }

        return -1;
    }
}
