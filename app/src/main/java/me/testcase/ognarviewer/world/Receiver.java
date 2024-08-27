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

package me.testcase.ognarviewer.world;

/**
 * A receiver or non-moving Target.
 */
public final class Receiver extends Target {
    private String mVersion;
    private double mNtpOffset = Double.NaN;
    private float mFreeRam = Float.NaN;
    private float mTotalRam = Float.NaN;
    private float mCpuTemperature = Float.NaN;
    private double mCpuLoad = Double.NaN;

    public Receiver(String callSign) {
        super(callSign);
    }

    @Override
    public int getColor() {
        return COLORS[Math.abs(getCallSign().hashCode()) % COLORS.length];
    }

    /**
     * Returns the version string.
     */
    public String getVersion() {
        return mVersion;
    }

    /**
     * Sets the version string.
     */
    public void setVersion(String version) {
        mVersion = version;
    }

    /**
     * Returns NTP offset in ms.
     */
    public double getNtpOffset() {
        return mNtpOffset;
    }

    /**
     * Sets NTP offset in ms.
     */
    public void setNtpOffset(double offset) {
        mNtpOffset = offset;
    }

    /**
     * Returns the free RAM in MB.
     */
    public float getFreeRam() {
        return mFreeRam;
    }

    /**
     * Sets the free RAM in MB.
     */
    public void setFreeRam(float amount) {
        mFreeRam = amount;
    }

    /**
     * Returns the total RAM in MB.
     */
    public float getTotalRam() {
        return mTotalRam;
    }

    /**
     * Sets the total RAM in MB.
     */
    public void setTotalRam(float amount) {
        mTotalRam = amount;
    }

    /**
     * Returns the CPU temperature in °C.
     */
    public float getCpuTemperature() {
        return mCpuTemperature;
    }

    /**
     * Sets the CPU temperature in °C.
     */
    public void setCpuTemperature(float temperature) {
        mCpuTemperature = temperature;
    }

    /**
     * Returns the CPU load.
     */
    public double getCpuLoad() {
        return mCpuLoad;
    }

    /**
     * Sets the CPU load.
     */
    public void setCpuLoad(double load) {
        mCpuLoad = load;
    }
}
