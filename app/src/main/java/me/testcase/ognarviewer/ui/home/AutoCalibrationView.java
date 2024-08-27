/*
 * Copyright © 2024 Ivan Akulinchev <ivan.akulinchev@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package me.testcase.ognarviewer.ui.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import me.testcase.ognarviewer.R;

public class AutoCalibrationView extends BaseSkyView {
    private final Path mPath = new Path();
    private final PathMeasure mPathMeasure;
    private final float mPathLength;
    private final Paint mPaint = new Paint();
    private final Bitmap mAirplaneBitmap;
    private final Matrix mAirplaneMatrix = new Matrix();

    private float mDistance = 250;

    public AutoCalibrationView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        mPath.moveTo(-212, 97); // bottom left
        mPath.cubicTo(-182, -61, 224, 123, 251, -36); // bottom left to top right
        mPath.cubicTo(265, -117, 128, -209, 51, -180);
        mPath.cubicTo(-79, -131, 87, 180, -44, 227);
        mPath.cubicTo(-111, 250, -225, 167, -212, 97);
        mPath.close();

        mPathMeasure = new PathMeasure(mPath, true);
        mPathLength = mPathMeasure.getLength();

        mPaint.setColor(0x33ffffff);
        mPaint.setStrokeWidth(10);
        mPaint.setStyle(Paint.Style.STROKE);

        final Drawable drawable = ResourcesCompat.getDrawable(context.getResources(),
                R.drawable.airplane, null);
        assert drawable != null;
        mAirplaneBitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        drawable.setBounds(0, 0, mAirplaneBitmap.getWidth(), mAirplaneBitmap.getHeight());
        final Canvas canvas = new Canvas(mAirplaneBitmap);
        drawable.draw(canvas);
    }

    @Override
    protected void drawForeground(Canvas canvas, long elapsed) {
        canvas.translate(getWidth() * 0.5f, getHeight() * 0.5f);
        canvas.drawPath(mPath, mPaint);
        mPathMeasure.getMatrix(mDistance, mAirplaneMatrix,
                PathMeasure.POSITION_MATRIX_FLAG | PathMeasure.TANGENT_MATRIX_FLAG);
        mAirplaneMatrix.preTranslate(-mAirplaneBitmap.getWidth(), -mAirplaneBitmap.getHeight());
        mAirplaneMatrix.preScale(2, 2);
        canvas.drawBitmap(mAirplaneBitmap, mAirplaneMatrix, null);
        mDistance += mPathLength * 0.0005f * elapsed; // 0.05% per ms
        if (mDistance > mPathLength) {
            mDistance = 0;
        }
    }
}
