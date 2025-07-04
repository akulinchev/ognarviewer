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

package me.testcase.ognarviewer.client;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.testcase.ognarviewer.CalibratedClock;
import me.testcase.ognarviewer.utils.UnitsConverter;

public class Parser {
    private static final String TAG = "Parser";

    private static final Pattern AIRCRAFT_LOCATION_RE = Pattern.compile(
            "^(?<callSign>\\w++)>(?:APRS|OGFLR[67]?+|OGADSB),[^:]++:/"
                    + "(?<time>\\d{6})h"
                    + "(?<latDeg>\\d{2})(?<latMin>\\d{2}\\.\\d{2})(?<latNS>[NS])."
                    + "(?<lonDeg>\\d{3})(?<lonMin>\\d{2}\\.\\d{2})(?<lonEW>[EW])."
                    + "(?:(?<heading>\\d{3})/(?<speed>\\d{3}))?+"
                    + "(?:/A=(?<alt>[+-]?+\\d++))?+ "
                    + "(?<extras>.++)$");

    private static final Pattern RE_SPACE = Pattern.compile("\\s++");
    private static final Pattern RE_ID = Pattern.compile("^id([\\dA-Fa-f]{8})$");
    private static final Pattern RE_CLIMB_RATE = Pattern.compile("^([+-]\\d++)fpm$");
    private static final Pattern RE_TURN_RATE = Pattern.compile("^([+-]?+\\d++(?:\\.\\d++)?+)rot$");
    private static final Pattern RE_FLIGHT_LEVEL = Pattern.compile("^FL(\\d++\\.\\d++)$");
    private static final Pattern RE_FINE_LOCATION = Pattern.compile("^!W(?<lat>\\d)(?<lon>\\d)!$");

    private static final Pattern RECEIVER_LOCATION_OLD_RE = Pattern.compile(
            "^(?<callSign>[\\w-]++)>APRS,[^:]++:/"
                    + "(?<time>\\d{6})h"
                    + "(?<latDeg>\\d{2})(?<latMin>\\d{2}\\.\\d{2})(?<latNS>[NS])."
                    + "(?<lonDeg>\\d{3})(?<lonMin>\\d{2}\\.\\d{2})(?<lonEW>[EW])."
                    + "/A=(?<alt>\\d{6})$");

    private static final Pattern RECEIVER_LOCATION_NEW_RE = Pattern.compile(
            "^(?<callSign>[\\w-]++)>OGNSDR,[^:]++:/"
                    + "(?<time>\\d{6})h"
                    + "(?<latDeg>\\d{2})(?<latMin>\\d{2}\\.\\d{2})(?<latNS>[NS])."
                    + "(?<lonDeg>\\d{3})(?<lonMin>\\d{2}\\.\\d{2})(?<lonEW>[EW])."
                    + "/A=(?<alt>\\d{6})");

    private static final Pattern RECEIVER_STATUS_RE = Pattern.compile(
            "^(?<callSign>[\\w-]++)>(?:APRS|OGNSDR),[^:]++:>"
                    + "\\d{6}h "
                    + "v(?<version>[^ ]++) "
                    + "(?:CPU:(?<cpu>\\d++\\.\\d++) )?+"
                    + "(?:RAM:(?<freeRam>\\d++\\.\\d++)/(?<totalRam>\\d++\\.\\d++)MB )?+"
                    + "(?:NTP:(?<ntp>\\d++\\.\\d++)ms/[+-][\\d.]++ppm )?+"
                    + "(?:[\\d.]+V )?"
                    + "(?:[\\d.]+A )?"
                    + "(?:(?<temperature>[+-]\\d++\\.\\d++)C )?");

    private static final String[] UNSUPPORTED = {
            "APRRDZ",
            "APWEE5",
            "APWEE5",
            "FXCAPP",
            "GENERIC",
            "OGADSL",
            "OGAIRM", // airmate
            "OGAPIK",
            "OGCAPT", // capturs
            "OGFLYM", // flymaster
            "OGINRE", // inreach
            "OGLT24",
            "OGNATO",
            "OGNAVI", // naviter
            "OGNDVS",
            "OGNFNO",
            "OGNFNT",
            "OGNMTK", // microtrak
            "OGNPUR",
            "OGNSKY", // safesky
            "OGNSXR", // Stratux (https://stratux.me)
            "OGNTRK",
            "OGNXCG",
            "OGPAW", // pilot aware
            "OGPURTK",
            "OGSKYL", // skylines
            "OGSPID", // spider
            "OGSPOT",
            "SimpleVFR", // EasyVFR (https://easyvfr4.aero)
            "SPOT", // same as OGSPOT above?
    };

    private final BufferedReader mReader;
    private String mCurrentLine;
    private boolean mIsEndOfStream;
    private boolean mNoAltitude;

    public Parser(InputStream stream) {
        mReader = new BufferedReader(new InputStreamReader(stream));
    }

