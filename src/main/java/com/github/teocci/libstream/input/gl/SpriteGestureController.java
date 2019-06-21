package com.github.teocci.libstream.input.gl;

import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;

import com.github.teocci.libstream.input.gl.render.objects.ObjectBaseFilterRender;

import static com.github.teocci.libstream.utils.CameraHelper.getFingerSpacing;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public class SpriteGestureController
{
    private ObjectBaseFilterRender objectBaseFilterRender;
    private float lastDistance;
    private boolean preventMoveOutside = true;

    public SpriteGestureController()
    {
    }

    public SpriteGestureController(ObjectBaseFilterRender sprite)
    {
        this.objectBaseFilterRender = sprite;
    }

    public ObjectBaseFilterRender getObjectBaseFilterRender()
    {
        return objectBaseFilterRender;
    }

    public void setObjectBaseFilterRender(ObjectBaseFilterRender ObjectBaseFilterRender)
    {
        this.objectBaseFilterRender = ObjectBaseFilterRender;
    }

    public void setPreventMoveOutside(boolean preventMoveOutside)
    {
        this.preventMoveOutside = preventMoveOutside;
    }

    public boolean spriteTouched(View view, MotionEvent motionEvent)
    {
        if (objectBaseFilterRender == null) return false;
        float xPercent = motionEvent.getX() * 100 / view.getWidth();
        float yPercent = motionEvent.getY() * 100 / view.getHeight();
        PointF scale = objectBaseFilterRender.getScale();
        PointF position = objectBaseFilterRender.getPosition();
        boolean xTouched = xPercent >= position.x && xPercent <= position.x + scale.x;
        boolean yTouched = yPercent >= position.y && yPercent <= position.y + scale.y;
        return xTouched && yTouched;
    }

    public void moveSprite(View view, MotionEvent motionEvent)
    {
        if (objectBaseFilterRender == null) return;
        if (motionEvent.getPointerCount() == 1) {
            float xPercent = motionEvent.getX() * 100 / view.getWidth();
            float yPercent = motionEvent.getY() * 100 / view.getHeight();
            PointF scale = objectBaseFilterRender.getScale();
            if (preventMoveOutside) {
                float x = xPercent - scale.x / 2.0F;
                float y = yPercent - scale.y / 2.0F;
                if (x < 0) {
                    x = 0;
                }
                if (x + scale.x > 100.0F) {
                    x = 100.0F - scale.x;
                }
                if (y < 0) {
                    y = 0;
                }
                if (y + scale.y > 100.0F) {
                    y = 100.0F - scale.y;
                }
                objectBaseFilterRender.setPosition(x, y);
            } else {
                objectBaseFilterRender.setPosition(xPercent - scale.x / 2f, yPercent - scale.y / 2f);
            }
        }
    }

    public void scaleSprite(MotionEvent motionEvent)
    {
        if (objectBaseFilterRender == null) return;
        if (motionEvent.getPointerCount() > 1) {
            float distance = getFingerSpacing(motionEvent);
            float percent = distance >= lastDistance ? 1 : -1;
            PointF scale = objectBaseFilterRender.getScale();
            scale.x += percent;
            scale.y += percent;
            objectBaseFilterRender.setScale(scale.x, scale.y);
            lastDistance = distance;
        }
    }
}
