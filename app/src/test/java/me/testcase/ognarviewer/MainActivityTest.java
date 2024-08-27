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

package me.testcase.ognarviewer;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowPhoneWindow;
import org.robolectric.shadows.ShadowWindow;

@RunWith(RobolectricTestRunner.class)
public class MainActivityTest {
    @Test
    public void testKeepScreenOn() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class)) {
            controller.setup();

            final ShadowWindow shadowWindow = Shadows.shadowOf(controller.get().getWindow());
            Assert.assertTrue(shadowWindow.getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));

            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(controller.get());
            sharedPreferences.edit().putBoolean("keep_screen_on", false).commit();
            Assert.assertFalse(shadowWindow.getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));

            sharedPreferences.edit().putBoolean("keep_screen_on", true).commit();
            Assert.assertTrue(shadowWindow.getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));

            sharedPreferences.edit().putBoolean("keep_screen_on", false).commit();
            Assert.assertFalse(shadowWindow.getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));

            sharedPreferences.edit().clear().commit();
            Assert.assertTrue(shadowWindow.getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));

            controller.pause();
            Assert.assertFalse(shadowWindow.getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));

            controller.resume();
            Assert.assertTrue(shadowWindow.getFlag(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
        }
    }

    @Test
    public void testPermissions() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class)) {
            controller.setup();

            final MainActivity activity = controller.get();
            final ShadowActivity shadowActivity = Shadows.shadowOf(activity);

            final ShadowActivity.PermissionsRequest permissionsRequest1 = shadowActivity.getLastRequestedPermission();
            Assert.assertArrayEquals(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
            }, permissionsRequest1.requestedPermissions);
        }

        Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.CAMERA);

        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class)) {
            controller.setup();

            final MainActivity activity = controller.get();
            final ShadowActivity shadowActivity = Shadows.shadowOf(activity);

            final ShadowActivity.PermissionsRequest permissionsRequest1 = shadowActivity.getLastRequestedPermission();
            Assert.assertArrayEquals(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
            }, permissionsRequest1.requestedPermissions);
        }
    }

    @Test
    public void testEdgeToEdge() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class)) {
            controller.setup();
            final Window window = controller.get().getWindow();
            final ShadowPhoneWindow shadowPhoneWindow = (ShadowPhoneWindow) Shadows.shadowOf(window);
            Assert.assertFalse(shadowPhoneWindow.getDecorFitsSystemWindows());
        }
    }

    @Test
    public void testIntents() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class)) {
            controller.setup();
            final NavController navController = Navigation.findNavController(controller.get(), R.id.nav_host_fragment);
            Assert.assertNotNull(navController.getCurrentDestination());
            Assert.assertEquals("OGN AR Viewer", navController.getCurrentDestination().getLabel());
            Assert.assertEquals(R.id.nav_home, navController.getCurrentDestination().getId());
        }

        final Intent intent = new Intent(Intent.ACTION_APPLICATION_PREFERENCES);
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class, intent)) {
            controller.setup();
            final NavController navController = Navigation.findNavController(controller.get(), R.id.nav_host_fragment);
            Assert.assertNotNull(navController.getCurrentDestination());
            Assert.assertEquals("Settings", navController.getCurrentDestination().getLabel());
            Assert.assertEquals(R.id.nav_settings, navController.getCurrentDestination().getId());
        }
    }

    @Test
    public void testStatusBarColor() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class)) {
            controller.setup();
            final MainActivity activity = controller.get();
            final View decorView = activity.getWindow().getDecorView();
            Assert.assertFalse(activity.getDrawerLayout().isOpen());
            Assert.assertEquals(0, decorView.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            activity.getDrawerLayout().openDrawer(GravityCompat.START, false);
            Assert.assertTrue(activity.getDrawerLayout().isOpen());
            Assert.assertEquals(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR, decorView.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }
}
