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

package me.testcase.ognarviewer.world;

import android.location.Location;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import me.testcase.ognarviewer.CalibratedClock;

@RunWith(RobolectricTestRunner.class)
public class WorldTest {
    private static final long TIME = 1721460905000L;

    private final float[] mCoordinates = new float[4];

    @Test
    public void testSamePosition() {
        final Location location = new Location("test");
        location.setLatitude(49);
        location.setLongitude(7);
        location.setAltitude(350);

        final World world = new World();
        world.setGeoidHeight(48.4);
        world.setPosition(location);

        final Aircraft aircraft = world.addAircraft(
                "ICA000001", 0x00000000, 49, 7, 350, TIME);
        aircraft.setGroundSpeed(360); // 360 km/h = 100 m/s
        aircraft.setClimbRate(1); // 1 m/s
        aircraft.setTurnRate(0);
        aircraft.setHeading(90); // East

        // Make sure there is no division by zero, etc.
        world.getTargetCoordinates(aircraft, TIME, mCoordinates);
        assertCoordinates(mCoordinates, 0.0, 0.0, 48.4000);
    }

    @Test
    public void testWorldBoundaries() {
        final Location location = new Location("test");
        location.setLatitude(-16.3934722441544);
        location.setLongitude(179.56172328323768);
        location.setAltitude(0);

        final World world = new World();
        world.setGeoidHeight(53.8);
        world.setPosition(location);

        Aircraft aircraft = world.addAircraft(
                "ICA000001", 0x00000000, -16.4, 179.5, 100, TIME);
        aircraft.setGroundSpeed(360);
        aircraft.setClimbRate(0);
        aircraft.setTurnRate(0);
        aircraft.setHeading(90);

        world.getTargetCoordinates(aircraft, TIME, mCoordinates);
        assertCoordinates(mCoordinates, -6591.6713, -726.6664, 153.8);

        aircraft = world.addAircraft("ICA000002", 0x00000000, -16.3, -180, 100, TIME);

        world.getTargetCoordinates(aircraft, TIME, mCoordinates);
        assertCoordinates(mCoordinates, 46805.2890, 10405.2822, 153.8);

        location.setLatitude(0);
        location.setLongitude(-179);
        world.setGeoidHeight(20.2);
        world.setPosition(location);

        // Try to look from the other side (it's another if-branch in the code).
        aircraft = world.addAircraft("ICA000003", 0x00000000, 0, 180, 100, TIME);
        world.getTargetCoordinates(aircraft, TIME, mCoordinates);
        assertCoordinates(mCoordinates, -111319.4921, 0.0, 120.2);

        location.setLatitude(7);
        location.setLongitude(180);
        world.setGeoidHeight(15.6);
        world.setPosition(location);

        // -180 should be the same as +180.
        aircraft = world.addAircraft("ICA000004", 0x00000000, 7, -180, 100, TIME);
        world.getTargetCoordinates(aircraft, TIME, mCoordinates);
        assertCoordinates(mCoordinates, 0.0, 0.0, 115.6);
    }

