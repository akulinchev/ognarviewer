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
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.List;

import me.testcase.ognarviewer.R;

public class AirTrafficView extends BaseSkyView {
    private final List<Airplane> mAirplanes = new ArrayList<>();

    private final Bitmap mAirplaneBitmap;
    private final Bitmap mFlippedAirplaneBitmap;

    /**
     * An all-purpose RectF to avoid memory allocation in drawForeground().
     */
    private final RectF mTempRect = new RectF();

    public AirTrafficView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        for (int i = 1; i <= 3; ++i) {
            mAirplanes.add(new Airplane(i));
        }

        // Make sure not all airplanes start in the same direction.
        boolean allLeftToRight = true;
        boolean allRightToLeft = true;
        for (Airplane a : mAirplanes) {
            allLeftToRight &= a.isLeftToRight;
            allRightToLeft &= !a.isLeftToRight;
        }
        if (allLeftToRight) {
            mAirplanes.get(0).isLeftToRight = false;
        } else if (allRightToLeft) {
            mAirplanes.get(0).isLeftToRight = true;
        }

        // TODO: use RenderNode once API level 28 is not supported anymore.

        final Drawable drawable = ResourcesCompat.getDrawable(context.getResources(),
                R.drawable.airplane_with_trail, null);
        assert drawable != null;
        mAirplaneBitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        drawable.setBounds(0, 0, mAirplaneBitmap.getWidth(), mAirplaneBitmap.getHeight());
        final Canvas canvas = new Canvas(mAirplaneBitmap);
        drawable.draw(canvas);

        mFlippedAirplaneBitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas flippedCanvas = new Canvas(mFlippedAirplaneBitmap);
        flippedCanvas.translate(mFlippedAirplaneBitmap.getWidth(), 0);
        flippedCanvas.scale(-1, 1);
        drawable.draw(flippedCanvas);
    }

    @Override
    protected void drawForeground(Canvas canvas, long elapsed) {
        final int width = getWidth();
        final int height = getHeight();

        for (Airplane a : mAirplanes) {
            mTempRect.left = a.x;
            mTempRect.top = height - a.flightLevel * height / 4;
            mTempRect.right = mTempRect.left + mAirplaneBitmap.getWidth() * 2;
            mTempRect.bottom = mTempRect.top + mAirplaneBitmap.getHeight() * 2;
            canvas.drawBitmap(a.isLeftToRight ? mAirplaneBitmap : mFlippedAirplaneBitmap, null,
                    mTempRect, null);
            if (a.x > width || a.x < -mAirplaneBitmap.getWidth() * 2 - 1) {
                a.isLeftToRight = Math.random() < 0.5;
                a.x = a.isLeftToRight ? (int) Math.ceil(-mTempRect.width()) : width;
            } else {
                a.x += (int) (a.speed * elapsed / 1000.0) * (a.isLeftToRight ? 1 : -1);
            }
        }
    }

    private static class Airplane {
        public final int flightLevel;
        public int x;
        public int speed = (int) (Math.random() * 300 + 300);
        public boolean isLeftToRight = Math.random() < 0.5;

        public Airplane(int level) {
            flightLevel = level;
        }
    }
}
