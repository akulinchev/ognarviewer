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

package me.testcase.ognarviewer.utils;

import android.location.Location;

public final class LocationObfuscator {
    public static final int COARSE_ACCURACY_KM = 5;

    private static final double EARTH_RADIUS = 6378.137;
    private static final double LATITUDE_PER_KM = 360 / (2 * Math.PI * EARTH_RADIUS);
    private static final double MAX_LATITUDE = 85;

    public static Location obfuscate(Location fine) {
        final Location coarse = new Location("obfuscated");
        final double latitudeAccuracy = LATITUDE_PER_KM * COARSE_ACCURACY_KM;
        coarse.setLatitude(Math.round(fine.getLatitude() / latitudeAccuracy) * latitudeAccuracy);
        final double longitudeAccuracy = latitudeAccuracy / Math.cos(Math.min(MAX_LATITUDE,
                Math.abs(coarse.getLatitude())));
        coarse.setLongitude(
                Math.round(fine.getLongitude() / longitudeAccuracy) * longitudeAccuracy);
        if (coarse.getLongitude() > 180) {
            coarse.setLongitude(coarse.getLongitude() - 360);
        } else if (coarse.getLongitude() < -180) {
            coarse.setLongitude(coarse.getLongitude() + 360);
        }
        if (coarse.getLatitude() > 90) {
            coarse.setLatitude(90);
        }
        return coarse;
    }
}