    @Test
    public void testStraightLine() {
        final Location location = new Location("test");
        location.setLatitude(49);
        location.setLongitude(7);
        location.setAltitude(350);

        final World world = new World();
        world.setPosition(location);
        world.setGeoidHeight(48.6);
        world.setLocationPredictionEnabled(true);

        Assert.assertTrue(world.isLocationPredictionEnabled());

        final Aircraft aircraft = world.addAircraft(
                "ICA000001", 0x00000000, 49.1, 7.1, 1400, TIME);
        aircraft.setGroundSpeed(360); // 360 km/h = 100 m/s
        aircraft.setClimbRate(1); // 1 m/s
        aircraft.setTurnRate(0);
        aircraft.setHeading(90); // East

        final double x = 7303.2158;
        final double y = 11131.9492;
        final double z = 1098.6;

        world.getTargetCoordinates(aircraft, TIME, mCoordinates);
        assertCoordinates(mCoordinates, x, y, z);

        world.getTargetCoordinates(aircraft, TIME + 100, mCoordinates);
        assertCoordinates(mCoordinates, x + 10.0, y, z + 0.1);

        world.getTargetCoordinates(aircraft, TIME + 1000, mCoordinates);
        assertCoordinates(mCoordinates, x + 100.0, y, z + 1.0);

        world.getTargetCoordinates(aircraft, TIME + 5000, mCoordinates);
        assertCoordinates(mCoordinates, x + 500.0, y, z + 5.0);

        world.getTargetCoordinates(aircraft, TIME + 10000, mCoordinates);
        assertCoordinates(mCoordinates, x + 1000.0, y, z + 10.0);

        world.getTargetCoordinates(aircraft, TIME + 15000, mCoordinates);
        assertCoordinates(mCoordinates, x + 1000.0, y, z + 10.0);

        world.getTargetCoordinates(aircraft, TIME - 1000, mCoordinates);
        assertCoordinates(mCoordinates, x, y, z);

        aircraft.setClimbRate(2); // 2 m/s
        aircraft.setHeading(270); // West

        world.getTargetCoordinates(aircraft, TIME, mCoordinates);
        assertCoordinates(mCoordinates, x, y, z);

        world.getTargetCoordinates(aircraft, TIME + 1000, mCoordinates);
        assertCoordinates(mCoordinates, x - 100.0, y, z + 2.0);

        aircraft.setClimbRate(-1); // -1 m/s
        aircraft.setHeading(360); // North

        world.getTargetCoordinates(aircraft, TIME, mCoordinates);
        assertCoordinates(mCoordinates, x, y, z);

        world.getTargetCoordinates(aircraft, TIME + 1000, mCoordinates);
        assertCoordinates(mCoordinates, x, y + 100.0, z - 1.0);

        aircraft.setClimbRate(-2); // -2 m/s
        aircraft.setHeading(180); // South

        world.getTargetCoordinates(aircraft, TIME, mCoordinates);
        assertCoordinates(mCoordinates, x, y, z);

        world.getTargetCoordinates(aircraft, TIME + 1000, mCoordinates);
        assertCoordinates(mCoordinates, x, y - 100.0, z - 2.0);

        aircraft.setClimbRate(0);
        aircraft.setHeading(45); // North-East

        world.getTargetCoordinates(aircraft, TIME, mCoordinates);
        assertCoordinates(mCoordinates, x, y, z);

        world.getTargetCoordinates(aircraft, TIME + 1000, mCoordinates);
        assertCoordinates(mCoordinates, x + 70.7105, y + 70.7110, z);

        aircraft.setHeading(225); // South-West

        world.getTargetCoordinates(aircraft, TIME, mCoordinates);
        assertCoordinates(mCoordinates, x, y, z);

        world.getTargetCoordinates(aircraft, TIME + 1000, mCoordinates);
        assertCoordinates(mCoordinates, x - 70.7104, y - 70.7109, z);

        aircraft.setClimbRate(1);
        aircraft.setHeading(0); // N/A => no prediction possible

        world.getTargetCoordinates(aircraft, TIME, mCoordinates);
        assertCoordinates(mCoordinates, x, y, z);

        world.getTargetCoordinates(aircraft, TIME + 1000, mCoordinates);
        assertCoordinates(mCoordinates, x, y, z);

        // TODO: test a receiver (it shouldn't move).
    }

