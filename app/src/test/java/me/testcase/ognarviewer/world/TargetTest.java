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

package me.testcase.ognarviewer.world;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TargetTest {
    @Test
    public void testSetPosition() {
        final Aircraft aircraft = new Aircraft("NOCALL", 1);
        aircraft.setPosition(1, 2, 3, 1234);
        Assert.assertEquals(3, aircraft.getAltitude(), 0.001);
        aircraft.setPosition(1, 2, 4, 1233);
        Assert.assertEquals(3, aircraft.getAltitude(), 0.001);
        aircraft.setPosition(1, 2, 4, 1234);
        Assert.assertEquals(3, aircraft.getAltitude(), 0.001);
        aircraft.setPosition(1, 2, 4, 1235);
        Assert.assertEquals(4, aircraft.getAltitude(), 0.001);
    }
}
