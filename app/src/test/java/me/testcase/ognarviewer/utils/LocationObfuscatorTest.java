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

package me.testcase.ognarviewer.utils;

import android.location.Location;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class LocationObfuscatorTest {
    @Test
    public void testEquator() {
        test(0, -0.11, 0, -0.0898);
        test(0, -0.10, 0, -0.0898);
        test(0, -0.09, 0, -0.0898);
        test(0, -0.08, 0, -0.0898);
        test(0, -0.07, 0, -0.0898);

        test(0, -0.06, 0, -0.0449);
        test(0, -0.05, 0, -0.0449);
        test(0, -0.04, 0, -0.0449);
        test(0, -0.03, 0, -0.0449);

        test(0, -0.02, 0, 0);
        test(0, -0.01, 0, 0);
        test(0, 0, 0, 0);
        test(0, +0.01, 0, 0);
        test(0, +0.02, 0, 0);

        test(0, +0.03, 0, +0.0449);
        test(0, +0.04, 0, +0.0449);
        test(0, +0.05, 0, +0.0449);
        test(0, +0.06, 0, +0.0449);

        test(0, +0.07, 0, +0.0898);
        test(0, +0.08, 0, +0.0898);
        test(0, +0.09, 0, +0.0898);
        test(0, +0.10, 0, +0.0898);
        test(0, +0.11, 0, +0.0898);

        test(0, +0.12, 0, +0.1347);
        test(0, +0.13, 0, +0.1347);
        test(0, +0.14, 0, +0.1347);
        test(0, +0.15, 0, +0.1347);
    }

    @Test
    public void testDateLine() {
        test(0, +179.92, 0, +179.9326);
        test(0, +179.93, 0, +179.9326);
        test(0, +179.94, 0, +179.9326);
        test(0, +179.95, 0, +179.9326);

        test(0, +179.96, 0, +179.9775);
        test(0, +179.97, 0, +179.9775);
        test(0, +179.98, 0, +179.9775);
        test(0, +179.99, 0, +179.9775);

        test(0, +180.00, 0, -179.9776);

        test(0, -180.00, 0, +179.9776);

        test(0, -179.99, 0, -179.9775);
        test(0, -179.98, 0, -179.9775);
        test(0, -179.97, 0, -179.9775);
        test(0, -179.96, 0, -179.9775);

        test(0, -179.95, 0, -179.9326);
        test(0, -179.94, 0, -179.9326);
        test(0, -179.93, 0, -179.9326);
        test(0, -179.92, 0, -179.9326);
    }

    @Test
    public void testLatitude60() {
        for (int i = 0; i < 2; ++i) {
            final int sign = (i == 0) ? -1 : 1;

            test(sign * 60.0, -0.07, sign * 60.0075, -0.0473);
            test(sign * 60.0, -0.06, sign * 60.0075, -0.0473);
            test(sign * 60.0, -0.05, sign * 60.0075, -0.0473);
            test(sign * 60.0, -0.04, sign * 60.0075, -0.0473);
            test(sign * 60.0, -0.03, sign * 60.0075, -0.0473);

            test(sign * 60.0, -0.02, sign * 60.0075, 0);
            test(sign * 60.0, -0.01, sign * 60.0075, 0);
            test(sign * 60.0, 0, sign * 60.0075, 0);
            test(sign * 60.0, +0.01, sign * 60.0075, 0);
            test(sign * 60.0, +0.02, sign * 60.0075, 0);

            test(sign * 60.0, +0.03, sign * 60.0075, +0.0473);
            test(sign * 60.0, +0.04, sign * 60.0075, +0.0473);
            test(sign * 60.0, +0.05, sign * 60.0075, +0.0473);
            test(sign * 60.0, +0.06, sign * 60.0075, +0.0473);
            test(sign * 60.0, +0.07, sign * 60.0075, +0.0473);

            test(sign * 60.0, +0.08, sign * 60.0075, +0.0945);
            test(sign * 60.0, +0.09, sign * 60.0075, +0.0945);
            test(sign * 60.0, +0.10, sign * 60.0075, +0.0945);
            test(sign * 60.0, +0.11, sign * 60.0075, +0.0945);

            test(sign * 60.0, +0.12, sign * 60.0075, +0.1418);
            test(sign * 60.0, +0.13, sign * 60.0075, +0.1418);
            test(sign * 60.0, +0.14, sign * 60.0075, +0.1418);
            test(sign * 60.0, +0.15, sign * 60.0075, +0.1418);
            test(sign * 60.0, +0.16, sign * 60.0075, +0.1418);
        }
    }

    @Test
    public void testLatitude90() {
        test(90, 0, 90, 0);
    }

    private void test(double fineLat, double fineLon, double coarseLat, double coarseLon) {
        final Location location = new Location("test");
        location.setLatitude(fineLat);
        location.setLongitude(fineLon);
        Assert.assertEquals(coarseLat, LocationObfuscator.obfuscate(location).getLatitude(), 0.0001);
        Assert.assertEquals(coarseLon, LocationObfuscator.obfuscate(location).getLongitude(), 0.0001);
    }
}
