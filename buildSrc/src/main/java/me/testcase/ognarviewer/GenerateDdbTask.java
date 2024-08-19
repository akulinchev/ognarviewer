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

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GenerateDdbTask extends DefaultTask {
    @OutputDirectory
    public String getOutputDirectory() {
        return getProject().getLayout().getBuildDirectory().get() + "/generated/res/ddb";
    }

    @TaskAction
    public void generate() throws IOException {
        final File rawResourceDir = new File(getOutputDirectory(), "raw");
        if (!rawResourceDir.exists() && !rawResourceDir.mkdirs()) {
            throw new RuntimeException("Cannot create " + rawResourceDir);
        }

        final File ddbFile = new File(rawResourceDir, "ogn_ddb");
        if (ddbFile.exists()) {
            getLogger().info("Not downloading the OGN DDB again, because it already exists.");
            getLogger().info("Delete it to re-download.");
            return;
        }

        final File valuesResourceDir = new File(getOutputDirectory(), "values");
        if (!valuesResourceDir.exists() && !valuesResourceDir.mkdirs()) {
            throw new RuntimeException("Cannot create " + valuesResourceDir);
        }

        final long now = System.currentTimeMillis();
        final URL url = new URL("https://ddb.glidernet.org/download/");
        try (InputStream inputStream = url.openStream()) {
            final DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(ddbFile));

            final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            while (true) {
                final String line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (line.startsWith("#")) {
                    continue;
                }
                final String[] values = line.split(",");

                int id = Integer.parseInt(values[1].substring(1, values[1].length() - 1).trim(), 16);
                switch (values[0]) {
                    case "'I'":
                        id += 0x01000000;
                        break;
                    case "'F'":
                        id += 0x02000000;
                        break;
                    case "'O'":
                        id += 0x03000000;
                        break;
                    default:
                        throw new RuntimeException("Unknown address type " + values[0]);
                }

                final String model = values[2].substring(1, values[2].length() - 1).trim();
                final String registration = values[3].substring(1, values[3].length() - 1).trim();
                final String competitionNumber = values[4].substring(1, values[4].length() - 1).trim();

                if (model.isEmpty() && registration.isEmpty() && competitionNumber.isEmpty()) {
                    continue;
                }

                outputStream.writeInt(id);
                outputStream.writeUTF(model);
                outputStream.writeUTF(registration);
                outputStream.writeUTF(competitionNumber);
            }
            outputStream.close();
        }

        final File stringsFile = new File(valuesResourceDir, "strings.xml");
        try (FileOutputStream stream = new FileOutputStream(stringsFile)) {
            stream.write("<resources>".getBytes(StandardCharsets.UTF_8));
            stream.write("<string name=\"ogn_ddb_access_time\">".getBytes(StandardCharsets.UTF_8));
            stream.write(String.valueOf(now).getBytes(StandardCharsets.UTF_8));
            stream.write("</string>".getBytes(StandardCharsets.UTF_8));
            stream.write("</resources>".getBytes(StandardCharsets.UTF_8));
        }
    }
}
