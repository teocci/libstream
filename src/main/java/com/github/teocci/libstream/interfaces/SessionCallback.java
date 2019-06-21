package com.github.teocci.libstream.interfaces;

import com.github.teocci.libstream.input.video.VideoStream;
import com.github.teocci.libstream.protocols.rtsp.rtsp.Session;


/**
 * The callback interface you need to implement to get some feedback
 * Those will be called from the UI thread.
 * <p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Dec-21
 */

public interface SessionCallback
{
    /**
     * Called periodically to inform you on the bandwidth
     * consumption of the streams when streaming.
     */
    void onBitrateUpdate(long bitrate);

    /**
     * Called when some error occurs.
     */
    void onSessionError(int reason, int streamType, Exception e);

    /**
     * Called when the previw of the {@link VideoStream}
     * has correctly been started.
     * If an error occurs while starting the preview,
     * {@link SessionCallback#onSessionError(int, int, Exception)} will be
     * called instead of {@link SessionCallback#onPreviewStarted()}.
     */
    void onPreviewStarted();

    /**
     * Called when the session has correctly been configured
     * after calling {@link Session#configure()}.
     * If an error occurs while configuring the {@link Session},
     * {@link SessionCallback#onSessionError(int, int, Exception)} will be
     * called instead of  {@link SessionCallback#onSessionConfigured()}.
     */
    void onSessionConfigured();

    /**
     * Called when the streams of the session have correctly been started.
     * If an error occurs while starting the {@link Session},
     * {@link SessionCallback#onSessionError(int, int, Exception)} will be
     * called instead of  {@link SessionCallback#onSessionStarted()}.
     */
    void onSessionStarted();

    /**
     * Called when the stream of the session have been stopped.
     */
    void onSessionStopped();
}