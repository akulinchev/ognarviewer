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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.TimeZone;

import me.testcase.ognarviewer.utils.UnitsConverter;

@RunWith(RobolectricTestRunner.class)
public class ParserTest {
    @Test
    public void testAircraftLocations() throws IOException {
        final Parser parser = createParser("# aprsc 2.1.15-gc67551b\n"
                + "# logresp NOCALL unverified, server GLIDERN2\n"
                + "# aprsc 2.1.15-gc67551b 3 Aug 2024 17:36:32 GMT GLIDERN2 51.68.189.96:14580\n"
                + "ICA896179>OGADSB,qAS,EDFW:/204949h4941.79N/01020.89E^110/530/A=035968 !W75! id25896179 +0fpm FL350.00 A5:UAE244 Sq5235\n"
                + "ICA4D2511>OGADSB,qAS,HLST:/081616h4839.66N/00802.68E^/A=022764 !W80! id014D2511 FL217.50\n"
                + "ICA4D22C2>OGADSB,qAS,HLST:/085409h4933.00N/00615.75E^/A=035439 !W34! id014D22C2 FL344.25\n"
                + "# aprsc 2.1.15-gc67551b 3 Aug 2024 17:40:53 GMT GLIDERN2 51.68.189.96:14580\n"
                + "ICA3950D0>OGADSB,qAS,Ivry:/111423h4841.30N/00214.71E^000/169 !W87! id253950D0 +1280fpm FL036.91 A0:AFR71ZZ\n"
                + "ICA3E7112>OGADSB,qAS,EDFWAVX:/113215h4946.10N/01115.02E^179/086/A=004593 !W71! id013E7112\n"
                + "OGNFD7540>APRS,qAS,NAVITER2:/172217h5008.82N/00733.29E'000/000/A=001453 !W69! id1EFD7540 +000fpm +0.0rot\n"
                + "OGNFD7EC0>APRS,qAS,NAVITER:/174713h4541.24N/00913.05E'217/060/A=000984 !W33! id1EFD7EC0 +000fpm +0.0rot\n"
                + "FLR2018EF>APRS,qAS,Leuk:/175025h4614.48N/00731.44Eg355/016/A=008131 !W43! id1E2018EF -197fpm +0.0rot 3.0dB 2e +5.0kHz\n"
                + "ICA3E6018>OGFLR,qAS,EDSW:/175400h4811.72N/00756.11E'062/051/A=003708 !W54! id053E6018 +297fpm -0.1rot 11.5dB -2.8kHz gps2x3\n"
                + "FLRDDD494>OGFLR,qAS,LFGA:/175400h4806.73N/00721.79E'161/000/A=000699 !W05! id06DDD494 -296fpm +0.0rot 42.2dB -9.1kHz gps8x15 +3.5dBm\n"
                + "FLRDDC287>OGFLR,qAS,EDKV:/175400h5022.73N/00636.71E'198/075/A=003888 !W20! id0ADDC287 +099fpm +0.0rot 4.0dB 2e -1.0kHz gps2x4 s7.22 h1E\n"
                + "FLR3e5cbc>APRS,qAS,NAVITER2:/142955h5138.38N/00720.06E'000/000/A=000180 !W05! id063e5cbc +000fpm +0.0rot\n"
                + "ICA4B292C>OGFLR7,qAS,Letzi:/114118h4710.59N\\00849.71E^091/125/A=004154 !W41! id214B292C -138fpm -0.2rot 8.0dB -6.5kHz gps2x3");

        testAircraftLocation(parser.parse(), "ICA896179", 49.69662, 10.34825, 110, 982, 10963, 0, Double.NaN);
        testAircraftLocation(parser.parse(), "ICA4D2511", 48.66113, 8.04467, 0, 0, 6938, Double.NaN, Double.NaN);
        testAircraftLocation(parser.parse(), "ICA4D22C2", 49.55005, 6.26257, 0, 0, 10802, Double.NaN, Double.NaN);
        testAircraftLocation(parser.parse(), "ICA3950D0", 48.68847, 2.24528, 0, 313, 1125, 6.5, Double.NaN);
        testAircraftLocation(parser.parse(), "ICA3E7112", 49.76845, 11.25035, 179, 159, 1400, Double.NaN, Double.NaN);
        testAircraftLocation(parser.parse(), "OGNFD7540", 50.1471, 7.55498, 0, 0, 443, 0.0, 0.0);
        testAircraftLocation(parser.parse(), "OGNFD7EC0", 45.68738, 9.21755, 217, 111, 300, 0.0, 0.0);
        testAircraftLocation(parser.parse(), "FLR2018EF", 46.2414, 7.52405, 355, 30, 2478, -1.0, 0.0);
        testAircraftLocation(parser.parse(), "ICA3E6018", 48.19542, 7.93523, 62, 94, 1130, 1.51666, -0.3);
        testAircraftLocation(parser.parse(), "FLRDDD494", 48.11217, 7.36325, 161, 0, 213, -1.5, 0.0);
        testAircraftLocation(parser.parse(), "FLRDDC287", 50.37887, 6.61183, 198, 139, 1185, 0.5, 0.0);
        testAircraftLocation(parser.parse(), "FLR3e5cbc", 51.63967, 7.33442, 0, 0, 55, 0.0, 0.0);
        testAircraftLocation(parser.parse(), "ICA4B292C", 47.17657, 8.82852, 91, 232, 1266, -0.7, -0.6);
    }

