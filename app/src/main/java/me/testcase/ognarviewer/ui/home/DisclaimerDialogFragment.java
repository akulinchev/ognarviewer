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

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import me.testcase.ognarviewer.R;

public class DisclaimerDialogFragment extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Context context = requireContext();
        final AlertDialog alertDialog = new MaterialAlertDialogBuilder(context)
                .setIcon(R.drawable.outline_info_24)
                .setTitle(R.string.beware_title)
                .setView(R.layout.dialog_disclaimer)
                .setPositiveButton(R.string.button_continue, (dialogInterface, i) -> {
                    final AlertDialog dialog = (AlertDialog) dialogInterface;
                    final CheckBox checkBox = dialog.findViewById(R.id.checkbox);
                    if (checkBox != null && checkBox.isChecked()) {
                        final SharedPreferences.Editor editor =
                                PreferenceManager.getDefaultSharedPreferences(context).edit();
                        editor.putBoolean("show_disclaimer", false);
                        editor.apply();
                    }
                })
                .create();
        alertDialog.setCanceledOnTouchOutside(false);
        return alertDialog;
    }
}
