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

package me.testcase.ognarviewer;

import android.location.Location;

import androidx.annotation.MainThread;

/**
 * An "exact" GPS-based clock, because the system time may be incorrect.
 */
public class CalibratedClock {
    /**
     * Time error.
     *
     * <p>If zero, the system time is in sync with the GPS time. If negative, the system time is
     * ahead of the GPS time. If positive, the system time is behind the GPS time.</p>
     */
    private static volatile long sTimeError;

    /**
     * Get the current time in milliseconds.
     *
     * <p>The time is believed to be "correct", even if an incorrect time is set in the system.</p>
     */
    public static long currentTimeMillis() {
        return System.currentTimeMillis() + sTimeError;
    }

    public static long getTimeError() {
        return sTimeError;
    }

    @MainThread
    public static void sync(Location location) {
        sTimeError = location.getTime() - System.currentTimeMillis();
        if (Math.abs(sTimeError) <= 50) {
            // It's more likely a propagation delay than an incorrect system clock.
            sTimeError = 0;
        }
    }
}
