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

package me.testcase.ognarviewer;

import android.location.Location;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class CalibratedClockTest {
    @Test
    public void testNmeaParser() {
        final Location location = new Location("test");
        Assert.assertEquals(0, CalibratedClock.getTimeError());

        location.setTime(System.currentTimeMillis());
        CalibratedClock.sync(location);
        Assert.assertTrue(-1 <= CalibratedClock.getTimeError() && CalibratedClock.getTimeError() <= 0);

        location.setTime(System.currentTimeMillis() - 100);
        CalibratedClock.sync(location);
        Assert.assertTrue(-101 <= CalibratedClock.getTimeError() && CalibratedClock.getTimeError() <= -100);

        location.setTime(System.currentTimeMillis() + 100);
        CalibratedClock.sync(location);
        Assert.assertTrue(100 <= CalibratedClock.getTimeError() && CalibratedClock.getTimeError() <= 101);
    }
}
