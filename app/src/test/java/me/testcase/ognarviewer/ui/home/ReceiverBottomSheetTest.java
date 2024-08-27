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
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import me.testcase.ognarviewer.R;

@RunWith(RobolectricTestRunner.class)
public class ReceiverBottomSheetTest {
    private ReceiverBottomSheet mBottomSheet;

    @Before
    public void setUp() {
        try (ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class)) {
            mBottomSheet = new ReceiverBottomSheet(controller.get());
        }
    }

    @Test
    public void testColor() {
        final View headerView = mBottomSheet.findViewById(R.id.backdrop);
        final TextView nameView = mBottomSheet.findViewById(R.id.displayNameTextView);
        final TextView typeView = mBottomSheet.findViewById(R.id.text_receiver);

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
        mBottomSheet.setName("EDRC");
        Assert.assertEquals("EDRC", view.getText());
        mBottomSheet.setName(null);
        Assert.assertEquals("N/A", view.getText());
        mBottomSheet.setName("     ");
        Assert.assertEquals("N/A", view.getText());
    }

    @Test
    public void testVersion() {
        final TextView view = mBottomSheet.findViewById(R.id.text_version);
        mBottomSheet.setVersion("v1.2.3.RPI-GPU");
        Assert.assertEquals("v1.2.3.RPI-GPU", view.getText());
        mBottomSheet.setVersion(null);
        Assert.assertEquals("N/A", view.getText());
        mBottomSheet.setVersion("     ");
        Assert.assertEquals("N/A", view.getText());
    }

    @Test
    public void testNtpOffset() {
        final TextView view = mBottomSheet.findViewById(R.id.text_ntp_offset);
        mBottomSheet.setNtpOffset(0);
        Assert.assertEquals("0.0 ms", view.getText());
        mBottomSheet.setNtpOffset(0.14);
        Assert.assertEquals("0.1 ms", view.getText());
        mBottomSheet.setNtpOffset(0.15);
        Assert.assertEquals("0.2 ms", view.getText());
        mBottomSheet.setNtpOffset(Double.NaN);
        Assert.assertEquals("N/A", view.getText());
    }

    @Test
    public void testFreeRam() {
        final TextView view = mBottomSheet.findViewById(R.id.text_free_ram);
        mBottomSheet.setFreeRam(256.4f);
        Assert.assertEquals("256 MB", view.getText());
        mBottomSheet.setFreeRam(256.5f);
        Assert.assertEquals("257 MB", view.getText());
        mBottomSheet.setFreeRam(Float.NaN);
        Assert.assertEquals("N/A", view.getText());
    }

    @Test
    public void testTotalRam() {
        final TextView view = mBottomSheet.findViewById(R.id.text_total_ram);
        mBottomSheet.setTotalRam(512.4f);
        Assert.assertEquals("512 MB", view.getText());
        mBottomSheet.setTotalRam(512.5f);
        Assert.assertEquals("513 MB", view.getText());
        mBottomSheet.setTotalRam(Float.NaN);
        Assert.assertEquals("N/A", view.getText());
    }

    @Test
    public void testCpuTemperature() {
        final TextView view = mBottomSheet.findViewById(R.id.text_cpu_temp);
        mBottomSheet.setCpuTemperature(42.14f);
        Assert.assertEquals("42.1 ℃", view.getText());
        mBottomSheet.setCpuTemperature(42.15f);
        Assert.assertEquals("42.2 ℃", view.getText());
        mBottomSheet.setCpuTemperature(Float.NaN);
        Assert.assertEquals("N/A", view.getText());
    }

    @Test
    public void testCpuLoad() {
        final TextView view = mBottomSheet.findViewById(R.id.text_cpu_load);
        mBottomSheet.setCpuLoad(0.514);
        Assert.assertEquals("51 %", view.getText());
        mBottomSheet.setCpuLoad(0.515);
        Assert.assertEquals("52 %", view.getText());
        mBottomSheet.setCpuLoad(Float.NaN);
        Assert.assertEquals("N/A", view.getText());
    }
}
