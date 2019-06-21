package com.github.teocci.libstream.protocols.rtsp.ntp;

import com.github.teocci.libstream.utils.LogHelper;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Nov-30
 */
public class AtomicTime extends Thread
{
    private static String TAG = LogHelper.makeLogTag(AtomicTime.class);

    private static final String SERVER_NAME = "pool.ntp.org";

    private volatile boolean initialized = false;

    public AtomicTime()
    {
        start();
    }

    public void run()
    {
        long currentTime = System.currentTimeMillis();
        NTPUDPClient client = new NTPUDPClient();
        // We want to timeout if a response takes longer than 2 seconds
        client.setDefaultTimeout(2000);

        try {
            InetAddress inetAddress = InetAddress.getByName(SERVER_NAME);
            TimeInfo timeInfo = client.getTime(inetAddress);
            if (timeInfo.getReturnTime() >= currentTime) {
                initialized = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            client.close();
            LogHelper.e(TAG, "has been initialized? " + (initialized ? "true" : "false"));
        }
    }

    public long getTime()
    {
        return initialized ? TimeStamp.getCurrentTime().getTime() : System.nanoTime();
    }
}
