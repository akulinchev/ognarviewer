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

package me.testcase.ognarviewer.world;

import android.graphics.Color;

public abstract class Target {
    // TODO: move these constants to resources.
    private static final int RED500 = Color.argb(0xff, 0xf4, 0x43, 0x36);
    private static final int PINK500 = Color.argb(0xff, 0xE9, 0x1E, 0x63);
    private static final int PURPLE300 = Color.argb(0xff, 0xBA, 0x68, 0xC8);
    private static final int DPURPLE600 = Color.argb(0xff, 0x5E, 0x35, 0xB1);
    private static final int INDIGO300 = Color.argb(0xff, 0x79, 0x86, 0xCB);
    private static final int LBLUE300 = Color.argb(0xff, 0x4F, 0xC3, 0xF7);
    private static final int CYAN300 = Color.argb(0xff, 0x4D, 0xD0, 0xE1);
    private static final int TEAL300 = Color.argb(0xff, 0x4D, 0xB6, 0xAC);
    private static final int GREEN500 = Color.argb(0xff, 0x4C, 0xAF, 0x50);
    private static final int LIME300 = Color.argb(0xff, 0xDC, 0xE7, 0x75);
    private static final int AMBER400 = Color.argb(0xff, 0xFF, 0xCA, 0x28);
    private static final int ORANGE400 = Color.argb(0xff, 0xFF, 0xA7, 0x26);
    private static final int BGRAY300 = Color.argb(0xff, 0x90, 0xA4, 0xAE);

    public static final int[] COLORS = new int[] {
            RED500,
            PINK500,
            PURPLE300,
            DPURPLE600,
            INDIGO300,
            LBLUE300,
            CYAN300,
            TEAL300,
            GREEN500,
            LIME300,
            AMBER400,
            ORANGE400,
            BGRAY300,
    };

    private final String mCallSign;
    private volatile double mLatitude;
    private volatile double mLongitude;
    private volatile double mAltitude;
    private volatile long mPositionTime;

    public Target(String callSign) {
        mCallSign = callSign;
    }

    public final String getCallSign() {
        return mCallSign;
    }

    public abstract int getColor();

    public final double getLatitude() {
        return mLatitude;
    }

    public final double getLongitude() {
        return mLongitude;
    }

    /**
     * Returns the altitude in meters over MSL.
     */
    public final double getAltitude() {
        return mAltitude;
    }

    public final long getPositionTime() {
        return mPositionTime;
    }

    public final void setPosition(double lat, double lon, double alt, long timestamp) {
        if (timestamp > mPositionTime) {
            mLatitude = lat;
            mLongitude = lon;
            mAltitude = alt;
            mPositionTime = timestamp;
        }
    }
}
