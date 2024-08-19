/*
 * Copyright © 2024 Ivan Akulinchev <ivan.akulinchev@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */

package me.testcase.ognarviewer.utils;

import org.junit.Assert;
import org.junit.Test;

public class AircraftIdTest {
    @Test
    public void testAddress() {
        Assert.assertEquals(0x123456, AircraftId.getAddress(0x87123456L));
    }

    @Test
    public void testAddressType() {
        Assert.assertEquals(AircraftId.ADDRESS_TYPE_RANDOM, AircraftId.getAddressType(0x84123456L));
        Assert.assertEquals(AircraftId.ADDRESS_TYPE_ICAO, AircraftId.getAddressType(0x85123456L));
        Assert.assertEquals(AircraftId.ADDRESS_TYPE_FLARM, AircraftId.getAddressType(0x86123456L));
        Assert.assertEquals(AircraftId.ADDRESS_TYPE_OGN, AircraftId.getAddressType(0x87123456L));
    }

    @Test
    public void testAircraftType() {
        Assert.assertEquals(1, AircraftId.getAircraftType(0x84123456L));
        Assert.assertEquals(1, AircraftId.getAircraftType(0x85123456L));
        Assert.assertEquals(1, AircraftId.getAircraftType(0x86123456L));
        Assert.assertEquals(1, AircraftId.getAircraftType(0x87123456L));
    }

    @Test
    public void testDirectoryId() {
        Assert.assertEquals(0x00123456, AircraftId.getDirectoryId(0x84123456L));
        Assert.assertEquals(0x01123456, AircraftId.getDirectoryId(0x85123456L));
        Assert.assertEquals(0x02123456, AircraftId.getDirectoryId(0x86123456L));
        Assert.assertEquals(0x03123456, AircraftId.getDirectoryId(0x87123456L));
    }
}