    private void testAircraftLocation(AprsMessage message, String callSign, double lat, double lon, int heading, int speed, int alt, double climbRate, double turnRate) {
        Assert.assertNotNull(message);
        Assert.assertEquals(callSign, message.callSign);

        AircraftLocationMessage ognMessage = (AircraftLocationMessage) message;
        Assert.assertNotNull(ognMessage);
        Assert.assertEquals(lat, ognMessage.latitude, 0.00001);
        Assert.assertEquals(lon, ognMessage.longitude, 0.00001);
        Assert.assertEquals(heading, ognMessage.heading);
        Assert.assertEquals(speed, ognMessage.groundSpeed);
        Assert.assertEquals(alt, ognMessage.altitude);
        Assert.assertEquals(climbRate, ognMessage.climbRate, 0.00001);
        Assert.assertEquals(turnRate, ognMessage.turnRate, 0.00001);
    }

    @Test
    public void testTimestamp() {
        final Parser parser = createParser("");

        final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        calendar.setTimeInMillis(parser.parseTimestamp("123456"));
        Assert.assertEquals(2024, calendar.get(Calendar.YEAR));
        Assert.assertEquals(6, calendar.get(Calendar.MONTH));
        Assert.assertEquals(30, calendar.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(12, calendar.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(34, calendar.get(Calendar.MINUTE));
        Assert.assertEquals(56, calendar.get(Calendar.SECOND));

        Assert.assertEquals(0, parser.parseTimestamp("240000"));

        calendar.setTimeInMillis(parser.parseTimestamp("230000"));
        Assert.assertEquals(2024, calendar.get(Calendar.YEAR));
        Assert.assertEquals(6, calendar.get(Calendar.MONTH));
        Assert.assertEquals(30, calendar.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(23, calendar.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(0, calendar.get(Calendar.MINUTE));
        Assert.assertEquals(0, calendar.get(Calendar.SECOND));

        Assert.assertEquals(0, parser.parseTimestamp("006000"));

        calendar.setTimeInMillis(parser.parseTimestamp("005900"));
        Assert.assertEquals(2024, calendar.get(Calendar.YEAR));
        Assert.assertEquals(6, calendar.get(Calendar.MONTH));
        Assert.assertEquals(30, calendar.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(59, calendar.get(Calendar.MINUTE));
        Assert.assertEquals(0, calendar.get(Calendar.SECOND));

        Assert.assertEquals(0, parser.parseTimestamp("000060"));

        calendar.setTimeInMillis(parser.parseTimestamp("000059"));
        Assert.assertEquals(2024, calendar.get(Calendar.YEAR));
        Assert.assertEquals(6, calendar.get(Calendar.MONTH));
        Assert.assertEquals(30, calendar.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(0, calendar.get(Calendar.MINUTE));
        Assert.assertEquals(59, calendar.get(Calendar.SECOND));

        calendar.setTimeInMillis(parser.parseTimestamp("235942"));
        Assert.assertEquals(2024, calendar.get(Calendar.YEAR));
        Assert.assertEquals(6, calendar.get(Calendar.MONTH));
        Assert.assertEquals(29, calendar.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(23, calendar.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(59, calendar.get(Calendar.MINUTE));
        Assert.assertEquals(42, calendar.get(Calendar.SECOND));

        calendar.setTimeInMillis(parser.parseTimestamp("235842"));
        Assert.assertEquals(2024, calendar.get(Calendar.YEAR));
        Assert.assertEquals(6, calendar.get(Calendar.MONTH));
        Assert.assertEquals(30, calendar.get(Calendar.DAY_OF_MONTH));
        Assert.assertEquals(23, calendar.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(58, calendar.get(Calendar.MINUTE));
        Assert.assertEquals(42, calendar.get(Calendar.SECOND));
    }

    @Test
    public void testLatitude() {
        // Normal values.
        Assert.assertEquals(+49.123, Parser.parseLatitude("49", "07.38", "0", "N"), 0.00001);
        Assert.assertEquals(+49.123, Parser.parseLatitude("49", "07.38", null, "N"), 0.00001);
        Assert.assertEquals(-49.123, Parser.parseLatitude("49", "07.38", "0", "S"), 0.00001);
        Assert.assertEquals(-49.123, Parser.parseLatitude("49", "07.38", null, "S"), 0.00001);
        Assert.assertEquals(+49.1234, Parser.parseLatitude("49", "07.40", "4", "N"), 0.00001);
        Assert.assertEquals(-49.1234, Parser.parseLatitude("49", "07.40", "4", "S"), 0.00001);

        // All zeros.
        Assert.assertEquals(0, Parser.parseLatitude("00", "00.00", "0", "N"), 0.00001);
        Assert.assertEquals(0, Parser.parseLatitude("00", "00.00", null, "N"), 0.00001);
        Assert.assertEquals(0, Parser.parseLatitude("00", "00.00", "0", "S"), 0.00001);
        Assert.assertEquals(0, Parser.parseLatitude("00", "00.00", null, "S"), 0.00001);

        // North pole.
        Assert.assertEquals(90, Parser.parseLatitude("90", "00.00", "0", "N"), 0.00001);
        Assert.assertEquals(90, Parser.parseLatitude("90", "00.00", null, "N"), 0.00001);
        Assert.assertTrue(Double.isNaN(Parser.parseLatitude("90", "00.00", "1", "N")));
        Assert.assertTrue(Double.isNaN(Parser.parseLatitude("90", "00.01", "0", "N")));
        Assert.assertTrue(Double.isNaN(Parser.parseLatitude("90", "00.01", null, "N")));

        // South pole.
        Assert.assertEquals(-90, Parser.parseLatitude("90", "00.00", "0", "S"), 0.00001);
        Assert.assertEquals(-90, Parser.parseLatitude("90", "00.00", null, "S"), 0.00001);
        Assert.assertTrue(Double.isNaN(Parser.parseLatitude("90", "00.00", "1", "S")));
        Assert.assertTrue(Double.isNaN(Parser.parseLatitude("90", "00.01", "0", "S")));
        Assert.assertTrue(Double.isNaN(Parser.parseLatitude("90", "00.01", null, "S")));
    }

    @Test
    public void testLongitude() {
        // Normal values.
        Assert.assertEquals(+7.321, Parser.parseLongitude("007", "19.26", "0", "E"), 0.00001);
        Assert.assertEquals(+7.321, Parser.parseLongitude("007", "19.26", null, "E"), 0.00001);
        Assert.assertEquals(-7.321, Parser.parseLongitude("007", "19.26", "0", "W"), 0.00001);
        Assert.assertEquals(-7.321, Parser.parseLongitude("007", "19.26", null, "W"), 0.00001);
        Assert.assertEquals(+7.4321, Parser.parseLongitude("007", "25.92", "6", "E"), 0.00001);
        Assert.assertEquals(-7.4321, Parser.parseLongitude("007", "25.92", "6", "W"), 0.00001);

        // All zeros.
        Assert.assertEquals(0, Parser.parseLongitude("000", "00.00", "0", "E"), 0.00001);
        Assert.assertEquals(0, Parser.parseLongitude("000", "00.00", null, "E"), 0.00001);
        Assert.assertEquals(0, Parser.parseLongitude("000", "00.00", "0", "W"), 0.00001);
        Assert.assertEquals(0, Parser.parseLongitude("000", "00.00", null, "W"), 0.00001);

        // "Right" border.
        Assert.assertEquals(180, Parser.parseLongitude("180", "00.00", "0", "E"), 0.00001);
        Assert.assertEquals(180, Parser.parseLongitude("180", "00.00", null, "E"), 0.00001);
        Assert.assertTrue(Double.isNaN(Parser.parseLongitude("180", "00.00", "1", "E")));
        Assert.assertTrue(Double.isNaN(Parser.parseLongitude("180", "00.01", "0", "E")));
        Assert.assertTrue(Double.isNaN(Parser.parseLongitude("180", "00.01", null, "E")));

        // "Left" border.
        Assert.assertEquals(-180, Parser.parseLongitude("180", "00.00", "0", "W"), 0.00001);
        Assert.assertEquals(-180, Parser.parseLongitude("180", "00.00", null, "W"), 0.00001);
        Assert.assertTrue(Double.isNaN(Parser.parseLongitude("180", "00.00", "1", "W")));
        Assert.assertTrue(Double.isNaN(Parser.parseLongitude("180", "00.01", "0", "W")));
        Assert.assertTrue(Double.isNaN(Parser.parseLongitude("180", "00.01", null, "W")));
    }

    @Test
    public void testHeading() {
        Assert.assertEquals(0, Parser.parseHeading(null));
        Assert.assertEquals(0, Parser.parseHeading("000"));
        Assert.assertEquals(1, Parser.parseHeading("001"));
        Assert.assertEquals(123, Parser.parseHeading("123"));
        // Heading 360 is valid (sic!), but 361 is not.
        Assert.assertEquals(360, Parser.parseHeading("360"));
        Assert.assertEquals(0, Parser.parseHeading("361"));
    }

    @Test
    public void testGroundSpeed() {
        Assert.assertEquals(0, Parser.parseGroundSpeed(null));
        Assert.assertEquals(0, Parser.parseGroundSpeed("000"));
        Assert.assertEquals(2, Parser.parseGroundSpeed("001"));
        Assert.assertEquals(UnitsConverter.knotsToKmh(123), Parser.parseGroundSpeed("123"));
    }

    @Test
    public void testAltitude() {
        Assert.assertEquals(0, Parser.parseAltitude("000000", null));
        Assert.assertEquals(0, Parser.parseAltitude("000001", null));
        Assert.assertEquals(1, Parser.parseAltitude("000003", null));
        Assert.assertEquals(10, Parser.parseAltitude("000032", null));
        Assert.assertEquals(10, Parser.parseAltitude("+32", null));

        // GPS altitude, if given, should take precedence over the flight level.
        Assert.assertEquals(10, Parser.parseAltitude("000032", "350.00"));

        // If no GPS altitude is given, use the flight level.
        Assert.assertEquals(10000, Parser.parseAltitude(null, "328.08"));

        // If neither GPS altitude nor the flight level are available, give up.
        Assert.assertEquals(Integer.MIN_VALUE, Parser.parseAltitude(null, null));

        // E.g. "//OGNFD6CD6>APRS,qAS,NAVITER2:/122825h5156.59N/00457.26E'000/000/A=-00003 !W57! id1EFD6CD6 +000fpm +0.0rot"
        Assert.assertEquals(-1, Parser.parseAltitude("-00003", null));
    }

    @Test
    public void testId() {
        Assert.assertEquals(0, Parser.parseId(null));
        Assert.assertEquals(0, Parser.parseId("00000000"));
        Assert.assertEquals(0xdeadbeafL, Parser.parseId("DEADBEAF"));
    }

    @Test
    public void testClimbRate() {
        Assert.assertTrue(Double.isNaN(Parser.parseClimbRate(null)));
        Assert.assertEquals(0, Parser.parseClimbRate("+000"), 0.0001);
        Assert.assertEquals(UnitsConverter.feetToMetres(123) / 60.0, Parser.parseClimbRate("+123"), 0.0001);
        Assert.assertEquals(UnitsConverter.feetToMetres(-4321) / 60.0, Parser.parseClimbRate("-4321"), 0.0001);
    }

    @Test
    public void testTurnRate() {
        Assert.assertTrue(Double.isNaN(Parser.parseTurnRate(null)));
        Assert.assertEquals(0, Parser.parseTurnRate("+0.0"), 0.0001);
        Assert.assertEquals(0, Parser.parseTurnRate("-0.0"), 0.0001);
        Assert.assertEquals(+12.6, Parser.parseTurnRate("+4.2"), 0.0001);
        Assert.assertEquals(-12.6, Parser.parseTurnRate("-4.2"), 0.0001);
        Assert.assertEquals(+30.3, Parser.parseTurnRate("+10.1"), 0.0001);
        Assert.assertEquals(-30.3, Parser.parseTurnRate("-10.1"), 0.0001);
    }

    @Test
    public void testUnimplementedMessages() {
        Assert.assertTrue(Parser.isNotImplementedYet("PWASFG>APRS,TCPIP*,qAC,GLIDERN1:>141440h v20240712 OGN-R/PilotAware"));
        Assert.assertTrue(Parser.isNotImplementedYet("Champsaur>OGNSDR,TCPIP*,qAC,GLIDERN5:/111344h4438.73NI00610.40E&/A=003487 SoftRF"));
        Assert.assertTrue(Parser.isNotImplementedYet("EDTQ2>OGNSDR,TCPIP*,qAC,GLIDERN5:/111404h4851.90NI00913.35E&/A=000961 AVIONIX ENGINEERING ADS-B/OGN receiver"));
        Assert.assertTrue(Parser.isNotImplementedYet("DO6GZ>APRS,TCPIP*,qAC,GLIDERN3:!5158.92N/00950.63E&Using AirGw2/LsaSi"));
        Assert.assertTrue(Parser.isNotImplementedYet("BAL065>APRS,TCPIP*,qAC,GLIDERN1:/114055h5029.61NI00531.41E&/A=000492 Belgian Aeromodelling League receiver 065"));
        Assert.assertTrue(Parser.isNotImplementedYet("BAL065>APRS,TCPIP*,qAC,GLIDERN1:/203202h5029.61NI00531.41E&/A=000492 Aeromodelling airfield 065"));

        Assert.assertTrue(Parser.isNotImplementedYet("97076TGO>OGNSXR,TCPIP*,qAC,GLIDERN1:/143112h4948.83NI00959.01E&/A=000787"));
        Assert.assertTrue(Parser.isNotImplementedYet("97076TGO>OGNSXR,TCPIP*,qAC,GLIDERN1:>143112h vMB145-ESP32-SX1276-OGNbase 0/min 0/0Acfts[1h]"));
        Assert.assertTrue(Parser.isNotImplementedYet("ATO4849EB>OGNATO,qAS,ATO761638:/095144h5243.89N/00630.64E'247/000/A=000174 !W61! id214849EB  gps1x1"));
        Assert.assertTrue(Parser.isNotImplementedYet("DMDDD>SimpleVFR,TCPIP*,qAC,GLIDERN3:/184547h5201.21N/00958.15E'182/096/A=003609 !W66!"));
        Assert.assertTrue(Parser.isNotImplementedYet("do6gz-6>APRRDZ,TCPIP*,qAC,GLIDERN4:!5158.93N/00950.65E( rdzTTGOsonde-dev20240924"));
        Assert.assertTrue(Parser.isNotImplementedYet("EDTA>OGNDVS,TCPIP*,qAC,GLIDERN2:>114105h 0:0 2.563s/0ms 23dB/+7kHz 083/0/0kt +42.7F 84.8% 0.0mm/h"));
        Assert.assertTrue(Parser.isNotImplementedYet("FLR3FF039>OGADSL,qAS,EDVI:/114029h5148.43N\\00922.71E^246/009/A=000938 !W09! id223FF039 -098fpm 37.8dB -6.8kHz gps63x63"));
        Assert.assertTrue(Parser.isNotImplementedYet("FLRDDFD06>OGAPIK,qAS,APIK:/172432h4533.40N/00600.14E'006/048/A=003202 !W45! id06DDFD06 euiecdb86fffe000e06"));
        Assert.assertTrue(Parser.isNotImplementedYet("FMTFE1D8C>OGFLYM,qAS,FLYMASTER:/142351h4535.51N/00645.14Eg000/000/A=002477 !W52! id1CFE1D8C -011fpm +0.0rot"));
        Assert.assertTrue(Parser.isNotImplementedYet("FNO000549>OGNFNO,qAS,Neurone:/074022h4434.39N/00601.86E'036/058/A=005095 !W66! id20000549 +216fpm +0.0rot"));
        Assert.assertTrue(Parser.isNotImplementedYet("FNT3FF039>OGNFNT,qAS,EDVI:/142659h5148.44N\\00922.82E^000/000/A=000988 !W20! id233FF039 +20fpm +0.0rot FNT15 sF1 cr1 -6.4kHz 7e"));
        Assert.assertTrue(Parser.isNotImplementedYet("FXC085DD4>FXCAPP,qAS,FLYXC:/141254h4647.35N/00537.85Eg075/025/A=004839 !W51! id1E085DD4"));
        Assert.assertTrue(Parser.isNotImplementedYet("ICA3FFA1F>OGNTRK,qAS,BRTWRSTLU:/142348h4934.05N\\00811.61E^133/081/A=002244 !W03! id213FFA1F -336fpm -0.1rot FL022.47 13.5dB -2.7kHz gps2x3"));
        Assert.assertTrue(Parser.isNotImplementedYet("ICA48665B>OGNSKY,qAS,SafeSky:/142355h5157.54N/00558.18E'296/087/A=002080 !W43! id2048665B +000fpm gps2x1"));
        Assert.assertTrue(Parser.isNotImplementedYet("MOITZFELD>APWEE5,TCPIP*,qAC,GLIDERN4:@031710z5058.12N/00709.19E_336/004g007t072r000p011P011b10128h66L037.weewx-5.0.2-WXT5x0"));
        Assert.assertTrue(Parser.isNotImplementedYet("MTKEB2389>OGNMTK,qAS,Microtrak:/143325h4524.93N/00523.27E'260/043/A=002992 !W50! id0FEB2389"));
        Assert.assertTrue(Parser.isNotImplementedYet("OGNF344FD>OGAIRM,qAS,Airmate:/030905h4978.59N/00756.60Ez092/116/A=002499 !W01! id0BF344FD -132fpm +0.0rot"));
        Assert.assertTrue(Parser.isNotImplementedYet("PAWF95F3A>OGPAW,qAS,LILH:/142806h4509.76N\\00928.25E^168/102/A=002008 !W78! id23F95F3A 8.0dB +1.2kHz +17.6dBm"));
        Assert.assertTrue(Parser.isNotImplementedYet("PUR123ABD>OGNPUR,qAS,PureTrk23:/114103h4550.99N/00618.75Eg000/000/A=003927 !W77! id1E123ABD +000fpm +0.0rot 0.0dB 0e +0.0kHz gps2x3"));
        Assert.assertTrue(Parser.isNotImplementedYet("PUR123ABD>OGPURTK,qAS,PureTrk23:/114055h4534.49N/00607.25E7000/000/A=004016 !W51! id1E123ABD +000fpm +0.0rot 0.0dB 0e +0.0kHz gps2x3"));
        Assert.assertTrue(Parser.isNotImplementedYet("XCG640470>OGNXCG,qAS,XCC640470:/142805h4924.62N/00515.14Eg174/000/A=001129 id1F640470 +000fpm gps0x0"));

        Assert.assertFalse(Parser.isNotImplementedYet("ICA4D2511>OGADSB,qAS,HLST:/081616h4839.66N/00802.68E^/A=022764 !W80! id014D2511 FL217.50"));
        Assert.assertFalse(Parser.isNotImplementedYet("EDRC>OGNSDR,TCPIP*,qAC,GLIDERN2:/171612h4927.06NI00702.63E&/A=001168"));
    }

    @Test
    public void testReceiverLocations() throws IOException {
        final Parser parser = createParser("# aprsc 2.1.19-g730c5c0\n"
                + "# logresp NOCALL unverified, server GLIDERN4\n"
                + "# aprsc 2.1.19-g730c5c0 3 Aug 2024 17:43:20 GMT GLIDERN4 192.168.1.14:14580\n"
                + "EDRC>OGNSDR,TCPIP*,qAC,GLIDERN2:/171612h4927.06NI00702.63E&/A=001168\n"
                + "EZAC>OGNSDR,TCPIP*,qAC,GLIDERN2:/184747h5115.33NI00353.60E&/A=000020\n"
                + "# aprsc 2.1.19-g730c5c0 3 Aug 2024 17:44:20 GMT GLIDERN4 192.168.1.14:14580\n"
                + "LFOY>OGNSDR,TCPIP*,qAC,GLIDERN5:/184746h4932.64NI00021.55E&/A=000417\n"
                + "LSXU>APRS,TCPIP*,qAC,GLIDERN1:/191946h4654.77NI00933.05E&/A=001804\n"
                + "EDTA>OGNSDR,TCPIP*,qAC,GLIDERN1:/074601h4738.82NI00823.09E&/A=001838 Bohlhof gliding club located at EDTA, 9dBi collinear, 20m AGL\n"
                + "BALDENAU>APRS,TCPIP*,qAC,66D9B8:/213838h4852.24NI00812.60E&/A=000370");

        testReceiverLocation(parser.parse(), "EDRC", 49.451, 7.04383, 356);
        testReceiverLocation(parser.parse(), "EZAC", 51.2555, 3.89333, 6);
        testReceiverLocation(parser.parse(), "LFOY", 49.544, 0.35916, 127);
        testReceiverLocation(parser.parse(), "LSXU", 46.91283, 9.55083, 550);
        testReceiverLocation(parser.parse(), "EDTA", 47.647, 8.38483, 560);
        testReceiverLocation(parser.parse(), "BALDENAU", 48.87067, 8.21, 113);
    }

    private void testReceiverLocation(AprsMessage message, String callSign, double lat, double lon, double alt) {
        Assert.assertNotNull(message);
        Assert.assertEquals(callSign, message.callSign);

        ReceiverLocationMessage locationMessage = (ReceiverLocationMessage) message;
        Assert.assertNotNull(locationMessage);
        Assert.assertEquals(lat, locationMessage.latitude, 0.00001);
        Assert.assertEquals(lon, locationMessage.longitude, 0.00001);
        Assert.assertEquals(alt, locationMessage.altitude, 0.00001);
    }

    @Test
    public void testReceiverStatuses() throws IOException {
        final Parser parser = createParser("# aprsc 2.1.19-g730c5c0\n"
                + "# logresp NOCALL unverified, server GLIDERN3\n"
                + "# aprsc 2.1.19-g730c5c0 3 Aug 2024 18:53:04 GMT GLIDERN3 85.188.1.173:14580\n"
                + "EDRC>OGNSDR,TCPIP*,qAC,GLIDERN2:>172612h v0.3.2.ARM CPU:1.1 RAM:1211.2/1939.0MB NTP:1.6ms/-12.3ppm +45.3C EGM96:+49m 4/4Acfts[1h] RF:+74+0.0ppm/+5.06dB/-6.3dB@10km[194]/-5.7dB@10km[3/5]\n"
                + "EZAC>OGNSDR,TCPIP*,qAC,GLIDERN2:>185747h v0.3.2.ARM CPU:0.9 RAM:207.5/966.8MB NTP:1.4ms/-7.4ppm +60.1C EGM96:+45m 0/0Acfts[1h] RF:+52-3.6ppm/+3.53dB/-9.5dB@10km[7188]/-10.1dB@10km[2/4]\n"
                + "# aprsc 2.1.19-g730c5c0 3 Aug 2024 18:53:44 GMT GLIDERN3 85.188.1.173:14580\n"
                + "LFOY>OGNSDR,TCPIP*,qAC,GLIDERN4:>190625h v0.3.2.RPI-GPU CPU:3.8 RAM:89.5/450.8MB NTP:2.0ms/-1.4ppm +47.6C EGM96:+45m 0/0Acfts[1h] RF:+58+1.9ppm/+0.8d0B\n"
                + "LSXU>APRS,TCPIP*,qAC,GLIDERN1:>191946h v0.2.6.RPI-GPU CPU:0.2 RAM:647.6/972.2MB NTP:0.3ms/-3.8ppm +52.1C 0/0Acfts[1h] RF:+0+0.0ppm/+21.83dB/+6.8dB@10km[242668]\n"
                + "EDTA>OGNSDR,TCPIP*,qAC,GLIDERN3:>151343h v0.3.3.ARM CPU:1.6 RAM:1569.9/2121.3MB NTP:2.0ms/-2.3ppm +73.0C EGM96:+48m 47/51Acfts[1h] Lat:2.7s RF:+78+0.0ppm/+4.12dB/+15.7dB@10km[290533]/+18.7dB@10km[106/211]\n"
                + "BALDENAU>APRS,TCPIP*,qAC,66D9B8:>212812h v0.1.0-28-ESP32 0.14V");

        testReceiverStatus(parser.parse(), "EDRC", "0.3.2.ARM", 1.1, 1211.2, 1939.0, 1.6, 45.3);
        testReceiverStatus(parser.parse(), "EZAC", "0.3.2.ARM", 0.9, 207.5, 966.8, 1.4, 60.1);
        testReceiverStatus(parser.parse(), "LFOY", "0.3.2.RPI-GPU", 3.8, 89.5, 450.8, 2.0, 47.6);
        testReceiverStatus(parser.parse(), "LSXU", "0.2.6.RPI-GPU", 0.2, 647.6, 972.2, 0.3, 52.1);
        testReceiverStatus(parser.parse(), "EDTA", "0.3.3.ARM", 1.6, 1569.9, 2121.3, 2.0, 73.0);
        testReceiverStatus(parser.parse(), "BALDENAU", "0.1.0-28-ESP32", Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
    }

    private void testReceiverStatus(AprsMessage message, String callSign, String version, double cpu, double freeRam, double totalRam, double ntp, double temp) {
        Assert.assertNotNull(message);
        Assert.assertEquals(callSign, message.callSign);

        ReceiverStatusMessage statusMessage = (ReceiverStatusMessage) message;
        Assert.assertNotNull(statusMessage);
        Assert.assertEquals(version, statusMessage.version);
        Assert.assertEquals(cpu, statusMessage.cpuLoad, 0.00001);
        Assert.assertEquals(freeRam, statusMessage.freeRam, 0.00001);
        Assert.assertEquals(totalRam, statusMessage.totalRam, 0.00001);
        Assert.assertEquals(ntp, statusMessage.ntpOffset, 0.00001);
        Assert.assertEquals(temp, statusMessage.cpuTemperature, 0.00001);
    }

    @Test
    public void testStatusNumber() {
        Assert.assertTrue(Double.isNaN(Parser.parseStatusNumber(null)));
        Assert.assertEquals(0.7, Parser.parseStatusNumber("0.7"), 0.0001);
    }

    @Test
    public void testEndOfStream() throws IOException {
        final Parser parser = createParser("");
        Assert.assertFalse(parser.isEndOfStream());
        for (int i = 0; i < 42; ++i) {
            Assert.assertNull(parser.parse());
            Assert.assertTrue(parser.isEndOfStream());
        }
    }

    private Parser createParser(String input) {
        return new TestParser(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), 1722297600000L);
    }

    private static class TestParser extends Parser {
        private final long mCurrentTime;

        public TestParser(InputStream stream, long currentTime) {
            super(stream);
            mCurrentTime = currentTime;
        }

        @Override
        protected long getCurrentTime() {
            return mCurrentTime;
        }
    }
}
