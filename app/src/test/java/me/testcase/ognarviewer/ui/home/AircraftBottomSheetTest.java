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
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;

import me.testcase.ognarviewer.R;

@RunWith(RobolectricTestRunner.class)
public class AircraftBottomSheetTest {
    private AircraftBottomSheet mBottomSheet;
    private SharedPreferences mSharedPreferences;

    @Before
    public void setUp() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class)) {
            mBottomSheet = new AircraftBottomSheet(controller.get());
            mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(controller.get());
        }
    }

    @Test
    public void testAircraftId() {
        Assert.assertEquals(0, mBottomSheet.getAircraftId());
        mBottomSheet.setAircraftId(42);
        Assert.assertEquals(42, mBottomSheet.getAircraftId());
    }

    @Test
    public void testColor() {
        final View headerView = mBottomSheet.findViewById(R.id.backdrop);
        final TextView nameView = mBottomSheet.findViewById(R.id.displayNameTextView);
        final TextView typeView = mBottomSheet.findViewById(R.id.typeTextView);

        mBottomSheet.setColor(Color.DKGRAY);
        Assert.assertNotNull(headerView.getBackgroundTintList());
        Assert.assertEquals(Color.DKGRAY, headerView.getBackgroundTintList().getDefaultColor());
        Assert.assertEquals(0xffffffff, nameView.getTextColors().getDefaultColor());
        Assert.assertEquals(0x99ffffff, typeView.getTextColors().getDefaultColor());

        mBottomSheet.setColor(Color.LTGRAY);
        Assert.assertNotNull(headerView.getBackgroundTintList());
        Assert.assertEquals(Color.LTGRAY, headerView.getBackgroundTintList().getDefaultColor());
        Assert.assertEquals(0xff000000, nameView.getTextColors().getDefaultColor());
        Assert.assertEquals(0x99000000, typeView.getTextColors().getDefaultColor());
    }

    @Test
    public void testName() {
        final TextView view = mBottomSheet.findViewById(R.id.displayNameTextView);
        mBottomSheet.setName("D-1234");
        Assert.assertEquals("D-1234", view.getText());
        mBottomSheet.setName(null);
        Assert.assertEquals("N/A", view.getText());
        mBottomSheet.setName("     ");
        Assert.assertEquals("N/A", view.getText());
    }

    @Test
    public void testType() {
        final TextView view = mBottomSheet.findViewById(R.id.typeTextView);
        mBottomSheet.setType(-1);
        Assert.assertEquals("Unknown", view.getText());
        mBottomSheet.setType(0);
        Assert.assertEquals("Unknown", view.getText());
        mBottomSheet.setType(1);
        Assert.assertEquals("Glider", view.getText());
        mBottomSheet.setType(2);
        Assert.assertEquals("Tow plane", view.getText());
        mBottomSheet.setType(3);
        Assert.assertEquals("Helicopter", view.getText());
        mBottomSheet.setType(4);
        Assert.assertEquals("Parachute", view.getText());
        mBottomSheet.setType(5);
        Assert.assertEquals("Drop plane", view.getText());
        mBottomSheet.setType(6);
        Assert.assertEquals("Hang glider", view.getText());
        mBottomSheet.setType(7);
        Assert.assertEquals("Para glider", view.getText());
        mBottomSheet.setType(8);
        Assert.assertEquals("Powered aircraft", view.getText());
        mBottomSheet.setType(9);
        Assert.assertEquals("Jet aircraft", view.getText());
        mBottomSheet.setType(10);
        Assert.assertEquals("UFO", view.getText());
        mBottomSheet.setType(11);
        Assert.assertEquals("Balloon", view.getText());
        mBottomSheet.setType(12);
        Assert.assertEquals("Airship", view.getText());
        mBottomSheet.setType(13);
        Assert.assertEquals("UAV", view.getText());
        mBottomSheet.setType(14);
        Assert.assertEquals("Unknown", view.getText());
        mBottomSheet.setType(15);
        Assert.assertEquals("Static object", view.getText());
        mBottomSheet.setType(16);
        Assert.assertEquals("Unknown", view.getText());
    }

    @Test
    public void testGroundSpeed() {
        final TextView view = mBottomSheet.findViewById(R.id.groundSpeedTextView);

        mBottomSheet.setGroundSpeed(0);
        Assert.assertEquals("0 km/h", view.getText());
        mBottomSheet.setGroundSpeed(1);
        Assert.assertEquals("1 km/h", view.getText());
        mBottomSheet.setGroundSpeed(100);
        Assert.assertEquals("100 km/h", view.getText());

        mSharedPreferences.edit().putString("units_speed", "knots").commit();
        mBottomSheet.setGroundSpeed(0);
        Assert.assertEquals("0 kt", view.getText());
        mBottomSheet.setGroundSpeed(1);
        Assert.assertEquals("1 kt", view.getText());
        mBottomSheet.setGroundSpeed(18);
        Assert.assertEquals("10 kt", view.getText());
        mBottomSheet.setGroundSpeed(185);
        Assert.assertEquals("100 kt", view.getText());

        mSharedPreferences.edit().putString("units_speed", "mph").commit();
        mBottomSheet.setGroundSpeed(0);
        Assert.assertEquals("0 mph", view.getText());
        mBottomSheet.setGroundSpeed(1);
        Assert.assertEquals("1 mph", view.getText());
        mBottomSheet.setGroundSpeed(16);
        Assert.assertEquals("10 mph", view.getText());
        mBottomSheet.setGroundSpeed(160);
        Assert.assertEquals("99 mph", view.getText());
    }

    @Test
    public void testAltitude() {
        final TextView view = mBottomSheet.findViewById(R.id.altitudeTextView);

        mBottomSheet.setAltitude(1000, 500);
        Assert.assertEquals("1000 m / 500 m", view.getText());
        mBottomSheet.setAltitude(1000.4, 500.4);
        Assert.assertEquals("1000 m / 500 m", view.getText());
        mBottomSheet.setAltitude(1000.5, 500.5);
        Assert.assertEquals("1001 m / 501 m", view.getText());
        mBottomSheet.setAltitude(100, -30);
        Assert.assertEquals("100 m / -30 m", view.getText());

        mSharedPreferences.edit().putString("units_altitude", "feet").commit();
        mBottomSheet.setAltitude(3280, 328);
        Assert.assertEquals("10761 ft / 1076 ft", view.getText());
    }

    @Test
    public void testClimbRate() {
        final TextView view = mBottomSheet.findViewById(R.id.text_climb_rate);

        mBottomSheet.setClimbRate(0);
        Assert.assertEquals("+0.0 m/s", view.getText());
        mBottomSheet.setClimbRate(0.44f);
        Assert.assertEquals("+0.4 m/s", view.getText());
        mBottomSheet.setClimbRate(0.45f);
        Assert.assertEquals("+0.4 m/s", view.getText());
        mBottomSheet.setClimbRate(0.46f);
        Assert.assertEquals("+0.5 m/s", view.getText());
        mBottomSheet.setClimbRate(-1);
        Assert.assertEquals("-1.0 m/s", view.getText());
        mBottomSheet.setClimbRate(-1.44f);
        Assert.assertEquals("-1.4 m/s", view.getText());
        mBottomSheet.setClimbRate(-1.45f);
        Assert.assertEquals("-1.5 m/s", view.getText());
        mBottomSheet.setClimbRate(-1.46f);
        Assert.assertEquals("-1.5 m/s", view.getText());

        mSharedPreferences.edit().putString("units_climb_rate", "feet_per_minute").commit();
        mBottomSheet.setClimbRate(0);
        Assert.assertEquals("+0 ft/min", view.getText());
        mBottomSheet.setClimbRate(0.5f);
        Assert.assertEquals("+98 ft/min", view.getText());
        mBottomSheet.setClimbRate(1);
        Assert.assertEquals("+197 ft/min", view.getText());
        mBottomSheet.setClimbRate(-0.5f);
        Assert.assertEquals("-98 ft/min", view.getText());
        mBottomSheet.setClimbRate(-1);
        Assert.assertEquals("-197 ft/min", view.getText());

        mBottomSheet.setClimbRate(Float.NaN);
        Assert.assertEquals("N/A", view.getText());
    }

    @Test
    public void testTrack() {
        final TextView view = mBottomSheet.findViewById(R.id.text_track);
        mBottomSheet.setTrack(0);
        Assert.assertEquals("N/A", view.getText());
        for (int i = 1; i < 24; ++i) {
            mBottomSheet.setTrack(i);
            Assert.assertEquals("N", view.getText());
        }
        for (int i = 24; i < 69; ++i) {
            mBottomSheet.setTrack(i);
            Assert.assertEquals("NE", view.getText());
        }
        for (int i = 69; i < 114; ++i) {
            mBottomSheet.setTrack(i);
            Assert.assertEquals("E", view.getText());
        }
        for (int i = 114; i < 159; ++i) {
            mBottomSheet.setTrack(i);
            Assert.assertEquals("SE", view.getText());
        }
        for (int i = 159; i < 204; ++i) {
            mBottomSheet.setTrack(i);
            Assert.assertEquals("S", view.getText());
        }
        for (int i = 204; i < 249; ++i) {
            mBottomSheet.setTrack(i);
            Assert.assertEquals("SW", view.getText());
        }
        for (int i = 249; i < 294; ++i) {
            mBottomSheet.setTrack(i);
            Assert.assertEquals("W", view.getText());
        }
        for (int i = 294; i < 339; ++i) {
            mBottomSheet.setTrack(i);
            Assert.assertEquals("NW", view.getText());
        }
        for (int i = 339; i < 361; ++i) {
            mBottomSheet.setTrack(i);
            Assert.assertEquals("N", view.getText());
        }
        mBottomSheet.setTrack(361);
        Assert.assertEquals("N/A", view.getText());
    }

    @Test
    public void testModel() {
        final TextView view = mBottomSheet.findViewById(R.id.modelTextView);
        mBottomSheet.setModel("Duo Discus");
        Assert.assertEquals("Duo Discus", view.getText());
        mBottomSheet.setModel(null);
        Assert.assertEquals("N/A", view.getText());
        mBottomSheet.setModel("     ");
        Assert.assertEquals("N/A", view.getText());
    }

    @Test
    public void testOwner() {
        final TextView view = mBottomSheet.findViewById(R.id.ownerTextView);
        mBottomSheet.setOwner("John Doe");
        Assert.assertEquals("John Doe", view.getText());
        mBottomSheet.setOwner(null);
        Assert.assertEquals("N/A", view.getText());
        mBottomSheet.setOwner("     ");
        Assert.assertEquals("N/A", view.getText());
    }

    @Test
    public void testCompetitionNumber() {
        final TextView view = mBottomSheet.findViewById(R.id.text_cn);
        mBottomSheet.setCompetitionNumber("7L");
        Assert.assertEquals("7L", view.getText());
        mBottomSheet.setCompetitionNumber(null);
        Assert.assertEquals("N/A", view.getText());
        mBottomSheet.setCompetitionNumber("     ");
        Assert.assertEquals("N/A", view.getText());
    }

    @Test
    public void testHome() {
        final TextView view = mBottomSheet.findViewById(R.id.text_home);
        mBottomSheet.setHome("My backyard");
        Assert.assertEquals("My backyard", view.getText());
        mBottomSheet.setHome(null);
        Assert.assertEquals("N/A", view.getText());
        mBottomSheet.setHome("     ");
        Assert.assertEquals("N/A", view.getText());
    }

    @Test
    public void testEditButton() {
        mBottomSheet.setAircraftId(42);
        final ImageButton button = mBottomSheet.findViewById(R.id.button_edit);
        button.performClick();
        final OnEditButtonListener listener = new OnEditButtonListener();
        mBottomSheet.setOnEditButtonListener(listener);
        button.performClick();
        Assert.assertEquals(1, listener.clickCount);
    }

    private static class OnEditButtonListener implements AircraftBottomSheet.OnEditButtonListener {
        public int clickCount = 0;

        @Override
        public void onEditButtonClicked(long aircraftId) {
            ++clickCount;
            Assert.assertEquals(42, aircraftId);
        }
    }
}
