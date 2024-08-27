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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;
import androidx.transition.Fade;
import androidx.transition.TransitionManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import me.testcase.ognarviewer.App;
import me.testcase.ognarviewer.MainActivity;
import me.testcase.ognarviewer.R;
import me.testcase.ognarviewer.databinding.FragmentHomeBinding;
import me.testcase.ognarviewer.directory.DirectoryEntry;
import me.testcase.ognarviewer.world.Aircraft;
import me.testcase.ognarviewer.world.Receiver;

public class HomeFragment extends Fragment implements AircraftBottomSheet.OnEditButtonListener,
        Toolbar.OnMenuItemClickListener, ManualCalibrationView.ManualCalibrationHost {
    private static final String TAG = "HomeFragment";

    private HomeViewModel mViewModel;

    private FragmentHomeBinding mBinding;
    private BottomSheetBehavior<FrameLayout> mBottomSheetBehavior;

    private AlertDialog mGpsAlertDialog;
    private AlertDialog mReconnectAlertDialog;
    private AlertDialog mSkipCalibrationDialog;

    private String mSelectedTarget;

    private final ActivityResultLauncher<Intent> mShowLocationSettings = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), o -> {
            });

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, String.format("HomeFragment %h created", this));

        super.onCreate(savedInstanceState);

        mViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        mSkipCalibrationDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.skip_calibration_title)
                .setMessage(R.string.skip_calibration_text)
                .setPositiveButton(R.string.give_up, (dialog, which) -> {
                    mViewModel.skipAutoCalibration();
                })
                .setNeutralButton(R.string.let_me_try_again, null)
                .create();

        mReconnectAlertDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.disconnected)
                .setMessage(R.string.connection_failed)
                .setNeutralButton(R.string.exit, (dialog, which) -> requireActivity().finish())
                .setPositiveButton(R.string.retry, (dialog, which) -> mViewModel.reconnect())
                .create();
        mReconnectAlertDialog.setCanceledOnTouchOutside(false);

        mViewModel.getShowReconnectDialog().observe(this, show -> {
            if (show != null && show) {
                mReconnectAlertDialog.show();
            } else {
                mReconnectAlertDialog.dismiss();
            }
        });

        if (savedInstanceState == null) {
            final SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(requireContext());
            if (sharedPreferences.getBoolean("show_disclaimer", true)) {
                new DisclaimerDialogFragment().show(getChildFragmentManager(), null);
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.v(TAG, String.format("HomeFragment %h creates its view", this));
        mBinding = FragmentHomeBinding.inflate(inflater, container, false);

        mBinding.worldView.setOnTargetClickListener(mViewModel::selectTarget);
        mBinding.worldView.setWorld(mViewModel.getWorld());

        mBottomSheetBehavior = BottomSheetBehavior.from(mBinding.bottomSheet);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        mBottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int state) {
                if (state == BottomSheetBehavior.STATE_HIDDEN) {
                    mViewModel.selectTarget(null);
                    mBinding.bottomSheet.removeAllViews();
                }
            }

            @Override
            public void onSlide(@NonNull View view, float v) {
                // Unused.
            }
        });

        mBinding.gpsWaiting.textAccuracy.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });

        mBinding.autoCalibration.getRoot().setVisibility(View.GONE);
        mBinding.autoCalibration.skipButton.setOnApplyWindowInsetsListener((v, insets) -> {
            final ConstraintLayout.LayoutParams layoutParams =
                    (ConstraintLayout.LayoutParams) v.getLayoutParams();
            layoutParams.bottomMargin = insets.getSystemWindowInsetBottom();
            return insets.consumeSystemWindowInsets();
        });

        mBinding.manualCalibration.setOnManualCalibrationListener(mBinding.worldView);
        mBinding.manualCalibration.setHost(this);

        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mViewModel.getOverlayMode().observe(getViewLifecycleOwner(), mode -> {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            switch (mode) {
                case HomeViewModel.OVERLAY_MODE_NONE:
                    mSkipCalibrationDialog.dismiss(); // not relevant anymore
                    mBinding.gpsWaiting.getRoot().setVisibility(View.GONE);
                    mBinding.autoCalibration.getRoot().setVisibility(View.GONE);
                    mBinding.manualCalibration.setVisibility(View.GONE);
                    mBinding.toolbar.setVisibility(View.VISIBLE);
                    mBinding.toolbar.getMenu().findItem(R.id.action_adjust).setVisible(true);
                    break;
                case HomeViewModel.OVERLAY_MODE_WAITING_GPS:
                case HomeViewModel.OVERLAY_MODE_LOW_ACCURACY:
                    if (mode == HomeViewModel.OVERLAY_MODE_WAITING_GPS) {
                        mBinding.gpsWaiting.textView2.setText(R.string.waiting_gps);
                    } else {
                        mBinding.gpsWaiting.textView2.setText(R.string.bad_accuracy);
                    }
                    mBinding.gpsWaiting.getRoot().setVisibility(View.VISIBLE);
                    mBinding.autoCalibration.getRoot().setVisibility(View.GONE);
                    mBinding.manualCalibration.setVisibility(View.GONE);
                    mBinding.toolbar.setVisibility(View.VISIBLE);
                    mBinding.toolbar.getMenu().findItem(R.id.action_adjust).setVisible(false);
                    break;
                case HomeViewModel.OVERLAY_MODE_AUTO_COMPASS_CALIBRATION:
                    mBinding.gpsWaiting.getRoot().setVisibility(View.GONE);
                    mBinding.autoCalibration.getRoot().setVisibility(View.VISIBLE);
                    mBinding.manualCalibration.setVisibility(View.GONE);
                    mBinding.toolbar.setVisibility(View.VISIBLE);
                    mBinding.toolbar.getMenu().findItem(R.id.action_adjust).setVisible(false);
                    break;
                case HomeViewModel.OVERLAY_MODE_MANUAL_COMPASS_CALIBRATION:
                    mBinding.gpsWaiting.getRoot().setVisibility(View.GONE);
                    mBinding.autoCalibration.getRoot().setVisibility(View.GONE);
                    mBinding.manualCalibration.setVisibility(View.VISIBLE);
                    mBinding.toolbar.setVisibility(View.GONE);
                    mBinding.toolbar.getMenu().findItem(R.id.action_adjust).setVisible(false);
                    break;
            }
        });

        mViewModel.getCompassAccuracy().observe(getViewLifecycleOwner(), accuracy -> {
            final String string;
            if (accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
                string = getString(R.string.accuracy_low);
            } else if (accuracy == SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) {
                string = getString(R.string.accuracy_medium);
            } else if (accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH) {
                string = getString(R.string.accuracy_high);
            } else {
                string = getString(R.string.accuracy_unknown);
            }
            mBinding.autoCalibration.currentAccuracy.setText(getString(R.string.current_accuracy,
                    string));
        });

        mViewModel.getLocationAccuracy().observe(getViewLifecycleOwner(), accuracy -> {
            if (accuracy == null) {
                mBinding.gpsWaiting.textAccuracy.setText(null);
            } else {
                mBinding.gpsWaiting.textAccuracy.setText(getString(R.string.waiting_gps_accuracy,
                        Math.round(accuracy.horizontal), Math.round(accuracy.vertical)));
            }
        });

        mViewModel.getSatelliteCount().observe(getViewLifecycleOwner(), count -> {
            if (count == null) {
                mBinding.gpsWaiting.textSatellites.setText(null);
            } else {
                mBinding.gpsWaiting.textSatellites.setText(
                        getResources().getQuantityString(R.plurals.waiting_gps_satellites,
                                count.total, count.total, count.used));
            }
        });

        mGpsAlertDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.gps_disabled)
                .setMessage(R.string.enable_gps)
                .setNeutralButton(R.string.button_continue, (dialog, which) -> {
                    final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    mShowLocationSettings.launch(intent);
                })
                .create();
        mGpsAlertDialog.setCanceledOnTouchOutside(false);

        mViewModel.getGpsEnabled().observe(getViewLifecycleOwner(), enabled -> {
            if (enabled == null || enabled) {
                mGpsAlertDialog.dismiss();
            } else {
                mGpsAlertDialog.show();
            }
        });

        mViewModel.getDemoMode().observe(getViewLifecycleOwner(), active -> {
            if (active != null && active) {
                mBinding.toolbar.setSubtitle(R.string.demo_mode_active);
            } else {
                mBinding.toolbar.setSubtitle(null);
            }
        });

        mViewModel.getSelectedTarget().observe(getViewLifecycleOwner(), target -> {
            if (target == null) {
                mSelectedTarget = null;
                mBinding.worldView.setCurrentTarget(null);
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            } else {
                mBinding.worldView.setCurrentTarget(target.getCallSign());
                if (!target.getCallSign().equals(mSelectedTarget)) {
                    mSelectedTarget = target.getCallSign();
                    if (mBinding.bottomSheet.getChildCount() > 0) {
                        TransitionManager.beginDelayedTransition(mBinding.bottomSheet, new Fade());
                        mBinding.bottomSheet.removeAllViews();
                    }
                    if (target instanceof Aircraft) {
                        final AircraftBottomSheet bottomSheet =
                                new AircraftBottomSheet(requireContext());
                        bottomSheet.setAircraftId(((Aircraft) target).getDirectoryId());
                        mBinding.bottomSheet.addView(bottomSheet);
                    } else if (target instanceof Receiver) {
                        final ReceiverBottomSheet bottomSheet =
                                new ReceiverBottomSheet(requireContext());
                        mBinding.bottomSheet.addView(bottomSheet);
                    }
                }
                if (target instanceof Aircraft) {
                    updateAircraftBottomSheet((Aircraft) target);
                } else if (target instanceof Receiver) {
                    updateReceiverBottomSheet((Receiver) target);
                }
                if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });

        mBinding.autoCalibration.skipButton.setOnClickListener(v -> {
            mSkipCalibrationDialog.show();
        });

        final Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            final NavController navController = Navigation.findNavController(view);
            final AppBarConfiguration appBarConfiguration =
                    ((MainActivity) activity).getAppBarConfiguration();
            NavigationUI.setupWithNavController(mBinding.toolbar, navController,
                    appBarConfiguration);
        }

        mBinding.toolbar.setOnMenuItemClickListener(this);
    }

    @Override
    public void onResume() {
        Log.v(TAG, String.format("HomeFragment %h resumed", this));
        super.onResume();

        if (!((MainActivity) requireActivity()).getDrawerLayout().isOpen()) {
            final Window window = requireActivity().getWindow();
            WindowCompat.getInsetsController(window, window.getDecorView())
                    .setAppearanceLightStatusBars(false);
        }

        mViewModel.requestLocationUpdates();
        mViewModel.registerSensorListener();

        mBinding.worldView.onResume();
        mBinding.gpsWaiting.trafficAnimation.onResume();
        mBinding.autoCalibration.compassAnimation.onResume();
    }

    @Override
    public void onPause() {
        Log.v(TAG, String.format("HomeFragment %h paused", this));

        mBinding.worldView.onPause();
        mBinding.gpsWaiting.trafficAnimation.onPause();
        mBinding.autoCalibration.compassAnimation.onPause();

        if (!requireActivity().isChangingConfigurations()) {
            mViewModel.unregisterSensorListener();
            mViewModel.stopLocationUpdates();
            mViewModel.disconnect();
        }

        super.onPause();
    }

    @Override
    public void onDestroyView() {
        Log.v(TAG, String.format("HomeFragment %h destroys its view", this));
        // Reset, otherwise the bottom sheet will not be shown when the view is re-created.
        mSelectedTarget = null;
        // Leak Canary reports a memory leak otherwise.
        mBinding = null;
        mBottomSheetBehavior = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, String.format("HomeFragment %h destroyed", this));
        super.onDestroy();
    }

    private void updateAircraftBottomSheet(Aircraft aircraft) {
        if (mBinding.bottomSheet.getChildCount() < 1) {
            return;
        }

        final View view = mBinding.bottomSheet.getChildAt(0);
        if (!(view instanceof AircraftBottomSheet)) {
            return;
        }

        final AircraftBottomSheet bottomSheet = (AircraftBottomSheet) view;
        if (bottomSheet.getAircraftId() != aircraft.getDirectoryId()) {
            return;
        }

        final DirectoryEntry entry = App.getDirectoryRepository().find(aircraft.getDirectoryId());

        bottomSheet.setColor(aircraft.getColor());
        String displayName = aircraft.getCallSign();
        if (entry != null && entry.getRegistration() != null) {
            displayName = entry.getRegistration();
        }
        bottomSheet.setName(displayName);
        bottomSheet.setType(aircraft.getType());
        bottomSheet.setGroundSpeed(aircraft.getGroundSpeed());
        final double absoluteAltitude = aircraft.getAltitude();
        final double relativeAltitude = absoluteAltitude - mViewModel.getWorld().getAltitudeMsl();
        bottomSheet.setAltitude(absoluteAltitude, relativeAltitude);
        bottomSheet.setClimbRate(aircraft.getClimbRate());
        bottomSheet.setTrack(aircraft.getHeading());
        // FIXME: no need to update it on each beacon, one time on view creation is enough.
        if (entry == null) {
            bottomSheet.setModel(null);
            bottomSheet.setCompetitionNumber(null);
            bottomSheet.setHome(null);
            bottomSheet.setOwner(null);
        } else {
            bottomSheet.setModel(entry.getModel());
            bottomSheet.setCompetitionNumber(entry.getCompetitionNumber());
            bottomSheet.setHome(entry.getBaseAirfield());
            bottomSheet.setOwner(entry.getOwner());
        }
        bottomSheet.setOnEditButtonListener(this);
    }

    private void updateReceiverBottomSheet(Receiver receiver) {
        if (mBinding.bottomSheet.getChildCount() < 1) {
            return;
        }

        final View view = mBinding.bottomSheet.getChildAt(0);
        if (!(view instanceof ReceiverBottomSheet)) {
            return;
        }

        final ReceiverBottomSheet bottomSheet = (ReceiverBottomSheet) view;
        bottomSheet.setColor(receiver.getColor());
        bottomSheet.setName(receiver.getCallSign());
        bottomSheet.setVersion(receiver.getVersion());
        bottomSheet.setNtpOffset(receiver.getNtpOffset());
        bottomSheet.setFreeRam(receiver.getFreeRam());
        bottomSheet.setTotalRam(receiver.getTotalRam());
        bottomSheet.setCpuTemperature(receiver.getCpuTemperature());
        bottomSheet.setCpuLoad(receiver.getCpuLoad());
    }

    @Override
    public void onEditButtonClicked(long aircraftId) {
        final NavController navController = Navigation.findNavController(requireView());
        navController.navigate(HomeFragmentDirections.editAction(aircraftId));
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_adjust) {
            mViewModel.startManualCalibration();
            return true;
        }
        return false;
    }

    @Override
    public void onManualCalibrationFinished() {
        mViewModel.finishManualCalibration();
    }
}

