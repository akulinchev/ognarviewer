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

package me.testcase.ognarviewer.ui.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseSkyView extends View {
    /**
     * Last time (in ms since boot) when onDraw() was called.
     */
    private long mLastTime;
    private boolean mIsPaused = true;

    private final List<Cloud> mClouds = new ArrayList<>();

    private final Paint mSkyPaint = new Paint();
    private final Rect mSkyRect = new Rect();
    private final RectF mPathBounds = new RectF();
    private final Bitmap mCloudBitmap;

    /**
     * An all-purpose RectF to avoid memory allocation in draw().
     */
    private final RectF mTempRect = new RectF();

    public BaseSkyView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        final Paint paint = new Paint();
        paint.setARGB(90, 255, 255, 255);
        final Path path = new Path();
        path.moveTo(19.35f, 10);
        path.cubicTo(18.67f, 6.59f, 15.64f, 4, 12, 4);
        path.cubicTo(9.11f, 4, 6.6f, 5.64f, 5.35f, 8.04f);
        path.cubicTo(2.34f, 8.36f, 0, 10.91f, 0, 14);
        path.rCubicTo(0, 3.31f, 2.69f, 6, 6, 6);
        path.rLineTo(13, 0);
        path.rCubicTo(2.76f, 0, 5, -2.24f, 5, -5);
        path.rCubicTo(0, -2.64f, -2.05f, -4.78f, -4.65f, -4.96f);
        path.close();
        path.computeBounds(mPathBounds, true);

        // Rasterize the path to a bitmap. Otherwise, performance is too low on my Pixel.
        // TODO: just ship it as a bitmap?
        mCloudBitmap = Bitmap.createBitmap((int) Math.ceil(mPathBounds.width() * 10),
                (int) Math.ceil(mPathBounds.height() * 10), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(mCloudBitmap);
        canvas.scale(10, 10);
        canvas.drawPath(path, paint);

        for (int i = 0; i < 30; ++i) {
            mClouds.add(new Cloud());
        }
    }

    protected abstract void drawForeground(Canvas canvas, long elapsed);

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        mSkyPaint.setShader(new LinearGradient(0, 0, 0, h, 0x660277bd, 0xffffffff,
                Shader.TileMode.CLAMP));
        mSkyRect.right = w;
        mSkyRect.bottom = h;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (mIsPaused) {
            mLastTime = 0;
            return;
        }

        final long now = SystemClock.uptimeMillis();
        if (mLastTime == 0) {
            mLastTime = now - 30;
        }
        final long elapsed = now - mLastTime;
        mLastTime = now;

        canvas.drawRect(mSkyRect, mSkyPaint);

        final int width = getWidth();
        final int height = getHeight();

        for (Cloud cloud : mClouds) {
            mTempRect.left = cloud.x;
            mTempRect.top =
                    (float) Math.sin(cloud.x / 100f) * 25 + height - mPathBounds.height() * 15;
            mTempRect.right = mTempRect.left + mCloudBitmap.getWidth() * 15f / cloud.scale;
            mTempRect.bottom = mTempRect.top + mCloudBitmap.getHeight() * 15f / cloud.scale;
            canvas.drawBitmap(mCloudBitmap, null, mTempRect, null);
            if (cloud.x > width) {
                cloud.x = -300;
            } else {
                cloud.x += elapsed / 30f * cloud.speed * 2;
            }
        }

        drawForeground(canvas, elapsed);

        postInvalidateOnAnimation();
    }

    public void onPause() {
        mIsPaused = true;
    }

    public void onResume() {
        mIsPaused = false;
        mLastTime = 0;
        invalidate();
    }

    private static class Cloud {
        public int x = (int) (Math.random() * (1080 + 300) - 300);
        public int speed = (int) (Math.random() * 2 + 1);
        public int scale = (int) (Math.random() * 20 + 5);
    }
}
