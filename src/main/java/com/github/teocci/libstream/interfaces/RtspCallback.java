package com.github.teocci.libstream.interfaces;

import com.github.teocci.libstream.protocols.rtsp.rtsp.RtspServerBase;

/**
 * Be careful: those callbacks won't necessarily be called from the ui thread !
 * <p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Dec-14
 */
public interface RtspCallback
{
    /**
     * Called when an error occurs.
     */
    void onError(RtspServerBase server, Exception e, int error);

    /**
     * Called when streaming starts/stops.
     */
    void onMessage(RtspServerBase server, int message);
}
