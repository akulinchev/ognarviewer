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

package me.testcase.ognarviewer.directory;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import androidx.appcompat.content.res.AppCompatResources;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import me.testcase.ognarviewer.App;
import me.testcase.ognarviewer.R;

@RunWith(RobolectricTestRunner.class)
public class PrivateDirectoryTest {
    @Test
    public void testBasic() {
        final PrivateDirectory db1 = new PrivateDirectory(RuntimeEnvironment.getApplication());
        Assert.assertEquals(0, db1.size());

        final DirectoryEntry entry1 = new DirectoryEntry();
        entry1.setId(0x01234567);
        entry1.setModel("Model   ");
        entry1.setRegistration("Registration");
        entry1.setCompetitionNumber("  Competition number");
        entry1.setBaseAirfield("  Base airfield   ");
        entry1.setOwner("Owner");

        final DirectoryEntry entry2 = new DirectoryEntry();
        entry2.setId(0x02345678);
        entry2.setModel("Model 2");
        entry2.setRegistration("        ");
        entry2.setCompetitionNumber("");

        final DirectoryEntry entry3 = new DirectoryEntry();
        entry3.setId(0x03456789);

        db1.update(entry1);
        db1.update(entry2);
        db1.update(entry3);

        Assert.assertEquals(2, db1.size());
        Assert.assertTrue(db1.list().contains(entry1));

        Assert.assertEquals("Model", db1.find(0x01234567).getModel());
        Assert.assertEquals("Registration", db1.find(0x01234567).getRegistration());
        Assert.assertEquals("Competition number", db1.find(0x01234567).getCompetitionNumber());
        Assert.assertEquals("Base airfield", db1.find(0x01234567).getBaseAirfield());
        Assert.assertEquals("Owner", db1.find(0x01234567).getOwner());

        Assert.assertEquals("Model 2", db1.find(0x02345678).getModel());
        Assert.assertNull(db1.find(0x02345678).getRegistration());
        Assert.assertNull(db1.find(0x02345678).getCompetitionNumber());
        Assert.assertNull(db1.find(0x02345678).getBaseAirfield());
        Assert.assertNull(db1.find(0x02345678).getOwner());

        Assert.assertNull(db1.find(0x03456789));

        final PrivateDirectory db2 = new PrivateDirectory(RuntimeEnvironment.getApplication());
        Assert.assertEquals(0, db2.size());

        db1.save();

        final PrivateDirectory db3 = new PrivateDirectory(RuntimeEnvironment.getApplication());
        Assert.assertEquals(2, db3.size());

        db3.delete(0x02345678);
        Assert.assertEquals(1, db3.size());
        final List<DirectoryEntry> list = new ArrayList<>(db3.list());
        Assert.assertEquals(0x01234567, list.get(0).getId());

        db3.nuke();
        Assert.assertEquals(0, db3.size());
        Assert.assertTrue(db3.list().isEmpty());
    }

