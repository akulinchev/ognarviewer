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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationManager;

import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.SensorEventBuilder;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLocationManager;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowSensor;
import org.robolectric.shadows.ShadowSensorManager;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import me.testcase.ognarviewer.CalibratedClock;
import me.testcase.ognarviewer.client.AircraftLocationMessage;
import me.testcase.ognarviewer.world.Aircraft;
import me.testcase.ognarviewer.world.Target;

@RunWith(RobolectricTestRunner.class)
public class HomeViewModelTest {
    private Application mApplication;
    private ShadowApplication mShadowApplication;
    private Sensor mMagneticFieldSensor;
    private ShadowSensorManager mShadowSensorManager;
    private ShadowLocationManager mShadowLocationManager;
    private SharedPreferences mSharedPreferences;
    private HomeViewModel mModel;

    @Before
    public void setUp() {
        ShadowLog.stream = System.out;

        mApplication = RuntimeEnvironment.getApplication();
        mShadowApplication = Shadows.shadowOf(mApplication);

        mMagneticFieldSensor = ShadowSensor.newInstance(Sensor.TYPE_MAGNETIC_FIELD);

        mShadowSensorManager = Shadows.shadowOf((SensorManager) mApplication.getSystemService(Context.SENSOR_SERVICE));
        mShadowSensorManager.addSensor(mMagneticFieldSensor);

        mShadowLocationManager = Shadows.shadowOf((LocationManager) mApplication.getSystemService(Context.LOCATION_SERVICE));

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mApplication);

