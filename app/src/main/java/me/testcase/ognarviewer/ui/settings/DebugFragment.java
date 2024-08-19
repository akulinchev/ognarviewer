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

package me.testcase.ognarviewer.ui.settings;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.util.Size;
import android.util.SizeF;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import java.util.Set;

import me.testcase.ognarviewer.MainActivity;
import me.testcase.ognarviewer.R;

public class DebugFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        final Context context = getPreferenceManager().getContext();
        final PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);

        Preference preference = new Preference(context);
        preference.setPersistent(false);
        preference.setTitle("API level");
        preference.setSummary(String.valueOf(Build.VERSION.SDK_INT));
        preference.setIconSpaceReserved(false);
        screen.addPreference(preference);

        preference = new Preference(context);
        preference.setPersistent(false);
        preference.setIconSpaceReserved(false);
        preference.setTitle("Display orientation");
        final WindowManager windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        switch (windowManager.getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_0:
                preference.setSummary("0 degrees");
                break;
            case Surface.ROTATION_90:
                preference.setSummary("90 degrees");
                break;
            case Surface.ROTATION_180:
                preference.setSummary("180 degrees");
                break;
            case Surface.ROTATION_270:
                preference.setSummary("270 degrees");
                break;
        }
        screen.addPreference(preference);

        final CameraManager cameraManager =
                (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                final CameraCharacteristics characteristics =
                        cameraManager.getCameraCharacteristics(cameraId);

                final PreferenceCategory category = new PreferenceCategory(context);
                category.setTitle("Camera " + cameraId);
                category.setIconSpaceReserved(false);
                screen.addPreference(category);

                final Integer supportLevel =
                        characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                if (supportLevel != null) {
                    preference = new Preference(context);
                    preference.setPersistent(false);
                    preference.setIconSpaceReserved(false);
                    preference.setTitle("Support level");
                    switch (supportLevel) {
                        case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED:
                            preference.setSummary("Limited");
                            break;
                        case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL:
                            preference.setSummary("Full");
                            break;
                        case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY:
                            preference.setSummary("Legacy");
                            break;
                        case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3:
                            preference.setSummary("Level 3");
                            break;
                        case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL:
                            preference.setSummary("External");
                            break;
                    }
                    category.addPreference(preference);
                }

                final Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (lensFacing != null) {
                    preference = new Preference(context);
                    preference.setPersistent(false);
                    preference.setIconSpaceReserved(false);
                    preference.setTitle("Lens facing");
                    switch (lensFacing) {
                        case CameraCharacteristics.LENS_FACING_FRONT:
                            preference.setSummary("Front");
                            break;
                        case CameraCharacteristics.LENS_FACING_BACK:
                            preference.setSummary("Back");
                            break;
                        case CameraCharacteristics.LENS_FACING_EXTERNAL:
                            preference.setSummary("External");
                            break;
                    }
                    category.addPreference(preference);
                }

                final Integer sensorOrientation =
                        characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                if (sensorOrientation != null) {
                    preference = new Preference(context);
                    preference.setPersistent(false);
                    preference.setIconSpaceReserved(false);
                    preference.setTitle("Sensor orientation");
                    preference.setSummary(sensorOrientation + " degrees");
                    category.addPreference(preference);
                }

                final SizeF sensorSize =
                        characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                if (sensorSize != null) {
                    preference = new Preference(context);
                    preference.setPersistent(false);
                    preference.setIconSpaceReserved(false);
                    preference.setTitle("Physical sensor size");
                    preference.setSummary(sensorSize + " mm");
                    category.addPreference(preference);
                }

                final float[] focalLengths = characteristics.get(
                        CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                if (focalLengths != null) {
                    preference = new Preference(context);
                    preference.setPersistent(false);
                    preference.setIconSpaceReserved(false);
                    preference.setTitle("Focal lengths");
                    final StringBuilder builder = new StringBuilder();
                    for (int i = 0; i < focalLengths.length; ++i) {
                        if (i != 0) {
                            builder.append(", ");
                        }
                        builder.append(focalLengths[i]);
                    }
                    builder.append(" mm");
                    preference.setSummary(builder.toString());
                    category.addPreference(preference);
                }

                final StreamConfigurationMap map =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map != null) {
                    preference = new Preference(context);
                    preference.setPersistent(false);
                    preference.setIconSpaceReserved(false);
                    preference.setTitle("Output sizes");
                    final StringBuilder builder = new StringBuilder();
                    final Size[] outputSizes = map.getOutputSizes(SurfaceTexture.class);
                    for (int i = 0; i < outputSizes.length; ++i) {
                        if (i != 0) {
                            builder.append(", ");
                        }
                        builder.append(outputSizes[i]);
                    }
                    builder.append(" px");
                    preference.setSummary(builder.toString());
                    category.addPreference(preference);
                }

                if (Build.VERSION.SDK_INT >= 28) {
                    final Set<String> physicalIds = characteristics.getPhysicalCameraIds();
                    preference = new Preference(context);
                    preference.setPersistent(false);
                    preference.setIconSpaceReserved(false);
                    preference.setTitle("Physical IDs");
                    if (physicalIds.isEmpty()) {
                        preference.setSummary(R.string.not_available);
                    } else {
                        preference.setSummary(String.join(", ", physicalIds));
                    }
                    category.addPreference(preference);
                }
            }
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }

        final PreferenceCategory sensors = new PreferenceCategory(context);
        sensors.setIconSpaceReserved(false);
        sensors.setTitle("Sensors");
        screen.addPreference(sensors);

        final SensorManager sensorManager =
                (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        for (Sensor sensor : sensorManager.getSensorList(Sensor.TYPE_ALL)) {
            preference = new Preference(context);
            preference.setPersistent(false);
            preference.setIconSpaceReserved(false);
            preference.setTitle(sensor.getName());
            preference.setSummary(sensor.getStringType());
            sensors.addPreference(preference);
        }

        final PreferenceCategory locationProviders = new PreferenceCategory(context);
        locationProviders.setTitle("Location providers");
        locationProviders.setIconSpaceReserved(false);
        screen.addPreference(locationProviders);

        final LocationManager locationManager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        for (String name : locationManager.getAllProviders()) {
            final LocationProvider provider = locationManager.getProvider(name);
            preference = new Preference(context);
            preference.setPersistent(false);
            preference.setIconSpaceReserved(false);
            preference.setTitle(name);
            preference.setSummary(provider.getClass().getName());
            locationProviders.addPreference(preference);
        }

        setPreferenceScreen(screen);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            final NavController navController = Navigation.findNavController(view);
            final AppBarConfiguration appBarConfiguration =
                    ((MainActivity) activity).getAppBarConfiguration();
            NavigationUI.setupWithNavController(view.findViewById(R.id.toolbar), navController,
                    appBarConfiguration);
        }
    }
}
