package com.github.teocci.libstream.input.gl.render.objects;

import android.opengl.GLES20;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.github.teocci.libstream.utils.gl.TextStreamObject;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class TextObjectFilterRender extends ObjectBaseFilterRender
{
    public TextObjectFilterRender()
    {
        super();
        streamObject = new TextStreamObject();
    }

    @Override
    protected void drawFilter()
    {
        super.drawFilter();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, streamObjectTextureId[0]);
        //Set alpha. 0f if no image loaded.
        GLES20.glUniform1f(uAlphaHandle, streamObjectTextureId[0] == -1 ? 0f : alpha);
    }

    public void setText(String text, float textSize, int textColor)
    {
        ((TextStreamObject) streamObject).load(text, textSize, textColor);
        textureLoader.setTextStreamObject((TextStreamObject) streamObject);
        shouldLoad = true;
    }
}
