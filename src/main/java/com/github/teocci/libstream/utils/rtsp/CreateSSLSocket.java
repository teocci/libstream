package com.github.teocci.libstream.utils.rtsp;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * This class is used for secure transport, to use replace socket on RtmpConnection with this and
 * you will have a secure stream under ssl/tls.
 * <p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */

public class CreateSSLSocket
{
    /**
     * @param host variable from RtspConnection
     * @param port variable from RtspConnection
     * @return a socket
     */
    public static Socket createSSlSocket(String host, int port)
    {
        try {
            TLSSocketFactory socketFactory = new TLSSocketFactory();
            return socketFactory.createSocket(host, port);
        } catch (NoSuchAlgorithmException | KeyManagementException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
