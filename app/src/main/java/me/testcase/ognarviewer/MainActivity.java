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

package me.testcase.ognarviewer;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements DrawerLayout.DrawerListener,
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
        SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "MainActivity";

    private static final int PERMISSION_REQUEST_CODE = 42;
    private static final int SETTINGS_REQUEST_CODE = 43;

    private boolean mPermissionsRequestInProgress;

    private SharedPreferences mSharedPreferences;

    private AppBarConfiguration mAppBarConfiguration;

    public AppBarConfiguration getAppBarConfiguration() {
        return mAppBarConfiguration;
    }

    public DrawerLayout getDrawerLayout() {
        return findViewById(R.id.nav_layout);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, String.format("MainActivity %h created", this));

        //EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        requestMissingPermissions();

        setContentView(R.layout.activity_main);

        final DrawerLayout navLayout = findViewById(R.id.nav_layout);
        navLayout.addDrawerListener(this);

        mAppBarConfiguration = new AppBarConfiguration.Builder(R.id.nav_home, R.id.nav_directory,
                R.id.nav_settings, R.id.nav_about)
                .setOpenableLayout(navLayout)
                .build();

        final NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
        assert navHostFragment != null;
        final NavController navController = navHostFragment.getNavController();
        navController.addOnDestinationChangedListener((controller, destination, bundle) -> {
            Log.v(TAG, "Navigation destination changed: " + destination);
        });
        navController.setGraph(R.navigation.mobile_navigation);

        final NavigationView navigationView = findViewById(R.id.nav_view);
        NavigationUI.setupWithNavController(navigationView, navController);

        final String action = getIntent().getAction();
        if (action != null && action.equals(Intent.ACTION_APPLICATION_PREFERENCES)) {
            final NavOptions options = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setRestoreState(true)
                    .setEnterAnim(0)
                    .setEnterAnim(0)
                    .setPopUpTo(R.id.nav_home, false, true)
                    .build();
            navController.navigate(R.id.nav_settings, null, options);
        }
    }

    @Override
    protected void onResume() {
        Log.v(TAG, String.format("MainActivity %h resumed", this));
        super.onResume();
        setKeepScreenOn(mSharedPreferences.getBoolean("keep_screen_on", true));
    }

    @Override
    protected void onPause() {
        Log.v(TAG, String.format("MainActivity %h paused", this));
        setKeepScreenOn(false);
        super.onPause();
    }

    private void setKeepScreenOn(boolean on) {
        if (on) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, String.format("MainActivity %h destroyed", this));
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        mSharedPreferences = null;
        mAppBarConfiguration = null;
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            Log.v(TAG, "onRequestPermissionsResult(): " + permissions.length);
            mPermissionsRequestInProgress = false;
            final List<String> deniedPermissions = new ArrayList<>();
            final List<String> confusingPermissions = new ArrayList<>();
            for (int i = 0; i < permissions.length; ++i) {
                final boolean isGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                final boolean isConfusing = shouldShowRequestPermissionRationale(permissions[i]);
                Log.v(TAG, String.format(
                        "onRequestPermissionsResult(): %s: isGranted=%b, isConfusing=%b",
                        permissions[i], isGranted, isConfusing));
                if (!isGranted) {
                    deniedPermissions.add(permissions[i]);
                }
                if (isConfusing) {
                    confusingPermissions.add(permissions[i]);
                }
            }
            if (deniedPermissions.isEmpty()) {
                return; // All fine - do nothing.
            }
            if (confusingPermissions.size() != deniedPermissions.size()) {
                Log.e(TAG, "onRequestPermissionsResult(): permission rejected twice - oh oh...");
                final AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.permission_denied_twice)
                        .setMessage(R.string.permission_denied_twice_message)
                        .setPositiveButton(R.string.button_continue,
                                new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final Intent intent =
                                        new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.fromParts("package", getPackageName(), null));
                                startActivityForResult(intent, SETTINGS_REQUEST_CODE);
                            }
                        })
                        .create();
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
                return;
            }
            final StringBuilder builder = new StringBuilder();
            for (String permission : confusingPermissions) {
                if (permission.equals(Manifest.permission.CAMERA)) {
                    builder.append("- ");
                    builder.append(getString(R.string.no_permission_camera));
                    builder.append('\n');
                } else if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    builder.append("- ");
                    builder.append(getString(R.string.no_permission_location));
                    builder.append('\n');
                }
            }
            builder.append('\n');
            builder.append(getString(R.string.no_permission_footer));
            final AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.no_permission_title)
                    .setMessage(builder.toString())
                    .setPositiveButton(R.string.button_continue,
                            (dialog1, which) -> requestMissingPermissions())
                    .create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SETTINGS_REQUEST_CODE) {
            // Try again...
            requestMissingPermissions();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void requestMissingPermissions() {
        Log.v(TAG, "requestMissingPermissions()");
        final List<String> missingPermissions = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            missingPermissions.add(Manifest.permission.CAMERA);
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_DENIED) {
            missingPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            missingPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (missingPermissions.isEmpty()) {
            Log.v(TAG, "requestMissingPermissions(): all fine");
            return; // Nothing to request.
        }
        if (mPermissionsRequestInProgress) {
            Log.w(TAG, "There are missing permissions, but requestPermissions() is already in "
                    + "progress - ignoring");
             return;
        }
        Log.v(TAG, "Missing permission - calling requestPermissions()");
        requestPermissions(missingPermissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        // Avoid requesting the permission twice - android doesn't like it.
        mPermissionsRequestInProgress = true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (BuildConfig.DEBUG && keyCode == KeyEvent.KEYCODE_ESCAPE) {
            // Take a "transparent" screenshot for Google Play when pressing the ESC key on PC.
            // The screenshot does not include the camera preview (that's kinda the idea).
            final View decorView = getWindow().getDecorView();
            final Bitmap bitmap = Bitmap.createBitmap(decorView.getWidth(), decorView.getHeight(),
                    Bitmap.Config.ARGB_8888);
            final Canvas canvas = new Canvas(bitmap);
            decorView.draw(canvas);
            final ContentValues contentValues = new ContentValues();
            final String fileName = "ognarviewer-" + System.currentTimeMillis() + ".png";
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            final ContentResolver contentResolver = getContentResolver();
            final Uri uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues);
            assert uri != null;
            try (OutputStream outputStream = contentResolver.openOutputStream(uri)) {
                assert outputStream != null;
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                Log.i(TAG, "Saved the transparent screenshot as " + fileName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                bitmap.recycle();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller,
                                             @NonNull Preference pref) {
        final NavController navController = Navigation.findNavController(MainActivity.this,
                R.id.nav_host_fragment);
        if (pref.getKey().equals("debug")) {
            navController.navigate(R.id.nav_debug);
            return true;
        }
        return false;
    }

    @Override
    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
    }

    @Override
    public void onDrawerOpened(@NonNull View drawerView) {
        final Window window = getWindow();
        WindowCompat.getInsetsController(window, window.getDecorView())
                .setAppearanceLightStatusBars(true);
    }

    @Override
    public void onDrawerClosed(@NonNull View drawerView) {
        boolean isHome = false;
        final NavDestination destination = Navigation.findNavController(this,
                R.id.nav_host_fragment).getCurrentDestination();
        if (destination != null) {
            isHome = destination.getId() == R.id.nav_home;
        }
        final Window window = getWindow();
        WindowCompat.getInsetsController(window, window.getDecorView())
                .setAppearanceLightStatusBars(!isHome);
    }

    @Override
    public void onDrawerStateChanged(int newState) {
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          @Nullable String key) {
        setKeepScreenOn(mSharedPreferences.getBoolean("keep_screen_on", true));
    }
}
