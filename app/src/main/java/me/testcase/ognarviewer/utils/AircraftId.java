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

package me.testcase.ognarviewer.utils;

public final class AircraftId {
    public static final int ADDRESS_TYPE_RANDOM = 0;
    public static final int ADDRESS_TYPE_ICAO = 1;
    public static final int ADDRESS_TYPE_FLARM = 2;
    public static final int ADDRESS_TYPE_OGN = 3;

    private static final long ADDRESS_MASK = 0x00ffffff;
    private static final long ADDRESS_TYPE_MASK = 0x03000000;
    private static final long AIRCRAFT_TYPE_MASK = 0x3C000000;

    public static int getAddress(long id) {
        return (int) (id & ADDRESS_MASK);
    }

    public static int getAddressType(long id) {
        return (int) ((id & ADDRESS_TYPE_MASK) >> 24);
    }

    public static int getAircraftType(long id) {
        return (int) ((id & AIRCRAFT_TYPE_MASK) >> 26);
    }

    public static long getDirectoryId(long id) {
        return id & (ADDRESS_TYPE_MASK | ADDRESS_MASK);
    }
}
