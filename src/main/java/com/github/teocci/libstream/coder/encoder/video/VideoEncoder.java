package com.github.teocci.libstream.coder.encoder.video;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.Pair;
import android.view.Surface;

import com.github.teocci.libstream.enums.VideoEncodingFormat;
import com.github.teocci.libstream.input.video.VideoQuality;
import com.github.teocci.libstream.interfaces.video.CameraSinker;
import com.github.teocci.libstream.interfaces.video.H264Sinker;
import com.github.teocci.libstream.utils.LogHelper;
import com.github.teocci.libstream.utils.YUVUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
import static android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED;
import static android.media.MediaCodec.PARAMETER_KEY_VIDEO_BITRATE;
import static com.github.teocci.libstream.enums.VideoEncodingFormat.SURFACE;
import static com.github.teocci.libstream.enums.VideoEncodingFormat.YUV420DYNAMICAL;
import static com.github.teocci.libstream.enums.VideoEncodingFormat.YUV420PACKEDPLANAR;
import static com.github.teocci.libstream.enums.VideoEncodingFormat.YUV420PLANAR;
import static com.github.teocci.libstream.enums.VideoEncodingFormat.YUV420SEMIPLANAR;
import static com.github.teocci.libstream.utils.Utils.minAPI21;
import static com.github.teocci.libstream.utils.Utils.minAPI19;
import static com.github.teocci.libstream.utils.YUVUtil.NV21toYUV420byColor;
import static com.github.teocci.libstream.utils.rtsp.RtpConstants.CLOCK_VIDEO_FREQUENCY;
import static com.github.teocci.libstream.utils.rtsp.RtpConstants.PAYLOAD_TYPE;

/**
 * This class need use same resolution, fps and imageFormat that Camera1ApiManagerGl
 * <p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */

public class VideoEncoder implements CameraSinker
{
    private static String TAG = LogHelper.makeLogTag(VideoEncoder.class);

    private MediaCodec videoEncoder;
    private MediaCodec.BufferInfo videoInfo = new MediaCodec.BufferInfo();

    private Thread thread;
    private Thread threadRotate;
    private Thread threadColor;

    private H264Sinker h264Sinker;

    private long presentTimeUs;

    private boolean running = false;
    private boolean hasPSPair = false;
    private boolean hardwareRotation = false;

    // Surface to buffer encoder
    private Surface inputSurface;

    // Buffer to buffer, 3 queue to optimize frames on rotation
    private BlockingQueue<byte[]> queueEncode = new LinkedBlockingQueue<>(30);
    private BlockingQueue<byte[]> queueRotate = new LinkedBlockingQueue<>(30);
    private BlockingQueue<byte[]> queueColor = new LinkedBlockingQueue<>(30);

    private int imageFormat = ImageFormat.NV21;
    private final Object sync = new Object();

    // Default parameters for encoder
    private String mime = "video/avc";
    //    private int width = 640;
//    private int height = 480;
//    private int fps = 30;
//    private int bitRate = 1200 * 1024; // In kbps
    private VideoQuality quality = VideoQuality.DEFAULT;

    private int rotation = 90;
    private VideoEncodingFormat formatCodec = YUV420DYNAMICAL;

    // For disable video
    private boolean sendBlackImage = false;
    private byte[] blackImage;

    public VideoEncoder(H264Sinker h264Sinker)
    {
        this.h264Sinker = h264Sinker;
    }

