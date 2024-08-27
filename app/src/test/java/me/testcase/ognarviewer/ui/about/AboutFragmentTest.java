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

import android.os.Looper;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.view.GravityCompat;
import androidx.navigation.Navigation;

import com.google.android.material.appbar.MaterialToolbar;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;

import me.testcase.ognarviewer.MainActivity;
import me.testcase.ognarviewer.R;

@RunWith(RobolectricTestRunner.class)
public class AboutFragmentTest {
    @Test
    public void testToolbar() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class)) {
            controller.setup();
            final MainActivity activity = controller.get();
            Navigation.findNavController(activity, R.id.nav_host_fragment).navigate(R.id.nav_about);
            final MaterialToolbar toolbar = activity.findViewById(R.id.toolbar);
            Assert.assertEquals("About", toolbar.getTitle());
            View navButton = null;
            for (int i = 0; i < toolbar.getChildCount(); ++i) {
                final View child = toolbar.getChildAt(i);
                if (child instanceof AppCompatImageButton) {
                    navButton = child;
                    break;
                }
            }
            Assert.assertNotNull(navButton);
            Assert.assertFalse(activity.getDrawerLayout().isDrawerVisible(GravityCompat.START));
            navButton.performClick();
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            activity.getDrawerLayout().computeScroll();
            Assert.assertTrue(activity.getDrawerLayout().isDrawerVisible(GravityCompat.START));
        }
    }
}
