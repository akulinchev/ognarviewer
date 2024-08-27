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

import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.MainThread;

import java.net.InetAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import me.testcase.ognarviewer.BuildConfig;

public class Client {
    private static final String TAG = "Client";

    private static final String HOST = "aprs.glidernet.org";
    private static final int PORT = 14580;

    private ClientThread mThread;

    @MainThread
    public void connect(Location location, int radius, MessageListener listener, Handler handler) {
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        mThread = new ClientThread(location, radius, listener, handler);
        mThread.start();
    }

    public void disconnect() {
        disconnect(false);
    }

    public void disconnect(boolean wait) {
        if (mThread != null) {
            mThread.interrupt();
            if (wait) {
                try {
                    mThread.join();
                } catch (InterruptedException e) {
                    // Ignore.
                }
            }
            mThread = null;
        }
    }

    public interface MessageListener {
        void onAprsMessage(AprsMessage message);
        void onAprsClientError(Exception e);
        void onInvalidAprsMessage(String message);
        void onAprsDisconnected();
    }

    private static class ClientThread extends Thread {
        private static final int KEEP_ALIVE_INTERVAL = 30 * 1000;
        private static final byte[] KEEP_ALIVE = new byte[]{'#', '\n'};

        private final Location mLocation;
        private final int mRadius;
        private final MessageListener mListener;
        private final Handler mHandler;

        public ClientThread(Location location, int radius, MessageListener listener,
                            Handler handler) {
            mLocation = location;
            mRadius = radius;
            mListener = listener;
            mHandler = handler;
        }

        @Override
        public void run() {
            // The OGN sometimes has problems with their server configuration.
            // But likely they have many, so we can try all of them.
            final InetAddress[] addresses;
            try {
                Log.i(TAG, "Trying to resolve " + HOST);
                addresses = InetAddress.getAllByName(HOST);
            } catch (UnknownHostException e) {
                Log.e(TAG, "Resolved failed!");
                postAprsClientError(e);
                return;
            }
            Log.i(TAG, "Resolve succeeded, got " + addresses.length + " addresses");
            for (int i = 0; i < addresses.length; ++i) {
                Log.v(TAG, "Trying " + addresses[i]);
                try (Socket socket = new Socket(addresses[i], PORT)) {
                    socket.setTcpNoDelay(true);
                    final String command = String.format(Locale.US,
                            "user NOCALL pass -1 vers ogn-ar-viewer %s filter r/%+.3f/%+.3f/%d\n",
                            BuildConfig.VERSION_NAME, mLocation.getLatitude(),
                            mLocation.getLongitude(), mRadius);
                    Log.v(TAG, "Connecting with: " + command.substring(0, command.length() - 1));
                    socket.getOutputStream().write(command.getBytes(StandardCharsets.UTF_8));
                    long lastKeepAlive = System.currentTimeMillis();
                    final Parser parser = new Parser(socket.getInputStream());
                    while (!isInterrupted()) {
                        final long now = System.currentTimeMillis();
                        if (now - lastKeepAlive > KEEP_ALIVE_INTERVAL) {
                            socket.getOutputStream().write(KEEP_ALIVE);
                            Log.d(TAG, "Sent keep alive comment");
                            lastKeepAlive = now;
                        }
                        final AprsMessage message = parser.parse();
                        if (message == null) {
                            if (parser.isEndOfStream()) {
                                postAprsDisconnected();
                                break;
                            }
                            postInvalidAprsMessage(parser.getCurrentLine());
                        } else {
                            postAprsMessage(message);
                        }
                    }
                    break; // Don't try other IP addresses.
                } catch (NoRouteToHostException e) {
                    Log.e(TAG, addresses[i] + " failed");
                    if (i == addresses.length - 1) {
                        Log.e(TAG, "All addresses failed. Giving up!");
                        postAprsClientError(e);
                    }
                } catch (Exception e) {
                    postAprsClientError(e);
                    break; // Don't try other IP addresses.
                }
            }
        }

        private void postAprsMessage(AprsMessage message) {
            mHandler.post(() -> mListener.onAprsMessage(message));
        }

        private void postInvalidAprsMessage(String message) {
            mHandler.post(() -> mListener.onInvalidAprsMessage(message));
        }

        private void postAprsClientError(Exception e) {
            mHandler.post(() -> mListener.onAprsClientError(e));
        }

        private void postAprsDisconnected() {
            mHandler.post(mListener::onAprsDisconnected);
        }
    }
}
