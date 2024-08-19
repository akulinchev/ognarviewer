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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import me.testcase.ognarviewer.R;

public class ManualCalibrationView extends FrameLayout implements View.OnClickListener {
    private final PointF mPressedPoint = new PointF();
    private OnManualCalibration mOnManualCalibration;
    private ManualCalibrationHost mHost;

    public ManualCalibrationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.overlay_manual_calibration, this);
        findViewById(R.id.button_done).setOnClickListener(this);
        findViewById(R.id.button_reset).setOnClickListener(this);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mPressedPoint.set(event.getX(), event.getY());
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            final float deltaX = event.getX() - mPressedPoint.x;
            final float deltaY = event.getY() - mPressedPoint.y;
            mPressedPoint.set(event.getX(), event.getY());
            if (mOnManualCalibration != null) {
                mOnManualCalibration.onManualCalibration(deltaX, deltaY);
            }
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    public void setOnManualCalibrationListener(OnManualCalibration listener) {
        mOnManualCalibration = listener;
    }

    public void setHost(ManualCalibrationHost host) {
        mHost = host;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_reset) {
            mOnManualCalibration.onManualCalibrationReset();
        } else if (v.getId() == R.id.button_done) {
            if (mHost != null) {
                mHost.onManualCalibrationFinished();
            }
        }
    }

    public interface OnManualCalibration {
        void onManualCalibration(float dx, float dy);
        void onManualCalibrationReset();
    }

    public interface ManualCalibrationHost {
        void onManualCalibrationFinished();
    }
}
