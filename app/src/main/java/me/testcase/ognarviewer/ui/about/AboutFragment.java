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

package me.testcase.ognarviewer.ui.about;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.util.Date;

import me.testcase.ognarviewer.App;
import me.testcase.ognarviewer.BuildConfig;
import me.testcase.ognarviewer.MainActivity;
import me.testcase.ognarviewer.R;
import me.testcase.ognarviewer.databinding.FragmentAboutBinding;

public class AboutFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "AboutFragment";

    private FragmentAboutBinding mBinding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, String.format("AboutFragment %h created", this));
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentAboutBinding.inflate(inflater, container, false);
        mBinding.textVersion.setText(getString(R.string.app_version, BuildConfig.VERSION_NAME));
        final Date accessTime = new Date(App.getDirectoryRepository().getOgnDdbAccessTime());
        mBinding.ognDdbAttribution.setText(getString(R.string.ognddb_attribution, accessTime));
        mBinding.buttonSources.setOnClickListener(this);
        mBinding.buttonIssues.setOnClickListener(this);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        final Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            final NavController navController = Navigation.findNavController(view);
            final AppBarConfiguration appBarConfiguration =
                    ((MainActivity) activity).getAppBarConfiguration();
            NavigationUI.setupWithNavController(mBinding.toolbar, navController,
                    appBarConfiguration);
        }
    }

    @Override
    public void onResume() {
        Log.v(TAG, String.format("AboutFragment %h resumed", this));
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.v(TAG, String.format("AboutFragment %h paused", this));
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        Log.v(TAG, String.format("AboutFragment %h destroys its view", this));
        mBinding = null; // Prevent memory leaks.
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, String.format("AboutFragment %h destroyed", this));
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        if (v == mBinding.buttonSources) {
            intent.setData(Uri.parse("https://github.com/akulinchev/ognarviewer"));
        } else {
            intent.setData(Uri.parse("https://github.com/akulinchev/ognarviewer/issues"));
        }
        startActivity(intent);
    }
}
