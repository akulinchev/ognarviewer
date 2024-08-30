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

package me.testcase.ognarviewer.client;

public abstract class AprsMessage {
    public String callSign;

    /**
     * The time at which the values in this message were measured.
     *
     * <p>The time is represented as the number of logical "milliseconds" since the Unix epoch. Note
     * that these are not real milliseconds, but more like the number of days multiplied by
     * <code>86400000</code>. In June and December, anomalies are especially likely due to the
     * <a href="https://en.wikipedia.org/wiki/Leap_second">leap seconds</a>.</p>
     */
    public long timestamp;
}
