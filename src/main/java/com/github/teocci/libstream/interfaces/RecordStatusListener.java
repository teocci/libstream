package com.github.teocci.libstream.interfaces;

import com.github.teocci.libstream.enums.RecordStatus;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2019-May-29
 */
public interface RecordStatusListener
{
    void onStatusChange(RecordStatus status);
}
