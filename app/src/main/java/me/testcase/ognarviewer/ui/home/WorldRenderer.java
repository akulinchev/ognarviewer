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

package me.testcase.ognarviewer.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.hardware.SensorManager;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SoundEffectConstants;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.preference.PreferenceManager;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Locale;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import me.testcase.ognarviewer.App;
import me.testcase.ognarviewer.BuildConfig;
import me.testcase.ognarviewer.CalibratedClock;
import me.testcase.ognarviewer.R;
import me.testcase.ognarviewer.directory.DirectoryEntry;
import me.testcase.ognarviewer.opengl.DrawableTexture;
import me.testcase.ognarviewer.opengl.Shader;
import me.testcase.ognarviewer.opengl.TextTexture;
import me.testcase.ognarviewer.utils.UnitsConverter;
import me.testcase.ognarviewer.world.Aircraft;
import me.testcase.ognarviewer.world.Receiver;
import me.testcase.ognarviewer.world.Target;
import me.testcase.ognarviewer.world.World;

public class WorldRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "WorldRenderer";

    private static final float SCREEN_MARGIN = 20;

    private static final float MANUAL_ADJUSTMENT_LIMIT = 10; // In degrees.

    private static final float Z_NEAR = 10f;
    private static final float Z_FAR = 51000f;

    private static final int COORDS_PER_VERTEX = 3;

    // Constants to manage texture units.
    private static final int TEXTURE_UNIT_TARGET = 0;
    private static final int TEXTURE_UNIT_ONSCREEN_TARGET_SELECTED = 1;
    private static final int TEXTURE_UNIT_OFFSCREEN_TARGET = 2;
    private static final int TEXTURE_UNIT_CAMERA = 10;
    private static final int TEXTURE_UNIT_TEXT = 15;

    // TODO: join the two arrays below into one.
    private static final float[] QUAD_VERTICES = {
            -0.5f, 0.5f, 0,
            -0.5f, -0.5f, 0,
            0.5f, 0.5f, 0,
            0.5f, -0.5f, 0,
            0.5f, 0.5f, 0,
            -0.5f, -0.5f, 0,
    };
    private static final float[] QUAD_TEX_COORDINATES = {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
    };

    /**
     * A vector pointing forward (-Z direction).
     *
     * <p>Used to decide if a target is in front or behind us.</p>
     */
    private static final float[] FORWARD_VECTOR = new float[]{0, 0, -1, 0};

    private static final String VERTEX_SHADER_CODE = "uniform mat4 uProjectionMatrix;\n"
            + "uniform mat4 uModelMatrix;\n"
            + "attribute vec3 aPosition;\n"
            + "attribute vec2 aTexCoordinate;\n"
            + "varying vec2 vTexCoordinate;\n"
            + "void main() {\n"
            + "  gl_Position = uProjectionMatrix * uModelMatrix * vec4(aPosition, 1.0);\n"
            + "  vTexCoordinate = aTexCoordinate;\n"
            + "}";

    private static final String FRAGMENT_SHADER_CODE =
            "#extension GL_OES_EGL_image_external : require\n"
            + "precision highp float;\n" // Otherwise the camera preview has strange artifacts.
            + "uniform vec3 uTintColor;"
            + "uniform sampler2D uTexture;\n"
            + "uniform samplerExternalOES uSurfaceTexture;\n"
            + "uniform mat4 uSurfaceTextureMatrix;\n"
            + "uniform bool uUseSurfaceTexture;\n"
            + "varying vec2 vTexCoordinate;\n"
            + "void main() {\n"
            + "  if (uUseSurfaceTexture) {\n"
            + "    vec4 tmp = uSurfaceTextureMatrix * vec4(vTexCoordinate, 0.0, 1.0);\n"
            + "    gl_FragColor = texture2D(uSurfaceTexture, tmp.st);\n"
            + "  } else {\n"
            + "    vec4 texel = texture2D(uTexture, vTexCoordinate);\n"
            + "    if (texel.a < 0.1) {\n"
            + "      discard;\n"
            + "    }\n"
            + "    gl_FragColor = texel * vec4(uTintColor.rgb, 1.0);\n"
            + "  }\n"
            + "}\n";

    private static final float[] COMPASS_POINTS = new float[]{
            0, Z_FAR, 0, 1, // North.
            0, -Z_FAR, 0, 1, // South.
            Z_FAR, 0, 0, 1, // East.
            -Z_FAR, 0, 0, 1, // West.
    };
    private static final int[] COMPASS_POINT_COLORS = new int[]{
            Color.argb(0xff, 0x19, 0x76, 0xd2), // North.
            Color.argb(0xff, 0xd3, 0x2f, 0x2f), // South.
            Color.argb(0xff, 0xff, 0xff, 0xff), // East.
            Color.argb(0xff, 0xff, 0xff, 0xff), // West.
    };
    private final String[] mCompassPointNames;

    private FloatBuffer mQuadVertexBuffer;
    private FloatBuffer mQuadTexCoordinateBuffer;

    /**
     * A sensors based view matrix, used on the CPU.
     *
     * <p>Input: +X East, -X West, +Y North, -Y South, +Z sky, -Z ground. All values in meters.</p>
     *
     * <p>Output: +X right, -X left, +Y up, -Y down, -Z behind the screen, +Z in front of the
     * screen.</p>
     */
    private final float[] mViewMatrix = new float[16];

    /**
     * An inverse of mViewMatrix, used on the CPU.
     */
    private final float[] mViewMatrixInverted = new float[16];

    /**
     * Rotation matrix for manual compass calibration, used on the CPU.
     */
    private final float[] mManualCalibrationMatrix = new float[16];

    /**
     * A perspective projection matrix, used on the CPU.
     */
    private final float[] mPerspectiveProjectionMatrix = new float[16];

    /**
     * An orthographic projection matrix to be used by OpenGL.
     *
     * <p>The origin is in the middle of the screen. The X axis goes right, the Y axis goes up.</p>
     *
     * <p>The unit is a density independent pixel (dp).</p>
     */
    private final float[] mOrthographicProjectionMatrix = new float[16];

    /**
     * A general-purpose 4x4 matrix to avoid expensive memory allocations in onDrawFrame().
     *
     * <p>May contain any rubbish, always reset it with Matrix.setIdentityM() before use.</p>
     */
    private final float[] mTemp4x4Matrix = new float[16];

    /**
     * Target coordinates.
     *
     * <p>Declared as a member to avoid memory allocation on each frame.</p>
     */
    private final float[] mTargetCoordinates = new float[4];

    /**
     * A weak reference to the view.
     */
    private final WeakReference<WorldView> mWeakView;

    /**
     * Application context to load resources.
     */
    private final Context mContext;
    private final DisplayMetrics mDisplayMetrics;

    private final boolean mShowAircraft;
    private final boolean mShowAircraftWithoutMotion;
    private final boolean mShowReceivers;
    private final boolean mShowCompass;
    private final boolean mShowFpsCounter;
    private final int mMaxDistance;
    private final String mDistanceUnits;
    private final boolean mCompassDeclination;
    private final boolean mDemoMode;

    private int mDisplayRotation;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private int mWidthPx;
    private float mWidthDp;
    private int mHeightPx;
    private float mHeightDp;
    private float mVerticalViewAngle = 1;

    private float mHorizontalRadius;
    private float mVerticalRadius;

    private int mCameraWidth;
    private int mCameraHeight;
    private int mFrameCounter;
    private long mFrameCounterStartTime;
    private Shader mShaderProgram;
    private int mPositionAttributeHandle;
    private int mTexCoordinateAttributeHandle;
    private int mProjectionMatrixUniformHandle;
    private int mModelMatrixUniformHandle;
    private int mTextureUniformHandle;
    private int mSurfaceTextureUniformHandle;
    private int mSurfaceTextureMatrixUniformHandle;
    private int mTintColorUniformHandle;
    private TextTexture mFpsTextTexture;
    private SurfaceTexture mSurfaceTexture;
    private int mUseSurfaceTextureUniformHandle;
    private DrawableTexture mOnscreenTargetTexture;
    private DrawableTexture mSelectedOnscreenTargetTexture;
    private DrawableTexture mOffscreenTargetTexture;

    private World mWorld;
    private Target[] mTargets = new Target[1];

    private float mVerticalRotation;
    private float mHorizontalRotation;

    /**
     * The call sign of the selected target or null if no target is currently selected.
     */
    private volatile String mSelectedTarget;

    /**
     * When the user taps the view, this member becomes non-null.
     *
     * <p>On the next frame, the actual hit test is done. The result is submitted back to the view
     * and this member becomes null again.</p>
     */
    private PointF mHitTestPoint;

    public WorldRenderer(WorldView view) {
        mWeakView = new WeakReference<>(view);
        mContext = view.getContext().getApplicationContext();
        mDisplayMetrics = mContext.getResources().getDisplayMetrics();
        mDisplayRotation = 0;

        mCompassPointNames = view.getResources().getStringArray(R.array.compass_signs);

        // Read the settings once because we're going to access them at >60 FPS.
        final SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(view.getContext());
        mShowAircraft = preferences.getBoolean("show_aircraft", true);
        mShowAircraftWithoutMotion = preferences.getBoolean("show_aircraft_without_motion", true);
        mShowReceivers = preferences.getBoolean("show_receivers", true);
        mShowCompass = preferences.getBoolean("show_compass", true);
        mShowFpsCounter = preferences.getBoolean("show_fps", false);
        mMaxDistance = preferences.getInt("max_distance", 20) * 1000;
        mDistanceUnits = preferences.getString("units_distance", "km");
        mCompassDeclination = preferences.getBoolean("compass_declination", true);
        mDemoMode = preferences.getBoolean("demo_mode", false);

        // Initialize all matrices just for sure.
        Matrix.setIdentityM(mViewMatrix, 0);
        Matrix.setIdentityM(mViewMatrixInverted, 0);
        Matrix.setIdentityM(mManualCalibrationMatrix, 0);
        Matrix.setIdentityM(mPerspectiveProjectionMatrix, 0);
        Matrix.setIdentityM(mOrthographicProjectionMatrix, 0);
    }

    public void setWorld(World world) {
        mWorld = world;
    }

    public void setCameraMetadata(int width, int height, float verticalViewAngle) {
        mCameraWidth = width;
        mCameraHeight = height;
        mVerticalViewAngle = verticalViewAngle;
        updatePerspectiveProjectionMatrix();
    }

    public void setRotationVector(float[] vector) {
        SensorManager.getRotationMatrixFromVector(mViewMatrix, vector);
        // mDisplayRotation is the screen rotation, not device rotation, i.e. it has the opposite
        // direction.
        if (mDisplayRotation == Surface.ROTATION_90) {
            SensorManager.remapCoordinateSystem(mViewMatrix, SensorManager.AXIS_Y,
                    SensorManager.AXIS_MINUS_X, mViewMatrix);
        } else if (mDisplayRotation == Surface.ROTATION_180) {
            SensorManager.remapCoordinateSystem(mViewMatrix, SensorManager.AXIS_MINUS_X,
                    SensorManager.AXIS_MINUS_Y, mViewMatrix);
        } else if (mDisplayRotation == Surface.ROTATION_270) {
            SensorManager.remapCoordinateSystem(mViewMatrix, SensorManager.AXIS_MINUS_Y,
                    SensorManager.AXIS_X, mViewMatrix);
        }
        // Compensate for compass declination.
        if (mCompassDeclination && mWorld.getGeomagneticField() != null) {
            final float declination = mWorld.getGeomagneticField().getDeclination();
            Matrix.rotateM(mViewMatrix, 0, declination * -1, 0, 0, 1);
        }
        Matrix.invertM(mViewMatrixInverted, 0, mViewMatrix, 0);
    }

    public void onManualCalibration(float dx, float dy) {
        final float aspectRatio = mWidthPx / (float) mHeightPx;
        mVerticalRotation -= (dy / mHeightPx) * mVerticalViewAngle;
        mVerticalRotation = Math.max(-MANUAL_ADJUSTMENT_LIMIT, Math.min(mVerticalRotation,
                MANUAL_ADJUSTMENT_LIMIT));
        mHorizontalRotation -= (dx / mWidthPx) * mVerticalViewAngle * aspectRatio;
        mHorizontalRotation = Math.max(-MANUAL_ADJUSTMENT_LIMIT, Math.min(mHorizontalRotation,
                MANUAL_ADJUSTMENT_LIMIT));
        Matrix.setIdentityM(mManualCalibrationMatrix, 0);
        Matrix.rotateM(mManualCalibrationMatrix, 0, mHorizontalRotation, 0, 1, 0);
        Matrix.rotateM(mManualCalibrationMatrix, 0, mVerticalRotation, 1, 0, 0);
    }

    public void onManualCalibrationReset() {
        mVerticalRotation = 0;
        mHorizontalRotation = 0;
        Matrix.setIdentityM(mManualCalibrationMatrix, 0);
    }

    public void setHitTestPoint(float x, float y) {
        mHitTestPoint = new PointF(x / mDisplayMetrics.density - mWidthDp * 0.5f,
                -y / mDisplayMetrics.density + mHeightDp * 0.5f);
    }

    public void setCurrentTarget(String callSign) {
        mSelectedTarget = callSign;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.v(TAG, "onSurfaceCreated()");

        // All old textures are invalid now.
        TextTexture.clearCache();

        mQuadVertexBuffer = ByteBuffer
                .allocateDirect(QUAD_VERTICES.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mQuadVertexBuffer.put(QUAD_VERTICES);
        mQuadVertexBuffer.position(0);

        mQuadTexCoordinateBuffer = ByteBuffer
                .allocateDirect(QUAD_TEX_COORDINATES.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mQuadTexCoordinateBuffer.put(QUAD_TEX_COORDINATES);
        mQuadTexCoordinateBuffer.position(0);

        mShaderProgram = new Shader(VERTEX_SHADER_CODE, FRAGMENT_SHADER_CODE);
        // Cache attribute and uniform handles.
        mPositionAttributeHandle = mShaderProgram.getAttributeLocation("aPosition");
        mTexCoordinateAttributeHandle = mShaderProgram.getAttributeLocation("aTexCoordinate");
        mProjectionMatrixUniformHandle = mShaderProgram.getUniformLocation("uProjectionMatrix");
        mModelMatrixUniformHandle = mShaderProgram.getUniformLocation("uModelMatrix");
        mTextureUniformHandle = mShaderProgram.getUniformLocation("uTexture");
        mSurfaceTextureUniformHandle = mShaderProgram.getUniformLocation("uSurfaceTexture");
        mSurfaceTextureMatrixUniformHandle = mShaderProgram.getUniformLocation(
                "uSurfaceTextureMatrix");
        mUseSurfaceTextureUniformHandle = mShaderProgram.getUniformLocation("uUseSurfaceTexture");
        mTintColorUniformHandle = mShaderProgram.getUniformLocation("uTintColor");

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glEnable(GL10.GL_BLEND);
        GLES20.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + TEXTURE_UNIT_TARGET);
        mOnscreenTargetTexture = new DrawableTexture(AppCompatResources.getDrawable(mContext,
                R.drawable.target_onscreen));

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + TEXTURE_UNIT_ONSCREEN_TARGET_SELECTED);
        mSelectedOnscreenTargetTexture =
                new DrawableTexture(AppCompatResources.getDrawable(mContext,
                        R.drawable.target_onscreen_selected));

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + TEXTURE_UNIT_OFFSCREEN_TARGET);
        mOffscreenTargetTexture = new DrawableTexture(AppCompatResources.getDrawable(mContext,
                R.drawable.target_offscreen));

        createSurfaceTexture();

        mShaderProgram.use();
        GLES20.glEnableVertexAttribArray(mPositionAttributeHandle);
        GLES20.glEnableVertexAttribArray(mTexCoordinateAttributeHandle);
        GLES20.glUniform1i(mSurfaceTextureUniformHandle, TEXTURE_UNIT_CAMERA);

        final WorldView view = mWeakView.get();
        if (view != null) {
            view.post(view::openCamera);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.v(TAG, String.format("onSurfaceChanged(%d, %d)", width, height));

        GLES20.glViewport(0, 0, width, height);

        mWidthPx = width;
        mWidthDp = width / mDisplayMetrics.density;
        mHeightPx = height;
        mHeightDp = height / mDisplayMetrics.density;

        // The ellipse on which the off-screen indicators are placed.
        mHorizontalRadius = mWidthDp * 0.5f - SCREEN_MARGIN;
        mVerticalRadius = mHeightDp * 0.5f - SCREEN_MARGIN;

        updatePerspectiveProjectionMatrix();

        Matrix.orthoM(mOrthographicProjectionMatrix,
                0,
                mWidthDp * -0.5f,
                mWidthDp * 0.5f,
                mHeightDp * -0.5f,
                mHeightDp * 0.5f,
                -1,
                Z_FAR);
        GLES20.glUniformMatrix4fv(mProjectionMatrixUniformHandle, 1, false,
                mOrthographicProjectionMatrix, 0);
    }

    private void updatePerspectiveProjectionMatrix() {
        final float aspectRatio = mWidthDp / mHeightDp;
        Matrix.perspectiveM(mPerspectiveProjectionMatrix, 0, mVerticalViewAngle, aspectRatio,
                Z_NEAR, Z_FAR);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        drawCameraPreview();
        if (mShowCompass) {
            drawCompass();
        }
        drawTargets();
        if (mShowFpsCounter) {
            drawFpsCounter();
        }
        checkError();
    }

    /**
     * Draws compass points (N, S, E, W).
     */
    private void drawCompass() {
        for (int i = 0; i < mCompassPointNames.length; ++i) {
            Matrix.multiplyMV(mTemp4x4Matrix, 0, mViewMatrix, 0, COMPASS_POINTS, i * 4);
            Matrix.multiplyMV(mTemp4x4Matrix, 0, mManualCalibrationMatrix, 0, mTemp4x4Matrix, 0);
            Matrix.multiplyMV(mTemp4x4Matrix, 0, mPerspectiveProjectionMatrix, 0, mTemp4x4Matrix,
                    0);
            perspectiveDivision(mTemp4x4Matrix);
            if (-1 <= mTemp4x4Matrix[2] && mTemp4x4Matrix[2] <= 1) {
                setTintColor(COMPASS_POINT_COLORS[i]);
                final float x = mTemp4x4Matrix[0] * mWidthDp * 0.5f;
                final float y = mTemp4x4Matrix[1] * mHeightDp * 0.5f;
                drawText(mCompassPointNames[i], x, y, 0);
            }
        }
    }

    private void drawTargets() {
        if (mWorld == null) {
            return;
        }

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);

        final long now = CalibratedClock.currentTimeMillis();

        mTargets = mWorld.getTargets(mTargets);
        for (Target target : mTargets) {
            if (target == null) {
                break;
            }
            if (target instanceof Aircraft) {
                if (!mShowAircraft) {
                    continue;
                }
                if (!mShowAircraftWithoutMotion) {
                    final Aircraft aircraft = (Aircraft) target;
                    if (aircraft.getGroundSpeed() == 0) {
                        continue;
                    }
                }
            } else if (target instanceof Receiver) {
                if (!mShowReceivers) {
                    continue;
                }
                // For receivers, there are two beacons - with and without location. Skip the one
                // without.
                if (target.getPositionTime() == 0) {
                    continue;
                }
            }

            mWorld.getTargetCoordinates(target, now, mTargetCoordinates);
            final double distance =
                    Math.sqrt(mTargetCoordinates[0] * mTargetCoordinates[0]
                            + mTargetCoordinates[1] * mTargetCoordinates[1]
                            + mTargetCoordinates[2] * mTargetCoordinates[2]);
            if (distance > mMaxDistance) {
                continue; // Too far away...
            }

            Matrix.multiplyMV(mTemp4x4Matrix, 0, mViewMatrix, 0, mTargetCoordinates, 0);
            Matrix.multiplyMV(mTemp4x4Matrix, 0, mManualCalibrationMatrix, 0, mTemp4x4Matrix, 0);
            Matrix.multiplyMV(mTemp4x4Matrix, 0, mPerspectiveProjectionMatrix, 0, mTemp4x4Matrix,
                    0);
            perspectiveDivision(mTemp4x4Matrix);
            float x = mTemp4x4Matrix[0] * mWidthDp * 0.5f;
            float y = mTemp4x4Matrix[1] * mHeightDp * 0.5f;

            // Remember if the target was outside the clip space before reusing mTemp4x4Matrix.
            final boolean offscreen =
                    mTemp4x4Matrix[0] < -1 || mTemp4x4Matrix[0] > 1 || mTemp4x4Matrix[1] < -1
                            || mTemp4x4Matrix[1] > 1;

            // A target is behind us if the dot product is negative (position * forward < 0).
            Matrix.multiplyMV(mTemp4x4Matrix, 4, mViewMatrixInverted, 0, FORWARD_VECTOR, 0);
            final boolean behind =
                    mTargetCoordinates[0] * mTemp4x4Matrix[4]
                            + mTargetCoordinates[1] * mTemp4x4Matrix[5]
                            + mTargetCoordinates[2] * mTemp4x4Matrix[6] < 0;

            setTintColor(target.getColor());

            if (behind || offscreen) {
                // Need to invert the coordinates if the target is behind us.
                if (behind) {
                    x = -x;
                    y = -y;
                }

                final float angle = (float) Math.toDegrees(Math.atan2(y, x));

                // Calculate a point on the ellipse. See https://math.stackexchange.com/a/22067.
                final float tan = (float) Math.tan(Math.toRadians(angle));
                float arrowX =
                        (float) ((mHorizontalRadius * mVerticalRadius) / Math.sqrt(
                                mVerticalRadius * mVerticalRadius
                                        + mHorizontalRadius * mHorizontalRadius * tan * tan));
                if (x < 0) {
                    arrowX = -arrowX;
                }
                float arrowY =
                        (float) ((mHorizontalRadius * mVerticalRadius) / Math.sqrt(
                                mHorizontalRadius * mHorizontalRadius
                                        + (mVerticalRadius * mVerticalRadius) / (tan * tan)));
                if (y < 0) {
                    arrowY = -arrowY;
                }

                drawOffscreenTargetIndicator(arrowX, arrowY, angle);
            } else {
                final float z = (float) -distance;

                final boolean selected =
                        mSelectedTarget != null && mSelectedTarget.equals(target.getCallSign());
                drawOnscreenTargetIndicator(x, y, z, selected);

                if (mHitTestPoint != null && !selected) {
                    if (x - 25 <= mHitTestPoint.x && mHitTestPoint.x <= x + 25
                            && y - 25 < mHitTestPoint.y && mHitTestPoint.y <= y + 25) {
                        mHandler.post(() -> {
                            final WorldView view = mWeakView.get();
                            if (view != null) {
                                view.playSoundEffect(SoundEffectConstants.CLICK);
                                view.onHitTestResult(target);
                            }
                        });
                        mHitTestPoint = null;
                    }
                }

                float textY = y - 38;
                String displayName = null;
                if (target instanceof Aircraft) {
                    final Aircraft aircraft = (Aircraft) target;
                    final DirectoryEntry entry =
                            App.getDirectoryRepository().find(aircraft.getDirectoryId());
                    if (entry != null) {
                        displayName = entry.getRegistration();
                    }
                } else if (target instanceof Receiver) {
                    final Receiver receiver = (Receiver) target;
                    displayName = receiver.getCallSign();
                }
                if (displayName != null) {
                    drawText(displayName, x, textY, z);
                    textY -= 20;
                }
                final String distanceString;
                if (mDistanceUnits.equals("nm")) {
                    final double nm = UnitsConverter.metresToNauticalMiles(distance);
                    distanceString = mContext.getString(R.string.distance_nm, nm);
                } else if (mDistanceUnits.equals("mi")) {
                    final double miles = UnitsConverter.metresToMiles(distance);
                    distanceString = mContext.getString(R.string.distance_mi, miles);
                } else if (distance > 1000) {
                    distanceString = mContext.getString(R.string.distance_km, distance / 1000);
                } else {
                    distanceString = mContext.getString(R.string.distance_m, Math.round(distance));
                }
                drawText(distanceString, x, textY, z);
            }
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        if (mHitTestPoint != null) {
            mHandler.post(() -> {
                final WorldView view = mWeakView.get();
                if (view != null) {
                    view.onHitTestResult(null);
                }
            });
            mHitTestPoint = null;
            mSelectedTarget = null;
        }
    }

    /**
     * Draws the current FPS in the middle of the screen.
     *
     * <p>Most users don't see it, because it is disabled by default.</p>
     */
    private void drawFpsCounter() {
        ++mFrameCounter;
        final long uptime = SystemClock.uptimeMillis();
        final long elapsed = uptime - mFrameCounterStartTime;
        if (elapsed > 1000) {
            final long fps = Math.round(mFrameCounter * 1000.0 / elapsed);
            final String text = String.format(Locale.US, "%d FPS", fps);
            // FIXME: find a better solution, the FPS texture can be deleted if there is not enough
            //  space in the cache!
            mFpsTextTexture = TextTexture.of(text, mDisplayMetrics.density);
            mFrameCounter = 0;
            mFrameCounterStartTime = uptime;
        }
        setTintColor(Color.GREEN);
        drawTextTexture(mFpsTextTexture, 0, 0, 0);
        checkError();
    }

    private void drawOnscreenTargetIndicator(float x, float y, float z, boolean selected) {
        Matrix.setIdentityM(mTemp4x4Matrix, 0);
        Matrix.translateM(mTemp4x4Matrix, 0, x, y, z);
        final float width;
        final float height;
        if (selected) {
            width = mSelectedOnscreenTargetTexture.getWidth();
            height = mSelectedOnscreenTargetTexture.getHeight();
        } else {
            width = mOnscreenTargetTexture.getWidth();
            height = mOnscreenTargetTexture.getHeight();
        }
        Matrix.scaleM(mTemp4x4Matrix, 0, width / mDisplayMetrics.density,
                height / mDisplayMetrics.density, 0);
        Matrix.rotateM(mTemp4x4Matrix, 0, 45, 0, 0, 1);

        GLES20.glUniform1i(mTextureUniformHandle,
                selected ? TEXTURE_UNIT_ONSCREEN_TARGET_SELECTED : TEXTURE_UNIT_TARGET);
        GLES20.glUniformMatrix4fv(mModelMatrixUniformHandle, 1, false, mTemp4x4Matrix, 0);

        GLES20.glVertexAttribPointer(mPositionAttributeHandle, 3, GLES20.GL_FLOAT, false, 12,
                mQuadVertexBuffer);
        GLES20.glVertexAttribPointer(mTexCoordinateAttributeHandle, 2, GLES20.GL_FLOAT, false, 8,
                mQuadTexCoordinateBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

        checkError();
    }

    private void drawOffscreenTargetIndicator(float x, float y, float angle) {
        Matrix.setIdentityM(mTemp4x4Matrix, 0);
        Matrix.translateM(mTemp4x4Matrix, 0, x, y, 0);
        Matrix.scaleM(mTemp4x4Matrix, 0,
                mOffscreenTargetTexture.getWidth() / mDisplayMetrics.density,
                mOffscreenTargetTexture.getHeight() / mDisplayMetrics.density, 0);
        Matrix.rotateM(mTemp4x4Matrix, 0, angle, 0, 0, 1);

        GLES20.glUniform1i(mTextureUniformHandle, TEXTURE_UNIT_OFFSCREEN_TARGET);
        GLES20.glUniformMatrix4fv(mModelMatrixUniformHandle, 1, false, mTemp4x4Matrix, 0);

        GLES20.glVertexAttribPointer(mPositionAttributeHandle, 3, GLES20.GL_FLOAT, false, 12,
                mQuadVertexBuffer);
        GLES20.glVertexAttribPointer(mTexCoordinateAttributeHandle, 2, GLES20.GL_FLOAT, false, 8,
                mQuadTexCoordinateBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

        checkError();
    }

    private void drawText(@NonNull String text, float x, float y, float z) {
        drawTextTexture(TextTexture.of(text, mDisplayMetrics.density), x, y, z);
    }

    private void drawTextTexture(@NonNull TextTexture texture, float x, float y, float z) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + TEXTURE_UNIT_TEXT);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture.getHandle());

        Matrix.setIdentityM(mTemp4x4Matrix, 0);
        Matrix.translateM(mTemp4x4Matrix, 0, x, y, z);
        Matrix.scaleM(mTemp4x4Matrix, 0, texture.getWidth(), texture.getHeight(), 0);
        GLES20.glUniformMatrix4fv(mModelMatrixUniformHandle, 1, false, mTemp4x4Matrix, 0);
        GLES20.glVertexAttribPointer(mPositionAttributeHandle, 3, GLES20.GL_FLOAT, false, 12,
                mQuadVertexBuffer);
        GLES20.glVertexAttribPointer(mTexCoordinateAttributeHandle, 2, GLES20.GL_FLOAT, false, 8,
                mQuadTexCoordinateBuffer);
        GLES20.glUniform1i(mTextureUniformHandle, TEXTURE_UNIT_TEXT);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, QUAD_VERTICES.length / COORDS_PER_VERTEX);

        checkError();
    }

    private void createSurfaceTexture() {
        final int[] handles = new int[1];
        GLES20.glGenTextures(1, handles, 0);
        mSurfaceTexture = new SurfaceTexture(handles[0]);
    }

    /**
     * Draws the most recent camera frame.
     */
    private void drawCameraPreview() {
        if (mCameraWidth == 0) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            return; // Avoid division by zero.
        }

        // Tell the fragment shader it should sample the GL_TEXTURE_EXTERNAL_OES target.
        // FIXME: is it more or less efficient to use a separate program for it?
        GLES20.glUniform1i(mUseSurfaceTextureUniformHandle, 1);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + TEXTURE_UNIT_CAMERA);
        mSurfaceTexture.updateTexImage(); // "Calls" glBindTexture().
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);

        mSurfaceTexture.getTransformMatrix(mTemp4x4Matrix);
        GLES20.glUniformMatrix4fv(mSurfaceTextureMatrixUniformHandle, 1, false, mTemp4x4Matrix, 0);

        Matrix.setIdentityM(mTemp4x4Matrix, 0);
        // FIXME: is the code below really correct?
        final float cameraAspectRatio = mCameraHeight / (float) mCameraWidth;
        if (mHeightPx > mWidthPx) {
            // Portrait orientation.
            Matrix.scaleM(mTemp4x4Matrix, 0, mHeightDp * cameraAspectRatio, mHeightDp, 0);
        } else {
            // Landscape orientation.
            Matrix.scaleM(mTemp4x4Matrix, 0, mWidthDp, mWidthDp * cameraAspectRatio, 0);
        }
        if (mDisplayRotation == Surface.ROTATION_90) {
            Matrix.rotateM(mTemp4x4Matrix, 0, 90, 0, 0, 1);
        } else if (mDisplayRotation == Surface.ROTATION_180) {
            Matrix.rotateM(mTemp4x4Matrix, 0, 180, 0, 0, 1);
        } else if (mDisplayRotation == Surface.ROTATION_270) {
            Matrix.rotateM(mTemp4x4Matrix, 0, 270, 0, 0, 1);
        }

        GLES20.glUniformMatrix4fv(mModelMatrixUniformHandle, 1, false, mTemp4x4Matrix, 0);
        GLES20.glUniform1i(mSurfaceTextureUniformHandle, TEXTURE_UNIT_CAMERA);
        GLES20.glVertexAttribPointer(mPositionAttributeHandle, 3, GLES20.GL_FLOAT, false, 12,
                mQuadVertexBuffer);
        GLES20.glVertexAttribPointer(mTexCoordinateAttributeHandle, 2, GLES20.GL_FLOAT, false, 8,
                mQuadTexCoordinateBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

        // Tell the fragment shader it should sample the GL_TEXTURE_2D target again.
        GLES20.glUniform1i(mUseSurfaceTextureUniformHandle, 0);

        // Make sure we are still in a valid state before leaving the function.
        checkError();
    }

    public boolean isAvailable() {
        return mSurfaceTexture != null;
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public void setDisplayRotation(int rotation) {
        mDisplayRotation = rotation;
    }

    /**
     * Updates the current tint color in the fragment shader.
     */
    private void setTintColor(int color) {
        final float red = Color.red(color) / 255.0f;
        final float green = Color.green(color) / 255.0f;
        final float blue = Color.blue(color) / 255.0f;
        GLES20.glUniform3f(mTintColorUniformHandle, red, green, blue);
        checkError();
    }

    /**
     * Does perspective division.
     */
    private static void perspectiveDivision(float[] vector) {
        vector[0] /= vector[3];
        vector[1] /= vector[3];
        vector[2] /= vector[3];
        vector[3] = 1;
    }

    /**
     * In debug mode, throws a RuntimeException if there have been any OpenGL errors since the last
     * call.
     *
     * <p>In release mode, does nothing.</p>
     */
    private static void checkError() {
        if (BuildConfig.DEBUG) {
            final int error = GLES20.glGetError();
            if (error != GLES20.GL_NO_ERROR) {
                throw new RuntimeException("glGetError() returned " + getGLErrorString(error));
            }
        }
    }

    private static String getGLErrorString(int error) {
        switch (error) {
            case GLES20.GL_INVALID_ENUM:
                return "GL_INVALID_ENUM";
            case GLES20.GL_INVALID_VALUE:
                return "GL_INVALID_VALUE";
            case GLES20.GL_INVALID_OPERATION:
                return "GL_INVALID_OPERATION";
            case GLES20.GL_INVALID_FRAMEBUFFER_OPERATION:
                return "GL_INVALID_FRAMEBUFFER_OPERATION";
            case GLES20.GL_OUT_OF_MEMORY:
                return "GL_OUT_OF_MEMORY";
            default:
                return GLUtils.getEGLErrorString(error);
        }
    }
}
