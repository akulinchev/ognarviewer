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

import me.testcase.ognarviewer.utils.AircraftId;

/**
 * An aircraft or moving Target.
 */
public final class Aircraft extends Target {
    private final long mId;
    private volatile int mHeading;
    private volatile int mGroundSpeed;
    private volatile float mClimbRate;
    private volatile double mTurnRate;

    public Aircraft(String callSign, long id) {
        super(callSign);
        mId = id;
    }

    @Override
    public int getColor() {
        return COLORS[(int) getDirectoryId() % COLORS.length];
    }

    public long getDirectoryId() {
        return AircraftId.getDirectoryId(mId);
    }

    /**
     * Returns the aircraft type (e.g. glider, helicopter, etc.) or 0 if unknown.
     *
     * <p>This method is thread safe.</p>
     */
    public int getType() {
        return AircraftId.getAircraftType(mId);
    }

    /**
     * Returns the heading in degrees.
     *
     * <p>If the heading is unknown, returns 0. If the heading is North, returns 360.</p>
     *
     * <p>This method is thread safe.</p>
     */
    public int getHeading() {
        return mHeading;
    }

    /**
     * Sets the heading in degrees.
     *
     * <p>This method is thread safe.</p>
     */
    public void setHeading(int heading) {
        mHeading = heading;
    }

    /**
     * Returns the ground speed in km/h.
     *
     * <p>This method is thread safe.</p>
     */
    public int getGroundSpeed() {
        return mGroundSpeed;
    }

    /**
     * Sets the ground speed in km/h.
     *
     * <p>This method is thread safe.</p>
     */
    public void setGroundSpeed(int speed) {
        mGroundSpeed = speed;
    }

    /**
     * Returns the rate of climb in m/s.
     *
     * <p>This method is thread safe.</p>
     */
    public float getClimbRate() {
        return mClimbRate;
    }

    /**
     * Sets the rate of climb in m/s.
     *
     * <p>This method is thread safe.</p>
     */
    public void setClimbRate(float rate) {
        mClimbRate = rate;
    }

    /**
     * Returns the turn rate in deg/s.
     *
     * <p>This method is thread safe.</p>
     */
    public double getTurnRate() {
        return mTurnRate;
    }

    /**
     * Sets the turn rate in deg/s.
     *
     * <p>This method is thread safe.</p>
     */
    public void setTurnRate(double turnRate) {
        mTurnRate = turnRate;
    }
}