    @Test
    public void testImport() throws IOException, JSONException {
        final PrivateDirectory db = new PrivateDirectory(RuntimeEnvironment.getApplication());

        // Add an entry to make sure it does not disappear on import errors.
        final DirectoryEntry entry1 = new DirectoryEntry();
        entry1.setId(123);
        entry1.setModel("ASK-13");
        db.update(entry1);
        Assert.assertEquals(1, db.size());

        // Try an empty file.
        db.importJson(Uri.fromFile(File.createTempFile("import", ".json")));
        Assert.assertEquals(1, db.size());

        // Try an invalid path.
        Assert.assertThrows(FileNotFoundException.class, () -> db.importJson(Uri.parse("file:///root/foobar")));
        Assert.assertEquals(1, db.size());

        // Try some rubbish.
        final File randomFile = File.createTempFile("import", ".json");
        randomFile.deleteOnExit();
        try (FileOutputStream stream = new FileOutputStream(randomFile)) {
            final Random random = new Random();
            final byte[] data = new byte[1024];
            random.nextBytes(data);
            stream.write(data);
        }
        Assert.assertThrows(JSONException.class, () -> db.importJson(Uri.fromFile(randomFile)));
        Assert.assertEquals(1, db.size());

        // Try a valid, but empty JSON.
        final File emptyJsonLinesFile = File.createTempFile("import", ".json");
        emptyJsonLinesFile.deleteOnExit();
        try (FileOutputStream stream = new FileOutputStream(emptyJsonLinesFile)) {
            for (int i = 0; i < 42; ++i) {
                stream.write("{}\n".getBytes(StandardCharsets.UTF_8));
            }
        }
        db.importJson(Uri.fromFile(emptyJsonLinesFile));
        Assert.assertEquals(1, db.size());

        // Try a non-empty JSON without IDs.
        final File withoutIdsFile = File.createTempFile("import", ".json");
        withoutIdsFile.deleteOnExit();
        try (FileOutputStream stream = new FileOutputStream(withoutIdsFile)) {
            for (int i = 0; i < 42; ++i) {
                stream.write("{\"model\":\"Cessna 172\",\"owner\":\"not me\"}\n".getBytes(StandardCharsets.UTF_8));
            }
        }
        db.importJson(Uri.fromFile(withoutIdsFile));
        Assert.assertEquals(1, db.size());

        // Try a valid JSON.
        final File validFile = File.createTempFile("import", ".json");
        validFile.deleteOnExit();
        try (FileOutputStream stream = new FileOutputStream(validFile)) {
            stream.write("{\"id\":1,\"model\":\"Cessna 172\",\"owner\":\"not me\"}\n".getBytes(StandardCharsets.UTF_8));
            stream.write("{\"id\":2,\"registration\":\"Registration\",\"model\":\"Model\",\"owner\":\"Owner\",\"home\":\"Home\",\"cn\":\"CN\"}\n".getBytes(StandardCharsets.UTF_8));
        }
        db.importJson(Uri.fromFile(validFile));
        Assert.assertEquals(3, db.size());
        Assert.assertEquals(entry1, db.find(123));
        Assert.assertNull(db.find(1).getRegistration());
        Assert.assertEquals("Cessna 172", db.find(1).getModel());
        Assert.assertEquals("not me", db.find(1).getOwner());
        Assert.assertNull(db.find(1).getBaseAirfield());
        Assert.assertNull(db.find(1).getCompetitionNumber());
        Assert.assertEquals("Registration", db.find(2).getRegistration());
        Assert.assertEquals("Model", db.find(2).getModel());
        Assert.assertEquals("Owner", db.find(2).getOwner());
        Assert.assertEquals("Home", db.find(2).getBaseAirfield());
        Assert.assertEquals("CN", db.find(2).getCompetitionNumber());
    }

    @Test
    public void testExportJson() throws IOException, JSONException {
        final PrivateDirectory db = new PrivateDirectory(RuntimeEnvironment.getApplication());
        final DirectoryEntry entry1 = new DirectoryEntry();
        entry1.setId(123);
        entry1.setModel("ASK-13");
        db.update(entry1);
        final DirectoryEntry entry2 = new DirectoryEntry();
        entry2.setId(321);
        entry2.setRegistration("Registration");
        entry2.setModel("Model");
        entry2.setOwner("Owner");
        entry2.setBaseAirfield("Home");
        entry2.setCompetitionNumber("CN");
        db.update(entry2);
        final File file = File.createTempFile("export", ".json");
        file.deleteOnExit();
        db.exportJson(Uri.fromFile(file));
        try (FileInputStream stream = new FileInputStream(file)) {
            final String string = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            Assert.assertEquals("{\"id\":321,\"registration\":\"Registration\",\"model\":\"Model\",\"owner\":\"Owner\",\"home\":\"Home\",\"cn\":\"CN\"}\n" +
                    "{\"id\":123,\"model\":\"ASK-13\"}\n", string);
        }
    }
}
