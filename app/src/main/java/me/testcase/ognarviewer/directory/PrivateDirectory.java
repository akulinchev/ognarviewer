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
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrivateDirectory {
    private static final String TAG = "PrivateDirectory";
    private static final String FILENAME = "directory.bin";
    private static final int VERSION = 1;

    private final Context mContext;
    private final Map<Long, DirectoryEntry> mEntries = new HashMap<>();

    public PrivateDirectory(Context context) {
        mContext = context;

        if (!context.getFileStreamPath(FILENAME).exists()) {
            return;
        }

        try (final InputStream inputStream = context.openFileInput(FILENAME)) {
            final DataInputStream dataInputStream = new DataInputStream(inputStream);

            final int version = dataInputStream.readInt();
            if (version > VERSION) {
                Log.e(TAG, "Unknown DB version; ignoring!");
                return;
            }

            while (dataInputStream.available() > 0) {
                final long id = dataInputStream.readInt();
                final String model = dataInputStream.readUTF();
                final String registration = dataInputStream.readUTF();
                final String competitionNumber = dataInputStream.readUTF();
                final String baseAirfield = dataInputStream.readUTF();
                final String owner = dataInputStream.readUTF();

                final DirectoryEntry entry = new DirectoryEntry();
                entry.setId(id);
                entry.setModel(model);
                entry.setRegistration(registration);
                entry.setCompetitionNumber(competitionNumber);
                entry.setBaseAirfield(baseAirfield);
                entry.setOwner(owner);
                mEntries.put(id, entry);
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    public void save() {
        // TODO: save to directory.bin~ first, then rename to directory.bin.
        try (OutputStream outputStream = mContext.openFileOutput(FILENAME, 0)) {
            final DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            dataOutputStream.writeInt(VERSION);
            for (DirectoryEntry entry : mEntries.values()) {
                if (entry.isEmpty()) {
                    continue;
                }
                dataOutputStream.writeInt((int) entry.getId());
                dataOutputStream.writeUTF(emptyIfNull(entry.getModel()));
                dataOutputStream.writeUTF(emptyIfNull(entry.getRegistration()));
                dataOutputStream.writeUTF(emptyIfNull(entry.getCompetitionNumber()));
                dataOutputStream.writeUTF(emptyIfNull(entry.getBaseAirfield()));
                dataOutputStream.writeUTF(emptyIfNull(entry.getOwner()));
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    public Collection<DirectoryEntry> list() {
        return mEntries.values();
    }

    public DirectoryEntry find(long id) {
        return mEntries.get(id);
    }

    public void update(DirectoryEntry entity) {
        if (entity.isEmpty()) {
            delete(entity.getId());
        } else {
            mEntries.put(entity.getId(), entity);
        }
    }

    public void delete(long id) {
        mEntries.remove(id);
    }

    public void nuke() {
        mEntries.clear();
        mContext.deleteFile(FILENAME);
    }

    public int size() {
        return mEntries.size();
    }

    public void importJson(@NonNull Uri uri) throws IOException, JSONException {
        final List<DirectoryEntry> entries = new ArrayList<>();
        final InputStream stream = mContext.getContentResolver().openInputStream(uri);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            while (true) {
                final String line = reader.readLine();
                if (line == null) {
                    break;
                }
                final JSONObject object = new JSONObject(line);
                final DirectoryEntry entry = new DirectoryEntry();
                if (!object.has("id")) {
                    continue; // Invalid
                }
                entry.setId(object.getInt("id"));
                if (object.has("registration")) {
                    entry.setRegistration(object.getString("registration"));
                }
                if (object.has("model")) {
                    entry.setModel(object.getString("model"));
                }
                if (object.has("owner")) {
                    entry.setOwner(object.getString("owner"));
                }
                if (object.has("home")) {
                    entry.setBaseAirfield(object.getString("home"));
                }
                if (object.has("cn")) {
                    entry.setCompetitionNumber(object.getString("cn"));
                }
                if (entry.isEmpty()) {
                    continue;
                }
                // Delay update(), import either the whole file or nothing.
                entries.add(entry);
            }
        }
        for (DirectoryEntry entry : entries) {
            // FIXME: what to do when the entry already exists?
            update(entry);
        }
        save();
    }

    public void exportJson(@NonNull Uri uri) throws IOException, JSONException {
        final OutputStream stream = mContext.getContentResolver().openOutputStream(uri);
        try (PrintWriter writer = new PrintWriter(stream)) {
            for (DirectoryEntry entry : mEntries.values()) {
                if (entry.isEmpty()) {
                    continue;
                }
                final JSONObject object = new JSONObject();
                object.put("id", entry.getId());
                if (entry.getRegistration() != null) {
                    object.put("registration", entry.getRegistration());
                }
                if (entry.getModel() != null) {
                    object.put("model", entry.getModel());
                }
                if (entry.getOwner() != null) {
                    object.put("owner", entry.getOwner());
                }
                if (entry.getBaseAirfield() != null) {
                    object.put("home", entry.getBaseAirfield());
                }
                if (entry.getCompetitionNumber() != null) {
                    object.put("cn", entry.getCompetitionNumber());
                }
                writer.println(object);
            }
        }
    }

    private String emptyIfNull(String string) {
        if (string == null) {
            return "";
        }
        return string;
    }
}
