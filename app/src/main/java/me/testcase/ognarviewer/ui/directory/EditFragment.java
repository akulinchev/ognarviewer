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

package me.testcase.ognarviewer.ui.directory;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import me.testcase.ognarviewer.App;
import me.testcase.ognarviewer.MainActivity;
import me.testcase.ognarviewer.R;
import me.testcase.ognarviewer.databinding.FragmentEditBinding;
import me.testcase.ognarviewer.directory.DirectoryEntry;
import me.testcase.ognarviewer.utils.AircraftId;

public class EditFragment extends Fragment implements Toolbar.OnMenuItemClickListener {
    private static final String TAG = "EditFragment";

    private long mId;

    private NavController mNavController;

    private FragmentEditBinding mBinding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mId = EditFragmentArgs.fromBundle(getArguments()).getId();
        requireActivity().getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showConfirmationDialog();
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedState) {
        mBinding = FragmentEditBinding.inflate(inflater, container, false);

        final int address = AircraftId.getAddress(mId);
        final int addressType = AircraftId.getAddressType(mId);
        final StringBuilder builder = new StringBuilder();
        if (addressType == AircraftId.ADDRESS_TYPE_RANDOM) {
            builder.append("random");
        } else if (addressType == AircraftId.ADDRESS_TYPE_ICAO) {
            builder.append("icao");
        } else if (addressType == AircraftId.ADDRESS_TYPE_FLARM) {
            builder.append("flarm");
        } else if (addressType == AircraftId.ADDRESS_TYPE_OGN) {
            builder.append("ogn");
        } else {
            builder.append("error");
        }
        builder.append(':');
        builder.append(Long.toHexString(address));
        mBinding.textAddress.setText(builder.toString());

        final DirectoryEntry entry = App.getDirectoryRepository().find(mId);
        if (entry != null) {
            mBinding.textRegistrationNumber.setText(entry.getRegistration());
            mBinding.competitionNumber.setText(entry.getCompetitionNumber());
            mBinding.model.setText(entry.getModel());
            mBinding.owner.setText(entry.getOwner());
            mBinding.textHomeBase.setText(entry.getBaseAirfield());
        }

        mBinding.toolbar.setOnMenuItemClickListener(this);
        mBinding.textRegistrationNumber.setFilters(
                new InputFilter[]{new RegistrationNumberFilter()});

        final Window window = requireActivity().getWindow();
        final WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(window, window.getDecorView());
        insetsController.setAppearanceLightStatusBars(true);

        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedState) {
        // TODO: move this to onStart?
        mNavController = Navigation.findNavController(view);
        final AppBarConfiguration appBarConfiguration =
                ((MainActivity) requireActivity()).getAppBarConfiguration();
        NavigationUI.setupWithNavController(mBinding.toolbar, mNavController, appBarConfiguration);
        mBinding.toolbar.setNavigationOnClickListener(v -> showConfirmationDialog());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
        mNavController = null;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        System.err.println("onMenuItemClick");
        if (item.getItemId() == R.id.action_save) {
            save();
            return true;
        }
        return false;
    }

    private void save() {
        final DirectoryEntry entry = new DirectoryEntry();
        entry.setId(mId);
        final Editable registration = mBinding.textRegistrationNumber.getText();
        if (registration != null) {
            entry.setRegistration(registration.toString());
        }
        final Editable competitionNumber = mBinding.competitionNumber.getText();
        if (competitionNumber != null) {
            entry.setCompetitionNumber(competitionNumber.toString());
        }
        final Editable model = mBinding.model.getText();
        if (model != null) {
            entry.setModel(model.toString());
        }
        final Editable owner = mBinding.owner.getText();
        if (owner != null) {
            entry.setOwner(owner.toString());
        }
        final Editable airfield = mBinding.textHomeBase.getText();
        if (airfield != null) {
            entry.setBaseAirfield(airfield.toString());
        }
        App.getDirectoryRepository().update(entry);
        Toast.makeText(requireContext(), R.string.saved, Toast.LENGTH_LONG).show();
        mNavController.popBackStack();
    }

    private void showConfirmationDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.close_without_saving)
                .setNegativeButton(R.string.close, (dialog, which) -> mNavController.popBackStack())
                .setPositiveButton(R.string.action_save, (dialog, which) -> save())
                .show();
    }

    private static class RegistrationNumberFilter implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                                   int dstart, int dend) {
            final StringBuilder filtered = new StringBuilder();
            for (int i = start; i < end; i++) {
                final char ch = source.charAt(i);
                if (ch >= 'A' && ch <= 'Z' || ch >= '0' && ch <= '9' || ch == '-') {
                    filtered.append(ch);
                } else if (ch >= 'a' && ch <= 'z') {
                    filtered.append(Character.toUpperCase(ch));
                }
            }
            // Note: no need to copy the spans because of android:inputType="textVisiblePassword".
            return filtered.toString();
        }
    }
}
