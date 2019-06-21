package com.github.teocci.libstream.threads;

import com.github.teocci.libstream.protocols.rtsp.rtsp.RtspServerBase;
import com.github.teocci.libstream.utils.LogHelper;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import static com.github.teocci.libstream.threads.WorkerThread.ERROR_BIND_FAILED;
import static com.github.teocci.libstream.utils.rtsp.RtpConstants.IPTOS_LOWDELAY;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Dec-15
 */

public class RequestWorker extends Thread implements Runnable
{
    private static String TAG = LogHelper.makeLogTag(RequestWorker.class);

    private final ServerSocket server;
    private RtspServerBase rtspServer;

    public RequestWorker(RtspServerBase rtspServer, int rtspPort) throws IOException
    {
        this.rtspServer = rtspServer;
        try {
            server = new ServerSocket(rtspPort);
            start();
        } catch (BindException e) {
            LogHelper.e(TAG, "Port already in use !");
            rtspServer.postError(e, ERROR_BIND_FAILED);
            throw e;
        }
    }

    public void run()
    {
        LogHelper.e(TAG, "RTSP server listening on port " + server.getLocalPort());
        while (!Thread.interrupted()) {
            try {
                Socket socket = server.accept();
                socket.setTcpNoDelay(true);
                socket.setTrafficClass(IPTOS_LOWDELAY);

                new WorkerThread(rtspServer, socket).start();
                LogHelper.e(TAG, "RTSP user");
            } catch (SocketException e) {
                if (!e.getLocalizedMessage().equals("Socket closed")) break;
            } catch (Exception e) {
                LogHelper.e(TAG, e.getMessage());
            }
        }
        LogHelper.e(TAG, "RTSP server stopped !");
    }

    public void kill()
    {
        try {
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            this.join();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }
}
