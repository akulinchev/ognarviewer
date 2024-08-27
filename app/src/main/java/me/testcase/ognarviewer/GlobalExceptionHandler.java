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

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final Context mApplicationContext;
    private final Thread.UncaughtExceptionHandler mDefaultHandler;

    public GlobalExceptionHandler(Context applicationContext,
                                  Thread.UncaughtExceptionHandler defaultHandler) {
        mApplicationContext = applicationContext;
        mDefaultHandler = defaultHandler;
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        try {
            final StringBuilder builder = new StringBuilder();
            while (e != null) {
                builder.append("Exception: ");
                builder.append(e.toString());
                builder.append("\n\nMessage: ");
                builder.append(e.getMessage());
                builder.append("\n\nBacktrace:\n");
                for (StackTraceElement el : e.getStackTrace()) {
                    builder.append(el.toString());
                    builder.append('\n');
                }
                builder.append('\n');
                e = e.getCause();
            }
            final Intent intent = new Intent(mApplicationContext, CrashActivity.class);
            intent.putExtra("backtrace", builder.toString());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mApplicationContext.startActivity(intent);
            System.exit(0);
        } catch (Throwable e2) {
            mDefaultHandler.uncaughtException(t, e2);
        }
    }
}
