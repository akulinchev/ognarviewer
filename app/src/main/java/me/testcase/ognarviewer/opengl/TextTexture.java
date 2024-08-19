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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class TextTexture {
    private static final int CACHE_SIZE = 100;
    private static final Map<String, TextTexture> HASH_TABLE = new HashMap<>(CACHE_SIZE);

    private int mHandle;
    private final String mText;
    private final float mWidth;
    private final float mHeight;

    @Nullable
    private TextTexture mNext;
    @Nullable
    private TextTexture mPrevious;
    @Nullable
    private static TextTexture sFirst;
    @Nullable
    private static TextTexture sLast;

    private TextTexture(@NonNull String text, float density) {
        mText = text;

        final Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(18 * density);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(density * 2);

        final Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        final int densityInt = (int) Math.ceil(density);
        bounds.inset(-densityInt, -densityInt);

        final Bitmap bitmap = Bitmap.createBitmap(bounds.width(), bounds.height(),
                Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        // OpenGL expects the first texture pixel is bottom-left, but Bitmap returns the top-left.
        // Simply mirror the Canvas vertically instead of bytes manipulation.
        canvas.scale(1, -1);
        canvas.drawText(text, -bounds.left, -bounds.bottom, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        canvas.drawText(text, -bounds.left, -bounds.bottom, paint);

        final int[] handles = new int[1];
        GLES20.glGenTextures(1, handles, 0);
        mHandle = handles[0];
        GLES20.glActiveTexture(GLES20.GL_TEXTURE15);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, handles[0]);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);

        bitmap.recycle();

        mWidth = bounds.width() / density;
        mHeight = bounds.height() / density;
    }

    public int getHandle() {
        return mHandle;
    }

    @NonNull
    public String getText() {
        return mText;
    }

    public float getWidth() {
        return mWidth;
    }

    public float getHeight() {
        return mHeight;
    }

    private void release() {
        final int[] handles = new int[]{mHandle};
        GLES20.glDeleteTextures(1, handles, 0);
        mHandle = 0;
        mNext = null;
        mPrevious = null;
        /*if (BuildConfig.DEBUG) {
            AppWatcher.INSTANCE.getObjectWatcher().expectWeaklyReachable(this,
                    "TextTexture.release() was called, the texture is unusable now");
        }*/
    }

    @NonNull
    public static TextTexture of(@NonNull String text, float density) {
        TextTexture texture = HASH_TABLE.get(text);
        if (texture == null) {
            if (HASH_TABLE.size() > CACHE_SIZE) {
                // We have too many textures now. Delete the least recently used before creating a
                // new one.
                assert sLast != null;
                HASH_TABLE.remove(sLast.getText());
                final TextTexture last = sLast;
                assert sLast.mPrevious != null;
                sLast = sLast.mPrevious;
                sLast.mNext = null;
                last.release();
            }
            texture = new TextTexture(text, density);
            HASH_TABLE.put(text, texture);
            texture.mNext = sFirst;
            if (sFirst != null) {
                sFirst.mPrevious = texture;
            }
            sFirst = texture;
            if (sLast == null) {
                sLast = texture;
            }
        } else if (sFirst != texture) {
            assert sFirst != null;
            texture.mNext = sFirst;
            sFirst.mPrevious = texture;
            sFirst = texture;
        }
        return texture;
    }

    public static void clearCache() {
        for (TextTexture texture = sFirst; texture != null; texture = texture.mNext) {
            texture.release();
        }
        HASH_TABLE.clear();
        sFirst = null;
        sLast = null;
    }
}
