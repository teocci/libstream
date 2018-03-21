package com.github.teocci.libstream.binders;

import android.os.Binder;

import com.github.teocci.libstream.protocols.rtsp.RtspServerBase;


/**
 * The Binder you obtain when a connection with the Service is established.
 * <p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2018-Feb-26
 */

public class CustonBinder extends Binder
{
    private RtspServerBase service;

    public CustonBinder(RtspServerBase service)
    {
        if (service == null) return;
        this.service = service;
    }

    public RtspServerBase getService()
    {
        return service.getService();
    }
}
