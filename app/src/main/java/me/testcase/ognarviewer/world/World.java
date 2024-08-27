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

import android.hardware.GeomagneticField;
import android.location.Location;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import me.testcase.ognarviewer.CalibratedClock;

/**
 * A list of aircraft and receivers with some additional features.
 */
public final class World {
    // FIXME: it's the horizontal radius. Take the vertical radius into account too.
    public static final double EARTH_RADIUS = 6378137; // WGS 84

    private static final double MAX_PREDICTION_TIME = 10.0; // seconds

    // 5 for moving, 20 for not, add 10 seconds for sure.
    private static final long EXPIRE_TIME_AIRCRAFT = 30;
    // Default is 5 minutes, add 30 seconds for sure.
    private static final long EXPIRE_TIME_RECEIVER = 300 + 30;

    private final Map<String, Target> mTargetMap = new HashMap<>();
    private volatile double mLatitude;
    private volatile double mLongitude;
    private volatile double mAltitude;
    private volatile GeomagneticField mGeomagneticField;
    private volatile boolean mDemoMode;
    private volatile boolean mLocationPrediction;
    private volatile double mGeoidHeight;

    // TODO: just pass lat, lon and alt as arguments, do not depend on Location.
    @MainThread
    public void setPosition(Location location) {
        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();
        mAltitude = location.getAltitude();
        // TODO: parse it from NMEA messages instead?
        mGeomagneticField = new GeomagneticField(
                (float) location.getLatitude(),
                (float) location.getLongitude(),
                (float) location.getAltitude(),
                CalibratedClock.currentTimeMillis());
    }

    /**
     * Returns the viewer altitude in meters over the WGS84 ellipsoid, <b>not</b> MSL.
     */
    public double getAltitude() {
        return mAltitude;
    }

    /**
     * Returns the viewer altitude in meters over MSL.
     */
    @MainThread
    public double getAltitudeMsl() {
        // Both values may be modified from the main thread only and now we are on the main thread
        // again => no locks are needed.
        return mAltitude - mGeoidHeight;
    }

