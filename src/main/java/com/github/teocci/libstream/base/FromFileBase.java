package com.github.teocci.libstream.base;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaPlayer;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Pair;

import com.github.teocci.libstream.coder.encoder.video.VideoEncoder;
import com.github.teocci.libstream.input.video.Frame;
import com.github.teocci.libstream.input.video.VideoQuality;
import com.github.teocci.libstream.interfaces.video.CameraSinker;
import com.github.teocci.libstream.interfaces.video.EncoderSinker;
import com.github.teocci.libstream.interfaces.video.VideoDecoderListener;
import com.github.teocci.libstream.utils.LogHelper;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.github.teocci.libstream.enums.FormatVideoEncoder.SURFACE;
import static com.github.teocci.libstream.utils.Utils.minAPI19;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class FromFileBase implements CameraSinker, EncoderSinker
{
    private static String TAG = LogHelper.makeLogTag(Camera2Base.class);

    protected VideoEncoder videoEncoder;
    //record
    private MediaMuxer mediaMuxer;
    private int videoTrack = -1;

    private boolean streaming;
    private boolean videoEnabled = true;
    private boolean recording = false;
    private boolean canRecord = false;
    private MediaFormat videoFormat;

    private VideoDecoderListener videoDecoderInterface;
    private MediaPlayer mediaPlayer;

    public FromFileBase(VideoDecoderListener videoDecoderInterface)
    {
        this.videoDecoderInterface = videoDecoderInterface;
        videoEncoder = new VideoEncoder(this);
        //videoDecoder = new VideoDecoder(videoDecoderInterface);
        streaming = false;
    }

    @Override
    public void onPSReady(Pair<ByteBuffer, ByteBuffer> psPair)
    {
        sendAVCInfo(psPair.first, psPair.second, null);
    }

    @Override
    public void onSpsPpsVpsReady(ByteBuffer sps, ByteBuffer pps, ByteBuffer vps)
    {
        sendAVCInfo(sps, pps, vps);
    }

    @Override
    public void onEncodedData(ByteBuffer videoBuffer, MediaCodec.BufferInfo info)
    {
        if (recording) {
            if (info.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) canRecord = true;
            if (canRecord) {
                mediaMuxer.writeSampleData(videoTrack, videoBuffer, info);
            }
        }
        getH264DataRtp(videoBuffer, info);
    }

    @Override
    public void onYUVData(byte[] buffer)
    {
        videoEncoder.onYUVData(buffer);
    }

    @Override
    public void onYUVData(Frame frame)
    {
        videoEncoder.onYUVData(frame);
    }

    @Override
    public void onVideoFormat(MediaFormat mediaFormat)
    {
        videoFormat = mediaFormat;
    }

    public boolean prepareVideo(String filePath, int bitRate) throws IOException
    {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(mediaPlayer -> videoDecoderInterface.onVideoDecoderFinished());
        mediaPlayer.setDataSource(filePath);
        mediaPlayer.prepare();
        mediaPlayer.setVolume(0, 0);
        //if (!videoDecoder.initExtractor(filePath)) return false;
        VideoQuality quality = new VideoQuality(mediaPlayer.getVideoWidth(), mediaPlayer.getVideoHeight(), 30, bitRate);
        boolean result = videoEncoder.prepare(quality, true, 0, SURFACE);
        mediaPlayer.setSurface(videoEncoder.getInputSurface());
        //videoDecoder.prepareVideo(videoEncoder.getInputSurface());
        return result;
    }

    /*Need be called while stream*/
    public void startRecord(String path) throws IOException
    {
        if (streaming) {
            mediaMuxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            videoTrack = mediaMuxer.addTrack(videoFormat);
            mediaMuxer.start();
            recording = true;
        } else {
            throw new IOException("Need be called while stream");
        }
    }

    public void stopRecord()
    {
        recording = false;
        if (mediaMuxer != null) {
            mediaMuxer.stop();
            mediaMuxer.release();
            mediaMuxer = null;
        }
        videoTrack = -1;
    }

    public void startStream(String url)
    {
        startStreamRtp(url);
        videoEncoder.start();
        //videoDecoder.start();
        mediaPlayer.start();
        streaming = true;
    }

    public void stopStream()
    {
        stopStreamRtp();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        //videoDecoder.stop();
        videoEncoder.stop();
        streaming = false;
    }

    public void setLoopMode(boolean loopMode)
    {
        //videoDecoder.setLoopMode(loopMode);
        mediaPlayer.setLooping(loopMode);
    }

    public void disableVideo()
    {
        videoEncoder.startSendBlackImage();
        videoEnabled = false;
    }

    public void enableVideo()
    {
        videoEncoder.stopSendBlackImage();
        videoEnabled = true;
    }

    /**
     * need min API 19
     */
    public void setVideoBitrateOnFly(int bitrate)
    {
        if (minAPI19()) {
            videoEncoder.setVideoBitrateOnFly(bitrate);
        }
    }



    public boolean isVideoEnabled()
    {
        return videoEnabled;
    }

    public boolean isStreaming()
    {
        return streaming;
    }


    // Abstract Methods

    public abstract void setAuthorization(String user, String password);

    protected abstract void startStreamRtp(String url);

    protected abstract void stopStreamRtp();

    protected abstract void sendAVCInfo(ByteBuffer sps, ByteBuffer pps, ByteBuffer vps);

    protected abstract void getH264DataRtp(ByteBuffer h264Buffer, MediaCodec.BufferInfo info);
}
