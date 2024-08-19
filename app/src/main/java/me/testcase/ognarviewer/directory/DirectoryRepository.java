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

import android.net.Uri;

import androidx.annotation.NonNull;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class DirectoryRepository {
    private final PublicDirectory mPublicDirectory;
    private final PrivateDirectory mPrivateDirectory;

    public DirectoryRepository(PublicDirectory publicDirectory, PrivateDirectory privateDirectory) {
        mPublicDirectory = publicDirectory;
        mPrivateDirectory = privateDirectory;
    }

    public List<DirectoryEntry> list() {
        final List<DirectoryEntry> sorted = new ArrayList<>(mPrivateDirectory.list());
        sorted.sort((entry1, entry2) -> {
            final String registration1 = entry1.getRegistration();
            if (registration1 == null) {
                return 1;
            }
            final String registration2 = entry2.getRegistration();
            if (registration2 == null) {
                return -1;
            }
            return registration1.compareTo(registration2);
        });
        return sorted;
    }

    public DirectoryEntry find(long id) {
        final DirectoryEntry entry = mPrivateDirectory.find(id);
        if (entry != null) {
            return entry;
        }
        return mPublicDirectory.find(id);
    }

    public void update(DirectoryEntry entry) {
        mPrivateDirectory.update(entry);
        mPrivateDirectory.save();
    }

    public void update(Collection<DirectoryEntry> entries) {
        for (DirectoryEntry entry : entries) {
            mPrivateDirectory.update(entry);
        }
        mPrivateDirectory.save();
    }

    public void delete(Collection<Long> ids) {
        for (long id : ids) {
            mPrivateDirectory.delete(id);
        }
        mPrivateDirectory.save();
    }

    public void deleteAll() {
        mPrivateDirectory.nuke();
    }

    public List<DirectoryEntry> filter(String query) {
        final String queryLower = query.toLowerCase(Locale.getDefault());
        final List<DirectoryEntry> filtered = new ArrayList<>();
        for (DirectoryEntry entry : mPrivateDirectory.list()) {
            final String registration = entry.getRegistration();
            if (registration == null) {
                continue;
            }
            if (registration.toLowerCase(Locale.getDefault()).contains(queryLower)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    public void importJson(@NonNull Uri uri) throws IOException, JSONException {
        mPrivateDirectory.importJson(uri);
    }

    public void exportJson(@NonNull Uri uri) throws IOException, JSONException {
        mPrivateDirectory.exportJson(uri);
    }
}
