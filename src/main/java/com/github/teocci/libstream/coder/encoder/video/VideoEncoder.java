package com.github.teocci.libstream.coder.encoder.video;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.Pair;
import android.view.Surface;

import com.github.teocci.libstream.enums.FormatVideoEncoder;
import com.github.teocci.libstream.input.video.FpsLimiter;
import com.github.teocci.libstream.input.video.Frame;
import com.github.teocci.libstream.input.video.VideoQuality;
import com.github.teocci.libstream.interfaces.video.CameraSinker;
import com.github.teocci.libstream.interfaces.video.EncoderSinker;
import com.github.teocci.libstream.utils.LogHelper;
import com.github.teocci.libstream.utils.yuv.YUVUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static android.media.MediaCodec.PARAMETER_KEY_VIDEO_BITRATE;
import static com.github.teocci.libstream.enums.FormatVideoEncoder.SURFACE;
import static com.github.teocci.libstream.enums.FormatVideoEncoder.YUV420DYNAMICAL;
import static com.github.teocci.libstream.utils.CodecUtil.H264_MIME;
import static com.github.teocci.libstream.utils.CodecUtil.H265_MIME;
import static com.github.teocci.libstream.utils.CodecUtil.IFRAME_INTERVAL;
import static com.github.teocci.libstream.utils.CodecUtil.MAX_INPUT_SIZE;
import static com.github.teocci.libstream.utils.CodecUtil.getColorFormat;
import static com.github.teocci.libstream.utils.CodecUtil.getVideoCodecInfo;
import static com.github.teocci.libstream.utils.BuildUtil.minAPI18;
import static com.github.teocci.libstream.utils.BuildUtil.minAPI19;
import static com.github.teocci.libstream.utils.BuildUtil.minAPI21;
import static com.github.teocci.libstream.utils.rtsp.RtpConstants.CLOCK_VIDEO_FREQUENCY;
import static com.github.teocci.libstream.utils.rtsp.RtpConstants.PAYLOAD_TYPE;
import static com.github.teocci.libstream.utils.yuv.YUVUtil.NV21toYUV420byColor;
import static com.github.teocci.libstream.utils.yuv.YUVUtil.YV12toYUV420byColor;

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
//    private Thread threadEncode;
//    private Thread threadRotate;
//    private Thread threadColor;

    private EncoderSinker encoderSinker;

    //    private long presentTimeUs;
//    private long frameIndex = 0;
    private long firstPts = 0;
    private long lastPts = 0;

    private boolean running = false;
    private boolean hasPSPair = false;
    private boolean hardwareRotation = false;

    // Surface to buffer encoder
    private Surface inputSurface;

    // Buffer to buffer, 3 queue to optimize frames on rotation
    private BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>(80);