    /**
     * Prepare encoder with custom parameters
     */
    public boolean prepare(
            VideoQuality quality,
            int rotation,
            boolean hardwareRotation,
            VideoEncodingFormat formatCodec
    )
    {
        this.quality.width = quality.width;
        this.quality.height = quality.height;
        this.quality.fps = quality.fps;
        this.quality.bitrate = quality.bitrate;
        this.rotation = rotation;
        this.hardwareRotation = hardwareRotation;
        this.formatCodec = formatCodec;

        MediaCodecInfo encoder = minAPI21() ? getVideoEncoderAPI21(mime) : getVideoEncoder(mime);

        try {
            if (encoder != null) {
                videoEncoder = MediaCodec.createByCodecName(encoder.getName());
                if (this.formatCodec == YUV420DYNAMICAL) {
                    this.formatCodec = getColorFormat(encoder);
                    if (this.formatCodec == null) {
                        LogHelper.e(TAG, "YUV420 dynamical choose failed");
                        return false;
                    }
                }
            } else {
                LogHelper.e(TAG, "valid encoder not found");
                return false;
            }

            MediaFormat videoFormat;
            // We need to swap width and height in rotation 90 or 270
            // For correct encoding resolution
            if (!hardwareRotation && (rotation == 90 || rotation == 270)) {
                videoFormat = MediaFormat.createVideoFormat(mime, quality.height, quality.width);
            } else {
                videoFormat = MediaFormat.createVideoFormat(mime, quality.width, quality.height);
            }

            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, this.formatCodec.getFormatCodec());
            videoFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);
            videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, quality.bitrate);
            videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, quality.fps);
            videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);

            if (hardwareRotation) {
                videoFormat.setInteger("rotation-degrees", rotation);
            }

            videoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            running = false;
            if (minAPI19() && formatCodec == SURFACE) {
                inputSurface = videoEncoder.createInputSurface();
            }
            prepareBlackImage();
            return true;
        } catch (IOException e) {
            LogHelper.e(TAG, "create videoEncoder failed.");
            e.printStackTrace();
            return false;
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void onYUVData(byte[] buffer)
    {
        synchronized (sync) {
            if (isRunning()) {
                try {
                    queueRotate.add(buffer);
                } catch (IllegalStateException e) {
                    LogHelper.i(TAG, "frame discarded");
                }
            }
        }
    }

    /**
     * Prepare encoder with default parameters
     */
    public boolean prepare()
    {
        return prepare(quality, rotation, false, formatCodec);
    }

    public void start()
    {
        synchronized (sync) {
            if (videoEncoder != null) {
                hasPSPair = false;
                presentTimeUs = System.nanoTime() / 1000;
                videoEncoder.start();
                // Surface to buffer
                if (formatCodec == SURFACE && minAPI19()) {
                    if (minAPI21()) {
                        encodeDataAPI21();
                    } else {
                        encodeData();
                    }
                } else {
                    // Buffer to buffer
                    if (imageFormat != ImageFormat.NV21 && imageFormat != ImageFormat.YV12) {
                        stop();
                        LogHelper.e(TAG, "Unsupported imageFormat");
                        return;
                    } else if (!(rotation == 0 || rotation == 90 || rotation == 180 || rotation == 270)) {
                        throw new RuntimeException("rotation value unsupported, select value 0, 90, 180 or 270");
                    }

                    thread = new Thread(() -> {
                        while (!Thread.interrupted()) {
                            try {
                                byte[] buffer = queueEncode.take();
                                if (minAPI21()) {
                                    getEncodedDataAPI21(buffer);
                                } else {
                                    getEncodedData(buffer);
                                }
                            } catch (InterruptedException e) {
                                if (thread != null) thread.interrupt();
                            }
                        }
                    });

                    threadRotate = new Thread(() -> {
                        while (!Thread.interrupted()) {
                            try {
                                byte[] buffer = queueRotate.take();
                                // Convert YV12 to NV21
                                if (imageFormat == ImageFormat.YV12) {
                                    buffer = YUVUtil.YV12toYUV420PackedSemiPlanar(buffer, quality.width, quality.height);
                                }
                                if (!hardwareRotation) {
                                    buffer = YUVUtil.rotateNV21(buffer, quality.width, quality.height, rotation);
                                    try {
                                        queueColor.add(buffer);
                                    } catch (IllegalStateException e) {
                                        LogHelper.i(TAG, "frame discarded");
                                    }
                                }
                            } catch (InterruptedException e) {
                                if (threadRotate != null) threadRotate.interrupt();
                            }
                        }
                    });

                    threadColor = new Thread(() -> {
                        while (!Thread.interrupted()) {
                            try {
                                byte[] buffer = queueColor.take();
                                buffer = (sendBlackImage) ?
                                        blackImage :
                                        NV21toYUV420byColor(buffer, quality.width, quality.height, formatCodec);
                                try {
                                    queueEncode.add(buffer);
                                } catch (IllegalStateException e) {
                                    LogHelper.i(TAG, "frame discarded");
                                }
                            } catch (InterruptedException e) {
                                if (threadColor != null) threadColor.interrupt();
                            }
                        }
                    });

                    thread.start();
                    threadRotate.start();
                    threadColor.start();
                }
                running = true;
            } else {
                LogHelper.e(TAG, "VideoEncoder need be prepared, VideoEncoder not enabled");
            }
        }
    }

    public void stop()
    {
        synchronized (sync) {
            running = false;
            if (thread != null) {
                thread.interrupt();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    thread.interrupt();
                }
                thread = null;
            }

            if (threadRotate != null) {
                threadRotate.interrupt();
                try {
                    threadRotate.join();
                } catch (InterruptedException e) {
                    threadRotate.interrupt();
                }
                threadRotate = null;
            }

            if (threadColor != null) {
                threadColor.interrupt();
                try {
                    threadColor.join();
                } catch (InterruptedException e) {
                    threadColor.interrupt();
                }
                threadColor = null;
            }

            if (videoEncoder != null) {
                videoEncoder.stop();
                videoEncoder.release();
                videoEncoder = null;
            }

            queueEncode.clear();
            queueRotate.clear();
            queueColor.clear();
            hasPSPair = false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void encodeDataAPI21()
    {
        thread = new Thread(() -> {
            while (!Thread.interrupted()) {
                encodeProcessAPI21(true);
            }
        });
        thread.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void getEncodedDataAPI21(byte[] buffer)
    {
        int inBufferIndex = videoEncoder.dequeueInputBuffer(-1);
        if (inBufferIndex >= 0) {
            ByteBuffer inputBuffer = videoEncoder.getInputBuffer(inBufferIndex);
            if (inputBuffer != null) {
                inputBuffer.put(buffer, 0, buffer.length);
            }
            long pts = System.nanoTime() / 1000 - presentTimeUs;
            videoEncoder.queueInputBuffer(inBufferIndex, 0, buffer.length, pts, 0);
        }

        encodeProcessAPI21(false);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void encodeProcessAPI21(boolean addPTU)
    {
        for (; ; ) {
            Pair<ByteBuffer, ByteBuffer> psPair;
            int outBufferIndex = videoEncoder.dequeueOutputBuffer(videoInfo, 0);
            if (outBufferIndex == INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat mediaFormat = videoEncoder.getOutputFormat();
                psPair = new Pair<>(mediaFormat.getByteBuffer("csd-0"), mediaFormat.getByteBuffer("csd-1"));
                h264Sinker.onVideoFormat(mediaFormat);
                h264Sinker.onPSReady(psPair);
                hasPSPair = true;
            } else if (outBufferIndex >= 0) {
                // This ByteBuffer is H264
                ByteBuffer buffer = videoEncoder.getOutputBuffer(outBufferIndex);
                if ((videoInfo.flags & BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    if (!hasPSPair && buffer != null) {
                        psPair = decodePSPair(buffer.duplicate(), videoInfo.size);
                        if (psPair != null) {
                            h264Sinker.onPSReady(psPair);
                            hasPSPair = true;
                        }
                    }
                }

                if (addPTU) {
                    videoInfo.presentationTimeUs = System.nanoTime() / 1000 - presentTimeUs;
                }
                 h264Sinker.onH264Data(buffer, videoInfo);
                videoEncoder.releaseOutputBuffer(outBufferIndex, false);
            } else {
                break;
            }
        }
    }

    private void encodeData()
    {
        thread = new Thread(() -> {
            while (!Thread.interrupted()) {
                encodeProcess(true);
            }
        });
        thread.start();
    }

    private void getEncodedData(byte[] buffer)
    {
        ByteBuffer[] inputBuffers = videoEncoder.getInputBuffers();

        int inBufferIndex = videoEncoder.dequeueInputBuffer(-1);
        if (inBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(buffer, 0, buffer.length);
            long pts = System.nanoTime() / 1000 - presentTimeUs;
            videoEncoder.queueInputBuffer(inBufferIndex, 0, buffer.length, pts, 0);
        }

        encodeProcess(false);
    }

    private void encodeProcess(boolean addPTU)
    {
        ByteBuffer[] outputBuffers = videoEncoder.getOutputBuffers();
        for (; ; ) {
            Pair<ByteBuffer, ByteBuffer> psPair;
            int outBufferIndex = videoEncoder.dequeueOutputBuffer(videoInfo, 0);
            if (outBufferIndex == INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat mediaFormat = videoEncoder.getOutputFormat();
                psPair = new Pair<>(mediaFormat.getByteBuffer("csd-0"), mediaFormat.getByteBuffer("csd-1"));
                h264Sinker.onVideoFormat(mediaFormat);
                h264Sinker.onPSReady(psPair);
                hasPSPair = true;
            } else if (outBufferIndex >= 0) {
                // This ByteBuffer is H264
                ByteBuffer buffer = outputBuffers[outBufferIndex];
                if ((videoInfo.flags & BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    if (!hasPSPair && buffer != null) {
                        psPair = decodePSPair(buffer.duplicate(), videoInfo.size);
                        if (psPair != null) {
                            h264Sinker.onPSReady(psPair);
                            hasPSPair = true;
                        }
                    }
                }

                if (addPTU) {
                    videoInfo.presentationTimeUs = System.nanoTime() / 1000 - presentTimeUs;
                }

                h264Sinker.onH264Data(buffer, videoInfo);
                videoEncoder.releaseOutputBuffer(outBufferIndex, false);
            } else {
                break;
            }
        }
    }

    /**
     * Get the video encoder by mime. API 21+
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private MediaCodecInfo getVideoEncoderAPI21(String mime)
    {
        MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaCodecInfo[] mediaCodecInfoList = mediaCodecList.getCodecInfos();
        for (MediaCodecInfo mci : mediaCodecInfoList) {
            if (!mci.isEncoder()) {
                continue;
            }

            return getSupportedVideoEncoder(mime, mci);
        }

        return null;
    }

    /**
     * Get the video encoder by mime. API < 21
     */
    private MediaCodecInfo getVideoEncoder(String mime)
    {
        int count = MediaCodecList.getCodecCount();
        for (int i = 0; i < count; i++) {
            MediaCodecInfo mci = MediaCodecList.getCodecInfoAt(i);
            if (!mci.isEncoder() || getSupportedVideoEncoder(mime, mci) == null) {
                continue;
            }
            return getSupportedVideoEncoder(mime, mci);
        }

        return null;
    }

    private MediaCodecInfo getSupportedVideoEncoder(String mime, MediaCodecInfo mci)
    {
        String[] types = mci.getSupportedTypes();
        for (String type : types) {
            if (type.equalsIgnoreCase(mime)) {
                LogHelper.i(TAG, String.format("videoEncoder %s type supported: %s", mci.getName(), type));
                MediaCodecInfo.CodecCapabilities codecCapabilities = mci.getCapabilitiesForType(mime);
                for (int color : codecCapabilities.colorFormats) {
                    LogHelper.i(TAG, "Color supported: " + color);
                    // Check if encoder support any yuv420 color
                    if (color == YUV420PLANAR.getFormatCodec()
                            || color == YUV420SEMIPLANAR.getFormatCodec()
                            || color == YUV420PACKEDPLANAR.getFormatCodec()) {
                        return mci;
                    }
                }
            }
        }

        return null;
    }

    private void prepareBlackImage()
    {
        Bitmap b = Bitmap.createBitmap(quality.width, quality.height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        canvas.drawColor(Color.BLACK);

        int x = b.getWidth();
        int y = b.getHeight();
        int[] data = new int[x * y];
        b.getPixels(data, 0, x, 0, 0, x, y);

        blackImage = YUVUtil.ARGBtoYUV420SemiPlanar(data, quality.width, quality.height);
    }

    /**
     * choose the video encoder by mime. API 19+
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void startSendBlackImage()
    {
        sendBlackImage = true;
        if (isRunning()) {
            Bundle bundle = new Bundle();
            bundle.putInt(PARAMETER_KEY_VIDEO_BITRATE, 100 * 1024);

            try {
                videoEncoder.setParameters(bundle);
            } catch (IllegalStateException e) {
                LogHelper.e(TAG, "encoder need be running");
                e.printStackTrace();
            }
        }
    }

    /**
     * choose the video encoder by mime. API 19+
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void stopSendBlackImage()
    {
        sendBlackImage = false;
        setVideoBitrateOnFly(quality.bitrate);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void setVideoBitrateOnFly(int bitrate)
    {
        if (isRunning()) {
            this.quality.bitrate = bitrate;
            Bundle bundle = new Bundle();
            bundle.putInt(PARAMETER_KEY_VIDEO_BITRATE, bitrate);

            try {
                videoEncoder.setParameters(bundle);
            } catch (IllegalStateException e) {
                LogHelper.e(TAG, "Encoder needs to be running");
                e.printStackTrace();
            }
        }
    }

    /**
     * decode two parameter sets from the H.264 specifications: sps and pps
     * if the encoder never call to MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
     */
    private Pair<ByteBuffer, ByteBuffer> decodePSPair(ByteBuffer buffer, int length)
    {
        byte[] sps = null, pps = null;
        byte[] csd = new byte[length];

        buffer.get(csd, 0, length);
        int i = 0;
        int spsIndex = -1;
        int ppsIndex = -1;

        while (i < length - 4) {
            if (csd[i] == 0 && csd[i + 1] == 0 && csd[i + 2] == 0 && csd[i + 3] == 1) {
                if (spsIndex == -1) {
                    spsIndex = i;
                } else {
                    ppsIndex = i;
                    break;
                }
            }
            i++;
        }

        if (spsIndex != -1 && ppsIndex != -1) {
            sps = new byte[ppsIndex];
            pps = new byte[length - ppsIndex];

            System.arraycopy(csd, spsIndex, sps, 0, ppsIndex);
            System.arraycopy(csd, ppsIndex, pps, 0, length - ppsIndex);
        }

        if (sps != null && pps != null) {
            return new Pair<>(ByteBuffer.wrap(sps), ByteBuffer.wrap(pps));
        }

        return null;
    }

    public static String createBody(int trackVideo, int port, String sps, String pps)
    {
        return "m=video " + port + " RTP/AVP " + PAYLOAD_TYPE + "\r\n" +
                "a=rtpmap:" + PAYLOAD_TYPE + " H264/" + CLOCK_VIDEO_FREQUENCY + "\r\n" +
                "a=fmtp:" + PAYLOAD_TYPE + " packetization-mode=1;sprop-parameter-sets=" + sps + "," + pps + ";\r\n" +
                "a=control:trackID=" + trackVideo + "\r\n";
    }

    private VideoEncodingFormat getColorFormat(MediaCodecInfo mediaCodecInfo)
    {
        for (int color : mediaCodecInfo.getCapabilitiesForType(mime).colorFormats) {
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

    public Surface getInputSurface()
    {
        return inputSurface;
    }

    public void setInputSurface(Surface inputSurface)
    {
        this.inputSurface = inputSurface;
    }

    public void setImageFormat(int imageFormat)
    {
        this.imageFormat = imageFormat;
    }

    public int getWidth()
    {
        return quality.width;
    }

    public int getHeight()
    {
        return quality.height;
    }

    public int getRotation()
    {
        return rotation;
    }

    public int getFps()
    {
        return quality.fps;
    }

    public boolean isRunning()
    {
        return running;
    }
}