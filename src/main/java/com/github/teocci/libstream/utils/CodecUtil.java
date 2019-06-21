package com.github.teocci.libstream.utils;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.github.teocci.libstream.enums.FormatVideoEncoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.media.MediaCodecList.ALL_CODECS;
import static com.github.teocci.libstream.enums.FormatVideoEncoder.SURFACE;
import static com.github.teocci.libstream.enums.FormatVideoEncoder.YUV420PACKEDPLANAR;
import static com.github.teocci.libstream.enums.FormatVideoEncoder.YUV420PLANAR;
import static com.github.teocci.libstream.enums.FormatVideoEncoder.YUV420SEMIPLANAR;
import static com.github.teocci.libstream.utils.Utils.minAPI21;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2019-May-29
 */
public class CodecUtil
{
    private static String TAG = LogHelper.makeLogTag(CodecUtil.class);

    public static final String H264_MIME = "video/avc";         // H.264 Advanced Video Coding
    public static final String H265_MIME = "video/hevc";        // H.265 Advanced Video Coding
    public static final String AAC_MIME = "audio/mp4a-latm";

    public static final int MAX_INPUT_SIZE = 0;                // 0 as maximum size in bytes of a buffer of data
    public static final int DEFAULT_FPS = 30;                   // 30 as default frames per second of the stream.
    public static final int IFRAME_INTERVAL = 2;                // 2 seconds between I-frames

    public static final int FORCE_HARDWARE = 0x00;
    public static final int FORCE_SOFTWARE = 0x01;

    @RequiresApi(api = Build.VERSION_CODES.P)
    public static List<String> showAllCodecsInfo()
    {
        List<MediaCodecInfo> mediaCodecInfoList = getAllCodecs();
        List<String> infos = new ArrayList<>();
        StringBuilder info;
        for (MediaCodecInfo mediaCodecInfo : mediaCodecInfoList) {
            info = new StringBuilder("*********\n");
            info.append("* Name: ")
                    .append(mediaCodecInfo.getName())
                    .append("\n");
            for (String type : mediaCodecInfo.getSupportedTypes()) {
                info.append("* Mime-Type: ")
                        .append(type)
                        .append("\n");

                MediaCodecInfo.CodecCapabilities codecCapabilities = mediaCodecInfo.getCapabilitiesForType(type);
                info.append("* Max instances: ")
                        .append(codecCapabilities.getMaxSupportedInstances())
                        .append("\n");
                if (mediaCodecInfo.isEncoder()) {
                    info.append("### Encoder info\n");
                    MediaCodecInfo.EncoderCapabilities encoderCapabilities =
                            codecCapabilities.getEncoderCapabilities();
                    info.append("Complexity range: ")
                            .append(encoderCapabilities.getComplexityRange().getLower())
                            .append(" - ")
                            .append(encoderCapabilities.getComplexityRange().getUpper())
                            .append("\n");
                    info.append("Quality range: ")
                            .append(encoderCapabilities.getQualityRange().getLower())
                            .append(" - ")
                            .append(encoderCapabilities.getQualityRange().getUpper())
                            .append("\n");
                    info.append("CBR supported: ")
                            .append(encoderCapabilities.isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR))
                            .append("\n");
                    info.append("VBR supported: ")
                            .append(encoderCapabilities.isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR))
                            .append("\n");
                    info.append("CQ supported: ")
                            .append(encoderCapabilities.isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ))
                            .append("\n");
                    info.append("\n");
                } else {
                    info.append("### Decoder info\n");
                    info.append("\n");
                }