    @Test
    public void testRightTurn() {
        final Location location = new Location("test");
        location.setLatitude(49);
        location.setLongitude(7);
        location.setAltitude(350);

        final World world = new World();
        world.setPosition(location);
        world.setGeoidHeight(48.6);
        world.setLocationPredictionEnabled(true);

        Assert.assertTrue(world.isLocationPredictionEnabled());

        final Aircraft aircraft = world.addAircraft(
                "ICA000001", 0x00000000, 49.1, 7.1, 1400, TIME);
        aircraft.setGroundSpeed(720); // 720 km/h = 200 m/s
        aircraft.setClimbRate(1); // 1 m/s
        aircraft.setTurnRate(48); // 48 deg/s
        aircraft.setHeading(90); // East

        final double x = 7303.2158;
        final double y = 11131.9492;
        final double z = 1098.6;

        world.getTargetCoordinates(aircraft, TIME, mCoordinates);
        assertCoordinates(mCoordinates, x, y, z);

        world.getTargetCoordinates(aircraft, TIME + 1000, mCoordinates);
        assertCoordinates(mCoordinates, x + 177.4126, y - 78.9892, z + 1.0);

        world.getTargetCoordinates(aircraft, TIME + 2000, mCoordinates);
        assertCoordinates(mCoordinates, x + 237.4248, y - 263.6865, z + 2.0);

        world.getTargetCoordinates(aircraft, TIME + 3000, mCoordinates);
        assertCoordinates(mCoordinates, x + 140.3233, y - 431.8711, z + 3.0);

        world.getTargetCoordinates(aircraft, TIME + 4000, mCoordinates);
        assertCoordinates(mCoordinates, x - 49.6352, y - 472.2480, z + 4.0);

        world.getTargetCoordinates(aircraft, TIME + 5000, mCoordinates);
        assertCoordinates(mCoordinates, x - 206.7485, y - 358.0986, z + 5.0);

        world.getTargetCoordinates(aircraft, TIME + 6000, mCoordinates);
        assertCoordinates(mCoordinates, x - 227.0478, y - 164.9599, z + 6.0);

        world.getTargetCoordinates(aircraft, TIME + 7000, mCoordinates);
        assertCoordinates(mCoordinates, x - 97.1011, y - 20.6396, z + 7.0);

        world.getTargetCoordinates(aircraft, TIME + 8000, mCoordinates);
        assertCoordinates(mCoordinates, x + 97.1011, y - 20.6396, z + 8.0);

        world.getTargetCoordinates(aircraft, TIME + 9000, mCoordinates);
        assertCoordinates(mCoordinates, x + 227.0478, y - 164.9599, z + 9.0);

        for (int i = 10000; i <= 20000; i += 500) {
            world.getTargetCoordinates(aircraft, TIME + i, mCoordinates);
            assertCoordinates(mCoordinates, x + 206.7486, y - 358.0986, z + 10.0);
        }

        aircraft.setHeading(180); // South

        world.getTargetCoordinates(aircraft, TIME, mCoordinates);
        assertCoordinates(mCoordinates, x, y, z);

        world.getTargetCoordinates(aircraft, TIME + 1000, mCoordinates);
        assertCoordinates(mCoordinates, x - 78.9892, y - 177.4131, z + 1.0);

        world.getTargetCoordinates(aircraft, TIME + 2000, mCoordinates);
        assertCoordinates(mCoordinates, x - 263.6865, y - 237.4248, z + 2.0);

        world.getTargetCoordinates(aircraft, TIME + 3000, mCoordinates);
        assertCoordinates(mCoordinates, x - 431.8711, y - 140.3232, z + 3.0);

        world.getTargetCoordinates(aircraft, TIME + 4000, mCoordinates);
        assertCoordinates(mCoordinates, x - 472.2480, y + 49.6358, z + 4.0);

        world.getTargetCoordinates(aircraft, TIME + 5000, mCoordinates);
        assertCoordinates(mCoordinates, x - 358.0986, y + 206.7481, z + 5.0);

        world.getTargetCoordinates(aircraft, TIME + 6000, mCoordinates);
        assertCoordinates(mCoordinates, x - 164.9599, y + 227.0479, z + 6.0);

        world.getTargetCoordinates(aircraft, TIME + 7000, mCoordinates);
        assertCoordinates(mCoordinates, x - 20.6396, y + 97.1016, z + 7.0);

        world.getTargetCoordinates(aircraft, TIME + 8000, mCoordinates);
        assertCoordinates(mCoordinates, x - 20.6396, y - 97.1015, z + 8.0);

        world.getTargetCoordinates(aircraft, TIME + 9000, mCoordinates);
        assertCoordinates(mCoordinates, x - 164.9599, y - 227.0478, z + 9.0);

        for (int i = 10000; i <= 20000; i += 500) {
            world.getTargetCoordinates(aircraft, TIME + i, mCoordinates);
            assertCoordinates(mCoordinates, x - 358.0986, y - 206.7481, z + 10.0);
        }

        aircraft.setHeading(270); // West

        world.getTargetCoordinates(aircraft, TIME, mCoordinates);
        assertCoordinates(mCoordinates, x, y, z);

        world.getTargetCoordinates(aircraft, TIME + 1000, mCoordinates);
        assertCoordinates(mCoordinates, x - 177.4126, y + 78.9893, z + 1.0);

        world.getTargetCoordinates(aircraft, TIME + 2000, mCoordinates);
        assertCoordinates(mCoordinates, x - 237.4248, y + 263.6865, z + 2.0);

        world.getTargetCoordinates(aircraft, TIME + 3000, mCoordinates);
        assertCoordinates(mCoordinates, x - 140.3232, y + 431.8711, z + 3.0);

        world.getTargetCoordinates(aircraft, TIME + 4000, mCoordinates);
        assertCoordinates(mCoordinates, x + 49.6353, y + 472.2481, z + 4.0);

        world.getTargetCoordinates(aircraft, TIME + 5000, mCoordinates);
        assertCoordinates(mCoordinates, x + 206.7486, y + 358.0987, z + 5.0);

        world.getTargetCoordinates(aircraft, TIME + 6000, mCoordinates);
        assertCoordinates(mCoordinates, x + 227.0478, y + 164.9599, z + 6.0);

        world.getTargetCoordinates(aircraft, TIME + 7000, mCoordinates);
        assertCoordinates(mCoordinates, x + 97.1011, y + 20.6397, z + 7.0);

        world.getTargetCoordinates(aircraft, TIME + 8000, mCoordinates);
        assertCoordinates(mCoordinates, x - 97.1011, y + 20.6397, z + 8.0);

        world.getTargetCoordinates(aircraft, TIME + 9000, mCoordinates);
        assertCoordinates(mCoordinates, x - 227.0478, y + 164.9600, z + 9.0);

        for (int i = 10000; i <= 20000; i += 500) {
            world.getTargetCoordinates(aircraft, TIME + i, mCoordinates);
            assertCoordinates(mCoordinates, x - 206.7485, y + 358.0987, z + 10.0);
        }

        aircraft.setHeading(360); // North

        world.getTargetCoordinates(aircraft, TIME, mCoordinates);
        assertCoordinates(mCoordinates, x, y, z);

        world.getTargetCoordinates(aircraft, TIME + 1000, mCoordinates);
        assertCoordinates(mCoordinates, x + 78.9893, y + 177.4131, z + 1.0);

        world.getTargetCoordinates(aircraft, TIME + 2000, mCoordinates);
        assertCoordinates(mCoordinates, x + 263.6865, y + 237.4248, z + 2.0);

        world.getTargetCoordinates(aircraft, TIME + 3000, mCoordinates);
        assertCoordinates(mCoordinates, x + 431.8711, y + 140.3233, z + 3.0);

        world.getTargetCoordinates(aircraft, TIME + 4000, mCoordinates);
        assertCoordinates(mCoordinates, x + 472.2481, y - 49.6357, z + 4.0);

        world.getTargetCoordinates(aircraft, TIME + 5000, mCoordinates);
        assertCoordinates(mCoordinates, x + 358.0987, y - 206.7480, z + 5.0);

        world.getTargetCoordinates(aircraft, TIME + 6000, mCoordinates);
        assertCoordinates(mCoordinates, x + 164.9600, y - 227.0478, z + 6.0);

        world.getTargetCoordinates(aircraft, TIME + 7000, mCoordinates);
        assertCoordinates(mCoordinates, x + 20.6397, y - 97.1015, z + 7.0);

        world.getTargetCoordinates(aircraft, TIME + 8000, mCoordinates);
        assertCoordinates(mCoordinates, x + 20.6397, y + 97.1016, z + 8.0);

        world.getTargetCoordinates(aircraft, TIME + 9000, mCoordinates);
        assertCoordinates(mCoordinates, x + 164.96, y + 227.0479, z + 9.0);

        for (int i = 10000; i <= 20000; i += 500) {
            world.getTargetCoordinates(aircraft, TIME + i, mCoordinates);
            assertCoordinates(mCoordinates, x + 358.0987, y + 206.7481, z + 10.0);
        }
    }

