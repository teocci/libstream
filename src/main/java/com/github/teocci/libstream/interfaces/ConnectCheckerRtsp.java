package com.github.teocci.libstream.interfaces;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public interface ConnectCheckerRtsp
{
    void onConnectionSuccessRtsp();

    void onConnectionFailedRtsp(String reason);

    void onDisconnectRtsp();

    void onAuthErrorRtsp();

    void onAuthSuccessRtsp();
}
