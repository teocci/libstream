package com.github.teocci.libstream.enums;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.IntDef;

import static com.github.teocci.libstream.utils.gl.GlUtil.BOTTOM;
import static com.github.teocci.libstream.utils.gl.GlUtil.BOTTOM_LEFT;
import static com.github.teocci.libstream.utils.gl.GlUtil.BOTTOM_RIGHT;
import static com.github.teocci.libstream.utils.gl.GlUtil.CENTER;
import static com.github.teocci.libstream.utils.gl.GlUtil.LEFT;
import static com.github.teocci.libstream.utils.gl.GlUtil.RIGHT;
import static com.github.teocci.libstream.utils.gl.GlUtil.TOP;
import static com.github.teocci.libstream.utils.gl.GlUtil.TOP_LEFT;
import static com.github.teocci.libstream.utils.gl.GlUtil.TOP_RIGHT;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jun-05
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
@IntDef({CENTER, LEFT, RIGHT, TOP, BOTTOM, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT})
public @interface TranslateTo {}