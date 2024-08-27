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

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GenerateDdbTask extends DefaultTask {
    private static final SimpleDateFormat HTTP_DATE_FORMAT =
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

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

        final File inFile = getProject()
                .getLayout()
                .getProjectDirectory()
                .file("src/main/registrations/ogn.csv")
                .getAsFile();
        final File outFile = new File(rawResourceDir, "ogn_ddb");

        try (BufferedReader reader = new BufferedReader(new FileReader(inFile))) {
            try (DataOutputStream out = new DataOutputStream(new FileOutputStream(outFile))) {
                while (true) {
                    final String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    if (line.startsWith("#")) {
                        final Date date = HTTP_DATE_FORMAT.parse(line, new ParsePosition(1));
                        if (date != null) {
                            out.writeLong(date.getTime());
                        }
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

                    out.writeInt(id);
                    out.writeUTF(model);
                    out.writeUTF(registration);
                    out.writeUTF(competitionNumber);
                }
            }
        }
    }
}
