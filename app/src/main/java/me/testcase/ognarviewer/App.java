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

package me.testcase.ognarviewer;

import android.app.Application;
import android.os.StrictMode;

import me.testcase.ognarviewer.directory.DirectoryRepository;
import me.testcase.ognarviewer.directory.PrivateDirectory;
import me.testcase.ognarviewer.directory.PublicDirectory;

public class App extends Application {
    private static DirectoryRepository sDirectoryRepository;

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                    .detectAll()
                    .penaltyLog()
                    .build());
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }

        Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler(this,
                Thread.getDefaultUncaughtExceptionHandler()));

        final PublicDirectory publicDirectory = new PublicDirectory(this);
        final PrivateDirectory privateDirectory = new PrivateDirectory(this);
        sDirectoryRepository = new DirectoryRepository(publicDirectory, privateDirectory);
    }

    // TODO: use dependency injection.
    public static DirectoryRepository getDirectoryRepository() {
        return sDirectoryRepository;
    }
}
