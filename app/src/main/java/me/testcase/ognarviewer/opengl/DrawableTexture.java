/*
 * Copyright © 2024 Ivan Akulinchev <ivan.akulinchev@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */

package me.testcase.ognarviewer.opengl;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import androidx.annotation.Nullable;

public class DrawableTexture {
    private static final int TARGET = GLES20.GL_TEXTURE_2D;

    private final int mHandle;
    private final int mWidth;
    private final int mHeight;

    public DrawableTexture(@Nullable Drawable drawable) {
        assert drawable != null;
        final Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        mWidth = bitmap.getWidth();
        mHeight = bitmap.getHeight();

        final int[] handles = new int[1];
        GLES20.glGenTextures(1, handles, 0);
        mHandle = handles[0];
        GLES20.glBindTexture(TARGET, mHandle);
        GLUtils.texImage2D(TARGET, 0, bitmap, 0);
        bitmap.recycle();
        GLES20.glTexParameteri(TARGET, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(TARGET, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(TARGET, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(TARGET, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    public int getHandle() {
        return mHandle;
    }

    /**
     * Returns the texture width in pixels (px).
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * Returns the texture height in pixels (px).
     */
    public int getHeight() {
        return mHeight;
    }
}