    /**
     * Returns the current line.
     *
     * <p>Useful for logging invalid messages.</p>
     */
    public String getCurrentLine() {
        return mCurrentLine;
    }

    public boolean isEndOfStream() {
        return mIsEndOfStream;
    }

    protected long getCurrentTime() {
        return CalibratedClock.currentTimeMillis();
    }

    @Nullable
    public AprsMessage parse() throws IOException {
        while (true) {
            mNoAltitude = false;
            mCurrentLine = mReader.readLine();
            if (mCurrentLine == null) {
                mIsEndOfStream = true;
                return null;
            }
            if (mCurrentLine.startsWith("#")) {
                continue;
            }
            Matcher m;
            if ((m = AIRCRAFT_LOCATION_RE.matcher(mCurrentLine)).matches()) {
                final AircraftLocationMessage message = parseAircraftLocation(m);
                if (mNoAltitude) {
                    // If parsing failed due to the missing altitude, just skip this message.
                    // We cannot accept such messages, but also don't want to return null, because
                    // it's not the parser fault.
                    continue;
                }
                return message;
            }
            if ((m = RECEIVER_STATUS_RE.matcher(mCurrentLine)).lookingAt()) {
                return parseReceiverStatus(m);
            }
            if ((m = RECEIVER_LOCATION_NEW_RE.matcher(mCurrentLine)).lookingAt()) {
                return parseReceiverLocation(m);
            }
            if ((m = RECEIVER_LOCATION_OLD_RE.matcher(mCurrentLine)).lookingAt()) {
                return parseReceiverLocation(m);
            }
            if (isNotImplementedYet(mCurrentLine)) {
                continue;
            }
            return null;
        }
    }

    private AircraftLocationMessage parseAircraftLocation(Matcher m) {
        final AircraftLocationMessage message = new AircraftLocationMessage();
        message.callSign = m.group("callSign");
        message.timestamp = parseTimestamp(m.group("time"));
        if (message.timestamp == 0) {
            return null;
        }
        message.altitude = parseAltitude(m.group("alt"));
        message.heading = parseHeading(m.group("heading"));
        message.groundSpeed = parseGroundSpeed(m.group("speed"));
        for (final String extra : RE_SPACE.split(m.group("extras"))) {
            Matcher m2;
            if ((m2 = RE_FINE_LOCATION.matcher(extra)).matches()) {
                message.latitude = parseLatitude(m.group("latDeg"), m.group("latMin"), m2.group(
                        "lat"), m.group("latNS"));
                message.longitude = parseLongitude(m.group("lonDeg"), m.group("lonMin"),
                        m2.group("lon"), m.group("lonEW"));
            } else if ((m2 = RE_ID.matcher(extra)).matches()) {
                message.id = parseId(m2.group(1));
            } else if ((m2 = RE_CLIMB_RATE.matcher(extra)).matches()) {
                message.climbRate = parseClimbRate(m2.group(1));
            } else if ((m2 = RE_TURN_RATE.matcher(extra)).matches()) {
                message.turnRate = parseTurnRate(m2.group(1));
            } else if ((m2 = RE_FLIGHT_LEVEL.matcher(extra)).matches()) {
                if (message.altitude == Integer.MIN_VALUE) {
                    message.altitude = parseFlightLevel(m2.group(1));
                }
            }
        }
        if (Double.isNaN(message.latitude) || Double.isNaN(message.longitude)) {
            return null;
        }
        if (message.altitude == Integer.MIN_VALUE) {
            mNoAltitude = true;
            return null;
        }
        if (message.id == 0) {
            return null;
        }
        return message;
    }

    private ReceiverStatusMessage parseReceiverStatus(Matcher m) {
        final ReceiverStatusMessage message = new ReceiverStatusMessage();
        message.callSign = m.group("callSign");
        message.version = m.group("version");
        message.cpuLoad = parseStatusNumber(m.group("cpu"));
        message.freeRam = parseStatusNumber(m.group("freeRam"));
        message.totalRam = parseStatusNumber(m.group("totalRam"));
        message.ntpOffset = parseStatusNumber(m.group("ntp"));
        message.cpuTemperature = parseStatusNumber(m.group("temperature"));
        return message;
    }

    private ReceiverLocationMessage parseReceiverLocation(Matcher m) {
        final ReceiverLocationMessage message = new ReceiverLocationMessage();
        message.callSign = m.group("callSign");
        message.timestamp = parseTimestamp(m.group("time"));
        if (message.timestamp == 0) {
            return null;
        }
        message.latitude = parseLatitude(m.group("latDeg"), m.group("latMin"), null,
                m.group("latNS"));
        message.longitude = parseLongitude(m.group("lonDeg"), m.group("lonMin"), null,
                m.group("lonEW"));
        if (Double.isNaN(message.latitude) || Double.isNaN(message.longitude)) {
            return null;
        }
        message.altitude = parseAltitude(m.group("alt"));
        if (message.altitude == Integer.MIN_VALUE) {
            return null;
        }
        return message;
    }

