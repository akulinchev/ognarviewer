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

public class DirectoryEntry {
    private long mId;
    private String mModel;
    private String mRegistration;
    private String mCompetitionNumber;
    private String mBaseAirfield;
    private String mOwner;

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        mId = id;
    }

    public String getRegistration() {
        return mRegistration;
    }

    public void setRegistration(String registration) {
        mRegistration = processString(registration);
    }

    public String getModel() {
        return mModel;
    }

    public void setModel(String model) {
        mModel = processString(model);
    }

    public String getOwner() {
        return mOwner;
    }

    public void setOwner(String owner) {
        mOwner = processString(owner);
    }

    public String getBaseAirfield() {
        return mBaseAirfield;
    }

    public void setBaseAirfield(String airfield) {
        mBaseAirfield = processString(airfield);
    }

    public String getCompetitionNumber() {
        return mCompetitionNumber;
    }

    public void setCompetitionNumber(String cn) {
        mCompetitionNumber = processString(cn);
    }

    public boolean isEmpty() {
        if (mModel != null) {
            return false;
        }
        if (mRegistration != null) {
            return false;
        }
        if (mCompetitionNumber != null) {
            return false;
        }
        if (mBaseAirfield != null) {
            return false;
        }
        return mOwner == null;
    }

    private String processString(String string) {
        if (string == null) {
            return null;
        }
        final String trimmed = string.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }
}
