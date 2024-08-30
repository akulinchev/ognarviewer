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

package me.testcase.ognarviewer.ui.home;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import me.testcase.ognarviewer.CalibratedClock;
import me.testcase.ognarviewer.client.AircraftLocationMessage;
import me.testcase.ognarviewer.client.AprsMessage;
import me.testcase.ognarviewer.client.Client;
import me.testcase.ognarviewer.client.ReceiverLocationMessage;
import me.testcase.ognarviewer.client.ReceiverStatusMessage;
import me.testcase.ognarviewer.utils.LocationObfuscator;
import me.testcase.ognarviewer.world.Aircraft;
import me.testcase.ognarviewer.world.Receiver;
import me.testcase.ognarviewer.world.Target;
import me.testcase.ognarviewer.world.World;

/**
 * State and business logic of the main ("Home") screen.
 *
 * <p>An instance of this class should outlive configuration changes (e.g. screen rotation).</p>
 */
public final class HomeViewModel extends AndroidViewModel implements SensorEventListener,
        LocationListener, OnNmeaMessageListener,
        SharedPreferences.OnSharedPreferenceChangeListener, Client.MessageListener {
    private static final String TAG = "HomeViewModel";

    public static final int OVERLAY_MODE_NONE = 0;
    public static final int OVERLAY_MODE_WAITING_GPS = 1;
    public static final int OVERLAY_MODE_LOW_ACCURACY = 2;
    public static final int OVERLAY_MODE_AUTO_COMPASS_CALIBRATION = 3;
    public static final int OVERLAY_MODE_MANUAL_COMPASS_CALIBRATION = 4;

    private static final int MINIMUM_ACCURACY = 10;

    private final World mWorld = new World();
    private final Client mClient = new Client();

    /**
     * The last location we have reported to the OGN or null if disconnected.
     *
     * <p>TODO: move this to the Client class?</p>
     */
    private Location mOgnLocation;

    private final SharedPreferences mSharedPreferences;

    private final MutableLiveData<Target> mSelectedTarget = new MutableLiveData<>();

    private boolean mCompassOk;
    private boolean mGpsFixAvailable;
    private boolean mGoodGpsAccuracy;
    private boolean mManuallyCalibrating;
    private boolean mSkipAutoCalibration;
    private final MutableLiveData<Integer> mOverlayMode = new MutableLiveData<>();

    private final LocationManager mLocationManager;
    private final MutableLiveData<Boolean> mGpsEnabled = new MutableLiveData<>();
    private final MutableLiveData<LocationAccuracy> mLocationAccuracy = new MutableLiveData<>();
    private final MutableLiveData<SatelliteCount> mSatelliteCount = new MutableLiveData<>();

    private final SensorManager mSensorManager;
    private final Sensor mMagneticFieldSensor; // Just to track the accuracy.
    private final MutableLiveData<Integer> mCompassAccuracy =
            new MutableLiveData<>(SensorManager.SENSOR_STATUS_NO_CONTACT);

    private final MutableLiveData<Boolean> mShowReconnectDialog = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mDemoMode = new MutableLiveData<>();

    private final GnssStatus.Callback mGnssStatusCallback = new GnssStatus.Callback() {
        @Override
        public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
            int used = 0;
            for (int i = 0; i < status.getSatelliteCount(); ++i) {
                if (status.usedInFix(i)) {
                    ++used;
                }
            }
            final SatelliteCount count = new SatelliteCount();
            count.total = status.getSatelliteCount();
            count.used = used;
            mSatelliteCount.setValue(count);
        }
    };

    public HomeViewModel(Application application) {
        super(application);
        Log.v(TAG, String.format("HomeViewModel %h created", this));

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(application);
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        mWorld.setLocationPredictionEnabled(mSharedPreferences.getBoolean("linear_interpolation",
                true));
        setDemoMode(mSharedPreferences.getBoolean("demo_mode", false));
        mClient.setHostname(mSharedPreferences.getString("aprs_server", Client.DEFAULT_HOST));

        mSensorManager = (SensorManager) application.getSystemService(Context.SENSOR_SERVICE);
        mMagneticFieldSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mLocationManager = (LocationManager) application.getSystemService(Context.LOCATION_SERVICE);

        updateOverlayMode();
    }

    @Override
    protected void onCleared() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    public World getWorld() {
        return mWorld;
    }

    public LiveData<Integer> getOverlayMode() {
        return mOverlayMode;
    }

    private void updateOverlayMode() {
        final int desiredMode;
        final Boolean demoMode = mDemoMode.getValue();
        if (mManuallyCalibrating) {
            // Manual calibration overlay has the highest priority because it was intentionally
            // requested by the user.
            desiredMode = OVERLAY_MODE_MANUAL_COMPASS_CALIBRATION;
        } else if (!mGpsFixAvailable && demoMode != null && !demoMode) {
            // Prefer GPS complaints over compass complaints: it is possible that the compass will
            // be calibrated in the background while the user is waiting for a GPS fix.
            desiredMode = OVERLAY_MODE_WAITING_GPS;
        } else if (!mGoodGpsAccuracy && demoMode != null && !demoMode) {
            desiredMode = OVERLAY_MODE_LOW_ACCURACY;
        } else if (!mCompassOk && !mSkipAutoCalibration) {
            // If everything else is OK, well, need to complain about the compass...
            desiredMode = OVERLAY_MODE_AUTO_COMPASS_CALIBRATION;
        } else {
            // If everything is OK, no overlay should be shown.
            desiredMode = OVERLAY_MODE_NONE;
        }
        final Integer currentMode = mOverlayMode.getValue();
        if (currentMode == null || currentMode != desiredMode) {
            mOverlayMode.setValue(desiredMode);
            if (desiredMode == OVERLAY_MODE_NONE && mSelectedTarget.getValue() != null) {
                mSelectedTarget.setValue(mSelectedTarget.getValue());
            }
        }
    }

    public LiveData<Target> getSelectedTarget() {
        return mSelectedTarget;
    }

    public void selectTarget(Target target) {
        mSelectedTarget.setValue(target);
    }

    public LiveData<Integer> getCompassAccuracy() {
        return mCompassAccuracy;
    }

    public void registerSensorListener() {
        Log.v(TAG, "registerSensorListeners()");
        mSensorManager.registerListener(this, mMagneticFieldSensor, 1000 * 1000);
    }

    public void unregisterSensorListener() {
        Log.v(TAG, "unregisterSensorListener()");
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Unused.
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensor == mMagneticFieldSensor) {
            mCompassAccuracy.setValue(accuracy);
            mCompassOk = accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH;
            updateOverlayMode();
        }
    }

    public void startManualCalibration() {
        mManuallyCalibrating = true;
        updateOverlayMode();
    }

    public void finishManualCalibration() {
        mManuallyCalibrating = false;
        updateOverlayMode();
    }

    public void skipAutoCalibration() {
        mSkipAutoCalibration = true;
        updateOverlayMode();
    }

    public LiveData<LocationAccuracy> getLocationAccuracy() {
        return mLocationAccuracy;
    }

    public LiveData<SatelliteCount> getSatelliteCount() {
        return mSatelliteCount;
    }

    public LiveData<Boolean> getGpsEnabled() {
        return mGpsEnabled;
    }

    public void requestLocationUpdates() {
        Log.v(TAG, "requestLocationUpdates()");
        if (getApplication().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_DENIED) {
            Log.w(TAG, "requestLocationUpdates(): access denied");
            return;
        }
        if (mWorld.isDemo()) {
            Log.w(TAG, "requestLocationUpdates(): demo mode active");
            return;
        }
        mGpsFixAvailable = false;
        mSatelliteCount.setValue(null); // Hide the number of satellites, it isn't known yet.
        mLocationAccuracy.setValue(null); // Hide the accuracy, it isn't known yet.
        mLocationManager.registerGnssStatusCallback(mGnssStatusCallback);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
        mLocationManager.addNmeaListener(this, null);
        mGpsEnabled.setValue(mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
        updateOverlayMode();
    }

    public void stopLocationUpdates() {
        Log.v(TAG, "stopLocationUpdates()");
        mLocationManager.removeUpdates(this);
        mLocationManager.unregisterGnssStatusCallback(mGnssStatusCallback);
        mLocationManager.removeNmeaListener(this);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.w(TAG, String.format("Location changed (horizontal accuracy %f m)",
                location.getAccuracy()));
        mGpsFixAvailable = true;
        mWorld.setPosition(location);
        CalibratedClock.sync(location);
        final LocationAccuracy accuracy = new LocationAccuracy();
        accuracy.horizontal = location.hasAccuracy() ? location.getAccuracy() : -1;
        accuracy.vertical = location.hasVerticalAccuracy()
                ? location.getVerticalAccuracyMeters() : -1;
        mLocationAccuracy.setValue(accuracy);
        if ((location.hasAccuracy()
                && location.getAccuracy() <= MINIMUM_ACCURACY
                || !location.hasAccuracy())
                && (location.hasVerticalAccuracy()
                && location.getVerticalAccuracyMeters() <= MINIMUM_ACCURACY
                || !location.hasVerticalAccuracy())) {
            mGoodGpsAccuracy = true;
            if (mOgnLocation == null || mOgnLocation.distanceTo(location) > 5000) {
                mClient.disconnect();
                mOgnLocation = LocationObfuscator.obfuscate(location);
                Log.v(TAG, "onLocationChanged(): calling reconnect()");
                reconnect();
            } else {
                Log.v(TAG, "onLocationChanged(): not calling reconnect()");
            }
        } else {
            mGoodGpsAccuracy = false;
            // FIXME: don't connect/disconnect when the accuracy jumps around MINIMUM_ACCURACY!
            Log.v(TAG, "onLocationChanged(): calling mClient.disconnect(), because accuracy <= "
                    + MINIMUM_ACCURACY);
            mClient.disconnect();
            mOgnLocation = null;
        }
        updateOverlayMode();
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        Log.v(TAG, String.format("Provider '%s' enabled", provider));
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            mGpsFixAvailable = false;
            mGpsEnabled.setValue(true);
            updateOverlayMode();
        }
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        Log.v(TAG, String.format("Provider '%s' disabled", provider));
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            mGpsFixAvailable = false;
            mClient.disconnect();
            mGpsEnabled.setValue(false);
            updateOverlayMode();
        }
    }

    @Override
    public void onNmeaMessage(String message, long timestamp) {
        final String[] values = message.split(",");
        if (values.length != 15 || values[11].isEmpty()) {
            return;
        }
        final String header = values[0];
        if (!header.endsWith("GGA")) {
            return;
        }
        try {
            mWorld.setGeoidHeight(Double.parseDouble(values[11]));
        } catch (NumberFormatException e) {
            // Do nothing.
        }
    }

    public LiveData<Boolean> getDemoMode() {
        return mDemoMode;
    }

    private void setDemoMode(boolean active) {
        mDemoMode.setValue(active);

        if (active) {
            mWorld.clear();
            mWorld.setDemo(true);

            final Location location = new Location("demo");
            location.setLatitude(49);
            location.setLongitude(7);
            location.setAltitude(350);
            mWorld.setPosition(location);

            final Aircraft aircraft1 = mWorld.addAircraft("FLR3EE227", 0x063EE227, 49.1, 7.1, 1350,
                    CalibratedClock.currentTimeMillis());
            aircraft1.setGroundSpeed(90);
            aircraft1.setClimbRate(0);
            aircraft1.setHeading(180);

            final Aircraft aircraft2 = mWorld.addAircraft("FLR3D238E", 0x0A3D238E, 48.9, 6.9, 4350,
                    CalibratedClock.currentTimeMillis());
            aircraft2.setGroundSpeed(200);
            aircraft2.setClimbRate(2);
            aircraft2.setHeading(1);

            final Aircraft aircraft3 = mWorld.addAircraft("FLR3FEF7C", 0x0A3FEF7C, 48.9, 6.9, 350,
                    CalibratedClock.currentTimeMillis());
            aircraft3.setGroundSpeed(0);

            final Receiver receiver = mWorld.addReceiver(
                    "TEST", 49.1, 7.1, 350, CalibratedClock.currentTimeMillis());
            receiver.setFreeRam(128);
            receiver.setTotalRam(1024);
            receiver.setCpuLoad(0.7f);
            receiver.setVersion("v1.2.3.DEMO");
            receiver.setCpuTemperature(72);
            receiver.setNtpOffset(0.5);
        } else if (mWorld.isDemo()) {
            mWorld.clear();
            mWorld.setDemo(false);
        }

        // When the demo mode is active, GPS overlays are impossible.
        updateOverlayMode();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          @Nullable String key) {
        if (key == null) {
            setDemoMode(false);
            mWorld.setLocationPredictionEnabled(true);
            mClient.setHostname(Client.DEFAULT_HOST);
        } else if (key.equals("linear_interpolation")) {
            mWorld.setLocationPredictionEnabled(mSharedPreferences.getBoolean(key, true));
        } else if (key.equals("demo_mode")) {
            setDemoMode(mSharedPreferences.getBoolean(key, false));
        } else if (key.equals("aprs_server")) {
            mClient.setHostname(mSharedPreferences.getString(key, Client.DEFAULT_HOST));
        }
    }

    public void disconnect() {
        mClient.disconnect();
        mOgnLocation = null;
    }

    @Override
    public void onAprsMessage(AprsMessage message) {
        if (message instanceof AircraftLocationMessage) {
            final AircraftLocationMessage ognMessage = (AircraftLocationMessage) message;
            final Aircraft aircraft = mWorld.addAircraft(message.callSign, ognMessage.id,
                    ognMessage.latitude, ognMessage.longitude, ognMessage.altitude,
                    ognMessage.timestamp);
            aircraft.setGroundSpeed(ognMessage.groundSpeed);
            aircraft.setClimbRate((float) ognMessage.climbRate);
            aircraft.setHeading(ognMessage.heading);
            aircraft.setTurnRate(ognMessage.turnRate);
            final Target selectedTarget = mSelectedTarget.getValue();
            if (selectedTarget != null && selectedTarget == aircraft) {
                mSelectedTarget.setValue(selectedTarget);
            }
        } else if (message instanceof ReceiverLocationMessage) {
            final ReceiverLocationMessage locationMessage = (ReceiverLocationMessage) message;
            mWorld.addReceiver(message.callSign, locationMessage.latitude,
                    locationMessage.longitude, locationMessage.altitude, locationMessage.timestamp);
        } else if (message instanceof ReceiverStatusMessage) {
            final ReceiverStatusMessage statusMessage = (ReceiverStatusMessage) message;
            // FIXME: add a method not taking the location.
            final Receiver receiver = mWorld.addReceiver(message.callSign, 0, 0, 0,
                    statusMessage.timestamp);
            receiver.setVersion(statusMessage.version);
            receiver.setNtpOffset(statusMessage.ntpOffset);
            receiver.setFreeRam((float) statusMessage.freeRam);
            receiver.setTotalRam((float) statusMessage.totalRam);
            receiver.setCpuTemperature((float) statusMessage.cpuTemperature);
            receiver.setCpuLoad(statusMessage.cpuLoad);
            final Target selectedTarget = mSelectedTarget.getValue();
            if (selectedTarget != null && selectedTarget == receiver) {
                mSelectedTarget.setValue(selectedTarget);
            }
        }
    }

    public LiveData<Boolean> getShowReconnectDialog() {
        return mShowReconnectDialog;
    }

    @Override
    public void onAprsClientError(Exception e) {
        Log.e(TAG, "Exception in client thread: " + e.getClass().getName() + ": " + e.getMessage());
        mShowReconnectDialog.setValue(true);
    }

    public void reconnect() {
        if (mOgnLocation == null) {
            return;
        }
        final int maxDistance = mSharedPreferences.getInt("max_distance",
                WorldRenderer.DEFAULT_DISTANCE);
        mClient.connect(mOgnLocation, maxDistance + LocationObfuscator.COARSE_ACCURACY_KM, this,
                null);
        mShowReconnectDialog.setValue(false);
    }

    @Override
    public void onInvalidAprsMessage(String message) {
        Log.w(TAG, "Got invalid APRS message: " + message);
    }

    @Override
    public void onAprsDisconnected() {
        Log.e(TAG, "Client disconnected");
    }

    public static final class LocationAccuracy {
        public double horizontal;
        public double vertical;
    }

    public static final class SatelliteCount {
        public int total;
        public int used;
    }
}
