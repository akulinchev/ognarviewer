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

import org.junit.Assert;
import org.junit.Test;

public class UnitsConverterTest {
    @Test
    public void testFeetToMetres() {
        Assert.assertEquals(0, UnitsConverter.feetToMetres(0));
        Assert.assertEquals(0, UnitsConverter.feetToMetres(1));
        Assert.assertEquals(1, UnitsConverter.feetToMetres(3));
        Assert.assertEquals(10, UnitsConverter.feetToMetres(32));
        Assert.assertEquals(100, UnitsConverter.feetToMetres(328));
        Assert.assertEquals(1000, UnitsConverter.feetToMetres(3280));
        Assert.assertEquals(10000, UnitsConverter.feetToMetres(32808));
        Assert.assertEquals(100000, UnitsConverter.feetToMetres(328084));
    }

    @Test
    public void testMetresToFeet() {
        Assert.assertEquals(0, UnitsConverter.metresToFeet(0));
        Assert.assertEquals(3, UnitsConverter.metresToFeet(1));
        Assert.assertEquals(33, UnitsConverter.metresToFeet(10));
        Assert.assertEquals(328, UnitsConverter.metresToFeet(100));
        Assert.assertEquals(3281, UnitsConverter.metresToFeet(1000));
        Assert.assertEquals(32808, UnitsConverter.metresToFeet(10000));
        Assert.assertEquals(328084, UnitsConverter.metresToFeet(100000));
    }

    @Test
    public void testMetresToMiles() {
        Assert.assertEquals(0, UnitsConverter.metresToMiles(0), 0.001);
        Assert.assertEquals(0.0006, UnitsConverter.metresToMiles(1), 0.0001);
        Assert.assertEquals(0.0062, UnitsConverter.metresToMiles(10), 0.0001);
        Assert.assertEquals(0.0621, UnitsConverter.metresToMiles(100), 0.0001);
        Assert.assertEquals(0.6214, UnitsConverter.metresToMiles(1000), 0.0001);
        Assert.assertEquals(6.2137, UnitsConverter.metresToMiles(10000), 0.0001);
        Assert.assertEquals(62.1371, UnitsConverter.metresToMiles(100000), 0.0001);
    }

    @Test
    public void testMetresToNauticalMiles() {
        Assert.assertEquals(0, UnitsConverter.metresToNauticalMiles(0), 0.001);
        Assert.assertEquals(0.0006, UnitsConverter.metresToNauticalMiles(1), 0.0001);
        Assert.assertEquals(0.0054, UnitsConverter.metresToNauticalMiles(10), 0.0001);
        Assert.assertEquals(0.0540, UnitsConverter.metresToNauticalMiles(100), 0.0001);
        Assert.assertEquals(0.5400, UnitsConverter.metresToNauticalMiles(1000), 0.0001);
        Assert.assertEquals(5.3996, UnitsConverter.metresToNauticalMiles(10000), 0.0001);
        Assert.assertEquals(53.9957, UnitsConverter.metresToNauticalMiles(100000), 0.0001);
    }

    @Test
    public void testKnotsToKmh() {
        Assert.assertEquals(0, UnitsConverter.knotsToKmh(0));
        Assert.assertEquals(2, UnitsConverter.knotsToKmh(1));
        Assert.assertEquals(185, UnitsConverter.knotsToKmh(100));
        Assert.assertEquals(1852, UnitsConverter.knotsToKmh(1000));
        Assert.assertEquals(18520, UnitsConverter.knotsToKmh(10000));
    }

    @Test
    public void testKmhToKnots() {
        Assert.assertEquals(0, UnitsConverter.kmhToKnots(0));
        Assert.assertEquals(1, UnitsConverter.kmhToKnots(2));
        Assert.assertEquals(100, UnitsConverter.kmhToKnots(185));
        Assert.assertEquals(1000, UnitsConverter.kmhToKnots(1852));
        Assert.assertEquals(10000, UnitsConverter.kmhToKnots(18520));
    }

    @Test
    public void testKmhToMph() {
        Assert.assertEquals(0, UnitsConverter.kmhToMph(0));
        Assert.assertEquals(10, UnitsConverter.kmhToMph(16));
        Assert.assertEquals(99, UnitsConverter.kmhToMph(160));
    }
}
