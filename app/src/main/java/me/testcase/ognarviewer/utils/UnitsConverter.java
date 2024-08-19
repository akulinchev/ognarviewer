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

public final class UnitsConverter {
    public static int feetToMetres(int feet) {
        return (int) Math.round(feet / 3.28084);
    }

    public static int metresToFeet(int meters) {
        return (int) Math.round(meters * 3.28084);
    }

    public static double metresToMiles(double meters) {
        return meters / 1609.344;
    }

    public static double metresToNauticalMiles(double meters) {
        return meters / 1852.0;
    }

    public static int knotsToKmh(int knots) {
        return (int) Math.round(knots * 1.852);
    }

    public static int kmhToKnots(int kmh) {
        return (int) Math.round(kmh / 1.852);
    }

    public static int kmhToMph(int kmh) {
        return (int) Math.round(kmh / 1.609344);
    }
}