    @Test
    public void testLeftTurn() {
        final Location location = new Location("test");
        location.setLatitude(49);
        location.setLongitude(7);
        location.setAltitude(350);

        final World world = new World();
        world.setPosition(location);
        world.setGeoidHeight(48.6);
        world.setLocationPredictionEnabled(true);

        Assert.assertTrue(world.isLocationPredictionEnabled());

        final Aircraft aircraft = world.addAircraft(
                "ICA000001", 0x00000000, 49.1, 7.1, 1400, TIME);
        aircraft.setGroundSpeed(720); // 720 km/h = 200 m/s
        aircraft.setClimbRate(-1); // -1 m/s
        aircraft.setTurnRate(-48); // -48 deg/s
        aircraft.setHeading(90); // East

        final double x = 7303.2158;
        final double y = 11131.9492;
        final double z = 1098.6;

        world.getTargetCoordinates(aircraft, TIME, mCoordinates);
        assertCoordinates(mCoordinates, x, y, z);

        world.getTargetCoordinates(aircraft, TIME + 1000, mCoordinates);
        assertCoordinates(mCoordinates, x + 177.4126, y + 78.9893, z - 1.0);

        world.getTargetCoordinates(aircraft, TIME + 2000, mCoordinates);
        assertCoordinates(mCoordinates, x + 237.4248, y + 263.6865, z - 2.0);

        world.getTargetCoordinates(aircraft, TIME + 3000, mCoordinates);
        assertCoordinates(mCoordinates, x + 140.3233, y + 431.8711, z - 3.0);

        world.getTargetCoordinates(aircraft, TIME + 4000, mCoordinates);
        assertCoordinates(mCoordinates, x - 49.6352, y + 472.2481, z - 4.0);

        world.getTargetCoordinates(aircraft, TIME + 5000, mCoordinates);
        assertCoordinates(mCoordinates, x - 206.7485, y + 358.0987, z - 5.0);

        world.getTargetCoordinates(aircraft, TIME + 6000, mCoordinates);
        assertCoordinates(mCoordinates, x - 227.0478, y + 164.9600, z - 6.0);

        world.getTargetCoordinates(aircraft, TIME + 7000, mCoordinates);
        assertCoordinates(mCoordinates, x - 97.1011, y + 20.6397, z - 7.0);

        world.getTargetCoordinates(aircraft, TIME + 8000, mCoordinates);
        assertCoordinates(mCoordinates, x + 97.1011, y + 20.6397, z - 8.0);

        world.getTargetCoordinates(aircraft, TIME + 9000, mCoordinates);
        assertCoordinates(mCoordinates, x + 227.0479, y + 164.9600, z - 9.0);

        for (int i = 10000; i <= 20000; i += 500) {
            world.getTargetCoordinates(aircraft, TIME + i, mCoordinates);
            assertCoordinates(mCoordinates, x + 206.7486, y + 358.0987, z - 10.0);
        }

        aircraft.setHeading(180); // South

        world.getTargetCoordinates(aircraft, TIME, mCoordinates);
        assertCoordinates(mCoordinates, x, y, z);

        world.getTargetCoordinates(aircraft, TIME + 1000, mCoordinates);
        assertCoordinates(mCoordinates, x + 78.9893, y - 177.4131, z - 1.0);

        world.getTargetCoordinates(aircraft, TIME + 2000, mCoordinates);
        assertCoordinates(mCoordinates, x + 263.6865, y - 237.4248, z - 2.0);

        world.getTargetCoordinates(aircraft, TIME + 3000, mCoordinates);
        assertCoordinates(mCoordinates, x + 431.8711, y - 140.3233, z - 3.0);

        world.getTargetCoordinates(aircraft, TIME + 4000, mCoordinates);
        assertCoordinates(mCoordinates, x + 472.2481, y + 49.6358, z - 4.0);

        world.getTargetCoordinates(aircraft, TIME + 5000, mCoordinates);
        assertCoordinates(mCoordinates, x + 358.0987, y + 206.7481, z - 5.0);

        world.getTargetCoordinates(aircraft, TIME + 6000, mCoordinates);
        assertCoordinates(mCoordinates, x + 164.9600, y + 227.0479, z - 6.0);

        world.getTargetCoordinates(aircraft, TIME + 7000, mCoordinates);
        assertCoordinates(mCoordinates, x + 20.6397, y + 97.1016, z - 7.0);

        world.getTargetCoordinates(aircraft, TIME + 8000, mCoordinates);
        assertCoordinates(mCoordinates, x + 20.6397, y - 97.1015, z - 8.0);

        world.getTargetCoordinates(aircraft, TIME + 9000, mCoordinates);
        assertCoordinates(mCoordinates, x + 164.9600, y - 227.0478, z - 9.0);

        for (int i = 10000; i <= 20000; i += 500) {
            world.getTargetCoordinates(aircraft, TIME + i, mCoordinates);
            assertCoordinates(mCoordinates, x + 358.0987, y - 206.7480, z - 10.0);
        }

        aircraft.setHeading(270); // West

        world.getTargetCoordinates(aircraft, TIME, mCoordinates);
        assertCoordinates(mCoordinates, x, y, z);

        world.getTargetCoordinates(aircraft, TIME + 1000, mCoordinates);
        assertCoordinates(mCoordinates, x - 177.4126, y - 78.9892, z - 1.0);

        world.getTargetCoordinates(aircraft, TIME + 2000, mCoordinates);
        assertCoordinates(mCoordinates, x - 237.4248, y - 263.6865, z - 2.0);

        world.getTargetCoordinates(aircraft, TIME + 3000, mCoordinates);
        assertCoordinates(mCoordinates, x - 140.3233, y - 431.8711, z - 3.0);

        world.getTargetCoordinates(aircraft, TIME + 4000, mCoordinates);
        assertCoordinates(mCoordinates, x + 49.6352, y - 472.2480, z - 4.0);

        world.getTargetCoordinates(aircraft, TIME + 5000, mCoordinates);
        assertCoordinates(mCoordinates, x + 206.7486, y - 358.0986, z - 5.0);

        world.getTargetCoordinates(aircraft, TIME + 6000, mCoordinates);
        assertCoordinates(mCoordinates, x + 227.0479, y - 164.9599, z - 6.0);

        world.getTargetCoordinates(aircraft, TIME + 7000, mCoordinates);
        assertCoordinates(mCoordinates, x + 97.1011, y - 20.6396, z - 7.0);

        world.getTargetCoordinates(aircraft, TIME + 8000, mCoordinates);
        assertCoordinates(mCoordinates, x - 97.1011, y - 20.6396, z - 8.0);

        world.getTargetCoordinates(aircraft, TIME + 9000, mCoordinates);
        assertCoordinates(mCoordinates, x - 227.0478, y - 164.9599, z - 9.0);

        for (int i = 10000; i <= 20000; i += 500) {
            world.getTargetCoordinates(aircraft, TIME + i, mCoordinates);
            assertCoordinates(mCoordinates, x - 206.7485, y - 358.0986, z - 10.0);
        }

        aircraft.setHeading(360); // North

        world.getTargetCoordinates(aircraft, TIME, mCoordinates);
        assertCoordinates(mCoordinates, x, y, z);

        world.getTargetCoordinates(aircraft, TIME + 1000, mCoordinates);
        assertCoordinates(mCoordinates, x - 78.9893, y + 177.4131, z - 1.0);

        world.getTargetCoordinates(aircraft, TIME + 2000, mCoordinates);
        assertCoordinates(mCoordinates, x - 263.6865, y + 237.4248, z - 2.0);

        world.getTargetCoordinates(aircraft, TIME + 3000, mCoordinates);
        assertCoordinates(mCoordinates, x - 431.8711, y + 140.3233, z - 3.0);

        world.getTargetCoordinates(aircraft, TIME + 4000, mCoordinates);
        assertCoordinates(mCoordinates, x - 472.2481, y - 49.6358, z - 4.0);

        world.getTargetCoordinates(aircraft, TIME + 5000, mCoordinates);
        assertCoordinates(mCoordinates, x - 358.0987, y - 206.7481, z - 5.0);

        world.getTargetCoordinates(aircraft, TIME + 6000, mCoordinates);
        assertCoordinates(mCoordinates, x - 164.9599, y - 227.0478, z - 6.0);

        world.getTargetCoordinates(aircraft, TIME + 7000, mCoordinates);
        assertCoordinates(mCoordinates, x - 20.6396, y - 97.1015, z - 7.0);

        world.getTargetCoordinates(aircraft, TIME + 8000, mCoordinates);
        assertCoordinates(mCoordinates, x - 20.6396, y + 97.1016, z - 8.0);

        world.getTargetCoordinates(aircraft, TIME + 9000, mCoordinates);
        assertCoordinates(mCoordinates, x - 164.9599, y + 227.0479, z - 9.0);

        for (int i = 10000; i <= 20000; i += 500) {
            world.getTargetCoordinates(aircraft, TIME + i, mCoordinates);
            assertCoordinates(mCoordinates, x - 358.0987, y + 206.7481, z - 10.0);
        }
    }

