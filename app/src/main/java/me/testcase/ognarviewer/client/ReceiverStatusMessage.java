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

package me.testcase.ognarviewer.client;

public class ReceiverStatusMessage extends AprsMessage {
    /**
     * Software version without the leading "v".
     */
    public String version;

    /**
     * CPU load.
     */
    public double cpuLoad;

    /**
     * Amount of free RAM in MB.
     */
    public double freeRam;

    /**
     * Total amount of RAM in MB.
     */
    public double totalRam;

    /**
     * NTP offset in ms.
     */
    public double ntpOffset;

    /**
     * CPU temperature in °C.
     */
    public double cpuTemperature;
}
