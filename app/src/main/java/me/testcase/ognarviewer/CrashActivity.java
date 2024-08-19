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

package me.testcase.ognarviewer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class CrashActivity extends AppCompatActivity {
    private static final String TAG = "CrashActivity";

    private final ActivityResultLauncher<Intent> mSendBacktrace = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> finish());

    private String mBacktrace;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, String.format("CrashActivity %h created", this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash);
        final Intent intent = getIntent();
        final Bundle extras = intent.getExtras();
        if (extras != null) {
            mBacktrace = extras.getString("backtrace");
            final TextView textView = findViewById(R.id.backtrace);
            textView.setText(mBacktrace);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_crash, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_email) {
            final Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:ivan.akulinchev@gmail.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "OGN AR Viewer has crashed");
            intent.putExtra(Intent.EXTRA_TEXT, mBacktrace);
            mSendBacktrace.launch(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
