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
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import me.testcase.ognarviewer.R;
import me.testcase.ognarviewer.databinding.BottomSheetReceiverBinding;

/**
 * Bottom sheet content view for a receiver.
 */
public class ReceiverBottomSheet extends FrameLayout {
    private final BottomSheetReceiverBinding mBinding;

    public ReceiverBottomSheet(@NonNull Context context) {
        super(context);
        mBinding = BottomSheetReceiverBinding.inflate(LayoutInflater.from(context), this, true);
    }

    /**
     * Set the header color.
     *
     * <p>Font color and the edit button tint are adjusted automatically for contrast.</p>
     *
     * @param color The ARGB color to use.
     */
    public void setColor(int color) {
        final boolean light = Color.luminance(color) >= 0.45;
        mBinding.backdrop.setBackgroundTintList(ColorStateList.valueOf(color));
        mBinding.displayNameTextView.setTextColor(light ? Color.BLACK : Color.WHITE);
        mBinding.textReceiver.setTextColor(light ? 0x99000000 : 0x99ffffff);
    }

    /**
     * Set the display name.
     *
     * @param name Display name.
     */
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            mBinding.displayNameTextView.setText(R.string.not_available);
        } else {
            mBinding.displayNameTextView.setText(name);
        }
    }

    /**
     * Set the version.
     *
     * @param version Version, e.g. "v1.2.3.RPI-GPU".
     */
    public void setVersion(String version) {
        if (version == null || version.trim().isEmpty()) {
            mBinding.textVersion.setText(R.string.not_available);
        } else {
            mBinding.textVersion.setText(version);
        }
    }

    /**
     * Set the NTP offset.
     */
    public void setNtpOffset(double offset) {
        if (Double.isNaN(offset)) {
            mBinding.textNtpOffset.setText(R.string.not_available);
        } else {
            final String text = getContext().getString(R.string.ntp_offset_format, offset);
            mBinding.textNtpOffset.setText(text);
        }
    }

    /**
     * Set the amount of free RAM.
     */
    public void setFreeRam(float ram) {
        if (Float.isNaN(ram)) {
            mBinding.textFreeRam.setText(R.string.not_available);
        } else {
            final String text = getContext().getString(R.string.ram_format, Math.round(ram));
            mBinding.textFreeRam.setText(text);
        }
    }

    /**
     * Set the total RAM.
     */
    public void setTotalRam(float ram) {
        if (Float.isNaN(ram)) {
            mBinding.textTotalRam.setText(R.string.not_available);
        } else {
            final String text = getContext().getString(R.string.ram_format, Math.round(ram));
            mBinding.textTotalRam.setText(text);
        }
    }

    /**
     * Set the CPU temperature.
     */
    public void setCpuTemperature(float temperature) {
        if (Float.isNaN(temperature)) {
            mBinding.textCpuTemp.setText(R.string.not_available);
        } else {
            mBinding.textCpuTemp.setText(getContext().getString(R.string.cpu_temperature_format,
                    temperature));
        }
    }

    /**
     * Set the CPU load.
     */
    public void setCpuLoad(double load) {
        if (Double.isNaN(load)) {
            mBinding.textCpuLoad.setText(R.string.not_available);
        } else {
            mBinding.textCpuLoad.setText(getContext().getString(R.string.cpu_load_format,
                    Math.round(load * 100)));
        }
    }
}
