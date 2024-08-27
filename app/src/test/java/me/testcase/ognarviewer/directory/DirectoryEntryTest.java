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

package me.testcase.ognarviewer.directory;

import org.junit.Assert;
import org.junit.Test;

public class DirectoryEntryTest {
    @Test
    public void testIsEmpty() {
        final DirectoryEntry entry = new DirectoryEntry();
        Assert.assertTrue(entry.isEmpty());

        entry.setId(0x01234567);
        Assert.assertTrue(entry.isEmpty());

        entry.setModel("             ");
        Assert.assertTrue(entry.isEmpty());
        entry.setModel(".");
        Assert.assertFalse(entry.isEmpty());
        entry.setModel(null);
        Assert.assertTrue(entry.isEmpty());

        entry.setRegistration("      ");
        Assert.assertTrue(entry.isEmpty());
        entry.setRegistration("aaaa");
        Assert.assertFalse(entry.isEmpty());
        entry.setRegistration(null);
        Assert.assertTrue(entry.isEmpty());

        entry.setCompetitionNumber("      ");
        Assert.assertTrue(entry.isEmpty());
        entry.setCompetitionNumber("bbbb");
        Assert.assertFalse(entry.isEmpty());
        entry.setCompetitionNumber(null);
        Assert.assertTrue(entry.isEmpty());

        entry.setBaseAirfield("      ");
        Assert.assertTrue(entry.isEmpty());
        entry.setBaseAirfield("cccc");
        Assert.assertFalse(entry.isEmpty());
        entry.setBaseAirfield(null);
        Assert.assertTrue(entry.isEmpty());

        entry.setOwner("      ");
        Assert.assertTrue(entry.isEmpty());
        entry.setOwner("dddd");
        Assert.assertFalse(entry.isEmpty());
        entry.setOwner(null);
        Assert.assertTrue(entry.isEmpty());
    }

    @Test
    public void testTrim() {
        final DirectoryEntry entry = new DirectoryEntry();
        entry.setId(0x01234567);

        entry.setModel("   Model    ");
        Assert.assertEquals("Model", entry.getModel());

        entry.setRegistration("   regISTRATION    ");
        Assert.assertEquals("regISTRATION", entry.getRegistration());

        entry.setCompetitionNumber("   competition number    ");
        Assert.assertEquals("competition number", entry.getCompetitionNumber());

        entry.setBaseAirfield("   base   Airfield    ");
        Assert.assertEquals("base   Airfield", entry.getBaseAirfield());

        entry.setOwner("   oWNEr    ");
        Assert.assertEquals("oWNEr", entry.getOwner());
    }

    @Test
    public void testNullIfEmpty() {
        final DirectoryEntry entry = new DirectoryEntry();
        entry.setId(0x01234567);

        entry.setModel("     ");
        Assert.assertNull(entry.getModel());

        entry.setRegistration("     ");
        Assert.assertNull(entry.getRegistration());

        entry.setCompetitionNumber("     ");
        Assert.assertNull(entry.getCompetitionNumber());

        entry.setBaseAirfield("     ");
        Assert.assertNull(entry.getBaseAirfield());

        entry.setOwner("     ");
        Assert.assertNull(entry.getOwner());
    }
}
