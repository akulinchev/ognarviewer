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

import android.content.Context;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import me.testcase.ognarviewer.R;

public class PublicDirectory {
    private static final String TAG = "PublicDirectory";

    private final Map<Long, DirectoryEntry> mEntries = new HashMap<>();

    private long mAccessTime;

    public PublicDirectory(Context context) {
        try (final InputStream inputStream =
                     context.getResources().openRawResource(R.raw.ogn_ddb)) {
            final DataInputStream dataInputStream = new DataInputStream(inputStream);
            mAccessTime = dataInputStream.readLong();
            while (dataInputStream.available() > 0) {
                final long id = dataInputStream.readInt();
                final String model = dataInputStream.readUTF();
                final String registration = dataInputStream.readUTF();
                final String competitionNumber = dataInputStream.readUTF();

                final DirectoryEntry entry = new DirectoryEntry();
                entry.setId(id);
                entry.setModel(model);
                entry.setRegistration(registration);
                entry.setCompetitionNumber(competitionNumber);
                mEntries.put(id, entry);
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }

        Log.v(TAG, String.format("Loaded %d built-in entries", mEntries.size()));
    }

    public DirectoryEntry find(long id) {
        return mEntries.get(id);
    }

    public int size() {
        return mEntries.size();
    }

    public long getAccessTime() {
        return mAccessTime;
    }
}