    public void setGeoidHeight(double height) {
        mGeoidHeight = height;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public double getGeoidHeight() {
        return mGeoidHeight;
    }

    @Nullable
    public GeomagneticField getGeomagneticField() {
        return mGeomagneticField;
    }

    /**
     * Returns a list of all non-expired targets.
     *
     * <p>This function is supposed to be called on each frame, so to avoid expensive memory
     * allocations, pass the array returned by a previous call. If the array is big enough, it will
     * be reused. It is enough to iterate up to the first null element.</p>
     */
    public Target[] getTargets(Target[] array) {
        final long now = CalibratedClock.currentTimeMillis();
        synchronized (mTargetMap) {
            // Keep the lock, otherwise ConcurrentModificationException is thrown when addAircraft()
            // or addReceiver() are called in the meantime.
            final Collection<Target> values = mTargetMap.values();
            if (array.length < values.size()) {
                array = new Target[values.size()];
            }
            int i = 0;
            // Iterate using an iterator to avoid ConcurrentModificationException.
            for (Iterator<Target> it = values.iterator(); it.hasNext();) {
                final Target target = it.next();
                final long expireTime =
                        target instanceof Aircraft ? EXPIRE_TIME_AIRCRAFT : EXPIRE_TIME_RECEIVER;
                if (!mDemoMode && (now - target.getPositionTime()) > expireTime * 1000) {
                    it.remove();
                } else {
                    array[i] = target;
                    ++i;
                }
            }
            // Reset the rest of the array to avoid memory leaks. TODO: shrink the array?
            for (int j = i; j < array.length; ++j) {
                array[j] = null;
            }
        }
        return array;
    }

    public Aircraft addAircraft(String source, long id, double lat, double lon, double alt,
                                long timestamp) {
        Aircraft aircraft;
        synchronized (mTargetMap) {
            aircraft = (Aircraft) mTargetMap.get(source);
            if (aircraft == null) {
                aircraft = new Aircraft(source, id);
                mTargetMap.put(source, aircraft);
            }
        }
        aircraft.setPosition(lat, lon, alt, timestamp);
        return aircraft;
    }

    public Receiver addReceiver(String callSign, double lat, double lon, double alt,
                                long timestamp) {
        Receiver receiver;
        synchronized (mTargetMap) {
            // FIXME: what happens if someone names their receiver as an aircraft?
            receiver = (Receiver) mTargetMap.get(callSign);
            if (receiver == null) {
                receiver = new Receiver(callSign);
                mTargetMap.put(callSign, receiver);
            }
        }
        if (lat != 0 && lon != 0 && alt != 0) {
            receiver.setPosition(lat, lon, alt, timestamp);
        }
        return receiver;
    }

    public boolean isDemo() {
        return mDemoMode;
    }

    @MainThread
    public void setDemo(boolean isDemo) {
        mDemoMode = isDemo;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public boolean isLocationPredictionEnabled() {
        return mLocationPrediction;
    }

    @MainThread
    public void setLocationPredictionEnabled(boolean enabled) {
        mLocationPrediction = enabled;
    }

    /**
     * Get the target position at the given time in world (OpenGL) coordinates.
     */
    public void getTargetCoordinates(Target target, long time, float[] coordinates) {
        double longitudeDelta = target.getLongitude() - mLongitude;
        if (longitudeDelta < -180) {
            longitudeDelta += 360;
        } else if (longitudeDelta > 180) {
            longitudeDelta -= 360;
        }
        coordinates[0] =
                (float) (EARTH_RADIUS * Math.toRadians(longitudeDelta)
                        * Math.cos(Math.toRadians(mLatitude)));
        coordinates[1] = (float) (EARTH_RADIUS * Math.toRadians(target.getLatitude() - mLatitude));
        coordinates[2] = (float) (target.getAltitude() - (mAltitude - mGeoidHeight));
        coordinates[3] = 1;
        if (!mLocationPrediction || !(target instanceof Aircraft)) {
            return;
        }

        final Aircraft aircraft = (Aircraft) target;

        final double trackInDegrees = aircraft.getHeading();
        if (trackInDegrees == 0) {
            return; // No track => no prediction!
        }

        final double timeDeltaInSeconds = Math.max(0,
                Math.min((time - target.getPositionTime()) * 0.001, MAX_PREDICTION_TIME));
        final double trackInRadians = Math.toRadians(trackInDegrees);
        final double turnRateInDegreesPerSecond = aircraft.getTurnRate();
        final double groundSpeedInMetrePerSecond = aircraft.getGroundSpeed() / 3.6;

        if (Math.abs(turnRateInDegreesPerSecond) < 1.0) {
            coordinates[0] += (float) (groundSpeedInMetrePerSecond * Math.sin(trackInRadians)
                    * timeDeltaInSeconds);
            coordinates[1] += (float) (groundSpeedInMetrePerSecond * Math.cos(trackInRadians)
                    * timeDeltaInSeconds);
        } else {
            // FIXME: maybe just take the fact that it may be negative and simplify the code? try
            //  it after testing
            final double turnRadiusInMeters =
                    groundSpeedInMetrePerSecond / Math.toRadians(
                            Math.abs(turnRateInDegreesPerSecond));
            double newTrackInDegrees =
                    (trackInDegrees + turnRateInDegreesPerSecond * timeDeltaInSeconds) % 360;
            if (newTrackInDegrees < 0) {
                newTrackInDegrees += 360;
            }
            final double newTrackInRadians = Math.toRadians(newTrackInDegrees);
            if (turnRateInDegreesPerSecond > 0) {
                final double centerX = coordinates[0]
                        + Math.cos(trackInRadians) * turnRadiusInMeters;
                final double centerY = coordinates[1]
                        - Math.sin(trackInRadians) * turnRadiusInMeters;
                coordinates[0] =
                        (float) (centerX - Math.cos(newTrackInRadians) * turnRadiusInMeters);
                coordinates[1] =
                        (float) (centerY + Math.sin(newTrackInRadians) * turnRadiusInMeters);
            } else {
                final double centerX = coordinates[0]
                        - Math.cos(trackInRadians) * turnRadiusInMeters;
                final double centerY = coordinates[1]
                        + Math.sin(trackInRadians) * turnRadiusInMeters;
                coordinates[0] =
                        (float) (centerX + Math.cos(newTrackInRadians) * turnRadiusInMeters);
                coordinates[1] =
                        (float) (centerY - Math.sin(newTrackInRadians) * turnRadiusInMeters);
            }
        }
        coordinates[2] += (float) (aircraft.getClimbRate() * timeDeltaInSeconds);
    }

    public void clear() {
        synchronized (mTargetMap) {
            mTargetMap.clear();
        }
    }
}