                if (codecCapabilities.colorFormats != null && codecCapabilities.colorFormats.length > 0) {
                    info.append("### Video info\n");
                    info.append("Supported colors: \n");
                    for (int color : codecCapabilities.colorFormats) { info.append(color + "\n"); }
                    for (MediaCodecInfo.CodecProfileLevel profile : codecCapabilities.profileLevels) {
                        info.append("Profile: ")
                                .append(profile.profile)
                                .append(", level: ")
                                .append(profile.level)
                                .append("\n");
                    }

                    MediaCodecInfo.VideoCapabilities videoCapabilities = codecCapabilities.getVideoCapabilities();
                    info.append("Bitrate range: ")
                            .append(videoCapabilities.getBitrateRange().getLower())
                            .append(" - ")
                            .append(videoCapabilities.getBitrateRange().getUpper())
                            .append("\n");
                    info.append("Frame rate range: ")
                            .append(videoCapabilities.getSupportedFrameRates().getLower())
                            .append(" - ")
                            .append(videoCapabilities.getSupportedFrameRates().getUpper())
                            .append("\n");
                    info.append("Width range: ")
                            .append(videoCapabilities.getSupportedWidths().getLower())
                            .append(" - ")
                            .append(videoCapabilities.getSupportedWidths().getUpper())
                            .append("\n");
                    info.append("Height range: ")
                            .append(videoCapabilities.getSupportedHeights().getLower())
                            .append(" - ")
                            .append(videoCapabilities.getSupportedHeights().getUpper())
                            .append("\n");
                    info.append("\n");
                } else {
                    info.append("### Audio info\n");
                    for (MediaCodecInfo.CodecProfileLevel profile : codecCapabilities.profileLevels) {
                        info.append("Profile: ")
                                .append(profile.profile)
                                .append(", level: ")
                                .append(profile.level)
                                .append("\n");
                    }

                    MediaCodecInfo.AudioCapabilities audioCapabilities = codecCapabilities.getAudioCapabilities();
                    info.append("Bitrate range: ")
                            .append(audioCapabilities.getBitrateRange().getLower())
                            .append(" - ")
                            .append(audioCapabilities.getBitrateRange().getUpper())
                            .append("\n");
                    info.append("Channels supported: ")
                            .append(audioCapabilities.getMaxInputChannelCount())
                            .append("\n");
                    try {
                        if (audioCapabilities.getSupportedSampleRates() != null
                                && audioCapabilities.getSupportedSampleRates().length > 0) {
                            info.append("Supported sample rate: \n");
                            for (int sr : audioCapabilities.getSupportedSampleRates()) { info.append(sr).append("\n"); }
                        }
                    } catch (Exception ignore) {}

                    info.append("\n");
                }
                info.append("Max instances: ")
                        .append(codecCapabilities.getMaxSupportedInstances())
                        .append("\n");
            }
            info.append("*********\n");

            infos.add(info.toString());
        }
        return infos;
    }

    public static List<MediaCodecInfo> getAllCodecs()
    {
        List<MediaCodecInfo> codecInfoList = new ArrayList<>();
        if (minAPI21()) {
            MediaCodecList codecList = new MediaCodecList(ALL_CODECS);
            MediaCodecInfo[] codecInfos = codecList.getCodecInfos();
            codecInfoList.addAll(Arrays.asList(codecInfos));
        } else {
            int count = MediaCodecList.getCodecCount();
            for (int i = 0; i < count; i++) {
                MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
                codecInfoList.add(codecInfo);
            }
        }
        return codecInfoList;
    }

    public static List<MediaCodecInfo> getAllHardwareEncoders(String mime)
    {
        List<MediaCodecInfo> codecInfoList = getAllEncoders(mime);
        List<MediaCodecInfo> hwCodecList = new ArrayList<>();
        for (MediaCodecInfo codecInfo : codecInfoList) {
            String name = codecInfo.getName().toLowerCase();
            if (!name.contains("omx.google") && !name.contains("sw")) {
                hwCodecList.add(codecInfo);
            }
        }

        return hwCodecList;
    }

    public static List<MediaCodecInfo> getAllHardwareDecoders(String mime)
    {
        List<MediaCodecInfo> codecInfoList = getAllDecoders(mime);
        List<MediaCodecInfo> hwCodecInfoList = new ArrayList<>();
        for (MediaCodecInfo codecInfo : codecInfoList) {
            String name = codecInfo.getName().toLowerCase();
            if (!name.contains("omx.google") && !name.contains("sw")) {
                hwCodecInfoList.add(codecInfo);
            }
        }

        return hwCodecInfoList;
    }

    public static List<MediaCodecInfo> getAllSoftwareEncoders(String mime)
    {
        List<MediaCodecInfo> codecInfoList = getAllEncoders(mime);
        List<MediaCodecInfo> swCodecInfoList = new ArrayList<>();
        for (MediaCodecInfo codecInfo : codecInfoList) {
            String name = codecInfo.getName().toLowerCase();
            if (name.contains("omx.google") || name.contains("sw")) {
                swCodecInfoList.add(codecInfo);
            }
        }

        return swCodecInfoList;
    }

    public static List<MediaCodecInfo> getAllSoftwareDecoders(String mime)
    {
        List<MediaCodecInfo> codecInfoList = getAllDecoders(mime);
        List<MediaCodecInfo> swCodecInfoList = new ArrayList<>();
        for (MediaCodecInfo codecInfo : codecInfoList) {
            String name = codecInfo.getName().toLowerCase();
            if (name.contains("omx.google") || name.contains("sw")) {
                swCodecInfoList.add(codecInfo);
            }
        }

        return swCodecInfoList;
    }

    public static List<MediaCodecInfo> getAllEncoders(String mime)
    {
        if (minAPI21()) {
            return getAllEncodersAPI21(mime);
        } else {
            return getAllEncodersAPI16(mime);
        }
    }

    public static List<MediaCodecInfo> getAllDecoders(String mime)
    {
        if (minAPI21()) {
            return getAllDecodersAPI21(mime);
        } else {
            return getAllDecodersAPI16(mime);
        }
    }

    /**
     * Gets a video encoder by mime. API 21+
     *
     * @param mime mine type of the encoder needed
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static List<MediaCodecInfo> getAllEncodersAPI21(String mime)
    {
        List<MediaCodecInfo> codecInfoList = new ArrayList<>();
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaCodecInfo[] codecInfos = codecList.getCodecInfos();
        for (MediaCodecInfo codecInfo : codecInfos) {
            LogHelper.i(TAG, String.format("Codec Name %s | is encoder: %s", codecInfo.getName(), codecInfo.isEncoder() ? "true" : "false"));
            if (!codecInfo.isEncoder()) continue;

            String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase(mime)) {
                    codecInfoList.add(codecInfo);
                }
            }
        }

        return codecInfoList;
    }


    /**
     * choose the video encoder by mime. API > 16
     */
    private static List<MediaCodecInfo> getAllEncodersAPI16(String mime)
    {
        List<MediaCodecInfo> codecInfoList = new ArrayList<>();
        int count = MediaCodecList.getCodecCount();
        for (int i = 0; i < count; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            LogHelper.i(TAG, String.format("Codec Name %s | is encoder: %s", codecInfo.getName(), codecInfo.isEncoder() ? "true" : "false"));
            if (!codecInfo.isEncoder()) continue;

            String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase(mime)) {
                    codecInfoList.add(codecInfo);
                }
            }
        }

        return codecInfoList;
    }

    /**
     * choose the video encoder by mime. API 21+
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static List<MediaCodecInfo> getAllDecodersAPI21(String mime)
    {
        List<MediaCodecInfo> codecInfoList = new ArrayList<>();
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaCodecInfo[] codecInfos = codecList.getCodecInfos();
        for (MediaCodecInfo codecInfo : codecInfos) {
            if (codecInfo.isEncoder()) continue;

            String[] mimeTypes = codecInfo.getSupportedTypes();
            for (String type : mimeTypes) {
                if (mime.equalsIgnoreCase(type)) {
                    codecInfoList.add(codecInfo);
                }
            }
        }

        return codecInfoList;
    }

    /**
     * choose the video encoder by mime. API > 16
     */
    private static List<MediaCodecInfo> getAllDecodersAPI16(String mime)
    {
        List<MediaCodecInfo> mediaCodecInfoList = new ArrayList<>();
        int count = MediaCodecList.getCodecCount();
        for (int i = 0; i < count; i++) {
            MediaCodecInfo mci = MediaCodecList.getCodecInfoAt(i);
            if (mci.isEncoder())continue;

            String[] types = mci.getSupportedTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase(mime)) {
                    mediaCodecInfoList.add(mci);
                }
            }
        }

        return mediaCodecInfoList;
    }


    /**
     * choose the video encoder by mime.
     */
    public static MediaCodecInfo getVideoCodecInfo(String mime, int force, FormatVideoEncoder formatCodec)
    {
        List<MediaCodecInfo> codecInfoList;
        if (force == FORCE_HARDWARE) {
            codecInfoList = getAllHardwareEncoders(mime);
        } else if (force == FORCE_SOFTWARE) {
            codecInfoList = getAllSoftwareEncoders(mime);
        } else {
            codecInfoList = getAllEncoders(mime);
        }
        for (MediaCodecInfo codecInfo : codecInfoList) {
            LogHelper.i(TAG, String.format("VideoEncoder %s", codecInfo.getName()));
            MediaCodecInfo.CodecCapabilities codecCapabilities = codecInfo.getCapabilitiesForType(mime);
            for (int color : codecCapabilities.colorFormats) {
                LogHelper.i(TAG, "Color supported: " + color);
                if (formatCodec == SURFACE) {
                    if (color == SURFACE.getFormatCodec()) return codecInfo;
                } else {
                    // Check if encoder support any yuv420 color
                    if (color == YUV420PLANAR.getFormatCodec() || color == YUV420SEMIPLANAR.getFormatCodec()) {
                        return codecInfo;
                    }
                }
            }
        }

        return null;
    }



    public static MediaCodecInfo getVideoCodecInfo(String mime, FormatVideoEncoder formatCodec)
    {
        List<MediaCodecInfo> codecInfoList = getAllEncoders(mime);

        for (MediaCodecInfo codecInfo : codecInfoList) {
            LogHelper.i(TAG, String.format("VideoEncoder %s", codecInfo.getName()));
            MediaCodecInfo.CodecCapabilities codecCapabilities = codecInfo.getCapabilitiesForType(mime);
            for (int color : codecCapabilities.colorFormats) {
                LogHelper.i(TAG, "Color supported: " + color);
                if (formatCodec == SURFACE) {
                    if (color == SURFACE.getFormatCodec()) return codecInfo;
                } else {
                    //check if encoder support any color formats 19 and 21
                    if (color == YUV420PLANAR.getFormatCodec() || color == YUV420SEMIPLANAR.getFormatCodec()) {
                        return codecInfo;
                    }
                }
            }
        }

        return null;
    }

    public static MediaCodecInfo getVideoCodecInfo(String mime)
    {
        List<MediaCodecInfo> codecInfoList = getAllEncoders(mime);

        int count = MediaCodecList.getCodecCount();
        for (int i = 0; i < count; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            MediaCodecInfo supportedCodecInfo = getSupportedVideoEncoder(mime, codecInfo);
            if (supportedCodecInfo == null) continue;

            return supportedCodecInfo;
        }

        return null;
    }

    public static MediaCodecInfo getSupportedVideoEncoder(String mime, MediaCodecInfo codecInfo)
    {
        String[] types = codecInfo.getSupportedTypes();
        LogHelper.i(TAG, "Supported VideoEncoder: " + Arrays.toString(types));
        for (String type : types) {
            if (type.equalsIgnoreCase(mime)) {
                LogHelper.e(TAG, String.format("videoEncoder %s type supported: %s", codecInfo.getName(), type));
                MediaCodecInfo.CodecCapabilities codecCapabilities = codecInfo.getCapabilitiesForType(mime);
                for (int color : codecCapabilities.colorFormats) {
                    LogHelper.e(TAG, "Color supported: " + color);
                    // Check if encoder support any yuv420 color
                    if (color == YUV420PLANAR.getFormatCodec()
                            || color == YUV420SEMIPLANAR.getFormatCodec()
                            || color == YUV420PACKEDPLANAR.getFormatCodec()) {
                        return codecInfo;
                    }
                }
            }
        }

        return null;
    }

    public static FormatVideoEncoder getColorFormat(MediaCodecInfo mediaCodecInfo)
    {
        for (int color : mediaCodecInfo.getCapabilitiesForType(H264_MIME).colorFormats) {
            LogHelper.e(TAG, "Encoder color format: " + color);
            if (color == YUV420PLANAR.getFormatCodec()) {
                return YUV420PLANAR;
            } else if (color == YUV420SEMIPLANAR.getFormatCodec()) {
                return YUV420SEMIPLANAR;
            } else if (color == YUV420PACKEDPLANAR.getFormatCodec()) {
                return YUV420PACKEDPLANAR;
            }
        }

        return null;
    }

    /**
     * Case 8 utilizing String.regionMatches()
     *
     * @param src string
     * @param sub substring
     * @return true is substring exits
     */
    public static boolean containsIgnore(String src, String sub)
    {
        if (src == null || sub == null) {
            return false;
        }

        final int length = sub.length();
        final int max = src.length() - length;
        for (int i = 0; i <= max; i++) {
            if (src.regionMatches(true, i, sub, 0, length)) return true;
        }

        return false;
    }
}
