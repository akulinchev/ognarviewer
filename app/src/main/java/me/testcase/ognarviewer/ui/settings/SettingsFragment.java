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
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceFragmentCompat;

import me.testcase.ognarviewer.MainActivity;
import me.testcase.ognarviewer.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = "SettingsFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, String.format("SettingsFragment %h created", this));
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final View container = view.findViewById(android.R.id.list_container);
        container.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });

        final Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            final NavController navController = Navigation.findNavController(view);
            final AppBarConfiguration appBarConfiguration =
                    ((MainActivity) activity).getAppBarConfiguration();
            NavigationUI.setupWithNavController(view.findViewById(R.id.toolbar), navController,
                    appBarConfiguration);
        }
    }

    @Override
    public void onResume() {
        Log.v(TAG, String.format("SettingsFragment %h resumed", this));
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.v(TAG, String.format("SettingsFragment %h paused", this));
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, String.format("SettingsFragment %h destroyed", this));
        super.onDestroy();
    }
}
