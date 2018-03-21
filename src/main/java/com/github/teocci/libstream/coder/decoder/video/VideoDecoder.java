package com.github.teocci.libstream.coder.decoder.video;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Process;
import android.view.Surface;

import com.github.teocci.libstream.interfaces.video.VideoDecoderListener;
import com.github.teocci.libstream.utils.LogHelper;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public class VideoDecoder
{
    private static String TAG = LogHelper.makeLogTag(VideoDecoder.class);

    private final VideoDecoderListener videoDecoderListener;
    private MediaExtractor videoExtractor;
    private MediaCodec videoDecoder;
    private MediaCodec.BufferInfo videoInfo = new MediaCodec.BufferInfo();
    private boolean decoding;
    private Thread thread;
    private MediaFormat videoFormat;
    private String mime = "";
    private int width;
    private int height;
    private boolean loopMode = false;

    public VideoDecoder(VideoDecoderListener videoDecoderListener)
    {
        this.videoDecoderListener = videoDecoderListener;
    }

    public boolean initExtractor(String filePath) throws IOException
    {
        decoding = false;
        videoExtractor = new MediaExtractor();
        videoExtractor.setDataSource(filePath);
        for (int i = 0; i < videoExtractor.getTrackCount() && !mime.startsWith("video/"); i++) {
            videoFormat = videoExtractor.getTrackFormat(i);
            mime = videoFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                videoExtractor.selectTrack(i);
            } else {
                videoFormat = null;
            }
        }
        if (videoFormat != null && mime.equals("video/avc")) {
            width = videoFormat.getInteger(MediaFormat.KEY_WIDTH);
            height = videoFormat.getInteger(MediaFormat.KEY_HEIGHT);
            return true;
        } else { // Video decoder not supported
            mime = "";
            videoFormat = null;
            return false;
        }
    }

    public boolean prepareVideo(Surface surface)
    {
        try {
            videoDecoder = MediaCodec.createDecoderByType(mime);
            videoDecoder.configure(videoFormat, surface, null, 0);
            return true;
        } catch (IOException e) {
            LogHelper.e(TAG, "Prepare decoder error:", e);
            return false;
        }
    }

    public void start()
    {
        decoding = true;
        videoDecoder.start();
        thread = new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE);
            decodeVideo();
        });
        thread.start();
    }

    public void stop()
    {
        decoding = false;
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException e) {
                thread.interrupt();
            }
            thread = null;
        }
        if (videoDecoder != null) {
            videoDecoder.stop();
            videoDecoder.release();
            videoDecoder = null;
        }
        if (videoExtractor != null) {
            videoExtractor.release();
            videoExtractor = null;
        }
    }

    private void decodeVideo()
    {
        ByteBuffer[] inputBuffers = videoDecoder.getInputBuffers();
        long startMs = System.currentTimeMillis();
        while (decoding) {
            int inIndex = videoDecoder.dequeueInputBuffer(10000);
            if (inIndex >= 0) {
                ByteBuffer buffer = inputBuffers[inIndex];
                int sampleSize = videoExtractor.readSampleData(buffer, 0);
                if (sampleSize < 0) {
                    videoDecoder.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    LogHelper.i(TAG, "End of file in");
                } else {
                    videoDecoder.queueInputBuffer(inIndex, 0, sampleSize, videoExtractor.getSampleTime(), 0);
                    videoExtractor.advance();
                }
            }
            int outIndex = videoDecoder.dequeueOutputBuffer(videoInfo, 10000);
            if (outIndex >= 0) {
                // Needed for fix decode speed
                while (videoInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        thread.interrupt();
                        break;
                    }
                }
                videoDecoder.releaseOutputBuffer(outIndex, videoInfo.size != 0);
            }
            if ((videoInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                LogHelper.i(TAG, "End of file out");
                if (loopMode) {
                    LogHelper.i(TAG, "Loop mode, re-streaming file");
                    videoExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                    videoDecoder.flush();
                } else {
                    videoDecoderListener.onVideoDecoderFinished();
                }
            }
        }
    }

    public void setLoopMode(boolean loopMode)
    {
        this.loopMode = loopMode;
    }

    public int getWidth()
    {
        return width;
    }

    public int getHeight()
    {
        return height;
    }
}
