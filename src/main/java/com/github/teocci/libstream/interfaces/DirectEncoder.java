package com.github.teocci.libstream.interfaces;

import android.hardware.Camera;
import android.view.SurfaceView;

import com.github.teocci.libstream.enums.ColorEffect;
import com.github.teocci.libstream.enums.Protocol;
import com.github.teocci.libstream.input.audio.AudioQuality;
import com.github.teocci.libstream.input.video.VideoQuality;

import java.util.List;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Feb-23
 */

public interface DirectEncoder
{
    void initEncoder();

    void setEffect(ColorEffect effect);

    void setSurfaceView(SurfaceView surfaceView);

    void setProtocol(Protocol protocol);

    void setVideoBitrateOnFly(int bitrate);

    void startPreview();

    void stopStream();

    boolean endStream();

    void enableAudio();

    void disableAudio();

    void enableVideo();

    void disableVideo();

    void switchCamera();

    void startStream();

    Camera.Size getBackCamResolution(int index);

    List<String> getBackCamResolutionList();

    int getBackCamResolutionIndex(int width, int height);

    long getBitrate();

    boolean prepareAudio(AudioQuality quality);

    boolean prepareVideo(VideoQuality quality);

    boolean prepareAudio(AudioQuality quality, boolean echoCanceler, boolean noiseSuppressor);

    boolean prepareVideo(VideoQuality quality, boolean hardwareRotation, int rotation);

    boolean isAudioMuted();

    boolean isVideoEnabled();

    /**
     * Returns whether or not the RTSP server is streaming to some client(s).
     */
    boolean isStreaming();
}
