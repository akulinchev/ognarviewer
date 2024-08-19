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

package me.testcase.ognarviewer.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.util.SizeF;
import android.view.MotionEvent;
import android.view.Surface;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.testcase.ognarviewer.BuildConfig;
import me.testcase.ognarviewer.world.Target;
import me.testcase.ognarviewer.world.World;

public class WorldView extends GLSurfaceView implements SensorEventListener,
        ManualCalibrationView.OnManualCalibration {
    private static final String TAG = "WorldView";

    private final WorldRenderer mRenderer;

    private OnTargetClickListener mOnTargetClickListener;

    private Surface mSurface;
    private Size mPreviewSize;
    private float mFocalLength;
    private CameraDevice mCameraDevice;
    private CaptureRequest mCameraRequest;
    private CameraStateCallback mCameraDeviceCallback;
    private CameraCaptureSession mCameraSession;
    private final CameraCaptureSession.StateCallback mCameraSessionCallback =
            new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Log.v(TAG, String.format("CameraCaptureSession %h created", session));
            mCameraSession = session;
            try {
                session.setRepeatingRequest(mCameraRequest, null, null);
            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.e(TAG, String.format("CameraCaptureSession %h failed", session));
        }
    };

    public WorldView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setEGLContextClientVersion(2);

        mRenderer = new WorldRenderer(this);
        setRenderer(mRenderer);
    }

    public void setWorld(World world) {
        queueEvent(() -> mRenderer.setWorld(world));
    }

    @Override
    public void onResume() {
        Log.v(TAG, "onResume()");
        super.onResume();
        final SensorManager manager =
                (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        // TODO: try TYPE_POSE_6DOF, more accurate but not available on my Pixel 4a
        final Sensor sensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onPause() {
        Log.v(TAG, "onPause()");
        final SensorManager manager =
                (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        manager.unregisterListener(this);
        closeCamera();
        super.onPause();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        queueEvent(() -> mRenderer.setDisplayRotation(getDisplay().getRotation()));
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            queueEvent(() -> mRenderer.setHitTestPoint(event.getX(), event.getY()));
            return true;
        }
        return false;
    }

    public void setOnTargetClickListener(@Nullable OnTargetClickListener listener) {
        mOnTargetClickListener = listener;
    }

    // FIXME: public to be called from WorldRenderer
    public void onHitTestResult(Target target) {
        if (mOnTargetClickListener != null) {
            mOnTargetClickListener.onTargetClick(target);
        }
    }

    // FIXME: public to be called from WorldRenderer
    public void openCamera() {
        Log.v(TAG, "openCamera()");
        if (mCameraDevice != null) {
            Log.e(TAG, String.format("openCamera(): device %h is already open!", mCameraDevice));
            return;
        }

        if (getContext().checkSelfPermission(Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {
            Log.w(TAG, "openCamera(): permission missing!");
            // openCamera() will be called again once the permission is granted.
            return;
        }

        if (!mRenderer.isAvailable()) {
            Log.w(TAG, "openCamera(): TextureView unavailable!");
            // openCamera() will be called again once the surface texture is available.
            return;
        }

        if (!isAttachedToWindow()) {
            // Happened on an emulator once.
            Log.w(TAG, "NOT ATTACHED?!");
            return;
        }

        if (mCameraDeviceCallback != null) {
            mCameraDeviceCallback.discard();
            mCameraDeviceCallback = null;
        }

        try {
            // Note: we need to use the application context here because otherwise LeakCanary
            // reports a memory leak.
            final Context applicationContext = getContext().getApplicationContext();
            final CameraManager cameraManager =
                    (CameraManager) applicationContext.getSystemService(Context.CAMERA_SERVICE);
            for (String cameraId : cameraManager.getCameraIdList()) {
                final CameraCharacteristics characteristics =
                        cameraManager.getCameraCharacteristics(cameraId);
                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing == null || facing != CameraCharacteristics.LENS_FACING_BACK) {
                    continue;
                }

                final float[] focalLengths = characteristics.get(
                        CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                if (focalLengths == null || focalLengths.length < 1) {
                    continue;
                }
                // Choose the first one and remember to use it in the CaptureRequest later.
                mFocalLength = focalLengths[0];

                final StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                //noinspection ConstantConditions
                final Size[] outputSizes = map.getOutputSizes(SurfaceTexture.class);
                Arrays.sort(outputSizes, (a, b) -> {
                    if (a.getWidth() == b.getWidth()) {
                        return Long.signum(a.getHeight() - b.getHeight());
                    }
                    return Long.signum(a.getWidth() - b.getWidth());
                });
                final int viewLongSide = Math.max(getWidth(), getHeight());
                final int viewShortSide = Math.min(getWidth(), getHeight());
                final double viewAspectRatio = viewShortSide / (double) viewLongSide;
                for (Size size : outputSizes) {
                    // Stop when the size becomes too large (performance issues).
                    // Note that 1088 is not an error - some manufactures round up to mod16.
                    if (size.getWidth() * size.getHeight() > 1920 * 1088) {
                        break;
                    }

                    // Skip too narrow sizes.
                    assert size.getWidth() >= size.getHeight(); // The sensor is always landscape.
                    final double sizeAspectRatio = size.getHeight() / (double) size.getWidth();
                    if (sizeAspectRatio < viewAspectRatio) {
                        continue;
                    }

                    // Best match so far.
                    mPreviewSize = size;

                    // Stop when the size is big enough.
                    if (size.getWidth() >= viewLongSide) {
                        break;
                    }
                }

                // If no suitable size is found, try the next camera.
                if (mPreviewSize == null) {
                    continue;
                }

                // Calculate the view angle. Assuming the device is in portrait orientation. Will be
                // corrected below.
                final SizeF sensorPhysicalSize = characteristics.get(
                        CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                assert sensorPhysicalSize != null; // Should be available on all devices.
                // FIXME: not all sensor pixels can receive light. It were more correctly to ask for
                //        SENSOR_INFO_ACTIVE_ARRAY_SIZE and SENSOR_INFO_PIXEL_ARRAY_SIZE and
                //        calculate the focal length in pixels. But it's too complicated and not
                //        required on most devices. Even when it does, the error is very small.
                double verticalViewAngle = Math.toDegrees(
                        2 * Math.atan(sensorPhysicalSize.getWidth() / (2 * mFocalLength)));
                // FIXME: the code below assumes sensor rotation 90 deg?
                if (getHeight() < getWidth()) {
                    verticalViewAngle *= getHeight() / (double) getWidth();
                }

                final float finalVerticalViewAngle = (float) verticalViewAngle;

                queueEvent(() -> mRenderer.setCameraMetadata(
                        mPreviewSize.getWidth(), mPreviewSize.getHeight(), finalVerticalViewAngle));

                mCameraDeviceCallback = new CameraStateCallback();
                cameraManager.openCamera(cameraId, mCameraDeviceCallback, null);

                return;
            }
        } catch (CameraAccessException e) {
            if (BuildConfig.DEBUG) {
                throw new RuntimeException(e);
            }
            Log.e(TAG, "CameraAccessException: " + e.toString());
            // Logging is enough: a toast is shown below.
        }

        Toast.makeText(getContext(), "No suitable camera", Toast.LENGTH_LONG).show();
    }

    private void closeCamera() {
        Log.v(TAG, "closeCamera()");
        if (mCameraDeviceCallback != null) {
            mCameraDeviceCallback.discard();
            mCameraDeviceCallback = null;
        }
        if (mCameraSession != null) {
            Log.v(TAG, "closeCamera(): stop repeating");
            try {
                mCameraSession.stopRepeating();
                mCameraSession.abortCaptures();
            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }
            // Don't call cameraSession.close(), cameraDevice.close() should be enough.
            mCameraSession = null;
        }
        if (mCameraDevice != null) {
            Log.v(TAG, String.format("closeCamera(): close device %h", mCameraDevice));
            mCameraDevice.close();
            mCameraDevice = null;
        }
        mCameraRequest = null;
        if (mSurface != null) {
            Log.v(TAG, String.format("closeCamera(): release surface %h", mSurface));
            mSurface.release();
            mSurface = null;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        queueEvent(() -> mRenderer.setRotationVector(event.values));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onManualCalibration(float dx, float dy) {
        queueEvent(() -> mRenderer.onManualCalibration(dx, dy));
    }

    @Override
    public void onManualCalibrationReset() {
        queueEvent(mRenderer::onManualCalibrationReset);
    }

    public void setCurrentTarget(String callSign) {
        queueEvent(() -> mRenderer.setCurrentTarget(callSign));
    }

    public interface OnTargetClickListener {
        void onTargetClick(Target target);
    }

    private class CameraStateCallback extends CameraDevice.StateCallback {
        private boolean mIsCanceled;

        /**
         * Discard any callbacks and close the camera if it was opened.
         */
        public void discard() {
            mIsCanceled = true;
        }

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.v(TAG, String.format("Camera %h id=%s opened", camera, camera.getId()));

            assert mSurface == null;
            assert mCameraDevice == null;

            // Race condition 1: bring the app to the foreground and then immediately to the
            // background, after openCamera() has been called but not this callback => the camera
            // stays open, nobody closes it.
            //
            // Race condition 2: navigate to the Home page and then immediately to another page,
            // after openCamera() has been called but not this callback => the View is already
            // destroyed.
            if (mIsCanceled) {
                Log.w(TAG, String.format(
                        "Immediately closing camera %h because the callback was canceled", camera));
                camera.close();
                return;
            }

            mCameraDevice = camera;
            final SurfaceTexture surfaceTexture = mRenderer.getSurfaceTexture();
            assert surfaceTexture != null;
            // Setting the default buffer size in openCamera() doesn't work - something is resetting
            // it to the view size after the activity is paused and then resumed again.
            Log.v(TAG, String.format("Setting default buffer size %s", mPreviewSize));
            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mSurface = new Surface(surfaceTexture);
            final List<OutputConfiguration> outputs = new ArrayList<>();
            outputs.add(new OutputConfiguration(mSurface));
            try {
                if (Build.VERSION.SDK_INT < 28) {
                    camera.createCaptureSessionByOutputConfigurations(outputs,
                            mCameraSessionCallback, null);
                } else {
                    camera.createCaptureSession(new SessionConfiguration(
                            SessionConfiguration.SESSION_REGULAR, outputs,
                            getContext().getMainExecutor(), mCameraSessionCallback));
                }

                final CaptureRequest.Builder builder =
                        camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                // Make sure auto-focus is enabled.
                builder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                // Turn off optical and digital stabilization.
                builder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF);
                builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF);
                // Make sure the focal length we used in the FOV calculation is the same one used
                // for capture.
                builder.set(CaptureRequest.LENS_FOCAL_LENGTH, mFocalLength);
                builder.addTarget(mSurface);
                mCameraRequest = builder.build();
            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            Log.v(TAG, String.format("Camera %h id=%s closed", camera, camera.getId()));
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.v(TAG, String.format("Camera %h id=%s disconnected", camera, camera.getId()));
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, String.format("Camera %h id=%s error %d", camera, camera.getId(), error));
            Toast.makeText(getContext(), "Camera error", Toast.LENGTH_LONG).show();

            camera.close();
            ((Activity) getContext()).finish();
        }
    }
}