    @Test
    public void testNoPrediction() {
        final Location location = new Location("test");
        location.setLatitude(49);
        location.setLongitude(7);
        location.setAltitude(350);

        final World world = new World();
        world.setGeoidHeight(48.6);
        world.setPosition(location);

        Assert.assertFalse(world.isLocationPredictionEnabled());

        final Aircraft aircraft = world.addAircraft(
                "ICA000001", 0x00000000, 49.1, 7.1, 1400, TIME);
        aircraft.setGroundSpeed(360); // 360 km/h = 100 m/s
        aircraft.setClimbRate(1); // 1 m/s
        aircraft.setTurnRate(6); // 6 deg/s
        aircraft.setHeading(90); // East

        for (int i = 0; i < 6000; i += 100) {
            world.getTargetCoordinates(aircraft, TIME + i, mCoordinates);
            assertCoordinates(mCoordinates, 7303.2158, 11131.9492, 1098.6);
        }
    }

    public void testTargetList() {
        final World world = new World();

        Target[] targets = new Target[1];
        targets = world.getTargets(targets);
        Assert.assertEquals(1, targets.length);
        Assert.assertNull(targets[0]);

        final Aircraft aircraft1 = world.addAircraft("FLR3EE227", 0x063EE227, 49.1, 7.1, 1350,
                CalibratedClock.currentTimeMillis());
        final Aircraft aircraft2 = world.addAircraft("FLR3D238E", 0x0A3D238E, 48.9, 6.9, 4350,
                CalibratedClock.currentTimeMillis());
        targets = world.getTargets(targets);
        Assert.assertEquals(2, targets.length);
        Assert.assertEquals(aircraft1, targets[0]);
        Assert.assertEquals(aircraft2, targets[1]);

        final Aircraft aircraft3 = world.addAircraft("FLR3FEF7C", 0x0A3FEF7C, 48.9, 6.9, 350,
                CalibratedClock.currentTimeMillis());
        final Receiver receiver = world.addReceiver(
                "TEST", 49.1, 7.1, 350, CalibratedClock.currentTimeMillis());
        targets = world.getTargets(targets);
        Assert.assertEquals(4, targets.length);
        Assert.assertEquals(aircraft1, targets[0]);
        Assert.assertEquals(aircraft2, targets[1]);
        Assert.assertEquals(aircraft3, targets[2]);
        Assert.assertEquals(receiver, targets[3]);

        // TODO: move the clock and test the targets are removed.
    }

    private void assertCoordinates(float[] coordinates, double x, double y, double z) {
        Assert.assertEquals(4, coordinates.length);
        Assert.assertEquals(x, coordinates[0], 0.0001);
        Assert.assertEquals(y, coordinates[1], 0.0001);
        Assert.assertEquals(z, coordinates[2], 0.0001);
        Assert.assertEquals(1.0, coordinates[3], 0.0001);
    }
}
