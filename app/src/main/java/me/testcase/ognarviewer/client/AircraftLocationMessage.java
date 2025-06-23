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

public class AircraftLocationMessage extends AprsMessage {
    /**
     * Latitude in degrees.
     */
    public double latitude = Double.NaN;

    /**
     * Longitude in degrees.
     */
    public double longitude = Double.NaN;

    /**
     * Altitude in meters.
     */
    public int altitude;

    /**
     * Heading in degrees, 1-360 or 0 if unknown.
     */
    public int heading;

    /**
     * Ground speed in km/h.
     */
    public int groundSpeed;

    /**
     * Aircraft ID including its type, stealth flags, etc.
     */
    public long id;

    /**
     * Climb rate in m/s.
     *
     * <p>May be NaN if missing in the message.</p>
     */
    public double climbRate = Double.NaN;

    /**
     * Turn rate in deg/s.
     *
     * <p>See <a href="https://en.wikipedia.org/wiki/Standard_rate_turn">Wikipedia</a>.</p>
     *
     * <p>May be NaN if missing in the message.</p>
     */
    public double turnRate = Double.NaN;
}