//    private BlockingQueue<byte[]> queueEncode = new LinkedBlockingQueue<>(30);
//    private BlockingQueue<byte[]> queueRotate = new LinkedBlockingQueue<>(30);
//    private BlockingQueue<byte[]> queueColor = new LinkedBlockingQueue<>(30);
//    private BlockingQueue<Frame> queue = new LinkedBlockingQueue<>(80);
//    private BlockingQueue<Frame> queueEncode = new LinkedBlockingQueue<>(30);
//    private BlockingQueue<Frame> queueRotate = new LinkedBlockingQueue<>(30);
//    private BlockingQueue<Frame> queueColor = new LinkedBlockingQueue<>(30);

    private final Object sync = new Object();

    private VideoQuality quality = VideoQuality.DEFAULT;

    private int rotation = 0;
    private int iFrameInterval = IFRAME_INTERVAL;
    private int imageFormat = ImageFormat.NV21;
    private String mineType = H264_MIME;

    private FormatVideoEncoder formatCodec = YUV420DYNAMICAL;

    // For disable video
    private boolean sendBlackImage = false;
    private byte[] blackImage;

    private FpsLimiter fpsLimiter = new FpsLimiter();

    public VideoEncoder(EncoderSinker encoderSinker)
    {
        this.encoderSinker = encoderSinker;
    }

    @Override
    public void onYUVData(byte[] buffer)
    {
        synchronized (sync) {
            if (isRunning()) {
                if (fpsLimiter.limitFPS(quality.fps)) return;
                try {
                    queue.add(buffer);
//                    queueRotate.add(buffer);
                } catch (IllegalStateException e) {
                    LogHelper.i(TAG, "onYUVData: frame discarded");
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onYUVData(Frame frame)
    {
//        synchronized (sync) {
//            if (isRunning()) {
//                try {
////                    queue.add(frame);
//                    queueRotate.add(frame);
//                } catch (IllegalStateException e) {
//                    LogHelper.i(TAG, "onYUVData: frame discarded");
//                    e.printStackTrace();
//                }
//            }
//        }
    }

    /**
     * Prepare encoder with default parameters
     */
    public boolean prepare()
    {
        return prepare(quality, false, rotation, IFRAME_INTERVAL, formatCodec);
    }

    /**
     * Prepare encoder with custom parameters with backward compatibility
     */
    public boolean prepare(VideoQuality quality, boolean hardwareRotation, int rotation, FormatVideoEncoder formatCodec)
    {
        return prepare(quality, hardwareRotation, rotation, IFRAME_INTERVAL, formatCodec);
    }

    /**
     * Call this method before use @startStream. If not you will do a stream without video. NOTE:
     * Rotation with encoder is silence ignored in some devices.
     *
     * @param quality          represents the quality of a video stream.
     * @param hardwareRotation true if you want rotate using encoder, false if you want rotate with
     *                         software if you are using a SurfaceView or TextureView or with OpenGl if you are using
     *                         OpenGlView.
     * @param rotation         could be 90, 180, 270 or 0. You should use CameraHelper.getCameraOrientation
     *                         with SurfaceView or TextureView and 0 with OpenGlView or LightOpenGlView. NOTE: Rotation with
     *                         encoder is silence ignored in some devices.
     * @param iFrameInterval   seconds between I-frames
     * @param formatCodec      the YUV color format
     * @return true if success, false if you get a error (Normally because the encoder selected
     * doesn't support any configuration seated or your device hasn't a H264 encoder).
     */
    public boolean prepare(VideoQuality quality, boolean hardwareRotation, int rotation, int iFrameInterval, FormatVideoEncoder formatCodec)
    {
        LogHelper.e(TAG, "prepare video encoder: " + formatCodec);
        this.quality.width = quality.width;
        this.quality.height = quality.height;
        this.quality.bitrate = quality.bitrate;
        this.quality.fps = quality.fps;

        LogHelper.e(TAG, "quality: " + this.quality);

        this.rotation = rotation;
        this.hardwareRotation = hardwareRotation;
        this.iFrameInterval = iFrameInterval < 0 ? iFrameInterval : IFRAME_INTERVAL;
        this.formatCodec = formatCodec;

        String resolution;

//        LogHelper.e(TAG, "Rotation: " + rotation);
        MediaCodecInfo encoder = getVideoCodecInfo(mineType, formatCodec);
        if (encoder == null) {
            LogHelper.e(TAG, "Valid encoder not found");
            return false;
        }

//        for (String codec : showAllCodecsInfo()) {
//            LogHelper.e(TAG, codec);
//        }

        try {
            // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
            // we can use for input and wrap it with a class that handles the EGL work.
            videoEncoder = MediaCodec.createByCodecName(encoder.getName());
//            videoEncoder = MediaCodec.createEncoderByType(mineType);
            if (videoEncoder != null) {
                if (this.formatCodec == YUV420DYNAMICAL) {
                    this.formatCodec = getColorFormat(videoEncoder.getCodecInfo());
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
            // We just need to swap width and height in rotation 90 or 270 to correct encoding resolution
            if (!hardwareRotation && (rotation == 90 || rotation == 270)) {
                resolution = quality.height + "x" + quality.width;
                videoFormat = MediaFormat.createVideoFormat(mineType, quality.height, quality.width);
            } else {
                resolution = quality.width + "x" + quality.height;
                videoFormat = MediaFormat.createVideoFormat(mineType, quality.width, quality.height);
            }

            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, this.formatCodec.getFormatCodec());
            videoFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MAX_INPUT_SIZE);
            videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, quality.bitrate);
            videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, quality.fps);
            videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

            if (hardwareRotation) {
                videoFormat.setInteger("rotation-degrees", rotation);
            }

            videoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            running = false;
            if (minAPI19() && formatCodec == SURFACE) {
                inputSurface = videoEncoder.createInputSurface();
            }

            prepareBlackImage();

            LogHelper.i(TAG, "Prepare video info: " + formatCodec.name() + ", " + resolution);
            LogHelper.e(TAG, "prepare video encoder: TRUE");

            return true;
        } catch (IOException e) {
            LogHelper.e(TAG, "create videoEncoder failed.");
            LogHelper.e(TAG, "prepare video encoder: FALSE");
            e.printStackTrace();

            return false;
        } catch (IllegalStateException e) {
            LogHelper.e(TAG, "prepare video encoder: FALSE");
            e.printStackTrace();

            return false;
        }
    }


    public void start()
    {
        encode(true);
    }

    private void encode(boolean resetTs)
    {
        if (videoEncoder == null) {
            LogHelper.e(TAG, "VideoEncoder need be prepared, VideoEncoder not enabled");
            return;
        }
        synchronized (sync) {
            hasPSPair = false;
//            if (resetTs) presentTimeUs = System.nanoTime() / 1000;
//            if (resetTs) frameIndex = 0;

            videoEncoder.start();
            // Surface to buffer
            if (isSurface()) {
                // Thread definition
                initSurfaceSingleProcess();
//                initSurfaceProcess();
            } else {
                // Buffer to buffer
                if (imageFormat != ImageFormat.NV21 && imageFormat != ImageFormat.YV12) {
                    stop();
                    LogHelper.e(TAG, "Unsupported imageFormat");

                    return;
                } else if (!(rotation == 0 || rotation == 90 || rotation == 180 || rotation == 270)) {
                    throw new RuntimeException("rotation value unsupported, select value 0, 90, 180 or 270");
                }

                // Thread definition
                initBufferSingleProcess();
//                initBufferProcess();
            }

            runProcess();
        }
    }

//    private void initSurfaceProcess()
//    {
//        threadEncode = new Thread(() -> {
//            if (minAPI21()) {
//                encodeDataAPI21();
//            } else {
//                encodeData();
//            }
//        });
//    }
//
//    private void initBufferProcess()
//    {
//        threadEncode = new Thread(() -> {
//            while (!Thread.interrupted()) {
//                try {
////                    Frame frame = queueEncode.take();
////                    byte[] buffer = frame.getBuffer();
//                    byte[] buffer = queueEncode.take();
//
//                    if (minAPI21()) {
//                        requestEncodedDataAPI21(buffer);
//                    } else {
//                        requestEncodedData(buffer);
//                    }
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                }
//            }
//        });
//
//        threadRotate = new Thread(() -> {
//            int width = quality.width;
//            int height = quality.height;
//            YUVUtil.preAllocateBuffers(width * height * 3 / 2);
//
//            while (!Thread.interrupted()) {
//                try {
////                    Frame frame = queueRotate.take();
////                    byte[] buffer = frame.getBuffer();
//                    byte[] buffer = queueRotate.take();
//
//                    // Convert YV12 to NV21
//                    if (imageFormat == ImageFormat.YV12) {
//                        buffer = toNV21(buffer, quality.width, quality.height);
//                    }
//
//                    if (!hardwareRotation) {
//                        int orientation = rotation;
//
//                        if (orientation >= 360) orientation -= 360;
//                        buffer = YUVUtil.rotateNV21(buffer, quality.width, quality.height, orientation);
//                        try {
//                            queueColor.add(buffer);
////                            frame.setBuffer(buffer);
////                            queueColor.add(frame);
//                        } catch (IllegalStateException e) {
//                            LogHelper.i(TAG, "rotate: frame discarded");
//                        }
//                    }
//                } catch (InterruptedException e) {
//                    if (threadRotate != null) threadRotate.interrupt();
//                }
//            }
//        });
//
//        threadColor = new Thread(() -> {
//            while (!Thread.interrupted()) {
//                try {
////                    Frame frame = queueColor.take();
////                    byte[] buffer = frame.getBuffer();
//                    byte[] buffer = queueColor.take();
//                    buffer = (sendBlackImage) ?
//                            blackImage : imageFormat == ImageFormat.YV12 ?
//                            YV12toYUV420byColor(buffer, quality.width, quality.height, formatCodec) :
//                            NV21toYUV420byColor(buffer, quality.width, quality.height, formatCodec);
//                    try {
//                        queueEncode.add(buffer);
////                        frame.setBuffer(buffer);
////                        queueEncode.add(frame);
////                        queueEncode.add(new Frame(buffer, frame.getOrientation(), frame.isFlip(), frame.getFormat()));
//                    } catch (IllegalStateException e) {
//                        LogHelper.i(TAG, "color: frame discarded");
//                    }
//                } catch (InterruptedException e) {
//                    if (threadColor != null) threadColor.interrupt();
//                }
//            }
//        });
//    }

    private void initSurfaceSingleProcess()
    {
        thread = new Thread(() -> {
            if (minAPI21()) {
                encodeDataAPI21();
            } else {
                encodeData();
            }
        });
    }

    private void initBufferSingleProcess()
    {
        thread = new Thread(() -> {
            int width = quality.width;
            int height = quality.height;

            YUVUtil.preAllocateBuffers(width * height * 3 / 2);
            while (running && !Thread.interrupted()) {
                try {
//                    Frame frame = queue.take();
//                    if (fpsLimiter.limitFPS(quality.fps)) continue;
//                    byte[] buffer = frame.getBuffer();

                    byte[] buffer = queue.take();
//                            boolean isYV12 = frame.getFormat() == ImageFormat.YV12;
                    if (!hardwareRotation) {
//                                 int orientation = frame.isFlip() ? frame.getOrientation() + 180 : frame.getOrientation();
                        int orientation = rotation;

                        if (orientation >= 360) orientation -= 360;
                        buffer = imageFormat == ImageFormat.YV12 ?
                                YUVUtil.rotateYV12(buffer, width, height, orientation) :
                                YUVUtil.rotateNV21(buffer, width, height, orientation);
                    }
                    buffer = (sendBlackImage) ?
                            blackImage : imageFormat == ImageFormat.YV12 ?
                            YV12toYUV420byColor(buffer, width, height, formatCodec) :
                            NV21toYUV420byColor(buffer, width, height, formatCodec);

                    if (Thread.currentThread().isInterrupted()) return;

                    if (minAPI21()) {
                        requestEncodedDataAPI21(buffer);
                    } else {
                        requestEncodedData(buffer);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    private void runProcess()
    {
        runSingleThread();
//        runThreads();
        running = true;
    }

    private void runSingleThread()
    {
        thread.start();
    }

//    private void runThreads()
//    {
//        if (isSurface()) {
//            threadEncode.start();
//        } else {
//            threadEncode.start();
//            threadRotate.start();
//            threadColor.start();
//        }
//    }

    public void stop()
    {
        synchronized (sync) {
            stopSingleProcess();
//            stopProcess();

            if (videoEncoder != null) {
                videoEncoder.flush();
                videoEncoder.stop();
                videoEncoder.release();
                videoEncoder = null;
            }

            queue.clear();
//            queueEncode.clear();
//            queueRotate.clear();
//            queueColor.clear();

            fpsLimiter.reset();
            hasPSPair = false;
            inputSurface = null;
            running = false;
        }
    }

    private void stopSingleProcess()
    {
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(100);
            } catch (InterruptedException e) {
                thread.interrupt();
            }
            thread = null;
        }
    }

//    private void stopProcess()
//    {
//        if (threadEncode != null) {
//            threadEncode.interrupt();
//            try {
//                threadEncode.join(100);
//            } catch (InterruptedException e) {
//                threadEncode.interrupt();
//            }
//            threadEncode = null;
//        }
//
//        if (threadRotate != null) {
//            threadRotate.interrupt();
//            try {
//                threadRotate.join(100);
//            } catch (InterruptedException e) {
//                threadRotate.interrupt();
//            }
//            threadRotate = null;
//        }
//
//        if (threadColor != null) {
//            threadColor.interrupt();
//            try {
//                threadColor.join(100);
//            } catch (InterruptedException e) {
//                threadColor.interrupt();
//            }
//            threadColor = null;
//        }
//    }

    public void reset()
    {
        synchronized (sync) {
            stop();
            prepare(quality, hardwareRotation, rotation, iFrameInterval, formatCodec);
            encode(false);
        }
    }

    /**
     * TODO: optimize
     *
     * @param mediaFormat
     */
    private void sendCodecInfo(MediaFormat mediaFormat)
    {
        if (mineType.equals(H265_MIME)) {
            // H265
            List<ByteBuffer> byteBufferList = decodeVpsSpsPps(mediaFormat.getByteBuffer("csd-0"));
            encoderSinker.onSpsPpsVpsReady(byteBufferList.get(1), byteBufferList.get(2), byteBufferList.get(0));
        } else {
            // H264
            Pair<ByteBuffer, ByteBuffer> psPair = new Pair<>(mediaFormat.getByteBuffer("csd-0"), mediaFormat.getByteBuffer("csd-1"));
            encoderSinker.onPSReady(psPair);
        }
    }


    /**
     * New Implementation
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void encodeDataAPI21()
    {
        while (!Thread.interrupted()) {
            drainEncoderAPI21();
        }
    }

    /**
     * New implementation
     *
     * @param buffer frame buffer
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void requestEncodedDataAPI21(byte[] buffer)
    {
        if (buffer == null) return;
        // Wait indefinitely for the availability of an input buffer
        int inBufferIndex = videoEncoder.dequeueInputBuffer(-1);
        if (inBufferIndex >= 0) {
//            long pts = System.nanoTime() / 1000 - presentTimeUs;
//            long pts = System.nanoTime() / 1000;
//            long pts = computePresentationTime();
//            long pts = computePresentationTime(frameIndex);
            // TODO: find a pts without glitching

            ByteBuffer inputBuffer = videoEncoder.getInputBuffer(inBufferIndex);
            if (inputBuffer != null) {
                inputBuffer.put(buffer, 0, buffer.length);
            }
            videoEncoder.queueInputBuffer(inBufferIndex, 0, buffer.length, 0, 0);
//            frameIndex++;
        }

        drainEncoderAPI21();
    }

    /**
     * New Implementation replace the duplicated code
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void drainEncoderAPI21()
    {
        for (; running; ) {
            int outBufferIndex = videoEncoder.dequeueOutputBuffer(videoInfo, 0);
            if (outBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat mediaFormat = videoEncoder.getOutputFormat();
                encoderSinker.onVideoFormat(mediaFormat);
                sendCodecInfo(mediaFormat);
                hasPSPair = true;
            } else if (outBufferIndex >= 0) {
//                videoInfo.presentationTimeUs = System.nanoTime() / 1000 - presentTimeUs;
//                videoInfo.presentationTimeUs = computePresentationTime(frameIndex);
//                videoInfo.presentationTimeUs = getNextRelativePts(System.nanoTime() / 1000);
//                LogHelper.e(TAG, "videoInfo: " + videoInfo.presentationTimeUs);
                // This ByteBuffer is H264
                ByteBuffer buffer = videoEncoder.getOutputBuffer(outBufferIndex);
                if ((videoInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    if (!hasPSPair && buffer != null) {
                        Pair<ByteBuffer, ByteBuffer> psPair = decodePSPair(buffer.duplicate(), videoInfo.size);
                        if (psPair != null) {
                            encoderSinker.onPSReady(psPair);
                            hasPSPair = true;
                        }
                    }
                }

                encoderSinker.onEncodedData(buffer, videoInfo);
                videoEncoder.releaseOutputBuffer(outBufferIndex, false);
            } else {
                break;
            }
        }
    }

//    /**
//     * Old implementation
//     */
//    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
//    private void encodeDataAPI21()
//    {
//        threadEncode = new Thread(() -> {
//            while (!Thread.interrupted()) {
//                drainEncoderAPI21(true);
//            }
//        });
//        threadEncode.start();
//    }

//    /**
//     * OLD implementation
//     *
//     * @param buffer
//     */
//    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
//    private void requestEncodedDataAPI21(byte[] buffer)
//    {
//        int inBufferIndex = videoEncoder.dequeueInputBuffer(-1);
//        if (inBufferIndex >= 0) {
//            ByteBuffer inputBuffer = videoEncoder.getInputBuffer(inBufferIndex);
//            if (inputBuffer != null) {
//                inputBuffer.put(buffer, 0, buffer.length);
//            }
//            long pts = System.nanoTime() / 1000 - presentTimeUs;
//            videoEncoder.queueInputBuffer(inBufferIndex, 0, buffer.length, pts, 0);
//        }
//
//        drainEncoderAPI21(false);
//    }

//    /**
//     * old
//     *
//     * @param addPTU
//     */
//    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
//    private void drainEncoderAPI21(boolean addPTU)
//    {
//        for (; ; ) {
//            Pair<ByteBuffer, ByteBuffer> psPair;
//            int outBufferIndex = videoEncoder.dequeueOutputBuffer(videoInfo, 0);
//            if (outBufferIndex == INFO_OUTPUT_FORMAT_CHANGED) {
//                MediaFormat mediaFormat = videoEncoder.getOutputFormat();
//                psPair = new Pair<>(mediaFormat.getByteBuffer("csd-0"), mediaFormat.getByteBuffer("csd-1"));
//                encoderSinker.onVideoFormat(mediaFormat);
//                encoderSinker.onPSReady(psPair);
//                hasPSPair = true;
//            } else if (outBufferIndex >= 0) {
//                // This ByteBuffer is H264
//                ByteBuffer buffer = videoEncoder.getOutputBuffer(outBufferIndex);
//                if ((videoInfo.flags & BUFFER_FLAG_CODEC_CONFIG) != 0) {
//                    if (!hasPSPair && buffer != null) {
//                        psPair = decodePSPair(buffer.duplicate(), videoInfo.size);
//                        if (psPair != null) {
//                            encoderSinker.onPSReady(psPair);
//                            hasPSPair = true;
//                        }
//                    }
//                }
//
//                if (addPTU) {
//                    videoInfo.presentationTimeUs = System.nanoTime() / 1000 - presentTimeUs;
//                }
//                encoderSinker.onEncodedData(buffer, videoInfo);
//                videoEncoder.releaseOutputBuffer(outBufferIndex, false);
//            } else {
//                break;
//            }
//        }
//    }


    /**
     * New implementation
     */
    private void encodeData()
    {
        while (!Thread.interrupted()) {
            drainEncoder();
        }
    }

    /**
     * NEW Implementation
     *
     * @param buffer
     */
    private void requestEncodedData(byte[] buffer)
    {
        ByteBuffer[] inputBuffers = videoEncoder.getInputBuffers();

        int inBufferIndex = videoEncoder.dequeueInputBuffer(-1);
        if (inBufferIndex >= 0) {
//            long pts = System.nanoTime() / 1000 - presentTimeUs;
//            long pts = computePresentationTime();
//            long pts = computePresentationTime(frameIndex);
//            long pts = System.nanoTime() / 1000;
            // TODO: find a pts without glitching

            ByteBuffer inputBuffer = inputBuffers[inBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(buffer, 0, buffer.length);
            videoEncoder.queueInputBuffer(inBufferIndex, 0, buffer.length, 0, 0);
//            frameIndex++;
        }

        drainEncoder();
    }


    /**
     * New implementation
     */
    private void drainEncoder()
    {
        ByteBuffer[] outputBuffers = videoEncoder.getOutputBuffers();
        for (; running; ) {
//            int outBufferIndex = videoEncoder.dequeueOutputBuffer(videoInfo, 10000);
            int outBufferIndex = videoEncoder.dequeueOutputBuffer(videoInfo, 0);
            if (outBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat mediaFormat = videoEncoder.getOutputFormat();
                encoderSinker.onVideoFormat(mediaFormat);
                sendCodecInfo(mediaFormat);
                hasPSPair = true;
            } else if (outBufferIndex >= 0) {
//                videoInfo.presentationTimeUs = System.nanoTime() / 1000 - presentTimeUs;
//                videoInfo.presentationTimeUs = computePresentationTime(frameIndex);
//                videoInfo.presentationTimeUs = getNextRelativePts(videoInfo.presentationTimeUs);

                //This ByteBuffer is H264
                ByteBuffer outputBuffer = outputBuffers[outBufferIndex];
                if ((videoInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    if (!hasPSPair && outputBuffer != null) {
                        Pair<ByteBuffer, ByteBuffer> psPair = decodePSPair(outputBuffer.duplicate(), videoInfo.size);
                        if (psPair != null) {
                            encoderSinker.onPSReady(psPair);
                            hasPSPair = true;
                        }
                    }
                }
                encoderSinker.onEncodedData(outputBuffer, videoInfo);
                videoEncoder.releaseOutputBuffer(outBufferIndex, false);
            } else {
                break;
            }
        }
    }


    /**
     * Return a relative pts given an absolute pts and trackIndex.
     *
     * This method advances the state of the Encoder, and must only
     * be called once per call to {@link #drainEncoder()}.
     */
    private long getNextRelativePts(long pts) {
        if (firstPts == 0) {
            firstPts = pts;
            return 0;
        }
        return getSafePts(pts - firstPts);
    }

    /**
     * Sometimes packets with non-increasing pts are dequeued from the MediaCodec output buffer.
     * This method ensures that a crash won't occur due to non monotonically increasing packet timestamp.
     */
    private long getSafePts(long pts) {
        if (lastPts >= pts) {
            // Enforce a non-zero minimum spacing between pts
            lastPts += 9643;
            return lastPts;
        }
        lastPts = pts;

        return pts;
    }

//    private long computePresentationTime()
//    {
//        return System.nanoTime() / 1000 - presentTimeUs;
//    }

//    private long computePresentationTime(long frameIndex)
//    {
//        return 132 + frameIndex * 1000000 / quality.fps;
//    }


//    /**
//     * OLD Implementation
//     */
//    private void encodeData()
//    {
//        threadEncode = new Thread(() -> {
//            while (!Thread.interrupted()) {
//                drainEncoder(true);
//            }
//        });
//        threadEncode.start();
//    }

//    /**
//     * OLD Implementation
//     *
//     * @param buffer
//     */
//    private void requestEncodedData(byte[] buffer)
//    {
//        ByteBuffer[] inputBuffers = videoEncoder.getInputBuffers();
//
//        int inBufferIndex = videoEncoder.dequeueInputBuffer(-1);
//        if (inBufferIndex >= 0) {
//            long pts = System.nanoTime() / 1000 - presentTimeUs;
//            ByteBuffer inputBuffer = inputBuffers[inBufferIndex];
//            inputBuffer.clear();
//            inputBuffer.put(buffer, 0, buffer.length);
//            videoEncoder.queueInputBuffer(inBufferIndex, 0, buffer.length, pts, 0);
//        }
//
//        drainEncoder(false);
//    }

//    /**
//     *  OLD Implementation
//     *
//     * @param addPTU
//     */
//    private void drainEncoder(boolean addPTU)
//    {
//        ByteBuffer[] outputBuffers = videoEncoder.getOutputBuffers();
//        for (; ; ) {
//            Pair<ByteBuffer, ByteBuffer> psPair;
//            int outBufferIndex = videoEncoder.dequeueOutputBuffer(videoInfo, 0);
//            if (outBufferIndex == INFO_OUTPUT_FORMAT_CHANGED) {
//                MediaFormat mediaFormat = videoEncoder.getOutputFormat();
//                psPair = new Pair<>(mediaFormat.getByteBuffer("csd-0"), mediaFormat.getByteBuffer("csd-1"));
//                encoderSinker.onVideoFormat(mediaFormat);
//                encoderSinker.onPSReady(psPair);
//                hasPSPair = true;
//            } else if (outBufferIndex >= 0) {
//                // This ByteBuffer is H264
//                ByteBuffer buffer = outputBuffers[outBufferIndex];
//                if ((videoInfo.flags & BUFFER_FLAG_CODEC_CONFIG) != 0) {
//                    if (!hasPSPair && buffer != null) {
//                        psPair = decodePSPair(buffer.duplicate(), videoInfo.size);
//                        if (psPair != null) {
//                            encoderSinker.onPSReady(psPair);
//                            hasPSPair = true;
//                        }
//                    }
//                }
//
//                if (addPTU) {
//                    videoInfo.presentationTimeUs = System.nanoTime() / 1000 - presentTimeUs;
//                }
//
//                encoderSinker.onEncodedData(buffer, videoInfo);
//                videoEncoder.releaseOutputBuffer(outBufferIndex, false);
//            } else {
//                break;
//            }
//        }
//    }

    private void prepareBlackImage()
    {
        Bitmap b = Bitmap.createBitmap(quality.width, quality.height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        canvas.drawColor(Color.BLACK);

        int x = b.getWidth();
        int y = b.getHeight();
        int[] data = new int[x * y];
        b.getPixels(data, 0, x, 0, 0, x, y);

        blackImage = YUVUtil.ARGBtoNV12(data, quality.width, quality.height);
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

    /**
     * Decode two parameter sets from the H.264 specifications: sps and pps
     * if the encoder never call to MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
     *
     * @param buffer
     * @param length
     * @return
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

    /**
     * You need find 0 0 0 1 byte sequence that is the initiation of vps, sps and pps
     * buffers.
     *
     * @param buffer get in MediaCodec case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
     * @return list with vps, sps and pps
     */
    private List<ByteBuffer> decodeVpsSpsPps(ByteBuffer buffer)
    {
        List<ByteBuffer> byteBufferList = new ArrayList<>();
        int vpsPosition = -1;
        int spsPosition = -1;
        int ppsPosition = -1;
        int initializer = 0;

        byte[] csdArray = buffer.array();
        for (int i = 0; i < csdArray.length; i++) {
            if (initializer == 3 && csdArray[i] == 1) {
                if (vpsPosition == -1) {
                    vpsPosition = i - 3;
                } else if (spsPosition == -1) {
                    spsPosition = i - 3;
                } else {
                    ppsPosition = i - 3;
                }
            }
            if (csdArray[i] == 0) {
                initializer++;
            } else {
                initializer = 0;
            }
        }

        byte[] vps = new byte[spsPosition];
        byte[] sps = new byte[ppsPosition - spsPosition];
        byte[] pps = new byte[csdArray.length - ppsPosition];
        for (int i = 0; i < csdArray.length; i++) {
            if (i < spsPosition) {
                vps[i] = csdArray[i];
            } else if (i < ppsPosition) {
                sps[i - spsPosition] = csdArray[i];
            } else {
                pps[i - ppsPosition] = csdArray[i];
            }
        }

        byteBufferList.add(ByteBuffer.wrap(vps));
        byteBufferList.add(ByteBuffer.wrap(sps));
        byteBufferList.add(ByteBuffer.wrap(pps));

        return byteBufferList;
    }


    public static String createBody(int trackVideo, int port, String sps, String pps)
    {
        return "m=video " + port + " RTP/AVP " + PAYLOAD_TYPE + "\r\n" +
                "a=rtpmap:" + PAYLOAD_TYPE + " H264/" + CLOCK_VIDEO_FREQUENCY + "\r\n" +
                "a=fmtp:" + PAYLOAD_TYPE + " packetization-mode=1;sprop-parameter-sets=" + sps + "," + pps + ";\r\n" +
                "a=control:trackID=" + trackVideo + "\r\n";
    }


    // Setters

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

    public void setInputSurface(Surface inputSurface)
    {
        this.inputSurface = inputSurface;
    }

    public void setFps(int fps)
    {
        this.quality.fps = fps;
    }

    public void setImageFormat(int imageFormat)
    {
        this.imageFormat = imageFormat;
    }


    // Getters

    public Surface getInputSurface()
    {
        return inputSurface;
    }

    public VideoQuality getQuality()
    {
        return quality;
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

    public int getBitRate()
    {
        return quality.bitrate;
    }


    // Boolean Methods

    private boolean isSurface()
    {
        return formatCodec == SURFACE && minAPI18();
    }

    public boolean isRunning()
    {
        return running;
    }
}