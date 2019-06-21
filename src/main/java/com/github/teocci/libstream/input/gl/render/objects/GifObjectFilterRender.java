package com.github.teocci.libstream.input.gl.render.objects;

import android.opengl.GLES20;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.github.teocci.libstream.utils.gl.GifStreamObject;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class GifObjectFilterRender extends ObjectBaseFilterRender
{
    public GifObjectFilterRender()
    {
        super();
        streamObject = new GifStreamObject();
    }

    @Override
    protected void drawFilter()
    {
        super.drawFilter();
        int position = ((GifStreamObject) streamObject).updateFrame(streamObjectTextureId.length);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, streamObjectTextureId[position]);
        // Set alpha. 0f if no image loaded.
        GLES20.glUniform1f(uAlphaHandle, streamObjectTextureId[position] == -1 ? 0f : alpha);
    }

    public void setGif(InputStream inputStream) throws IOException
    {
        ((GifStreamObject) streamObject).load(inputStream);
        textureLoader.setGifStreamObject((GifStreamObject) streamObject);
        shouldLoad = true;
    }
}
