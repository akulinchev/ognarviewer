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
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PublicDirectoryTest {
    @Test
    public void testSize() {
        final PublicDirectory db = new PublicDirectory(RuntimeEnvironment.getApplication());
        Assert.assertTrue(db.size() > 30000);
    }

    @Test
    public void testWellKnown() {
        final PublicDirectory db = new PublicDirectory(RuntimeEnvironment.getApplication());

        final DirectoryEntry duoDiscus = db.find(0x023EE227);
        Assert.assertEquals("Duo Discus", duoDiscus.getModel());
        Assert.assertEquals("D-0400", duoDiscus.getRegistration());
        Assert.assertEquals("6V", duoDiscus.getCompetitionNumber());

        final DirectoryEntry dr400 = db.find(0x023D238E);
        Assert.assertEquals("DR-400", dr400.getModel());
        Assert.assertEquals("D-ELSG", dr400.getRegistration());
        Assert.assertEquals("SG", dr400.getCompetitionNumber());

        final DirectoryEntry ct2k = db.find(0x023FEF7C);
        Assert.assertEquals("Flight Design CTSW", ct2k.getModel());
        Assert.assertEquals("D-MSNG", ct2k.getRegistration());
        Assert.assertEquals("NG", ct2k.getCompetitionNumber());
    }

    @Test
    public void testPartial() {
        final PublicDirectory db = new PublicDirectory(RuntimeEnvironment.getApplication());

        final DirectoryEntry entity1 = db.find(0x03001549);
        Assert.assertEquals("Paraglider", entity1.getModel());
        Assert.assertEquals("36445", entity1.getRegistration());
        Assert.assertNull(entity1.getCompetitionNumber());

        final DirectoryEntry entity2 = db.find(0x02002047);
        Assert.assertEquals("Flight Design CTSW", entity2.getModel());
        Assert.assertEquals("G-RILA", entity2.getRegistration());
        Assert.assertNull(entity2.getCompetitionNumber());

        final DirectoryEntry entity3 = db.find(0x02111C19);
        Assert.assertEquals("Paraglider", entity3.getModel());
        Assert.assertNull(entity3.getRegistration());
        Assert.assertEquals("UdH", entity3.getCompetitionNumber());
    }

    @Test
    public void testNotFound() {
        final PublicDirectory db = new PublicDirectory(RuntimeEnvironment.getApplication());
        Assert.assertNull(db.find(0x01234567));
        Assert.assertNull(db.find(0x02345678));
        Assert.assertNull(db.find(0x03456789));

        // Exist on the website, but don't want to be tracked.
        Assert.assertNull(db.find(0x013D117A));
        Assert.assertNull(db.find(0x0239265B));
        Assert.assertNull(db.find(0x033579B6));
    }
}
