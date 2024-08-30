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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.Navigation;

import com.google.android.material.appbar.MaterialToolbar;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowActivity;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import me.testcase.ognarviewer.App;
import me.testcase.ognarviewer.BuildConfig;
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

    @Test
    public void testLinks() {
        Assume.assumeTrue(BuildConfig.DEBUG);
        try (FragmentScenario<AboutFragment> scenario = FragmentScenario.launch(AboutFragment.class, null, R.style.Theme_App)) {
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onFragment(fragment -> {
                final View view = fragment.requireView();
                final ShadowActivity shadowActivity = Shadows.shadowOf(fragment.requireActivity());

                view.findViewById(R.id.button_sources).performClick();
                Intent intent = shadowActivity.getNextStartedActivity();
                Assert.assertEquals(Intent.ACTION_VIEW, intent.getAction());
                Assert.assertEquals(Uri.parse("https://github.com/akulinchev/ognarviewer"), intent.getData());

                view.findViewById(R.id.button_issues).performClick();
                intent = shadowActivity.getNextStartedActivity();
                Assert.assertEquals(Intent.ACTION_VIEW, intent.getAction());
                Assert.assertEquals(Uri.parse("https://github.com/akulinchev/ognarviewer/issues"), intent.getData());

                view.findViewById(R.id.button_contact).performClick();
                intent = shadowActivity.getNextStartedActivity();
                Assert.assertEquals(Intent.ACTION_SENDTO, intent.getAction());
                Assert.assertEquals(Uri.parse("mailto:ivan.akulinchev@gmail.com"), intent.getData());
                final Bundle extras = intent.getExtras();
                Assert.assertNotNull(extras);
                Assert.assertEquals(extras.getString(Intent.EXTRA_SUBJECT), "OGN AR Viewer");
            });
        }
    }

    @Test
    public void testVersion() {
        Assume.assumeTrue(BuildConfig.DEBUG);
        try (FragmentScenario<AboutFragment> scenario = FragmentScenario.launch(AboutFragment.class, null, R.style.Theme_App)) {
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onFragment(fragment -> {
                final TextView view = fragment.requireView().findViewById(R.id.text_version);
                Assert.assertTrue(view.getText().toString().matches("Version \\d+\\.\\d+\\.\\d+"));
            });
        }
    }

    @Test
    public void testOgnDdbAttribution() {
        Assume.assumeTrue(BuildConfig.DEBUG);
        try (FragmentScenario<AboutFragment> scenario = FragmentScenario.launch(AboutFragment.class, null, R.style.Theme_App)) {
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onFragment(fragment -> {
                final TextView view = fragment.requireView().findViewById(R.id.ogn_ddb_attribution);
                final String text = view.getText().toString();
                Assert.assertTrue(text.contains("ODC-BY"));
                final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US);
                final Date date = dateFormat.parse(text, new ParsePosition(text.indexOf(": ") + 2));
                Assert.assertNotNull(date);
                Assert.assertEquals(App.getDirectoryRepository().getOgnDdbAccessTime(), date.getTime());
            });
        }
    }
}