    @VisibleForTesting
    long parseTimestamp(String string) {
        final int parsed = Integer.parseInt(string);

        final int hours = parsed / 10000;
        if (hours < 0 || hours > 23) {
            return 0;
        }

        final int minutes = parsed / 100 % 100;
        if (minutes < 0 || minutes > 59) {
            return 0;
        }

        final int seconds = parsed % 100;
        if (seconds < 0 || seconds > 59) {
            return 0;
        }

        final long now = getCurrentTime();
        long timestamp = now - now % (24 * 60 * 60 * 1000);
        timestamp -= timestamp % (24 * 60 * 60 * 1000);
        timestamp += seconds * 1000;
        timestamp += minutes * 60 * 1000;
        timestamp += hours * 60 * 60 * 1000;
        if (timestamp > now && hours == 23 && minutes == 59) {
            // 23:59:59 at 00:00:00 was yesterday.
            timestamp -= 24 * 60 * 60 * 1000;
        }
        return timestamp;
    }

    @VisibleForTesting
    static double parseLatitude(String degrees, String minutes, String enhancement, String sign) {
        double latitudeInMinutes;
        latitudeInMinutes = Integer.parseInt(degrees) * 60;
        latitudeInMinutes += Double.parseDouble(minutes);
        if (enhancement != null) {
            latitudeInMinutes += (enhancement.charAt(0) - '0') * 0.001;
        }
        if (latitudeInMinutes > 90 * 60) {
            return Double.NaN;
        }
        if (sign.equals("S")) {
            latitudeInMinutes = -latitudeInMinutes;
        }
        return latitudeInMinutes / 60;
    }

    @VisibleForTesting
    static double parseLongitude(String degrees, String minutes, String enhancement, String sign) {
        double longitudeInMinutes;
        longitudeInMinutes = Integer.parseInt(degrees) * 60;
        longitudeInMinutes += Double.parseDouble(minutes);
        if (enhancement != null) {
            longitudeInMinutes += (enhancement.charAt(0) - '0') * 0.001;
        }
        if (longitudeInMinutes > 180 * 60) {
            return Double.NaN;
        }
        if (sign.equals("W")) {
            longitudeInMinutes = -longitudeInMinutes;
        }
        return longitudeInMinutes / 60;
    }

    @VisibleForTesting
    static int parseHeading(String string) {
        if (string == null) {
            return 0;
        }
        final int heading = Integer.parseInt(string);
        if (heading > 360) {
            return 0;
        }
        return heading;
    }

    @VisibleForTesting
    static int parseGroundSpeed(String string) {
        if (string == null) {
            return 0;
        }
        return UnitsConverter.knotsToKmh(Integer.parseInt(string));
    }

    @VisibleForTesting
    static int parseAltitude(String string) {
        if (string == null) {
            return Integer.MIN_VALUE;
        }
        final int altitude = UnitsConverter.feetToMetres(Integer.parseInt(string));
        if (altitude < -100 || altitude > 100000) {
            return Integer.MIN_VALUE;
        }
        return altitude;
    }

    @VisibleForTesting
    static long parseId(String string) {
        if (string == null) {
            return 0;
        }
        return Long.parseLong(string, 16);
    }

    @VisibleForTesting
    static double parseClimbRate(String string) {
        if (string == null) {
            return Double.NaN;
        }
        return UnitsConverter.feetToMetres(Integer.parseInt(string)) / 60.0;
    }

    @VisibleForTesting
    static double parseTurnRate(String string) {
        if (string == null) {
            return Double.NaN;
        }
        return Double.parseDouble(string) * 3;
    }

    @VisibleForTesting
    static int parseFlightLevel(String string) {
        if (string == null) {
            return Integer.MIN_VALUE;
        }
        final int altitude =
                UnitsConverter.feetToMetres((int) Math.round(Double.parseDouble(string) * 100));
        if (altitude < -100 || altitude > 100000) {
            return Integer.MIN_VALUE;
        }
        return altitude;
    }

    @VisibleForTesting
    static boolean isNotImplementedYet(String message) {
        if (message.endsWith("OGN-R/PilotAware")) {
            return true;
        }
        if (message.endsWith("SoftRF")) {
            return true;
        }
        if (message.endsWith("AVIONIX ENGINEERING ADS-B/OGN receiver")) {
            return true;
        }
        if (message.endsWith("065")) {
            if (message.endsWith("Belgian Aeromodelling League receiver 065")) {
                return true;
            }
            if (message.endsWith("Aeromodelling airfield 065")) {
                return true;
            }
            return false;
        }
        if (message.endsWith("&Using AirGw2/LsaSi")) {
            return true; // They provide us no altitude - useless for AR anyway.
        }
        final int index = message.indexOf('>');
        if (index == -1) {
            return false;
        }
        for (String unsupported : UNSUPPORTED) {
            if (message.startsWith(unsupported, index + 1)) {
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    static double parseStatusNumber(String string) {
        if (string == null) {
            return Double.NaN;
        }
        return Double.parseDouble(string);
    }
}
