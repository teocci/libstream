package com.github.teocci.libstream.input.audio;

import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;

import com.github.teocci.libstream.utils.LogHelper;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public class AudioPostProcessEffect
{
    private static String TAG = LogHelper.makeLogTag(AudioPostProcessEffect.class);

    private int microphoneId;
    private AcousticEchoCanceler acousticEchoCanceler = null;
    private AutomaticGainControl automaticGainControl = null;
    private NoiseSuppressor noiseSuppressor = null;

    public AudioPostProcessEffect(int microphoneId)
    {
        this.microphoneId = microphoneId;
    }

    public void enableAutoGainControl()
    {
        if (AutomaticGainControl.isAvailable() && automaticGainControl == null) {
            automaticGainControl = AutomaticGainControl.create(microphoneId);
            automaticGainControl.setEnabled(true);
            LogHelper.i(TAG, "AutoGainControl enabled");
        } else {
            LogHelper.e(TAG, "This device don't support AutoGainControl");
        }
    }

    public void releaseAutoGainControl()
    {
        if (automaticGainControl != null) {
            automaticGainControl.setEnabled(false);
            automaticGainControl.release();
            automaticGainControl = null;
        }
    }

    public void enableEchoCanceler()
    {
        if (AcousticEchoCanceler.isAvailable() && acousticEchoCanceler == null) {
            acousticEchoCanceler = AcousticEchoCanceler.create(microphoneId);
            acousticEchoCanceler.setEnabled(true);
            LogHelper.i(TAG, "EchoCanceler enabled");
        } else {
            LogHelper.e(TAG, "This device don't support EchoCanceler");
        }
    }

    public void releaseEchoCanceler()
    {
        if (acousticEchoCanceler != null) {
            acousticEchoCanceler.setEnabled(false);
            acousticEchoCanceler.release();
            acousticEchoCanceler = null;
        }
    }

    public void enableNoiseSuppressor()
    {
        if (NoiseSuppressor.isAvailable() && noiseSuppressor == null) {
            noiseSuppressor = NoiseSuppressor.create(microphoneId);
            noiseSuppressor.setEnabled(true);
            LogHelper.i(TAG, "NoiseSuppressor enabled");
        } else {
            LogHelper.e(TAG, "This device don't support NoiseSuppressor");
        }
    }

    public void releaseNoiseSuppressor()
    {
        if (noiseSuppressor != null) {
            noiseSuppressor.setEnabled(false);
            noiseSuppressor.release();
            noiseSuppressor = null;
        }
    }
}