        mModel = new HomeViewModel(mApplication);
    }

    @Test
    public void testOverlayMode() {
        // Initial state.
        Assert.assertNotNull(mModel.getOverlayMode().getValue());
        Assert.assertEquals(HomeViewModel.OVERLAY_MODE_WAITING_GPS, (long) mModel.getOverlayMode().getValue());
        Assert.assertNotNull(mModel.getCompassAccuracy().getValue());
        Assert.assertEquals(SensorManager.SENSOR_STATUS_NO_CONTACT, (int) mModel.getCompassAccuracy().getValue());

        // First fix is available.
        // In the past there used to be a "Low GPS accuracy" overlay here.
        final Location location = new Location("test");
        location.setAccuracy(11);
        location.setVerticalAccuracyMeters(11);
        mModel.onLocationChanged(location);
        Assert.assertEquals(HomeViewModel.OVERLAY_MODE_AUTO_COMPASS_CALIBRATION, (long) mModel.getOverlayMode().getValue());

        // First sensor event received.
        /*final SensorEvent sensorEvent = SensorEventBuilder.newBuilder()
                .setSensor(sensor)
                .setValues(new float[]{42})
                .setAccuracy(SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM)
                .build();
        shadowSensorManager.sendSensorEventToListeners(sensorEvent);*/
        // Robolectric has no API to trigger onAccuracyChanged() => need to call it manually.
        mModel.onAccuracyChanged(mMagneticFieldSensor, SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM);
        Assert.assertEquals(SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM, (int) mModel.getCompassAccuracy().getValue());
        Assert.assertEquals(HomeViewModel.OVERLAY_MODE_AUTO_COMPASS_CALIBRATION, (long) mModel.getOverlayMode().getValue());

        // Compass accuracy has increased.
        mModel.onAccuracyChanged(mMagneticFieldSensor, SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
        Assert.assertEquals(SensorManager.SENSOR_STATUS_ACCURACY_HIGH, (int) mModel.getCompassAccuracy().getValue());
        Assert.assertEquals(HomeViewModel.OVERLAY_MODE_NONE, (long) mModel.getOverlayMode().getValue());

        // "Adjust" button clicked.
        mModel.startManualCalibration();
        Assert.assertEquals(HomeViewModel.OVERLAY_MODE_MANUAL_COMPASS_CALIBRATION, (long) mModel.getOverlayMode().getValue());

        // Compass accuracy is bad again. But the manual calibration overlay should take precedence.
        mModel.onAccuracyChanged(mMagneticFieldSensor, SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM);
        Assert.assertEquals(HomeViewModel.OVERLAY_MODE_MANUAL_COMPASS_CALIBRATION, (long) mModel.getOverlayMode().getValue());

        // Manual calibration canceled. The overlay for auto calibration popups again.
        mModel.finishManualCalibration();
        Assert.assertEquals(HomeViewModel.OVERLAY_MODE_AUTO_COMPASS_CALIBRATION, (long) mModel.getOverlayMode().getValue());

        // The compass was auto-calibrated again.
        mModel.onAccuracyChanged(mMagneticFieldSensor, SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
        Assert.assertEquals(HomeViewModel.OVERLAY_MODE_NONE, (long) mModel.getOverlayMode().getValue());
    }

    @Test
    public void testToolbarSubtitle() {
        // By default, there is no subtitle.
        Assert.assertNull(mModel.getToolbarSubtitle().getValue());

        // First fix is available.
        final Location location = new Location("test");
        location.setAccuracy(11);
        location.setVerticalAccuracyMeters(11);
        mModel.onLocationChanged(location);
        Assert.assertEquals("Low GPS accuracy: 11 m / 11 m", mModel.getToolbarSubtitle().getValue());

        // Accuracy increased.
        location.setAccuracy(10);
        location.setVerticalAccuracyMeters(10);
        mModel.onLocationChanged(location);
        Assert.assertNull(mModel.getToolbarSubtitle().getValue());

        // Accuracy decreased again.
        location.setAccuracy(12);
        location.setVerticalAccuracyMeters(12);
        mModel.onLocationChanged(location);
        Assert.assertEquals("Low GPS accuracy: 12 m / 12 m", mModel.getToolbarSubtitle().getValue());

        // Demo mode was activated. It should override the bad GPS problem.
        mSharedPreferences.edit().putBoolean("demo_mode", true).commit();
        Assert.assertEquals("Demo mode active", mModel.getToolbarSubtitle().getValue());

        // Demo mode was deactivated.
        mSharedPreferences.edit().putBoolean("demo_mode", false).commit();
        Assert.assertEquals("Low GPS accuracy: 12 m / 12 m", mModel.getToolbarSubtitle().getValue());

        // Accuracy increased again.
        location.setAccuracy(10);
        location.setVerticalAccuracyMeters(10);
        mModel.onLocationChanged(location);
        Assert.assertNull(mModel.getToolbarSubtitle().getValue());

        // Try good a horizontal and a bad vertical accuracies.
        // This happens on some devices and users start giving me 1-star reviews.
        location.setAccuracy(10);
        location.setVerticalAccuracyMeters(100);
        mModel.onLocationChanged(location);
        Assert.assertEquals("Low GPS accuracy: 10 m / 100 m", mModel.getToolbarSubtitle().getValue());

        // Some phones do not report the vertical accuracy at all.
        location.setAccuracy(11);
        location.removeVerticalAccuracy();
        mModel.onLocationChanged(location);
        Assert.assertEquals("Low GPS accuracy: 11 m / -1 m", mModel.getToolbarSubtitle().getValue());
    }

    @Test
    public void testSensorListener() {
        Assert.assertFalse(mShadowSensorManager.hasListener(mModel, mMagneticFieldSensor));
        mModel.registerSensorListener();
        Assert.assertTrue(mShadowSensorManager.hasListener(mModel, mMagneticFieldSensor));
        mModel.unregisterSensorListener();
        Assert.assertFalse(mShadowSensorManager.hasListener(mModel, mMagneticFieldSensor));
    }

    @Test
    public void testNmeaMessages() {
        Assert.assertEquals(0, mModel.getWorld().getGeoidHeight(), 0.0001);

        // Subscribe for NMEA messages.
        mShadowApplication.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);
        mModel.requestLocationUpdates();

        // Something I really observe on my Pixel 4a.
        // GP... is GPS, GL... is GLONASS, GA... is Galileo, GN... is a fusion of all above.
        mShadowLocationManager.simulateNmeaMessage("$GPGSA,A,1,,,,,,,,,,,,,,,,*32", 1723800079001L);
        mShadowLocationManager.simulateNmeaMessage("$GPVTG,,T,,M,,N,,K,N*2C", 1723800079001L);
        mShadowLocationManager.simulateNmeaMessage("$GPDTM,,,,,,,,*4A", 1723800079001L);
        mShadowLocationManager.simulateNmeaMessage("$GPRMC,,V,,,,,,,,,,N,V*29", 1723800079001L);
        mShadowLocationManager.simulateNmeaMessage("$GPGNS,,,,,,N,,,,,,,V*79", 1723800079001L);
        mShadowLocationManager.simulateNmeaMessage("$GPGGA,,,,,,0,,,,,,,,*66", 1723800079001L);
        mShadowLocationManager.simulateNmeaMessage("$GPGSV,3,1,12,02,85,074,,03,51,244,,04,08,187,,08,19,170,,1*68", 1723800079559L);
        mShadowLocationManager.simulateNmeaMessage("$GLGSV,3,1,09,74,10,333,,66,45,040,,82,82,045,,73,04,293,,1*7E", 1723800079559L);
        mShadowLocationManager.simulateNmeaMessage("$GAGSV,3,1,09,03,50,092,,05,19,039,,08,34,164,,13,57,300,,7*7D", 1723800079559L);
        Robolectric.flushForegroundThreadScheduler();
        Assert.assertEquals(0, mModel.getWorld().getGeoidHeight(), 0.0001);

        // Try a valid message containing the geoid height.
        mShadowLocationManager.simulateNmeaMessage("$GNGGA,104500.00,4900.000000,N,00700.000000,E,1,07,2.7,257.9,M,48.6,M,,*79", 1723805089057L);
        Robolectric.flushForegroundThreadScheduler();
        Assert.assertEquals(48.6, mModel.getWorld().getGeoidHeight(), 0.0001);

        // Unsubscribe from NMEA messages.
        mModel.stopLocationUpdates();

        // Make sure the NMEA listener was unsubscribed.
        mShadowLocationManager.simulateNmeaMessage("$GNGGA,104500.00,4900.000000,N,00700.000000,E,1,07,2.7,257.9,M,12.3,M,,*79", 1723805089057L);
        Robolectric.flushForegroundThreadScheduler();
        Assert.assertEquals(48.6, mModel.getWorld().getGeoidHeight(), 0.0001);
    }

    @Test
    public void testGpsEnabled() {
        // Allow GPS access.
        mShadowApplication.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);

        // Initial state: we don't know yet whether GPS is enabled or not.
        Assert.assertNull(mModel.getGpsEnabled().getValue());

        // On Robolectric, GPS is enabled by default.
        mModel.requestLocationUpdates();
        Assert.assertNotNull(mModel.getGpsEnabled().getValue());
        Assert.assertTrue(mModel.getGpsEnabled().getValue());

        // Disable GPS.
        mShadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, false);
        Robolectric.flushForegroundThreadScheduler();
        Assert.assertFalse(mModel.getGpsEnabled().getValue());

        // Enable GPS again.
        mShadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, true);
        Robolectric.flushForegroundThreadScheduler();
        Assert.assertTrue(mModel.getGpsEnabled().getValue());

        // After the LocationListener has been removed, the model still believes GPS was enabled.
        mModel.stopLocationUpdates();
        Robolectric.flushForegroundThreadScheduler();
        Assert.assertTrue(mModel.getGpsEnabled().getValue());

        // Try to really disable GPS to make sure stopLocationUpdates() worked.
        mShadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, false);
        Robolectric.flushForegroundThreadScheduler();
        Assert.assertTrue(mModel.getGpsEnabled().getValue());
    }

    @Test
    public void testLocationAccuracy() {
        // Initial state: location accuracy is unknown.
        Assert.assertEquals(-1, mModel.getHorizontalLocationAccuracy(), 0.0001);
        Assert.assertEquals(-1, mModel.getVerticalLocationAccuracy(), 0.0001);

        // Subscribe for location updates.
        mShadowApplication.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);
        mModel.requestLocationUpdates();

        // First fix.
        final Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setLatitude(49);
        location.setLongitude(7);
        location.setAccuracy(13);
        location.setVerticalAccuracyMeters(10);
        mShadowLocationManager.simulateLocation(location);
        Robolectric.flushForegroundThreadScheduler();
        Assert.assertEquals(13, mModel.getHorizontalLocationAccuracy(), 0.0001);
        Assert.assertEquals(10, mModel.getVerticalLocationAccuracy(), 0.0001);

        // Second fix in 2 seconds and at another position.
        location.setElapsedRealtimeNanos(location.getElapsedRealtimeNanos() + 2L * 1000 * 1000 * 1000);
        location.setLatitude(49.1);
        location.setLongitude(7.1);
        location.setAccuracy(130);
        location.setVerticalAccuracyMeters(100);
        mShadowLocationManager.simulateLocation(location);
        Robolectric.flushForegroundThreadScheduler();
        Assert.assertEquals(130, mModel.getHorizontalLocationAccuracy(), 0.0001);
        Assert.assertEquals(100, mModel.getVerticalLocationAccuracy(), 0.0001);

        // Third fix in 3 seconds. This time, the accuracy is not set => should be -1.
        location.setElapsedRealtimeNanos(location.getElapsedRealtimeNanos() + 3L * 1000 * 1000 * 1000);
        location.setLatitude(49);
        location.setLongitude(7);
        location.removeAccuracy();
        location.removeVerticalAccuracy();
        mShadowLocationManager.simulateLocation(location);
        Robolectric.flushForegroundThreadScheduler();
        Assert.assertEquals(-1, mModel.getHorizontalLocationAccuracy(), 0.0001);
        Assert.assertEquals(-1, mModel.getVerticalLocationAccuracy(), 0.0001);

        // Unsubscribe from location updates.
        mModel.stopLocationUpdates();

        // Fourth fix in 4 seconds and at another position.
        // Make sure the listener has really unsubscribed.
        location.setElapsedRealtimeNanos(location.getElapsedRealtimeNanos() + 4L * 1000 * 1000 * 1000);
        location.setLatitude(49.1);
        location.setLongitude(7.1);
        location.setAccuracy(13);
        location.setVerticalAccuracyMeters(10);
        mShadowLocationManager.simulateLocation(location);
        Robolectric.flushForegroundThreadScheduler();
        Assert.assertEquals(-1, mModel.getHorizontalLocationAccuracy(), 0.0001);
        Assert.assertEquals(-1, mModel.getVerticalLocationAccuracy(), 0.0001);
    }

    @Test
    public void testSatelliteCount() {
        // Initial state: satellite count is unknown.
        Assert.assertNull(mModel.getSatelliteCount().getValue());

        // Subscribe for location updates.
        mShadowApplication.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);
        mModel.requestLocationUpdates();

        // Simulate for GNSS statuses.
        final GnssStatus status1 = new GnssStatus.Builder()
                .addSatellite(GnssStatus.CONSTELLATION_GPS, 1, 0, 0, 0, false, false, true, false, 0, false, 0)
                .addSatellite(GnssStatus.CONSTELLATION_GPS, 1, 0, 0, 0, false, false, false, false, 0, false, 0)
                .build();
        mShadowLocationManager.simulateGnssStatus(status1);
        Robolectric.flushForegroundThreadScheduler();
        Assert.assertNotNull(mModel.getSatelliteCount().getValue());
        Assert.assertEquals(2, mModel.getSatelliteCount().getValue().total);
        Assert.assertEquals(1, mModel.getSatelliteCount().getValue().used);

        // Unsubscribe from GNSS statuses.
        mModel.stopLocationUpdates();

        // Make sure we have really unsubscribed.
        final GnssStatus status2 = new GnssStatus.Builder()
                .addSatellite(GnssStatus.CONSTELLATION_GPS, 1, 0, 0, 0, false, false, true, false, 0, false, 0)
                .addSatellite(GnssStatus.CONSTELLATION_GPS, 1, 0, 0, 0, false, false, true, false, 0, false, 0)
                .build();
        mShadowLocationManager.simulateGnssStatus(status2);
        Robolectric.flushForegroundThreadScheduler();
        Assert.assertEquals(2, mModel.getSatelliteCount().getValue().total);
        Assert.assertEquals(1, mModel.getSatelliteCount().getValue().used);
    }

    @Test
    public void testSelectedTarget() {
        final TargetObserver observer = new TargetObserver();
        mModel.getSelectedTarget().observeForever(observer);
        Assert.assertNull(mModel.getSelectedTarget().getValue());
        Assert.assertEquals(0, observer.targets.size());
        mModel.selectTarget(null);
        Assert.assertNull(mModel.getSelectedTarget().getValue());
        Assert.assertEquals(1, observer.targets.size());

        final Aircraft aircraft = mModel.getWorld().addAircraft("FLR3EE227", 0x063EE227, 49.1, 7.1, 1350, CalibratedClock.currentTimeMillis());
        aircraft.setGroundSpeed(90);
        aircraft.setClimbRate(0);
        aircraft.setHeading(180);

        mModel.selectTarget(aircraft);
        Assert.assertNotNull(mModel.getSelectedTarget().getValue());
        Assert.assertEquals(aircraft, mModel.getSelectedTarget().getValue());
        Assert.assertEquals(2, observer.targets.size());

        // FIXME: make a mock Client instead, don't call onAprsMessage() directly.
        final AircraftLocationMessage message = new AircraftLocationMessage();
        message.callSign = "FLR3EE227";
        mModel.onAprsMessage(message);

        // onAprsMessage() should emit the same value again, otherwise the bottom sheet will not be updated.
        Assert.assertEquals(aircraft, mModel.getSelectedTarget().getValue());
        Assert.assertEquals(3, observer.targets.size());
        mModel.onAprsMessage(message);
        Assert.assertEquals(aircraft, mModel.getSelectedTarget().getValue());
        Assert.assertEquals(4, observer.targets.size());
        mModel.onAprsMessage(message);
        Assert.assertEquals(aircraft, mModel.getSelectedTarget().getValue());
        Assert.assertEquals(5, observer.targets.size());
        // If it was another aircraft, do not update.
        message.callSign = "FLR3D238E";
        mModel.onAprsMessage(message);
        Assert.assertEquals(aircraft, mModel.getSelectedTarget().getValue());
        Assert.assertEquals(5, observer.targets.size());

        mModel.getSelectedTarget().removeObserver(observer);
    }

    @Test
    public void testShowReconnectDialog() {
        // Initial state: unknown.
        Assert.assertNull(mModel.getShowReconnectDialog().getValue());

        // Simulate the first GPS fix, which triggers a connect.
        mShadowApplication.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);
        mModel.requestLocationUpdates();
        final Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setAccuracy(10);
        location.setVerticalAccuracyMeters(10);
        mShadowLocationManager.simulateLocation(location);
        Robolectric.flushForegroundThreadScheduler();
        Assert.assertNotNull(mModel.getShowReconnectDialog().getValue());
        Assert.assertFalse(mModel.getShowReconnectDialog().getValue());

        // FIXME: make a mock Client instead, don't call onAprsClientError() directly.
        mModel.onAprsClientError(new SocketException("Software caused connection abort"));
        Assert.assertTrue(mModel.getShowReconnectDialog().getValue());

        // The "Reconnect" button was pressed.
        mModel.reconnect();
        Assert.assertFalse(mModel.getShowReconnectDialog().getValue());

        // Turn off the Internet again.
        mModel.onAprsClientError(new SocketException("Software caused connection abort"));
        Assert.assertTrue(mModel.getShowReconnectDialog().getValue());

        // A new GPS fix in 2 s with another location is available => causes a reconnect too.
        // FIXME: but should it?
        location.setElapsedRealtimeNanos(location.getElapsedRealtimeNanos() + 2L * 1000 * 1000 * 1000);
        location.setLatitude(49);
        location.setLongitude(7);
        mShadowLocationManager.simulateLocation(location);
        Robolectric.flushForegroundThreadScheduler();
        Assert.assertFalse(mModel.getShowReconnectDialog().getValue());

        mModel.stopLocationUpdates();
    }

    @Test
    public void testSettings() {
        Assert.assertFalse(mModel.isDemoMode());
        Assert.assertFalse(mModel.getWorld().isDemo());
        mSharedPreferences.edit().putBoolean("demo_mode", true).commit();
        Assert.assertTrue(mModel.getWorld().isDemo());
        Assert.assertTrue(mModel.isDemoMode());
        mSharedPreferences.edit().putBoolean("demo_mode", false).commit();
        Assert.assertFalse(mModel.getWorld().isDemo());
        Assert.assertFalse(mModel.isDemoMode());

        Assert.assertTrue(mModel.getWorld().isLocationPredictionEnabled());
        mSharedPreferences.edit().putBoolean("linear_interpolation", false).commit();
        Assert.assertFalse(mModel.getWorld().isLocationPredictionEnabled());
        mSharedPreferences.edit().putBoolean("linear_interpolation", true).commit();
        Assert.assertTrue(mModel.getWorld().isLocationPredictionEnabled());

        mSharedPreferences.edit()
                .putBoolean("demo_mode", true)
                .putBoolean("linear_interpolation", false)
                .commit();
        Assert.assertTrue(mModel.getWorld().isDemo());
        Assert.assertTrue(mModel.isDemoMode());
        Assert.assertFalse(mModel.getWorld().isLocationPredictionEnabled());
        mSharedPreferences.edit().clear().commit();
        Assert.assertFalse(mModel.getWorld().isDemo());
        Assert.assertFalse(mModel.isDemoMode());
        Assert.assertTrue(mModel.getWorld().isLocationPredictionEnabled());

        // TODO: test max_distance
    }

    private static final class TargetObserver implements Observer<Target> {
        public List<Target> targets = new ArrayList<>();

        @Override
        public void onChanged(Target target) {
            targets.add(target);
        }
    }
}
