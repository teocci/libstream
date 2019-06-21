package com.github.teocci.libstream.input.video;

import android.hardware.Camera;
import android.hardware.Camera.Size;

import com.github.teocci.libstream.utils.LogHelper;

import java.util.Iterator;
import java.util.List;

/**
 * A class that represents the quality of a video stream.
 * It contains the resolution, the fps (in fps) and the bitrate (in bps) of the stream.
 * <p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public class VideoQuality
{
    private final static String TAG = LogHelper.makeLogTag(VideoQuality.class);

    /**
     * Default video stream quality.
     */
    public final static VideoQuality DEFAULT = new VideoQuality(720, 480, 30, 1200 * 1024);

    public int fps = 0;
    public int bitrate = 0;
    public int width = 0;
    public int height = 0;

    /**
     * Creates an undefined quality for a video stream.
     */
    public VideoQuality() {}

    /**
     * Creates a quality instance base on a vertical and horizontal resolution for a video stream.
     *
     * @param width  The horizontal resolution in px.
     * @param height The vertical resolution in px.
     */
    public VideoQuality(int width, int height)
    {
        this.width = width;
        this.height = height;
    }

    /**
     * Creates a quality instance base on a vertical and horizontal resolution, fps, and bitrate for a video stream.
     *
     * @param width   The horizontal resolution in px.
     * @param height  The vertical resolution in px.
     * @param fps     The fps in frames per second of the stream.
     * @param bitrate The bitrate in bit per seconds
     */
    public VideoQuality(int width, int height, int fps, int bitrate)
    {
        this.fps = fps;
        this.bitrate = bitrate;
        this.width = width;
        this.height = height;
    }

    public boolean equals(VideoQuality quality)
    {
        if (quality == null) return false;
        return (quality.width == this.width &&
                quality.height == this.height &&
                quality.fps == this.fps &&
                quality.bitrate == this.bitrate);
    }

    public VideoQuality clone()
    {
        return new VideoQuality(width, height, fps, bitrate);
    }

    public static VideoQuality parseQuality(String str)
    {
        VideoQuality quality = DEFAULT.clone();
        if (str != null) {
            String[] config = str.split("-");
            try {
                quality.bitrate = Integer.parseInt(config[0]) * 1000; // conversion to bit/s
                quality.fps = Integer.parseInt(config[1]);
                quality.width = Integer.parseInt(config[2]);
                quality.height = Integer.parseInt(config[3]);
            } catch (IndexOutOfBoundsException ignore) {}
        }

        return quality;
    }

    public String toString()
    {
        return width + "x" + height + " px, " + fps + " fps, " + bitrate / 1000 + " kbps";
    }

    /**
     * Checks if the requested resolution is supported by the camera.
     * If not, it modifies it by supported parameters.
     */
    public static VideoQuality closestSupportedResolution(Camera.Parameters parameters, VideoQuality quality)
    {
        VideoQuality v = quality.clone();
        int minDist = Integer.MAX_VALUE;

        StringBuilder sb = new StringBuilder("Supported resolutions: ");
        List<Size> supportedSizes = parameters.getSupportedPreviewSizes();
        for (Iterator<Size> it = supportedSizes.iterator(); it.hasNext(); ) {
            Size size = it.next();

            sb.append(size.width);
            sb.append("x");
            sb.append(size.height);
            sb.append((it.hasNext() ? ", " : ""));

            int dist = Math.abs(quality.width - size.width);
            dist += Math.abs(quality.height - size.height);
            if (dist < minDist) {
                minDist = dist;
                v.width = size.width;
                v.height = size.height;
            }
        }

        LogHelper.e(TAG, sb.toString());
        if (quality.width != v.width || quality.height != v.height) {
            LogHelper.e(TAG, "Resolution modified: " + quality.width + "x" + quality.height +
                    "->" + v.width + "x" + v.height);
        }

        return v;
    }

    public static int[] maximumSupportedFramerate(Camera.Parameters parameters)
    {
        int[] maxFps = new int[]{0, 0};
        List<int[]> supportedFpsRanges = parameters.getSupportedPreviewFpsRange();
        StringBuilder sb = new StringBuilder("Supported frame rates: ");
        for (Iterator<int[]> it = supportedFpsRanges.iterator(); it.hasNext(); ) {
            int[] interval = it.next();
            // Intervals are returned as integers, for example "29970" means "29.970" FPS.
            sb.append(interval[0] / 1000);
            sb.append("-");
            sb.append(interval[1] / 1000);
            sb.append("fps");
            sb.append((it.hasNext() ? ", " : ""));

            if (interval[1] > maxFps[1] || (interval[0] > maxFps[0] && interval[1] == maxFps[1])) {
                maxFps = interval;
            }
        }
        LogHelper.v(TAG, sb.toString());
        return maxFps;
    }
}
