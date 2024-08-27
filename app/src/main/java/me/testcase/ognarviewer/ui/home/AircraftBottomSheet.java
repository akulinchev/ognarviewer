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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.preference.PreferenceManager;

import me.testcase.ognarviewer.R;
import me.testcase.ognarviewer.databinding.BottomSheetAircraftBinding;
import me.testcase.ognarviewer.utils.UnitsConverter;

/**
 * Bottom sheet content view for an aircraft.
 */
public final class AircraftBottomSheet extends FrameLayout implements View.OnClickListener {
    private final BottomSheetAircraftBinding mBinding;
    private final SharedPreferences mSharedPreferences;

    private long mId;
    private OnEditButtonListener mListener;

    /**
     * Construct a new bottom sheet.
     *
     * @param context Context.
     */
    public AircraftBottomSheet(Context context) {
        super(context);
        mBinding = BottomSheetAircraftBinding.inflate(LayoutInflater.from(context), this, true);
        mBinding.buttonEdit.setOnClickListener(this);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * Get the aircraft ID.
     *
     * @return Aircraft ID.
     */
    public long getAircraftId() {
        return mId;
    }

    /**
     * Get the aircraft ID.
     *
     * @param id Aircraft ID.
     */
    public void setAircraftId(long id) {
        mId = id;
    }

    /**
     * Set the header color.
     *
     * <p>Font color and the edit button tint are adjusted automatically for contrast.</p>
     *
     * @param color The ARGB color to use.
     */
    public void setColor(int color) {
        final boolean light = Color.luminance(color) >= 0.45;
        mBinding.backdrop.setBackgroundTintList(ColorStateList.valueOf(color));
        mBinding.displayNameTextView.setTextColor(light ? Color.BLACK : Color.WHITE);
        mBinding.typeTextView.setTextColor(light ? 0x99000000 : 0x99ffffff);
        mBinding.buttonEdit.setColorFilter(light ? Color.BLACK : Color.WHITE);
    }

    /**
     * Set the display name.
     *
     * @param name Display name.
     */
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            mBinding.displayNameTextView.setText(R.string.not_available);
        } else {
            mBinding.displayNameTextView.setText(name);
        }
    }

    /**
     * Set the aircraft type displayed in the header below the registration number.
     *
     * @param type Aircraft type (0-15).
     */
    public void setType(int type) {
        final String[] types = getResources().getStringArray(R.array.aircraft_types);
        if (type < 0 || type >= types.length) {
            type = 0;
        }
        mBinding.typeTextView.setText(types[type]);
    }

    /**
     * Set the ground speed.
     *
     * @param speed Ground speed in km/h.
     */
    public void setGroundSpeed(int speed) {
        final String text;
        switch (mSharedPreferences.getString("units_speed", "kmh")) {
            case "knots":
                final int knots = UnitsConverter.kmhToKnots(speed);
                text = getContext().getString(R.string.ground_speed_format_kt, knots);
                break;
            case "mph":
                final int mph = UnitsConverter.kmhToMph(speed);
                text = getContext().getString(R.string.ground_speed_format_mph, mph);
                break;
            case "kmh":
            default:
                text = getContext().getString(R.string.ground_speed_format_kmh, speed);
                break;
        }
        mBinding.groundSpeedTextView.setText(text);
    }

    /**
     * Set the altitude in meters.
     *
     * @param absolute Altitude over MSL.
     * @param relative Altitude over the user.
     */
    public void setAltitude(double absolute, double relative) {
        final String text;
        switch (mSharedPreferences.getString("units_altitude", "meters")) {
            case "feet":
                final int absoluteFeet = UnitsConverter.metresToFeet((int) Math.round(absolute));
                final int relativeFeet = UnitsConverter.metresToFeet((int) Math.round(relative));
                text = getContext().getString(R.string.altitude_format_feet, absoluteFeet,
                        relativeFeet);
                break;
            case "meters":
            default:
                final long absoluteMeters = Math.round(absolute);
                final long relativeMeters = Math.round(relative);
                text = getContext().getString(R.string.altitude_format_meters, absoluteMeters,
                        relativeMeters);
                break;
        }
        mBinding.altitudeTextView.setText(text);
    }

    /**
     * Set the climb rate.
     *
     * @param climbRate Rate of climb in m/s.
     */
    public void setClimbRate(float climbRate) {
        if (Float.isNaN(climbRate)) {
            mBinding.textClimbRate.setText(R.string.not_available);
        } else if (mSharedPreferences.getString("units_climb_rate", "meters_per_second").equals(
                "feet_per_minute")) {
            final long value = UnitsConverter.metresToFeet(Math.round(climbRate * 60));
            mBinding.textClimbRate.setText(
                    getContext().getString(R.string.climb_rate_format_imperial, value));
        } else {
            mBinding.textClimbRate.setText(
                    getContext().getString(R.string.climb_rate_format_metric, climbRate));
        }
    }

    /**
     * Set the track.
     *
     * @param track Track in degrees (1-360) or 0 for "N/A".
     */
    public void setTrack(int track) {
        if (track < 1 || track > 360) {
            mBinding.textTrack.setText(R.string.not_available);
        } else {
            final String[] tracks = getResources().getStringArray(R.array.aircraft_track);
            mBinding.textTrack.setText(tracks[(int) Math.round((track - 1) / 45.0)]);
        }
    }

    /**
     * Set the aircraft model.
     *
     * <p>When null or a whitespace only string, "N/A" is shown instead.</p>
     *
     * @param model Aircraft model.
     */
    public void setModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            mBinding.modelTextView.setText(R.string.not_available);
        } else {
            mBinding.modelTextView.setText(model);
        }
    }

    /**
     * Set the aircraft owner.
     *
     * <p>When null or a whitespace only string, "N/A" is shown instead.</p>
     *
     * @param owner Aircraft owner.
     */
    public void setOwner(String owner) {
        if (owner == null || owner.trim().isEmpty()) {
            mBinding.ownerTextView.setText(R.string.not_available);
        } else {
            mBinding.ownerTextView.setText(owner);
        }
    }

    /**
     * Set the competition number.
     *
     * <p>When null or a whitespace only string, "N/A" is shown instead.</p>
     *
     * @param competitionNumber Competition number (e.g. "7L").
     */
    public void setCompetitionNumber(String competitionNumber) {
        if (competitionNumber == null || competitionNumber.trim().isEmpty()) {
            mBinding.textCn.setText(R.string.not_available);
        } else {
            mBinding.textCn.setText(competitionNumber);
        }
    }

    /**
     * Set the base airfield.
     *
     * <p>When null or a whitespace only string, "N/A" is shown instead.</p>
     *
     * @param home Base airfield (e.g. "Marpingen").
     */
    public void setHome(String home) {
        if (home == null || home.trim().isEmpty()) {
            mBinding.textHome.setText(R.string.not_available);
        } else {
            mBinding.textHome.setText(home);
        }
    }

    /**
     * Set the callback called when the user presses the edit button in the header.
     *
     * @param listener The callback.
     */
    public void setOnEditButtonListener(OnEditButtonListener listener) {
        mListener = listener;
    }

    @Override
    public void onClick(View view) {
        if (view == mBinding.buttonEdit && mListener != null) {
            mListener.onEditButtonClicked(mId);
        }
    }

    public interface OnEditButtonListener {
        void onEditButtonClicked(long aircraftId);
    }
}
