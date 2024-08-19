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

package me.testcase.ognarviewer.client;

import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
public class ClientTest {
    @Test
    public void testRealForSixtySeconds() throws InterruptedException {
        ShadowLog.stream = System.out;

        final Location location = new Location("test");
        location.setLatitude(49);
        location.setLongitude(7);
        location.setAltitude(350);

        final Client client = new Client();

        final MessageListener listener = new MessageListener();
        Assert.assertEquals(0, listener.receiverStatusMessages.get());
        Assert.assertEquals(0, listener.receiverLocationMessages.get());
        Assert.assertEquals(0, listener.aircraftLocationMessages.get());

        final ExceptionHandler exceptionHandler = new ExceptionHandler();
        final HandlerThread thread = new HandlerThread("test");
        thread.setUncaughtExceptionHandler(exceptionHandler);
        thread.start();
        final Handler handler = new Handler(thread.getLooper());

        client.connect(location, 500, listener, handler);

        thread.join(60 * 1000);

        client.disconnect(true);
        thread.quitSafely();

        if (exceptionHandler.exception != null) {
            throw new RuntimeException(exceptionHandler.exception);
        }

        System.out.println("The time is over.");
        System.out.println("Receiver status messages: " + listener.receiverStatusMessages);
        System.out.println("Receiver location messages: " + listener.receiverLocationMessages);
        System.out.println("Aircraft location messages: " + listener.aircraftLocationMessages);
        Assert.assertTrue("Too few receiver statuses", listener.receiverStatusMessages.get() > 80);
        Assert.assertTrue("Too few receiver locations", listener.receiverLocationMessages.get() > 80);
        Assert.assertTrue("Too few aircraft locations", listener.aircraftLocationMessages.get() > 1000);
    }

    private static final class MessageListener implements Client.MessageListener {
        public final AtomicInteger receiverStatusMessages = new AtomicInteger(0);
        public final AtomicInteger receiverLocationMessages = new AtomicInteger(0);
        public final AtomicInteger aircraftLocationMessages = new AtomicInteger(0);

        @Override
        public void onAprsMessage(AprsMessage message) {
            if (message instanceof ReceiverStatusMessage) {
                receiverStatusMessages.incrementAndGet();
            } else if (message instanceof ReceiverLocationMessage) {
                receiverLocationMessages.incrementAndGet();
            } else if (message instanceof AircraftLocationMessage) {
                aircraftLocationMessages.incrementAndGet();
            } else {
                Assert.fail("Got unexpected message of type " + message.getClass().getSimpleName());
            }
        }

        @Override
        public void onAprsClientError(Exception e) {
            throw new RuntimeException(e);
        }

        @Override
        public void onInvalidAprsMessage(String message) {
            Assert.fail("Cannot parse: " + message);
        }

        @Override
        public void onAprsDisconnected() {
            Assert.fail("Disconnected");
        }
    }

    private static final class ExceptionHandler implements Thread.UncaughtExceptionHandler {
        public volatile Throwable exception;

        @Override
        public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
            exception = e;
        }
    }
}
